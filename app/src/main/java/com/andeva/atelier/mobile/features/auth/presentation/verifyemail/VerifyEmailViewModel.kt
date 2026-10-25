package com.andeva.atelier.mobile.features.auth.presentation.verifyemail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.text.toUiText
import com.andeva.atelier.mobile.features.auth.application.ResendVerificationUseCase
import com.andeva.atelier.mobile.features.auth.application.VerifyEmailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VerifyEmailViewModel @Inject constructor(
    private val verifyEmail: VerifyEmailUseCase,
    private val resendVerification: ResendVerificationUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val email: String = savedStateHandle["email"] ?: ""

    private val _uiState = MutableStateFlow(VerifyEmailUiState(email = email))
    val uiState: StateFlow<VerifyEmailUiState> = _uiState.asStateFlow()

    private var ticker: Job? = null

    init {
        startTicker()
    }

    fun onCodeChange(value: String) = _uiState.update { it.copy(code = value, error = null) }

    /** `POST /auth/verify-email { email, token }`. Con éxito la cuenta queda activa y se vuelve al login. */
    fun onVerify() {
        val current = _uiState.value
        if (!current.canSubmit) return
        _uiState.update { it.copy(isVerifying = true, error = null, info = null) }
        viewModelScope.launch {
            verifyEmail(email, current.code)
                .onSuccess { _uiState.update { it.copy(isVerifying = false, verified = true) } }
                .onFailure { e -> _uiState.update { it.copy(isVerifying = false, error = e.toUiText()) } }
        }
    }

    /** `POST /auth/resend-verification`: código nuevo, y vuelven a correr el reenvío (60 s) y la vigencia (15 min). */
    fun onResend() {
        if (!_uiState.value.canResend) return
        _uiState.update { it.copy(isResending = true, error = null, info = null) }
        viewModelScope.launch {
            resendVerification(email)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isResending = false,
                            code = "",
                            info = UiText.Resource(R.string.auth_otp_resent),
                            resendSecondsLeft = VerifyEmailUiState.RESEND_COOLDOWN_SECONDS,
                            expiresSecondsLeft = VerifyEmailUiState.CODE_LIFETIME_SECONDS
                        )
                    }
                    startTicker()
                }
                .onFailure { e -> _uiState.update { it.copy(isResending = false, error = e.toUiText()) } }
        }
    }

    /** Un solo contador de 1 s; se detiene cuando ya no hay nada que descontar. */
    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (true) {
                delay(1_000)
                val next = _uiState.updateAndGet { state ->
                    state.copy(
                        resendSecondsLeft = (state.resendSecondsLeft - 1).coerceAtLeast(0),
                        expiresSecondsLeft = state.expiresSecondsLeft?.let { (it - 1).coerceAtLeast(0) }
                    )
                }
                if (next.resendSecondsLeft == 0 && (next.expiresSecondsLeft ?: 0) == 0) break
            }
        }
    }
}
