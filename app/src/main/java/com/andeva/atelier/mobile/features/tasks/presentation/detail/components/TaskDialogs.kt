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
import com.andeva.atelier.mobile.features.tasks.domain.PauseReason
import com.andeva.atelier.mobile.features.tasks.presentation.detail.CompleteForm

/** US27 E2: tipifica la pausa. El fin de turno solo lo acepta el backend si el turno ya terminó (E3). */
@Composable
internal fun PauseReasonDialog(onSelect: (PauseReason) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text(stringResource(R.string.task_pause_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.task_pause_body),
                    style = MaterialTheme.typography.bodyLarge
                )
                AtelierButton(
                    text = stringResource(R.string.task_pause_break),
                    onClick = { onSelect(PauseReason.BREAK) },
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.Tonal
                )
                AtelierButton(
                    text = stringResource(R.string.task_pause_end_of_shift),
                    onClick = { onSelect(PauseReason.END_OF_SHIFT) },
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.Tonal
                )
                AtelierButton(
                    text = stringResource(R.string.proposals_cancel),
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.Text
                )
            }
        }
    )
}

/** US30: confirmar las horas efectivas (sugeridas desde el cronómetro) y cerrar la faena. */
@Composable
internal fun CompleteTaskDialog(
    form: CompleteForm,
    isSaving: Boolean,
    invalidField: String?,
    error: UiText?,
    onHoursChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text(stringResource(R.string.task_complete_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.task_complete_body),
                    style = MaterialTheme.typography.bodyLarge
                )
                AtelierTextField(
                    label = stringResource(R.string.task_complete_hours),
                    value = form.hours,
                    onValueChange = onHoursChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    isError = invalidField == "actualLaborHours",
                    errorText = if (invalidField == "actualLaborHours") stringResource(R.string.task_field_hours) else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
                )
                AtelierTextField(
                    label = stringResource(R.string.task_complete_notes),
                    value = form.notes,
                    onValueChange = onNotesChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    isError = invalidField == "notes",
                    errorText = if (invalidField == "notes") stringResource(R.string.task_field_notes) else null
                )
                error?.let { AtelierBanner(message = it.asString(), icon = Icons.Filled.Error) }
                AtelierButton(
                    text = stringResource(R.string.task_complete_confirm),
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = form.hours.isNotBlank(),
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
