package com.andeva.atelier.mobile.features.auth.presentation.register

import androidx.lifecycle.SavedStateHandle
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.features.auth.application.AcceptInvitationUseCase
import com.andeva.atelier.mobile.features.auth.application.ValidateInvitationUseCase
import com.andeva.atelier.mobile.features.auth.domain.AcceptInvitation
import com.andeva.atelier.mobile.features.auth.domain.AuthRepository
import com.andeva.atelier.mobile.features.auth.domain.InvalidInvitationException
import com.andeva.atelier.mobile.features.auth.domain.InvitationPreview
import com.andeva.atelier.mobile.features.auth.presentation.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RegisterViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = mockk<AuthRepository>()
    private val preview = InvitationPreview("andres@taller.pe", "Taller Sede San Isidro", "Mecánico")
    private val link = "https://atelier.andeva.tech/auth/accept-invitation?token=abc123"

    private fun viewModel(token: String? = null) = RegisterViewModel(
        ValidateInvitationUseCase(repository),
        AcceptInvitationUseCase(repository),
        SavedStateHandle(if (token == null) emptyMap() else mapOf("token" to token))
    )

    private fun RegisterViewModel.validate(text: String = link) {
        onLinkChange(text)
        onValidateLink()
        mainRule.dispatcher.scheduler.advanceUntilIdle()
    }

    private fun RegisterViewModel.fillForm(password: String = "Taller123", confirm: String = password) {
        onFirstNameChange("Andrés")
        onLastNameChange("Vílchez Ramos")
        onPasswordChange(password)
        onConfirmPasswordChange(confirm)
    }

    @Test
    fun `enlace valido pasa al formulario con taller y rol`() = runTest(mainRule.dispatcher) {
        coEvery { repository.validateInvitation("abc123") } returns Result.success(preview)
        val vm = viewModel()
        vm.validate()
        assertEquals(RegisterStep.Form, vm.uiState.value.step)
        assertEquals(preview, vm.uiState.value.invitation)
    }

    @Test
    fun `invitacion vencida muestra la vista de vencida`() = runTest(mainRule.dispatcher) {
        coEvery { repository.validateInvitation(any()) } returns Result.failure(InvalidInvitationException())
        val vm = viewModel()
        vm.validate()
        assertEquals(RegisterStep.Gone, vm.uiState.value.step)
        assertEquals(InvitationProblem.Expired, vm.uiState.value.problem)
    }

    @Test
    fun `410 es vencida y 404 es inexistente o revocada`() = runTest(mainRule.dispatcher) {
        coEvery { repository.validateInvitation("a") } returns Result.failure(ApiException(410, "GONE", "vencida"))
        coEvery { repository.validateInvitation("b") } returns Result.failure(ApiException(404, "NOT_FOUND", "no existe"))
        val expired = viewModel().also { it.validate("a") }
        val missing = viewModel().also { it.validate("b") }
        assertEquals(InvitationProblem.Expired, expired.uiState.value.problem)
        assertEquals(InvitationProblem.NotFound, missing.uiState.value.problem)
    }

    @Test
    fun `un error de servidor deja el enlace para reintentar`() = runTest(mainRule.dispatcher) {
        coEvery { repository.validateInvitation(any()) } returns Result.failure(ApiException(500, "ERR", "Error del servidor"))
        val vm = viewModel()
        vm.validate()
        assertEquals(RegisterStep.Link, vm.uiState.value.step)
        assertEquals("Error del servidor", (vm.uiState.value.error as UiText.Dynamic).value)
    }

    @Test
    fun `el token de la ruta se valida solo`() = runTest(mainRule.dispatcher) {
        coEvery { repository.validateInvitation("tok-1") } returns Result.success(preview)
        val vm = viewModel(token = "tok-1")
        mainRule.dispatcher.scheduler.advanceUntilIdle()
        assertEquals(RegisterStep.Form, vm.uiState.value.step)
    }

    @Test
    fun `las reglas de contrasena siguen lo que se escribe`() {
        val vm = viewModel()
        vm.onPasswordChange("taller")
        assertFalse(vm.uiState.value.hasMinLength)
        assertFalse(vm.uiState.value.hasUppercase)
        vm.onPasswordChange("Taller123")
        assertTrue(vm.uiState.value.hasMinLength)
        assertTrue(vm.uiState.value.hasUppercase)
        assertFalse(vm.uiState.value.passwordsMatch)
        vm.onConfirmPasswordChange("Taller123")
        assertTrue(vm.uiState.value.passwordsMatch)
    }

    @Test
    fun `registro completo acepta la invitacion con el token del enlace`() = runTest(mainRule.dispatcher) {
        coEvery { repository.validateInvitation("abc123") } returns Result.success(preview)
        coEvery { repository.acceptInvitation(any()) } returns Result.success(Unit)
        val vm = viewModel()
        vm.validate()
        vm.fillForm()
        assertTrue(vm.uiState.value.canSubmit)
        vm.onSubmit()
        mainRule.dispatcher.scheduler.advanceUntilIdle()

        coVerify {
            repository.acceptInvitation(AcceptInvitation("abc123", "Andrés", "Vílchez Ramos", "Taller123", null))
        }
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `no se puede enviar con contrasenas distintas ni sin mayuscula`() = runTest(mainRule.dispatcher) {
        coEvery { repository.validateInvitation(any()) } returns Result.success(preview)
        val vm = viewModel()
        vm.validate()
        vm.fillForm(password = "Taller123", confirm = "Taller124")
        assertFalse(vm.uiState.value.canSubmit)
        vm.fillForm(password = "taller123")
        assertFalse(vm.uiState.value.canSubmit)
    }

    @Test
    fun `409 al aceptar muestra el mensaje y conserva el formulario`() = runTest(mainRule.dispatcher) {
        coEvery { repository.validateInvitation(any()) } returns Result.success(preview)
        coEvery { repository.acceptInvitation(any()) } returns
            Result.failure(ApiException(409, "CONFLICT", "Ya tienes una membresía activa en otro taller."))
        val vm = viewModel()
        vm.validate()
        vm.fillForm()
        vm.onSubmit()
        mainRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(RegisterStep.Form, vm.uiState.value.step)
        assertEquals("Ya tienes una membresía activa en otro taller.", (vm.uiState.value.error as UiText.Dynamic).value)
        assertEquals("Andrés", vm.uiState.value.firstName)
    }

    @Test
    fun `410 al aceptar lleva a la vista de vencida`() = runTest(mainRule.dispatcher) {
        coEvery { repository.validateInvitation(any()) } returns Result.success(preview)
        coEvery { repository.acceptInvitation(any()) } returns Result.failure(ApiException(410, "GONE", "vencida"))
        val vm = viewModel()
        vm.validate()
        vm.fillForm()
        vm.onSubmit()
        mainRule.dispatcher.scheduler.advanceUntilIdle()
        assertEquals(RegisterStep.Gone, vm.uiState.value.step)
        assertNull(vm.uiState.value.error)
    }
}
