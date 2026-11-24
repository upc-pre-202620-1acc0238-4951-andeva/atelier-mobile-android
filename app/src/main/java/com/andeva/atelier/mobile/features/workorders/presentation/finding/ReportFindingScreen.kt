package com.andeva.atelier.mobile.features.workorders.presentation.finding

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNoPermissionState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSectionHeader
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.features.workorders.domain.PendingProposal
import com.andeva.atelier.mobile.features.workorders.domain.ProposalSeverity
import com.andeva.atelier.mobile.features.workorders.domain.ServiceOption
import com.andeva.atelier.mobile.features.workorders.presentation.components.ChoiceChip
import com.andeva.atelier.mobile.features.workorders.presentation.components.SubmittableSeverities
import com.andeva.atelier.mobile.features.workorders.presentation.components.chipKind
import com.andeva.atelier.mobile.features.workorders.presentation.components.labelRes
import com.andeva.atelier.mobile.features.workorders.presentation.components.rememberPhotoCapture
import com.andeva.atelier.mobile.features.workorders.presentation.detail.components.PickRow
import java.time.Instant

/**
 * US14: reporte técnico de un imprevisto encontrado en el foso o la bahía. Funciona sin red: el hallazgo se encola
 * y sale como "pendiente de sincronizar" hasta que el backend lo recibe.
 */
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

@OptIn(ExperimentalLayoutApi::class)
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
    Scaffold(
        modifier = modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { AtelierTopBar(title = stringResource(R.string.finding_title), onBack = onBack) },
        bottomBar = {
            if (state.canCreate) {
                AtelierButton(
                    text = stringResource(R.string.finding_submit),
                    onClick = onSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    enabled = state.canSubmit,
                    loading = state.isSaving
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!state.canCreate) {
                AtelierNoPermissionState(text = stringResource(R.string.reception_no_permission))
                return@Column
            }
            Text(
                text = stringResource(R.string.finding_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = AtelierTheme.colors.inkSecondary
            )
            if (state.saved) {
                AtelierBanner(
                    message = stringResource(R.string.finding_saved),
                    icon = Icons.Outlined.CheckCircle,
                    kind = BannerKind.Ok
                )
            }

            AtelierSectionHeader(title = stringResource(R.string.finding_severity))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SubmittableSeverities.forEach { severity ->
                    ChoiceChip(
                        label = stringResource(severity.labelRes()),
                        selected = state.severity == severity,
                        enabled = !state.isSaving,
                        onClick = { onSeverityChange(severity) }
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

            AtelierSectionHeader(title = stringResource(R.string.finding_photo))
            PhotoArea(state, onTakePhoto)

            Services(state, onServiceChange, onRetryServices)

            state.error?.let { AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline) }

            PendingList(state.pending)
        }
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
            color = if (state.invalidField == "photo") AtelierTheme.colors.bad else AtelierTheme.colors.inkSecondary
        )
    } else {
        AsyncImage(
            model = photo,
            contentDescription = stringResource(R.string.finding_photo),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(MaterialTheme.shapes.small),
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
            text = stringResource(R.string.common_retry),
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
            style = AtelierButtonStyle.Text
        )
        return
    }
    AtelierSectionHeader(title = stringResource(R.string.finding_service))
    Column {
        PickRow(
            title = stringResource(R.string.finding_service_none),
            selected = state.suggestedServiceId == null,
            enabled = !state.isSaving,
            onClick = { onServiceChange(null) }
        )
        state.services.forEach { service ->
            PickRow(
                title = service.name,
                selected = state.suggestedServiceId == service.id,
                enabled = !state.isSaving,
                onClick = { onServiceChange(service.id) }
            )
        }
    }
}

/** Hallazgos guardados que aún esperan señal para enviarse con su foto. */
@Composable
private fun PendingList(pending: List<PendingProposal>) {
    if (pending.isEmpty()) return
    AtelierSectionHeader(title = stringResource(R.string.finding_pending_title), count = pending.size)
    Column {
        pending.forEach { proposal ->
            val failed = proposal.status == "FAILED"
            AtelierListRow(
                title = proposal.description,
                subtitle = proposal.severity?.let { stringResource(it.labelRes()) },
                trailing = {
                    AtelierChip(
                        text = stringResource(if (failed) R.string.gallery_status_failed else R.string.gallery_status_queued),
                        kind = if (failed) ChipKind.Bad else ChipKind.Wait
                    )
                }
            )
        }
    }
}

// region Previews

private val previewState = ReportFindingUiState(
    description = "Fuga de aceite en el cárter", severity = ProposalSeverity.CRITICAL,
    services = listOf(
        ServiceOption("s1", "Cambio de empaque del cárter", null, 90),
        ServiceOption("s2", "Limpieza de motor", null, 45)
    ),
    suggestedServiceId = "s1", isLoadingServices = false, canCreate = true,
    pending = listOf(
        PendingProposal("p1", null, "", "Disco de freno delantero desgastado", ProposalSeverity.CRITICAL, "PENDING", null, Instant.EPOCH)
    ),
    saved = true
)

@Composable
private fun PreviewContent(state: ReportFindingUiState) {
    ReportFindingContent(
        state = state,
        onBack = {}, onDescriptionChange = {}, onSeverityChange = {}, onServiceChange = {},
        onTakePhoto = {}, onRetryServices = {}, onSubmit = {}
    )
}

@Preview(name = "Hallazgo · claro", widthDp = 390, heightDp = 844)
@Composable
private fun ReportFindingPreviewLight() = AtelierWorkshopTheme(darkTheme = false) { PreviewContent(previewState) }

@Preview(name = "Hallazgo · oscuro", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ReportFindingPreviewDark() = AtelierWorkshopTheme(darkTheme = true) { PreviewContent(previewState) }

@Preview(name = "Hallazgo · sin permiso", widthDp = 390, heightDp = 844)
@Composable
private fun ReportFindingPreviewNoPermission() = AtelierWorkshopTheme(darkTheme = false) {
    PreviewContent(ReportFindingUiState(isLoadingServices = false))
}

// endregion
