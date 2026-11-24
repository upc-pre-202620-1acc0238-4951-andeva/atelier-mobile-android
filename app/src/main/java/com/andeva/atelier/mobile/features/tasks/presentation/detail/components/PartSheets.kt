package com.andeva.atelier.mobile.features.tasks.presentation.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
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
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBottomSheet
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSelectionCard
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
 * US32 E2: hoja inferior para buscar un repuesto, elegirlo (bloqueando agotados, US31 E3) y fijar la cantidad.
 * Si pide más de lo disponible, el backend responde 409 y su `message` se muestra dentro de la hoja (E3).
 */
@Composable
internal fun ChargePartSheet(
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
    AtelierBottomSheet(
        title = stringResource(R.string.charge_title),
        onDismiss = onDismiss,
        actions = {
            AtelierButton(
                text = stringResource(R.string.charge_confirm),
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                enabled = form.selected != null && form.quantity.isNotBlank() && !isSaving,
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
    ) {
        if (form.selected == null) {
            AtelierTextField(
                label = stringResource(R.string.catalog_search_label),
                value = form.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
                placeholder = stringResource(R.string.catalog_search_hint),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )
            Text(
                text = stringResource(R.string.charge_select_part_prompt),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            when {
                form.isSearching -> Text(
                    text = stringResource(R.string.search_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AtelierTheme.colors.inkSecondary
                )
                form.results.isEmpty() -> Text(
                    text = stringResource(R.string.catalog_empty_filtered),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AtelierTheme.colors.inkSecondary
                )
                else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    form.results.take(MAX_RESULTS).forEach { part ->
                        PartOptionRow(part = part, onSelect = onSelect, enabled = !isSaving)
                    }
                }
            }
        } else {
            PartOptionRow(part = form.selected, onSelect = {}, enabled = false, selected = true)
            AtelierButton(
                text = stringResource(R.string.charge_change_part_btn),
                onClick = { onQueryChange(form.query) },
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Text,
                enabled = !isSaving
            )
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
        error?.let { AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline) }
    }
}

@Composable
internal fun PartOptionRow(
    part: PartOption,
    onSelect: (PartOption) -> Unit,
    enabled: Boolean,
    selected: Boolean = false
) {
    val stockText = if (part.isAvailable) {
        stringResource(R.string.part_stock, part.totalStock.formatQuantity())
    } else {
        stringResource(R.string.stock_out)
    }
    val priceFormatted = part.basePrice?.let {
        stringResource(R.string.part_price, Money(it, Money.PEN).format())
    }

    AtelierSelectionCard(
        title = part.name,
        subtitle = stockText,
        price = priceFormatted,
        badgeText = if (!part.isAvailable) stringResource(R.string.stock_out) else null,
        badgeColor = if (!part.isAvailable) AtelierTheme.colors.badContainer else null,
        selected = selected,
        enabled = enabled && part.isAvailable,
        onClick = { onSelect(part) }
    )
}

/** US33: hoja inferior para corregir la cantidad de un repuesto imputado o devolverlo al lote de origen. */
@Composable
internal fun PartEditSheet(
    form: PartEditForm,
    isSaving: Boolean,
    invalidField: String?,
    error: UiText?,
    onQuantityChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val returning = form.mode == PartEditMode.RETURN
    AtelierBottomSheet(
        title = stringResource(if (returning) R.string.return_title else R.string.edit_part_title),
        onDismiss = onDismiss,
        actions = {
            AtelierButton(
                text = stringResource(if (returning) R.string.return_confirm else R.string.edit_part_confirm),
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                style = if (returning) AtelierButtonStyle.DestructiveFilled else AtelierButtonStyle.Filled,
                enabled = (returning || form.quantity.isNotBlank()) && !isSaving,
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
    ) {
        Text(
            text = form.product.productName ?: stringResource(R.string.task_part_unnamed),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(if (returning) R.string.return_body else R.string.edit_part_body),
            style = MaterialTheme.typography.bodyLarge,
            color = AtelierTheme.colors.inkSecondary
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
        error?.let { AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline) }
    }
}

/** Compatibilidad con `WorkOrderDetailScreen` sin modificar archivos fuera del alcance de `tasks`. */
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
) = ChargePartSheet(
    form = form,
    isSaving = isSaving,
    invalidField = invalidField,
    error = error,
    onQueryChange = onQueryChange,
    onSelect = onSelect,
    onQuantityChange = onQuantityChange,
    onConfirm = onConfirm,
    onDismiss = onDismiss
)

/** Compatibilidad con `WorkOrderDetailScreen` sin modificar archivos fuera del alcance de `tasks`. */
@Composable
internal fun PartEditDialog(
    form: PartEditForm,
    isSaving: Boolean,
    invalidField: String?,
    error: UiText?,
    onQuantityChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) = PartEditSheet(
    form = form,
    isSaving = isSaving,
    invalidField = invalidField,
    error = error,
    onQuantityChange = onQuantityChange,
    onConfirm = onConfirm,
    onDismiss = onDismiss
)

private const val MAX_RESULTS = 6
