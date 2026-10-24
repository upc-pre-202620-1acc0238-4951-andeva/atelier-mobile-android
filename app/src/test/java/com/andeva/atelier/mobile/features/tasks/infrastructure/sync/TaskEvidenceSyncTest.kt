package com.andeva.atelier.mobile.features.tasks.infrastructure.sync

import com.andeva.atelier.mobile.core.data.sync.OperationResult
import com.andeva.atelier.mobile.core.data.sync.PendingOperationEntity
import com.andeva.atelier.mobile.core.data.sync.UploadFileHandler
import com.andeva.atelier.mobile.features.tasks.infrastructure.TaskTestEnv
import com.andeva.atelier.mobile.features.tasks.infrastructure.remote.AttachTaskEvidencePayload
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TaskEvidenceSyncTest {

    private val env = TaskTestEnv()
    private lateinit var handler: AttachTaskEvidenceHandler

    @Before
    fun setUp() = runTest {
        env.start()
        env.signIn()
        handler = AttachTaskEvidenceHandler(env.taskService, env.gson)
    }

    @After
    fun tearDown() = env.stop()

    private fun operation(payload: AttachTaskEvidencePayload) = PendingOperationEntity(
        id = "op1", type = AttachTaskEvidenceHandler.TYPE, payload = env.gson.toJson(payload), dedupeKey = "TASK_EVIDENCE:t1:f1",
        userId = "u1", tenantId = "t1", nextAttemptAt = 0, createdAt = 0
    )

    private val payload = AttachTaskEvidencePayload("t1", "https://demo.supabase.co/storage/v1/object/public/atelier/x.jpg", "Montaje")

    @Test
    fun `capturar guarda la foto y encola la subida y el registro con depends_on y URL final`() = runTest {
        val photo = kotlin.io.path.createTempFile(suffix = ".jpg").toFile().apply { writeBytes(byteArrayOf(1, 2, 3)) }

        env.capture().capture("t1", "wo1", photo, "Montaje").getOrThrow()

        val operations = env.db.pendingOperationDao().observeForUser("u1").first()
        val upload = operations.single { it.type == UploadFileHandler.TYPE }
        val attach = operations.single { it.type == AttachTaskEvidenceHandler.TYPE }
        assertEquals(upload.id, attach.dependsOn)
        val photoId = attach.dedupeKey.removePrefix("TASK_EVIDENCE:t1:")
        val stored = env.db.pendingUploadDao().findById(photoId)!!
        assertEquals("t1/work-orders/wo1/tasks/t1/$photoId.jpg", stored.storagePath)
        assertEquals("TASK_EVIDENCE", stored.target)
        assertTrue(File(stored.localFilePath).exists())
        val sent = env.gson.fromJson(attach.payload, AttachTaskEvidencePayload::class.java)
        assertEquals("https://demo.supabase.co/storage/v1/object/public/atelier/${stored.storagePath}", sent.imageUrl)
        verify { env.syncScheduler.requestSync() }
    }

    @Test
    fun `registro aceptado se aplica y envia url y descripcion`() = runTest {
        env.responses["POST /api/v1/tasks/t1/evidence-images"] =
            env.json(mapOf("id" to "e1", "taskId" to "t1", "imageUrl" to payload.imageUrl, "description" to "Montaje"), 201)

        assertEquals(OperationResult.Applied, handler.send(operation(payload)))
        val body = env.requested("POST", "/api/v1/tasks/t1/evidence-images").single().body.readUtf8()
        assertEquals("""{"imageUrl":"${payload.imageUrl}","description":"Montaje"}""", body)
    }

    @Test
    fun `url rechazada falla con el message y un 5xx se reintenta`() = runTest {
        env.responses["POST /api/v1/tasks/t1/evidence-images"] = env.error(400, "BAD_REQUEST", "URL de imagen inválida")
        val failed = handler.send(operation(payload)) as OperationResult.Failed
        assertEquals("URL de imagen inválida", failed.message)

        env.responses["POST /api/v1/tasks/t1/evidence-images"] = env.error(503, "INTERNAL_ERROR", "Error")
        assertTrue(handler.send(operation(payload)) is OperationResult.Retry)
    }
}
