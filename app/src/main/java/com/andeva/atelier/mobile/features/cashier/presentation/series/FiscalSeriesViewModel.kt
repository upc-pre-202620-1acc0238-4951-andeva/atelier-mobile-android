package com.andeva.atelier.mobile.features.cashier.presentation.series

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.cashier.application.ConfigureSeriesUseCase
import com.andeva.atelier.mobile.features.cashier.application.GetBranchSeriesUseCase
import com.andeva.atelier.mobile.features.cashier.application.GetTenantFiscalProfileUseCase
import com.andeva.atelier.mobile.features.cashier.application.SetSeriesActiveUseCase
import com.andeva.atelier.mobile.features.cashier.application.UpdateTenantFiscalProfileUseCase
import com.andeva.atelier.mobile.features.cashier.domain.FiscalSeries
import com.andeva.atelier.mobile.features.cashier.domain.VoucherType
import com.andeva.atelier.mobile.features.cashier.presentation.toCashierText
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FiscalSeriesViewModel @Inject constructor(
    private val getSeries: GetBranchSeriesUseCase,
    private val configureSeries: ConfigureSeriesUseCase,
    private val setSeriesActive: SetSeriesActiveUseCase,
    private val observeSession: ObserveSessionUseCase,
    connectivity: ConnectivityObserver,
    private val getTenantFiscalProfile: GetTenantFiscalProfileUseCase? = null,
    private val updateTenantFiscalProfile: UpdateTenantFiscalProfileUseCase? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(FiscalSeriesUiState())
    val uiState: StateFlow<FiscalSeriesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                _uiState.update {
                    it.copy(
                        canManage = session?.has(Permission.FISCAL_CONFIG_MANAGE) == true,
                        canReadTenant = session?.has(Permission.TENANTS_READ) == true,
                        canUpdateTenant = session?.has(Permission.TENANTS_UPDATE) == true
                    )
                }
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
        val session = observeSession().firstOrNull()
        val canManage = session?.has(Permission.FISCAL_CONFIG_MANAGE) == true || _uiState.value.canManage
        val canReadTenant = session?.has(Permission.TENANTS_READ) == true ||
            session?.has(Permission.TENANTS_UPDATE) == true ||
            _uiState.value.canReadTenant ||
            _uiState.value.canUpdateTenant

        var failure: Throwable? = null
        if (canReadTenant && getTenantFiscalProfile != null) {
            getTenantFiscalProfile()
                .onSuccess { profile -> _uiState.update { it.copy(tenantProfile = profile) } }
                .onFailure { e -> failure = e }
        }
        if (canManage || session == null) {
            getSeries()
                .onSuccess { series ->
                    val sorted = series.sortedWith(compareBy({ it.type?.ordinal ?: Int.MAX_VALUE }, { it.serie }))
                    _uiState.update { it.copy(series = sorted) }
                }
                .onFailure { e -> failure = failure ?: e }
        }
        _uiState.update {
            it.copy(
                hasLoaded = true,
                isRefreshing = false,
                error = failure?.toCashierText()
            )
        }
    }

    fun onEditTenant() {
        val state = _uiState.value
        if (!state.canUpdateTenant) return
        val profile = state.tenantProfile
        val form = TenantProfileForm(
            name = profile?.name.orEmpty(),
            legalName = profile?.legalName?.ifBlank { profile.name }.orEmpty(),
            taxId = profile?.taxId.orEmpty()
        )
        _uiState.update { it.copy(sheet = SeriesSheet.EditTenant(form), sheetError = null, notice = null) }
    }

    fun onTenantName(value: String) = updateTenantForm { it.copy(name = value.take(100)) }
    fun onTenantLegalName(value: String) = updateTenantForm { it.copy(legalName = value.take(150)) }

    private fun updateTenantForm(change: (TenantProfileForm) -> TenantProfileForm) = _uiState.update { state ->
        val sheet = state.sheet as? SeriesSheet.EditTenant ?: return@update state
        state.copy(sheet = SeriesSheet.EditTenant(change(sheet.form)), sheetError = null)
    }

    fun onSaveTenant() {
        val state = _uiState.value
        val form = (state.sheet as? SeriesSheet.EditTenant)?.form ?: return
        val updater = updateTenantFiscalProfile ?: return
        if (!state.canUpdateTenant || !form.canSubmit || !state.isOnline || state.isSaving) return
        _uiState.update { it.copy(isSaving = true, sheetError = null) }
        viewModelScope.launch {
            updater(form.name, form.legalName)
                .onSuccess { updated ->
                    _uiState.update {
                        it.copy(
                            tenantProfile = updated,
                            sheet = null,
                            notice = UiText.Resource(R.string.cashier_fiscal_tenant_saved)
                        )
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(sheetError = e.toCashierText()) } }
            _uiState.update { it.copy(isSaving = false) }
        }
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
