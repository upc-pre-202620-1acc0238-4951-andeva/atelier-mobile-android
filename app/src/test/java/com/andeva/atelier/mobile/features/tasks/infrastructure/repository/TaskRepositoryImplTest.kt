package com.andeva.atelier.mobile.features.tasks.infrastructure.repository

import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.tasks.domain.PauseReason
import com.andeva.atelier.mobile.features.tasks.domain.TaskId
import com.andeva.atelier.mobile.features.tasks.domain.TaskStatus
import com.andeva.atelier.mobile.features.tasks.infrastructure.TaskTestEnv
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TaskRepositoryImplTest {

    private val env = TaskTestEnv()
    private lateinit var repository: TaskRepositoryImpl
    private val id = TaskId("t1")

    @Before
    fun setUp() {
        env.start()
        repository = env.repository()
    }

    @After
    fun tearDown() = env.stop()

    private fun stubMyTasks(vararg tasks: Map<String, Any?>) {
        env.responses["GET /api/v1/tasks/my"] = env.json(tasks.toList())
    }

    private fun stubRefs() {
        env.responses["GET /api/v1/work-bays"] = env.json(listOf(mapOf("id" to "bay1", "branchId" to "b1", "name" to "Elevador 1", "bayType" to "LIFT", "status" to "OCCUPIED")))
        env.responses["GET /api/v1/vehicles/v1"] = env.json(mapOf("id" to "v1", "plate" to "ABC-123", "brand" to "Toyota", "model" to "Yaris"))
    }

    @Test
    fun `refrescar guarda mis tareas con placa y bahia sin pedir el detalle de cada una`() = runTest {
        env.signIn()
        stubMyTasks(env.assigned())
        stubRefs()

        repository.refreshMyTasks().getOrThrow()

        val task = repository.observeMyTasks().first().single()
        assertEquals("ABC-123", task.plate)
        assertEquals("Elevador 1", task.bayName)
        assertEquals(BigDecimal("1.5"), task.estimatedHours)
        assertTrue(env.requested("GET", "/api/v1/tasks/t1").isEmpty())
        assertEquals("b1", env.requested("GET", "/api/v1/work-bays").single().requestUrl!!.queryParameter("branchId"))
    }

    @Test
    fun `sin permiso de vehiculos ni bahias no los consulta`() = runTest {
        env.signIn(listOf(Permission.TASKS_READ))
        stubMyTasks(env.assigned())
        stubRefs()

        repository.refreshMyTasks().getOrThrow()

        assertNull(repository.observeMyTasks().first().single().plate)
        assertTrue(env.requested("GET", "/api/v1/vehicles/v1").isEmpty())
        assertTrue(env.requested("GET", "/api/v1/work-bays").isEmpty())
    }

    @Test
    fun `el vehiculo vigente no se vuelve a pedir`() = runTest {
        env.signIn()
        stubMyTasks(env.assigned())
        stubRefs()

        repository.refreshMyTasks().getOrThrow()
        repository.refreshMyTasks().getOrThrow()

        assertEquals(1, env.requested("GET", "/api/v1/vehicles/v1").size)
    }

    @Test
    fun `sin red falla pero conserva la cache`() = runTest {
        env.signIn()
        stubMyTasks(env.assigned())
        repository.refreshMyTasks().getOrThrow()
        env.responses["GET /api/v1/tasks/my"] = env.disconnect()

        val error = repository.refreshMyTasks().exceptionOrNull()

        assertTrue(error is IOException)
        assertEquals(1, repository.observeMyTasks().first().size)
    }

    @Test
    fun `fallar al leer placa o bahia no falla el refresco`() = runTest {
        env.signIn()
        stubMyTasks(env.assigned())
        env.responses["GET /api/v1/vehicles/v1"] = env.error(500, "INTERNAL_ERROR", "Error")

        assertTrue(repository.refreshMyTasks().isSuccess)
        assertNull(repository.observeMyTasks().first().single().plate)
    }

    @Test
    fun `el detalle de una tarea propia se fusiona sin perder vehiculo ni bahia`() = runTest {
        env.signIn()
        stubMyTasks(env.assigned())
        repository.refreshMyTasks().getOrThrow()
        env.responses["GET /api/v1/tasks/t1"] = env.json(env.detail(products = listOf(env.product())))

        val detail = repository.getDetail(id).getOrThrow()

        assertEquals("Frenos", detail.serviceName)
        assertEquals(BigDecimal("36.0"), detail.products.single().totalAmount!!.amount)
        val cached = repository.observeTask(id).first()!!
        assertEquals(TaskStatus.IN_PROGRESS, cached.status)
        assertEquals("Frenos", cached.serviceName)
        assertEquals("v1", cached.vehicleId)
        assertEquals("bay1", cached.bayId)
        assertEquals(101, cached.workOrderNumber)
    }

    @Test
    fun `una tarea ajena abierta por un supervisor no entra a mis tareas`() = runTest {
        env.signIn()
        env.responses["GET /api/v1/tasks/t9"] = env.json(env.detail(id = "t9", pendingSuspensionReason = "Pieza rota"))

        val detail = repository.getDetail(TaskId("t9")).getOrThrow()

        assertTrue(detail.hasPendingSuspension)
        assertTrue(repository.observeMyTasks().first().isEmpty())
    }

    @Test
    fun `pausar envia el motivo y deja la pausa en cache`() = runTest {
        env.signIn()
        stubMyTasks(env.assigned(status = "IN_PROGRESS"))
        repository.refreshMyTasks().getOrThrow()
        env.responses["POST /api/v1/tasks/t1/pause"] = env.json(env.detail(status = "ON_HOLD", holdReason = "BREAK"))

        repository.pause(id, PauseReason.BREAK).getOrThrow()

        assertEquals("""{"reason":"BREAK"}""", env.requested("POST", "/api/v1/tasks/t1/pause").single().body.readUtf8())
        assertTrue(repository.observeTask(id).first()!!.isPausedByMechanic)
    }

    @Test
    fun `pausa anticipada vuelve con el message del backend y la tarea sigue en curso`() = runTest {
        env.signIn()
        stubMyTasks(env.assigned(status = "IN_PROGRESS"))
        repository.refreshMyTasks().getOrThrow()
        env.responses["POST /api/v1/tasks/t1/pause"] = env.error(422, "UNPROCESSABLE_ENTITY", "Tu turno termina a las 17:00")

        val error = repository.pause(id, PauseReason.END_OF_SHIFT).exceptionOrNull() as ApiException

        assertEquals(422, error.httpStatus)
        assertEquals("Tu turno termina a las 17:00", error.message)
        assertEquals(TaskStatus.IN_PROGRESS, repository.observeTask(id).first()!!.status)
    }

    @Test
    fun `cerrar la tarea la saca de mis tareas`() = runTest {
        env.signIn()
        stubMyTasks(env.assigned(status = "IN_PROGRESS"))
        repository.refreshMyTasks().getOrThrow()
        env.responses["POST /api/v1/tasks/t1/complete"] = env.json(env.detail(status = "COMPLETED"))

        repository.complete(id, BigDecimal("1.75"), "  ").getOrThrow()

        assertEquals("""{"actualLaborHours":1.75}""", env.requested("POST", "/api/v1/tasks/t1/complete").single().body.readUtf8())
        assertNull(repository.observeTask(id).first())
    }

    @Test
    fun `sin red una accion falla con IOException y no se encola`() = runTest {
        env.signIn()
        env.responses["POST /api/v1/tasks/t1/start"] = env.disconnect()

        assertTrue(repository.start(id).exceptionOrNull() is IOException)
        assertTrue(env.db.pendingOperationDao().observeForUser("u1").first().isEmpty())
    }

    @Test
    fun `imputar no envia precio para usar el de catalogo y el stock insuficiente trae el message`() = runTest {
        env.signIn()
        env.responses["POST /api/v1/tasks/t1/products"] = env.json(env.product(), 201)

        val product = repository.addPart(id, "p1", BigDecimal(3)).getOrThrow()

        assertEquals(BigDecimal("12.0"), product.unitPrice!!.amount)
        val body = env.requested("POST", "/api/v1/tasks/t1/products").single().body.readUtf8()
        assertEquals("""{"productId":"p1","quantity":3}""", body)

        env.responses["POST /api/v1/tasks/t1/products"] = env.error(409, "CONFLICT", "Insufficient stock for SKU: requested 3, available 1")
        assertEquals(409, (repository.addPart(id, "p1", BigDecimal(3)).exceptionOrNull() as ApiException).httpStatus)
    }

    @Test
    fun `retirar un repuesto acepta 204 sin cuerpo`() = runTest {
        env.signIn()
        env.responses["DELETE /api/v1/tasks/t1/products/p1"] = env.noContent()

        assertTrue(repository.removePart(id, "p1").isSuccess)
    }

    @Test
    fun `buscar repuestos manda el texto y marca los que no tienen saldo`() = runTest {
        env.signIn()
        env.responses["GET /api/v1/inventory/items"] = env.json(
            listOf(
                mapOf("id" to "p1", "name" to "Bujía", "basePrice" to 12.0, "totalStock" to 0),
                mapOf("id" to "p2", "name" to "Bujía iridio", "basePrice" to 30.0, "totalStock" to 8)
            )
        )

        val parts = repository.searchParts(" bujia ").getOrThrow()

        assertEquals("bujia", env.requested("GET", "/api/v1/inventory/items").single().requestUrl!!.queryParameter("search"))
        assertFalse(parts[0].isAvailable)
        assertTrue(parts[1].isAvailable)
    }

    @Test
    fun `una foto capturada aparece pendiente con su archivo local`() = runTest {
        env.signIn()
        val photo = kotlin.io.path.createTempFile(suffix = ".jpg").toFile().apply { writeBytes(byteArrayOf(1, 2, 3)) }

        repository.attachEvidence(id, "wo1", photo, "Desmontaje").getOrThrow()

        val pending = repository.observePendingEvidence(id).first().single()
        assertNotNull(pending.localFilePath)
        assertTrue(pending.imageUrl.contains("/t1/work-orders/wo1/tasks/t1/"))
        assertEquals("Desmontaje", pending.description)
        assertTrue(repository.observePendingEvidence(TaskId("t2")).first().isEmpty())
    }
}
