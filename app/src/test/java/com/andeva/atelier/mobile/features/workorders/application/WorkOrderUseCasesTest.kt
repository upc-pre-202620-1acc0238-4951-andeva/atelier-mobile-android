package com.andeva.atelier.mobile.features.workorders.application

import com.andeva.atelier.mobile.features.workorders.domain.InvalidWorkOrderInputException
import com.andeva.atelier.mobile.features.workorders.domain.NewWorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.NewWorkOrderTask
import com.andeva.atelier.mobile.features.workorders.domain.ProposalApproval
import com.andeva.atelier.mobile.features.workorders.domain.ProposalSeverity
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.math.BigDecimal

class WorkOrderUseCasesTest {

    private val repository = mockk<WorkOrderRepository>()

    private fun field(result: Result<*>) = (result.exceptionOrNull() as InvalidWorkOrderInputException).field

    @Test
    fun `datos invalidos no llegan al backend`() = runTest {
        assertEquals("mileageIn", field(OpenWorkOrderUseCase(repository)(NewWorkOrder("v1", "c1", -1))))
        assertEquals("diagnosticSummary", field(UpdateWorkOrderUseCase(repository)("wo1", diagnosticSummary = "x".repeat(2001))))
        assertEquals("description", field(AddWorkOrderTaskUseCase(repository)("wo1", NewWorkOrderTask("srv1", " "))))
        assertEquals("price", field(AddWorkOrderTaskUseCase(repository)("wo1", NewWorkOrderTask("srv1", "Frenos", price = BigDecimal(-1)))))
        assertEquals("laborHours", field(ApproveProposalUseCase(repository)("wo1", "p1", ProposalApproval("srv1", BigDecimal.TEN, BigDecimal(-1)))))
        assertEquals("customerNotes", field(RejectProposalUseCase(repository)("wo1", "p1", "")))
        assertEquals("reason", field(RejectQuotationUseCase(repository)("wo1", " ")))
        assertEquals("description", field(AttachIntakePhotoUseCase(repository)("wo1", File("x.jpg"), "x".repeat(501))))
        coVerify(exactly = 0) { repository.open(any()) }
        coVerify(exactly = 0) { repository.addTask(any(), any()) }
    }

    @Test
    fun `la propuesta exige foto y no acepta HIGH`() = runTest {
        val submit = SubmitProposalUseCase(repository)

        assertEquals("photo", field(submit("wo1", "Fuga en el radiador", ProposalSeverity.CRITICAL, photo = null)))
        assertEquals("severity", field(submit("wo1", "Fuga en el radiador", ProposalSeverity.HIGH, File("x.jpg"))))
        coVerify(exactly = 0) { repository.submitProposal(any(), any(), any(), any(), any()) }

        val photo = File("x.jpg")
        coEvery { repository.submitProposal("wo1", "Fuga en el radiador", ProposalSeverity.MEDIUM, photo, null) } returns Result.success(Unit)
        assertTrue(submit("wo1", "Fuga en el radiador", ProposalSeverity.MEDIUM, photo).isSuccess)
    }
}
