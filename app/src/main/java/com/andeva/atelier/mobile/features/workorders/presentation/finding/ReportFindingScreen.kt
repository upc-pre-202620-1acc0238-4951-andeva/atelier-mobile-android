package com.andeva.atelier.mobile.features.workorders.presentation.finding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardShape
import com.andeva.atelier.mobile.features.workorders.domain.PendingProposal
import com.andeva.atelier.mobile.features.workorders.domain.ProposalSeverity
import com.andeva.atelier.mobile.features.workorders.domain.ServiceOption
import com.andeva.atelier.mobile.features.workorders.presentation.components.SubmittableSeverities
import com.andeva.atelier.mobile.features.workorders.presentation.components.chipKind
import com.andeva.atelier.mobile.features.workorders.presentation.components.labelRes
import com.andeva.atelier.mobile.features.workorders.presentation.components.rememberPhotoCapture
import java.time.Instant

/** US14: reporte técnico de un imprevisto encontrado en el foso o la bahía. */
@Composable
fun ReportFindingScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReportFindingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val openCamera = rememberPhotoCapture(onCaptured = viewModel::onPhotoCaptured, onDenied = viewModel::onCameraDenied)

    ReportFindingContent(
        state = state,
        onBack = onBack,
        onDescriptionChange = viewModel::onDescriptionChange,
        onSeverityChange = viewModel::onSeverityChange,
        onServiceChange = viewModel::onServiceChange,
        onTakePhoto = openCamera,
        onRetryServices = viewModel::loadServices,
        onSubmit = viewModel::onSubmit,
        modifier = modifier
    )
}

@Composable
internal fun ReportFindingContent(
    state: ReportFindingUiState,
    onBack: () -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSeverityChange: (ProposalSeverity) -> Unit,
    onServiceChange: (String?) -> Unit,
    onTakePhoto: () -> Unit,
    onRetryServices: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        AtelierTopBar(title = stringResource(R.string.finding_title), onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.finding_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = AtelierTheme.colors.inkSecondary
            )
            if (!state.canCreate) {
                AtelierBanner(
                    message = stringResource(R.string.reception_no_permission),
                    icon = Icons.Outlined.Info,
                    kind = BannerKind.Wait
                )
            }
            if (state.saved) {
                AtelierBanner(
                    message = stringResource(R.string.finding_saved),
                    icon = Icons.Outlined.CheckCircle,
                    kind = BannerKind.Ok
                )
            }

            SectionLabel(R.string.finding_severity)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SubmittableSeverities.forEach { severity ->
                    AtelierButton(
                        text = stringResource(severity.labelRes()),
                        onClick = { onSeverityChange(severity) },
                        modifier = Modifier.weight(1f),
                        style = if (state.severity == severity) AtelierButtonStyle.Tonal else AtelierButtonStyle.Outlined,
                        enabled = !state.isSaving
                    )
                }
            }

            AtelierTextField(
                label = stringResource(R.string.finding_description),
                value = state.description,
                onValueChange = onDescriptionChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving,
                isError = state.invalidField == "description",
                errorText = if (state.invalidField == "description") stringResource(R.string.finding_field_description) else null
            )

            SectionLabel(R.string.finding_photo)
            PhotoArea(state, onTakePhoto)

            Services(state, onServiceChange, onRetryServices)

            state.error?.let { AtelierBanner(message = it.asString(), icon = Icons.Filled.Error) }

            PendingList(state.pending)
        }

        AtelierButton(
            text = stringResource(R.string.finding_submit),
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            enabled = state.canSubmit,
            loading = state.isSaving
        )
    }
}

/** La foto es obligatoria (US14 E2): sin ella el botón de enviar queda deshabilitado. */
@Composable
private fun PhotoArea(state: ReportFindingUiState, onTakePhoto: () -> Unit) {
    val photo = state.photo
    if (photo == null) {
        AtelierButton(
            text = stringResource(R.string.finding_take_photo),
            onClick = onTakePhoto,
            modifier = Modifier.fillMaxWidth(),
            style = AtelierButtonStyle.Outlined,
            enabled = state.canCreate && !state.isSaving,
            icon = Icons.Outlined.PhotoCamera
        )
        Text(
            text = stringResource(R.string.finding_photo_required),
            style = MaterialTheme.typography.bodyMedium,
            color = AtelierTheme.colors.muted
        )
    } else {
        AsyncImage(
            model = photo,
            contentDescription = stringResource(R.string.finding_photo),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(CardShape),
            contentScale = ContentScale.Crop
        )
        AtelierButton(
            text = stringResource(R.string.finding_change_photo),
            onClick = onTakePhoto,
            modifier = Modifier.fillMaxWidth(),
            style = AtelierButtonStyle.Text,
            enabled = !state.isSaving
        )
    }
}

@Composable
private fun Services(state: ReportFindingUiState, onServiceChange: (String?) -> Unit, onRetry: () -> Unit) {
    if (state.isLoadingServices) return
    if (state.services.isEmpty()) {
        AtelierButton(
            text = stringResource(R.string.branch_retry),
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
            style = AtelierButtonStyle.Text
        )
        return
    }
    SectionLabel(R.string.finding_service)
    AtelierButton(
        text = stringResource(R.string.finding_service_none),
        onClick = { onServiceChange(null) },
        modifier = Modifier.fillMaxWidth(),
        style = if (state.suggestedServiceId == null) AtelierButtonStyle.Tonal else AtelierButtonStyle.Outlined,
        enabled = !state.isSaving
    )
    state.services.forEach { service ->
        AtelierButton(
            text = service.name,
            onClick = { onServiceChange(service.id) },
            modifier = Modifier.fillMaxWidth(),
            style = if (state.suggestedServiceId == service.id) AtelierButtonStyle.Tonal else AtelierButtonStyle.Outlined,
            enabled = !state.isSaving
        )
    }
}

/** Hallazgos guardados que aún esperan señal para enviarse con su foto. */
@Composable
private fun PendingList(pending: List<PendingProposal>) {
    if (pending.isEmpty()) return
    SectionLabel(R.string.finding_pending_title)
    pending.forEach { proposal ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(AtelierTheme.colors.card)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                proposal.severity?.let { AtelierChip(text = stringResource(it.labelRes()), kind = it.chipKind()) }
                AtelierChip(
                    text = stringResource(if (proposal.status == "FAILED") R.string.gallery_status_failed else R.string.gallery_status_queued),
                    kind = if (proposal.status == "FAILED") ChipKind.Bad else ChipKind.Wait
                )
            }
            Text(
                text = proposal.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SectionLabel(textRes: Int) = Text(
    text = stringResource(textRes),
    style = MaterialTheme.typography.titleMedium,
    color = MaterialTheme.colorScheme.onBackground
)

@Preview(name = "Hallazgo · formulario · oscuro", widthDp = 360, heightDp = 1100, showBackground = true)
@Composable
private fun ReportFindingPreviewForm() = AtelierWorkshopTheme(darkTheme = true) {
    ReportFindingContent(
        state = ReportFindingUiState(
            description = "Fuga de aceite en el cárter", severity = ProposalSeverity.CRITICAL,
            services = listOf(ServiceOption("s1", "Cambio de empaque", null, 90)),
            isLoadingServices = false, canCreate = true
        ),
        onBack = {}, onDescriptionChange = {}, onSeverityChange = {}, onServiceChange = {},
        onTakePhoto = {}, onRetryServices = {}, onSubmit = {}
    )
}

@Preview(name = "Hallazgo · guardado · claro", widthDp = 360, heightDp = 1100, showBackground = true)
@Composable
private fun ReportFindingPreviewSaved() = AtelierWorkshopTheme(darkTheme = false) {
    ReportFindingContent(
        state = ReportFindingUiState(
            saved = true, isLoadingServices = false, canCreate = true,
            pending = listOf(
                PendingProposal("p1", null, "", "Fuga de aceite en el cárter", ProposalSeverity.CRITICAL, "PENDING", null, Instant.EPOCH)
            )
        ),
        onBack = {}, onDescriptionChange = {}, onSeverityChange = {}, onServiceChange = {},
        onTakePhoto = {}, onRetryServices = {}, onSubmit = {}
    )
}
