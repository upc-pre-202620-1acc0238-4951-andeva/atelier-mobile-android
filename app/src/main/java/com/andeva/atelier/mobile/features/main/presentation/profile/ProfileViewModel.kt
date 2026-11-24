package com.andeva.atelier.mobile.features.main.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.core.notifications.PushInbox
import com.andeva.atelier.mobile.features.auth.application.GetSelectableBranchesUseCase
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
    private val getBranches: GetSelectableBranchesUseCase,
    private val pushInbox: PushInbox,
    private val signOut: SignOutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                if (session == null) return@collect
                val plan = session.plan
                _uiState.update { state ->
                    state.copy(
                        fullName = session.fullName,
                        email = session.email,
                        tenantName = session.tenantName,
                        branchId = session.branchId,
                        planTier = plan?.tier?.lowercase()?.replaceFirstChar { it.uppercase() }.orEmpty(),
                        iotTelemetryEnabled = plan?.iotTelemetryEnabled ?: false,
                        aiDiagnosticsEnabled = plan?.aiDiagnosticsEnabled ?: false
                    )
                }
                resolveBranch(session.branchId, session.canSwitchBranch())
            }
        }
        viewModelScope.launch {
            observePendingCount().collect { count -> _uiState.update { it.copy(pendingCount = count) } }
        }
        viewModelScope.launch {
            pushInbox.history.collect { notifications ->
                _uiState.update { it.copy(unreadNotificationsCount = notifications.count { n -> !n.read }) }
            }
        }
    }

    /** Sin red conserva el estado previo: el nombre de la sede no está en la sesión. */
    private fun resolveBranch(branchId: String?, allowSwitch: Boolean) {
        if (branchId.isNullOrBlank()) return
        viewModelScope.launch {
            getBranches().onSuccess { branches ->
                _uiState.update {
                    it.copy(
                        branchName = branches.firstOrNull { b -> b.id == branchId }?.name ?: it.branchName,
                        canChangeBranch = allowSwitch && branches.size > 1
                    )
                }
            }
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
