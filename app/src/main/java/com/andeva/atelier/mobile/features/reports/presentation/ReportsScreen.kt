package com.andeva.atelier.mobile.features.reports.presentation

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierAdaptiveRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBottomSheet
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierEmptyState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierErrorState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNoPermissionState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierOfflineState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierPlanLockedState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRefreshBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierReportPanel
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRequiresConnection
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSectionHeader
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSegmentedControl
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSurface
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.PanelFigure
import com.andeva.atelier.mobile.core.designsystem.component.SurfaceTier
import com.andeva.atelier.mobile.core.designsystem.component.rememberAtelierHaptics
import com.andeva.atelier.mobile.core.designsystem.component.rememberDelayedLoading
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardLargeShape
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.core.presentation.DocumentIntents
import com.andeva.atelier.mobile.core.presentation.format
import com.andeva.atelier.mobile.core.presentation.formatLimaDate
import com.andeva.atelier.mobile.features.reception.domain.Customer
import com.andeva.atelier.mobile.features.reports.domain.CashFlowMovement
import com.andeva.atelier.mobile.features.reports.domain.CashFlowSummary
import com.andeva.atelier.mobile.features.reports.domain.ReportPeriod
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private const val PLACEHOLDER_TOTAL = "S/ 0.00"

private enum class PeriodTab { THIS_MONTH, PREVIOUS_MONTH, THIS_YEAR, CUSTOM }

private fun PeriodChoice.toTab(): PeriodTab = when (this) {
    PeriodChoice.ThisMonth -> PeriodTab.THIS_MONTH
    PeriodChoice.PreviousMonth -> PeriodTab.PREVIOUS_MONTH
    PeriodChoice.ThisYear -> PeriodTab.THIS_YEAR
    is PeriodChoice.Custom -> PeriodTab.CUSTOM
}

/**
 * "Reportes": el flujo de caja del taller en un periodo (todas las sedes, no solo la de la sesión) y las descargas
 * en PDF. No muestra egresos ni cuentas por cobrar: el backend los envía siempre en 0 (H31).
 */
@Composable
fun ReportsScreen(
    onBack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptics = rememberAtelierHaptics()
    val chooserTitle = stringResource(R.string.reports_pdf_chooser)
    LaunchedEffect(state.pdf) {
        val file = state.pdf ?: return@LaunchedEffect
        val opened = when (state.pdfActionMode) {
            ReportPdfActionMode.OPEN -> DocumentIntents.openPdf(context, file, chooserTitle)
            ReportPdfActionMode.SHARE -> DocumentIntents.sharePdf(context, file, chooserTitle)
        }
        if (opened) viewModel.onPdfHandled() else viewModel.onPdfOpenFailed()
    }
    LaunchedEffect(state.actionError, state.error) {
        if (state.actionError != null || state.error != null) haptics.reject()
    }
    ReportsContent(
        state = state,
        onBack = onBack,
        onRetry = viewModel::load,
        onChoice = viewModel::onChoice,
        onOpenCustomRange = viewModel::onOpenCustomRange,
        onDismissCustomRange = viewModel::onDismissCustomRange,
        onCustomRangePicked = viewModel::onCustomRangePicked,
        onOpenCashFlowPdf = {
            haptics.confirm()
            viewModel.onOpenCashFlowPdf()
        },
        onShareCashFlowPdf = {
            haptics.confirm()
            viewModel.onShareCashFlowPdf()
        },
        onOpenFleet = viewModel::onOpenFleet,
        onFleetActionMode = viewModel::onFleetActionMode,
        onFleetQuery = viewModel::onFleetQuery,
        onFleetSearch = viewModel::onFleetSearch,
        onFleetPick = { customer ->
            haptics.confirm()
            viewModel.onFleetPick(customer)
        },
        onDismissFleet = viewModel::onDismissFleet
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportsContent(
    state: ReportsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onChoice: (PeriodChoice) -> Unit,
    onOpenCustomRange: () -> Unit,
    onDismissCustomRange: () -> Unit,
    onCustomRangePicked: (Long?, Long?) -> Unit,
    onOpenCashFlowPdf: () -> Unit,
    onShareCashFlowPdf: () -> Unit,
    onOpenFleet: () -> Unit,
    onFleetActionMode: (ReportPdfActionMode) -> Unit,
    onFleetQuery: (String) -> Unit,
    onFleetSearch: () -> Unit,
    onFleetPick: (Customer) -> Unit,
    onDismissFleet: () -> Unit
) {
    val summary = state.summary
    val rawSkeleton = state.canReadCashFlow && state.isLoading && summary == null && state.error == null
    val showSkeleton = rememberDelayedLoading(rawSkeleton)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { AtelierTopBar(title = stringResource(R.string.reports_title), onBack = onBack) }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading && summary != null,
            onRefresh = onRetry,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                if (state.canReadCashFlow) {
                    item(key = "panel") {
                        AtelierReportPanel(
                            context = stringResource(R.string.reports_context),
                            legend = summary?.totalIncome?.format() ?: PLACEHOLDER_TOTAL,
                            caption = stringResource(R.string.reports_legend_caption),
                            loading = showSkeleton,
                            figures = summary?.let {
                                listOf(
                                    PanelFigure(
                                        it.movements.size.toString(),
                                        pluralStringResource(R.plurals.reports_movements_label, it.movements.size)
                                    )
                                )
                            }.orEmpty()
                        )
                    }
                }
                item(key = "refresh") {
                    AtelierRefreshBar(visible = state.isLoading && summary != null)
                }
                if (!state.hasAnyAccess) {
                    item(key = "no-access") {
                        AtelierNoPermissionState(
                            text = stringResource(R.string.reports_no_access),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    return@LazyColumn
                }
                if (state.canReadCashFlow) {
                    item(key = "scope") {
                        Column(
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AtelierBanner(
                                message = stringResource(R.string.reports_scope_note),
                                icon = Icons.Outlined.Info,
                                kind = BannerKind.Info
                            )
                            Text(
                                text = stringResource(R.string.reports_h31_income_only_note),
                                style = MaterialTheme.typography.bodySmall,
                                color = AtelierTheme.colors.inkSecondary
                            )
                        }
                    }
                    item(key = "period") {
                        PeriodSelector(
                            choice = state.choice,
                            period = state.period,
                            onChoice = onChoice,
                            onOpenCustomRange = onOpenCustomRange
                        )
                    }
                }
                item(key = "downloads") {
                    Downloads(
                        state = state,
                        onOpenCashFlowPdf = onOpenCashFlowPdf,
                        onShareCashFlowPdf = onShareCashFlowPdf,
                        onOpenFleet = onOpenFleet
                    )
                }
                if (state.canReadCashFlow) {
                    item(key = "movements-header") {
                        AtelierSectionHeader(
                            title = stringResource(R.string.reports_movements_header),
                            count = summary?.movements?.size,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    when {
                        showSkeleton -> items(3, key = { "bone-$it" }) {
                            AtelierListRow(title = "Concepto del cobro", subtitle = "16/11/2026", loading = true)
                        }
                        rawSkeleton -> Unit
                        summary == null -> item(key = "error") {
                            if (!state.isOnline) {
                                AtelierOfflineState(modifier = Modifier.padding(horizontal = 16.dp), onRetry = onRetry)
                            } else {
                                AtelierErrorState(
                                    message = state.error?.asString().orEmpty(),
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    onRetry = onRetry
                                )
                            }
                        }
                        summary.movements.isEmpty() -> item(key = "empty") {
                            AtelierEmptyState(
                                text = stringResource(R.string.reports_movements_empty),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        else -> items(summary.movements, key = { it.transactionId }) { movement ->
                            AtelierListRow(
                                title = movement.concept.ifBlank { movement.category },
                                subtitle = stringResource(
                                    R.string.reports_movement_detail,
                                    movement.movementDate.formatLimaDate(),
                                    movement.referenceNumber
                                ),
                                trailing = {
                                    Text(
                                        text = movementAmount(movement.amount, summary.currency),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    if (state.showCustomRangePicker) {
        CustomDateRangePickerDialog(
            initialPeriod = state.period,
            onDismiss = onDismissCustomRange,
            onConfirm = onCustomRangePicked
        )
    }
    state.fleetSheet?.let { sheet ->
        FleetSheetContent(
            sheet = sheet,
            busy = state.busy == ReportDownload.FleetHealth,
            online = state.isOnline,
            onActionMode = onFleetActionMode,
            onQuery = onFleetQuery,
            onSearch = onFleetSearch,
            onPick = onFleetPick,
            onDismiss = onDismissFleet
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomDateRangePickerDialog(
    initialPeriod: ReportPeriod,
    onDismiss: () -> Unit,
    onConfirm: (Long?, Long?) -> Unit
) {
    val initialStartMs = initialPeriod.from.takeIf { it != LocalDate.MIN }
        ?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    val initialEndMs = initialPeriod.to.takeIf { it != LocalDate.MIN }
        ?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    val pickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStartMs,
        initialSelectedEndDateMillis = initialEndMs
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(pickerState.selectedStartDateMillis, pickerState.selectedEndDateMillis)
                },
                enabled = pickerState.selectedStartDateMillis != null
            ) {
                Text(stringResource(R.string.common_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    ) {
        DateRangePicker(
            state = pickerState,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

private fun movementAmount(amount: BigDecimal, currency: String): String {
    val money = Money(amount.abs(), if (currency == Money.USD) Money.USD else Money.PEN).format()
    return if (amount.signum() < 0) "−$money" else money
}

@Composable
private fun PeriodSelector(
    choice: PeriodChoice,
    period: ReportPeriod,
    onChoice: (PeriodChoice) -> Unit,
    onOpenCustomRange: () -> Unit
) {
    val monthLabel = stringResource(R.string.reports_period_month)
    val prevMonthLabel = stringResource(R.string.reports_period_previous_month)
    val yearLabel = stringResource(R.string.reports_period_year)
    val customLabel = stringResource(R.string.reports_period_custom)
    AtelierSurface(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            .fillMaxWidth(),
        tier = SurfaceTier.Elevated,
        shape = CardLargeShape,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AtelierSegmentedControl(
                options = PeriodTab.entries,
                selected = choice.toTab(),
                onSelect = { tab ->
                    when (tab) {
                        PeriodTab.THIS_MONTH -> onChoice(PeriodChoice.ThisMonth)
                        PeriodTab.PREVIOUS_MONTH -> onChoice(PeriodChoice.PreviousMonth)
                        PeriodTab.THIS_YEAR -> onChoice(PeriodChoice.ThisYear)
                        PeriodTab.CUSTOM -> onOpenCustomRange()
                    }
                },
                label = { tab ->
                    when (tab) {
                        PeriodTab.THIS_MONTH -> monthLabel
                        PeriodTab.PREVIOUS_MONTH -> prevMonthLabel
                        PeriodTab.THIS_YEAR -> yearLabel
                        PeriodTab.CUSTOM -> customLabel
                    }
                }
            )
            Text(
                text = stringResource(R.string.reports_period_range, period.from.formatDay(), period.to.formatDay()),
                style = MaterialTheme.typography.bodyMedium,
                color = AtelierTheme.colors.inkSecondary
            )
        }
    }
}

private fun LocalDate.formatDay(): String = "%02d/%02d/%d".format(dayOfMonth, monthValue, year)

@Composable
private fun Downloads(
    state: ReportsUiState,
    onOpenCashFlowPdf: () -> Unit,
    onShareCashFlowPdf: () -> Unit,
    onOpenFleet: () -> Unit
) {
    if (!state.canReadCashFlow && !state.canDownloadFleet && !state.fleetLockedByPlan) return
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        AtelierSectionHeader(title = stringResource(R.string.reports_downloads_header))
        AtelierSurface(
            modifier = Modifier.fillMaxWidth(),
            tier = SurfaceTier.Elevated,
            shape = CardLargeShape,
            contentPadding = PaddingValues(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (state.canReadCashFlow) {
                    Text(
                        text = stringResource(R.string.reports_download_cashflow),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    AtelierAdaptiveRow(equalWidthInRow = true) {
                        AtelierButton(
                            text = stringResource(R.string.reports_open_pdf),
                            onClick = onOpenCashFlowPdf,
                            style = AtelierButtonStyle.Tonal,
                            enabled = state.isOnline && state.busy == null,
                            loading = state.busy == ReportDownload.CashFlow && state.pdfActionMode == ReportPdfActionMode.OPEN
                        )
                        AtelierButton(
                            text = stringResource(R.string.reports_share_pdf),
                            onClick = onShareCashFlowPdf,
                            style = AtelierButtonStyle.Outlined,
                            enabled = state.isOnline && state.busy == null,
                            loading = state.busy == ReportDownload.CashFlow && state.pdfActionMode == ReportPdfActionMode.SHARE
                        )
                    }
                }
                if (state.canDownloadFleet) {
                    AtelierButton(
                        text = stringResource(R.string.reports_download_fleet),
                        onClick = onOpenFleet,
                        modifier = Modifier.fillMaxWidth(),
                        style = AtelierButtonStyle.Outlined,
                        enabled = state.isOnline && state.busy == null
                    )
                }
                if (state.fleetLockedByPlan) {
                    AtelierPlanLockedState(stringResource(R.string.reports_fleet_locked))
                }
                if (!state.isOnline) AtelierRequiresConnection()
                state.actionError?.let { error ->
                    AtelierBanner(message = error.asString(), icon = Icons.Outlined.ErrorOutline)
                }
            }
        }
    }
}

@Composable
private fun FleetSheetContent(
    sheet: FleetSheet,
    busy: Boolean,
    online: Boolean,
    onActionMode: (ReportPdfActionMode) -> Unit,
    onQuery: (String) -> Unit,
    onSearch: () -> Unit,
    onPick: (Customer) -> Unit,
    onDismiss: () -> Unit
) {
    val openLabel = stringResource(R.string.reports_open_pdf)
    val shareLabel = stringResource(R.string.reports_share_pdf)
    AtelierBottomSheet(
        title = stringResource(R.string.reports_fleet_title),
        onDismiss = onDismiss,
        actions = {
            AtelierButton(
                text = stringResource(R.string.reports_fleet_search_action),
                onClick = onSearch,
                modifier = Modifier.fillMaxWidth(),
                enabled = sheet.query.isNotBlank() && !busy,
                loading = sheet.isSearching
            )
            if (!online) AtelierRequiresConnection()
        }
    ) {
        Text(
            text = stringResource(R.string.reports_fleet_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = AtelierTheme.colors.inkSecondary
        )
        Text(
            text = stringResource(R.string.reports_fleet_scope_note),
            style = MaterialTheme.typography.bodySmall,
            color = AtelierTheme.colors.inkSecondary
        )
        AtelierSegmentedControl(
            options = ReportPdfActionMode.entries,
            selected = sheet.actionMode,
            onSelect = onActionMode,
            label = { mode ->
                when (mode) {
                    ReportPdfActionMode.OPEN -> openLabel
                    ReportPdfActionMode.SHARE -> shareLabel
                }
            }
        )
        AtelierTextField(
            label = stringResource(R.string.reports_fleet_search),
            value = sheet.query,
            onValueChange = onQuery
        )
        sheet.error?.let { AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline) }
        when {
            sheet.hasSearched && sheet.results.isEmpty() && sheet.error == null ->
                AtelierEmptyState(
                    text = stringResource(R.string.reports_fleet_empty)
                )
            else -> sheet.results.forEach { customer ->
                AtelierListRow(
                    title = customer.displayName,
                    subtitle = customer.taxId,
                    onClick = if (online && !busy) ({ onPick(customer) }) else null,
                    trailing = null
                )
            }
        }
    }
}

// region Previews

private fun soles(amount: String) = Money(BigDecimal(amount), Money.PEN)

private val previewSummary = CashFlowSummary(
    totalIncome = soles("18450.00"),
    totalExpenses = soles("0.00"),
    accountsReceivable = soles("0.00"),
    netCashFlow = BigDecimal("18450.00"),
    currency = "PEN",
    movements = listOf(
        CashFlowMovement("m1", Instant.parse("2026-11-14T16:20:00Z"), "INFLOW", "PAYMENT", "Cobro boleta B001-00000944", "B001-944", BigDecimal("1200.00"), BigDecimal("1200.00")),
        CashFlowMovement("m2", Instant.parse("2026-11-15T14:05:00Z"), "INFLOW", "PAYMENT", "Cobro factura F001-00000128", "F001-128", BigDecimal("3540.00"), BigDecimal("4740.00")),
        CashFlowMovement("m3", Instant.parse("2026-11-16T19:45:00Z"), "INFLOW", "PAYMENT", "Cobro boleta B001-00000945", "B001-945", BigDecimal("708.00"), BigDecimal("5448.00"))
    )
)

private val previewState = ReportsUiState(
    today = LocalDate.of(2026, 11, 16),
    period = ReportPeriod.month(java.time.YearMonth.of(2026, 11)),
    summary = previewSummary,
    canReadCashFlow = true,
    canDownloadFleet = true
)

@Composable
private fun PreviewContent(state: ReportsUiState) {
    ReportsContent(
        state = state,
        onBack = {},
        onRetry = {},
        onChoice = {},
        onOpenCustomRange = {},
        onDismissCustomRange = {},
        onCustomRangePicked = { _, _ -> },
        onOpenCashFlowPdf = {},
        onShareCashFlowPdf = {},
        onOpenFleet = {},
        onFleetActionMode = {},
        onFleetQuery = {},
        onFleetSearch = {},
        onFleetPick = {},
        onDismissFleet = {}
    )
}

@Preview(name = "Reportes · claro", widthDp = 390, heightDp = 844)
@Composable
private fun ReportsPreviewLight() = AtelierWorkshopTheme(darkTheme = false) { PreviewContent(previewState) }

@Preview(name = "Reportes · oscuro", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ReportsPreviewDark() = AtelierWorkshopTheme(darkTheme = true) { PreviewContent(previewState) }

@Preview(name = "Reportes · cargando", widthDp = 390, heightDp = 844)
@Composable
private fun ReportsPreviewLoading() = AtelierWorkshopTheme(darkTheme = false) {
    PreviewContent(previewState.copy(summary = null, isLoading = true))
}

@Preview(name = "Reportes · plan sin flota", widthDp = 390, heightDp = 844)
@Composable
private fun ReportsPreviewLocked() = AtelierWorkshopTheme(darkTheme = false) {
    PreviewContent(previewState.copy(canDownloadFleet = false, fleetLockedByPlan = true))
}

// endregion
