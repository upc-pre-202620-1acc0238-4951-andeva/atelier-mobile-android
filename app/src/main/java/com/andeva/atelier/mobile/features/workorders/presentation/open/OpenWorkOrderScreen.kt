package com.andeva.atelier.mobile.features.workorders.presentation.open

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.features.workorders.domain.MechanicOption
import com.andeva.atelier.mobile.features.workorders.domain.ServiceOption

/** US12: formulario de apertura de la orden de servicio. [onOpened] recibe el id de la orden ya lista. */
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        AtelierTopBar(title = stringResource(R.string.open_order_title), onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!state.hasPermission) {
                AtelierBanner(
                    message = stringResource(R.string.reception_no_permission),
                    icon = Icons.Outlined.Info,
                    kind = BannerKind.Wait
                )
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

            SectionLabel(R.string.open_order_fuel)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FuelLevel.entries.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { level ->
                            AtelierButton(
                                text = stringResource(level.labelRes()),
                                onClick = { onFuelChange(level) },
                                modifier = Modifier.weight(1f),
                                style = if (state.fuel == level) AtelierButtonStyle.Tonal else AtelierButtonStyle.Outlined,
                                enabled = !state.formLocked
                            )
                        }
                        repeat(3 - row.size) { Column(modifier = Modifier.weight(1f)) {} }
                    }
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

            Catalog(state, onToggleService, onMechanicChange, onRetryCatalog)

            state.error?.let { AtelierBanner(message = it.asString(), icon = Icons.Filled.Error) }
        }

        AtelierButton(
            text = stringResource(if (state.orderSaved) R.string.open_order_retry_tasks else R.string.open_order_submit),
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            enabled = state.canSubmit,
            loading = state.isSaving
        )
    }
}

@Composable
private fun Catalog(
    state: OpenWorkOrderUiState,
    onToggleService: (String) -> Unit,
    onMechanicChange: (String?) -> Unit,
    onRetryCatalog: () -> Unit
) {
    SectionLabel(R.string.open_order_tasks)
    when {
        state.isLoadingCatalog -> Text(
            text = stringResource(R.string.search_loading),
            style = MaterialTheme.typography.bodyLarge,
            color = AtelierTheme.colors.inkSecondary
        )
        state.services.isEmpty() -> {
            Text(
                text = stringResource(R.string.open_order_tasks_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = AtelierTheme.colors.inkSecondary
            )
            AtelierButton(
                text = stringResource(R.string.branch_retry),
                onClick = onRetryCatalog,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Text
            )
        }
        else -> state.services.forEach { service ->
            val added = service.id in state.addedServiceIds
            AtelierButton(
                text = if (added) stringResource(R.string.open_order_task_added, service.name) else service.name,
                onClick = { onToggleService(service.id) },
                modifier = Modifier.fillMaxWidth(),
                style = if (service.id in state.selectedServiceIds) AtelierButtonStyle.Tonal else AtelierButtonStyle.Outlined,
                enabled = !state.isSaving && !added
            )
        }
    }

    if (state.mechanics.isNotEmpty()) {
        SectionLabel(R.string.open_order_mechanic)
        AtelierButton(
            text = stringResource(R.string.open_order_mechanic_none),
            onClick = { onMechanicChange(null) },
            modifier = Modifier.fillMaxWidth(),
            style = if (state.selectedMechanicId == null) AtelierButtonStyle.Tonal else AtelierButtonStyle.Outlined,
            enabled = !state.isSaving
        )
        state.mechanics.forEach { mechanic ->
            AtelierButton(
                text = mechanic.jobTitle ?: stringResource(R.string.open_order_mechanic_fallback, mechanic.membershipId.takeLast(4)),
                onClick = { onMechanicChange(mechanic.membershipId) },
                modifier = Modifier.fillMaxWidth(),
                style = if (state.selectedMechanicId == mechanic.membershipId) AtelierButtonStyle.Tonal else AtelierButtonStyle.Outlined,
                enabled = !state.isSaving
            )
        }
    }
}

@Composable
private fun SectionLabel(textRes: Int) = Text(
    text = stringResource(textRes),
    style = MaterialTheme.typography.titleMedium,
    color = MaterialTheme.colorScheme.onBackground
)

private fun FuelLevel.labelRes(): Int = when (this) {
    FuelLevel.RESERVE -> R.string.fuel_reserve
    FuelLevel.QUARTER -> R.string.fuel_quarter
    FuelLevel.HALF -> R.string.fuel_half
    FuelLevel.THREE_QUARTERS -> R.string.fuel_three_quarters
    FuelLevel.FULL -> R.string.fuel_full
}

private val previewServices = listOf(
    ServiceOption("s1", "Cambio de aceite", null, 45),
    ServiceOption("s2", "Revisión de frenos", null, 60)
)
private val previewMechanics = listOf(MechanicOption("m-0001", "Mecánico", "ACTIVE"))

@Preview(name = "Abrir orden · oscuro", widthDp = 360, heightDp = 1100, showBackground = true)
@Composable
private fun OpenWorkOrderPreviewDark() = AtelierWorkshopTheme(darkTheme = true) {
    OpenWorkOrderContent(
        state = OpenWorkOrderUiState(
            mileage = "42000", fuel = FuelLevel.HALF, reason = "Ruido al frenar",
            services = previewServices, mechanics = previewMechanics, isLoadingCatalog = false,
            selectedServiceIds = setOf("s1"), canCreate = true
        ),
        onBack = {}, onMileageChange = {}, onFuelChange = {}, onReasonChange = {},
        onToggleService = {}, onMechanicChange = {}, onRetryCatalog = {}, onSubmit = {}
    )
}

@Preview(name = "Abrir orden · borrador · claro", widthDp = 360, heightDp = 1100, showBackground = true)
@Composable
private fun OpenWorkOrderPreviewDraft() = AtelierWorkshopTheme(darkTheme = false) {
    OpenWorkOrderContent(
        state = OpenWorkOrderUiState(
            workOrderId = "wo1", services = previewServices, isLoadingCatalog = false, canCreate = true, canUpdate = true
        ),
        onBack = {}, onMileageChange = {}, onFuelChange = {}, onReasonChange = {},
        onToggleService = {}, onMechanicChange = {}, onRetryCatalog = {}, onSubmit = {}
    )
}
