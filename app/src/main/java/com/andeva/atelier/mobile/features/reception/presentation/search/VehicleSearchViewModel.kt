package com.andeva.atelier.mobile.features.reception.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.BusinessTime
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.reception.application.CancelAppointmentUseCase
import com.andeva.atelier.mobile.features.reception.application.ConfirmAppointmentUseCase
import com.andeva.atelier.mobile.features.reception.application.CreateAppointmentUseCase
import com.andeva.atelier.mobile.features.reception.application.FindVehicleByPlateUseCase
import com.andeva.atelier.mobile.features.reception.application.GetCustomerVehiclesUseCase
import com.andeva.atelier.mobile.features.reception.application.MarkAppointmentArrivedUseCase
import com.andeva.atelier.mobile.features.reception.application.ObserveAppointmentsUseCase
import com.andeva.atelier.mobile.features.reception.application.ObserveCustomersUseCase
import com.andeva.atelier.mobile.features.reception.application.ObserveWorkshopVehiclesUseCase
import com.andeva.atelier.mobile.features.reception.application.RefreshAppointmentsUseCase
import com.andeva.atelier.mobile.features.reception.application.RefreshCustomersUseCase
import com.andeva.atelier.mobile.features.reception.application.RescheduleAppointmentUseCase
import com.andeva.atelier.mobile.features.reception.application.SearchCustomersUseCase
import com.andeva.atelier.mobile.features.reception.application.UpdateCustomerContactUseCase
import com.andeva.atelier.mobile.features.reception.domain.Customer
import com.andeva.atelier.mobile.features.reception.domain.CustomerType
import com.andeva.atelier.mobile.features.reception.domain.InvalidReceptionInputException
import com.andeva.atelier.mobile.features.reception.domain.ReceptionValidation
import com.andeva.atelier.mobile.features.reception.presentation.invalidReceptionField
import com.andeva.atelier.mobile.features.reception.presentation.toReceptionUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import javax.inject.Inject

/** US11: buscar la unidad por placa o el cliente por documento, atender/agendar citas del día y editar contacto. */
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
    observeWorkshopVehicles: ObserveWorkshopVehiclesUseCase? = null,
    connectivity: ConnectivityObserver? = null,
    private val createAppointment: CreateAppointmentUseCase? = null,
    private val confirmAppointment: ConfirmAppointmentUseCase? = null,
    private val rescheduleAppointment: RescheduleAppointmentUseCase? = null,
    private val cancelAppointment: CancelAppointmentUseCase? = null,
    private val updateCustomerContact: UpdateCustomerContactUseCase? = null
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
    private val selectedRange = MutableStateFlow(AppointmentRange.TODAY)

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
                        canUpdateCustomer = session?.has(Permission.CUSTOMERS_UPDATE) == true,
                        canViewAppointments = canViewAppointments,
                        canCreateAppointments = session?.has(Permission.APPOINTMENTS_CREATE) == true,
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
        @OptIn(ExperimentalCoroutinesApi::class)
        viewModelScope.launch {
            selectedRange
                .flatMapLatest { range ->
                    if (range == AppointmentRange.TODAY) {
                        observeAppointments(today)
                    } else {
                        observeAppointments(today, range.endDate(today))
                    }
                }
                .collect { list -> _uiState.update { it.copy(appointments = list) } }
        }
        if (connectivity != null) {
            viewModelScope.launch {
                connectivity.isOnline.collect { online -> _uiState.update { it.copy(isOnline = online) } }
            }
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
        _uiState.update { it.copy(section = section, error = null, notice = null) }
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

    /** Cambia el horizonte de la agenda (Hoy / 7 días / 30 días) y recarga desde el backend. */
    fun onAppointmentRangeChange(range: AppointmentRange) {
        if (_uiState.value.appointmentRange == range) return
        _uiState.update { it.copy(appointmentRange = range, error = null) }
        selectedRange.value = range
        refreshRange(force = true)
    }

    /** Recarga la agenda para el rango activo. Sin red se sigue con la caché; sin permiso de lectura no se llama. */
    fun refresh() = refreshRange(force = false)

    private fun refreshRange(force: Boolean) {
        val state = _uiState.value
        if (!state.canViewAppointments || (!force && state.isRefreshingAppointments)) return
        val range = state.appointmentRange
        _uiState.update { it.copy(isRefreshingAppointments = true) }
        viewModelScope.launch {
            val result = if (range == AppointmentRange.TODAY) {
                refreshAppointments(today)
            } else {
                refreshAppointments(today, range.endDate(today))
            }
            val failure = result.exceptionOrNull()?.takeUnless { it is IOException }
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
        customerVehicles = emptyList(), error = null, notice = null
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
                error = null,
                notice = null
            )
        }
        viewModelScope.launch {
            getCustomerVehicles(customer.id)
                .onSuccess { vehicles -> _uiState.update { it.copy(isLoadingVehicles = false, customerVehicles = vehicles) } }
                .onFailure { e -> _uiState.update { it.copy(isLoadingVehicles = false, error = e.toReceptionUiText()) } }
        }
    }

    // --- Hoja de acciones de cita: Confirmar, Llegada (H10), Reprogramar y Cancelar ---

    fun onOpenAppointmentActions(appointmentId: String) {
        _uiState.update { it.copy(actionAppointmentId = appointmentId, error = null, notice = null) }
    }

    fun onDismissAppointmentActions() {
        val state = _uiState.value
        if (state.arrivingId != null || state.updatingAppointmentId != null) return
        _uiState.update { it.copy(actionAppointmentId = null, error = null) }
    }

    /** `POST /appointments/{id}/arrive` (US11 E1). En línea. La orden en borrador puede tardar (H10). */
    fun onMarkArrived(appointmentId: String) {
        val state = _uiState.value
        if (!state.canMarkArrival || state.arrivingId != null || state.updatingAppointmentId != null) return
        _uiState.update { it.copy(arrivingId = appointmentId, arrival = null, error = null, notice = null) }
        viewModelScope.launch {
            markArrived(appointmentId)
                .onSuccess { result ->
                    _uiState.update { it.copy(arrivingId = null, actionAppointmentId = null, arrival = result) }
                }
                .onFailure { e -> _uiState.update { it.copy(arrivingId = null, error = e.toReceptionUiText()) } }
        }
    }

    /** `POST /appointments/{id}/confirm`. En línea. */
    fun onConfirmAppointment(appointmentId: String) {
        val confirm = confirmAppointment ?: return
        val state = _uiState.value
        if (!state.canMarkArrival || !state.isOnline || state.updatingAppointmentId != null) return
        _uiState.update { it.copy(updatingAppointmentId = appointmentId, error = null, notice = null) }
        viewModelScope.launch {
            confirm(appointmentId)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            updatingAppointmentId = null,
                            actionAppointmentId = null,
                            notice = UiText.Resource(R.string.reception_appointment_confirmed_notice)
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(updatingAppointmentId = null, error = e.toReceptionUiText()) }
                }
        }
    }

    /** `POST /appointments/{id}/reschedule`. En línea. */
    fun onRescheduleAppointment(appointmentId: String, dateText: String, timeText: String) {
        val reschedule = rescheduleAppointment ?: return
        val state = _uiState.value
        if (!state.canMarkArrival || !state.isOnline || state.updatingAppointmentId != null) return
        val newInstant = parseLimaDateTime(dateText, timeText)
        if (newInstant == null) {
            _uiState.update { it.copy(error = UiText.Resource(R.string.reception_appointment_invalid_datetime)) }
            return
        }
        _uiState.update { it.copy(updatingAppointmentId = appointmentId, error = null, notice = null) }
        viewModelScope.launch {
            reschedule(appointmentId, newInstant)
                .onSuccess {
                    refresh()
                    _uiState.update {
                        it.copy(
                            updatingAppointmentId = null,
                            actionAppointmentId = null,
                            notice = UiText.Resource(R.string.reception_appointment_rescheduled_notice)
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(updatingAppointmentId = null, error = e.toReceptionUiText()) }
                }
        }
    }

    /** `POST /appointments/{id}/cancel`. En línea. */
    fun onCancelAppointment(appointmentId: String, reason: String) {
        val cancel = cancelAppointment ?: return
        val state = _uiState.value
        if (!state.canMarkArrival || !state.isOnline || state.updatingAppointmentId != null) return
        _uiState.update { it.copy(updatingAppointmentId = appointmentId, error = null, notice = null) }
        viewModelScope.launch {
            cancel(appointmentId, reason)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            updatingAppointmentId = null,
                            actionAppointmentId = null,
                            notice = UiText.Resource(R.string.reception_appointment_canceled_notice)
                        )
                    }
                }
                .onFailure { e ->
                    val message = if (e is InvalidReceptionInputException) {
                        UiText.Resource(R.string.reception_appointment_invalid_cancel_reason)
                    } else {
                        e.toReceptionUiText()
                    }
                    _uiState.update { it.copy(updatingAppointmentId = null, error = message) }
                }
        }
    }

    // --- Nueva cita (`POST /appointments`, `APPOINTMENTS_CREATE`) ---

    fun onOpenCreateAppointment() {
        val state = _uiState.value
        if (!state.canCreateAppointments || !state.isOnline) return
        val preselectedCustomer = state.selectedCustomer ?: state.vehicleResult?.owner
        val preselectedVehicle = state.vehicleResult?.vehicle
        val initialVehicles = when {
            preselectedCustomer != null && preselectedCustomer.id == state.selectedCustomer?.id && state.customerVehicles.isNotEmpty() ->
                state.customerVehicles
            preselectedVehicle != null -> listOf(preselectedVehicle)
            else -> emptyList()
        }
        _uiState.update {
            it.copy(
                createAppointmentForm = CreateAppointmentForm(
                    selectedCustomer = preselectedCustomer,
                    vehicles = initialVehicles,
                    selectedVehicleId = preselectedVehicle?.id ?: initialVehicles.firstOrNull()?.id,
                    dateText = today.toString()
                ),
                error = null,
                notice = null
            )
        }
        if (preselectedCustomer != null && initialVehicles.isEmpty()) {
            loadVehiclesForAppointmentCustomer(preselectedCustomer.id)
        }
        if (state.portfolioCustomers.isEmpty() && state.canReadCustomers) {
            refreshCustomerPortfolio(force = false)
        }
    }

    fun onDismissCreateAppointment() = _uiState.update {
        if (it.createAppointmentForm?.isSaving == true) it
        else it.copy(createAppointmentForm = null, error = null)
    }

    fun onAppointmentCustomerQueryChange(query: String) = editAppointmentForm {
        it.copy(customerQuery = query)
    }

    fun onClearAppointmentCustomer() = editAppointmentForm {
        it.copy(selectedCustomer = null, vehicles = emptyList(), selectedVehicleId = null, invalidField = null)
    }

    fun onSelectAppointmentCustomer(customer: Customer) {
        editAppointmentForm {
            it.copy(
                selectedCustomer = customer,
                vehicles = emptyList(),
                isLoadingVehicles = true,
                selectedVehicleId = null,
                invalidField = null
            )
        }
        loadVehiclesForAppointmentCustomer(customer.id)
    }

    private fun loadVehiclesForAppointmentCustomer(customerId: String) {
        viewModelScope.launch {
            getCustomerVehicles(customerId)
                .onSuccess { list ->
                    _uiState.update { state ->
                        val form = state.createAppointmentForm ?: return@update state
                        if (form.selectedCustomer?.id != customerId) return@update state
                        state.copy(
                            createAppointmentForm = form.copy(
                                vehicles = list,
                                isLoadingVehicles = false,
                                selectedVehicleId = form.selectedVehicleId ?: list.firstOrNull()?.id
                            )
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { state ->
                        val form = state.createAppointmentForm ?: return@update state
                        state.copy(
                            createAppointmentForm = form.copy(isLoadingVehicles = false),
                            error = e.toReceptionUiText()
                        )
                    }
                }
        }
    }

    fun onSelectAppointmentVehicle(vehicleId: String) = editAppointmentForm {
        it.copy(selectedVehicleId = vehicleId, invalidField = null)
    }

    fun onAppointmentDateChange(dateText: String) = editAppointmentForm {
        it.copy(dateText = dateText, invalidField = null)
    }

    fun onAppointmentTimeChange(timeText: String) = editAppointmentForm {
        it.copy(timeText = timeText, invalidField = null)
    }

    fun onAppointmentDurationChange(durationText: String) = editAppointmentForm {
        it.copy(durationText = durationText.filter(Char::isDigit), invalidField = null)
    }

    fun onAppointmentReasonChange(reason: String) = editAppointmentForm {
        it.copy(reason = reason, invalidField = null)
    }

    fun onSelectAppointmentQuickDate(daysFromToday: Long) = editAppointmentForm {
        it.copy(dateText = today.plusDays(daysFromToday).toString(), invalidField = null)
    }

    private fun editAppointmentForm(change: (CreateAppointmentForm) -> CreateAppointmentForm) = _uiState.update { state ->
        val form = state.createAppointmentForm ?: return@update state
        if (form.isSaving) state else state.copy(createAppointmentForm = change(form), error = null)
    }

    fun onConfirmCreateAppointment() {
        val create = createAppointment ?: return
        val state = _uiState.value
        val form = state.createAppointmentForm ?: return
        if (!state.canCreateAppointments || !state.isOnline || form.isSaving) return

        val customerId = form.selectedCustomer?.id
        val vehicleId = form.selectedVehicleId
        if (customerId.isNullOrBlank()) {
            _uiState.update { it.copy(createAppointmentForm = form.copy(invalidField = "customerId")) }
            return
        }
        if (vehicleId.isNullOrBlank()) {
            _uiState.update { it.copy(createAppointmentForm = form.copy(invalidField = "vehicleId")) }
            return
        }
        val scheduledAt = parseLimaDateTime(form.dateText, form.timeText)
        if (scheduledAt == null) {
            _uiState.update {
                it.copy(
                    createAppointmentForm = form.copy(invalidField = "scheduledAt"),
                    error = UiText.Resource(R.string.reception_appointment_invalid_datetime)
                )
            }
            return
        }
        val duration = form.durationText.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
        if (form.durationText.isNotBlank() && duration == null) {
            _uiState.update {
                it.copy(
                    createAppointmentForm = form.copy(invalidField = "estimatedDurationMinutes"),
                    error = UiText.Resource(R.string.reception_appointment_invalid_duration)
                )
            }
            return
        }

        _uiState.update { it.copy(createAppointmentForm = form.copy(isSaving = true, invalidField = null), error = null) }
        viewModelScope.launch {
            create(
                customerId = customerId,
                vehicleId = vehicleId,
                scheduledAt = scheduledAt,
                estimatedDurationMinutes = duration,
                reason = form.reason.trim().ifBlank { null }
            )
                .onSuccess {
                    refresh()
                    _uiState.update {
                        it.copy(
                            createAppointmentForm = null,
                            notice = UiText.Resource(R.string.reception_appointment_scheduled_notice)
                        )
                    }
                }
                .onFailure { e ->
                    val field = e.invalidReceptionField()
                    val errorMsg = if (field == "estimatedDurationMinutes") {
                        UiText.Resource(R.string.reception_appointment_invalid_duration)
                    } else {
                        e.toReceptionUiText()
                    }
                    _uiState.update {
                        it.copy(
                            createAppointmentForm = it.createAppointmentForm?.copy(isSaving = false, invalidField = field),
                            error = errorMsg
                        )
                    }
                }
        }
    }

    // --- Editar contacto del cliente (`PUT /customers/{id}/contact`, `CUSTOMERS_UPDATE`) ---

    fun onOpenEditContact(customer: Customer) {
        val state = _uiState.value
        if (!state.canUpdateCustomer || !state.isOnline) return
        _uiState.update {
            it.copy(
                editContactForm = EditContactForm(
                    customerId = customer.id,
                    customerName = customer.displayName,
                    email = customer.email.orEmpty(),
                    phone = customer.phone.orEmpty()
                ),
                error = null,
                notice = null
            )
        }
    }

    fun onDismissEditContact() = _uiState.update {
        if (it.editContactForm?.isSaving == true) it
        else it.copy(editContactForm = null, error = null)
    }

    fun onEditContactEmailChange(email: String) = _uiState.update { state ->
        val form = state.editContactForm ?: return@update state
        if (form.isSaving) state else state.copy(editContactForm = form.copy(email = email, invalidField = null), error = null)
    }

    fun onEditContactPhoneChange(phone: String) = _uiState.update { state ->
        val form = state.editContactForm ?: return@update state
        if (form.isSaving) state else state.copy(editContactForm = form.copy(phone = phone, invalidField = null), error = null)
    }

    fun onConfirmEditContact() {
        val updateContact = updateCustomerContact ?: return
        val state = _uiState.value
        val form = state.editContactForm ?: return
        if (!state.canUpdateCustomer || !state.isOnline || form.isSaving) return

        _uiState.update { it.copy(editContactForm = form.copy(isSaving = true, invalidField = null), error = null) }
        viewModelScope.launch {
            updateContact(form.customerId, form.email.trim().ifBlank { null }, form.phone.trim().ifBlank { null })
                .onSuccess { updated ->
                    _uiState.update { current ->
                        current.copy(
                            editContactForm = null,
                            selectedCustomer = if (current.selectedCustomer?.id == updated.id) updated else current.selectedCustomer,
                            customers = current.customers.map { if (it.id == updated.id) updated else it },
                            vehicleResult = current.vehicleResult?.let { vr ->
                                if (vr.owner?.id == updated.id) vr.copy(owner = updated) else vr
                            },
                            notice = UiText.Resource(R.string.reception_edit_contact_notice)
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            editContactForm = it.editContactForm?.copy(isSaving = false, invalidField = e.invalidReceptionField()),
                            error = e.toReceptionUiText()
                        )
                    }
                }
        }
    }

    private fun parseLimaDateTime(dateText: String, timeText: String): Instant? = runCatching {
        val date = LocalDate.parse(dateText.trim())
        val time = LocalTime.parse(timeText.trim())
        ZonedDateTime.of(date, time, BusinessTime.ZONE).toInstant()
    }.getOrNull()
}
