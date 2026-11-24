package com.andeva.atelier.mobile.features.tasks.presentation.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierEmptyState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierPhotoGrid
import com.andeva.atelier.mobile.core.designsystem.component.AtelierPhotoItem
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSectionHeader
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSegmentedControl
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.PhotoSyncState
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.domain.BusinessTime
import com.andeva.atelier.mobile.features.tasks.domain.EvidenceKind
import com.andeva.atelier.mobile.features.tasks.domain.PendingEvidence
import com.andeva.atelier.mobile.features.tasks.presentation.detail.TaskAction
import com.andeva.atelier.mobile.features.tasks.presentation.detail.TaskDetailUiState
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

private val EvidenceTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("es-PE"))

private fun Instant.formatEvidenceTime(): String =
    EvidenceTimeFormatter.format(atZone(BusinessTime.ZONE))

@Composable
internal fun TaskEvidenceSection(
    state: TaskDetailUiState,
    onKindChange: (EvidenceKind) -> Unit,
    onNoteChange: (String) -> Unit,
    onTakePhoto: () -> Unit,
    onSelectFailedEvidence: (PendingEvidence) -> Unit,
    modifier: Modifier = Modifier
) {
    val pendingById = state.pendingEvidence.associateBy { "pending-${it.operationId}" }
    val photoItems = buildList {
        state.evidenceImages.forEach { image ->
            add(
                AtelierPhotoItem(
                    id = "synced-${image.id}",
                    model = image.imageUrl,
                    caption = image.description,
                    capturedAtLabel = image.uploadedAt?.formatEvidenceTime(),
                    syncState = PhotoSyncState.SYNCED
                )
            )
        }
        state.pendingEvidence.forEach { pending ->
            add(
                AtelierPhotoItem(
                    id = "pending-${pending.operationId}",
                    model = pending.localFilePath ?: pending.imageUrl,
                    caption = pending.description,
                    capturedAtLabel = pending.capturedAt.formatEvidenceTime(),
                    syncState = if (pending.status == "FAILED") PhotoSyncState.FAILED else PhotoSyncState.PENDING
                )
            )
        }
    }

    val maxLen = state.maxEvidenceNoteLength
    val overLimit = state.evidenceNote.length > maxLen

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AtelierSectionHeader(
            title = stringResource(R.string.tasks_seg2_evidence_section),
            count = photoItems.size
        )
        if (state.canUploadPhotos) {
            Text(
                text = stringResource(R.string.tasks_seg2_evidence_kind_label),
                style = MaterialTheme.typography.labelLarge,
                color = AtelierTheme.colors.inkSecondary
            )
            val evidenceKindLabels = mapOf(
                EvidenceKind.DISASSEMBLY to stringResource(R.string.tasks_seg2_evidence_disassembly),
                EvidenceKind.ASSEMBLY to stringResource(R.string.tasks_seg2_evidence_assembly)
            )
            AtelierSegmentedControl(
                options = listOf(EvidenceKind.DISASSEMBLY, EvidenceKind.ASSEMBLY),
                selected = state.evidenceKind,
                onSelect = onKindChange,
                label = { evidenceKindLabels[it].orEmpty() }
            )
            AtelierTextField(
                label = stringResource(R.string.tasks_seg2_evidence_note_label),
                value = state.evidenceNote,
                onValueChange = onNoteChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = stringResource(R.string.tasks_seg2_evidence_note_hint),
                enabled = state.busyAction != TaskAction.ATTACH_EVIDENCE,
                isError = state.invalidField == "description" || overLimit,
                errorText = if (state.invalidField == "description" || overLimit) {
                    stringResource(R.string.tasks_seg2_evidence_field_error)
                } else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )
            Text(
                text = stringResource(
                    R.string.tasks_seg2_evidence_note_counter,
                    state.evidenceNote.length,
                    maxLen
                ),
                style = MaterialTheme.typography.labelSmall,
                color = AtelierTheme.colors.inkSecondary
            )
        }
        if (photoItems.isEmpty() && !state.canUploadPhotos) {
            AtelierEmptyState(text = stringResource(R.string.tasks_seg2_evidence_empty))
        } else {
            AtelierPhotoGrid(
                items = photoItems,
                columns = 2,
                addLabel = if (state.canUploadPhotos) stringResource(R.string.tasks_seg2_evidence_take_photo) else null,
                onAddPhoto = if (state.canUploadPhotos && !overLimit) onTakePhoto else null,
                onPhotoClick = { item ->
                    if (item.syncState == PhotoSyncState.FAILED) {
                        pendingById[item.id]?.let(onSelectFailedEvidence)
                    }
                }
            )
        }
    }
}
