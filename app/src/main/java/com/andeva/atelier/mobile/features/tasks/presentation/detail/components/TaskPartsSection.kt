package com.andeva.atelier.mobile.features.tasks.presentation.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardShape
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
    canCharge: Boolean,
    canEdit: Boolean,
    isBusy: Boolean,
    onCharge: () -> Unit,
    onEdit: (TaskProduct) -> Unit,
    onReturn: (TaskProduct) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.task_parts_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (products.isEmpty()) {
            Text(
                text = stringResource(R.string.task_parts_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = AtelierTheme.colors.inkSecondary
            )
        }
        products.forEach { product ->
            ProductRow(product, canEdit, isBusy, onEdit = { onEdit(product) }, onReturn = { onReturn(product) })
        }
        if (canCharge) {
            AtelierButton(
                text = stringResource(R.string.task_parts_charge),
                onClick = onCharge,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Tonal,
                enabled = !isBusy
            )
        }
    }
}

@Composable
private fun ProductRow(
    product: TaskProduct,
    canEdit: Boolean,
    isBusy: Boolean,
    onEdit: () -> Unit,
    onReturn: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(AtelierTheme.colors.card)
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
            AtelierButton(
                text = stringResource(R.string.task_part_edit),
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Outlined,
                enabled = !isBusy
            )
            AtelierButton(
                text = stringResource(R.string.task_part_return),
                onClick = onReturn,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Text,
                enabled = !isBusy
            )
        }
    }
}
