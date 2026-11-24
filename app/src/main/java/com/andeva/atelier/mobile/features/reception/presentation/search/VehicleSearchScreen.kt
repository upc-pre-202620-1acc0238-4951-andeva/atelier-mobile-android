package com.andeva.atelier.mobile.features.reception.presentation.search

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierEmptyState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierErrorState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierFab
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNoPermissionState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierOfflineState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierPlate
import com.andeva.atelier.mobile.core.designsystem.component.AtelierPlateField
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRefreshBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRequiresConnection
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSectionHeader
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSegmentedControl
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSurface
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.component.PlateSize
import com.andeva.atelier.mobile.core.designsystem.component.SurfaceTier
import com.andeva.atelier.mobile.core.designsystem.component.bone
import com.andeva.atelier.mobile.core.designsystem.component.floatingBarPadding
import com.andeva.atelier.mobile.core.designsystem.component.rememberAtelierHaptics
import com.andeva.atelier.mobile.core.designsystem.component.rememberDelayedLoading
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardLargeShape
import com.andeva.atelier.mobile.core.designsystem.theme.HeroShape
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
import java.util.Locale

private val TimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(BusinessTime.ZONE)
private val DateTimeFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd MMM · HH:mm", Locale.forLanguageTag("es-PE")).withZone(BusinessTime.ZONE)

/**
 * Recibir vehículo (US11): la placa es el gesto. Debajo del campo, la ficha del vehículo con sus acciones;
 * más abajo, las citas de hoy con gestión completa y creación de citas.
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
    val haptics = rememberAtelierHaptics()

    LaunchedEffect(state.notice, state.arrival) {
        if (state.notice != null || state.arrival != null) haptics.confirm()
    }
    LaunchedEffect(state.error) {
        if (state.error != null) haptics.reject()
    }

    VehicleSearchContent(
        state = state,
        actions = VehicleSearchActions(
            onBack = onBack,
            onModeChange = viewModel::onModeChange,
            onQueryChange = viewModel::onQueryChange,
            onSearch = viewModel::onSearch,
            onSelectCustomer = viewModel::onSelectCustomer,
            onAppointmentRangeChange = viewModel::onAppointmentRangeChange,
            onOpenAppointmentActions = viewModel::onOpenAppointmentActions,
            onDismissAppointmentActions = viewModel::onDismissAppointmentActions,
            onConfirmAppointment = viewModel::onConfirmAppointment,
            onMarkArrived = viewModel::onMarkArrived,
            onRescheduleAppointment = viewModel::onRescheduleAppointment,
            onCancelAppointment = viewModel::onCancelAppointment,
            onOpenCreateAppointment = viewModel::onOpenCreateAppointment,
            onDismissCreateAppointment = viewModel::onDismissCreateAppointment,
            onAppointmentCustomerQueryChange = viewModel::onAppointmentCustomerQueryChange,
            onSelectAppointmentCustomer = viewModel::onSelectAppointmentCustomer,
            onClearAppointmentCustomer = viewModel::onClearAppointmentCustomer,
            onSelectAppointmentVehicle = viewModel::onSelectAppointmentVehicle,
            onAppointmentQuickDate = viewModel::onSelectAppointmentQuickDate,
            onAppointmentDateChange = viewModel::onAppointmentDateChange,
            onAppointmentTimeChange = viewModel::onAppointmentTimeChange,
            onAppointmentDurationChange = viewModel::onAppointmentDurationChange,
            onAppointmentReasonChange = viewModel::onAppointmentReasonChange,
            onConfirmCreateAppointment = viewModel::onConfirmCreateAppointment,
            onOpenEditContact = viewModel::onOpenEditContact,
            onDismissEditContact = viewModel::onDismissEditContact,
            onEditContactEmailChange = viewModel::onEditContactEmailChange,
            onEditContactPhoneChange = viewModel::onEditContactPhoneChange,
            onConfirmEditContact = viewModel::onConfirmEditContact,
            onRefresh = viewModel::refresh,
            onRegisterNew = onRegisterNew,
            onAddVehicleForCustomer = onAddVehicleForCustomer,
            onAffiliate = onAffiliate,
            onOpenWorkOrder = onOpenWorkOrder
        ),
        modifier = modifier
    )
}

internal class VehicleSearchActions(
    val onBack: () -> Unit = {},
    val onModeChange: (SearchMode) -> Unit = {},
    val onQueryChange: (String) -> Unit = {},
    val onSearch: () -> Unit = {},
    val onSelectCustomer: (Customer) -> Unit = {},
    val onAppointmentRangeChange: (AppointmentRange) -> Unit = {},
    val onOpenAppointmentActions: (String) -> Unit = {},
    val onDismissAppointmentActions: () -> Unit = {},
    val onConfirmAppointment: (String) -> Unit = {},
    val onMarkArrived: (String) -> Unit = {},
    val onRescheduleAppointment: (String, String, String) -> Unit = { _, _, _ -> },
    val onCancelAppointment: (String, String) -> Unit = { _, _ -> },
    val onOpenCreateAppointment: () -> Unit = {},
    val onDismissCreateAppointment: () -> Unit = {},
    val onAppointmentCustomerQueryChange: (String) -> Unit = {},
    val onSelectAppointmentCustomer: (Customer) -> Unit = {},
    val onClearAppointmentCustomer: () -> Unit = {},
    val onSelectAppointmentVehicle: (String) -> Unit = {},
    val onAppointmentQuickDate: (Long) -> Unit = {},
    val onAppointmentDateChange: (String) -> Unit = {},
    val onAppointmentTimeChange: (String) -> Unit = {},
    val onAppointmentDurationChange: (String) -> Unit = {},
    val onAppointmentReasonChange: (String) -> Unit = {},
    val onConfirmCreateAppointment: () -> Unit = {},
    val onOpenEditContact: (Customer) -> Unit = {},
    val onDismissEditContact: () -> Unit = {},
    val onEditContactEmailChange: (String) -> Unit = {},
    val onEditContactPhoneChange: (String) -> Unit = {},
    val onConfirmEditContact: () -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onRegisterNew: (String?) -> Unit = {},
    val onAddVehicleForCustomer: (Customer) -> Unit = {},
    val onAffiliate: () -> Unit = {},
    val onOpenWorkOrder: (vehicleId: String, customerId: String, workOrderId: String?) -> Unit = { _, _, _ -> }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VehicleSearchContent(
    state: VehicleSearchUiState,
    actions: VehicleSearchActions,
    modifier: Modifier = Modifier
) {
    val showAppointmentsSkeleton = rememberDelayedLoading(state.appointments.isEmpty() && state.isRefreshingAppointments)
    val showSearchSkeleton = rememberDelayedLoading(state.isSearching)
    val hasOpenSheet = state.actionAppointmentId != null || state.createAppointmentForm != null || state.editContactForm != null

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { AtelierTopBar(title = stringResource(R.string.reception_search_title), onBack = actions.onBack) },
        floatingActionButton = {
            if (state.canCreateAppointments && state.isOnline) {
                AtelierFab(
                    text = stringResource(R.string.reception_new_appointment),
                    icon = Icons.Outlined.Add,
                    onClick = actions.onOpenCreateAppointment
                )
            }
        }
    ) { padding ->
        if (!state.canSearch && !state.canViewAppointments) {
            AtelierNoPermissionState(
                text = stringResource(R.string.reception_search_no_access),
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = Spacing.md)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                AtelierRefreshBar(visible = state.isRefreshingAppointments && state.appointments.isNotEmpty())
                PullToRefreshBox(
                    isRefreshing = state.isRefreshingAppointments && state.appointments.isNotEmpty(),
                    onRefresh = actions.onRefresh,
                    modifier = Modifier.weight(1f)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .floatingBarPadding(),
                        contentPadding = PaddingValues(bottom = if (state.canCreateAppointments) 96.dp else Spacing.lg)
                    ) {
                        item(key = "search") {
                            SearchBlock(state, actions.onModeChange, actions.onQueryChange, actions.onSearch)
                        }
                        state.notice?.let { notice ->
                            item(key = "notice") {
                                AtelierBanner(
                                    message = notice.asString(),
                                    icon = Icons.Outlined.CheckCircle,
                                    kind = BannerKind.Ok,
                                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs)
                                )
                            }
                        }
                        if (!hasOpenSheet) {
                            state.error?.let { error ->
                                item(key = "error") {
                                    AtelierErrorState(
                                        message = error.asString(),
                                        onRetry = if (state.canSubmitSearch) actions.onSearch else actions.onRefresh,
                                        modifier = Modifier.padding(horizontal = Spacing.md)
                                    )
                                }
                            }
                        }
                        if (showSearchSkeleton) {
                            item(key = "search-skeleton") {
                                AtelierListRow(
                                    title = "Buscando",
                                    subtitle = "Consultando padrón",
                                    plate = "ABC123",
                                    loading = true
                                )
                            }
                        } else if (state.canSearch) {
                            results(
                                state = state,
                                onSelectCustomer = actions.onSelectCustomer,
                                onEditContact = actions.onOpenEditContact,
                                onRegisterNew = actions.onRegisterNew,
                                onAddVehicleForCustomer = actions.onAddVehicleForCustomer,
                                onAffiliate = actions.onAffiliate,
                                onOpenWorkOrder = actions.onOpenWorkOrder
                            )
                        }
                        if (state.canViewAppointments) {
                            appointments(
                                state = state,
                                showSkeleton = showAppointmentsSkeleton,
                                onRangeChange = actions.onAppointmentRangeChange,
                                onRefresh = actions.onRefresh,
                                onOpenWorkOrder = actions.onOpenWorkOrder,
                                onOpenActions = actions.onOpenAppointmentActions
                            )
                        }
                    }
                }
            }
        }
    }

    state.appointments.firstOrNull { it.id == state.actionAppointmentId }?.let { appointment ->
        AppointmentActionsSheet(
            appointment = appointment,
            state = state,
            onConfirmAppointment = { actions.onConfirmAppointment(appointment.id) },
            onMarkArrived = { actions.onMarkArrived(appointment.id) },
            onReschedule = { dateText, timeText -> actions.onRescheduleAppointment(appointment.id, dateText, timeText) },
            onCancelAppointment = { reason -> actions.onCancelAppointment(appointment.id, reason) },
            onDismiss = actions.onDismissAppointmentActions
        )
    }

    state.createAppointmentForm?.let { form ->
        CreateAppointmentSheet(
            form = form,
            customers = state.portfolioCustomers,
            isOnline = state.isOnline,
            error = state.error,
            onCustomerQueryChange = actions.onAppointmentCustomerQueryChange,
            onSelectCustomer = actions.onSelectAppointmentCustomer,
            onClearCustomer = actions.onClearAppointmentCustomer,
            onSelectVehicle = actions.onSelectAppointmentVehicle,
            onQuickDate = actions.onAppointmentQuickDate,
            onDateChange = actions.onAppointmentDateChange,
            onTimeChange = actions.onAppointmentTimeChange,
            onDurationChange = actions.onAppointmentDurationChange,
            onReasonChange = actions.onAppointmentReasonChange,
            onConfirm = actions.onConfirmCreateAppointment,
            onDismiss = actions.onDismissCreateAppointment
        )
    }

    state.editContactForm?.let { form ->
        EditCustomerContactSheet(
            form = form,
            isOnline = state.isOnline,
            error = state.error,
            onEmailChange = actions.onEditContactEmailChange,
            onPhoneChange = actions.onEditContactPhoneChange,
            onConfirm = actions.onConfirmEditContact,
            onDismiss = actions.onDismissEditContact
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
        val modeLabels = mapOf(
            SearchMode.PLATE to stringResource(R.string.premium_search_mode_plate),
            SearchMode.DOCUMENT to stringResource(R.string.premium_search_mode_document)
        )
        AtelierSurface(
            tier = SurfaceTier.Elevated,
            shape = HeroShape,
            contentPadding = PaddingValues(Spacing.md),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                AtelierSegmentedControl(
                    options = SearchMode.entries,
                    selected = state.mode,
                    onSelect = { if (!state.isSearching && it != state.mode) onModeChange(it) },
                    label = { modeLabels.getValue(it) }
                )
                if (state.mode == SearchMode.PLATE) {
                    AtelierPlateField(
                        label = stringResource(R.string.search_field_plate),
                        value = state.query,
                        onValueChange = onQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = stringResource(R.string.reception_search_plate_placeholder),
                        enabled = !state.isSearching,
                        imeAction = ImeAction.Search,
                        onImeAction = onSearch
                    )
                } else {
                    AtelierTextField(
                        label = stringResource(R.string.search_field_document),
                        value = state.query,
                        onValueChange = { onQueryChange(cleanDocumentQuery(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = Icons.Outlined.Search,
                        enabled = !state.isSearching,
                        keyboardOptions = KeyboardOptions(
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(onSearch = { onSearch() })
                    )
                }
                AtelierButton(
                    text = stringResource(R.string.search_submit),
                    onClick = onSearch,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.canSubmitSearch,
                    loading = state.isSearching
                )
            }
        }
    }
}

/** Documento en solo dígitos (DNI de 8, RUC de 11). */
private fun cleanDocumentQuery(raw: String): String = raw.filter(Char::isDigit).take(11)

private fun LazyListScope.results(
    state: VehicleSearchUiState,
    onSelectCustomer: (Customer) -> Unit,
    onEditContact: (Customer) -> Unit,
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
                canUpdateCustomer = state.canUpdateCustomer,
                isOnline = state.isOnline,
                onOpenWorkOrder = onOpenWorkOrder,
                onEditContact = onEditContact,
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
                count = state.customers.size,
                modifier = Modifier.padding(horizontal = Spacing.md)
            )
        }
        items(state.customers, key = { "customer-${it.id}" }) { customer ->
            val selected = customer.id == state.selectedCustomer?.id
            AtelierListRow(
                title = customer.displayName,
                subtitle = listOfNotNull(customer.taxId, customer.phone).joinToString(" · ").ifEmpty { null },
                onClick = { onSelectCustomer(customer) },
                trailing = {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        if (state.canUpdateCustomer && state.isOnline) {
                            AtelierButton(
                                text = stringResource(R.string.reception_edit_contact_btn),
                                onClick = { onEditContact(customer) },
                                style = AtelierButtonStyle.Text
                            )
                        }
                        if (selected) {
                            AtelierChip(text = stringResource(R.string.reception_selected), kind = ChipKind.Info)
                        }
                    }
                }
            )
        }
    }

    state.selectedCustomer?.let { customer ->
        item(key = "customer-vehicles-header") {
            AtelierSectionHeader(
                title = stringResource(R.string.search_customer_vehicles_title, customer.displayName),
                modifier = Modifier.padding(start = Spacing.md, end = Spacing.xs),
                action = {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        if (state.canUpdateCustomer && state.isOnline) {
                            AtelierButton(
                                text = stringResource(R.string.reception_edit_contact_btn),
                                onClick = { onEditContact(customer) },
                                style = AtelierButtonStyle.Text,
                                icon = Icons.Outlined.Edit
                            )
                        }
                        if (state.canRegister && state.isOnline) {
                            AtelierButton(
                                text = stringResource(R.string.reception_add_vehicle),
                                onClick = { onAddVehicleForCustomer(customer) },
                                style = AtelierButtonStyle.Text
                            )
                        }
                    }
                }
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
    canUpdateCustomer: Boolean,
    isOnline: Boolean,
    onOpenWorkOrder: (vehicleId: String, customerId: String, workOrderId: String?) -> Unit,
    onEditContact: (Customer) -> Unit,
    modifier: Modifier = Modifier
) {
    val vehicle = result.vehicle
    val owner = result.owner
    val customerId = owner?.id ?: vehicle.currentOwnerId
    AtelierSurface(
        modifier = modifier.fillMaxWidth(),
        tier = SurfaceTier.Card,
        shape = CardLargeShape,
        contentPadding = PaddingValues(Spacing.md)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            AtelierPlate(vehicle.plate, size = PlateSize.L)
            Column {
                Text(
                    text = vehicleLine(vehicle),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = owner?.displayName ?: stringResource(R.string.search_vehicle_no_owner),
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
            }
            if (owner != null && canUpdateCustomer) {
                AtelierButton(
                    text = stringResource(R.string.reception_edit_contact_btn),
                    onClick = { onEditContact(owner) },
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.Outlined,
                    enabled = isOnline,
                    icon = Icons.Outlined.Edit
                )
            }
            if ((canOpenWorkOrder || canUpdateCustomer) && !isOnline) {
                AtelierRequiresConnection()
            }
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
    showSkeleton: Boolean,
    onRangeChange: (AppointmentRange) -> Unit,
    onRefresh: () -> Unit,
    onOpenWorkOrder: (vehicleId: String, customerId: String, workOrderId: String?) -> Unit,
    onOpenActions: (String) -> Unit
) {
    item(key = "appointments-header") {
        Column(
            modifier = Modifier.padding(start = Spacing.md, end = Spacing.md, top = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            AtelierSectionHeader(
                title = stringResource(R.string.reception_appointments_section_title),
                count = state.appointments.size.takeIf { it > 0 }
            )
            val rangeLabels = mapOf(
                AppointmentRange.TODAY to stringResource(R.string.reception_appointments_range_today),
                AppointmentRange.WEEK to stringResource(R.string.reception_appointments_range_week),
                AppointmentRange.MONTH to stringResource(R.string.reception_appointments_range_month)
            )
            AtelierSegmentedControl(
                options = AppointmentRange.entries,
                selected = state.appointmentRange,
                onSelect = onRangeChange,
                label = { rangeLabels.getValue(it) }
            )
        }
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
        showSkeleton -> items(3, key = { "appointment-bone-$it" }) {
            AtelierListRow(title = "Cita", subtitle = "Cliente", plate = "ABC123", loading = true)
        }
        state.appointments.isEmpty() && state.isRefreshingAppointments -> Unit
        state.appointments.isEmpty() && !state.isOnline -> item(key = "appointments-offline") {
            AtelierOfflineState(modifier = Modifier.padding(horizontal = Spacing.md), onRetry = onRefresh)
        }
        state.appointments.isEmpty() -> item(key = "appointments-empty") {
            val emptyRes = when (state.appointmentRange) {
                AppointmentRange.TODAY -> R.string.search_appointments_empty
                AppointmentRange.WEEK -> R.string.reception_appointments_empty_week
                AppointmentRange.MONTH -> R.string.reception_appointments_empty_month
            }
            AtelierEmptyState(
                text = stringResource(emptyRes),
                modifier = Modifier.padding(horizontal = Spacing.md)
            )
        }
        else -> items(state.appointments, key = { it.id }) { appointment ->
            val canAct = state.canMarkArrival &&
                (appointment.status == AppointmentStatus.PENDING || appointment.status == AppointmentStatus.CONFIRMED)
            val status = appointment.status
            AtelierListRow(
                title = appointment.customerName ?: stringResource(R.string.reception_appointment_no_customer),
                subtitle = appointmentLine(appointment, includeDate = state.appointmentRange != AppointmentRange.TODAY),
                plate = appointment.plate,
                onClick = if (canAct) { { onOpenActions(appointment.id) } } else null,
                trailing = if (status != null) {
                    { AtelierChip(text = stringResource(status.labelRes()), kind = status.chipKind()) }
                } else null
            )
        }
    }
}

private fun vehicleLine(vehicle: Vehicle): String =
    listOfNotNull(vehicle.brand, vehicle.model, vehicle.year?.toString()).joinToString(" ")

@Composable
private fun appointmentLine(appointment: Appointment, includeDate: Boolean): String {
    val whenLabel = if (includeDate) DateTimeFormat.format(appointment.scheduledAt) else TimeFormat.format(appointment.scheduledAt)
    return appointment.reason?.let { stringResource(R.string.search_appointment_line, whenLabel, it) } ?: whenLabel
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
private val previewOwner = Customer("c1", CustomerType.INDIVIDUAL, "Ana Torres Quispe", "45678912", "ana@correo.pe", "987654321", "ACTIVE")
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
    VehicleSearchContent(state = state, actions = VehicleSearchActions())
}

private val previewAllowed = VehicleSearchUiState(
    canSearch = true,
    canReadCustomers = true,
    canUpdateCustomer = true,
    canViewAppointments = true,
    canCreateAppointments = true,
    canMarkArrival = true,
    canRegister = true,
    canOpenWorkOrder = true
)

@Preview(name = "Recibir · ficha y citas · claro", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun SearchPreviewResultLight() = SearchPreview(
    previewAllowed.copy(
        query = "BKR-418", searched = true, vehicleResult = VehicleWithOwner(previewVehicle, previewOwner),
        appointments = previewAppointments
    ),
    dark = false
)

@Preview(name = "Recibir · ficha y citas · oscuro", widthDp = 390, heightDp = 844, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SearchPreviewResultDark() = SearchPreview(
    previewAllowed.copy(
        query = "BKR-418", searched = true, vehicleResult = VehicleWithOwner(previewVehicle, previewOwner),
        appointments = previewAppointments
    ),
    dark = true
)

@Preview(name = "Recibir · sin resultado, sin red · claro", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun SearchPreviewNotFoundOffline() = SearchPreview(
    previewAllowed.copy(query = "XYZ-999", searched = true, isOnline = false, appointments = previewAppointments),
    dark = false
)

@Preview(name = "Recibir · error · claro", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun SearchPreviewError() = SearchPreview(
    previewAllowed.copy(query = "BKR-418", error = UiText.Dynamic("No se pudo consultar el vehículo. Inténtalo de nuevo.")),
    dark = false
)

@Preview(name = "Recibir · sin permiso · oscuro", widthDp = 390, heightDp = 844, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SearchPreviewNoPermission() = SearchPreview(VehicleSearchUiState(), dark = true)
