package com.andeva.atelier.mobile.features.cashier.presentation.series

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.cashier.application.ConfigureSeriesUseCase
import com.andeva.atelier.mobile.features.cashier.application.GetBranchSeriesUseCase
import com.andeva.atelier.mobile.features.cashier.application.SetSeriesActiveUseCase
import com.andeva.atelier.mobile.features.cashier.domain.FiscalSeries
import com.andeva.atelier.mobile.features.cashier.domain.VoucherType
import com.andeva.atelier.mobile.features.cashier.presentation.toCashierText
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FiscalSeriesViewModel @Inject constructor(
    private val getSeries: GetBranchSeriesUseCase,
    private val configureSeries: ConfigureSeriesUseCase,
    private val setSeriesActive: SetSeriesActiveUseCase,
    observeSession: ObserveSessionUseCase,
    connectivity: ConnectivityObserver
) : ViewModel() {

    private val _uiState = MutableStateFlow(FiscalSeriesUiState())
    val uiState: StateFlow<FiscalSeriesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                _uiState.update { it.copy(canManage = session?.has(Permission.FISCAL_CONFIG_MANAGE) == true) }
            }
        }
        viewModelScope.launch { connectivity.isOnline.collect { online -> _uiState.update { it.copy(isOnline = online) } } }
        refresh()
    }

    fun refresh() {
        if (_uiState.value.isRefreshing) return
        _uiState.update { it.copy(isRefreshing = true, error = null) }
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        getSeries()
            .onSuccess { series ->
                val sorted = series.sortedWith(compareBy({ it.type?.ordinal ?: Int.MAX_VALUE }, { it.serie }))
                _uiState.update { it.copy(series = sorted, hasLoaded = true, isRefreshing = false) }
            }
            .onFailure { e -> _uiState.update { it.copy(hasLoaded = true, isRefreshing = false, error = e.toCashierText()) } }
    }

    fun onAdd() {
        if (!_uiState.value.canManage) return
        _uiState.update { it.copy(sheet = SeriesSheet.Create(SeriesForm()), sheetError = null, notice = null) }
    }

    fun onOpenSeries(series: FiscalSeries) {
        if (!_uiState.value.canManage) return
        _uiState.update { it.copy(sheet = SeriesSheet.Detail(series), sheetError = null, notice = null) }
    }

    fun onType(type: VoucherType) = updateForm { it.copy(type = type) }
    fun onSerie(value: String) = updateForm { it.copy(serie = value.filter(Char::isLetterOrDigit).uppercase().take(4)) }
    fun onInitial(value: String) = updateForm { it.copy(initial = value.filter(Char::isDigit).take(9)) }

    private fun updateForm(change: (SeriesForm) -> SeriesForm) = _uiState.update { state ->
        val sheet = state.sheet as? SeriesSheet.Create ?: return@update state
        state.copy(sheet = SeriesSheet.Create(change(sheet.form)), sheetError = null)
    }

    fun onSave() {
        val state = _uiState.value
        val form = (state.sheet as? SeriesSheet.Create)?.form ?: return
        if (!form.canSubmit || !state.isOnline || state.isSaving) return
        _uiState.update { it.copy(isSaving = true, sheetError = null) }
        viewModelScope.launch {
            configureSeries(form.type, form.serie, form.initialCorrelative)
                .onSuccess { _ ->
                    _uiState.update { it.copy(sheet = null, notice = UiText.Resource(R.string.cashier_series_saved)) }
                    load()
                }
                .onFailure { e -> _uiState.update { it.copy(sheetError = e.toCashierText()) } }
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    fun onToggleActive() {
        val state = _uiState.value
        val series = (state.sheet as? SeriesSheet.Detail)?.series ?: return
        if (!state.isOnline || state.isSaving) return
        _uiState.update { it.copy(isSaving = true, sheetError = null) }
        viewModelScope.launch {
            setSeriesActive(series.id, !series.active)
                .onSuccess {
                    _uiState.update { it.copy(sheet = null, notice = UiText.Resource(R.string.cashier_series_updated)) }
                    load()
                }
                .onFailure { e -> _uiState.update { it.copy(sheetError = e.toCashierText()) } }
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    fun onDismissSheet() {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(sheet = null, sheetError = null) }
    }
}
