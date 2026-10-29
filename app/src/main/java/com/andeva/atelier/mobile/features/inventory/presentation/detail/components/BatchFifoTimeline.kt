package com.andeva.atelier.mobile.features.inventory.presentation.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardShape
import com.andeva.atelier.mobile.core.presentation.format
import com.andeva.atelier.mobile.core.presentation.formatLimaDate
import com.andeva.atelier.mobile.core.presentation.formatQuantity
import com.andeva.atelier.mobile.features.inventory.domain.Batch

/**
 * Línea de tiempo de lotes del más antiguo al más nuevo (US32 E1). El lote [nextBatchId] es el prioritario del
 * próximo descargo FIFO; los agotados se atenúan al final de su orden cronológico.
 */
@Composable
internal fun BatchFifoTimeline(
    batches: List<Batch>,
    nextBatchId: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        batches.forEachIndexed { index, batch ->
            BatchRow(
                batch = batch,
                isNext = batch.id == nextBatchId,
                isLast = index == batches.lastIndex
            )
        }
    }
}

@Composable
private fun BatchRow(batch: Batch, isNext: Boolean, isLast: Boolean) {
    val colors = AtelierTheme.colors
    val dot = when {
        isNext -> MaterialTheme.colorScheme.primary
        batch.isActive -> colors.ok
        else -> colors.muted
    }
    Row(modifier = Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            modifier = Modifier.width(16.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.padding(top = 6.dp).size(12.dp).clip(CircleShape).background(dot))
            if (!isLast) {
                Box(modifier = Modifier.width(2.dp).weight(1f).background(colors.muted.copy(alpha = 0.4f)))
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 12.dp)
                .clip(CardShape)
                .background(colors.card)
                .padding(16.dp)
                .alpha(if (batch.isActive) 1f else 0.6f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    isNext -> AtelierChip(text = stringResource(R.string.batch_next), kind = ChipKind.Info)
                    batch.isActive -> AtelierChip(text = stringResource(R.string.batch_waiting), kind = ChipKind.Ok)
                    else -> AtelierChip(text = stringResource(R.string.batch_depleted), kind = ChipKind.Idle)
                }
            }
            Text(
                text = stringResource(R.string.batch_number, batch.batchNumber),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            batch.arrivalDate?.let {
                Text(
                    text = stringResource(R.string.batch_arrival, it.formatLimaDate()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkSecondary
                )
            }
            Text(
                text = stringResource(R.string.batch_remaining, batch.remainingQty.formatQuantity()),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            batch.unitCost?.let {
                Text(
                    text = stringResource(R.string.batch_unit_cost, it.format()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkSecondary
                )
            }
        }
    }
}
