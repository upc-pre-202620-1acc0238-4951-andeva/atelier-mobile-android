package com.andeva.atelier.mobile.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.domain.Session
import com.andeva.atelier.mobile.core.notifications.PushDestination
import com.andeva.atelier.mobile.core.notifications.PushInbox
import com.andeva.atelier.mobile.features.main.application.ObservePendingOperationCountUseCase
import com.andeva.atelier.mobile.features.main.presentation.navigation.prefersOperationalDashboard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    /** [needsBranchChoice]: cuenta que debe elegir o confirmar sede (A5b / Tarea 2). */
    data class SignedIn(
        val needsBranchChoice: Boolean = false,
        val prefersDashboard: Boolean = false
    ) : SessionState
}

@HiltViewModel
class AppNavHostViewModel @Inject constructor(
    sessionManager: SessionManager,
    observePendingCount: ObservePendingOperationCountUseCase? = null,
    private val pushInbox: PushInbox? = null
) : ViewModel() {

    constructor(sessionManager: SessionManager) : this(sessionManager, null, null)

    val sessionFlow: StateFlow<Session?> = sessionManager.session
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val pendingSyncCountFlow: StateFlow<Int> = (observePendingCount?.invoke() ?: kotlinx.coroutines.flow.flowOf(0))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val pendingPush: StateFlow<PushDestination?> = pushInbox?.pending
        ?: kotlinx.coroutines.flow.MutableStateFlow(null)

    fun consumePush(): PushDestination? = pushInbox?.consume()

    /** Cuando el refresh falla la sesión se borra y esto vuelve a [SessionState.SignedOut]. */
    val sessionState: StateFlow<SessionState> = sessionManager.session
        .map { session ->
            if (session == null) {
                SessionState.SignedOut
            } else {
                SessionState.SignedIn(
                    needsBranchChoice = session.needsBranchChoice,
                    prefersDashboard = session.prefersOperationalDashboard()
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionState.Loading)
}
