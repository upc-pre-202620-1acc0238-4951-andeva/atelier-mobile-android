package com.andeva.atelier.mobile.features.reports.presentation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
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
import com.andeva.atelier.mobile.core.designsystem.component.AtelierReportPanel
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRequiresConnection
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSectionHeader
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.PanelFigure
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.core.presentation.format
import com.andeva.atelier.mobile.core.presentation.formatLimaDate
import com.andeva.atelier.mobile.features.reception.domain.Customer
import com.andeva.atelier.mobile.features.reports.domain.CashFlowMovement
import com.andeva.atelier.mobile.features.reports.domain.CashFlowSummary
import com.andeva.atelier.mobile.features.reports.domain.ReportPeriod
import java.io.File
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

private const val PLACEHOLDER_TOTAL = "S/ 0.00"

/** Abre el menú de compartir con el PDF descargado en `cacheDir/pdfs`; false si no hay app que lo reciba. */
private fun shareOrOpenPdf(context: Context, pdfFile: File, chooserTitle: String): Boolean = try {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
} catch (_: ActivityNotFoundException) {
    false
} catch (_: IllegalArgumentException) {
    false
}

/**
 * "Reportes": el flujo de caja del taller en un periodo (todas las sedes, no solo la de la sesión) y las descargas
 * en PDF. No muestra egresos ni cuentas por cobrar: el backend los envía siempre en 0.
 */
@Composable
fun ReportsScreen(
    onBack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val chooserTitle = stringResource(R.string.reports_pdf_chooser)
    LaunchedEffect(state.pdf) {
        val file = state.pdf ?: return@LaunchedEffect
        if (shareOrOpenPdf(context, file, chooserTitle)) viewModel.onPdfHandled() else viewModel.onPdfOpenFailed()
    }
    ReportsContent(
        state = state,
        onBack = onBack,
        onRetry = viewModel::load,
        onChoice = viewModel::onChoice,
        onDownloadCashFlow = viewModel::onDownloadCashFlow,
        onOpenFleet = viewModel::onOpenFleet,
        onFleetQuery = viewModel::onFleetQuery,
        onFleetSearch = viewModel::onFleetSearch,
        onFleetPick = viewModel::onFleetPick,
        onDismissFleet = viewModel::onDismissFleet
    )
}

@Composable
private fun ReportsContent(
    state: ReportsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onChoice: (PeriodChoice) -> Unit,
    onDownloadCashFlow: () -> Unit,
    onOpenFleet: () -> Unit,
    onFleetQuery: (String) -> Unit,
    onFleetSearch: () -> Unit,
    onFleetPick: (Customer) -> Unit,
    onDismissFleet: () -> Unit
) {
    val summary = state.summary
    val showSkeleton = state.canReadCashFlow && summary == null && state.error == null
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item(key = "panel") {
                if (state.canReadCashFlow) {
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
                        }.orEmpty(),
                        actions = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.common_back))
                            }
                        }
                    )
                } else {
                    AtelierTopBar(title = stringResource(R.string.reports_title), onBack = onBack)
                }
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
                    AtelierBanner(
                        message = stringResource(R.string.reports_scope_note),
                        icon = Icons.Outlined.Info,
                        kind = BannerKind.Info,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
                    )
                }
                item(key = "period") { PeriodSelector(state.choice, state.period, onChoice) }
            }
            item(key = "downloads") {
                Downloads(state, onDownloadCashFlow, onOpenFleet)
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
    state.fleetSheet?.let { sheet ->
        FleetSheetContent(
            sheet = sheet,
            busy = state.busy == ReportDownload.FleetHealth,
            online = state.isOnline,
            onQuery = onFleetQuery,
            onSearch = onFleetSearch,
            onPick = onFleetPick,
            onDismiss = onDismissFleet
        )
    }
}

private fun movementAmount(amount: BigDecimal, currency: String): String {
    val money = Money(amount.abs(), if (currency == Money.USD) Money.USD else Money.PEN).format()
    return if (amount.signum() < 0) "−$money" else money
}

@Composable
private fun PeriodSelector(choice: PeriodChoice, period: ReportPeriod, onChoice: (PeriodChoice) -> Unit) {
    Column(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PeriodChoice.entries.forEach { option ->
                val selected = option == choice
                AtelierButton(
                    text = stringResource(
                        when (option) {
                            PeriodChoice.ThisMonth -> R.string.reports_period_month
                            PeriodChoice.PreviousMonth -> R.string.reports_period_previous_month
                            PeriodChoice.ThisYear -> R.string.reports_period_year
                        }
                    ),
                    onClick = { onChoice(option) },
                    modifier = Modifier
                        .weight(1f)
                        .semantics { this.selected = selected },
                    style = if (selected) AtelierButtonStyle.Tonal else AtelierButtonStyle.Outlined
                )
            }
        }
        Text(
            text = stringResource(R.string.reports_period_range, period.from.formatDay(), period.to.formatDay()),
            style = MaterialTheme.typography.bodyMedium,
            color = AtelierTheme.colors.inkSecondary
        )
    }
}

private fun LocalDate.formatDay(): String = "%02d/%02d/%d".format(dayOfMonth, monthValue, year)

@Composable
private fun Downloads(state: ReportsUiState, onDownloadCashFlow: () -> Unit, onOpenFleet: () -> Unit) {
    if (!state.canReadCashFlow && !state.canDownloadFleet && !state.fleetLockedByPlan) return
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        AtelierSectionHeader(title = stringResource(R.string.reports_downloads_header))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.canReadCashFlow) {
                AtelierButton(
                    text = stringResource(R.string.reports_download_cashflow),
                    onClick = onDownloadCashFlow,
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.Outlined,
                    enabled = state.isOnline && state.busy == null,
                    loading = state.busy == ReportDownload.CashFlow
                )
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

@Composable
private fun FleetSheetContent(
    sheet: FleetSheet,
    busy: Boolean,
    online: Boolean,
    onQuery: (String) -> Unit,
    onSearch: () -> Unit,
    onPick: (Customer) -> Unit,
    onDismiss: () -> Unit
) {
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
        AtelierTextField(
            label = stringResource(R.string.reports_fleet_search),
            value = sheet.query,
            onValueChange = onQuery
        )
        sheet.error?.let { AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline) }
        when {
            sheet.hasSearched && sheet.results.isEmpty() && sheet.error == null ->
                Text(
                    text = stringResource(R.string.reports_fleet_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
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
    ReportsContent(state, {}, {}, {}, {}, {}, {}, {}, {}, {})
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
