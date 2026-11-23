package com.andeva.atelier.mobile.features.cashier.presentation.dailycash

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierEmptyState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierErrorState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNoPermissionState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierOfflineState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierReportPanel
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSectionHeader
import com.andeva.atelier.mobile.core.designsystem.component.PanelFigure
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.core.presentation.format
import com.andeva.atelier.mobile.features.cashier.domain.DailyCashSummary
import com.andeva.atelier.mobile.features.cashier.domain.Payment
import com.andeva.atelier.mobile.features.cashier.domain.PaymentMethod
import com.andeva.atelier.mobile.features.cashier.presentation.formatLimaDateTime
import com.andeva.atelier.mobile.features.cashier.presentation.labelRes
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PanelDateFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.forLanguageTag("es-PE"))
private val FullDateFormatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es-PE"))

/**
 * "Caja del día": cuánto se cobró en la sede un día, en Display, y por qué medio de pago. Un solo reporte en línea;
 * no hay caché de este dato, así que sin red se dice y se reintenta.
 */
@Composable
fun DailyCashScreen(
    onBack: () -> Unit,
    viewModel: DailyCashViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DailyCashContent(
        state = state,
        onBack = onBack,
        onRetry = viewModel::load,
        onPreviousDay = viewModel::onPreviousDay,
        onNextDay = viewModel::onNextDay,
        onToday = viewModel::onToday
    )
}

@Composable
private fun DailyCashContent(
    state: DailyCashUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit
) {
    val summary = state.summary
    val showSkeleton = state.isLoading && summary == null
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item(key = "panel") {
                AtelierReportPanel(
                    context = stringResource(R.string.cashier_daily_context, PanelDateFormatter.format(state.date)),
                    legend = summary?.totalCollected?.format() ?: PLACEHOLDER_TOTAL,
                    caption = stringResource(R.string.cashier_daily_caption),
                    loading = showSkeleton,
                    figures = summary?.let {
                        listOf(
                            PanelFigure(
                                it.paymentCount.toString(),
                                pluralStringResource(R.plurals.cashier_daily_payments, it.paymentCount)
                            )
                        )
                    }.orEmpty(),
                    actions = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.common_back))
                        }
                    }
                )
            }
            if (!state.canRead) {
                item(key = "no-access") {
                    AtelierNoPermissionState(
                        text = stringResource(R.string.cashier_daily_no_access),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                return@LazyColumn
            }
            item(key = "date") { DateSwitcher(state, onPreviousDay, onNextDay, onToday) }
            when {
                showSkeleton -> items(3, key = { "bone-$it" }) {
                    AtelierListRow(title = "Efectivo", subtitle = "Detalle", loading = true)
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
                summary.paymentCount == 0 && summary.payments.isEmpty() -> item(key = "empty") {
                    AtelierEmptyState(
                        text = stringResource(R.string.cashier_daily_empty),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                else -> {
                    item(key = "methods-header") {
                        AtelierSectionHeader(
                            title = stringResource(R.string.cashier_daily_by_method),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    items(summary.byMethod.entries.sortedByDescending { it.value.amount }, key = { "method-${it.key}" }) { (method, total) ->
                        AtelierListRow(
                            title = stringResource(method.labelRes()),
                            trailing = { AmountText(total.format()) }
                        )
                    }
                    item(key = "payments-header") {
                        AtelierSectionHeader(
                            title = stringResource(R.string.cashier_daily_payments_header),
                            count = summary.payments.size,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    items(summary.payments, key = { "payment-${it.id}" }) { payment ->
                        AtelierListRow(
                            title = stringResource(payment.method.labelRes()),
                            subtitle = listOfNotNull(
                                payment.paidAt?.formatLimaDateTime(),
                                payment.transactionReference?.takeIf { it.isNotBlank() }
                            ).joinToString(" · ").ifBlank { null },
                            trailing = { AmountText(payment.amount.format()) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AmountText(value: String) {
    Text(text = value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
}

@Composable
private fun DateSwitcher(state: DailyCashUiState, onPrevious: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 12.dp, top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = stringResource(R.string.cashier_daily_previous))
        }
        Text(
            text = FullDateFormatter.format(state.date),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onNext, enabled = state.canGoNext) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = stringResource(R.string.cashier_daily_next))
        }
        if (!state.isToday) {
            AtelierButton(
                text = stringResource(R.string.cashier_daily_today),
                onClick = onToday,
                style = AtelierButtonStyle.Text
            )
        }
    }
}

private const val PLACEHOLDER_TOTAL = "S/ 0.00"

// region Previews

private fun soles(amount: String) = Money(BigDecimal(amount), Money.PEN)

private val previewSummary = DailyCashSummary(
    date = LocalDate.of(2026, 11, 16),
    totalCollected = soles("2840.00"),
    paymentCount = 4,
    byMethod = mapOf(
        PaymentMethod.CASH to soles("1200.00"),
        PaymentMethod.DIGITAL_WALLET_YAPE to soles("940.00"),
        PaymentMethod.CREDIT_CARD to soles("700.00")
    ),
    payments = listOf(
        Payment("p1", "v1", soles("1200.00"), PaymentMethod.CASH, null, "COMPLETED", Instant.parse("2026-11-16T15:10:00Z")),
        Payment("p2", "v2", soles("940.00"), PaymentMethod.DIGITAL_WALLET_YAPE, "YP-883201", "COMPLETED", Instant.parse("2026-11-16T17:42:00Z")),
        Payment("p3", "v3", soles("400.00"), PaymentMethod.CREDIT_CARD, "POS-5521", "COMPLETED", Instant.parse("2026-11-16T19:05:00Z")),
        Payment("p4", "v4", soles("300.00"), PaymentMethod.CREDIT_CARD, "POS-5522", "COMPLETED", Instant.parse("2026-11-16T21:30:00Z"))
    )
)

private val previewState = DailyCashUiState(
    today = LocalDate.of(2026, 11, 16),
    date = LocalDate.of(2026, 11, 16),
    summary = previewSummary,
    isLoading = false,
    canRead = true
)

@Composable
private fun PreviewContent(state: DailyCashUiState) {
    DailyCashContent(state, onBack = {}, onRetry = {}, onPreviousDay = {}, onNextDay = {}, onToday = {})
}

@Preview(name = "Caja del día · claro", widthDp = 390, heightDp = 844)
@Composable
private fun DailyCashPreviewLight() = AtelierWorkshopTheme(darkTheme = false) { PreviewContent(previewState) }

@Preview(name = "Caja del día · oscuro", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DailyCashPreviewDark() = AtelierWorkshopTheme(darkTheme = true) { PreviewContent(previewState) }

@Preview(name = "Caja del día · cargando", widthDp = 390, heightDp = 844)
@Composable
private fun DailyCashPreviewLoading() = AtelierWorkshopTheme(darkTheme = false) {
    PreviewContent(previewState.copy(summary = null, isLoading = true))
}

@Preview(name = "Caja del día · vacío", widthDp = 390, heightDp = 844)
@Composable
private fun DailyCashPreviewEmpty() = AtelierWorkshopTheme(darkTheme = false) {
    PreviewContent(
        previewState.copy(
            date = LocalDate.of(2026, 11, 15),
            summary = DailyCashSummary(LocalDate.of(2026, 11, 15), soles("0.00"), 0, emptyMap(), emptyList())
        )
    )
}

// endregion
