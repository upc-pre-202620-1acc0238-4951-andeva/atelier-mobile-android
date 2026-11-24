package com.andeva.atelier.mobile.features.tasks.presentation.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierEmptyState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierGroupedCard
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRequiresConnection
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSectionHeader
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.presentation.format
import com.andeva.atelier.mobile.core.presentation.formatQuantity
import com.andeva.atelier.mobile.features.tasks.domain.TaskProduct

/**
 * Repuestos imputados a la tarea (US33 E1). Con permiso de edición cada uno ofrece corregir la cantidad o devolverlo
 * al inventario; el precio queda fijo al imputar y no se recalcula aquí (US35 E3).
 */
@Composable
internal fun TaskPartsSection(
    products: List<TaskProduct>,
    available: Boolean,
    canCharge: Boolean,
    canEdit: Boolean,
    isOnline: Boolean,
    isBusy: Boolean,
    onCharge: () -> Unit,
    onEdit: (TaskProduct) -> Unit,
    onReturn: (TaskProduct) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AtelierSectionHeader(
            title = stringResource(R.string.task_parts_title),
            count = if (available) products.size else null
        )
        if (!available) {
            // D3: sin el detalle en línea no se sabe qué repuestos hay; no se dice que no hay ninguno.
            Text(
                text = stringResource(R.string.task_parts_online_only),
                style = MaterialTheme.typography.bodyLarge,
                color = AtelierTheme.colors.inkSecondary
            )
        } else if (products.isEmpty()) {
            AtelierEmptyState(text = stringResource(R.string.task_parts_empty))
        }
        if (available && products.isNotEmpty()) {
            products.forEach { product ->
                ProductCard(
                    product = product,
                    canEdit = canEdit,
                    isOnline = isOnline,
                    isBusy = isBusy,
                    onEdit = { onEdit(product) },
                    onReturn = { onReturn(product) }
                )
            }
        }
        if (canCharge) {
            AtelierButton(
                text = stringResource(R.string.task_parts_charge),
                onClick = onCharge,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                style = AtelierButtonStyle.Tonal,
                enabled = isOnline && !isBusy,
                icon = Icons.Outlined.Add
            )
            if (!isOnline) AtelierRequiresConnection()
        }
    }
}

@Composable
private fun ProductCard(
    product: TaskProduct,
    canEdit: Boolean,
    isOnline: Boolean,
    isBusy: Boolean,
    onEdit: () -> Unit,
    onReturn: () -> Unit
) {
    AtelierGroupedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = product.productName ?: stringResource(R.string.task_part_unnamed),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(R.string.task_part_quantity, product.quantity.formatQuantity()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                product.unitPrice?.let {
                    Text(
                        text = stringResource(R.string.task_part_unit_price, it.format()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AtelierTheme.colors.inkSecondary
                    )
                }
            }
            product.totalAmount?.let {
                Text(
                    text = stringResource(R.string.task_part_total, it.format()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AtelierTheme.colors.inkSecondary
                )
            }
            if (canEdit) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AtelierButton(
                        text = stringResource(R.string.task_part_edit),
                        onClick = onEdit,
                        modifier = Modifier.weight(1f),
                        style = AtelierButtonStyle.Outlined,
                        enabled = isOnline && !isBusy
                    )
                    AtelierButton(
                        text = stringResource(R.string.task_part_return),
                        onClick = onReturn,
                        modifier = Modifier.weight(1f),
                        style = AtelierButtonStyle.Text,
                        enabled = isOnline && !isBusy
                    )
                }
            }
        }
    }
}
