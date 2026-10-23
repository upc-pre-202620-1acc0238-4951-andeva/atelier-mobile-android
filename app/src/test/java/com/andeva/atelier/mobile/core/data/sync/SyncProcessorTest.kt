package com.andeva.atelier.mobile.core.data.sync

import com.andeva.atelier.mobile.core.domain.ApiException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** Casos de docs/06-offline-sync.md §8: 2xx, 409 "ya aplicada", 422, 5xx, timeout y límite de intentos. */
class SyncProcessorTest {

    private val now = 1_000_000L
    private val dao = FakePendingOperationDao()
    private val sent = mutableListOf<String>()

    private fun handler(vararg results: OperationResult): OperationHandler {
        val queue = ArrayDeque(results.toList())
        return object : OperationHandler {
            override suspend fun send(operation: PendingOperationEntity): OperationResult {
                sent += operation.id
                return queue.removeFirstOrNull() ?: OperationResult.Applied
            }
        }
    }

    private fun processor(handlers: Map<String, OperationHandler>) =
        SyncProcessor(dao, handlers, "No se pudo sincronizar") { now }

    private suspend fun enqueue(id: String, type: String = "CLOCK_IN", userId: String = "u1", createdAt: Long = now, dependsOn: String? = null, attempts: Int = 0) =
        dao.enqueue(
            PendingOperationEntity(
                id = id, type = type, payload = "{}", dedupeKey = "$type:$id", userId = userId, tenantId = "t1",
                attempts = attempts, nextAttemptAt = createdAt, createdAt = createdAt, dependsOn = dependsOn
            )
        )

    @Test
    fun `2xx borra la operacion`() = runTest {
        enqueue("a")
        processor(mapOf("CLOCK_IN" to handler(OperationResult.Applied))).run("u1")
        assertNull(dao.findById("a"))
    }

    @Test
    fun `409 ya aplicada se considera exito`() = runTest {
        enqueue("a")
        val alreadyApplied = OperationResult.from(Result.failure<Unit>(ApiException(409, "CONFLICT", "Ya marcaste ingreso"))) {
            it.httpStatus == 409
        }
        processor(mapOf("CLOCK_IN" to handler(alreadyApplied))).run("u1")
        assertNull(dao.findById("a"))
    }

    @Test
    fun `422 de regla de negocio queda FAILED con el message del backend`() = runTest {
        enqueue("a")
        val failed = OperationResult.from(Result.failure<Unit>(ApiException(422, "UNPROCESSABLE_ENTITY", "Estás a 230 m de la sede")))
        processor(mapOf("CLOCK_IN" to handler(failed))).run("u1")
        val op = dao.findById("a")!!
        assertEquals(PendingOperationEntity.STATUS_FAILED, op.status)
        assertEquals("Estás a 230 m de la sede", op.lastErrorMessage)
        assertEquals(422, op.lastHttpStatus)
    }

    @Test
    fun `5xx y timeout reintentan con backoff exponencial`() = runTest {
        enqueue("a")
        enqueue("b", type = "TELEMETRY_BATCH")
        val serverError = OperationResult.from(Result.failure<Unit>(ApiException(503, "HTTP_503", "Error 503")))
        val timeout = OperationResult.from(Result.failure<Unit>(IOException("timeout")))
        processor(mapOf("CLOCK_IN" to handler(serverError), "TELEMETRY_BATCH" to handler(timeout))).run("u1")

        for (id in listOf("a", "b")) {
            val op = dao.findById(id)!!
            assertEquals(PendingOperationEntity.STATUS_PENDING, op.status)
            assertEquals(1, op.attempts)
            assertEquals(now + 2 * 60_000L, op.nextAttemptAt)
        }
        assertEquals(60 * 60_000L, SyncProcessor.backoffMillis(7))
    }

    @Test
    fun `al octavo intento queda FAILED`() = runTest {
        enqueue("a", attempts = 7)
        processor(mapOf("CLOCK_IN" to handler(OperationResult.Retry(503)))).run("u1")
        val op = dao.findById("a")!!
        assertEquals(PendingOperationEntity.STATUS_FAILED, op.status)
        assertEquals(8, op.attempts)
        assertEquals("No se pudo sincronizar", op.lastErrorMessage)
    }

    @Test
    fun `espera a la operacion de la que depende y la envia despues`() = runTest {
        enqueue("upload", type = "UPLOAD_FILE", createdAt = now - 10)
        enqueue("attach", type = "ATTACH_TASK_EVIDENCE", dependsOn = "upload")
        processor(
            mapOf("UPLOAD_FILE" to handler(OperationResult.Applied), "ATTACH_TASK_EVIDENCE" to handler(OperationResult.Applied))
        ).run("u1")
        assertEquals(listOf("upload", "attach"), sent)
        assertTrue(dao.all().isEmpty())
    }

    @Test
    fun `no envia la dependiente si la subida sigue pendiente`() = runTest {
        enqueue("upload", type = "UPLOAD_FILE", createdAt = now - 10)
        enqueue("attach", type = "ATTACH_TASK_EVIDENCE", dependsOn = "upload")
        processor(mapOf("UPLOAD_FILE" to handler(OperationResult.Retry()), "ATTACH_TASK_EVIDENCE" to handler())).run("u1")
        assertEquals(listOf("upload"), sent)
        assertEquals(PendingOperationEntity.STATUS_PENDING, dao.findById("attach")!!.status)
    }

    @Test
    fun `FAILED en cascada a las dependientes`() = runTest {
        enqueue("upload", type = "UPLOAD_FILE", createdAt = now - 20)
        enqueue("attach", type = "ATTACH_TASK_EVIDENCE", createdAt = now - 10, dependsOn = "upload")
        enqueue("next", type = "ATTACH_TASK_EVIDENCE", dependsOn = "attach")
        processor(
            mapOf("UPLOAD_FILE" to handler(OperationResult.Failed(null, "FILE_NOT_FOUND", "La foto ya no está en el teléfono")))
        ).run("u1")
        for (id in listOf("upload", "attach", "next")) {
            val op = dao.findById(id)!!
            assertEquals(PendingOperationEntity.STATUS_FAILED, op.status)
            assertEquals("La foto ya no está en el teléfono", op.lastErrorMessage)
        }
    }

    @Test
    fun `401 deja la operacion pendiente y detiene el envio`() = runTest {
        enqueue("a", createdAt = now - 10)
        enqueue("b")
        val outcome = processor(mapOf("CLOCK_IN" to handler(OperationResult.Unauthorized))).run("u1")
        assertEquals(SyncProcessor.Outcome.STOPPED_UNAUTHORIZED, outcome)
        assertEquals(listOf("a"), sent)
        assertEquals(PendingOperationEntity.STATUS_PENDING, dao.findById("a")!!.status)
        assertEquals(0, dao.findById("a")!!.attempts)
    }

    @Test
    fun `solo envia la cola del usuario actual y deja PENDING los tipos sin handler`() = runTest {
        enqueue("mine")
        enqueue("other", userId = "u2")
        enqueue("unknown", type = "NOT_REGISTERED")
        processor(mapOf("CLOCK_IN" to handler())).run("u1")
        assertEquals(listOf("mine"), sent)
        assertFalse(dao.findById("other") == null)
        assertEquals(PendingOperationEntity.STATUS_PENDING, dao.findById("unknown")!!.status)
    }

    @Test
    fun `una excepcion del handler se reintenta`() = runTest {
        enqueue("a")
        val throwing = object : OperationHandler {
            override suspend fun send(operation: PendingOperationEntity): OperationResult = error("boom")
        }
        processor(mapOf("CLOCK_IN" to throwing)).run("u1")
        assertEquals(1, dao.findById("a")!!.attempts)
        assertEquals(PendingOperationEntity.STATUS_PENDING, dao.findById("a")!!.status)
    }
}
