package com.andeva.atelier.mobile.features.auth.presentation.register

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.core.designsystem.text.toUiText
import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.features.auth.application.AcceptInvitationUseCase
import com.andeva.atelier.mobile.features.auth.application.ValidateInvitationUseCase
import com.andeva.atelier.mobile.features.auth.domain.AcceptInvitation
import com.andeva.atelier.mobile.features.auth.domain.AuthValidation
import com.andeva.atelier.mobile.features.auth.domain.InvalidInvitationException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val validateInvitation: ValidateInvitationUseCase,
    private val acceptInvitation: AcceptInvitationUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    /** Token ya extraído del enlace; se envía tal cual al aceptar. */
    private var token: String = ""

    init {
        // Argumento `token` de `RegisterRoute`: lo usará un deep link cuando exista (H1 de docs/03).
        val routeToken: String? = savedStateHandle["token"]
        if (!routeToken.isNullOrBlank()) {
            _uiState.update { it.copy(linkInput = routeToken) }
            onValidateLink()
        }
    }

    fun onLinkChange(value: String) = _uiState.update { it.copy(linkInput = value, error = null) }

    fun onFirstNameChange(value: String) = _uiState.update { it.copy(firstName = value, error = null) }

    fun onLastNameChange(value: String) = _uiState.update { it.copy(lastName = value, error = null) }

    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, error = null) }

    fun onConfirmPasswordChange(value: String) = _uiState.update { it.copy(confirmPassword = value, error = null) }

    fun onPasswordVisibleChange(visible: Boolean) = _uiState.update { it.copy(passwordVisible = visible) }

    fun onConfirmVisibleChange(visible: Boolean) = _uiState.update { it.copy(confirmVisible = visible) }

    /** `GET /invitations/validate`: si es válida pasa al formulario; si venció o no existe, a la vista de aviso. */
    fun onValidateLink() {
        val current = _uiState.value
        if (!current.canValidate) return
        val extracted = AuthValidation.tokenFrom(current.linkInput)
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            validateInvitation(extracted)
                .onSuccess { preview ->
                    token = extracted
                    _uiState.update {
                        it.copy(isLoading = false, step = RegisterStep.Form, invitation = preview, problem = null)
                    }
                }
                .onFailure { e -> _uiState.update { it.failedWith(e) } }
        }
    }

    /** `POST /invitations/accept`: el backend devuelve la sesión y `AppNavHost` entra a la app. */
    fun onSubmit() {
        val current = _uiState.value
        if (!current.canSubmit) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            acceptInvitation(
                AcceptInvitation(
                    token = token,
                    firstName = current.firstName,
                    lastName = current.lastName,
                    password = current.password,
                    phone = null
                )
            )
                .onSuccess { _uiState.update { it.copy(isLoading = false) } }
                .onFailure { e -> _uiState.update { it.failedWith(e) } }
        }
    }

    /** 410 y 404 cierran el flujo con el aviso; el resto (409, red, 400) se muestra y se puede reintentar. */
    private fun RegisterUiState.failedWith(e: Throwable): RegisterUiState {
        val problem = when {
            e is InvalidInvitationException -> InvitationProblem.Expired
            e is ApiException && e.httpStatus == 410 -> InvitationProblem.Expired
            e is ApiException && e.httpStatus == 404 -> InvitationProblem.NotFound
            else -> null
        }
        return if (problem != null) {
            copy(isLoading = false, step = RegisterStep.Gone, problem = problem, error = null)
        } else {
            copy(isLoading = false, error = e.toUiText())
        }
    }
}
