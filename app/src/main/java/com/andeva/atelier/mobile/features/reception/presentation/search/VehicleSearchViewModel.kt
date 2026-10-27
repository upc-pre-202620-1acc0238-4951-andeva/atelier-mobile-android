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
import com.andeva.atelier.mobile.features.reception.application.RefreshAppointmentsUseCase
import com.andeva.atelier.mobile.features.reception.application.SearchCustomersUseCase
import com.andeva.atelier.mobile.features.reception.domain.Customer
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

/** US11: buscar la unidad por placa o el cliente por documento, y atender las citas del día. */
@HiltViewModel
class VehicleSearchViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val findVehicleByPlate: FindVehicleByPlateUseCase,
    private val searchCustomers: SearchCustomersUseCase,
    private val getCustomerVehicles: GetCustomerVehiclesUseCase,
    private val refreshAppointments: RefreshAppointmentsUseCase,
    observeAppointments: ObserveAppointmentsUseCase,
    private val markArrived: MarkAppointmentArrivedUseCase,
    clock: Clock
) : ViewModel() {

    private val today = BusinessTime.limaDate(clock.instant())

    private val _uiState = MutableStateFlow(VehicleSearchUiState())
    val uiState: StateFlow<VehicleSearchUiState> = _uiState.asStateFlow()

    private var agendaRequested = false

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                val canViewAppointments = session?.has(Permission.APPOINTMENTS_READ) == true
                _uiState.update {
                    it.copy(
                        canSearch = session?.has(Permission.VEHICLES_READ) == true &&
                            session.has(Permission.CUSTOMERS_READ),
                        canViewAppointments = canViewAppointments,
                        canMarkArrival = session?.has(Permission.APPOINTMENTS_UPDATE) == true,
                        canRegister = session?.has(Permission.CUSTOMERS_CREATE) == true &&
                            session.has(Permission.VEHICLES_CREATE)
                    )
                }
                // La sesión llega de forma asíncrona: la agenda se carga al conocer el permiso, una sola vez.
                if (canViewAppointments && !agendaRequested) {
                    agendaRequested = true
                    refresh()
                }
            }
        }
        viewModelScope.launch {
            observeAppointments(today).collect { list -> _uiState.update { it.copy(appointments = list) } }
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

    /** Búsqueda por documento: carga los vehículos del cliente elegido. */
    fun onSelectCustomer(customer: Customer) {
        if (_uiState.value.isLoadingVehicles) return
        _uiState.update { it.copy(selectedCustomer = customer, customerVehicles = emptyList(), isLoadingVehicles = true, error = null) }
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
