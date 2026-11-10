package com.andeva.atelier.mobile.features.auth.presentation.reset

import androidx.lifecycle.SavedStateHandle
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.features.auth.application.ResetPasswordUseCase
import com.andeva.atelier.mobile.features.auth.domain.AuthRepository
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
import java.io.IOException

class ResetPasswordViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = mockk<AuthRepository>()

    private fun viewModel(token: String? = null) = ResetPasswordViewModel(
        ResetPasswordUseCase(repository),
        SavedStateHandle(if (token == null) emptyMap() else mapOf("token" to token))
    )

    private fun ResetPasswordViewModel.fill(token: String = "abc123", password: String = "Taller123") {
        onTokenChange(token)
        onPasswordChange(password)
        onConfirmationChange(password)
    }

    @Test
    fun `el token de la ruta llega precargado`() {
        assertEquals("tok-1", viewModel(token = "tok-1").uiState.value.token)
    }

    @Test
    fun `las reglas de contrasena siguen lo que se escribe`() {
        val vm = viewModel()
        vm.onPasswordChange("taller")
        assertFalse(vm.uiState.value.hasMinLength)
        assertFalse(vm.uiState.value.hasUppercase)
        vm.onPasswordChange("Taller123")
        vm.onConfirmationChange("Taller123")
        assertTrue(vm.uiState.value.hasMinLength)
        assertTrue(vm.uiState.value.hasUppercase)
        assertTrue(vm.uiState.value.passwordsMatch)
    }

    @Test
    fun `no se puede enviar sin token, con contrasenas distintas ni sin mayuscula`() {
        val vm = viewModel()
        vm.fill(token = "")
        assertFalse(vm.uiState.value.canSubmit)
        vm.fill()
        assertTrue(vm.uiState.value.canSubmit)
        vm.onConfirmationChange("Taller124")
        assertFalse(vm.uiState.value.canSubmit)
        vm.fill(password = "taller123")
        assertFalse(vm.uiState.value.canSubmit)
    }

    @Test
    fun `el token pegado se envia y la contrasena nueva queda guardada`() = runTest(mainRule.dispatcher) {
        coEvery { repository.resetPassword("abc123", "Taller123") } returns Result.success("ok")
        val vm = viewModel()
        vm.fill()
        vm.onSubmit()
        mainRule.dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value.done)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `un enlace completo se reduce a su token`() = runTest(mainRule.dispatcher) {
        coEvery { repository.resetPassword("abc123", "Taller123") } returns Result.success("ok")
        val vm = viewModel()
        vm.fill(token = "https://atelier.pe/reset-password?token=abc123")
        vm.onSubmit()
        mainRule.dispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 1) { repository.resetPassword("abc123", "Taller123") }
        assertTrue(vm.uiState.value.done)
    }

    @Test
    fun `400 marca el enlace como vencido o invalido`() = runTest(mainRule.dispatcher) {
        coEvery { repository.resetPassword(any(), any()) } returns
            Result.failure(ApiException(400, "BAD_REQUEST", "Token inválido."))
        val vm = viewModel()
        vm.fill()
        vm.onSubmit()
        mainRule.dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value.tokenInvalid)
        assertFalse(vm.uiState.value.done)
        vm.onTokenChange("otro")
        assertFalse(vm.uiState.value.tokenInvalid)
    }

    @Test
    fun `404 por token inexistente tambien se muestra como enlace invalido`() = runTest(mainRule.dispatcher) {
        coEvery { repository.resetPassword(any(), any()) } returns
            Result.failure(ApiException(404, "NOT_FOUND", "VerificationToken with identifier x was not found"))
        val vm = viewModel()
        vm.fill()
        vm.onSubmit()
        mainRule.dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value.tokenInvalid)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `otro error muestra el mensaje del backend`() = runTest(mainRule.dispatcher) {
        coEvery { repository.resetPassword(any(), any()) } returns
            Result.failure(ApiException(500, "INTERNAL_ERROR", "Error interno."))
        val vm = viewModel()
        vm.fill()
        vm.onSubmit()
        mainRule.dispatcher.scheduler.advanceUntilIdle()
        assertEquals("Error interno.", (vm.uiState.value.error as UiText.Dynamic).value)
        assertFalse(vm.uiState.value.tokenInvalid)
    }

    @Test
    fun `sin red se muestra el aviso de conexion`() = runTest(mainRule.dispatcher) {
        coEvery { repository.resetPassword(any(), any()) } returns Result.failure(IOException("sin red"))
        val vm = viewModel()
        vm.fill()
        vm.onSubmit()
        mainRule.dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value.error is UiText.Resource)
    }
}
