package com.andeva.atelier.mobile.features.reception.presentation.search

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.reception.domain.Appointment
import com.andeva.atelier.mobile.features.reception.domain.ArrivalResult
import com.andeva.atelier.mobile.features.reception.domain.Customer
import com.andeva.atelier.mobile.features.reception.domain.CustomerType
import com.andeva.atelier.mobile.features.reception.domain.Vehicle
import com.andeva.atelier.mobile.features.reception.domain.VehicleWithOwner

/** Qué se escribe en la barra: la placa del vehículo o el documento (DNI/RUC) del cliente. */
enum class SearchMode { PLATE, DOCUMENT }

/** Secciones del módulo de Recepción y CRM (EP03). */
enum class ReceptionSection { SEARCH_AND_TODAY, CUSTOMERS, VEHICLES }

data class VehicleSearchUiState(
    val section: ReceptionSection = ReceptionSection.SEARCH_AND_TODAY,
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
    val portfolioCustomers: List<Customer> = emptyList(),
    val customerFilterQuery: String = "",
    val customerFilterType: CustomerType? = null,
    val isRefreshingCustomers: Boolean = false,
    val workshopVehicles: List<VehicleWithOwner> = emptyList(),
    val vehicleFilterQuery: String = "",
    val appointments: List<Appointment> = emptyList(),
    val isRefreshingAppointments: Boolean = false,
    /** Cita cuya llegada se está registrando. */
    val arrivingId: String? = null,
    val arrival: ArrivalResult? = null,
    val error: UiText? = null,
    val canSearch: Boolean = false,
    val canReadCustomers: Boolean = false,
    val canViewAppointments: Boolean = false,
    val canMarkArrival: Boolean = false,
    val canRegister: Boolean = false,
    val canCreateVehicles: Boolean = false,
    val canOpenWorkOrder: Boolean = false
) {
    val canSubmitSearch: Boolean get() = canSearch && query.isNotBlank() && !isSearching
    val vehicleNotFound: Boolean get() = searched && mode == SearchMode.PLATE && vehicleResult == null
    val customersNotFound: Boolean get() = searched && mode == SearchMode.DOCUMENT && customers.isEmpty()

    val initialPlateCandidate: String?
        get() = query.trim().takeIf { mode == SearchMode.PLATE && it.isNotBlank() }

    val directoryCustomers: List<Customer> get() = portfolioCustomers
    val directoryFilter: String
        get() = if (section == ReceptionSection.VEHICLES) vehicleFilterQuery else customerFilterQuery
    val isRefreshingPortfolio: Boolean get() = isRefreshingCustomers
    val filteredDirectoryCustomers: List<Customer> get() = filteredPortfolioCustomers

    val filteredPortfolioCustomers: List<Customer>
        get() {
            val q = customerFilterQuery.trim().lowercase()
            return portfolioCustomers.filter { customer ->
                val matchesType = customerFilterType == null || customer.type == customerFilterType
                val matchesQuery = q.isEmpty() ||
                    customer.displayName.lowercase().contains(q) ||
                    customer.taxId?.lowercase()?.contains(q) == true ||
                    customer.email?.lowercase()?.contains(q) == true ||
                    customer.phone?.lowercase()?.contains(q) == true
                matchesType && matchesQuery
            }
        }

    val filteredWorkshopVehicles: List<VehicleWithOwner>
        get() {
            val q = vehicleFilterQuery.trim().lowercase()
            if (q.isEmpty()) return workshopVehicles
            val normalizedQuery = q.replace("-", "").replace(" ", "")
            return workshopVehicles.filter { item ->
                item.vehicle.plate.lowercase().contains(normalizedQuery) ||
                    item.vehicle.brand.lowercase().contains(q) ||
                    item.vehicle.model.lowercase().contains(q) ||
                    item.owner?.displayName?.lowercase()?.contains(q) == true ||
                    item.owner?.taxId?.lowercase()?.contains(q) == true
            }
        }
}
