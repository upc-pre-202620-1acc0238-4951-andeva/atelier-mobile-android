package com.andeva.atelier.mobile.features.reception.presentation.affiliation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.features.reception.domain.AffiliationLookup
import com.andeva.atelier.mobile.features.reception.domain.AffiliationStep
import com.andeva.atelier.mobile.features.reception.domain.PlatformAccount

/** US44: buscar por correo si el conductor ya está en Atelier, afiliarlo con sus autos o registrarlo en el counter. */
@Composable
fun AffiliationScreen(
    onBack: () -> Unit,
    onRegisterWalkIn: (email: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AffiliationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AffiliationContent(
        state = state,
        onBack = onBack,
        onEmailChange = viewModel::onEmailChange,
        onTaxIdChange = viewModel::onTaxIdChange,
        onPhoneChange = viewModel::onPhoneChange,
        onSearch = viewModel::onSearch,
        onAffiliate = viewModel::onAffiliate,
        onRegisterWalkIn = { onRegisterWalkIn(state.email.trim()) },
        modifier = modifier
    )
}

@Composable
internal fun AffiliationContent(
    state: AffiliationUiState,
    onBack: () -> Unit,
    onEmailChange: (String) -> Unit,
    onTaxIdChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onSearch: () -> Unit,
    onAffiliate: () -> Unit,
    onRegisterWalkIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        AtelierTopBar(title = stringResource(R.string.affiliation_title), onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.affiliation_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = AtelierTheme.colors.inkSecondary
            )

            AtelierTextField(
                label = stringResource(R.string.affiliation_email),
                value = state.email,
                onValueChange = onEmailChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isBusy,
                isError = state.invalidField == "email",
                errorText = if (state.invalidField == "email") stringResource(R.string.reception_field_email) else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() })
            )
            AtelierButton(
                text = stringResource(R.string.affiliation_search),
                onClick = onSearch,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.canSearch,
                loading = state.isSearching
            )
            if (!state.canLookup) {
                AtelierBanner(
                    message = stringResource(R.string.reception_no_permission),
                    icon = Icons.Outlined.Info,
                    kind = BannerKind.Wait
                )
            }

            state.error?.let { AtelierBanner(message = it.asString(), icon = Icons.Filled.Error) }

            LookupResult(
                state = state,
                onTaxIdChange = onTaxIdChange,
                onPhoneChange = onPhoneChange,
                onAffiliate = onAffiliate,
                onRegisterWalkIn = onRegisterWalkIn
            )
        }
    }
}

@Composable
private fun LookupResult(
    state: AffiliationUiState,
    onTaxIdChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onAffiliate: () -> Unit,
    onRegisterWalkIn: () -> Unit
) {
    val lookup = state.lookup ?: return
    state.affiliated?.let { customer ->
        AtelierBanner(
            message = stringResource(R.string.affiliation_done, customer.displayName),
            icon = Icons.Outlined.CheckCircle,
            kind = BannerKind.Ok
        )
        return
    }
    when (state.step) {
        AffiliationStep.ALREADY_CUSTOMER -> AtelierBanner(
            title = stringResource(R.string.affiliation_already_title),
            message = stringResource(R.string.affiliation_already_body, lookup.workshopCustomer?.displayName ?: state.email),
            icon = Icons.Outlined.CheckCircle,
            kind = BannerKind.Ok
        )
        AffiliationStep.AFFILIATE -> {
            AtelierBanner(
                title = stringResource(R.string.affiliation_found_title),
                message = stringResource(
                    R.string.affiliation_found_body,
                    lookup.platformAccount?.fullName ?: state.email,
                    lookup.vehicles.size
                ),
                icon = Icons.Outlined.Info,
                kind = BannerKind.Info
            )
            AtelierTextField(
                label = stringResource(R.string.affiliation_tax_id),
                value = state.taxId,
                onValueChange = onTaxIdChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isBusy,
                isError = state.invalidField == "taxId",
                errorText = if (state.invalidField == "taxId") stringResource(R.string.reception_field_tax_id_person) else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
            )
            AtelierTextField(
                label = stringResource(R.string.affiliation_phone),
                value = state.phone,
                onValueChange = onPhoneChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isBusy,
                isError = state.invalidField == "phone",
                errorText = if (state.invalidField == "phone") stringResource(R.string.reception_field_phone) else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onAffiliate() })
            )
            AtelierButton(
                text = stringResource(R.string.affiliation_submit),
                onClick = onAffiliate,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.canSubmitAffiliation,
                loading = state.isAffiliating
            )
            if (!state.canAffiliate) {
                AtelierBanner(
                    message = stringResource(R.string.reception_no_permission),
                    icon = Icons.Outlined.Info,
                    kind = BannerKind.Wait
                )
            }
        }
        AffiliationStep.REGISTER_WALK_IN, null -> {
            AtelierBanner(
                title = stringResource(R.string.affiliation_not_found_title),
                message = stringResource(R.string.affiliation_not_found_body),
                icon = Icons.Outlined.Info,
                kind = BannerKind.Wait
            )
            AtelierButton(
                text = stringResource(R.string.affiliation_register_walk_in),
                onClick = onRegisterWalkIn,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Tonal,
                enabled = state.canAffiliate
            )
        }
    }
}

private val previewLookup = AffiliationLookup(
    workshopCustomer = null,
    platformAccount = PlatformAccount("u1", "ana@correo.pe", "Ana Torres", "ACTIVE"),
    vehicles = emptyList(),
    nextStep = AffiliationStep.AFFILIATE
)

@Preview(name = "Afiliar · encontrada · oscuro", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun AffiliationPreviewFound() = AtelierWorkshopTheme(darkTheme = true) {
    AffiliationContent(
        state = AffiliationUiState(
            email = "ana@correo.pe", lookup = previewLookup, canLookup = true, canAffiliate = true
        ),
        onBack = {}, onEmailChange = {}, onTaxIdChange = {}, onPhoneChange = {},
        onSearch = {}, onAffiliate = {}, onRegisterWalkIn = {}
    )
}

@Preview(name = "Afiliar · error · claro", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun AffiliationPreviewError() = AtelierWorkshopTheme(darkTheme = false) {
    AffiliationContent(
        state = AffiliationUiState(
            email = "ana@", canLookup = true, invalidField = "email",
            error = UiText.Dynamic("Revisa los datos marcados."),
            affiliated = null
        ),
        onBack = {}, onEmailChange = {}, onTaxIdChange = {}, onPhoneChange = {},
        onSearch = {}, onAffiliate = {}, onRegisterWalkIn = {}
    )
}
