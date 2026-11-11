package com.andeva.atelier.mobile.features.reception.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierMainTopBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardShape
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

/** US11: consulta rápida en el counter por placa o documento, agenda de citas, cartera de clientes y vehículos. */
@Composable
fun VehicleSearchScreen(
    onRegisterNew: (initialPlate: String?) -> Unit,
    onAffiliate: () -> Unit,
    onOpenWorkOrder: (vehicleId: String, customerId: String, workOrderId: String?) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onOpenMenu: (() -> Unit)? = null,
    onOpenNotifications: (() -> Unit)? = null,
    onOpenSync: (() -> Unit)? = null,
    onAddVehicleForCustomer: (Customer) -> Unit = {},
    viewModel: VehicleSearchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    VehicleSearchContent(
        state = state,
        onBack = onBack,
        onOpenMenu = onOpenMenu,
        onOpenNotifications = onOpenNotifications,
        onOpenSync = onOpenSync,
        onSectionChange = viewModel::onSectionChange,
        onModeChange = viewModel::onModeChange,
        onQueryChange = viewModel::onQueryChange,
        onSearch = viewModel::onSearch,
        onSelectCustomer = viewModel::onSelectCustomer,
        onMarkArrived = viewModel::onMarkArrived,
        onRefresh = viewModel::refresh,
        onDirectoryFilterChange = viewModel::onDirectoryFilterChange,
        onRefreshPortfolio = viewModel::refreshPortfolio,
        onRegisterNew = onRegisterNew,
        onAddVehicleForCustomer = onAddVehicleForCustomer,
        onAffiliate = onAffiliate,
        onOpenWorkOrder = onOpenWorkOrder,
        modifier = modifier
    )
}

@Composable
internal fun VehicleSearchContent(
    state: VehicleSearchUiState,
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
    onBack: (() -> Unit)? = null,
    onOpenMenu: (() -> Unit)? = null,
    onOpenNotifications: (() -> Unit)? = null,
    onOpenSync: (() -> Unit)? = null,
    onSectionChange: (ReceptionSection) -> Unit = {},
    onDirectoryFilterChange: (String) -> Unit = {},
    onRefreshPortfolio: () -> Unit = {},
    onAddVehicleForCustomer: (Customer) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        if (onBack != null) {
            AtelierTopBar(title = stringResource(R.string.search_title), onBack = onBack)
        } else if (onOpenMenu != null) {
            AtelierMainTopBar(
                title = stringResource(R.string.search_title),
                onOpenMenu = onOpenMenu,
                onOpenNotifications = onOpenNotifications,
                onOpenSync = onOpenSync
            )
        }

        val sections = ReceptionSection.entries
        PrimaryTabRow(selectedTabIndex = sections.indexOf(state.section)) {
            sections.forEach { section ->
                Tab(
                    selected = state.section == section,
                    onClick = { onSectionChange(section) },
                    text = {
                        Text(
                            text = when (section) {
                                ReceptionSection.SEARCH_AND_TODAY -> stringResource(R.string.reception_tab_counter)
                                ReceptionSection.CUSTOMERS -> stringResource(
                                    R.string.reception_tab_customers_count,
                                    state.directoryCustomers.size
                                )
                                ReceptionSection.VEHICLES -> stringResource(
                                    R.string.reception_tab_vehicles_count,
                                    state.workshopVehicles.size
                                )
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            state.error?.let { AtelierBanner(message = it.asString(), icon = Icons.Filled.Error) }

            when (state.section) {
                ReceptionSection.SEARCH_AND_TODAY -> CounterSearchSection(
                    state = state,
                    onModeChange = onModeChange,
                    onQueryChange = onQueryChange,
                    onSearch = onSearch,
                    onSelectCustomer = onSelectCustomer,
                    onMarkArrived = onMarkArrived,
                    onRefresh = onRefresh,
                    onRegisterNew = onRegisterNew,
                    onAddVehicleForCustomer = onAddVehicleForCustomer,
                    onAffiliate = onAffiliate,
                    onOpenWorkOrder = onOpenWorkOrder
                )
                ReceptionSection.CUSTOMERS -> CustomersDirectorySection(
                    state = state,
                    onDirectoryFilterChange = onDirectoryFilterChange,
                    onSelectCustomer = onSelectCustomer,
                    onRefreshPortfolio = onRefreshPortfolio,
                    onRegisterNew = { onRegisterNew(null) },
                    onAddVehicleForCustomer = onAddVehicleForCustomer,
                    onOpenWorkOrder = onOpenWorkOrder
                )
                ReceptionSection.VEHICLES -> WorkshopVehiclesSection(
                    state = state,
                    onDirectoryFilterChange = onDirectoryFilterChange,
                    onRefreshPortfolio = onRefreshPortfolio,
                    onRegisterNew = { onRegisterNew(null) },
                    onOpenWorkOrder = onOpenWorkOrder
                )
            }
        }
    }
}

@Composable
private fun CounterSearchSection(
    state: VehicleSearchUiState,
    onModeChange: (SearchMode) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onSelectCustomer: (Customer) -> Unit,
    onMarkArrived: (String) -> Unit,
    onRefresh: () -> Unit,
    onRegisterNew: (String?) -> Unit,
    onAddVehicleForCustomer: (Customer) -> Unit,
    onAffiliate: () -> Unit,
    onOpenWorkOrder: (vehicleId: String, customerId: String, workOrderId: String?) -> Unit
) {
    val plateMode = state.mode == SearchMode.PLATE
    Text(
        text = stringResource(R.string.search_subtitle),
        style = MaterialTheme.typography.bodyLarge,
        color = AtelierTheme.colors.inkSecondary
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        AtelierButton(
            text = stringResource(R.string.search_mode_plate),
            onClick = { onModeChange(SearchMode.PLATE) },
            modifier = Modifier.weight(1f),
            style = if (plateMode) AtelierButtonStyle.Tonal else AtelierButtonStyle.Outlined,
            enabled = !state.isSearching
        )
        AtelierButton(
            text = stringResource(R.string.search_mode_document),
            onClick = { onModeChange(SearchMode.DOCUMENT) },
            modifier = Modifier.weight(1f),
            style = if (plateMode) AtelierButtonStyle.Outlined else AtelierButtonStyle.Tonal,
            enabled = !state.isSearching
        )
    }
    AtelierTextField(
        label = stringResource(if (plateMode) R.string.search_field_plate else R.string.search_field_document),
        value = state.query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = !state.isSearching,
        keyboardOptions = KeyboardOptions(
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
        loading = state.isSearching,
        icon = Icons.Outlined.Search
    )
    if (state.canRegister) {
        AtelierButton(
            text = stringResource(R.string.reception_quick_walk_in),
            onClick = { onRegisterNew(state.initialPlateCandidate) },
            modifier = Modifier.fillMaxWidth(),
            style = AtelierButtonStyle.Tonal,
            icon = Icons.Outlined.PersonAdd
        )
    }
    if (!state.canSearch) NoPermissionBanner()

    SearchResults(
        state = state,
        onSelectCustomer = onSelectCustomer,
        onRegisterNew = { onRegisterNew(state.initialPlateCandidate) },
        onAddVehicleForCustomer = onAddVehicleForCustomer,
        onAffiliate = onAffiliate,
        onOpenWorkOrder = onOpenWorkOrder
    )

    Appointments(state, onMarkArrived, onRefresh, onOpenWorkOrder)
}

@Composable
private fun CustomersDirectorySection(
    state: VehicleSearchUiState,
    onDirectoryFilterChange: (String) -> Unit,
    onSelectCustomer: (Customer) -> Unit,
    onRefreshPortfolio: () -> Unit,
    onRegisterNew: () -> Unit,
    onAddVehicleForCustomer: (Customer) -> Unit,
    onOpenWorkOrder: (vehicleId: String, customerId: String, workOrderId: String?) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionTitle(text = stringResource(R.string.reception_customers_directory_title))
        if (state.canRegister) {
            AtelierButton(
                text = stringResource(R.string.reception_new_customer_btn),
                onClick = onRegisterNew,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Tonal,
                icon = Icons.Outlined.PersonAdd
            )
        }
    }
    AtelierTextField(
        label = stringResource(R.string.reception_customers_filter_label),
        value = state.directoryFilter,
        onValueChange = onDirectoryFilterChange,
        modifier = Modifier.fillMaxWidth()
    )

    val customers = state.filteredDirectoryCustomers
    if (customers.isEmpty()) {
        AtelierBanner(
            message = stringResource(R.string.reception_customers_empty_directory),
            icon = Icons.Outlined.Info,
            kind = BannerKind.Info
        )
    } else {
        customers.forEach { customer ->
            val selected = state.selectedCustomer?.id == customer.id
            CustomerDirectoryCard(
                customer = customer,
                selected = selected,
                canRegister = state.canRegister,
                canOpenWorkOrder = state.canOpenWorkOrder,
                isLoadingVehicles = selected && state.isLoadingVehicles,
                vehicles = if (selected) state.customerVehicles else emptyList(),
                onSelect = { onSelectCustomer(customer) },
                onAddVehicle = { onAddVehicleForCustomer(customer) },
                onOpenWorkOrder = { vehicleId -> onOpenWorkOrder(vehicleId, customer.id, null) }
            )
        }
    }

    AtelierButton(
        text = stringResource(R.string.reception_refresh_directory),
        onClick = onRefreshPortfolio,
        modifier = Modifier.fillMaxWidth(),
        style = AtelierButtonStyle.Text,
        loading = state.isRefreshingPortfolio
    )
}

@Composable
private fun WorkshopVehiclesSection(
    state: VehicleSearchUiState,
    onDirectoryFilterChange: (String) -> Unit,
    onRefreshPortfolio: () -> Unit,
    onRegisterNew: () -> Unit,
    onOpenWorkOrder: (vehicleId: String, customerId: String, workOrderId: String?) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionTitle(text = stringResource(R.string.reception_vehicles_directory_title))
        if (state.canRegister) {
            AtelierButton(
                text = stringResource(R.string.reception_new_vehicle_btn),
                onClick = onRegisterNew,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Tonal,
                icon = Icons.Outlined.DirectionsCar
            )
        }
    }
    AtelierTextField(
        label = stringResource(R.string.reception_vehicles_filter_label),
        value = state.directoryFilter,
        onValueChange = onDirectoryFilterChange,
        modifier = Modifier.fillMaxWidth()
    )

    val vehicles = state.filteredWorkshopVehicles
    if (vehicles.isEmpty()) {
        AtelierBanner(
            message = stringResource(R.string.reception_vehicles_empty_directory),
            icon = Icons.Outlined.Info,
            kind = BannerKind.Info
        )
    } else {
        vehicles.forEach { item ->
            InfoCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.vehicle.plate,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    item.vehicle.engineType?.let { engine ->
                        AtelierChip(text = engine.name, kind = ChipKind.Info)
                    }
                }
                Text(
                    text = listOfNotNull(item.vehicle.brand, item.vehicle.model, item.vehicle.year?.toString())
                        .joinToString(" "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AtelierTheme.colors.inkSecondary
                )
                Text(
                    text = item.owner?.let { stringResource(R.string.search_vehicle_owner, it.displayName) }
                        ?: stringResource(R.string.search_vehicle_no_owner),
                    style = MaterialTheme.typography.bodySmall,
                    color = AtelierTheme.colors.muted
                )
                item.vehicle.currentMileage?.let { km ->
                    Text(
                        text = stringResource(R.string.reception_vehicle_mileage_km, km),
                        style = MaterialTheme.typography.bodySmall,
                        color = AtelierTheme.colors.muted
                    )
                }
                val customerId = item.owner?.id ?: item.vehicle.currentOwnerId
                if (state.canOpenWorkOrder && customerId != null) {
                    StartServiceButton { onOpenWorkOrder(item.vehicle.id, customerId, null) }
                }
            }
        }
    }

    AtelierButton(
        text = stringResource(R.string.reception_refresh_directory),
        onClick = onRefreshPortfolio,
        modifier = Modifier.fillMaxWidth(),
        style = AtelierButtonStyle.Text,
        loading = state.isRefreshingPortfolio
    )
}

@Composable
private fun CustomerDirectoryCard(
    customer: Customer,
    selected: Boolean,
    canRegister: Boolean,
    canOpenWorkOrder: Boolean,
    isLoadingVehicles: Boolean,
    vehicles: List<Vehicle>,
    onSelect: () -> Unit,
    onAddVehicle: () -> Unit,
    onOpenWorkOrder: (vehicleId: String) -> Unit
) = InfoCard(modifier = Modifier.clickable(onClick = onSelect)) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = customer.displayName,
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        AtelierChip(
            text = if (customer.type == CustomerType.COMPANY) "RUC" else "DNI",
            kind = if (selected) ChipKind.Info else ChipKind.Idle
        )
    }
    customer.taxId?.let {
        Text(text = it, style = MaterialTheme.typography.bodyMedium, color = AtelierTheme.colors.inkSecondary)
    }
    val contact = listOfNotNull(customer.phone, customer.email).joinToString(" · ")
    if (contact.isNotBlank()) {
        Text(text = contact, style = MaterialTheme.typography.bodySmall, color = AtelierTheme.colors.muted)
    }
    if (selected) {
        if (canRegister) {
            AtelierButton(
                text = stringResource(R.string.reception_add_vehicle_to_customer),
                onClick = onAddVehicle,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Outlined
            )
        }
        when {
            isLoadingVehicles -> Text(
                text = stringResource(R.string.search_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = AtelierTheme.colors.inkSecondary
            )
            vehicles.isEmpty() -> Text(
                text = stringResource(R.string.search_customer_vehicles_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = AtelierTheme.colors.inkSecondary
            )
            else -> vehicles.forEach { vehicle ->
                VehicleTexts(vehicle)
                if (canOpenWorkOrder) {
                    StartServiceButton { onOpenWorkOrder(vehicle.id) }
                }
            }
        }
    }
}

@Composable
private fun NoPermissionBanner() = AtelierBanner(
    message = stringResource(R.string.reception_no_permission),
    icon = Icons.Outlined.Info,
    kind = BannerKind.Wait
)

@Composable
private fun SearchResults(
    state: VehicleSearchUiState,
    onSelectCustomer: (Customer) -> Unit,
    onRegisterNew: () -> Unit,
    onAddVehicleForCustomer: (Customer) -> Unit,
    onAffiliate: () -> Unit,
    onOpenWorkOrder: (vehicleId: String, customerId: String, workOrderId: String?) -> Unit
) {
    state.vehicleResult?.let { result ->
        VehicleResultCard(result)
        val customerId = result.owner?.id ?: result.vehicle.currentOwnerId
        if (state.canOpenWorkOrder && customerId != null) {
            StartServiceButton { onOpenWorkOrder(result.vehicle.id, customerId, null) }
        }
    }

    if (state.vehicleNotFound) {
        AtelierBanner(
            title = stringResource(R.string.search_not_found_title),
            message = stringResource(R.string.search_not_found_body),
            icon = Icons.Outlined.Info,
            kind = BannerKind.Wait
        )
        NotFoundActions(state.canRegister, onRegisterNew, onAffiliate)
    }

    if (state.customersNotFound) {
        AtelierBanner(
            message = stringResource(R.string.search_customers_empty),
            icon = Icons.Outlined.Info,
            kind = BannerKind.Wait
        )
        NotFoundActions(state.canRegister, onRegisterNew, onAffiliate)
    }

    if (state.customers.size > 1) {
        SectionTitle(stringResource(R.string.search_customers_title))
        state.customers.forEach { customer ->
            CustomerRow(customer, selected = customer.id == state.selectedCustomer?.id, onClick = { onSelectCustomer(customer) })
        }
    }

    state.selectedCustomer?.let { customer ->
        if (state.customers.size == 1) CustomerRow(customer, selected = true, onClick = {})
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionTitle(stringResource(R.string.search_customer_vehicles_title, customer.displayName))
            if (state.canRegister) {
                AtelierButton(
                    text = stringResource(R.string.reception_add_vehicle_to_customer),
                    onClick = { onAddVehicleForCustomer(customer) },
                    style = AtelierButtonStyle.Text
                )
            }
        }
        when {
            state.isLoadingVehicles -> Text(
                text = stringResource(R.string.search_loading),
                style = MaterialTheme.typography.bodyLarge,
                color = AtelierTheme.colors.inkSecondary
            )
            state.customerVehicles.isEmpty() -> Text(
                text = stringResource(R.string.search_customer_vehicles_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = AtelierTheme.colors.inkSecondary
            )
            else -> state.customerVehicles.forEach { vehicle ->
                VehicleRow(vehicle)
                if (state.canOpenWorkOrder) StartServiceButton { onOpenWorkOrder(vehicle.id, customer.id, null) }
            }
        }
    }
}

@Composable
private fun StartServiceButton(onClick: () -> Unit) = AtelierButton(
    text = stringResource(R.string.search_start_service),
    onClick = onClick,
    modifier = Modifier.fillMaxWidth()
)

@Composable
private fun NotFoundActions(canRegister: Boolean, onRegisterNew: () -> Unit, onAffiliate: () -> Unit) {
    AtelierButton(
        text = stringResource(R.string.search_register),
        onClick = onRegisterNew,
        modifier = Modifier.fillMaxWidth(),
        enabled = canRegister
    )
    AtelierButton(
        text = stringResource(R.string.search_affiliate),
        onClick = onAffiliate,
        modifier = Modifier.fillMaxWidth(),
        style = AtelierButtonStyle.Outlined,
        enabled = canRegister
    )
}

@Composable
private fun Appointments(
    state: VehicleSearchUiState,
    onMarkArrived: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenWorkOrder: (vehicleId: String, customerId: String, workOrderId: String?) -> Unit
) {
    if (!state.canViewAppointments) return
    SectionTitle(stringResource(R.string.search_appointments_title))
    state.arrival?.let { arrival ->
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
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    if (state.appointments.isEmpty()) {
        Text(
            text = stringResource(R.string.search_appointments_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = AtelierTheme.colors.inkSecondary
        )
    }
    state.appointments.forEach { appointment ->
        AppointmentCard(
            appointment = appointment,
            canMarkArrival = state.canMarkArrival,
            isArriving = state.arrivingId == appointment.id,
            busy = state.arrivingId != null,
            onMarkArrived = { onMarkArrived(appointment.id) }
        )
    }
    AtelierButton(
        text = stringResource(R.string.search_appointments_refresh),
        onClick = onRefresh,
        modifier = Modifier.fillMaxWidth(),
        style = AtelierButtonStyle.Text,
        loading = state.isRefreshingAppointments
    )
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) = Text(
    text = text,
    modifier = modifier,
    style = MaterialTheme.typography.titleMedium,
    color = MaterialTheme.colorScheme.onBackground
)

@Composable
private fun InfoCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) = Column(
    modifier = modifier
        .fillMaxWidth()
        .clip(CardShape)
        .background(AtelierTheme.colors.card)
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp)
) { content() }

@Composable
private fun VehicleResultCard(result: VehicleWithOwner) = InfoCard {
    VehicleTexts(result.vehicle)
    Text(
        text = result.owner?.let { stringResource(R.string.search_vehicle_owner, it.displayName) }
            ?: stringResource(R.string.search_vehicle_no_owner),
        style = MaterialTheme.typography.bodyMedium,
        color = AtelierTheme.colors.inkSecondary
    )
}

@Composable
private fun VehicleRow(vehicle: Vehicle) = InfoCard { VehicleTexts(vehicle) }

@Composable
private fun VehicleTexts(vehicle: Vehicle) {
    Text(text = vehicle.plate, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    Text(
        text = listOfNotNull(vehicle.brand, vehicle.model, vehicle.year?.toString()).joinToString(" "),
        style = MaterialTheme.typography.bodyMedium,
        color = AtelierTheme.colors.inkSecondary
    )
}

@Composable
private fun CustomerRow(customer: Customer, selected: Boolean, onClick: () -> Unit) = InfoCard(
    modifier = Modifier.clickable(onClick = onClick)
) {
    Text(
        text = customer.displayName,
        style = MaterialTheme.typography.titleMedium,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    )
    customer.taxId?.let {
        Text(text = it, style = MaterialTheme.typography.bodyMedium, color = AtelierTheme.colors.inkSecondary)
    }
}

@Composable
private fun AppointmentCard(
    appointment: Appointment,
    canMarkArrival: Boolean,
    isArriving: Boolean,
    busy: Boolean,
    onMarkArrived: () -> Unit
) = InfoCard {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(
                R.string.search_appointment_line,
                TimeFormat.format(appointment.scheduledAt),
                appointment.plate ?: stringResource(R.string.search_appointment_unknown_plate)
            ),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        appointment.status?.let { status ->
            AtelierChip(text = stringResource(status.labelRes()), kind = status.chipKind())
        }
    }
    appointment.customerName?.let {
        Text(text = it, style = MaterialTheme.typography.bodyMedium, color = AtelierTheme.colors.inkSecondary)
    }
    appointment.reason?.let {
        Text(text = it, style = MaterialTheme.typography.bodyMedium, color = AtelierTheme.colors.muted)
    }
    val canArrive = appointment.status == AppointmentStatus.PENDING || appointment.status == AppointmentStatus.CONFIRMED
    if (canArrive) {
        AtelierButton(
            text = stringResource(R.string.search_appointment_arrived),
            onClick = onMarkArrived,
            modifier = Modifier.fillMaxWidth(),
            style = AtelierButtonStyle.Tonal,
            enabled = canMarkArrival && !busy,
            loading = isArriving
        )
    }
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

private val previewVehicle = Vehicle("v1", "ABC123", null, "Toyota", "Yaris", 2019, EngineType.GASOLINE, "c1", 42_000)
private val previewOwner = Customer("c1", CustomerType.INDIVIDUAL, "Ana Torres", "45678912", null, null, "ACTIVE")
private val previewAppointment = Appointment(
    "a1", "b1", "c1", "v1", Instant.parse("2026-10-27T15:00:00Z"), 60, "Cambio de aceite",
    AppointmentStatus.CONFIRMED, plate = "ABC123", customerName = "Ana Torres"
)

@Preview(name = "Recepción · resultado · oscuro", widthDp = 360, heightDp = 900, showBackground = true)
@Composable
private fun VehicleSearchPreviewResult() = AtelierWorkshopTheme(darkTheme = true) {
    VehicleSearchContent(
        state = VehicleSearchUiState(
            query = "ABC123", searched = true, vehicleResult = VehicleWithOwner(previewVehicle, previewOwner),
            appointments = listOf(previewAppointment),
            canSearch = true, canViewAppointments = true, canMarkArrival = true, canRegister = true,
            canOpenWorkOrder = true
        ),
        onBack = {}, onModeChange = {}, onQueryChange = {}, onSearch = {}, onSelectCustomer = {},
        onMarkArrived = {}, onRefresh = {}, onRegisterNew = {}, onAffiliate = {}, onOpenWorkOrder = { _, _, _ -> }
    )
}

@Preview(name = "Recepción · no encontrado · claro", widthDp = 360, heightDp = 900, showBackground = true)
@Composable
private fun VehicleSearchPreviewNotFound() = AtelierWorkshopTheme(darkTheme = false) {
    VehicleSearchContent(
        state = VehicleSearchUiState(
            query = "XYZ999", searched = true, canSearch = true, canRegister = true,
            error = UiText.Dynamic("Ocurrió un error. Inténtalo de nuevo.")
        ),
        onBack = {}, onModeChange = {}, onQueryChange = {}, onSearch = {}, onSelectCustomer = {},
        onMarkArrived = {}, onRefresh = {}, onRegisterNew = {}, onAffiliate = {}, onOpenWorkOrder = { _, _, _ -> }
    )
}
