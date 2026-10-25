package com.andeva.atelier.mobile.features.auth.presentation.verifyemail

import androidx.lifecycle.SavedStateHandle
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.features.auth.application.ResendVerificationUseCase
import com.andeva.atelier.mobile.features.auth.application.VerifyEmailUseCase
import com.andeva.atelier.mobile.features.auth.domain.AuthRepository
import com.andeva.atelier.mobile.features.auth.presentation.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VerifyEmailViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = mockk<AuthRepository>()

    private fun viewModel() = VerifyEmailViewModel(
        VerifyEmailUseCase(repository),
        ResendVerificationUseCase(repository),
        SavedStateHandle(mapOf("email" to "andres@taller.pe"))
    )

    @Test
    fun `el correo se muestra enmascarado`() {
        assertEquals("a•••••@taller.pe", viewModel().uiState.value.maskedEmail)
    }

    @Test
    fun `solo se puede verificar con los 6 digitos`() {
        val vm = viewModel()
        vm.onCodeChange("482")
        assertFalse(vm.uiState.value.canSubmit)
        vm.onCodeChange("482170")
        assertTrue(vm.uiState.value.canSubmit)
    }

    @Test
    fun `codigo correcto marca el correo como verificado`() = runTest(mainRule.dispatcher) {
        coEvery { repository.verifyEmail("andres@taller.pe", "482170") } returns Result.success("ok")
        val vm = viewModel()
        vm.onCodeChange("482170")
        vm.onVerify()
        mainRule.dispatcher.scheduler.runCurrent()
        assertTrue(vm.uiState.value.verified)
    }

    @Test
    fun `codigo incorrecto muestra el error y se corrige al escribir`() = runTest(mainRule.dispatcher) {
        coEvery { repository.verifyEmail(any(), any()) } returns
            Result.failure(ApiException(400, "BAD_REQUEST", "El código es incorrecto o venció."))
        val vm = viewModel()
        vm.onCodeChange("482170")
        vm.onVerify()
        mainRule.dispatcher.scheduler.runCurrent()

        assertFalse(vm.uiState.value.verified)
        assertEquals("El código es incorrecto o venció.", (vm.uiState.value.error as UiText.Dynamic).value)
        vm.onCodeChange("48217")
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `el reenvio se habilita a los 60 segundos y reinicia los contadores`() = runTest(mainRule.dispatcher) {
        coEvery { repository.resendVerification("andres@taller.pe") } returns Result.success("enviado")
        val vm = viewModel()
        assertFalse(vm.uiState.value.canResend)
        assertNull(vm.uiState.value.expiresSecondsLeft)

        mainRule.dispatcher.scheduler.advanceTimeBy(60_000)
        mainRule.dispatcher.scheduler.runCurrent()
        assertEquals(0, vm.uiState.value.resendSecondsLeft)
        assertTrue(vm.uiState.value.canResend)

        vm.onResend()
        mainRule.dispatcher.scheduler.runCurrent()
        coVerify(exactly = 1) { repository.resendVerification("andres@taller.pe") }
        assertEquals(VerifyEmailUiState.RESEND_COOLDOWN_SECONDS, vm.uiState.value.resendSecondsLeft)
        assertEquals(VerifyEmailUiState.CODE_LIFETIME_SECONDS, vm.uiState.value.expiresSecondsLeft)
        assertFalse(vm.uiState.value.canResend)
        assertTrue(vm.uiState.value.info is UiText.Resource)
    }

    @Test
    fun `no se puede reenviar antes de tiempo`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        vm.onResend()
        mainRule.dispatcher.scheduler.runCurrent()
        coVerify(exactly = 0) { repository.resendVerification(any()) }
    }
}
