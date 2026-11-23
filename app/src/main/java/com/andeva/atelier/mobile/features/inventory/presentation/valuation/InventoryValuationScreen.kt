package com.andeva.atelier.mobile.features.inventory.presentation.valuation

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNoPermissionState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierOfflineState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRefreshBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierReportPanel
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSectionHeader
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.PanelFigure
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
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
                item(key = "header") {
                    AtelierSectionHeader(
                        title = stringResource(R.string.inventory_valuation_by_category),
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
                    )
                }
                if (state.byCategory.isEmpty()) {
                    item(key = "empty") {
                        AtelierEmptyState(
                            text = stringResource(R.string.inventory_valuation_empty),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    items(state.byCategory, key = { it.category?.name ?: "none" }) { row ->
                        AtelierListRow(
                            title = stringResource(row.category?.labelRes() ?: R.string.inventory_valuation_uncategorized),
                            subtitle = pluralStringResource(
                                R.plurals.inventory_valuation_parts, row.partCount, row.partCount
                            ),
                            trailing = {
                                Text(
                                    text = stringResource(R.string.inventory_valuation_units, row.units.formatQuantity()),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        )
                    }
                }
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
