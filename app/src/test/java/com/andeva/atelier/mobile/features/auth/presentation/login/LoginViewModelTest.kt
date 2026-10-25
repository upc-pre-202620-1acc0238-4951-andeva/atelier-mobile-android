package com.andeva.atelier.mobile.features.auth.presentation.login

import androidx.lifecycle.SavedStateHandle
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.features.auth.application.SignInUseCase
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

class LoginViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = mockk<AuthRepository>()

    private fun viewModel(verifiedEmail: String? = null) = LoginViewModel(
        SignInUseCase(repository),
        SavedStateHandle(if (verifiedEmail == null) emptyMap() else mapOf("verifiedEmail" to verifiedEmail))
    )

    @Test
    fun `no se puede enviar sin correo valido o sin contrasena`() {
        val vm = viewModel()
        assertFalse(vm.uiState.value.canSubmit)
        vm.onEmailChange("andres")
        vm.onPasswordChange("x")
        assertFalse(vm.uiState.value.canSubmit)
        vm.onEmailChange("andres@taller.pe")
        assertTrue(vm.uiState.value.canSubmit)
    }

    @Test
    fun `llegar desde la verificacion precarga el correo y muestra el aviso`() {
        val state = viewModel(verifiedEmail = "andres@taller.pe").uiState.value
        assertEquals("andres@taller.pe", state.email)
        assertTrue(state.emailVerified)
    }

    @Test
    fun `inicio correcto libera el boton y no muestra error`() = runTest(mainRule.dispatcher) {
        coEvery { repository.signIn("andres@taller.pe", "Secreta123") } returns Result.success(Unit)
        val vm = viewModel()
        vm.onEmailChange("andres@taller.pe")
        vm.onPasswordChange("Secreta123")
        vm.onSubmit()
        assertTrue(vm.uiState.value.isLoading)
        mainRule.dispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `credenciales malas muestran el mensaje del backend`() = runTest(mainRule.dispatcher) {
        coEvery { repository.signIn(any(), any()) } returns
            Result.failure(ApiException(401, "UNAUTHORIZED", "Correo o contraseña incorrectos. Te quedan 3 intentos."))
        val vm = viewModel()
        vm.onEmailChange("andres@taller.pe")
        vm.onPasswordChange("mala")
        vm.onSubmit()
        mainRule.dispatcher.scheduler.advanceUntilIdle()

        val error = vm.uiState.value.error
        assertTrue(error is UiText.Dynamic)
        assertEquals("Correo o contraseña incorrectos. Te quedan 3 intentos.", (error as UiText.Dynamic).value)
        assertNull(vm.uiState.value.unverifiedEmail)
    }

    @Test
    fun `cuenta sin verificar lleva a la pantalla de codigo en vez de mostrar error`() = runTest(mainRule.dispatcher) {
        coEvery { repository.signIn(any(), any()) } returns
            Result.failure(ApiException(403, "EMAIL_NOT_VERIFIED", "Tu cuenta aún no está verificada."))
        val vm = viewModel()
        vm.onEmailChange(" andres@taller.pe ")
        vm.onPasswordChange("Secreta123")
        vm.onSubmit()
        mainRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals("andres@taller.pe", vm.uiState.value.unverifiedEmail)
        assertNull(vm.uiState.value.error)
        vm.onVerificationHandled()
        assertNull(vm.uiState.value.unverifiedEmail)
    }

    @Test
    fun `sin red se muestra el aviso de conexion`() = runTest(mainRule.dispatcher) {
        coEvery { repository.signIn(any(), any()) } returns Result.failure(IOException("sin red"))
        val vm = viewModel()
        vm.onEmailChange("andres@taller.pe")
        vm.onPasswordChange("Secreta123")
        vm.onSubmit()
        mainRule.dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value.error is UiText.Resource)
    }

    @Test
    fun `escribir limpia el error y un segundo toque mientras carga no repite la llamada`() = runTest(mainRule.dispatcher) {
        coEvery { repository.signIn(any(), any()) } returns Result.success(Unit)
        val vm = viewModel()
        vm.onEmailChange("andres@taller.pe")
        vm.onPasswordChange("Secreta123")
        vm.onSubmit()
        vm.onSubmit()
        mainRule.dispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 1) { repository.signIn(any(), any()) }
    }
}
