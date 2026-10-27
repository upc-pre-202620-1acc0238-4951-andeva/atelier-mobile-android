package com.andeva.atelier.mobile.features.reception.presentation.register

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
import com.andeva.atelier.mobile.features.reception.domain.CustomerType
import com.andeva.atelier.mobile.features.reception.domain.EngineType

/** US11 E2 y US44 E2: alta presencial del cliente y su vehículo. */
@Composable
fun RegisterCustomerScreen(
    onBack: () -> Unit,
    onRegistered: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RegisterCustomerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.done) {
        if (state.done) onRegistered()
    }

    RegisterCustomerContent(
        state = state,
        onBack = onBack,
        actions = RegisterCustomerActions(
            onKindChange = viewModel::onKindChange,
            onFirstNameChange = viewModel::onFirstNameChange,
            onLastNameChange = viewModel::onLastNameChange,
            onCompanyNameChange = viewModel::onCompanyNameChange,
            onTaxIdChange = viewModel::onTaxIdChange,
            onEmailChange = viewModel::onEmailChange,
            onPhoneChange = viewModel::onPhoneChange,
            onPlateChange = viewModel::onPlateChange,
            onBrandChange = viewModel::onBrandChange,
            onModelChange = viewModel::onModelChange,
            onEngineTypeChange = viewModel::onEngineTypeChange,
            onVinChange = viewModel::onVinChange,
            onYearChange = viewModel::onYearChange,
            onSubmit = viewModel::onSubmit
        ),
        modifier = modifier
    )
}

internal class RegisterCustomerActions(
    val onKindChange: (CustomerType) -> Unit,
    val onFirstNameChange: (String) -> Unit,
    val onLastNameChange: (String) -> Unit,
    val onCompanyNameChange: (String) -> Unit,
    val onTaxIdChange: (String) -> Unit,
    val onEmailChange: (String) -> Unit,
    val onPhoneChange: (String) -> Unit,
    val onPlateChange: (String) -> Unit,
    val onBrandChange: (String) -> Unit,
    val onModelChange: (String) -> Unit,
    val onEngineTypeChange: (EngineType) -> Unit,
    val onVinChange: (String) -> Unit,
    val onYearChange: (String) -> Unit,
    val onSubmit: () -> Unit
)

@Composable
internal fun RegisterCustomerContent(
    state: RegisterCustomerUiState,
    onBack: () -> Unit,
    actions: RegisterCustomerActions,
    modifier: Modifier = Modifier
) {
    val customerEnabled = !state.isSaving && !state.customerLocked
    val vehicleEnabled = !state.isSaving
    val company = state.kind == CustomerType.COMPANY

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        AtelierTopBar(title = stringResource(R.string.register_customer_title), onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!state.canRegister) {
                AtelierBanner(
                    message = stringResource(R.string.reception_no_permission),
                    icon = Icons.Outlined.Info,
                    kind = BannerKind.Wait
                )
            }
            if (state.customerLocked) {
                AtelierBanner(
                    message = stringResource(R.string.register_customer_saved_pending_vehicle),
                    icon = Icons.Outlined.Info,
                    kind = BannerKind.Info
                )
            }

            SectionTitle(R.string.register_customer_section_customer)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                KindButton(R.string.register_customer_individual, !company, customerEnabled, Modifier.weight(1f)) {
                    actions.onKindChange(CustomerType.INDIVIDUAL)
                }
                KindButton(R.string.register_customer_company, company, customerEnabled, Modifier.weight(1f)) {
                    actions.onKindChange(CustomerType.COMPANY)
                }
            }
            if (company) {
                Field(R.string.register_customer_company_name, state.companyName, actions.onCompanyNameChange, customerEnabled,
                    state.invalidField, "companyName", R.string.reception_field_company_name)
            } else {
                Field(R.string.register_customer_first_name, state.firstName, actions.onFirstNameChange, customerEnabled,
                    state.invalidField, "firstName", R.string.reception_field_first_name)
                Field(R.string.register_customer_last_name, state.lastName, actions.onLastNameChange, customerEnabled,
                    state.invalidField, "lastName", R.string.reception_field_last_name)
            }
            Field(
                label = if (company) R.string.register_customer_tax_id_company else R.string.register_customer_tax_id_person,
                value = state.taxId, onChange = actions.onTaxIdChange, enabled = customerEnabled,
                invalidField = state.invalidField, field = "taxId",
                errorRes = if (company) R.string.reception_field_tax_id_company else R.string.reception_field_tax_id_person,
                keyboardType = KeyboardType.Number
            )
            Field(R.string.register_customer_email, state.email, actions.onEmailChange, customerEnabled,
                state.invalidField, "email", R.string.reception_field_email, KeyboardType.Email)
            Field(R.string.register_customer_phone, state.phone, actions.onPhoneChange, customerEnabled,
                state.invalidField, "phone", R.string.reception_field_phone, KeyboardType.Phone)

            SectionTitle(R.string.register_customer_section_vehicle)
            Field(R.string.register_vehicle_plate, state.plate, actions.onPlateChange, vehicleEnabled,
                state.invalidField, "plate", R.string.reception_field_plate)
            Field(R.string.register_vehicle_brand, state.brand, actions.onBrandChange, vehicleEnabled,
                state.invalidField, "brand", R.string.reception_field_brand)
            Field(R.string.register_vehicle_model, state.model, actions.onModelChange, vehicleEnabled,
                state.invalidField, "model", R.string.reception_field_model)
            Text(
                text = stringResource(R.string.register_vehicle_engine),
                style = MaterialTheme.typography.labelLarge,
                color = AtelierTheme.colors.inkSecondary
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EngineType.entries.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        pair.forEach { engine ->
                            KindButton(engine.labelRes(), state.engineType == engine, vehicleEnabled, Modifier.weight(1f)) {
                                actions.onEngineTypeChange(engine)
                            }
                        }
                    }
                }
            }
            Field(R.string.register_vehicle_vin, state.vin, actions.onVinChange, vehicleEnabled,
                state.invalidField, "vin", R.string.reception_field_vin)
            Field(R.string.register_vehicle_year, state.year, actions.onYearChange, vehicleEnabled,
                state.invalidField, "year", R.string.reception_field_year, KeyboardType.Number, ImeAction.Done)

            state.error?.let { AtelierBanner(message = it.asString(), icon = Icons.Filled.Error) }
        }

        AtelierButton(
            text = stringResource(
                if (state.customerLocked) R.string.register_customer_retry_vehicle else R.string.register_customer_submit
            ),
            onClick = actions.onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            enabled = state.canSubmit,
            loading = state.isSaving
        )
    }
}

@Composable
private fun SectionTitle(textRes: Int) = Text(
    text = stringResource(textRes),
    style = MaterialTheme.typography.titleMedium,
    color = MaterialTheme.colorScheme.onBackground
)

@Composable
private fun Field(
    label: Int,
    value: String,
    onChange: (String) -> Unit,
    enabled: Boolean,
    invalidField: String?,
    field: String,
    errorRes: Int,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next
) = AtelierTextField(
    label = stringResource(label),
    value = value,
    onValueChange = onChange,
    modifier = Modifier.fillMaxWidth(),
    enabled = enabled,
    isError = invalidField == field,
    errorText = if (invalidField == field) stringResource(errorRes) else null,
    keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction)
)

/** Opción de una elección corta (tipo de cliente, motor): la elegida va rellena y las demás con borde. */
@Composable
private fun KindButton(textRes: Int, selected: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) =
    AtelierButton(
        text = stringResource(textRes),
        onClick = onClick,
        modifier = modifier,
        style = if (selected) AtelierButtonStyle.Tonal else AtelierButtonStyle.Outlined,
        enabled = enabled
    )

private fun EngineType.labelRes(): Int = when (this) {
    EngineType.GASOLINE -> R.string.engine_gasoline
    EngineType.DIESEL -> R.string.engine_diesel
    EngineType.ELECTRIC -> R.string.engine_electric
    EngineType.HYBRID -> R.string.engine_hybrid
}

private val previewActions = RegisterCustomerActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})

@Preview(name = "Registrar cliente · oscuro", widthDp = 360, heightDp = 1000, showBackground = true)
@Composable
private fun RegisterCustomerPreviewDark() = AtelierWorkshopTheme(darkTheme = true) {
    RegisterCustomerContent(
        state = RegisterCustomerUiState(
            firstName = "Ana", lastName = "Torres", taxId = "45678912",
            plate = "ABC123", brand = "Toyota", model = "Yaris", canRegister = true
        ),
        onBack = {}, actions = previewActions
    )
}

@Preview(name = "Registrar cliente · empresa · claro", widthDp = 360, heightDp = 1000, showBackground = true)
@Composable
private fun RegisterCustomerPreviewCompany() = AtelierWorkshopTheme(darkTheme = false) {
    RegisterCustomerContent(
        state = RegisterCustomerUiState(
            kind = CustomerType.COMPANY, companyName = "Transportes Andes SAC", taxId = "20123456789",
            registeredCustomerId = "c1", canRegister = true
        ),
        onBack = {}, actions = previewActions
    )
}
