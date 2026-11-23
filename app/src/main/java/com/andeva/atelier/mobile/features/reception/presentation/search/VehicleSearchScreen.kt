package com.andeva.atelier.mobile.features.reception.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBottomSheet
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierEmptyState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierErrorState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNoPermissionState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierOfflineState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierPlate
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRequiresConnection
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSectionHeader
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.component.PlateSize
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.PanelShape
import com.andeva.atelier.mobile.core.designsystem.theme.Spacing
import com.andeva.atelier.mobile.core.domain.BusinessTime
import com.andeva.atelier.mobile.features.reception.domain.Appointment
import com.andeva.atelier.mobile.features.reception.domain.AppointmentStatus
import com.andeva.atelier.mobile.features.reception.domain.ArrivalResult
import com.andeva.atelier.mobile.features.reception.domain.Customer
import com.andeva.atelier.mobile.features.reception.domain.CustomerType
import com.andeva.atelier.mobile.features.reception.domain.EngineType
import com.andeva.atelier.mobile.features.reception.domain.Vehicle
import com.andeva.atelier.mobile.features.reception.domain.VehicleWithOwner
import java.time.Instant
import java.time.format.DateTimeFormatter

private val TimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(BusinessTime.ZONE)

/**
 * Recibir vehículo (US11): la placa es el gesto. Debajo del campo, la ficha del vehículo con sus acciones;
 * más abajo, las citas de hoy. La orden en borrador la continúa la pantalla de la orden.
 */
@Composable
fun VehicleSearchScreen(
    onBack: () -> Unit,
    onRegisterNew: (initialPlate: String?) -> Unit,
    onAffiliate: () -> Unit,
    onOpenWorkOrder: (vehicleId: String, customerId: String, workOrderId: String?) -> Unit,
    modifier: Modifier = Modifier,
    onAddVehicleForCustomer: (Customer) -> Unit = {},
    viewModel: VehicleSearchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    VehicleSearchContent(
        state = state,
        onBack = onBack,
        onModeChange = viewModel::onModeChange,
        onQueryChange = viewModel::onQueryChange,
        onSearch = viewModel::onSearch,
        onSelectCustomer = viewModel::onSelectCustomer,
        onMarkArrived = viewModel::onMarkArrived,
        onRefresh = viewModel::refresh,
        onRegisterNew = onRegisterNew,
        onAddVehicleForCustomer = onAddVehicleForCustomer,
        onAffiliate = onAffiliate,
        onOpenWorkOrder = onOpenWorkOrder,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VehicleSearchContent(
    state: VehicleSearchUiState,
    onBack: () -> Unit,
    onModeChange: (SearchMode) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onSelectCustomer: (Customer) -> Unit,
    onMarkArrived: (String) -> Unit,
    onRefresh: () -> Unit,
    onRegisterNew: (String?) -> Unit,
    onAffiliate: () -> Unit,
    onOpenWorkOrder: (vehicleId: String, customerId: String, workOrderId: String?) -> Unit,
    modifier: Modifier = Modifier,
    onAddVehicleForCustomer: (Customer) -> Unit = {}
) {
    var arriveForId by rememberSaveable { mutableStateOf<String?>(null) }
    var arriveSubmitted by rememberSaveable { mutableStateOf(false) }
    // La hoja se cierra cuando termina de registrar la llegada sin error; si falla, el error se ve en la hoja.
    LaunchedEffect(state.arrivingId, state.error) {
        if (arriveSubmitted && state.arrivingId == null && state.error == null) {
            arriveForId = null
            arriveSubmitted = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        AtelierTopBar(title = stringResource(R.string.reception_search_title), onBack = onBack)

        if (!state.canSearch && !state.canViewAppointments) {
            AtelierNoPermissionState(
                text = stringResource(R.string.reception_search_no_access),
                modifier = Modifier.padding(horizontal = Spacing.md)
            )
        } else {
            PullToRefreshBox(
                isRefreshing = state.isRefreshingAppointments && state.appointments.isNotEmpty(),
                onRefresh = onRefresh,
                modifier = Modifier.weight(1f)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = Spacing.lg)
                ) {
                    item(key = "search") {
                        SearchBlock(state, onModeChange, onQueryChange, onSearch)
                    }
                    state.error?.let { error ->
                        item(key = "error") {
                            AtelierErrorState(
                                message = error.asString(),
                                onRetry = if (state.canSubmitSearch) onSearch else onRefresh,
                                modifier = Modifier.padding(horizontal = Spacing.md)
                            )
                        }
                    }
                    if (state.canSearch) {
                        results(state, onSelectCustomer, onRegisterNew, onAddVehicleForCustomer, onAffiliate, onOpenWorkOrder)
                    }
                    if (state.canViewAppointments) {
                        appointments(
                            state = state,
                            onRefresh = onRefresh,
                            onOpenWorkOrder = onOpenWorkOrder,
                            onArrive = { arriveForId = it }
                        )
                    }
                }
            }
        }
    }

    state.appointments.firstOrNull { it.id == arriveForId }?.let { appointment ->
        ArriveSheet(
            appointment = appointment,
            state = state,
            onConfirm = {
                arriveSubmitted = true
                onMarkArrived(appointment.id)
            },
            onDismiss = {
                arriveForId = null
                arriveSubmitted = false
            }
        )
    }
}

@Composable
private fun SearchBlock(
    state: VehicleSearchUiState,
    onModeChange: (SearchMode) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        if (!state.isOnline) {
            AtelierBanner(
                message = stringResource(R.string.reception_search_offline_cache),
                icon = Icons.Outlined.Info,
                kind = BannerKind.Info
            )
        }
        if (!state.canSearch) {
            AtelierNoPermissionState(stringResource(R.string.reception_search_no_search_permission))
            return@Column
        }
        val plateMode = state.mode == SearchMode.PLATE
        AtelierTextField(
            label = stringResource(if (plateMode) R.string.search_field_plate else R.string.search_field_document),
            value = state.query,
            onValueChange = { onQueryChange(cleanQuery(it, state.mode)) },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = Icons.Outlined.Search,
            placeholder = if (plateMode) stringResource(R.string.reception_search_plate_placeholder) else null,
            enabled = !state.isSearching,
            keyboardOptions = KeyboardOptions(
                capitalization = if (plateMode) KeyboardCapitalization.Characters else KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = if (plateMode) KeyboardType.Ascii else KeyboardType.Number,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(onSearch = { onSearch() })
        )
        AtelierButton(
            text = stringResource(R.string.search_submit),
            onClick = onSearch,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.canSubmitSearch,
            loading = state.isSearching
        )
        AtelierButton(
            text = stringResource(if (plateMode) R.string.reception_search_by_document else R.string.reception_search_by_plate),
            onClick = { onModeChange(if (plateMode) SearchMode.DOCUMENT else SearchMode.PLATE) },
            modifier = Modifier.fillMaxWidth(),
            style = AtelierButtonStyle.Text,
            enabled = !state.isSearching
        )
    }
}

/** Placa en mayúsculas y sin símbolos; el documento, solo dígitos (DNI de 8, RUC de 11). */
private fun cleanQuery(raw: String, mode: SearchMode): String = when (mode) {
    SearchMode.PLATE -> raw.uppercase().filter { it.isLetterOrDigit() || it == '-' }.take(8)
    SearchMode.DOCUMENT -> raw.filter(Char::isDigit).take(11)
}

private fun LazyListScope.results(
    state: VehicleSearchUiState,
    onSelectCustomer: (Customer) -> Unit,
    onRegisterNew: (String?) -> Unit,
    onAddVehicleForCustomer: (Customer) -> Unit,
    onAffiliate: () -> Unit,
    onOpenWorkOrder: (vehicleId: String, customerId: String, workOrderId: String?) -> Unit
) {
    state.vehicleResult?.let { result ->
        item(key = "vehicle-result") {
            VehicleFicha(
                result = result,
                canOpenWorkOrder = state.canOpenWorkOrder,
                isOnline = state.isOnline,
                onOpenWorkOrder = onOpenWorkOrder,
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
            )
        }
    }

    if (state.vehicleNotFound || state.customersNotFound) {
        item(key = "not-found") {
            NotFound(
                text = if (state.vehicleNotFound) {
                    stringResource(R.string.reception_plate_not_found, state.query.trim())
                } else {
                    stringResource(R.string.search_customers_empty)
                },
                state = state,
                onRegisterNew = { onRegisterNew(state.initialPlateCandidate) },
                onAffiliate = onAffiliate
            )
        }
    }

    if (state.customers.size > 1) {
        item(key = "customers-header") {
            AtelierSectionHeader(
                title = stringResource(R.string.search_customers_title),
                modifier = Modifier.padding(horizontal = Spacing.md)
            )
        }
        items(state.customers, key = { "customer-${it.id}" }) { customer ->
            val selected = customer.id == state.selectedCustomer?.id
            AtelierListRow(
                title = customer.displayName,
                subtitle = customer.taxId,
                onClick = { onSelectCustomer(customer) },
                trailing = if (selected) {
                    { AtelierChip(text = stringResource(R.string.reception_selected), kind = ChipKind.Info) }
                } else null
            )
        }
    }

    state.selectedCustomer?.let { customer ->
        item(key = "customer-vehicles-header") {
            AtelierSectionHeader(
                title = stringResource(R.string.search_customer_vehicles_title, customer.displayName),
                modifier = Modifier.padding(start = Spacing.md, end = Spacing.xs),
                action = if (state.canRegister && state.isOnline) {
                    {
                        AtelierButton(
                            text = stringResource(R.string.reception_add_vehicle),
                            onClick = { onAddVehicleForCustomer(customer) },
                            style = AtelierButtonStyle.Text
                        )
                    }
                } else null
            )
        }
        when {
            state.isLoadingVehicles && state.customerVehicles.isEmpty() -> items(2, key = { "vehicle-bone-$it" }) {
                AtelierListRow(title = "Vehículo", subtitle = "Marca modelo", plate = "ABC123", loading = true)
            }
            state.customerVehicles.isEmpty() -> item(key = "customer-vehicles-empty") {
                AtelierEmptyState(
                    text = stringResource(R.string.search_customer_vehicles_empty),
                    modifier = Modifier.padding(horizontal = Spacing.md)
                )
            }
            else -> items(state.customerVehicles, key = { "customer-vehicle-${it.id}" }) { vehicle ->
                AtelierListRow(
                    title = vehicleLine(vehicle),
                    plate = vehicle.plate,
                    trailing = if (state.canOpenWorkOrder) {
                        {
                            AtelierButton(
                                text = stringResource(R.string.reception_open_order),
                                onClick = { onOpenWorkOrder(vehicle.id, customer.id, null) },
                                style = AtelierButtonStyle.Text,
                                enabled = state.isOnline
                            )
                        }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun VehicleFicha(
    result: VehicleWithOwner,
    canOpenWorkOrder: Boolean,
    isOnline: Boolean,
    onOpenWorkOrder: (vehicleId: String, customerId: String, workOrderId: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val vehicle = result.vehicle
    val customerId = result.owner?.id ?: vehicle.currentOwnerId
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(PanelShape)
            .background(AtelierTheme.colors.card)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, PanelShape)
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        AtelierPlate(vehicle.plate, size = PlateSize.L)
        Column {
            Text(
                text = vehicleLine(vehicle),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = result.owner?.displayName ?: stringResource(R.string.search_vehicle_no_owner),
                style = MaterialTheme.typography.bodyMedium,
                color = AtelierTheme.colors.inkSecondary
            )
        }
        if (canOpenWorkOrder && customerId != null) {
            AtelierButton(
                text = stringResource(R.string.reception_open_order),
                onClick = { onOpenWorkOrder(vehicle.id, customerId, null) },
                modifier = Modifier.fillMaxWidth(),
                enabled = isOnline
            )
            if (!isOnline) AtelierRequiresConnection()
        }
    }
}

@Composable
private fun NotFound(
    text: String,
    state: VehicleSearchUiState,
    onRegisterNew: () -> Unit,
    onAffiliate: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
        if (state.canRegister) {
            AtelierButton(
                text = stringResource(R.string.search_register),
                onClick = onRegisterNew,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.isOnline
            )
            AtelierButton(
                text = stringResource(R.string.search_affiliate),
                onClick = onAffiliate,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Outlined,
                enabled = state.isOnline
            )
            if (!state.isOnline) AtelierRequiresConnection()
        }
    }
}

private fun LazyListScope.appointments(
    state: VehicleSearchUiState,
    onRefresh: () -> Unit,
    onOpenWorkOrder: (vehicleId: String, customerId: String, workOrderId: String?) -> Unit,
    onArrive: (String) -> Unit
) {
    item(key = "appointments-header") {
        AtelierSectionHeader(
            title = stringResource(R.string.search_appointments_title),
            modifier = Modifier.padding(start = Spacing.md, end = Spacing.md, top = Spacing.sm)
        )
    }
    state.arrival?.let { arrival ->
        item(key = "arrival") {
            Column(
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                val ready = arrival is ArrivalResult.WorkOrderReady
                AtelierBanner(
                    message = stringResource(if (ready) R.string.search_arrival_ready else R.string.search_arrival_pending),
                    icon = Icons.Outlined.CheckCircle,
                    kind = if (ready) BannerKind.Ok else BannerKind.Info
                )
                if (arrival is ArrivalResult.WorkOrderReady && state.canOpenWorkOrder) {
                    AtelierButton(
                        text = stringResource(R.string.search_complete_order),
                        onClick = {
                            onOpenWorkOrder(arrival.appointment.vehicleId, arrival.appointment.customerId, arrival.workOrderId)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.isOnline
                    )
                }
            }
        }
    }
    when {
        state.appointments.isEmpty() && state.isRefreshingAppointments -> items(2, key = { "appointment-bone-$it" }) {
            AtelierListRow(title = "Cita", subtitle = "Cliente", plate = "ABC123", loading = true)
        }
        state.appointments.isEmpty() && !state.isOnline -> item(key = "appointments-offline") {
            AtelierOfflineState(modifier = Modifier.padding(horizontal = Spacing.md), onRetry = onRefresh)
        }
        state.appointments.isEmpty() -> item(key = "appointments-empty") {
            AtelierEmptyState(
                text = stringResource(R.string.search_appointments_empty),
                modifier = Modifier.padding(horizontal = Spacing.md)
            )
        }
        else -> items(state.appointments, key = { it.id }) { appointment ->
            val canArrive = state.canMarkArrival &&
                (appointment.status == AppointmentStatus.PENDING || appointment.status == AppointmentStatus.CONFIRMED)
            val status = appointment.status
            AtelierListRow(
                title = appointment.customerName ?: stringResource(R.string.reception_appointment_no_customer),
                subtitle = appointmentLine(appointment),
                plate = appointment.plate,
                onClick = if (canArrive) { { onArrive(appointment.id) } } else null,
                trailing = if (status != null) {
                    { AtelierChip(text = stringResource(status.labelRes()), kind = status.chipKind()) }
                } else null
            )
        }
    }
}

@Composable
private fun ArriveSheet(
    appointment: Appointment,
    state: VehicleSearchUiState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AtelierBottomSheet(
        title = stringResource(R.string.reception_arrive_title),
        onDismiss = onDismiss,
        actions = {
            if (!state.isOnline) AtelierRequiresConnection()
            AtelierButton(
                text = stringResource(R.string.reception_arrive_confirm),
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.isOnline && state.arrivingId == null,
                loading = state.arrivingId == appointment.id
            )
            AtelierButton(
                text = stringResource(R.string.common_cancel),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Text
            )
        }
    ) {
        appointment.plate?.let { AtelierPlate(it, size = PlateSize.L) }
        Text(
            text = appointment.customerName ?: stringResource(R.string.reception_appointment_no_customer),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = listOfNotNull(
                stringResource(R.string.reception_arrive_time, TimeFormat.format(appointment.scheduledAt)),
                appointment.reason
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = AtelierTheme.colors.inkSecondary
        )
        state.error?.let {
            AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline)
        }
    }
}

private fun vehicleLine(vehicle: Vehicle): String =
    listOfNotNull(vehicle.brand, vehicle.model, vehicle.year?.toString()).joinToString(" ")

@Composable
private fun appointmentLine(appointment: Appointment): String {
    val time = TimeFormat.format(appointment.scheduledAt)
    return appointment.reason?.let { stringResource(R.string.search_appointment_line, time, it) } ?: time
}

private fun AppointmentStatus.labelRes(): Int = when (this) {
    AppointmentStatus.PENDING -> R.string.appointment_status_pending
    AppointmentStatus.CONFIRMED -> R.string.appointment_status_confirmed
    AppointmentStatus.ARRIVED -> R.string.appointment_status_arrived
    AppointmentStatus.CANCELED -> R.string.appointment_status_canceled
}

private fun AppointmentStatus.chipKind(): ChipKind = when (this) {
    AppointmentStatus.PENDING -> ChipKind.Wait
    AppointmentStatus.CONFIRMED -> ChipKind.Info
    AppointmentStatus.ARRIVED -> ChipKind.Ok
    AppointmentStatus.CANCELED -> ChipKind.Idle
}

private val previewVehicle = Vehicle("v1", "BKR418", null, "Toyota", "Yaris", 2019, EngineType.GASOLINE, "c1", 42_000)
private val previewOwner = Customer("c1", CustomerType.INDIVIDUAL, "Ana Torres Quispe", "45678912", null, null, "ACTIVE")
private val previewAppointments = listOf(
    Appointment(
        "a1", "b1", "c1", "v1", Instant.parse("2026-10-27T15:00:00Z"), 60, "Cambio de aceite",
        AppointmentStatus.CONFIRMED, plate = "BKR418", customerName = "Ana Torres Quispe"
    ),
    Appointment(
        "a2", "b1", "c2", "v2", Instant.parse("2026-10-27T17:30:00Z"), 90, "Frenos",
        AppointmentStatus.PENDING, plate = "F4T119", customerName = "Transportes Andes SAC"
    ),
    Appointment(
        "a3", "b1", "c3", "v3", Instant.parse("2026-10-27T14:00:00Z"), 60, null,
        AppointmentStatus.ARRIVED, plate = "D2K807", customerName = "Luis Ramos Huamán"
    )
)

@Composable
private fun SearchPreview(state: VehicleSearchUiState, dark: Boolean) = AtelierWorkshopTheme(darkTheme = dark) {
    VehicleSearchContent(
        state = state,
        onBack = {}, onModeChange = {}, onQueryChange = {}, onSearch = {}, onSelectCustomer = {},
        onMarkArrived = {}, onRefresh = {}, onRegisterNew = {}, onAffiliate = {}, onOpenWorkOrder = { _, _, _ -> }
    )
}

private val previewAllowed = VehicleSearchUiState(
    canSearch = true, canViewAppointments = true, canMarkArrival = true, canRegister = true, canOpenWorkOrder = true
)

@Preview(name = "Recibir · ficha y citas · claro", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun SearchPreviewResultLight() = SearchPreview(
    previewAllowed.copy(
        query = "BKR418", searched = true, vehicleResult = VehicleWithOwner(previewVehicle, previewOwner),
        appointments = previewAppointments
    ),
    dark = false
)

@Preview(name = "Recibir · ficha y citas · oscuro", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun SearchPreviewResultDark() = SearchPreview(
    previewAllowed.copy(
        query = "BKR418", searched = true, vehicleResult = VehicleWithOwner(previewVehicle, previewOwner),
        appointments = previewAppointments
    ),
    dark = true
)

@Preview(name = "Recibir · sin resultado, sin red · claro", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun SearchPreviewNotFoundOffline() = SearchPreview(
    previewAllowed.copy(query = "XYZ999", searched = true, isOnline = false, appointments = previewAppointments),
    dark = false
)

@Preview(name = "Recibir · cargando citas · oscuro", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun SearchPreviewLoading() = SearchPreview(
    previewAllowed.copy(isRefreshingAppointments = true),
    dark = true
)

@Preview(name = "Recibir · error · claro", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun SearchPreviewError() = SearchPreview(
    previewAllowed.copy(query = "BKR418", error = UiText.Dynamic("No se pudo consultar el vehículo. Inténtalo de nuevo.")),
    dark = false
)

@Preview(name = "Recibir · sin permiso · oscuro", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun SearchPreviewNoPermission() = SearchPreview(VehicleSearchUiState(), dark = true)
