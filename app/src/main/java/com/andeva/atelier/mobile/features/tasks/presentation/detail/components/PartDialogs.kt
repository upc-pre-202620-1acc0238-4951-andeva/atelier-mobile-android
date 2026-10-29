package com.andeva.atelier.mobile.features.tasks.presentation.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.core.presentation.format
import com.andeva.atelier.mobile.core.presentation.formatQuantity
import com.andeva.atelier.mobile.features.tasks.domain.PartOption
import com.andeva.atelier.mobile.features.tasks.presentation.detail.ChargeForm
import com.andeva.atelier.mobile.features.tasks.presentation.detail.PartEditForm
import com.andeva.atelier.mobile.features.tasks.presentation.detail.PartEditMode

/**
 * US32 E2: buscar el repuesto, elegirlo y fijar la cantidad. Los agotados no se pueden elegir (US31 E3); si pide más
 * de lo que hay, el backend responde 409 y su `message` se muestra aquí (E3).
 */
@Composable
internal fun ChargePartDialog(
    form: ChargeForm,
    isSaving: Boolean,
    invalidField: String?,
    error: UiText?,
    onQueryChange: (String) -> Unit,
    onSelect: (PartOption) -> Unit,
    onQuantityChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text(stringResource(R.string.charge_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AtelierTextField(
                    label = stringResource(R.string.catalog_search_label),
                    value = form.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    placeholder = stringResource(R.string.catalog_search_hint),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                )
                when {
                    form.isSearching -> Text(
                        text = stringResource(R.string.search_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AtelierTheme.colors.inkSecondary
                    )
                    form.query.isNotBlank() && form.results.isEmpty() && form.selected == null -> Text(
                        text = stringResource(R.string.catalog_empty_filtered),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AtelierTheme.colors.inkSecondary
                    )
                }
                if (form.selected == null) {
                    form.results.take(MAX_RESULTS).forEach { part -> PartOptionRow(part, onSelect, !isSaving) }
                } else {
                    PartOptionRow(form.selected, onSelect, enabled = false, selected = true)
                    AtelierTextField(
                        label = stringResource(R.string.charge_quantity),
                        value = form.quantity,
                        onValueChange = onQuantityChange,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving,
                        isError = invalidField == "quantity",
                        errorText = if (invalidField == "quantity") stringResource(R.string.task_field_quantity) else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done)
                    )
                }
                error?.let { AtelierBanner(message = it.asString(), icon = Icons.Filled.Error) }
                AtelierButton(
                    text = stringResource(R.string.charge_confirm),
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = form.selected != null && form.quantity.isNotBlank(),
                    loading = isSaving
                )
                AtelierButton(
                    text = stringResource(R.string.proposals_cancel),
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.Text,
                    enabled = !isSaving
                )
            }
        }
    )
}

@Composable
private fun PartOptionRow(
    part: PartOption,
    onSelect: (PartOption) -> Unit,
    enabled: Boolean,
    selected: Boolean = false
) {
    val detail = buildString {
        append(stringResource(R.string.part_stock, part.totalStock.formatQuantity()))
        part.basePrice?.let {
            append(" · ")
            append(stringResource(R.string.part_price, Money(it, Money.PEN).format()))
        }
    }
    AtelierButton(
        text = if (part.isAvailable) "${part.name}\n$detail" else "${part.name}\n${stringResource(R.string.stock_out)}",
        onClick = { onSelect(part) },
        modifier = Modifier.fillMaxWidth(),
        style = if (selected) AtelierButtonStyle.Tonal else AtelierButtonStyle.Outlined,
        enabled = enabled && part.isAvailable
    )
}

/** US33: corregir la cantidad de un repuesto imputado o devolverlo al lote de origen. */
@Composable
internal fun PartEditDialog(
    form: PartEditForm,
    isSaving: Boolean,
    invalidField: String?,
    error: UiText?,
    onQuantityChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val returning = form.mode == PartEditMode.RETURN
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text(stringResource(if (returning) R.string.return_title else R.string.edit_part_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = form.product.productName ?: stringResource(R.string.task_part_unnamed),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(if (returning) R.string.return_body else R.string.edit_part_body),
                    style = MaterialTheme.typography.bodyLarge
                )
                if (!returning) {
                    AtelierTextField(
                        label = stringResource(R.string.charge_quantity),
                        value = form.quantity,
                        onValueChange = onQuantityChange,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving,
                        isError = invalidField == "quantity",
                        errorText = if (invalidField == "quantity") stringResource(R.string.task_field_quantity) else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done)
                    )
                }
                error?.let { AtelierBanner(message = it.asString(), icon = Icons.Filled.Error) }
                AtelierButton(
                    text = stringResource(if (returning) R.string.return_confirm else R.string.edit_part_confirm),
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = returning || form.quantity.isNotBlank(),
                    loading = isSaving
                )
                AtelierButton(
                    text = stringResource(R.string.proposals_cancel),
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.Text,
                    enabled = !isSaving
                )
            }
        }
    )
}

private const val MAX_RESULTS = 6
