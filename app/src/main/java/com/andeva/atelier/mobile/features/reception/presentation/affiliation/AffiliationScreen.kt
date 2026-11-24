package com.andeva.atelier.mobile.features.reception.presentation.affiliation

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNoPermissionState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRequiresConnection
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSectionHeader
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSurface
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.SurfaceTier
import com.andeva.atelier.mobile.core.designsystem.component.bone
import com.andeva.atelier.mobile.core.designsystem.component.floatingBarPadding
import com.andeva.atelier.mobile.core.designsystem.component.rememberAtelierHaptics
import com.andeva.atelier.mobile.core.designsystem.component.rememberDelayedLoading
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardLargeShape
import com.andeva.atelier.mobile.core.designsystem.theme.Spacing
import com.andeva.atelier.mobile.features.reception.domain.AffiliationLookup
import com.andeva.atelier.mobile.features.reception.domain.AffiliationStep
import com.andeva.atelier.mobile.features.reception.domain.EngineType
import com.andeva.atelier.mobile.features.reception.domain.PlatformAccount
import com.andeva.atelier.mobile.features.reception.domain.Vehicle

/** US44: buscar por correo si el conductor ya está en Atelier, afiliarlo con sus autos o registrarlo en el counter. */
@Composable
fun AffiliationScreen(
    onBack: () -> Unit,
    onRegisterWalkIn: (email: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AffiliationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = rememberAtelierHaptics()

    LaunchedEffect(state.affiliated?.id) {
        if (state.affiliated != null) haptics.confirm()
    }
    LaunchedEffect(state.error) {
        if (state.error != null) haptics.reject()
    }

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
    val showSkeleton = rememberDelayedLoading(state.isSearching && state.lookup == null)
    val searchingAnnouncement = stringResource(R.string.affiliation_searching_label)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { AtelierTopBar(title = stringResource(R.string.affiliation_title), onBack = onBack) },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(
                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        state.error?.let { AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline) }
                        if (!state.isOnline) AtelierRequiresConnection()
                        when {
                            state.affiliated != null || state.step == AffiliationStep.ALREADY_CUSTOMER -> Unit
                            state.lookup == null -> AtelierButton(
                                text = stringResource(R.string.affiliation_search),
                                onClick = onSearch,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = state.canSearch,
                                loading = state.isSearching
                            )
                            state.step == AffiliationStep.AFFILIATE -> AtelierButton(
                                text = stringResource(R.string.affiliation_submit),
                                onClick = onAffiliate,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = state.canSubmitAffiliation,
                                loading = state.isAffiliating
                            )
                            else -> AtelierButton(
                                text = stringResource(R.string.affiliation_register_walk_in),
                                onClick = onRegisterWalkIn,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = state.canAffiliate && state.isOnline
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .floatingBarPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.md, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(
                text = stringResource(R.string.affiliation_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = AtelierTheme.colors.inkSecondary
            )
            if (!state.canLookup) {
                AtelierNoPermissionState(stringResource(R.string.reception_no_permission))
            } else {
                AtelierSurface(
                    tier = SurfaceTier.Card,
                    shape = CardLargeShape,
                    contentPadding = PaddingValues(Spacing.md),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AtelierTextField(
                        label = stringResource(R.string.affiliation_email),
                        value = state.email,
                        onValueChange = onEmailChange,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = Icons.Outlined.Email,
                        enabled = !state.isBusy,
                        isError = state.invalidField == "email",
                        errorText = if (state.invalidField == "email") stringResource(R.string.reception_field_email) else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearch() })
                    )
                }
                if (showSkeleton) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = searchingAnnouncement },
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Text(
                            text = searchingAnnouncement,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .bone(loading = true, widthFraction = 0.7f)
                        )
                        AtelierListRow(
                            title = "Vehículo de plataforma",
                            subtitle = "Toyota Yaris 2021",
                            plate = "BKL482",
                            loading = true
                        )
                    }
                } else {
                    LookupResult(
                        state = state,
                        onTaxIdChange = onTaxIdChange,
                        onPhoneChange = onPhoneChange,
                        onAffiliate = onAffiliate
                    )
                }
            }
        }
    }
}

@Composable
private fun LookupResult(
    state: AffiliationUiState,
    onTaxIdChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onAffiliate: () -> Unit
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
            if (lookup.vehicles.isNotEmpty()) {
                AtelierSectionHeader(
                    title = stringResource(R.string.affiliation_vehicles_header),
                    count = lookup.vehicles.size
                )
                AtelierSurface(
                    tier = SurfaceTier.Card,
                    shape = CardLargeShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        lookup.vehicles.forEachIndexed { index, vehicle ->
                            AtelierListRow(
                                title = listOfNotNull(vehicle.brand, vehicle.model, vehicle.year?.toString()).joinToString(" "),
                                plate = vehicle.plate,
                                showDivider = index < lookup.vehicles.lastIndex
                            )
                        }
                    }
                }
            }
            AtelierSurface(
                tier = SurfaceTier.Card,
                shape = CardLargeShape,
                contentPadding = PaddingValues(Spacing.md),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
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
                }
            }
        }
        AffiliationStep.REGISTER_WALK_IN, null -> AtelierBanner(
            title = stringResource(R.string.affiliation_not_found_title),
            message = stringResource(R.string.affiliation_not_found_body),
            icon = Icons.Outlined.Info,
            kind = BannerKind.Wait
        )
    }
}

private val previewLookup = AffiliationLookup(
    workshopCustomer = null,
    platformAccount = PlatformAccount("u1", "ana.torres@correo.pe", "Ana Torres Quispe", "ACTIVE"),
    vehicles = listOf(
        Vehicle("v1", "BKR418", null, "Toyota", "Yaris", 2021, EngineType.GASOLINE, null, null)
    ),
    nextStep = AffiliationStep.AFFILIATE
)

@Composable
private fun AffiliationPreview(state: AffiliationUiState, dark: Boolean) = AtelierWorkshopTheme(darkTheme = dark) {
    AffiliationContent(
        state = state,
        onBack = {}, onEmailChange = {}, onTaxIdChange = {}, onPhoneChange = {},
        onSearch = {}, onAffiliate = {}, onRegisterWalkIn = {}
    )
}

@Preview(name = "Afiliar · encontrada · claro", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun AffiliationPreviewFoundLight() = AffiliationPreview(
    AffiliationUiState(email = "ana.torres@correo.pe", lookup = previewLookup, canLookup = true, canAffiliate = true),
    dark = false
)

@Preview(name = "Afiliar · encontrada · oscuro", widthDp = 390, heightDp = 844, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AffiliationPreviewFoundDark() = AffiliationPreview(
    AffiliationUiState(email = "ana.torres@correo.pe", lookup = previewLookup, canLookup = true, canAffiliate = true),
    dark = true
)

@Preview(name = "Afiliar · error · claro", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun AffiliationPreviewError() = AffiliationPreview(
    AffiliationUiState(
        email = "ana@", canLookup = true, invalidField = "email",
        error = UiText.Dynamic("Revisa los datos marcados.")
    ),
    dark = false
)

@Preview(name = "Afiliar · sin cuenta, sin red · oscuro", widthDp = 390, heightDp = 844, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AffiliationPreviewOffline() = AffiliationPreview(
    AffiliationUiState(
        email = "luis.ramos@correo.pe", canLookup = true, canAffiliate = true, isOnline = false,
        lookup = AffiliationLookup(
            workshopCustomer = null, platformAccount = null, vehicles = emptyList(),
            nextStep = AffiliationStep.REGISTER_WALK_IN
        )
    ),
    dark = true
)

@Preview(name = "Afiliar · sin permiso · claro", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun AffiliationPreviewNoPermission() = AffiliationPreview(AffiliationUiState(), dark = false)
