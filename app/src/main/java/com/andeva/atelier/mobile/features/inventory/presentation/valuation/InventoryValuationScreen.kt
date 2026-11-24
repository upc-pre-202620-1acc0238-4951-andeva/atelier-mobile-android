package com.andeva.atelier.mobile.features.inventory.presentation.valuation

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierEmptyState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierErrorState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierMetricBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNoPermissionState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierOfflineState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRefreshBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierReportPanel
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSectionHeader
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSurface
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.PanelFigure
import com.andeva.atelier.mobile.core.designsystem.component.SurfaceTier
import com.andeva.atelier.mobile.core.designsystem.component.rememberDelayedLoading
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardLargeShape
import com.andeva.atelier.mobile.core.designsystem.theme.HeroShape
import com.andeva.atelier.mobile.core.designsystem.theme.Spacing
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.core.presentation.format
import com.andeva.atelier.mobile.core.presentation.formatQuantity
import com.andeva.atelier.mobile.features.inventory.domain.ItemCategory
import com.andeva.atelier.mobile.features.inventory.domain.Part
import com.andeva.atelier.mobile.features.inventory.domain.PartStatus
import com.andeva.atelier.mobile.features.inventory.presentation.labelRes
import java.math.BigDecimal

/** Valorización del inventario: una cifra en soles y las existencias por categoría. Requiere `BATCHES_READ`. */
@Composable
fun InventoryValuationScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InventoryValuationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    InventoryValuationContent(state, onBack, viewModel::refresh, modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InventoryValuationContent(
    state: InventoryValuationUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val delayedBones = rememberDelayedLoading(state.isLoading && state.parts.isEmpty())
    val showCategoryBones = state.isLoading && state.parts.isEmpty() && (LocalInspectionMode.current || delayedBones)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        AtelierTopBar(title = stringResource(R.string.inventory_valuation_title), onBack = onBack)
        if (!state.canRead) {
            AtelierNoPermissionState(
                text = stringResource(R.string.inventory_valuation_no_permission),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            return@Column
        }
        val valuation = state.valuation
        AtelierReportPanel(
            context = stringResource(R.string.inventory_valuation_context),
            legend = valuation?.format() ?: "S/ 00,000.00",
            caption = stringResource(R.string.inventory_valuation_caption),
            loading = valuation == null && state.isLoading,
            figures = listOf(
                PanelFigure(state.withStockCount.toString(), stringResource(R.string.inventory_valuation_with_stock)),
                PanelFigure(state.lowStockCount.toString(), stringResource(R.string.inventory_valuation_low)),
                PanelFigure(state.outOfStockCount.toString(), stringResource(R.string.inventory_valuation_out))
            )
        )
        AtelierRefreshBar(visible = state.isLoading && valuation != null)
        PullToRefreshBox(isRefreshing = false, onRefresh = onRefresh, modifier = Modifier.weight(1f)) {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                val error = state.error
                when {
                    valuation == null && !state.isOnline -> item(key = "offline") {
                        AtelierOfflineState(onRetry = onRefresh, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                    valuation == null && error != null -> item(key = "error") {
                        AtelierErrorState(
                            message = error.asString(),
                            onRetry = onRefresh,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
                item(key = "health-summary") {
                    if (state.parts.isNotEmpty()) {
                        InventoryHealthCard(
                            state = state,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                        )
                    }
                }
                item(key = "header") {
                    AtelierSectionHeader(
                        title = stringResource(R.string.inventory_valuation_by_category),
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
                    )
                }
                when {
                    showCategoryBones -> items(4, key = { "bone-$it" }) {
                        AtelierListRow(title = "Frenos", subtitle = "8 repuestos", loading = true)
                    }
                    state.byCategory.isEmpty() -> item(key = "empty") {
                        AtelierEmptyState(
                            text = stringResource(R.string.inventory_valuation_empty),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    else -> {
                        items(state.byCategory, key = { it.category?.name ?: "none" }) { row ->
                            val categoryParts = remember(state.parts, row.category) {
                                state.parts.filter { it.totalStock > BigDecimal.ZERO && it.category == row.category }
                            }
                            CategoryValuationCard(
                                row = row,
                                categoryParts = categoryParts,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Spacing.md, vertical = Spacing.xs)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryHealthCard(
    state: InventoryValuationUiState,
    modifier: Modifier = Modifier
) {
    val colors = AtelierTheme.colors
    val totalParts = state.parts.size.coerceAtLeast(1)
    val healthyCount = (state.withStockCount - state.lowStockCount).coerceAtLeast(0)
    val progress = (state.withStockCount.toFloat() / totalParts.toFloat()).coerceIn(0f, 1f)
    val markerAt = (healthyCount.toFloat() / totalParts.toFloat())
        .takeIf { state.lowStockCount > 0 || state.outOfStockCount > 0 }
        ?.coerceIn(0.08f, 0.92f)
    val barColor = when {
        state.outOfStockCount > healthyCount -> colors.badMark
        state.lowStockCount > 0 || state.outOfStockCount > 0 -> colors.warnMark
        else -> colors.okMark
    }

    AtelierSurface(
        tier = SurfaceTier.Elevated,
        shape = HeroShape,
        contentPadding = PaddingValues(Spacing.md),
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.premium_valuation_health_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${state.withStockCount}/${state.parts.size}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            AtelierMetricBar(
                progress = progress,
                color = barColor,
                height = 8.dp,
                markerAt = markerAt
            )
            Text(
                text = stringResource(
                    R.string.premium_valuation_health_summary,
                    state.withStockCount,
                    state.lowStockCount,
                    state.outOfStockCount
                ),
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkSecondary
            )
        }
    }
}

@Composable
private fun CategoryValuationCard(
    row: CategoryStock,
    categoryParts: List<Part>,
    modifier: Modifier = Modifier
) {
    val colors = AtelierTheme.colors
    val totalUnits = row.units.toFloat().coerceAtLeast(0f)
    val minUnits = categoryParts.fold(BigDecimal.ZERO) { acc, part -> acc + part.minimumStock }.toFloat().coerceAtLeast(0f)
    val scaleMax = maxOf(minUnits * 2f, totalUnits * 1.15f, 1f)
    val progress = (totalUnits / scaleMax).coerceIn(0f, 1f)
    val markerAt = if (minUnits > 0f) (minUnits / scaleMax).coerceIn(0.08f, 0.92f) else null
    val hasOutOfStock = categoryParts.any { it.isOutOfStock }
    val hasLowStock = categoryParts.any { it.isLowStock }
    val barColor = when {
        totalUnits <= 0f || hasOutOfStock -> colors.badMark
        totalUnits <= minUnits || hasLowStock -> colors.warnMark
        else -> colors.okMark
    }

    AtelierSurface(
        tier = SurfaceTier.Elevated,
        shape = CardLargeShape,
        contentPadding = PaddingValues(Spacing.md),
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(row.category?.labelRes() ?: R.string.inventory_valuation_uncategorized),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = pluralStringResource(R.plurals.inventory_valuation_parts, row.partCount, row.partCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkSecondary
                    )
                }
                Text(
                    text = stringResource(R.string.inventory_valuation_units, row.units.formatQuantity()),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            AtelierMetricBar(
                progress = progress,
                color = barColor,
                height = 6.dp,
                markerAt = markerAt
            )
            if (minUnits > 0f) {
                Text(
                    text = stringResource(
                        R.string.premium_valuation_category_min,
                        categoryParts.fold(BigDecimal.ZERO) { acc, part -> acc + part.minimumStock }.formatQuantity()
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkSecondary
                )
            }
        }
    }
}

private fun part(id: String, category: ItemCategory, stock: String, min: String = "4") = Part(
    id = id, name = "Repuesto $id", sku = "SKU-$id", category = category, basePrice = null,
    totalStock = BigDecimal(stock), minimumStock = BigDecimal(min), unitOfMeasure = "UNIT", status = PartStatus.ACTIVE
)

private val previewState = InventoryValuationUiState(
    valuation = Money(BigDecimal("48250.50"), Money.PEN),
    isLoading = false,
    parts = listOf(
        part("1", ItemCategory.BRAKES, "14"), part("2", ItemCategory.BRAKES, "3"),
        part("3", ItemCategory.LUBRICANTS, "40"), part("4", ItemCategory.FILTERS, "0"),
        part("5", ItemCategory.TIRES, "12")
    )
)

@Preview(name = "Valorización · claro", widthDp = 390, heightDp = 844)
@Composable
private fun InventoryValuationPreviewLight() = AtelierWorkshopTheme(darkTheme = false) {
    InventoryValuationContent(previewState, {}, {})
}

@Preview(name = "Valorización · oscuro", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun InventoryValuationPreviewDark() = AtelierWorkshopTheme(darkTheme = true) {
    InventoryValuationContent(previewState, {}, {})
}

@Preview(name = "Valorización · sin red", widthDp = 390, heightDp = 844)
@Composable
private fun InventoryValuationPreviewOffline() = AtelierWorkshopTheme(darkTheme = false) {
    InventoryValuationContent(InventoryValuationUiState(isLoading = false, isOnline = false), {}, {})
}
