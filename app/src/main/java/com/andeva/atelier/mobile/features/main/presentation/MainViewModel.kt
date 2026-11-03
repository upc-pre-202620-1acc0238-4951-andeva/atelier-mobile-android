package com.andeva.atelier.mobile.features.main.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.features.main.application.ObservePendingOperationCountUseCase
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.main.presentation.navigation.MainTab
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    observePendingCount: ObservePendingOperationCountUseCase
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = combine(observeSession(), observePendingCount()) { session, pending ->
        MainUiState(
            tabs = session?.let(MainTab::tabsFor).orEmpty(),
            pendingCount = pending
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())
}
