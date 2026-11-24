package com.andeva.atelier.mobile.features.workorders.presentation.open

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
import com.andeva.atelier.mobile.core.designsystem.component.AtelierEmptyState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNoPermissionState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRequiresConnection
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSectionHeader
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.rememberDelayedLoading
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.core.presentation.format
import com.andeva.atelier.mobile.features.workorders.domain.MechanicOption
import com.andeva.atelier.mobile.features.workorders.domain.ServiceOption
import com.andeva.atelier.mobile.features.workorders.presentation.components.ChoiceChip
import com.andeva.atelier.mobile.features.workorders.presentation.detail.components.PickRow

/**
 * US12: apertura de la orden de servicio (kilometraje, combustible, motivo y faenas). Se hace en línea.
 * [onOpened] recibe el id de la orden ya lista.
 */
@Composable
fun OpenWorkOrderScreen(
    onBack: () -> Unit,
    onOpened: (workOrderId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OpenWorkOrderViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.doneWorkOrderId) {
        state.doneWorkOrderId?.let(onOpened)
    }

    OpenWorkOrderContent(
        state = state,
        onBack = onBack,
        onMileageChange = viewModel::onMileageChange,
        onFuelChange = viewModel::onFuelChange,
        onReasonChange = viewModel::onReasonChange,
        onToggleService = viewModel::onToggleService,
        onMechanicChange = viewModel::onMechanicChange,
        onRetryCatalog = viewModel::loadCatalog,
        onSubmit = viewModel::onSubmit,
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun OpenWorkOrderContent(
    state: OpenWorkOrderUiState,
    onBack: () -> Unit,
    onMileageChange: (String) -> Unit,
    onFuelChange: (FuelLevel) -> Unit,
    onReasonChange: (String) -> Unit,
    onToggleService: (String) -> Unit,
    onMechanicChange: (String?) -> Unit,
    onRetryCatalog: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { AtelierTopBar(title = stringResource(R.string.open_order_title), onBack = onBack) },
        bottomBar = {
            if (state.hasPermission) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!state.isOnline) AtelierRequiresConnection()
                    AtelierButton(
                        text = stringResource(if (state.orderSaved) R.string.open_order_retry_tasks else R.string.open_order_submit),
                        onClick = onSubmit,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.canSubmit,
                        loading = state.isSaving
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!state.hasPermission) {
                AtelierNoPermissionState(text = stringResource(R.string.reception_no_permission))
                return@Column
            }
            if (state.isCompletingDraft) {
                AtelierBanner(
                    message = stringResource(R.string.open_order_draft_notice),
                    icon = Icons.Outlined.Info,
                    kind = BannerKind.Info
                )
            }
            if (state.orderSaved) {
                AtelierBanner(
                    message = stringResource(R.string.open_order_saved_pending_tasks),
                    icon = Icons.Outlined.Info,
                    kind = BannerKind.Info
                )
            }

            AtelierTextField(
                label = stringResource(R.string.open_order_mileage),
                value = state.mileage,
                onValueChange = onMileageChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.formLocked,
                isError = state.invalidField == "mileageIn",
                errorText = if (state.invalidField == "mileageIn") stringResource(R.string.open_order_field_mileage) else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
            )

            AtelierSectionHeader(title = stringResource(R.string.open_order_fuel))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FuelLevel.entries.forEach { level ->
                    ChoiceChip(
                        label = stringResource(level.labelRes()),
                        selected = state.fuel == level,
                        enabled = !state.formLocked,
                        onClick = { onFuelChange(level) }
                    )
                }
            }

            AtelierTextField(
                label = stringResource(R.string.open_order_reason),
                value = state.reason,
                onValueChange = onReasonChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.formLocked,
                isError = state.invalidField == "diagnosticSummary",
                errorText = if (state.invalidField == "diagnosticSummary") stringResource(R.string.open_order_field_reason) else null
            )
            if (!state.formLocked && state.reason.isBlank()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        stringResource(R.string.open_order_quick_reason_general),
                        stringResource(R.string.open_order_quick_reason_preventive)
                    ).forEach { preset ->
                        ChoiceChip(label = preset, selected = false, enabled = true, onClick = { onReasonChange(preset) })
                    }
                }
            }

            Catalog(state, onToggleService, onMechanicChange, onRetryCatalog)

            state.error?.let { AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline) }
        }
    }
}

@Composable
private fun Catalog(
    state: OpenWorkOrderUiState,
    onToggleService: (String) -> Unit,
    onMechanicChange: (String?) -> Unit,
    onRetryCatalog: () -> Unit
) {
    AtelierSectionHeader(title = stringResource(R.string.open_order_tasks))
    val loading = rememberDelayedLoading(state.isLoadingCatalog)
    when {
        state.isLoadingCatalog -> if (loading) {
            repeat(3) { AtelierListRow(title = "Cambio de aceite", subtitle = "S/ 120.00", loading = true) }
        }
        state.services.isEmpty() -> {
            AtelierEmptyState(
                text = stringResource(R.string.open_order_tasks_empty),
                actionLabel = stringResource(R.string.common_retry),
                onAction = onRetryCatalog
            )
        }
        else -> Column {
            state.services.forEach { service ->
                val added = service.id in state.addedServiceIds
                val title = if (added) stringResource(R.string.open_order_task_added, service.name) else service.name
                ServiceRow(
                    title = title,
                    subtitle = service.basePrice?.format(),
                    checked = service.id in state.selectedServiceIds || added,
                    enabled = !state.isSaving && !added,
                    onToggle = { onToggleService(service.id) }
                )
            }
        }
    }

    if (state.mechanics.isNotEmpty()) {
        AtelierSectionHeader(title = stringResource(R.string.open_order_mechanic))
        Column {
            PickRow(
                title = stringResource(R.string.open_order_mechanic_none),
                selected = state.selectedMechanicId == null,
                enabled = !state.isSaving,
                onClick = { onMechanicChange(null) }
            )
            state.mechanics.forEach { mechanic ->
                PickRow(
                    title = mechanic.displayName,
                    selected = state.selectedMechanicId == mechanic.membershipId,
                    enabled = !state.isSaving,
                    onClick = { onMechanicChange(mechanic.membershipId) }
                )
            }
        }
    }
}

/** Fila de selección múltiple: la marca es un ícono, no solo color. */
@Composable
private fun ServiceRow(title: String, subtitle: String?, checked: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = { onToggle() })
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (checked) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (checked) MaterialTheme.colorScheme.primary else AtelierTheme.colors.muted,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else AtelierTheme.colors.muted
                )
                if (subtitle != null) {
                    Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = AtelierTheme.colors.inkSecondary)
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

private fun FuelLevel.labelRes(): Int = when (this) {
    FuelLevel.RESERVE -> R.string.fuel_reserve
    FuelLevel.QUARTER -> R.string.fuel_quarter
    FuelLevel.HALF -> R.string.fuel_half
    FuelLevel.THREE_QUARTERS -> R.string.fuel_three_quarters
    FuelLevel.FULL -> R.string.fuel_full
}

// region Previews

private val previewServices = listOf(
    ServiceOption("s1", "Cambio de aceite y filtro", Money.of("120.00", Money.PEN), 45),
    ServiceOption("s2", "Revisión de frenos", Money.of("80.00", Money.PEN), 60)
)
private val previewMechanics = listOf(
    MechanicOption("m-0001", "Mecánico", "ACTIVE", name = "Andrés Villanueva"),
    MechanicOption("m-0002", "Mecánico", "ACTIVE", name = "Julio Rojas")
)

private val previewState = OpenWorkOrderUiState(
    mileage = "84500", fuel = FuelLevel.HALF, reason = "Ruido al frenar y vibración en el volante",
    services = previewServices, mechanics = previewMechanics, isLoadingCatalog = false,
    selectedServiceIds = setOf("s2"), selectedMechanicId = "m-0001", canCreate = true
)

@Composable
private fun PreviewContent(state: OpenWorkOrderUiState) {
    OpenWorkOrderContent(
        state = state,
        onBack = {}, onMileageChange = {}, onFuelChange = {}, onReasonChange = {},
        onToggleService = {}, onMechanicChange = {}, onRetryCatalog = {}, onSubmit = {}
    )
}

@Preview(name = "Abrir orden · claro", widthDp = 390, heightDp = 844)
@Composable
private fun OpenWorkOrderPreviewLight() = AtelierWorkshopTheme(darkTheme = false) { PreviewContent(previewState) }

@Preview(name = "Abrir orden · oscuro", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OpenWorkOrderPreviewDark() = AtelierWorkshopTheme(darkTheme = true) { PreviewContent(previewState) }

@Preview(name = "Abrir orden · borrador sin conexión", widthDp = 390, heightDp = 844)
@Composable
private fun OpenWorkOrderPreviewDraftOffline() = AtelierWorkshopTheme(darkTheme = false) {
    PreviewContent(previewState.copy(workOrderId = "wo1", canUpdate = true, isOnline = false, mileage = ""))
}

// endregion
