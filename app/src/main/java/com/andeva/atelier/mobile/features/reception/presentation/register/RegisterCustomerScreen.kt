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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNoPermissionState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRequiresConnection
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSectionHeader
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.Spacing
import com.andeva.atelier.mobile.features.reception.domain.CustomerType
import com.andeva.atelier.mobile.features.reception.domain.EngineType

/** US11 E2 y US44 E2: alta presencial del cliente y su vehículo. En línea: sin red el botón queda deshabilitado. */
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

    // safeDrawingPadding incluye el teclado: el botón de abajo sube con él.
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        AtelierTopBar(
            title = stringResource(
                if (state.presetCustomerName != null) R.string.reception_form_vehicle_title else R.string.register_customer_title
            ),
            onBack = onBack
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            if (!state.canRegister) {
                AtelierNoPermissionState(stringResource(R.string.reception_no_permission))
            }
            if (state.customerLocked) {
                AtelierBanner(
                    message = if (state.presetCustomerName != null) {
                        stringResource(R.string.register_vehicle_for_customer_banner, state.presetCustomerName)
                    } else {
                        stringResource(R.string.register_customer_saved_pending_vehicle)
                    },
                    icon = Icons.Outlined.Info,
                    kind = BannerKind.Info
                )
            }

            if (state.presetCustomerName == null) {
                AtelierSectionHeader(stringResource(R.string.register_customer_section_customer))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.fillMaxWidth()) {
                    Choice(R.string.register_customer_individual, !company, customerEnabled, Modifier.weight(1f)) {
                        actions.onKindChange(CustomerType.INDIVIDUAL)
                    }
                    Choice(R.string.register_customer_company, company, customerEnabled, Modifier.weight(1f)) {
                        actions.onKindChange(CustomerType.COMPANY)
                    }
                }
                if (company) {
                    Field(R.string.register_customer_company_name, state.companyName, actions.onCompanyNameChange, customerEnabled,
                        state.invalidField, "companyName", R.string.reception_field_company_name,
                        capitalization = KeyboardCapitalization.Words)
                } else {
                    Field(R.string.register_customer_first_name, state.firstName, actions.onFirstNameChange, customerEnabled,
                        state.invalidField, "firstName", R.string.reception_field_first_name,
                        capitalization = KeyboardCapitalization.Words)
                    Field(R.string.register_customer_last_name, state.lastName, actions.onLastNameChange, customerEnabled,
                        state.invalidField, "lastName", R.string.reception_field_last_name,
                        capitalization = KeyboardCapitalization.Words)
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
            }

            AtelierSectionHeader(stringResource(R.string.register_customer_section_vehicle))
            Field(R.string.register_vehicle_plate, state.plate, { actions.onPlateChange(it.uppercase()) }, vehicleEnabled,
                state.invalidField, "plate", R.string.reception_field_plate,
                capitalization = KeyboardCapitalization.Characters)
            Field(R.string.register_vehicle_brand, state.brand, actions.onBrandChange, vehicleEnabled,
                state.invalidField, "brand", R.string.reception_field_brand,
                capitalization = KeyboardCapitalization.Words)
            Field(R.string.register_vehicle_model, state.model, actions.onModelChange, vehicleEnabled,
                state.invalidField, "model", R.string.reception_field_model,
                capitalization = KeyboardCapitalization.Words)
            AtelierSectionHeader(stringResource(R.string.register_vehicle_engine))
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                EngineType.entries.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.fillMaxWidth()) {
                        pair.forEach { engine ->
                            Choice(engine.labelRes(), state.engineType == engine, vehicleEnabled, Modifier.weight(1f)) {
                                actions.onEngineTypeChange(engine)
                            }
                        }
                    }
                }
            }
            Field(R.string.register_vehicle_vin, state.vin, { actions.onVinChange(it.uppercase()) }, vehicleEnabled,
                state.invalidField, "vin", R.string.reception_field_vin,
                capitalization = KeyboardCapitalization.Characters)
            Field(R.string.register_vehicle_year, state.year, actions.onYearChange, vehicleEnabled,
                state.invalidField, "year", R.string.reception_field_year, KeyboardType.Number, ImeAction.Done)
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            state.error?.let { AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline) }
            if (!state.isOnline) AtelierRequiresConnection()
            AtelierButton(
                text = stringResource(
                    if (state.customerLocked) R.string.register_customer_retry_vehicle else R.string.register_customer_submit
                ),
                onClick = actions.onSubmit,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.canSubmit,
                loading = state.isSaving
            )
        }
    }
}

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
    imeAction: ImeAction = ImeAction.Next,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None
) = AtelierTextField(
    label = stringResource(label),
    value = value,
    onValueChange = onChange,
    modifier = Modifier.fillMaxWidth(),
    enabled = enabled,
    isError = invalidField == field,
    errorText = if (invalidField == field) stringResource(errorRes) else null,
    keyboardOptions = KeyboardOptions(capitalization = capitalization, keyboardType = keyboardType, imeAction = imeAction)
)

/** Opción de una elección corta (tipo de cliente, motor): la elegida va rellena y con marca, no solo con color. */
@Composable
private fun Choice(textRes: Int, selected: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) =
    AtelierButton(
        text = stringResource(textRes),
        onClick = onClick,
        modifier = modifier.semantics { this.selected = selected },
        style = if (selected) AtelierButtonStyle.Tonal else AtelierButtonStyle.Outlined,
        enabled = enabled,
        icon = if (selected) Icons.Outlined.Check else null
    )

private fun EngineType.labelRes(): Int = when (this) {
    EngineType.GASOLINE -> R.string.engine_gasoline
    EngineType.DIESEL -> R.string.engine_diesel
    EngineType.ELECTRIC -> R.string.engine_electric
    EngineType.HYBRID -> R.string.engine_hybrid
}

private val previewActions = RegisterCustomerActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})

@Composable
private fun RegisterPreview(state: RegisterCustomerUiState, dark: Boolean) = AtelierWorkshopTheme(darkTheme = dark) {
    RegisterCustomerContent(state = state, onBack = {}, actions = previewActions)
}

@Preview(name = "Registrar · persona · oscuro", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun RegisterPreviewPerson() = RegisterPreview(
    RegisterCustomerUiState(
        firstName = "Ana", lastName = "Torres Quispe", taxId = "45678912", phone = "987654321",
        plate = "BKR418", brand = "Toyota", model = "Yaris", canRegister = true
    ),
    dark = true
)

@Preview(name = "Registrar · empresa con error · claro", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun RegisterPreviewCompanyError() = RegisterPreview(
    RegisterCustomerUiState(
        kind = CustomerType.COMPANY, companyName = "Transportes Andes SAC", taxId = "2012345",
        plate = "F4T119", brand = "Hyundai", model = "H1", engineType = EngineType.DIESEL, canRegister = true,
        invalidField = "taxId", error = UiText.Dynamic("Revisa los datos marcados.")
    ),
    dark = false
)

@Preview(name = "Registrar · sin red · claro", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun RegisterPreviewOffline() = RegisterPreview(
    RegisterCustomerUiState(
        firstName = "Luis", lastName = "Ramos Huamán", taxId = "45678913",
        plate = "D2K807", brand = "Kia", model = "Rio", canRegister = true, isOnline = false
    ),
    dark = false
)

@Preview(name = "Registrar · vehículo de cliente · oscuro", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun RegisterPreviewVehicleOnly() = RegisterPreview(
    RegisterCustomerUiState(
        registeredCustomerId = "c1", presetCustomerName = "Ana Torres Quispe",
        plate = "BKR418", brand = "Toyota", model = "Yaris", canRegister = true
    ),
    dark = true
)
