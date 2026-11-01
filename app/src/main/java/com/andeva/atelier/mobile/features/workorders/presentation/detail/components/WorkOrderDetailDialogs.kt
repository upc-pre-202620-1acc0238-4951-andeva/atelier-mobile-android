package com.andeva.atelier.mobile.features.workorders.presentation.detail.components

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
import com.andeva.atelier.mobile.core.presentation.format
import com.andeva.atelier.mobile.features.workorders.domain.ServiceOption
import com.andeva.atelier.mobile.features.workorders.presentation.detail.AddTaskForm
import com.andeva.atelier.mobile.features.workorders.presentation.detail.EditLaborForm
import com.andeva.atelier.mobile.features.workorders.presentation.detail.RejectQuotationForm

/** US22 E1: agregar una nueva tarea con su servicio y precio de mano de obra sin IGV. */
@Composable
internal fun AddTaskDialog(
    form: AddTaskForm,
    services: List<ServiceOption>,
    isSaving: Boolean,
    invalidField: String?,
    error: UiText?,
    onSelectService: (ServiceOption) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text(stringResource(R.string.order_add_task_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.order_add_task_service),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                services.forEach { service ->
                    val selected = service.id == form.serviceId
                    val label = service.basePrice?.let { "${service.name} · ${it.format()}" } ?: service.name
                    AtelierButton(
                        text = label,
                        onClick = { onSelectService(service) },
                        modifier = Modifier.fillMaxWidth(),
                        style = if (selected) AtelierButtonStyle.Tonal else AtelierButtonStyle.Outlined,
                        enabled = !isSaving
                    )
                }
                if (invalidField == "serviceId") {
                    Text(
                        text = stringResource(R.string.order_field_service),
                        style = MaterialTheme.typography.bodySmall,
                        color = AtelierTheme.colors.bad
                    )
                }
                AtelierTextField(
                    label = stringResource(R.string.order_add_task_description),
                    value = form.description,
                    onValueChange = onDescriptionChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    isError = invalidField == "description",
                    errorText = if (invalidField == "description") stringResource(R.string.order_field_description) else null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                AtelierTextField(
                    label = stringResource(R.string.order_add_task_price),
                    value = form.price,
                    onValueChange = onPriceChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    isError = invalidField == "price",
                    errorText = if (invalidField == "price") stringResource(R.string.order_field_price) else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done)
                )
                error?.let { AtelierBanner(message = it.asString(), icon = Icons.Filled.Error) }
                AtelierButton(
                    text = stringResource(R.string.order_add_task_confirm),
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = form.serviceId != null && form.description.isNotBlank(),
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

/** US22 E2: actualizar el precio de mano de obra sin IGV de una tarea existente. */
@Composable
internal fun EditLaborDialog(
    form: EditLaborForm,
    isSaving: Boolean,
    invalidField: String?,
    error: UiText?,
    onPriceChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text(stringResource(R.string.order_edit_labor_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = form.taskTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                AtelierTextField(
                    label = stringResource(R.string.order_add_task_price),
                    value = form.price,
                    onValueChange = onPriceChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    isError = invalidField == "price",
                    errorText = if (invalidField == "price") stringResource(R.string.order_field_price) else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done)
                )
                error?.let { AtelierBanner(message = it.asString(), icon = Icons.Filled.Error) }
                AtelierButton(
                    text = stringResource(R.string.order_edit_labor_confirm),
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = form.price.isNotBlank(),
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

/**
 * US25 E2: modal «Cancelar la orden» tras desistimiento del cliente.
 * «Los repuestos reservados volverán al inventario.» Exige motivo obligatorio (`reason`).
 */
@Composable
internal fun RejectQuotationDialog(
    form: RejectQuotationForm,
    isSaving: Boolean,
    invalidField: String?,
    error: UiText?,
    onReasonChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text(stringResource(R.string.quotation_reject_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.quotation_reject_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = AtelierTheme.colors.inkSecondary
                )
                AtelierTextField(
                    label = stringResource(R.string.quotation_reject_reason_label),
                    value = form.reason,
                    onValueChange = onReasonChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    placeholder = stringResource(R.string.quotation_reject_reason_hint),
                    isError = invalidField == "reason",
                    errorText = if (invalidField == "reason") stringResource(R.string.quotation_reject_field_error) else null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
                error?.let { AtelierBanner(message = it.asString(), icon = Icons.Filled.Error) }
                AtelierButton(
                    text = stringResource(R.string.quotation_reject_confirm),
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = form.reason.isNotBlank(),
                    loading = isSaving
                )
                AtelierButton(
                    text = stringResource(R.string.quotation_reject_back),
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.Text,
                    enabled = !isSaving
                )
            }
        }
    )
}
