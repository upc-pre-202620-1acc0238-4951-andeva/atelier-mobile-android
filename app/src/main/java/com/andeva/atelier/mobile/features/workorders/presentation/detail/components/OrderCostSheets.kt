package com.andeva.atelier.mobile.features.workorders.presentation.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBottomSheet
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierEmptyState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.core.presentation.formatLimaDate
import com.andeva.atelier.mobile.core.presentation.formatQuantity
import com.andeva.atelier.mobile.features.workorders.domain.PartMovement
import com.andeva.atelier.mobile.features.workorders.domain.Profitability
import com.andeva.atelier.mobile.features.workorders.presentation.detail.OnDemand
import java.math.BigDecimal
import java.util.Locale

private val PeruLocale: Locale = Locale.Builder().setLanguage("es").setRegion("PE").build()

/** La ganancia puede ser negativa: no pasa por `Money`, que no admite montos menores a cero. */
private fun BigDecimal?.soles(currency: String?): String {
    if (this == null) return "—"
    val symbol = if (currency == Money.USD) "US$" else "S/"
    return "$symbol ${String.format(PeruLocale, "%,.2f", this)}"
}

/** US35 E2: ingreso de repuestos contra su costo de ventas FIFO. Se pide al abrir la hoja. */
@Composable
internal fun ProfitabilitySheet(
    state: OnDemand<Profitability>,
    partName: (String) -> String?,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    AtelierBottomSheet(
        title = stringResource(R.string.orders_profitability_title),
        onDismiss = onDismiss,
        actions = {
            AtelierButton(
                text = stringResource(R.string.quotation_pdf_close),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Text
            )
        }
    ) {
        val data = state.data
        when {
            state.isLoading -> Text(
                text = stringResource(R.string.search_loading),
                style = MaterialTheme.typography.bodyLarge,
                color = AtelierTheme.colors.inkSecondary
            )
            state.error != null -> {
                AtelierBanner(message = state.error.asString(), icon = Icons.Outlined.ErrorOutline)
                AtelierButton(
                    text = stringResource(R.string.common_retry),
                    onClick = onRetry,
                    style = AtelierButtonStyle.Outlined
                )
            }
            data != null -> {
                val cur = data.currency
                SummaryRow(stringResource(R.string.orders_profit_labor_revenue), data.laborRevenue.soles(cur))
                SummaryRow(stringResource(R.string.orders_profit_parts_revenue), data.partsRevenue.soles(cur))
                SummaryRow(stringResource(R.string.orders_profit_cost_of_sales), data.partsCostOfSales.soles(cur))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SummaryRow(stringResource(R.string.orders_profit_parts_gross), data.partsGrossProfit.soles(cur), emphasized = true)
                SummaryRow(stringResource(R.string.orders_profit_subtotal), data.subtotal.soles(cur))
                if (data.lines.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.orders_profit_lines),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .semantics { heading() }
                    )
                    Column {
                        data.lines.forEach { line ->
                            AtelierListRow(
                                title = partName(line.productId) ?: stringResource(R.string.task_part_unnamed),
                                subtitle = stringResource(
                                    R.string.orders_profit_line_detail,
                                    line.quantity.formatQuantity(),
                                    line.costOfSales.soles(cur)
                                ),
                                trailing = {
                                    Text(
                                        text = line.grossProfit.soles(cur),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** US33 E2: qué repuestos salieron de qué lote y cuánto volvió. Se pide al abrir la hoja. */
@Composable
internal fun PartMovementsSheet(
    state: OnDemand<List<PartMovement>>,
    partName: (String) -> String?,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    AtelierBottomSheet(
        title = stringResource(R.string.orders_movements_title),
        onDismiss = onDismiss,
        actions = {
            AtelierButton(
                text = stringResource(R.string.quotation_pdf_close),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Text
            )
        }
    ) {
        val data = state.data
        when {
            state.isLoading -> Text(
                text = stringResource(R.string.search_loading),
                style = MaterialTheme.typography.bodyLarge,
                color = AtelierTheme.colors.inkSecondary
            )
            state.error != null -> {
                AtelierBanner(message = state.error.asString(), icon = Icons.Outlined.ErrorOutline)
                AtelierButton(
                    text = stringResource(R.string.common_retry),
                    onClick = onRetry,
                    style = AtelierButtonStyle.Outlined
                )
            }
            data != null && data.isEmpty() -> AtelierEmptyState(text = stringResource(R.string.orders_movements_empty))
            data != null -> Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                data.forEach { movement ->
                    val returned = movement.returnedQuantity.signum() > 0
                    AtelierListRow(
                        title = partName(movement.productId) ?: stringResource(R.string.task_part_unnamed),
                        subtitle = buildString {
                            append(
                                stringResource(
                                    R.string.orders_movement_detail,
                                    movement.chargedQuantity.formatQuantity(),
                                    movement.returnedQuantity.formatQuantity()
                                )
                            )
                            val whenText = (if (returned) movement.lastReturnedAt else movement.chargedAt)?.formatLimaDate()
                            if (whenText != null) append(" · ").append(whenText)
                            if (returned) movement.lastReturnReason?.takeIf { it.isNotBlank() }?.let { append(" · ").append(it) }
                        }
                    )
                }
            }
        }
    }
}
