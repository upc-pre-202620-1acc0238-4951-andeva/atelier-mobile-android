package com.andeva.atelier.mobile.features.main.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.features.auth.application.SignOutUseCase
import com.andeva.atelier.mobile.features.main.application.ObservePendingOperationCountUseCase
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    observePendingCount: ObservePendingOperationCountUseCase,
    private val signOut: SignOutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                if (session == null) return@collect
                _uiState.update {
                    it.copy(fullName = session.fullName, email = session.email, tenantName = session.tenantName)
                }
            }
        }
        viewModelScope.launch {
            observePendingCount().collect { count -> _uiState.update { it.copy(pendingCount = count) } }
        }
    }

    fun onSignOutClick() = _uiState.update { it.copy(confirmingSignOut = true) }

    fun onSignOutDismiss() = _uiState.update { it.copy(confirmingSignOut = false) }

    /** Al borrarse la sesión `AppNavHost` cambia al grafo de autenticación. */
    fun onSignOutConfirm() {
        if (_uiState.value.signingOut) return
        _uiState.update { it.copy(confirmingSignOut = false, signingOut = true) }
        viewModelScope.launch { signOut() }
    }
}
