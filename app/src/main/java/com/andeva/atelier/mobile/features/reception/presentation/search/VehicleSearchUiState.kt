package com.andeva.atelier.mobile.features.reception.presentation.search

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.reception.domain.Appointment
import com.andeva.atelier.mobile.features.reception.domain.ArrivalResult
import com.andeva.atelier.mobile.features.reception.domain.Customer
import com.andeva.atelier.mobile.features.reception.domain.Vehicle
import com.andeva.atelier.mobile.features.reception.domain.VehicleWithOwner

/** Qué se escribe en la barra: la placa del vehículo o el documento (DNI/RUC) del cliente. */
enum class SearchMode { PLATE, DOCUMENT }

data class VehicleSearchUiState(
    val mode: SearchMode = SearchMode.PLATE,
    val query: String = "",
    val isSearching: Boolean = false,
    /** Ya se buscó con el texto actual: distingue "sin resultados" de "aún no se busca". */
    val searched: Boolean = false,
    val vehicleResult: VehicleWithOwner? = null,
    val customers: List<Customer> = emptyList(),
    val selectedCustomer: Customer? = null,
    val customerVehicles: List<Vehicle> = emptyList(),
    val isLoadingVehicles: Boolean = false,
    val appointments: List<Appointment> = emptyList(),
    val isRefreshingAppointments: Boolean = false,
    /** Cita cuya llegada se está registrando. */
    val arrivingId: String? = null,
    val arrival: ArrivalResult? = null,
    val error: UiText? = null,
    val canSearch: Boolean = false,
    val canViewAppointments: Boolean = false,
    val canMarkArrival: Boolean = false,
    val canRegister: Boolean = false,
    val canOpenWorkOrder: Boolean = false
) {
    val canSubmitSearch: Boolean get() = canSearch && query.isNotBlank() && !isSearching
    val vehicleNotFound: Boolean get() = searched && mode == SearchMode.PLATE && vehicleResult == null
    val customersNotFound: Boolean get() = searched && mode == SearchMode.DOCUMENT && customers.isEmpty()
}
