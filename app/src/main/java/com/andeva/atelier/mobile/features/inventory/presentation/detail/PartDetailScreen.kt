package com.andeva.atelier.mobile.features.inventory.presentation.detail

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierEmptyState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierFichaPanel
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNoPermissionState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRefreshBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSectionHeader
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.core.presentation.format
import com.andeva.atelier.mobile.core.presentation.formatLimaDate
import com.andeva.atelier.mobile.core.presentation.formatQuantity
import com.andeva.atelier.mobile.features.inventory.domain.Batch
import com.andeva.atelier.mobile.features.inventory.domain.ItemCategory
import com.andeva.atelier.mobile.features.inventory.domain.Part
import com.andeva.atelier.mobile.features.inventory.domain.PartStatus
import com.andeva.atelier.mobile.features.inventory.presentation.labelRes
import com.andeva.atelier.mobile.features.inventory.presentation.stockChipKind
import com.andeva.atelier.mobile.features.inventory.presentation.stockLabelRes
import java.math.BigDecimal
import java.time.Instant

/**
 * US32: ficha del repuesto. Las existencias salen de la caché; los lotes en orden FIFO y sus costos solo se ven
 * con `BATCHES_READ`.
 */
@Composable
fun PartDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PartDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PartDetailContent(
        state = state,
        onBack = onBack,
        onRefresh = { viewModel.refresh(force = true) },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PartDetailContent(
    state: PartDetailUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val part = state.part
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        AtelierFichaPanel(
            onBack = onBack,
            title = part?.name ?: stringResource(R.string.part_detail_title),
            subtitle = part?.let { stringResource(R.string.part_sku, it.sku) },
            status = part?.let { { AtelierChip(text = stringResource(it.stockLabelRes()), kind = it.stockChipKind()) } }
        )
        AtelierRefreshBar(visible = state.isLoading)
        PullToRefreshBox(isRefreshing = false, onRefresh = onRefresh, modifier = Modifier.weight(1f)) {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                if (state.showingCache) {
                    item(key = "cache") {
                        AtelierBanner(
                            message = state.error?.asString() ?: stringResource(R.string.batches_cache_notice),
                            icon = Icons.Outlined.Info,
                            kind = BannerKind.Info,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                if (part != null) {
                    item(key = "facts-header") {
                        AtelierSectionHeader(
                            title = stringResource(R.string.inventory_detail_stock_section),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    item(key = "stock") { Fact(R.string.inventory_detail_stock, part.totalStock.formatQuantity()) }
                    item(key = "min") { Fact(R.string.inventory_detail_minimum, part.minimumStock.formatQuantity()) }
                    part.basePrice?.let { price ->
                        item(key = "price") { Fact(R.string.inventory_detail_price, price.format()) }
                    }
                    part.category?.let { category ->
                        item(key = "category") {
                            Fact(R.string.inventory_detail_category, stringResource(category.labelRes()))
                        }
                    }
                }
                item(key = "batches-header") {
                    AtelierSectionHeader(
                        title = stringResource(R.string.batches_title),
                        count = if (state.canReadBatches && state.batches.isNotEmpty()) state.batches.size else null,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                when {
                    !state.canReadBatches -> item(key = "batches-no-permission") {
                        AtelierNoPermissionState(
                            text = stringResource(R.string.batches_no_permission),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    state.batches.isEmpty() && state.isLoading -> items(2, key = { "bone-$it" }) {
                        AtelierListRow(title = "Lote L-0001", subtitle = "Saldo: 6 · Ingresó el 10/03/2026", loading = true)
                    }
                    state.batches.isEmpty() -> item(key = "batches-empty") {
                        AtelierEmptyState(
                            text = stringResource(R.string.batches_empty),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    else -> items(state.batches, key = { it.id }) { batch ->
                        BatchRow(batch, isNext = batch.id == state.nextBatchId)
                    }
                }
            }
        }
    }
}

@Composable
private fun Fact(labelRes: Int, value: String) {
    AtelierListRow(
        title = stringResource(labelRes),
        trailing = {
            Text(text = value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    )
}

@Composable
private fun BatchRow(batch: Batch, isNext: Boolean) {
    val arrival = batch.arrivalDate?.let { stringResource(R.string.batch_arrival, it.formatLimaDate()) }
    val cost = batch.unitCost?.let { stringResource(R.string.batch_unit_cost, it.format()) }
    AtelierListRow(
        title = stringResource(R.string.batch_number, batch.batchNumber),
        subtitle = listOfNotNull(
            stringResource(R.string.batch_remaining, batch.remainingQty.formatQuantity()),
            arrival,
            cost
        ).joinToString(" · "),
        trailing = {
            when {
                isNext -> AtelierChip(text = stringResource(R.string.batch_next), kind = ChipKind.Info)
                batch.isActive -> AtelierChip(text = stringResource(R.string.batch_waiting), kind = ChipKind.Ok)
                else -> AtelierChip(text = stringResource(R.string.batch_depleted), kind = ChipKind.Idle)
            }
        }
    )
}

private val previewPart = Part(
    id = "1", name = "Pastillas de freno delanteras", sku = "BRK-001", category = ItemCategory.BRAKES,
    basePrice = Money(BigDecimal("89.90"), Money.PEN), totalStock = BigDecimal("14"), minimumStock = BigDecimal("5"),
    unitOfMeasure = "UNIT", status = PartStatus.ACTIVE
)

private fun previewBatch(id: String, number: String, remaining: String, month: Int) = Batch(
    id = id, itemId = "1", batchNumber = number, remainingQty = BigDecimal(remaining),
    unitCost = Money(BigDecimal("52.00"), Money.PEN), arrivalDate = Instant.parse("2026-0$month-10T12:00:00Z")
)

private val previewState = PartDetailUiState(
    part = previewPart, isLoading = false, canReadBatches = true,
    batches = listOf(
        previewBatch("b1", "L-0001", "0", 3),
        previewBatch("b2", "L-0002", "6", 5),
        previewBatch("b3", "L-0003", "8", 7)
    )
)

@Preview(name = "Repuesto · claro", widthDp = 390, heightDp = 844)
@Composable
private fun PartDetailPreviewLight() = AtelierWorkshopTheme(darkTheme = false) {
    PartDetailContent(previewState, onBack = {}, onRefresh = {})
}

@Preview(name = "Repuesto · oscuro", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PartDetailPreviewDark() = AtelierWorkshopTheme(darkTheme = true) {
    PartDetailContent(previewState, onBack = {}, onRefresh = {})
}

@Preview(name = "Repuesto · sin permiso de lotes", widthDp = 390, heightDp = 844)
@Composable
private fun PartDetailPreviewNoPermission() = AtelierWorkshopTheme(darkTheme = false) {
    PartDetailContent(previewState.copy(canReadBatches = false, batches = emptyList()), onBack = {}, onRefresh = {})
}
