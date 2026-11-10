package com.andeva.atelier.mobile.features.auth.presentation.reset

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.core.designsystem.text.toUiText
import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.features.auth.application.ResetPasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val resetPassword: ResetPasswordUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** Argumento `token` de `ResetPasswordRoute`: llega precargado si hubiera deep link (H1). */
    private val routeToken: String? = savedStateHandle["token"]

    private val _uiState = MutableStateFlow(ResetPasswordUiState(token = routeToken.orEmpty()))
    val uiState: StateFlow<ResetPasswordUiState> = _uiState.asStateFlow()

    fun onTokenChange(value: String) = _uiState.update { it.copy(token = value, error = null, tokenInvalid = false) }

    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, error = null) }

    fun onConfirmationChange(value: String) = _uiState.update { it.copy(confirmation = value, error = null) }

    fun onPasswordVisibleChange(visible: Boolean) = _uiState.update { it.copy(passwordVisible = visible) }

    fun onConfirmationVisibleChange(visible: Boolean) = _uiState.update { it.copy(confirmationVisible = visible) }

    /**
     * `POST /auth/reset-password { token, newPassword }`. El token o el enlace pegado se pasa tal cual:
     * `ResetPasswordUseCase` extrae el `token` de la URL.
     */
    fun onSubmit() {
        val current = _uiState.value
        if (!current.canSubmit) return
        _uiState.update { it.copy(isLoading = true, error = null, tokenInvalid = false) }
        viewModelScope.launch {
            resetPassword(current.token, current.password)
                .onSuccess { _uiState.update { it.copy(isLoading = false, done = true) } }
                .onFailure { e ->
                    _uiState.update {
                        if (e is ApiException && e.httpStatus in INVALID_TOKEN_STATUSES) {
                            it.copy(isLoading = false, tokenInvalid = true)
                        } else {
                            it.copy(isLoading = false, error = e.toUiText())
                        }
                    }
                }
        }
    }

    private companion object {
        /** `iam.md` documenta 400, pero el backend real responde 404 («VerificationToken … was not found») si el token no existe. */
        val INVALID_TOKEN_STATUSES = setOf(400, 404)
    }
}
