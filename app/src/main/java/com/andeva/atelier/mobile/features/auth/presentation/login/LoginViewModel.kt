package com.andeva.atelier.mobile.features.auth.presentation.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.core.designsystem.text.toUiText
import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.features.auth.application.SignInUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val signIn: SignInUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** Argumento `verifiedEmail` de `LoginRoute`: correo recién verificado en A4. */
    private val verifiedEmail: String? = savedStateHandle["verifiedEmail"]

    private val _uiState = MutableStateFlow(
        LoginUiState(email = verifiedEmail.orEmpty(), emailVerified = verifiedEmail != null)
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, error = null) }

    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, error = null) }

    fun onPasswordVisibleChange(visible: Boolean) = _uiState.update { it.copy(passwordVisible = visible) }

    fun onSubmit() {
        val current = _uiState.value
        if (!current.canSubmit) return
        _uiState.update { it.copy(isLoading = true, error = null, emailVerified = false) }
        viewModelScope.launch {
            signIn(current.email, current.password)
                // Con la sesión guardada AppNavHost cambia de grafo: aquí solo se libera el botón.
                .onSuccess { _uiState.update { it.copy(isLoading = false) } }
                .onFailure { e ->
                    _uiState.update {
                        if (e.isUnverifiedAccount()) {
                            it.copy(isLoading = false, unverifiedEmail = current.email.trim())
                        } else {
                            it.copy(isLoading = false, error = e.toUiText())
                        }
                    }
                }
        }
    }

    fun onVerificationHandled() = _uiState.update { it.copy(unverifiedEmail = null) }

    /** El backend marca la cuenta sin verificar con `code = EMAIL_NOT_VERIFIED` (403): de ahí se pasa a la pantalla OTP. */
    private fun Throwable.isUnverifiedAccount() = this is ApiException && code == EMAIL_NOT_VERIFIED

    private companion object {
        const val EMAIL_NOT_VERIFIED = "EMAIL_NOT_VERIFIED"
    }
}
