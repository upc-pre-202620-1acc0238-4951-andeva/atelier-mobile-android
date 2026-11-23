package com.andeva.atelier.mobile.features.cashier.presentation.dailycash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.domain.BusinessTime
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.cashier.application.GetDailyCashSummaryUseCase
import com.andeva.atelier.mobile.features.cashier.presentation.toCashierText
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DailyCashViewModel @Inject constructor(
    private val getDailyCashSummary: GetDailyCashSummaryUseCase,
    observeSession: ObserveSessionUseCase,
    connectivity: ConnectivityObserver,
    clock: Clock
) : ViewModel() {

    private val today = BusinessTime.limaDate(clock.instant())
    private val _uiState = MutableStateFlow(DailyCashUiState(today = today, date = today))
    val uiState: StateFlow<DailyCashUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                val canRead = session?.has(Permission.INVOICES_READ) == true
                _uiState.update { it.copy(canRead = canRead) }
            }
        }
        viewModelScope.launch {
            connectivity.isOnline.collect { online ->
                val retry = online && !_uiState.value.isOnline && _uiState.value.summary == null
                _uiState.update { it.copy(isOnline = online) }
                if (retry) load()
            }
        }
        load()
    }

    fun load() {
        val date = _uiState.value.date
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, error = null) }
        loadJob = viewModelScope.launch {
            getDailyCashSummary(date)
                .onSuccess { summary -> _uiState.update { it.copy(summary = summary, isLoading = false) } }
                .onFailure { e -> _uiState.update { it.copy(summary = null, isLoading = false, error = e.toCashierText()) } }
        }
    }

    fun onPreviousDay() = goTo(_uiState.value.date.minusDays(1))
    fun onNextDay() = if (_uiState.value.canGoNext) goTo(_uiState.value.date.plusDays(1)) else Unit
    fun onToday() = goTo(today)

    private fun goTo(date: LocalDate) {
        _uiState.update { it.copy(date = date, summary = null) }
        load()
    }
}
