package com.andeva.atelier.mobile.features.cashier.presentation.series

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import com.andeva.atelier.mobile.core.designsystem.component.AtelierFab
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNoPermissionState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierOfflineState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRefreshBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRequiresConnection
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSectionHeader
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSurface
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.component.SurfaceTier
import com.andeva.atelier.mobile.core.designsystem.component.bone
import com.andeva.atelier.mobile.core.designsystem.component.rememberAtelierHaptics
import com.andeva.atelier.mobile.core.designsystem.component.rememberDelayedLoading
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardLargeShape
import com.andeva.atelier.mobile.core.designsystem.theme.Spacing
import com.andeva.atelier.mobile.features.cashier.domain.FiscalSeries
import com.andeva.atelier.mobile.features.cashier.domain.TenantFiscalProfile
import com.andeva.atelier.mobile.features.cashier.domain.VoucherType
import com.andeva.atelier.mobile.features.cashier.presentation.ChoiceGrid
import com.andeva.atelier.mobile.features.cashier.presentation.labelRes

/**
 * "Series de facturación": información fiscal del taller (RUC, razón social, nombre comercial y PSE administrado)
 * más las series SUNAT de la sede (F001, B001, FC01…).
 */
@Composable
fun FiscalSeriesScreen(
    onBack: () -> Unit,
    viewModel: FiscalSeriesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = rememberAtelierHaptics()

    LaunchedEffect(state.notice) {
        if (state.notice != null) haptics.confirm()
    }
    LaunchedEffect(state.sheetError) {
        if (state.sheetError != null) haptics.reject()
    }

    FiscalSeriesContent(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onAdd = viewModel::onAdd,
        onOpenSeries = viewModel::onOpenSeries,
        onType = viewModel::onType,
        onSerie = viewModel::onSerie,
        onInitial = viewModel::onInitial,
        onSave = viewModel::onSave,
        onToggleActive = viewModel::onToggleActive,
        onEditTenant = viewModel::onEditTenant,
        onTenantName = viewModel::onTenantName,
        onTenantLegalName = viewModel::onTenantLegalName,
        onSaveTenant = viewModel::onSaveTenant,
        onDismissSheet = viewModel::onDismissSheet
    )
}

@Composable
private fun FiscalSeriesContent(
    state: FiscalSeriesUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onAdd: () -> Unit,
    onOpenSeries: (FiscalSeries) -> Unit,
    onType: (VoucherType) -> Unit,
    onSerie: (String) -> Unit,
    onInitial: (String) -> Unit,
    onSave: () -> Unit,
    onToggleActive: () -> Unit,
    onEditTenant: () -> Unit = {},
    onTenantName: (String) -> Unit = {},
    onTenantLegalName: (String) -> Unit = {},
    onSaveTenant: () -> Unit = {},
    onDismissSheet: () -> Unit
) {
    val showSkeleton = rememberDelayedLoading(state.showSkeleton && state.hasAccess)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { AtelierTopBar(title = stringResource(R.string.cashier_series_title), onBack = onBack) },
        floatingActionButton = {
            if (state.canManage) {
                AtelierFab(
                    text = stringResource(R.string.cashier_series_add),
                    icon = Icons.Outlined.Add,
                    onClick = onAdd
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            AtelierRefreshBar(visible = state.isRefreshing && state.hasLoaded)
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
                if (!state.hasAccess) {
                    item(key = "no-access") {
                        AtelierNoPermissionState(
                            text = stringResource(R.string.cashier_series_no_access),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    return@LazyColumn
                }
                state.notice?.let { notice ->
                    item(key = "notice") {
                        AtelierBanner(
                            message = notice.asString(),
                            icon = Icons.Outlined.CheckCircle,
                            kind = BannerKind.Ok,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                        )
                    }
                }
                if (state.canReadTenant || state.canUpdateTenant || state.tenantProfile != null) {
                    item(key = "tenant-fiscal-header") {
                        AtelierSectionHeader(
                            title = stringResource(R.string.cashier_fiscal_tenant_section),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                    item(key = "tenant-fiscal-card") {
                        TenantFiscalSectionCard(
                            profile = state.tenantProfile,
                            canUpdateTenant = state.canUpdateTenant,
                            isOnline = state.isOnline,
                            onEdit = onEditTenant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
                item(key = "pse-managed-card") {
                    PseManagedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                if (state.canManage) {
                    item(key = "series-section-header") {
                        AtelierSectionHeader(
                            title = stringResource(R.string.cashier_series_branch_section),
                            count = state.series.size,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                    item(key = "hint") {
                        Text(
                            text = stringResource(R.string.cashier_series_nc_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AtelierTheme.colors.inkSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
                when {
                    showSkeleton -> items(3, key = { "bone-$it" }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "B001",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.bone(loading = true, widthFraction = 0.25f)
                                )
                                Text(
                                    text = "Boleta · Último número",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.bone(loading = true, widthFraction = 0.6f)
                                )
                            }
                            Text(
                                text = "Activa",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.bone(loading = true)
                            )
                        }
                    }
                    state.showSkeleton -> Unit
                    state.error != null && state.series.isEmpty() -> item(key = "error") {
                        if (!state.isOnline) {
                            AtelierOfflineState(modifier = Modifier.padding(horizontal = 16.dp), onRetry = onRefresh)
                        } else {
                            AtelierErrorState(
                                message = state.error.asString(),
                                modifier = Modifier.padding(horizontal = 16.dp),
                                onRetry = onRefresh
                            )
                        }
                    }
                    state.series.isEmpty() && state.canManage -> item(key = "empty") {
                        AtelierEmptyState(
                            text = stringResource(R.string.cashier_series_empty),
                            modifier = Modifier.padding(horizontal = 16.dp),
                            actionLabel = stringResource(R.string.cashier_series_add),
                            onAction = onAdd
                        )
                    }
                    else -> items(state.series, key = { it.id }) { series ->
                        AtelierListRow(
                            title = series.serie,
                            subtitle = listOf(
                                stringResource(series.type.labelRes()),
                                stringResource(R.string.cashier_series_correlative, series.currentCorrelative)
                            ).joinToString(" · "),
                            onClick = { onOpenSeries(series) },
                            trailing = {
                                if (series.active) {
                                    AtelierChip(stringResource(R.string.cashier_series_active), kind = ChipKind.Ok)
                                } else {
                                    AtelierChip(stringResource(R.string.cashier_series_inactive), kind = ChipKind.Idle)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
    SeriesSheets(
        state = state,
        onType = onType,
        onSerie = onSerie,
        onInitial = onInitial,
        onSave = onSave,
        onToggleActive = onToggleActive,
        onTenantName = onTenantName,
        onTenantLegalName = onTenantLegalName,
        onSaveTenant = onSaveTenant,
        onDismiss = onDismissSheet
    )
}

@Composable
private fun TenantFiscalSectionCard(
    profile: TenantFiscalProfile?,
    canUpdateTenant: Boolean,
    isOnline: Boolean,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    AtelierSurface(
        tier = SurfaceTier.Card,
        shape = CardLargeShape,
        contentPadding = PaddingValues(Spacing.md),
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            if (profile != null) {
                Text(
                    text = profile.legalName.ifBlank { profile.name },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (profile.name.isNotBlank() && profile.name != profile.legalName) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AtelierTheme.colors.inkSecondary
                    )
                }
                if (profile.taxId.isNotBlank()) {
                    Text(
                        text = "${stringResource(R.string.cashier_doc_ruc)}: ${profile.taxId}",
                        style = MaterialTheme.typography.labelLarge,
                        color = AtelierTheme.colors.primaryText
                    )
                }
            }
            if (canUpdateTenant) {
                AtelierButton(
                    text = stringResource(R.string.cashier_fiscal_tenant_edit),
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
                    style = AtelierButtonStyle.Tonal,
                    enabled = isOnline
                )
            }
        }
    }
}

@Composable
private fun PseManagedCard(modifier: Modifier = Modifier) {
    AtelierSurface(
        tier = SurfaceTier.Card,
        shape = CardLargeShape,
        contentPadding = PaddingValues(Spacing.md),
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.cashier_fiscal_pse_managed_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                AtelierChip(text = stringResource(R.string.cashier_series_active), kind = ChipKind.Ok)
            }
            Text(
                text = stringResource(R.string.cashier_fiscal_pse_managed_body),
                style = MaterialTheme.typography.bodySmall,
                color = AtelierTheme.colors.inkSecondary
            )
        }
    }
}

@Composable
private fun SeriesSheets(
    state: FiscalSeriesUiState,
    onType: (VoucherType) -> Unit,
    onSerie: (String) -> Unit,
    onInitial: (String) -> Unit,
    onSave: () -> Unit,
    onToggleActive: () -> Unit,
    onTenantName: (String) -> Unit,
    onTenantLegalName: (String) -> Unit,
    onSaveTenant: () -> Unit,
    onDismiss: () -> Unit
) {
    when (val sheet = state.sheet) {
        is SeriesSheet.Create -> AtelierBottomSheet(
            title = stringResource(R.string.cashier_series_new_title),
            onDismiss = onDismiss,
            actions = {
                AtelierButton(
                    text = stringResource(R.string.cashier_series_save),
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = sheet.form.canSubmit && state.isOnline,
                    loading = state.isSaving
                )
                if (!state.isOnline) AtelierRequiresConnection()
                CancelButton(onDismiss, enabled = !state.isSaving)
            }
        ) {
            SeriesFormContent(sheet.form, state.sheetError?.asString(), onType, onSerie, onInitial)
        }
        is SeriesSheet.Detail -> AtelierBottomSheet(
            title = stringResource(R.string.cashier_series_detail_title, sheet.series.serie),
            onDismiss = onDismiss,
            actions = {
                AtelierButton(
                    text = stringResource(
                        if (sheet.series.active) R.string.cashier_series_deactivate else R.string.cashier_series_activate
                    ),
                    onClick = onToggleActive,
                    modifier = Modifier.fillMaxWidth(),
                    style = if (sheet.series.active) AtelierButtonStyle.Destructive else AtelierButtonStyle.Filled,
                    enabled = state.isOnline,
                    loading = state.isSaving
                )
                if (!state.isOnline) AtelierRequiresConnection()
                CancelButton(onDismiss, enabled = !state.isSaving)
            }
        ) {
            SeriesDetailContent(sheet.series, state.sheetError?.asString())
        }
        is SeriesSheet.EditTenant -> AtelierBottomSheet(
            title = stringResource(R.string.cashier_fiscal_tenant_section),
            onDismiss = onDismiss,
            actions = {
                AtelierButton(
                    text = stringResource(R.string.cashier_fiscal_tenant_save),
                    onClick = onSaveTenant,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = sheet.form.canSubmit && state.isOnline,
                    loading = state.isSaving
                )
                if (!state.isOnline) AtelierRequiresConnection()
                CancelButton(onDismiss, enabled = !state.isSaving)
            }
        ) {
            if (sheet.form.taxId.isNotBlank()) {
                AtelierTextField(
                    label = stringResource(R.string.cashier_fiscal_tenant_ruc),
                    value = sheet.form.taxId,
                    onValueChange = {},
                    enabled = false
                )
            }
            AtelierTextField(
                label = stringResource(R.string.cashier_fiscal_tenant_legal_name),
                value = sheet.form.legalName,
                onValueChange = onTenantLegalName,
                isError = sheet.form.legalName.isNotEmpty() && !sheet.form.legalNameValid,
                errorText = stringResource(R.string.cashier_err_tenant_name).takeIf {
                    sheet.form.legalName.isNotEmpty() && !sheet.form.legalNameValid
                }
            )
            AtelierTextField(
                label = stringResource(R.string.cashier_fiscal_tenant_name),
                value = sheet.form.name,
                onValueChange = onTenantName,
                isError = sheet.form.name.isNotEmpty() && !sheet.form.nameValid,
                errorText = stringResource(R.string.cashier_err_tenant_name).takeIf {
                    sheet.form.name.isNotEmpty() && !sheet.form.nameValid
                }
            )
            state.sheetError?.let { AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline) }
        }
        null -> Unit
    }
}

@Composable
private fun CancelButton(onClick: () -> Unit, enabled: Boolean) {
    AtelierButton(
        text = stringResource(R.string.common_cancel),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        style = AtelierButtonStyle.Text,
        enabled = enabled
    )
}

@Composable
private fun SeriesFormContent(
    form: SeriesForm,
    error: String?,
    onType: (VoucherType) -> Unit,
    onSerie: (String) -> Unit,
    onInitial: (String) -> Unit
) {
    Text(
        text = stringResource(R.string.cashier_series_type),
        style = MaterialTheme.typography.titleSmall,
        color = AtelierTheme.colors.inkSecondary
    )
    ChoiceGrid(
        options = listOf(VoucherType.BOLETA, VoucherType.FACTURA, VoucherType.NOTA_CREDITO),
        selected = form.type,
        label = { stringResource(it.labelRes()) },
        onSelect = onType,
        columns = 3
    )
    AtelierTextField(
        label = stringResource(R.string.cashier_series_serie),
        value = form.serie,
        onValueChange = onSerie,
        placeholder = when (form.type) {
            VoucherType.FACTURA -> "F001"
            VoucherType.BOLETA -> "B001"
            VoucherType.NOTA_CREDITO -> "FC01"
        },
        isError = form.serie.length == 4 && !form.serieValid,
        errorText = stringResource(
            if (form.type == VoucherType.NOTA_CREDITO) R.string.cashier_nc_serie_error else R.string.cashier_series_serie_error
        ).takeIf { form.serie.length == 4 && !form.serieValid }
    )
    AtelierTextField(
        label = stringResource(R.string.cashier_series_initial),
        value = form.initial,
        onValueChange = onInitial,
        isError = !form.initialValid,
        errorText = stringResource(R.string.cashier_series_initial_error).takeIf { !form.initialValid },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
    error?.let { AtelierBanner(message = it, icon = Icons.Outlined.ErrorOutline) }
}

@Composable
private fun SeriesDetailContent(series: FiscalSeries, error: String?) {
    Text(
        text = listOf(
            stringResource(series.type.labelRes()),
            stringResource(R.string.cashier_series_correlative, series.currentCorrelative)
        ).joinToString(" · "),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface
    )
    Text(
        text = stringResource(if (series.active) R.string.cashier_series_deactivate_body else R.string.cashier_series_activate_body),
        style = MaterialTheme.typography.bodyLarge,
        color = AtelierTheme.colors.inkSecondary
    )
    error?.let { AtelierBanner(message = it, icon = Icons.Outlined.ErrorOutline) }
}

// region Previews

private val previewSeries = listOf(
    FiscalSeries("s1", "b1", VoucherType.FACTURA, "F001", 128, true),
    FiscalSeries("s2", "b1", VoucherType.BOLETA, "B001", 944, true),
    FiscalSeries("s3", "b1", VoucherType.BOLETA, "B002", 0, false)
)

private val previewState = FiscalSeriesUiState(series = previewSeries, hasLoaded = true, canManage = true)

@Composable
private fun PreviewContent(state: FiscalSeriesUiState) {
    FiscalSeriesContent(
        state = state,
        onBack = {},
        onRefresh = {},
        onAdd = {},
        onOpenSeries = {},
        onType = {},
        onSerie = {},
        onInitial = {},
        onSave = {},
        onToggleActive = {},
        onDismissSheet = {}
    )
}

@Preview(name = "Series · claro", widthDp = 390, heightDp = 844)
@Composable
private fun SeriesPreviewLight() = AtelierWorkshopTheme(darkTheme = false) { PreviewContent(previewState) }

@Preview(name = "Series · oscuro", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SeriesPreviewDark() = AtelierWorkshopTheme(darkTheme = true) { PreviewContent(previewState) }

@Preview(name = "Series · cargando", widthDp = 390, heightDp = 844)
@Composable
private fun SeriesPreviewLoading() = AtelierWorkshopTheme(darkTheme = false) {
    PreviewContent(previewState.copy(series = emptyList(), hasLoaded = false))
}

@Preview(name = "Series · vacío", widthDp = 390, heightDp = 844)
@Composable
private fun SeriesPreviewEmpty() = AtelierWorkshopTheme(darkTheme = false) {
    PreviewContent(previewState.copy(series = emptyList()))
}

// endregion
