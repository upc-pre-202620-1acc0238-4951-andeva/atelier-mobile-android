package com.andeva.atelier.mobile.features.workorders.presentation.proposals

import androidx.lifecycle.SavedStateHandle
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.domain.Session
import com.andeva.atelier.mobile.features.auth.presentation.MainDispatcherRule
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.workorders.application.ApproveProposalUseCase
import com.andeva.atelier.mobile.features.workorders.application.GetServiceCatalogUseCase
import com.andeva.atelier.mobile.features.workorders.application.GetWorkOrderDetailUseCase
import com.andeva.atelier.mobile.features.workorders.application.NotifyProposalUseCase
import com.andeva.atelier.mobile.features.workorders.application.RejectProposalUseCase
import com.andeva.atelier.mobile.features.workorders.domain.Proposal
import com.andeva.atelier.mobile.features.workorders.domain.ProposalApproval
import com.andeva.atelier.mobile.features.workorders.domain.ProposalSeverity
import com.andeva.atelier.mobile.features.workorders.domain.ProposalStatus
import com.andeva.atelier.mobile.features.workorders.domain.ServiceOption
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderDetail
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal

class ProposalsPanelViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = mockk<WorkOrderRepository>()
    private val empaque = ServiceOption("s1", "Cambio de empaque", null, 90)

    private fun propuesta(id: String, status: ProposalStatus = ProposalStatus.PENDING_REVIEW, serviceId: String? = "s1", notes: String? = null) =
        Proposal(id, "wo1", null, serviceId, "m1", "Fuga de aceite", ProposalSeverity.CRITICAL, "https://x.test/$id.jpg", status, notes, null)

    private fun detalle(vararg proposals: Proposal) = mockk<WorkOrderDetail> { every { this@mockk.proposals } returns proposals.toList() }

    private val allPermissions = listOf(Permission.PROPOSALS_NOTIFY, Permission.PROPOSALS_RESOLVE, Permission.PROPOSALS_CREATE)

    private fun sessionWith(permissions: List<Permission>) = Session(
        userId = "u", email = "a@taller.pe", fullName = "Asesor", tenantId = "t", tenantName = "Taller",
        membershipId = "m", permissions = permissions.map { it.value }.toSet(), plan = null
    )

    private fun viewModel(permissions: List<Permission> = allPermissions): ProposalsPanelViewModel {
        val observeSession = mockk<ObserveSessionUseCase>()
        every { observeSession() } returns flowOf(sessionWith(permissions))
        coEvery { repository.getServiceCatalog() } returns Result.success(listOf(empaque))
        return ProposalsPanelViewModel(
            observeSession,
            GetWorkOrderDetailUseCase(repository),
            GetServiceCatalogUseCase(repository),
            NotifyProposalUseCase(repository),
            ApproveProposalUseCase(repository),
            RejectProposalUseCase(repository),
            SavedStateHandle(mapOf("workOrderId" to "wo1"))
        )
    }

    private fun ProposalsPanelViewModel.settle() = mainRule.dispatcher.scheduler.advanceUntilIdle()

    private fun conPropuestas(vararg proposals: Proposal = arrayOf(propuesta("p1"))) =
        coEvery { repository.getDetail("wo1") } returns Result.success(detalle(*proposals))

    @Test
    fun `carga las propuestas de la orden y los servicios`() = runTest(mainRule.dispatcher) {
        conPropuestas(propuesta("p1"), propuesta("p2", ProposalStatus.APPROVED))
        val vm = viewModel()
        vm.settle()

        assertEquals(listOf("p1", "p2"), vm.uiState.value.proposals.map { it.id })
        assertEquals(listOf(empaque), vm.uiState.value.services)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `un error al cargar muestra el mensaje y se puede reintentar`() = runTest(mainRule.dispatcher) {
        coEvery { repository.getDetail("wo1") } returnsMany listOf(
            Result.failure(ApiException(403, "FORBIDDEN", "No puedes ver esta orden.")),
            Result.success(detalle(propuesta("p1")))
        )
        val vm = viewModel()
        vm.settle()
        assertEquals("No puedes ver esta orden.", (vm.uiState.value.error as UiText.Dynamic).value)

        vm.load()
        vm.settle()
        assertNull(vm.uiState.value.error)
        assertEquals(1, vm.uiState.value.proposals.size)
    }

    @Test
    fun `los permisos de la sesion habilitan cada accion`() = runTest(mainRule.dispatcher) {
        conPropuestas()
        val vm = viewModel()
        vm.settle()
        assertTrue(vm.uiState.value.canNotify && vm.uiState.value.canResolve && vm.uiState.value.canReport)

        val solo = viewModel(listOf(Permission.PROPOSALS_NOTIFY))
        solo.settle()
        assertFalse(solo.uiState.value.canResolve)
    }

    @Test
    fun `notificar avisa a que correo se envio`() = runTest(mainRule.dispatcher) {
        conPropuestas()
        coEvery { repository.notifyProposal("wo1", "p1") } returns Result.success("ana@correo.pe")
        val vm = viewModel()
        vm.settle()
        vm.onNotify("p1")
        vm.settle()

        val notice = vm.uiState.value.notice as UiText.Resource
        assertEquals("ana@correo.pe", notice.args.single())
        assertNull(vm.uiState.value.busyProposalId)
    }

    @Test
    fun `un error al notificar muestra el mensaje del backend`() = runTest(mainRule.dispatcher) {
        conPropuestas()
        coEvery { repository.notifyProposal(any(), any()) } returns Result.failure(ApiException(422, "ERR", "El cliente no tiene correo."))
        val vm = viewModel()
        vm.settle()
        vm.onNotify("p1")
        vm.settle()

        assertEquals("El cliente no tiene correo.", (vm.uiState.value.error as UiText.Dynamic).value)
        assertNull(vm.uiState.value.busyProposalId)
    }

    @Test
    fun `sin permiso de notificar no se envia el correo`() = runTest(mainRule.dispatcher) {
        conPropuestas()
        val vm = viewModel(listOf(Permission.PROPOSALS_RESOLVE))
        vm.settle()
        vm.onNotify("p1")
        vm.settle()

        coVerify(exactly = 0) { repository.notifyProposal(any(), any()) }
    }

    @Test
    fun `abrir la aprobacion propone el servicio que sugirio el mecanico`() = runTest(mainRule.dispatcher) {
        conPropuestas()
        val vm = viewModel()
        vm.settle()
        vm.onOpenForm("p1", ProposalAction.APPROVE)

        assertEquals("s1", vm.uiState.value.form?.serviceId)
        assertEquals(ProposalAction.APPROVE, vm.uiState.value.form?.action)
    }

    @Test
    fun `aprobar envia precio y horas con coma o punto y recarga las propuestas`() = runTest(mainRule.dispatcher) {
        conPropuestas()
        val approval = ProposalApproval("s1", BigDecimal("120.50"), BigDecimal("1.5"), null, "ok")
        coEvery { repository.approveProposal("wo1", "p1", approval) } returns Result.success(detalle(propuesta("p1", ProposalStatus.APPROVED)))
        val vm = viewModel()
        vm.settle()
        vm.onOpenForm("p1", ProposalAction.APPROVE)
        vm.onFinalPriceChange("120,50")
        vm.onLaborHoursChange(" 1.5 ")
        vm.onNotesChange(" ok ")
        vm.onConfirm()
        vm.settle()

        coVerify(exactly = 1) { repository.approveProposal("wo1", "p1", approval) }
        assertEquals(ProposalStatus.APPROVED, vm.uiState.value.proposals.single().status)
        assertNull(vm.uiState.value.form)
        assertTrue(vm.uiState.value.notice is UiText.Resource)
    }

    @Test
    fun `aprobar sin servicio marca el campo y no llama al backend`() = runTest(mainRule.dispatcher) {
        conPropuestas(propuesta("p1", serviceId = null))
        val vm = viewModel()
        vm.settle()
        vm.onOpenForm("p1", ProposalAction.APPROVE)
        vm.onFinalPriceChange("100")
        vm.onLaborHoursChange("1")
        vm.onConfirm()
        vm.settle()

        assertEquals("serviceId", vm.uiState.value.invalidField)
        coVerify(exactly = 0) { repository.approveProposal(any(), any(), any()) }

        vm.onServiceChange("s1")
        assertNull(vm.uiState.value.invalidField)
    }

    @Test
    fun `un precio que no es numero marca el campo`() = runTest(mainRule.dispatcher) {
        conPropuestas()
        val vm = viewModel()
        vm.settle()
        vm.onOpenForm("p1", ProposalAction.APPROVE)
        vm.onFinalPriceChange("abc")
        vm.onLaborHoursChange("1")
        vm.onConfirm()
        vm.settle()

        assertEquals("finalPrice", vm.uiState.value.invalidField)
        coVerify(exactly = 0) { repository.approveProposal(any(), any(), any()) }
    }

    @Test
    fun `un precio negativo lo rechaza la validacion y deja el formulario abierto`() = runTest(mainRule.dispatcher) {
        conPropuestas()
        val vm = viewModel()
        vm.settle()
        vm.onOpenForm("p1", ProposalAction.APPROVE)
        vm.onFinalPriceChange("-5")
        vm.onLaborHoursChange("1")
        vm.onConfirm()
        vm.settle()

        assertEquals("finalPrice", vm.uiState.value.invalidField)
        assertTrue(vm.uiState.value.form != null)
        coVerify(exactly = 0) { repository.approveProposal(any(), any(), any()) }
    }

    @Test
    fun `faltan precio u horas y no se puede confirmar`() = runTest(mainRule.dispatcher) {
        conPropuestas()
        val vm = viewModel()
        vm.settle()
        vm.onOpenForm("p1", ProposalAction.APPROVE)
        vm.onFinalPriceChange("100")
        assertFalse(vm.uiState.value.canConfirm)
        vm.onLaborHoursChange("2")
        assertTrue(vm.uiState.value.canConfirm)
    }

    @Test
    fun `rechazar exige las observaciones del cliente y las envia recortadas`() = runTest(mainRule.dispatcher) {
        conPropuestas()
        val rechazada = propuesta("p1", ProposalStatus.REJECTED, notes = "Es muy caro")
        coEvery { repository.rejectProposal("wo1", "p1", "Es muy caro") } returns Result.success(rechazada)
        val vm = viewModel()
        vm.settle()
        vm.onOpenForm("p1", ProposalAction.REJECT)
        assertFalse(vm.uiState.value.canConfirm)

        vm.onNotesChange("  Es muy caro ")
        vm.onConfirm()
        vm.settle()

        coVerify(exactly = 1) { repository.rejectProposal("wo1", "p1", "Es muy caro") }
        assertEquals(ProposalStatus.REJECTED, vm.uiState.value.proposals.single().status)
        assertEquals("Es muy caro", vm.uiState.value.proposals.single().customerNotes)
        assertNull(vm.uiState.value.form)
    }

    @Test
    fun `un error al rechazar deja el formulario abierto con el mensaje`() = runTest(mainRule.dispatcher) {
        conPropuestas()
        coEvery { repository.rejectProposal(any(), any(), any()) } returns Result.failure(IOException("sin red"))
        val vm = viewModel()
        vm.settle()
        vm.onOpenForm("p1", ProposalAction.REJECT)
        vm.onNotesChange("No")
        vm.onConfirm()
        vm.settle()

        assertTrue(vm.uiState.value.error is UiText.Resource)
        assertTrue(vm.uiState.value.form != null)
        assertNull(vm.uiState.value.busyProposalId)
    }

    @Test
    fun `sin permiso de resolver no se abre el formulario`() = runTest(mainRule.dispatcher) {
        conPropuestas()
        val vm = viewModel(listOf(Permission.PROPOSALS_NOTIFY))
        vm.settle()
        vm.onOpenForm("p1", ProposalAction.APPROVE)

        assertNull(vm.uiState.value.form)
    }

    @Test
    fun `cancelar cierra el formulario`() = runTest(mainRule.dispatcher) {
        conPropuestas()
        val vm = viewModel()
        vm.settle()
        vm.onOpenForm("p1", ProposalAction.REJECT)
        vm.onCloseForm()

        assertNull(vm.uiState.value.form)
    }
}
