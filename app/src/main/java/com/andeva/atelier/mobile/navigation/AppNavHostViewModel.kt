package com.andeva.atelier.mobile.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.core.data.local.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    /** [needsBranchChoice]: cuenta sin perfil laboral que aún no eligió sede (A5b). */
    data class SignedIn(val needsBranchChoice: Boolean = false) : SessionState
}

@HiltViewModel
class AppNavHostViewModel @Inject constructor(
    sessionManager: SessionManager
) : ViewModel() {
    /** Cuando el refresh falla la sesión se borra y esto vuelve a [SessionState.SignedOut]. */
    val sessionState: StateFlow<SessionState> = sessionManager.session
        .map { if (it == null) SessionState.SignedOut else SessionState.SignedIn(it.needsBranchChoice) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionState.Loading)
}
