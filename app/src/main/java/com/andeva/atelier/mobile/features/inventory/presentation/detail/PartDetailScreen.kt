package com.andeva.atelier.mobile.features.inventory.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.core.presentation.format
import com.andeva.atelier.mobile.core.presentation.formatQuantity
import com.andeva.atelier.mobile.features.inventory.domain.Batch
import com.andeva.atelier.mobile.features.inventory.domain.ItemCategory
import com.andeva.atelier.mobile.features.inventory.domain.Part
import com.andeva.atelier.mobile.features.inventory.domain.PartStatus
import com.andeva.atelier.mobile.features.inventory.presentation.detail.components.BatchFifoTimeline
import com.andeva.atelier.mobile.features.inventory.presentation.labelRes
import com.andeva.atelier.mobile.features.inventory.presentation.stockChipKind
import com.andeva.atelier.mobile.features.inventory.presentation.stockLabelRes
import java.math.BigDecimal
import java.time.Instant

/** US32: ficha del repuesto con sus lotes de compra en orden FIFO. */
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

@Composable
internal fun PartDetailContent(
    state: PartDetailUiState,
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
        AtelierTopBar(title = stringResource(R.string.part_detail_title), onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            state.part?.let { PartSummary(it) }
            if (state.showingCache) {
                AtelierBanner(
                    message = state.error?.asString() ?: stringResource(R.string.batches_cache_notice),
                    icon = Icons.Outlined.WifiOff,
                    kind = BannerKind.Wait
                )
            } else {
                state.error?.let { AtelierBanner(message = it.asString(), icon = Icons.Filled.Error) }
            }

            Text(
                text = stringResource(R.string.batches_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.batches_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = AtelierTheme.colors.inkSecondary
            )
            when {
                !state.canReadBatches -> AtelierBanner(
                    message = stringResource(R.string.batches_no_permission),
                    icon = Icons.Outlined.Info,
                    kind = BannerKind.Wait
                )
                state.batches.isEmpty() -> Text(
                    text = stringResource(if (state.isLoading) R.string.search_loading else R.string.batches_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = AtelierTheme.colors.inkSecondary
                )
                else -> BatchFifoTimeline(batches = state.batches, nextBatchId = state.nextBatchId)
            }
            if (state.canReadBatches) {
                AtelierButton(
                    text = stringResource(R.string.tasks_refresh),
                    onClick = onRefresh,
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.Outlined,
                    loading = state.isLoading
                )
            }
        }
    }
}

@Composable
private fun PartSummary(part: Part) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PartChips(part)
        Text(text = part.name, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        Text(
            text = stringResource(R.string.part_sku, part.sku),
            style = MaterialTheme.typography.bodyMedium,
            color = AtelierTheme.colors.inkSecondary
        )
        Text(
            text = stringResource(R.string.part_stock, part.totalStock.formatQuantity()),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        part.basePrice?.let {
            Text(
                text = stringResource(R.string.part_price, it.format()),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun PartChips(part: Part) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AtelierChip(text = stringResource(part.stockLabelRes()), kind = part.stockChipKind())
        part.category?.let { AtelierChip(text = stringResource(it.labelRes())) }
    }
}

private val previewPart = Part(
    id = "1", name = "Pastillas de freno delanteras", sku = "BRK-001", category = ItemCategory.BRAKES,
    basePrice = Money(BigDecimal("89.90"), Money.PEN), totalStock = BigDecimal("14"), minimumStock = BigDecimal("5"),
    unitOfMeasure = "UNIT", status = PartStatus.ACTIVE
)

private fun previewBatch(id: String, number: String, remaining: String, day: Int) = Batch(
    id = id, itemId = "1", batchNumber = number, remainingQty = BigDecimal(remaining),
    unitCost = Money(BigDecimal("52.00"), Money.PEN), arrivalDate = Instant.parse("2026-0${day}-10T12:00:00Z")
)

@Preview(name = "Repuesto · lotes FIFO · oscuro", widthDp = 360, heightDp = 1000, showBackground = true)
@Composable
private fun PartDetailPreviewDark() = AtelierWorkshopTheme(darkTheme = true) {
    PartDetailContent(
        state = PartDetailUiState(
            part = previewPart, isLoading = false, canReadBatches = true,
            batches = listOf(
                previewBatch("b1", "L-0001", "0", 3),
                previewBatch("b2", "L-0002", "6", 5),
                previewBatch("b3", "L-0003", "8", 7)
            )
        ),
        onBack = {}, onRefresh = {}
    )
}

@Preview(name = "Repuesto · sin permiso · claro", widthDp = 360, heightDp = 700, showBackground = true)
@Composable
private fun PartDetailPreviewNoPermission() = AtelierWorkshopTheme(darkTheme = false) {
    PartDetailContent(
        state = PartDetailUiState(part = previewPart, isLoading = false, canReadBatches = false),
        onBack = {}, onRefresh = {}
    )
}
