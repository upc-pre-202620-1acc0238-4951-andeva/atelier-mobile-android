package com.andeva.atelier.mobile.features.main.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.domain.Session
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
                val derivedRole = deriveRoleTitle(session)
                val plan = session.plan
                _uiState.update { state ->
                    state.copy(
                        fullName = session.fullName,
                        email = session.email,
                        tenantName = session.tenantName,
                        branchId = session.branchId,
                        roleTitle = derivedRole,
                        planName = plan?.tier?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Básico",
                        iotTelemetryEnabled = plan?.iotTelemetryEnabled ?: false,
                        aiDiagnosticsEnabled = plan?.aiDiagnosticsEnabled ?: false,
                        permissionsCount = session.permissions.size,
                        permissions = session.permissions.sorted(),
                        membershipId = session.membershipId
                    )
                }
                resolveBranchName(session.branchId)
            }
        }
        viewModelScope.launch {
            observePendingCount().collect { count ->
                _uiState.update { it.copy(pendingCount = count) }
            }
        }
        viewModelScope.launch {
            pushInbox.history.collect { notifications ->
                val unread = notifications.count { !it.read }
                _uiState.update { it.copy(unreadNotificationsCount = unread) }
            }
        }
    }

    private fun resolveBranchName(branchId: String?) {
        if (branchId.isNullOrBlank()) return
        viewModelScope.launch {
            try {
                val branches = getBranches().getOrNull().orEmpty()
                val matched = branches.firstOrNull { it.id == branchId }?.name
                if (matched != null) {
                    _uiState.update { it.copy(branchName = matched) }
                }
            } catch (_: Exception) {
                // Si no hay conexión o falla la carga, conserva el estado previo
            }
        }
    }

    private fun deriveRoleTitle(session: Session): String {
        return when {
            session.has(Permission.ATTENDANCE_AUDIT_ALL) || session.has(Permission.WORK_ORDERS_CANCEL) ->
                "Supervisor general de taller"
            session.has(Permission.BAYS_ASSIGN) ->
                "Jefe de patio y bahías"
            session.has(Permission.VEHICLES_CREATE) ->
                "Asesor de recepción de vehículos"
            session.has(Permission.TASKS_TRACK_TIME) ->
                "Técnico especialista de servicio"
            session.has(Permission.PARTS_READ) ->
                "Encargado de almacén y repuestos"
            else -> "Colaborador de taller"
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
