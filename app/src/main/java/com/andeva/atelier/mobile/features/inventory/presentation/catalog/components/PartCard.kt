package com.andeva.atelier.mobile.features.inventory.presentation.catalog.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardShape
import com.andeva.atelier.mobile.core.presentation.format
import com.andeva.atelier.mobile.core.presentation.formatQuantity
import com.andeva.atelier.mobile.features.inventory.domain.Part
import com.andeva.atelier.mobile.features.inventory.presentation.labelRes
import com.andeva.atelier.mobile.features.inventory.presentation.stockChipKind
import com.andeva.atelier.mobile.features.inventory.presentation.stockLabelRes

/**
 * Repuesto del catálogo con su semáforo de existencias, saldo y precio base unitario sin IGV (US31 E2). Un agotado
 * se atenúa y se marca en rojo (E3): se puede consultar, pero no se elige para imputar.
 */
@Composable
internal fun PartCard(part: Part, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(AtelierTheme.colors.card)
            .clickable(onClick = onClick)
            .heightIn(min = 72.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AtelierChip(text = stringResource(part.stockLabelRes()), kind = part.stockChipKind())
            part.category?.let { AtelierChip(text = stringResource(it.labelRes())) }
        }
        Column(
            modifier = Modifier.alpha(if (part.isOutOfStock) 0.6f else 1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = part.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            part.brand?.let {
                Text(
                    text = stringResource(R.string.part_brand, it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AtelierTheme.colors.inkSecondary
                )
            }
            Text(
                text = stringResource(R.string.part_sku, part.sku),
                style = MaterialTheme.typography.bodyMedium,
                color = AtelierTheme.colors.inkSecondary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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
    }
}
