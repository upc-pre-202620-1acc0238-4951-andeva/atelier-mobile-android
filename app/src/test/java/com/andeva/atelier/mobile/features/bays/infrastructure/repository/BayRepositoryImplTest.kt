package com.andeva.atelier.mobile.features.bays.infrastructure.repository

import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.core.domain.MissingBranchException
import com.andeva.atelier.mobile.features.bays.domain.BayStatus
import com.andeva.atelier.mobile.features.bays.infrastructure.BayTestEnv
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Duration
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.SocketPolicy
import com.andeva.atelier.mobile.features.bays.domain.BayBoardItem
import com.andeva.atelier.mobile.features.workorders.infrastructure.local.toEntity
import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.WorkOrderDto
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BayRepositoryImplTest {

    private val env = BayTestEnv()
    private lateinit var repository: BayRepositoryImpl

    /**
     * Los tableros observados corren en hilos reales. Se cancelan y se esperan antes de cerrar Room y el servidor:
     * si no, una consulta a medias falla con la base ya cerrada y el error aparece en el test siguiente.
     */
    private val uncaught = CopyOnWriteArrayList<Throwable>()
    private val collectors = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, e -> uncaught += e }
    )

    @Before
    fun setUp() {
        env.start()
        repository = env.repository()
    }

    @After
    fun tearDown() {
        runBlocking { collectors.coroutineContext.job.cancelAndJoin() }
        env.stop()
        assertEquals(emptyList<Throwable>(), uncaught)
    }

    private fun observeInto(emissions: Channel<List<BayBoardItem>>) {
        collectors.launch { repository.observeBoard().collect { emissions.send(it) } }
    }

    @Test
    fun `refresca las bahias de la sede de la sesion`() = runTest {
        env.signIn()
        env.responses["GET /api/v1/work-bays"] = env.json(listOf(env.bay("bay2", "Foso 2"), env.bay("bay1", "Elevador 1")))

        repository.refreshBranchBays().getOrThrow()

        assertEquals("b1", env.requested("GET", "/api/v1/work-bays").single().requestUrl!!.queryParameter("branchId"))
        assertEquals(listOf("Elevador 1", "Foso 2"), repository.observeBoard().first().map { it.bay.name })
        val metadata = env.db.cacheMetadataDao()
        assertEquals(2, metadata.get(BayCacheScope.of("b1"))!!.recordCount)
    }

    @Test
    fun `sin sede falla con MissingBranchException sin llamar al backend`() = runTest {
        env.signIn(branchId = null)

        val error = repository.refreshBranchBays().exceptionOrNull()

        assertTrue(error is MissingBranchException)
        assertTrue(env.requests.isEmpty())
    }

    @Test
    fun `el tablero muestra placa, mecanicos y tiempo de la bahia ocupada`() = runTest {
        env.signIn()
        env.cachePlate()
        env.responses["GET /api/v1/work-bays"] = env.json(
            listOf(env.bay("bay1", "Elevador 1", "OCCUPIED", "wo1"), env.bay("bay2", "Foso 2"))
        )
        env.responses["GET /api/v1/work-orders"] = env.json(listOf(env.workOrderSummary(currentBayId = "bay1")))
        env.responses["GET /api/v1/work-orders/wo1"] = env.json(
            env.workOrderDetail(tasks = listOf(env.task("t1", "m9"), env.task("t2", "m8"), env.task("t3", "m9", status = "COMPLETED")))
        )
        env.db.workOrderDao().replaceBranch("b1", listOf(summaryEntity()))
        repository.refreshBranchBays().getOrThrow()

        val emissions = Channel<List<BayBoardItem>>(Channel.UNLIMITED)
        observeInto(emissions)
        val board = emissions.untilLoaded()

        val occupied = board.first { it.bay.id == "bay1" }
        assertEquals(BayStatus.OCCUPIED, occupied.bay.status)
        val occupant = occupied.occupant!!
        assertEquals("ABC123", occupant.plate)
        assertEquals(15, occupant.internalNumber)
        assertEquals(listOf("m9", "m8"), occupant.mechanicIds)
        // Dos tareas en curso: (60 min - 10 min de pausa) cada una.
        assertEquals(Duration.ofMinutes(100), occupant.laborElapsed)
        assertNull(board.first { it.bay.id == "bay2" }.occupant)
    }

    @Test
    fun `el detalle de la orden se lee una sola vez aunque se refresque el tablero`() = runTest {
        env.signIn()
        env.responses["GET /api/v1/work-bays"] = env.json(listOf(env.bay("bay1", "Elevador 1", "OCCUPIED", "wo1")))
        env.responses["GET /api/v1/work-orders/wo1"] = env.json(env.workOrderDetail(tasks = listOf(env.task())))
        env.db.workOrderDao().replaceBranch("b1", listOf(summaryEntity()))
        val emissions = Channel<List<BayBoardItem>>(Channel.UNLIMITED)
        repository.refreshBranchBays().getOrThrow()
        observeInto(emissions)

        val first = emissions.untilLoaded().single()
        repository.refreshBranchBays().getOrThrow()
        emissions.next()
        repository.refreshBranchBays().getOrThrow()
        emissions.next()

        assertEquals(listOf("m9"), first.occupant!!.mechanicIds)
        assertEquals(1, env.requested("GET", "/api/v1/work-orders/wo1").size)
    }

    @Test
    fun `reasignar a una bahia ocupada devuelve el message del 409 y no cambia la cache`() = runTest {
        env.signIn()
        env.responses["GET /api/v1/work-bays"] = env.json(listOf(env.bay("bay1", "Elevador 1"), env.bay("bay2", "Foso 2", "OCCUPIED", "wo9")))
        repository.refreshBranchBays().getOrThrow()
        env.db.workOrderDao().replaceBranch("b1", listOf(summaryEntity()))
        env.responses["PUT /api/v1/work-orders/wo1/bay"] = env.error(409, "CONFLICT", "La bahía ya está ocupada")
        val requestsBefore = env.requests.size

        val error = repository.reassignBay("wo1", "bay2").exceptionOrNull() as ApiException

        assertEquals(409, error.httpStatus)
        assertEquals("La bahía ya está ocupada", error.message)
        assertEquals(1, env.requests.size - requestsBefore)
        assertEquals(listOf(BayStatus.AVAILABLE, BayStatus.OCCUPIED), repository.observeBoard().first().map { it.bay.status })
        assertNull(env.db.workOrderDao().findById("wo1")!!.currentBayId)
    }

    @Test
    fun `reasignar actualiza la cabecera de la orden y recarga las bahias`() = runTest {
        env.signIn()
        env.responses["GET /api/v1/work-bays"] = env.json(listOf(env.bay("bay1", "Elevador 1"), env.bay("bay2", "Foso 2")))
        repository.refreshBranchBays().getOrThrow()
        env.db.workOrderDao().replaceBranch("b1", listOf(summaryEntity()))
        env.responses["PUT /api/v1/work-orders/wo1/bay"] = env.json(env.workOrderDetail(currentBayId = "bay2", bayName = "Foso 2"))
        env.responses["GET /api/v1/work-bays"] = env.json(
            listOf(env.bay("bay1", "Elevador 1"), env.bay("bay2", "Foso 2", "OCCUPIED", "wo1"))
        )

        repository.reassignBay("wo1", "bay2").getOrThrow()

        val body = env.requested("PUT", "/api/v1/work-orders/wo1/bay").single().body.readUtf8()
        assertTrue(body.contains("\"bayId\":\"bay2\""))
        val header = env.db.workOrderDao().findById("wo1")!!
        assertEquals("bay2", header.currentBayId)
        assertEquals("Foso 2", header.bayName)
        assertEquals(2, env.requested("GET", "/api/v1/work-bays").size)
        assertEquals(BayStatus.OCCUPIED, repository.observeBoard().first().first { it.bay.id == "bay2" }.bay.status)
    }

    @Test
    fun `el tablero sale de Room aunque el detalle de la orden no responda`() = runTest {
        env.signIn()
        env.cachePlate()
        env.responses["GET /api/v1/work-bays"] = env.json(listOf(env.bay("bay1", "Elevador 1", "OCCUPIED", "wo1")))
        env.responses["GET /api/v1/work-orders/wo1"] = MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE)
        env.db.workOrderDao().replaceBranch("b1", listOf(summaryEntity()))
        repository.refreshBranchBays().getOrThrow()
        val emissions = Channel<List<BayBoardItem>>(Channel.UNLIMITED)
        observeInto(emissions)

        val occupant = emissions.next().single().occupant!!

        assertEquals("ABC123", occupant.plate)
        assertEquals(emptyList<String>(), occupant.mechanicIds)
        assertNull(occupant.laborElapsed)
    }

    @Test
    fun `un detalle que falla no se reintenta en cada emision`() = runTest {
        env.signIn()
        env.responses["GET /api/v1/work-bays"] = env.json(listOf(env.bay("bay1", "Elevador 1", "OCCUPIED", "wo1")))
        env.responses["GET /api/v1/work-orders/wo1"] = env.error(403, "FORBIDDEN", "Sin permiso")
        env.db.workOrderDao().replaceBranch("b1", listOf(summaryEntity()))
        val emissions = Channel<List<BayBoardItem>>(Channel.UNLIMITED)
        repository.refreshBranchBays().getOrThrow()
        observeInto(emissions)

        emissions.next()
        repository.refreshBranchBays().getOrThrow()
        emissions.next()
        repository.refreshBranchBays().getOrThrow()
        emissions.next()

        assertEquals(1, env.requested("GET", "/api/v1/work-orders/wo1").size)
    }

    @Test
    fun `el repositorio de bahias se comparte en toda la app`() {
        assertTrue(BayRepositoryImpl::class.java.isAnnotationPresent(javax.inject.Singleton::class.java))
    }

    @Test
    fun `reasignar el mecanico refresca el tablero observado con la misma instancia`() = runTest {
        env.signIn()
        env.responses["GET /api/v1/work-bays"] = env.json(listOf(env.bay("bay1", "Elevador 1", "OCCUPIED", "wo1")))
        env.responses["GET /api/v1/work-orders/wo1"] = env.json(env.workOrderDetail(tasks = listOf(env.task("t1", "m9"))))
        env.responses["PUT /api/v1/tasks/t1/mechanic"] = env.json(env.task("t1", "m7"))
        env.db.workOrderDao().replaceBranch("b1", listOf(summaryEntity()))
        repository.refreshBranchBays().getOrThrow()
        val emissions = Channel<List<BayBoardItem>>(Channel.UNLIMITED)
        observeInto(emissions)
        assertEquals(listOf("m9"), emissions.untilLoaded().single().occupant!!.mechanicIds)
        env.responses["GET /api/v1/work-orders/wo1"] = env.json(env.workOrderDetail(tasks = listOf(env.task("t1", "m7"))))

        repository.reassignMechanic("t1", "m7").getOrThrow()

        while (emissions.next().single().occupant!!.mechanicIds != listOf("m7")) Unit
        assertTrue(env.requested("PUT", "/api/v1/tasks/t1/mechanic").single().body.readUtf8().contains("\"mechanicId\":\"m7\""))
        assertEquals(2, env.requested("GET", "/api/v1/work-orders/wo1").size)
    }

    @Test
    fun `el tablero guarda la cabecera del detalle y muestra el numero interno sin lista previa`() = runTest {
        env.signIn()
        env.responses["GET /api/v1/work-bays"] = env.json(listOf(env.bay("bay1", "Elevador 1", "OCCUPIED", "wo1")))
        env.responses["GET /api/v1/work-orders/wo1"] = env.json(env.workOrderDetail(tasks = listOf(env.task())))
        repository.refreshBranchBays().getOrThrow()
        val emissions = Channel<List<BayBoardItem>>(Channel.UNLIMITED)
        observeInto(emissions)

        var occupant = emissions.next().single().occupant!!
        while (occupant.internalNumber == null) occupant = emissions.next().single().occupant!!

        assertEquals(15, occupant.internalNumber)
    }

    @Test
    fun `un detalle ilegible deja la bahia sin mecanicos y no tumba el tablero`() = runTest {
        env.signIn()
        env.cachePlate()
        env.responses["GET /api/v1/work-bays"] = env.json(listOf(env.bay("bay1", "Elevador 1", "OCCUPIED", "wo1")))
        env.responses["GET /api/v1/work-orders/wo1"] = MockResponse().setBody("""{"id":"wo1","tasks":"ilegible"}""")
        env.db.workOrderDao().replaceBranch("b1", listOf(summaryEntity()))
        repository.refreshBranchBays().getOrThrow()
        val emissions = Channel<List<BayBoardItem>>(Channel.UNLIMITED)
        observeInto(emissions)
        assertEquals("ABC123", emissions.next().single().occupant!!.plate)
        withContext(Dispatchers.Default) {
            withTimeout(5_000) { while (env.requested("GET", "/api/v1/work-orders/wo1").isEmpty()) delay(10) }
        }

        repository.refreshBranchBays().getOrThrow()

        val occupant = emissions.next().single().occupant!!
        assertEquals(emptyList<String>(), occupant.mechanicIds)
        assertNull(occupant.laborElapsed)
    }

    private fun summaryEntity() = env.gson
        .fromJson(env.gson.toJson(env.workOrderSummary()), WorkOrderDto::class.java)
        .toEntity(env.clock.millis())

    /** Salta la emisión de Room que sale antes de que llegue el detalle. */
    private suspend fun Channel<List<BayBoardItem>>.untilLoaded(): List<BayBoardItem> {
        while (true) {
            val board = next()
            if (board.all { it.occupant == null || it.occupant.mechanicIds.isNotEmpty() }) return board
        }
    }

    private suspend fun <T> Channel<T>.next(): T = withContext(Dispatchers.Default) { withTimeout(5_000) { receive() } }
}
