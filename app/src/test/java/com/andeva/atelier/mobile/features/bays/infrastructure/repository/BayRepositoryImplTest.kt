package com.andeva.atelier.mobile.features.bays.infrastructure.repository

import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.core.domain.MissingBranchException
import com.andeva.atelier.mobile.features.bays.domain.BayStatus
import com.andeva.atelier.mobile.features.bays.infrastructure.BayTestEnv
import com.andeva.atelier.mobile.features.tasks.infrastructure.repository.TaskRepositoryImpl
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
import com.andeva.atelier.mobile.features.bays.domain.BayBoardItem
import com.andeva.atelier.mobile.features.workorders.infrastructure.local.toEntity
import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.WorkOrderDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BayRepositoryImplTest {

    private val env = BayTestEnv()
    private lateinit var repository: BayRepositoryImpl

    @Before
    fun setUp() {
        env.start()
        repository = env.repository()
    }

    @After
    fun tearDown() = env.stop()

    @Test
    fun `refresca las bahias de la sede de la sesion`() = runTest {
        env.signIn()
        env.responses["GET /api/v1/work-bays"] = env.json(listOf(env.bay("bay2", "Foso 2"), env.bay("bay1", "Elevador 1")))

        repository.refreshBranchBays().getOrThrow()

        assertEquals("b1", env.requested("GET", "/api/v1/work-bays").single().requestUrl!!.queryParameter("branchId"))
        assertEquals(listOf("Elevador 1", "Foso 2"), repository.observeBoard().first().map { it.bay.name })
        val metadata = env.db.cacheMetadataDao()
        assertEquals(2, metadata.get(TaskRepositoryImpl.bayScope("b1"))!!.recordCount)
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

        val board = repository.observeBoard().first()

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
        backgroundScope.launch(Dispatchers.Default) { repository.observeBoard().collect { emissions.send(it) } }

        val first = emissions.next().single()
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

    private fun summaryEntity() = env.gson
        .fromJson(env.gson.toJson(env.workOrderSummary()), WorkOrderDto::class.java)
        .toEntity(env.clock.millis())

    private suspend fun <T> Channel<T>.next(): T = withContext(Dispatchers.Default) { withTimeout(5_000) { receive() } }
}
