package com.andeva.atelier.mobile.features.auth.presentation.forgot

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.features.auth.application.ForgotPasswordUseCase
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

class ForgotPasswordViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = mockk<AuthRepository>()

    private fun viewModel() = ForgotPasswordViewModel(ForgotPasswordUseCase(repository))

    @Test
    fun `solo se envia con un correo valido`() {
        val vm = viewModel()
        assertFalse(vm.uiState.value.canSubmit)
        vm.onEmailChange("andres")
        assertFalse(vm.uiState.value.canSubmit)
        vm.onEmailChange("andres@taller.pe")
        assertTrue(vm.uiState.value.canSubmit)
    }

    @Test
    fun `solicitud correcta muestra la confirmacion`() = runTest(mainRule.dispatcher) {
        coEvery { repository.forgotPassword("andres@taller.pe") } returns Result.success("enviado")
        val vm = viewModel()
        vm.onEmailChange("andres@taller.pe")
        vm.onSubmit()
        mainRule.dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value.sent)
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `400 muestra el mensaje del backend y no confirma`() = runTest(mainRule.dispatcher) {
        coEvery { repository.forgotPassword(any()) } returns
            Result.failure(ApiException(400, "BAD_REQUEST", "El correo no es válido."))
        val vm = viewModel()
        vm.onEmailChange("andres@taller.pe")
        vm.onSubmit()
        mainRule.dispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.uiState.value.sent)
        assertEquals("El correo no es válido.", (vm.uiState.value.error as UiText.Dynamic).value)
        vm.onEmailChange("andres@taller.com")
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `sin red se muestra el aviso de conexion`() = runTest(mainRule.dispatcher) {
        coEvery { repository.forgotPassword(any()) } returns Result.failure(IOException("sin red"))
        val vm = viewModel()
        vm.onEmailChange("andres@taller.pe")
        vm.onSubmit()
        mainRule.dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value.error is UiText.Resource)
        assertFalse(vm.uiState.value.sent)
    }

    @Test
    fun `un segundo toque mientras carga no repite la llamada`() = runTest(mainRule.dispatcher) {
        coEvery { repository.forgotPassword(any()) } returns Result.success("enviado")
        val vm = viewModel()
        vm.onEmailChange("andres@taller.pe")
        vm.onSubmit()
        vm.onSubmit()
        mainRule.dispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 1) { repository.forgotPassword(any()) }
    }

    @Test
    fun `volver a pedir quita la confirmacion y conserva el correo`() = runTest(mainRule.dispatcher) {
        coEvery { repository.forgotPassword(any()) } returns Result.success("enviado")
        val vm = viewModel()
        vm.onEmailChange("andres@taller.pe")
        vm.onSubmit()
        mainRule.dispatcher.scheduler.advanceUntilIdle()
        vm.onRequestAgain()
        assertFalse(vm.uiState.value.sent)
        assertEquals("andres@taller.pe", vm.uiState.value.email)
    }
}
