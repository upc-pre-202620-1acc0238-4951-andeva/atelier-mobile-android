package com.andeva.atelier.mobile.features.cashier.presentation.series

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.getValue
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
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.features.cashier.domain.FiscalSeries
import com.andeva.atelier.mobile.features.cashier.domain.VoucherType
import com.andeva.atelier.mobile.features.cashier.presentation.ChoiceGrid
import com.andeva.atelier.mobile.features.cashier.presentation.labelRes

/**
 * "Series de facturación": las series SUNAT de la sede (F001, B001…). Solo quien administra la facturación las ve y
 * las cambia. Agregar y activar o desactivar se decide en hoja inferior; sin red, deshabilitado.
 */
@Composable
fun FiscalSeriesScreen(
    onBack: () -> Unit,
    viewModel: FiscalSeriesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
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
    onDismissSheet: () -> Unit
) {
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
                if (!state.canManage) {
                    item(key = "no-access") {
                        AtelierNoPermissionState(
                            text = stringResource(R.string.cashier_series_no_access),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    return@LazyColumn
                }
                item(key = "hint") {
                    Text(
                        text = stringResource(R.string.cashier_series_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = AtelierTheme.colors.inkSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
                state.notice?.let { notice ->
                    item(key = "notice") {
                        AtelierBanner(
                            message = notice.asString(),
                            icon = Icons.Outlined.CheckCircle,
                            kind = BannerKind.Ok,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                        )
                    }
                }
                when {
                    state.showSkeleton -> items(3, key = { "bone-$it" }) {
                        AtelierListRow(title = "B001", subtitle = "Boleta · Último número", loading = true)
                    }
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
                    state.series.isEmpty() -> item(key = "empty") {
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
    SeriesSheets(state, onType, onSerie, onInitial, onSave, onToggleActive, onDismissSheet)
}

@Composable
private fun SeriesSheets(
    state: FiscalSeriesUiState,
    onType: (VoucherType) -> Unit,
    onSerie: (String) -> Unit,
    onInitial: (String) -> Unit,
    onSave: () -> Unit,
    onToggleActive: () -> Unit,
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
        options = listOf(VoucherType.BOLETA, VoucherType.FACTURA),
        selected = form.type,
        label = { stringResource(it.labelRes()) },
        onSelect = onType,
        columns = 2
    )
    AtelierTextField(
        label = stringResource(R.string.cashier_series_serie),
        value = form.serie,
        onValueChange = onSerie,
        placeholder = if (form.type == VoucherType.FACTURA) "F001" else "B001",
        isError = form.serie.length == 4 && !form.serieValid,
        errorText = stringResource(R.string.cashier_series_serie_error).takeIf { form.serie.length == 4 && !form.serieValid }
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
    FiscalSeriesContent(state, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
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
