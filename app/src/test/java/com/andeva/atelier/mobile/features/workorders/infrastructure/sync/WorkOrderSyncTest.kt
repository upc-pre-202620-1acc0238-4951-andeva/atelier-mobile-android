package com.andeva.atelier.mobile.features.workorders.infrastructure.sync

import com.andeva.atelier.mobile.core.data.sync.OperationResult
import com.andeva.atelier.mobile.core.data.sync.UploadFileHandler
import com.andeva.atelier.mobile.features.reception.infrastructure.ReceptionTestEnv
import com.andeva.atelier.mobile.features.workorders.domain.ProposalSeverity
import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.WorkOrderService
import com.andeva.atelier.mobile.features.workorders.infrastructure.repository.WorkOrderRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WorkOrderSyncTest {

    private val env = ReceptionTestEnv()
    private lateinit var repository: WorkOrderRepositoryImpl

    @Before
    fun setUp() = runTest {
        env.start()
        env.signIn()
        repository = env.workOrderRepository()
    }

    @After
    fun tearDown() = env.stop()

    private fun photo(): File = kotlin.io.path.createTempFile(suffix = ".jpg").toFile().apply { writeBytes(byteArrayOf(1, 2, 3)) }

    private suspend fun queued() = env.db.pendingOperationDao().observeForUser("u1").first()

    @Test
    fun `una foto de recepcion se encola detras de su subida y aparece pendiente`() = runTest {
        repository.attachIntakePhoto("wo1", photo(), "Raspón puerta izquierda").getOrThrow()

        val operations = queued()
        val upload = operations.single { it.type == UploadFileHandler.TYPE }
        val attach = operations.single { it.type == AttachIntakeImageHandler.TYPE }
        assertEquals(upload.id, attach.dependsOn)
        assertTrue(attach.payload.contains("/t1/work-orders/wo1/intake/"))

        val pending = repository.observePendingIntakePhotos("wo1").first().single()
        assertNotNull(pending.localFilePath)
        assertEquals("Raspón puerta izquierda", pending.description)
        assertTrue(repository.observePendingIntakePhotos("wo2").first().isEmpty())
    }

    @Test
    fun `una propuesta se encola con el mecanico de la sesion`() = runTest {
        repository.submitProposal("wo1", "Fuga en el radiador", ProposalSeverity.CRITICAL, photo(), null).getOrThrow()

        val attach = queued().single { it.type == SubmitProposalHandler.TYPE }
        assertTrue(attach.payload.contains("\"mechanicId\":\"m1\""))
        assertTrue(attach.payload.contains("\"severity\":\"CRITICAL\""))
        assertTrue(attach.payload.contains("/t1/work-orders/wo1/proposals/"))
        assertEquals(ProposalSeverity.CRITICAL, repository.observePendingProposals("wo1").first().single().severity)
    }

    @Test
    fun `los handlers dan por aplicada la respuesta 201 y FAILED un 400`() = runTest {
        repository.attachIntakePhoto("wo1", photo(), null).getOrThrow()
        repository.submitProposal("wo1", "Fuga", ProposalSeverity.LOW, photo(), "srv1").getOrThrow()
        val service = env.service(WorkOrderService::class.java)
        val intake = AttachIntakeImageHandler(service, env.gson)
        val proposal = SubmitProposalHandler(service, env.gson)
        val operations = queued()
        env.responses["POST /api/v1/work-orders/wo1/intake-images"] = env.json(
            mapOf("id" to "img1", "workOrderId" to "wo1", "imageUrl" to "https://x", "uploadedAt" to "2026-10-24T14:00:00Z"), 201
        )
        env.responses["POST /api/v1/work-orders/wo1/proposals"] = env.error(400, "VALIDATION_FAILED", "La URL de la imagen no es válida")

        assertEquals(OperationResult.Applied, intake.send(operations.single { it.type == AttachIntakeImageHandler.TYPE }))
        val failed = proposal.send(operations.single { it.type == SubmitProposalHandler.TYPE })
        assertEquals("La URL de la imagen no es válida", (failed as OperationResult.Failed).message)

        val body = env.requested("POST", "/api/v1/work-orders/wo1/proposals").single().body.readUtf8()
        assertTrue(body.contains("\"suggestedServiceId\":\"srv1\""))
        assertTrue(!body.contains("workOrderId"))
    }
}
