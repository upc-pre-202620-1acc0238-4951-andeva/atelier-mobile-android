package com.andeva.atelier.mobile.features.tasks.presentation.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.WarningAmber
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
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRequiresConnection
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.domain.BusinessTime
import com.andeva.atelier.mobile.features.tasks.domain.PartOption
import com.andeva.atelier.mobile.features.tasks.domain.PauseReason
import com.andeva.atelier.mobile.features.tasks.domain.PendingEvidence
import com.andeva.atelier.mobile.features.tasks.presentation.detail.CompleteForm
import com.andeva.atelier.mobile.features.tasks.presentation.detail.ReopenTaskForm
import com.andeva.atelier.mobile.features.tasks.presentation.detail.RequestSuspensionForm
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SuspensionTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-PE"))

/** US27 E2/E3: hoja inferior para tipificar la pausa (`BREAK` o `END_OF_SHIFT`). */
@Composable
internal fun PauseReasonSheet(
    isOnline: Boolean,
    isSaving: Boolean,
    onSelect: (PauseReason) -> Unit,
    onDismiss: () -> Unit
) {
    AtelierBottomSheet(
        title = stringResource(R.string.task_pause_title),
        onDismiss = onDismiss,
        actions = {
            if (!isOnline) AtelierRequiresConnection()
            AtelierButton(
                text = stringResource(R.string.task_pause_break),
                onClick = { onSelect(PauseReason.BREAK) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                style = AtelierButtonStyle.Tonal,
                enabled = isOnline && !isSaving,
                icon = Icons.Outlined.Coffee
            )
            AtelierButton(
                text = stringResource(R.string.task_pause_end_of_shift),
                onClick = { onSelect(PauseReason.END_OF_SHIFT) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                style = AtelierButtonStyle.Outlined,
                enabled = isOnline && !isSaving,
                icon = Icons.Outlined.Logout
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
            text = stringResource(R.string.task_pause_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** US30: hoja inferior para confirmar horas efectivas precargadas con `SuggestLaborHoursUseCase` y notas. */
@Composable
internal fun CompleteTaskSheet(
    form: CompleteForm,
    isOnline: Boolean,
    isSaving: Boolean,
    invalidField: String?,
    error: UiText?,
    onHoursChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    hasAssemblyEvidence: Boolean = true
) {
    AtelierBottomSheet(
        title = stringResource(R.string.task_complete_title),
        onDismiss = onDismiss,
        actions = {
            if (!isOnline) AtelierRequiresConnection()
            AtelierButton(
                text = stringResource(R.string.task_complete_confirm),
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                enabled = isOnline && hasAssemblyEvidence && form.hours.isNotBlank() && !isSaving,
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
            text = stringResource(R.string.task_complete_body),
            style = MaterialTheme.typography.bodyLarge,
            color = AtelierTheme.colors.inkSecondary
        )
        if (!hasAssemblyEvidence) {
            AtelierBanner(
                message = stringResource(R.string.tasks_seg2_complete_missing_assembly_warning),
                icon = Icons.Outlined.WarningAmber,
                kind = BannerKind.Wait
            )
        }
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
            errorText = if (invalidField == "notes") stringResource(R.string.task_field_notes) else null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )
        error?.let { AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline) }
    }
}

/** US29 E1 (mecánico): solicitar suspensión por pieza defectuosa o faltante + repuesto opcional del catálogo. */
@Composable
internal fun RequestSuspensionSheet(
    form: RequestSuspensionForm,
    isOnline: Boolean,
    isSaving: Boolean,
    invalidField: String?,
    error: UiText?,
    onMissingItemChange: (String) -> Unit,
    onPartQueryChange: (String) -> Unit,
    onPartSelect: (PartOption?) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    canSearchParts: Boolean = true
) {
    AtelierBottomSheet(
        title = stringResource(R.string.tasks_seg2_suspension_request_title),
        onDismiss = onDismiss,
        actions = {
            if (!isOnline) AtelierRequiresConnection()
            AtelierButton(
                text = stringResource(R.string.tasks_seg2_suspension_request_confirm),
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                enabled = isOnline && form.missingItemDescription.isNotBlank() && !isSaving,
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
            text = stringResource(R.string.tasks_seg2_suspension_request_body),
            style = MaterialTheme.typography.bodyLarge,
            color = AtelierTheme.colors.inkSecondary
        )
        AtelierTextField(
            label = stringResource(R.string.tasks_seg2_suspension_item_label),
            value = form.missingItemDescription,
            onValueChange = onMissingItemChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving,
            placeholder = stringResource(R.string.tasks_seg2_suspension_item_hint),
            isError = invalidField == "missingItemDescription",
            errorText = if (invalidField == "missingItemDescription") {
                stringResource(R.string.tasks_seg2_suspension_item_error)
            } else null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        if (canSearchParts) {
            if (form.selectedPart != null) {
                PartOptionRow(part = form.selectedPart, onSelect = {}, enabled = false, selected = true)
                AtelierButton(
                    text = stringResource(R.string.tasks_seg2_suspension_part_clear),
                    onClick = { onPartSelect(null) },
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.Text,
                    enabled = !isSaving
                )
            } else {
                AtelierTextField(
                    label = stringResource(R.string.tasks_seg2_suspension_part_optional),
                    value = form.query,
                    onValueChange = onPartQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving && isOnline,
                    placeholder = stringResource(R.string.catalog_search_hint),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                )
                when {
                    form.isSearching -> Text(
                        text = stringResource(R.string.search_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AtelierTheme.colors.inkSecondary
                    )
                    form.results.isNotEmpty() -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        form.results.take(4).forEach { part ->
                            PartOptionRow(part = part, onSelect = { onPartSelect(it) }, enabled = !isSaving)
                        }
                    }
                }
            }
        }
        error?.let { AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline) }
    }
}

/**
 * US29 (supervisor): aprueba o rechaza una solicitud pendiente (`ApproveSuspensionUseCase` / `RejectSuspensionUseCase`)
 * o levanta una suspensión activa cuando llega el repuesto (`ResumeSuspendedTaskUseCase`, H5/H6).
 */
@Composable
internal fun ReviewSuspensionSheet(
    taskTitle: String,
    isSuspended: Boolean,
    pendingReason: String?,
    missingItemDescription: String?,
    requestedAt: Instant?,
    isOnline: Boolean,
    busyApprove: Boolean,
    busyReject: Boolean,
    busyResume: Boolean,
    error: UiText?,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onResume: () -> Unit,
    onDismiss: () -> Unit
) {
    val busy = busyApprove || busyReject || busyResume
    AtelierBottomSheet(
        title = stringResource(
            if (isSuspended) R.string.tasks_seg2_suspension_resume_sheet_title else R.string.orders_suspension_title
        ),
        onDismiss = onDismiss,
        actions = {
            if (!isOnline) AtelierRequiresConnection()
            if (isSuspended) {
                AtelierButton(
                    text = stringResource(R.string.orders_resume_task),
                    onClick = onResume,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    enabled = isOnline && !busy,
                    loading = busyResume,
                    icon = Icons.Outlined.PlayArrow
                )
            } else {
                AtelierButton(
                    text = stringResource(R.string.orders_suspension_approve),
                    onClick = onApprove,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    enabled = isOnline && !busy,
                    loading = busyApprove
                )
                AtelierButton(
                    text = stringResource(R.string.orders_suspension_reject),
                    onClick = onReject,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    style = AtelierButtonStyle.Destructive,
                    enabled = isOnline && !busy,
                    loading = busyReject
                )
            }
            AtelierButton(
                text = stringResource(R.string.proposals_cancel),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Text,
                enabled = !busy
            )
        }
    ) {
        Text(
            text = taskTitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (isSuspended) {
            Text(
                text = stringResource(R.string.tasks_seg2_suspension_resume_sheet_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        } else {
            Text(
                text = pendingReason?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.orders_suspension_no_reason),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        missingItemDescription?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = stringResource(R.string.orders_suspension_missing_item, it),
                style = MaterialTheme.typography.bodyMedium,
                color = AtelierTheme.colors.inkSecondary
            )
        }
        requestedAt?.let {
            Text(
                text = stringResource(
                    R.string.tasks_seg2_suspension_requested_at,
                    SuspensionTimeFormatter.format(it.atZone(BusinessTime.ZONE))
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = AtelierTheme.colors.inkSecondary
            )
        }
        error?.let { AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline) }
    }
}

/** Reapertura de tarea completada (`POST /api/v1/tasks/{taskId}/reopen`). */
@Composable
internal fun ReopenTaskSheet(
    form: ReopenTaskForm,
    isOnline: Boolean,
    isSaving: Boolean,
    error: UiText?,
    onReasonChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AtelierBottomSheet(
        title = stringResource(R.string.tasks_seg2_reopen_title),
        onDismiss = onDismiss,
        actions = {
            if (!isOnline) AtelierRequiresConnection()
            AtelierButton(
                text = stringResource(R.string.tasks_seg2_reopen_confirm),
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                enabled = isOnline && !isSaving,
                loading = isSaving,
                icon = Icons.Outlined.RestartAlt
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
            text = stringResource(R.string.tasks_seg2_reopen_body),
            style = MaterialTheme.typography.bodyLarge,
            color = AtelierTheme.colors.inkSecondary
        )
        AtelierTextField(
            label = stringResource(R.string.tasks_seg2_reopen_reason_label),
            value = form.reason,
            onValueChange = onReasonChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving,
            placeholder = stringResource(R.string.tasks_seg2_reopen_reason_hint),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )
        error?.let { AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline) }
    }
}

/** Hoja para reintentar o descartar una evidencia fotográfica cuya subida falló en `pending_operations`. */
@Composable
internal fun FailedEvidenceSheet(
    evidence: PendingEvidence,
    onRetry: () -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit
) {
    AtelierBottomSheet(
        title = stringResource(R.string.tasks_seg2_evidence_failed_sheet_title),
        onDismiss = onDismiss,
        actions = {
            AtelierButton(
                text = stringResource(R.string.sync_retry_btn),
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                icon = Icons.Outlined.Refresh
            )
            AtelierButton(
                text = stringResource(R.string.sync_discard_btn),
                onClick = onDiscard,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                style = AtelierButtonStyle.Destructive,
                icon = Icons.Outlined.DeleteOutline
            )
            AtelierButton(
                text = stringResource(R.string.proposals_cancel),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Text
            )
        }
    ) {
        Text(
            text = evidence.description.orEmpty(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        evidence.lastErrorMessage?.takeIf { it.isNotBlank() }?.let { err ->
            AtelierBanner(message = err, icon = Icons.Outlined.ErrorOutline)
        }
    }
}

/** Confirmación al retroceder con una nota de evidencia escrita y aún no fotografiada. */
@Composable
internal fun UnsavedEvidenceExitSheet(
    onConfirmExit: () -> Unit,
    onDismiss: () -> Unit
) {
    AtelierBottomSheet(
        title = stringResource(R.string.tasks_seg2_unsaved_exit_title),
        onDismiss = onDismiss,
        actions = {
            AtelierButton(
                text = stringResource(R.string.tasks_seg2_unsaved_exit_confirm),
                onClick = onConfirmExit,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                style = AtelierButtonStyle.Destructive
            )
            AtelierButton(
                text = stringResource(R.string.proposals_cancel),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Text
            )
        }
    ) {
        Text(
            text = stringResource(R.string.tasks_seg2_unsaved_exit_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
