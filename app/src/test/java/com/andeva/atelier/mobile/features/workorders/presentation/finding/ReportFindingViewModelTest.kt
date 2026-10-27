package com.andeva.atelier.mobile.features.workorders.presentation.finding

import androidx.lifecycle.SavedStateHandle
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.domain.Session
import com.andeva.atelier.mobile.features.auth.presentation.MainDispatcherRule
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.workorders.application.GetServiceCatalogUseCase
import com.andeva.atelier.mobile.features.workorders.application.ObservePendingProposalsUseCase
import com.andeva.atelier.mobile.features.workorders.application.SubmitProposalUseCase
import com.andeva.atelier.mobile.features.workorders.domain.PendingProposal
import com.andeva.atelier.mobile.features.workorders.domain.ProposalSeverity
import com.andeva.atelier.mobile.features.workorders.domain.ServiceOption
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import com.andeva.atelier.mobile.features.workorders.presentation.components.SubmittableSeverities
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.IOException
import java.time.Instant

class ReportFindingViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = mockk<WorkOrderRepository>()
    private val pending = MutableStateFlow<List<PendingProposal>>(emptyList())
    private val foto = File("hallazgo.jpg")
    private val empaque = ServiceOption("s1", "Cambio de empaque", null, 90)

    private fun sessionWith(permissions: List<Permission>) = Session(
        userId = "u", email = "m@taller.pe", fullName = "Mecánico", tenantId = "t", tenantName = "Taller",
        membershipId = "m", permissions = permissions.map { it.value }.toSet(), plan = null
    )

    private fun viewModel(permissions: List<Permission> = listOf(Permission.PROPOSALS_CREATE)): ReportFindingViewModel {
        val observeSession = mockk<ObserveSessionUseCase>()
        every { observeSession() } returns flowOf(sessionWith(permissions))
        every { repository.observePendingProposals("wo1") } returns pending
        coEvery { repository.getServiceCatalog() } returns Result.success(listOf(empaque))
        return ReportFindingViewModel(
            observeSession,
            SubmitProposalUseCase(repository),
            ObservePendingProposalsUseCase(repository),
            GetServiceCatalogUseCase(repository),
            SavedStateHandle(mapOf("workOrderId" to "wo1"))
        )
    }

    private fun ReportFindingViewModel.settle() = mainRule.dispatcher.scheduler.advanceUntilIdle()

    private fun ReportFindingViewModel.fill() {
        onDescriptionChange("  Fuga de aceite en el cárter ")
        onSeverityChange(ProposalSeverity.CRITICAL)
        onPhotoCaptured(foto)
    }

    @Test
    fun `solo se ofrecen las severidades que el backend acepta`() {
        assertEquals(listOf(ProposalSeverity.LOW, ProposalSeverity.MEDIUM, ProposalSeverity.CRITICAL), SubmittableSeverities)
    }

    @Test
    fun `la severidad alta no se puede elegir`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        vm.settle()
        vm.onSeverityChange(ProposalSeverity.HIGH)
        assertNull(vm.uiState.value.severity)
    }

    @Test
    fun `sin foto no se puede enviar`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        vm.settle()
        vm.onDescriptionChange("Fuga")
        vm.onSeverityChange(ProposalSeverity.LOW)
        assertFalse(vm.uiState.value.canSubmit)

        vm.onSubmit()
        vm.settle()
        coVerify(exactly = 0) { repository.submitProposal(any(), any(), any(), any(), any()) }

        vm.onPhotoCaptured(foto)
        assertTrue(vm.uiState.value.canSubmit)
    }

    @Test
    fun `faltan descripcion o severidad y no se puede enviar`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        vm.settle()
        vm.onPhotoCaptured(foto)
        assertFalse(vm.uiState.value.canSubmit)
        vm.onDescriptionChange("Fuga")
        assertFalse(vm.uiState.value.canSubmit)
        vm.onSeverityChange(ProposalSeverity.MEDIUM)
        assertTrue(vm.uiState.value.canSubmit)
    }

    @Test
    fun `envia el hallazgo con la foto la severidad y el servicio sugerido`() = runTest(mainRule.dispatcher) {
        coEvery { repository.submitProposal("wo1", "Fuga de aceite en el cárter", ProposalSeverity.CRITICAL, foto, "s1") } returns Result.success(Unit)
        val vm = viewModel()
        vm.settle()
        vm.fill()
        vm.onServiceChange("s1")
        vm.onSubmit()
        vm.settle()

        coVerify(exactly = 1) { repository.submitProposal("wo1", "Fuga de aceite en el cárter", ProposalSeverity.CRITICAL, foto, "s1") }
        assertFalse(vm.uiState.value.isSaving)
    }

    @Test
    fun `al guardar el hallazgo el formulario queda limpio y avisa`() = runTest(mainRule.dispatcher) {
        coEvery { repository.submitProposal(any(), any(), any(), any(), any()) } returns Result.success(Unit)
        val vm = viewModel()
        vm.settle()
        vm.fill()
        vm.onSubmit()
        vm.settle()

        val state = vm.uiState.value
        assertTrue(state.saved)
        assertEquals("", state.description)
        assertNull(state.severity)
        assertNull(state.photo)
        assertNull(state.suggestedServiceId)
    }

    @Test
    fun `editar el formulario despues de guardar quita el aviso`() = runTest(mainRule.dispatcher) {
        coEvery { repository.submitProposal(any(), any(), any(), any(), any()) } returns Result.success(Unit)
        val vm = viewModel()
        vm.settle()
        vm.fill()
        vm.onSubmit()
        vm.settle()
        vm.onDescriptionChange("Otro")

        assertFalse(vm.uiState.value.saved)
    }

    @Test
    fun `un error del backend muestra el mensaje y conserva lo escrito`() = runTest(mainRule.dispatcher) {
        coEvery { repository.submitProposal(any(), any(), any(), any(), any()) } returns Result.failure(ApiException(400, "ERR", "La foto es obligatoria."))
        val vm = viewModel()
        vm.settle()
        vm.fill()
        vm.onSubmit()
        vm.settle()

        assertEquals("La foto es obligatoria.", (vm.uiState.value.error as UiText.Dynamic).value)
        assertEquals(foto, vm.uiState.value.photo)
        assertFalse(vm.uiState.value.saved)
        assertFalse(vm.uiState.value.isSaving)
    }

    @Test
    fun `una descripcion demasiado larga marca el campo`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        vm.settle()
        vm.onDescriptionChange("x".repeat(2100))
        vm.onSeverityChange(ProposalSeverity.LOW)
        vm.onPhotoCaptured(foto)
        vm.onSubmit()
        vm.settle()

        assertEquals("description", vm.uiState.value.invalidField)
        coVerify(exactly = 0) { repository.submitProposal(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `sin permiso de crear propuestas no se envia nada`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(emptyList())
        vm.settle()
        vm.fill()
        vm.onSubmit()
        vm.settle()

        assertFalse(vm.uiState.value.canSubmit)
        coVerify(exactly = 0) { repository.submitProposal(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `carga los servicios sugeridos y un fallo no impide enviar`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        vm.settle()
        assertEquals(listOf(empaque), vm.uiState.value.services)

        coEvery { repository.getServiceCatalog() } returns Result.failure(IOException("sin red"))
        vm.loadServices()
        vm.settle()
        assertTrue(vm.uiState.value.error is UiText.Resource)
        assertFalse(vm.uiState.value.isLoadingServices)

        vm.fill()
        assertTrue(vm.uiState.value.canSubmit)
    }

    @Test
    fun `las propuestas en cola aparecen como pendientes de envio`() = runTest(mainRule.dispatcher) {
        val enCola = PendingProposal("p1", "/f/p1.jpg", "https://x.test/p1.jpg", "Fuga", ProposalSeverity.LOW, "PENDING", null, Instant.EPOCH)
        pending.value = listOf(enCola)
        val vm = viewModel()
        vm.settle()

        assertEquals(listOf(enCola), vm.uiState.value.pending)
        pending.value = emptyList()
    }

    @Test
    fun `negar la camara avisa al usuario`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        vm.settle()
        vm.onCameraDenied()

        assertTrue(vm.uiState.value.error is UiText.Resource)
    }
}
