package com.andeva.atelier.mobile.features.reception.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.core.domain.BusinessTime
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.reception.application.FindVehicleByPlateUseCase
import com.andeva.atelier.mobile.features.reception.application.GetCustomerVehiclesUseCase
import com.andeva.atelier.mobile.features.reception.application.MarkAppointmentArrivedUseCase
import com.andeva.atelier.mobile.features.reception.application.ObserveAppointmentsUseCase
import com.andeva.atelier.mobile.features.reception.application.ObserveCustomersUseCase
import com.andeva.atelier.mobile.features.reception.application.ObserveWorkshopVehiclesUseCase
import com.andeva.atelier.mobile.features.reception.application.RefreshAppointmentsUseCase
import com.andeva.atelier.mobile.features.reception.application.RefreshCustomersUseCase
import com.andeva.atelier.mobile.features.reception.application.SearchCustomersUseCase
import com.andeva.atelier.mobile.features.reception.domain.Customer
import com.andeva.atelier.mobile.features.reception.domain.CustomerType
import com.andeva.atelier.mobile.features.reception.domain.ReceptionValidation
import com.andeva.atelier.mobile.features.reception.presentation.toReceptionUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.Clock
import javax.inject.Inject

/** US11: buscar la unidad por placa o el cliente por documento, atender las citas del día y consultar cartera de clientes y vehículos. */
@HiltViewModel
class VehicleSearchViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val findVehicleByPlate: FindVehicleByPlateUseCase,
    private val searchCustomers: SearchCustomersUseCase,
    private val getCustomerVehicles: GetCustomerVehiclesUseCase,
    private val refreshAppointments: RefreshAppointmentsUseCase,
    observeAppointments: ObserveAppointmentsUseCase,
    private val markArrived: MarkAppointmentArrivedUseCase,
    clock: Clock,
    private val refreshCustomers: RefreshCustomersUseCase? = null,
    observeCustomers: ObserveCustomersUseCase? = null,
    observeWorkshopVehicles: ObserveWorkshopVehiclesUseCase? = null
) : ViewModel() {

    constructor(
        observeSession: ObserveSessionUseCase,
        findVehicleByPlate: FindVehicleByPlateUseCase,
        searchCustomers: SearchCustomersUseCase,
        getCustomerVehicles: GetCustomerVehiclesUseCase,
        refreshAppointments: RefreshAppointmentsUseCase,
        observeAppointments: ObserveAppointmentsUseCase,
        markArrived: MarkAppointmentArrivedUseCase,
        clock: Clock
    ) : this(
        observeSession = observeSession,
        findVehicleByPlate = findVehicleByPlate,
        searchCustomers = searchCustomers,
        getCustomerVehicles = getCustomerVehicles,
        refreshAppointments = refreshAppointments,
        observeAppointments = observeAppointments,
        markArrived = markArrived,
        clock = clock,
        refreshCustomers = null,
        observeCustomers = null,
        observeWorkshopVehicles = null
    )

    private val today = BusinessTime.limaDate(clock.instant())

    private val _uiState = MutableStateFlow(VehicleSearchUiState())
    val uiState: StateFlow<VehicleSearchUiState> = _uiState.asStateFlow()

    private var lastAgendaBranchId: String? = "__none__"
    private var portfolioRequested = false

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                val canViewAppointments = session?.has(Permission.APPOINTMENTS_READ) == true
                val canReadCustomers = session?.has(Permission.CUSTOMERS_READ) == true
                val canCreateVehicles = session?.has(Permission.VEHICLES_CREATE) == true
                val branchKey = session?.branchId ?: ""
                _uiState.update {
                    it.copy(
                        canSearch = session?.has(Permission.VEHICLES_READ) == true && canReadCustomers,
                        canReadCustomers = canReadCustomers,
                        canViewAppointments = canViewAppointments,
                        canMarkArrival = session?.has(Permission.APPOINTMENTS_UPDATE) == true,
                        canRegister = session?.has(Permission.CUSTOMERS_CREATE) == true && canCreateVehicles,
                        canCreateVehicles = canCreateVehicles,
                        canOpenWorkOrder = session?.has(Permission.WORK_ORDERS_CREATE) == true
                    )
                }
                if (canViewAppointments && lastAgendaBranchId != branchKey) {
                    lastAgendaBranchId = branchKey
                    refresh()
                }
                if (canReadCustomers && !portfolioRequested) {
                    portfolioRequested = true
                    refreshCustomerPortfolio(force = false)
                }
            }
        }
        viewModelScope.launch {
            observeAppointments(today).collect { list -> _uiState.update { it.copy(appointments = list) } }
        }
        if (observeCustomers != null) {
            viewModelScope.launch {
                observeCustomers().collect { list ->
                    _uiState.update { state ->
                        val refreshedSelected = state.selectedCustomer?.let { sel -> list.firstOrNull { it.id == sel.id } ?: sel }
                        state.copy(portfolioCustomers = list, selectedCustomer = refreshedSelected)
                    }
                }
            }
        }
        if (observeWorkshopVehicles != null) {
            viewModelScope.launch {
                observeWorkshopVehicles().collect { vehicles ->
                    _uiState.update { state ->
                        // Si el usuario buscó una placa que no existía y acaba de registrarla, se sincroniza de inmediato.
                        val autoMatched = if (state.searched && state.mode == SearchMode.PLATE && state.vehicleResult == null && state.query.isNotBlank()) {
                            val normalized = runCatching { ReceptionValidation.normalizePlate(state.query) }.getOrNull()
                            vehicles.firstOrNull { it.vehicle.plate.equals(normalized, ignoreCase = true) }
                        } else {
                            state.vehicleResult?.let { current -> vehicles.firstOrNull { it.vehicle.id == current.vehicle.id } ?: current }
                        }
                        val updatedCustomerVehicles = state.selectedCustomer?.let { sel ->
                            val fromRoom = vehicles.filter { it.vehicle.currentOwnerId == sel.id }.map { it.vehicle }
                            if (fromRoom.size > state.customerVehicles.size) fromRoom else state.customerVehicles
                        } ?: state.customerVehicles
                        state.copy(
                            workshopVehicles = vehicles,
                            vehicleResult = autoMatched,
                            customerVehicles = updatedCustomerVehicles
                        )
                    }
                }
            }
        }
    }

    fun onSectionChange(section: ReceptionSection) {
        _uiState.update { it.copy(section = section, error = null) }
        if (section == ReceptionSection.CUSTOMERS && _uiState.value.portfolioCustomers.isEmpty()) {
            refreshCustomerPortfolio(force = true)
        }
    }

    fun onCustomerFilterQueryChange(query: String) = _uiState.update {
        it.copy(customerFilterQuery = query)
    }

    fun onCustomerFilterTypeChange(type: CustomerType?) = _uiState.update {
        it.copy(customerFilterType = if (it.customerFilterType == type) null else type)
    }

    fun onVehicleFilterQueryChange(query: String) = _uiState.update {
        it.copy(vehicleFilterQuery = query, error = null)
    }

    fun onDirectoryFilterChange(query: String) {
        if (_uiState.value.section == ReceptionSection.VEHICLES) {
            onVehicleFilterQueryChange(query)
        } else {
            onCustomerFilterQueryChange(query)
        }
    }

    fun refreshPortfolio() = refreshCustomerPortfolio(force = true)

    /** Busca una placa desde la pestaña de Vehículos si aún no estaba en la caché local. */
    fun onLookupVehicleFromFilter() {
        val rawPlate = _uiState.value.vehicleFilterQuery.trim()
        if (rawPlate.isBlank() || _uiState.value.isSearching) return
        _uiState.update { it.copy(isSearching = true, error = null) }
        viewModelScope.launch {
            findVehicleByPlate(rawPlate)
                .onSuccess { found ->
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            vehicleResult = found ?: it.vehicleResult
                        )
                    }
                }
                .onFailure(::failSearch)
        }
    }

    /** Recarga la cartera de clientes del taller (`GET /api/v1/customers`). */
    fun refreshCustomerPortfolio(force: Boolean = true) {
        val useCase = refreshCustomers ?: return
        val state = _uiState.value
        if (!state.canReadCustomers || state.isRefreshingCustomers) return
        _uiState.update { it.copy(isRefreshingCustomers = true, error = null) }
        viewModelScope.launch {
            val failure = useCase(force).exceptionOrNull()?.takeUnless { it is IOException }
            _uiState.update {
                it.copy(isRefreshingCustomers = false, error = failure?.toReceptionUiText() ?: it.error)
            }
        }
    }

    /** Recarga la agenda de hoy. Sin red se sigue con la caché; sin permiso de lectura no se llama. */
    fun refresh() {
        val state = _uiState.value
        if (!state.canViewAppointments || state.isRefreshingAppointments) return
        _uiState.update { it.copy(isRefreshingAppointments = true) }
        viewModelScope.launch {
            val failure = refreshAppointments(today).exceptionOrNull()?.takeUnless { it is IOException }
            _uiState.update {
                it.copy(isRefreshingAppointments = false, error = failure?.toReceptionUiText() ?: it.error)
            }
        }
    }

    fun onModeChange(mode: SearchMode) = _uiState.update {
        if (it.isSearching || it.mode == mode) it else clearedSearch(it).copy(mode = mode, query = "")
    }

    fun onQueryChange(value: String) = _uiState.update {
        if (it.isSearching) it else clearedSearch(it).copy(query = value)
    }

    private fun clearedSearch(state: VehicleSearchUiState) = state.copy(
        searched = false, vehicleResult = null, customers = emptyList(), selectedCustomer = null,
        customerVehicles = emptyList(), error = null
    )

    fun onSearch() {
        val state = _uiState.value
        if (!state.canSubmitSearch) return
        _uiState.update { clearedSearch(it).copy(isSearching = true, arrival = null) }
        viewModelScope.launch {
            when (state.mode) {
                SearchMode.PLATE -> findVehicleByPlate(state.query)
                    .onSuccess { found ->
                        _uiState.update { it.copy(isSearching = false, searched = true, vehicleResult = found) }
                    }
                    .onFailure(::failSearch)
                SearchMode.DOCUMENT -> searchCustomers(state.query.trim())
                    .onSuccess { list ->
                        _uiState.update { it.copy(isSearching = false, searched = true, customers = list) }
                        list.singleOrNull()?.let(::onSelectCustomer)
                    }
                    .onFailure(::failSearch)
            }
        }
    }

    private fun failSearch(error: Throwable) = _uiState.update {
        it.copy(isSearching = false, error = error.toReceptionUiText())
    }

    /** Búsqueda por documento o selección en cartera: carga los vehículos del cliente elegido. */
    fun onSelectCustomer(customer: Customer) {
        if (_uiState.value.isLoadingVehicles) return
        val cachedVehicles = _uiState.value.workshopVehicles
            .filter { it.vehicle.currentOwnerId == customer.id }
            .map { it.vehicle }
        _uiState.update {
            it.copy(
                selectedCustomer = customer,
                customerVehicles = cachedVehicles,
                isLoadingVehicles = true,
                error = null
            )
        }
        viewModelScope.launch {
            getCustomerVehicles(customer.id)
                .onSuccess { vehicles -> _uiState.update { it.copy(isLoadingVehicles = false, customerVehicles = vehicles) } }
                .onFailure { e -> _uiState.update { it.copy(isLoadingVehicles = false, error = e.toReceptionUiText()) } }
        }
    }

    /** `POST /appointments/{id}/arrive` (US11 E1). En línea. La orden en borrador puede tardar (H10). */
    fun onMarkArrived(appointmentId: String) {
        val state = _uiState.value
        if (!state.canMarkArrival || state.arrivingId != null) return
        _uiState.update { it.copy(arrivingId = appointmentId, arrival = null, error = null) }
        viewModelScope.launch {
            markArrived(appointmentId)
                .onSuccess { result -> _uiState.update { it.copy(arrivingId = null, arrival = result) } }
                .onFailure { e -> _uiState.update { it.copy(arrivingId = null, error = e.toReceptionUiText()) } }
        }
    }
}
