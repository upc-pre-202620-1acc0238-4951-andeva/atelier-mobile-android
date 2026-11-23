package com.andeva.atelier.mobile.features.workorders.presentation.intake

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNoPermissionState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSectionHeader
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.component.bone
import com.andeva.atelier.mobile.core.designsystem.component.rememberDelayedLoading
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.features.workorders.domain.IntakeImage
import com.andeva.atelier.mobile.features.workorders.presentation.components.rememberPhotoCapture

/**
 * US13: galería pericial de la recepción. Las fotos se encolan con o sin red: las que esperan señal llevan su marca
 * "pendiente de sincronizar". [onDone] cierra la galería cuando el asesor terminó de fotografiar.
 */
@Composable
fun IntakeGalleryScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    onOpenProposals: (workOrderId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: IntakeGalleryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // El panel que se está fotografiando sobrevive a que Android recree la pantalla mientras abre la cámara.
    var capturingAngle by rememberSaveable { mutableStateOf(InspectionAngle.FRONT) }
    val openCamera = rememberPhotoCapture(
        onCaptured = { file -> viewModel.onPhotoCaptured(capturingAngle, file) },
        onDenied = viewModel::onCameraDenied
    )

    IntakeGalleryContent(
        state = state,
        onBack = onBack,
        onDone = onDone,
        onOpenProposals = { onOpenProposals(viewModel.workOrderId) },
        onNoteChange = viewModel::onNoteChange,
        onCapture = { angle ->
            capturingAngle = angle
            openCamera()
        },
        modifier = modifier
    )
}

@Composable
internal fun IntakeGalleryContent(
    state: IntakeGalleryUiState,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onOpenProposals: () -> Unit,
    onNoteChange: (String) -> Unit,
    onCapture: (InspectionAngle) -> Unit,
    modifier: Modifier = Modifier
) {
    val panels = InspectionAngle.entries.filter { it != InspectionAngle.OTHER }
    // Sin nada que pintar: los paneles salen como huesos (la espera de 150 ms evita el parpadeo).
    val loading = rememberDelayedLoading(state.isLoading && state.photos.isEmpty() && state.canRead)
    val noAccess = !state.canUpload && !state.canRead && !state.isLoading
    Scaffold(
        modifier = modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { AtelierTopBar(title = stringResource(R.string.gallery_title), onBack = onBack) },
        bottomBar = {
            if (!noAccess) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AtelierButton(
                        text = stringResource(R.string.gallery_done),
                        onClick = onDone,
                        modifier = Modifier.fillMaxWidth()
                    )
                    AtelierButton(
                        text = stringResource(R.string.gallery_proposals),
                        onClick = onOpenProposals,
                        modifier = Modifier.fillMaxWidth(),
                        style = AtelierButtonStyle.Text
                    )
                }
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
            if (noAccess) {
                AtelierNoPermissionState(text = stringResource(R.string.reception_no_permission))
                return@Column
            }
            Text(
                text = stringResource(R.string.gallery_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = AtelierTheme.colors.inkSecondary
            )
            if (!state.canUpload) {
                AtelierNoPermissionState(text = stringResource(R.string.reception_no_permission))
            }
            AtelierTextField(
                label = stringResource(R.string.gallery_note),
                value = state.note,
                onValueChange = onNoteChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving && state.canUpload
            )
            state.error?.let { AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline) }

            panels.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    pair.forEach { angle ->
                        PhotoSlot(
                            label = stringResource(angle.labelRes()),
                            photo = state.photosFor(angle).firstOrNull(),
                            enabled = state.canUpload && !state.isSaving,
                            loading = loading,
                            onClick = { onCapture(angle) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            val others = state.photosFor(InspectionAngle.OTHER)
            AtelierSectionHeader(title = stringResource(R.string.gallery_other), count = others.size.takeIf { it > 0 })
            others.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    pair.forEach { photo ->
                        PhotoSlot(
                            label = photo.description ?: stringResource(R.string.gallery_angle_other),
                            photo = photo,
                            enabled = false,
                            loading = false,
                            onClick = {},
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) Box(modifier = Modifier.weight(1f)) {}
                }
            }
            AtelierButton(
                text = stringResource(R.string.gallery_add_other),
                onClick = { onCapture(InspectionAngle.OTHER) },
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Outlined,
                enabled = state.canUpload && !state.isSaving,
                loading = state.isSaving
            )
        }
    }
}

/** Panel de la cuadrícula: la foto si ya existe (con su estado) o el ícono de cámara para tomarla. */
@Composable
private fun PhotoSlot(
    label: String,
    photo: GalleryPhoto?,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val takeLabel = stringResource(R.string.gallery_take_photo, label)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(MaterialTheme.shapes.small)
                .background(AtelierTheme.colors.card)
                .bone(loading)
                .clickable(enabled = enabled && !loading, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (loading) {
                // Hueso: nada que dibujar encima.
            } else if (photo == null) {
                Icon(
                    imageVector = Icons.Outlined.PhotoCamera,
                    contentDescription = takeLabel,
                    tint = if (enabled) MaterialTheme.colorScheme.primary else AtelierTheme.colors.muted
                )
            } else {
                AsyncImage(
                    model = photo.model,
                    contentDescription = label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                AtelierChip(
                    text = stringResource(photo.status.labelRes()),
                    kind = photo.status.chipKind(),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

private fun InspectionAngle.labelRes(): Int = when (this) {
    InspectionAngle.FRONT -> R.string.gallery_angle_front
    InspectionAngle.REAR -> R.string.gallery_angle_rear
    InspectionAngle.LEFT -> R.string.gallery_angle_left
    InspectionAngle.RIGHT -> R.string.gallery_angle_right
    InspectionAngle.ROOF -> R.string.gallery_angle_roof
    InspectionAngle.DASHBOARD -> R.string.gallery_angle_dashboard
    InspectionAngle.OTHER -> R.string.gallery_angle_other
}

private fun PhotoStatus.labelRes(): Int = when (this) {
    PhotoStatus.UPLOADED -> R.string.gallery_status_uploaded
    PhotoStatus.QUEUED -> R.string.gallery_status_queued
    PhotoStatus.FAILED -> R.string.gallery_status_failed
}

private fun PhotoStatus.chipKind(): ChipKind = when (this) {
    PhotoStatus.UPLOADED -> ChipKind.Ok
    PhotoStatus.QUEUED -> ChipKind.Wait
    PhotoStatus.FAILED -> ChipKind.Bad
}

// region Previews

private val previewPhotos = listOf(
    IntakeImage("i1", "https://example.invalid/frente.jpg", "Frente", null),
    IntakeImage("i2", "https://example.invalid/techo.jpg", "Techo: rayón en el capó", null)
)

@Preview(name = "Galería · claro", widthDp = 390, heightDp = 844)
@Composable
private fun IntakeGalleryPreviewLight() = AtelierWorkshopTheme(darkTheme = false) {
    IntakeGalleryContent(
        state = IntakeGalleryUiState(isLoading = false, canUpload = true, canRead = true, uploaded = previewPhotos),
        onBack = {}, onDone = {}, onOpenProposals = {}, onNoteChange = {}, onCapture = {}
    )
}

@Preview(name = "Galería · oscuro", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun IntakeGalleryPreviewDark() = AtelierWorkshopTheme(darkTheme = true) {
    IntakeGalleryContent(
        state = IntakeGalleryUiState(
            isLoading = false, canUpload = true, canRead = true,
            error = UiText.Dynamic("La foto no se pudo guardar.")
        ),
        onBack = {}, onDone = {}, onOpenProposals = {}, onNoteChange = {}, onCapture = {}
    )
}

@Preview(name = "Galería · sin permiso", widthDp = 390, heightDp = 844)
@Composable
private fun IntakeGalleryPreviewNoPermission() = AtelierWorkshopTheme(darkTheme = false) {
    IntakeGalleryContent(
        state = IntakeGalleryUiState(isLoading = false),
        onBack = {}, onDone = {}, onOpenProposals = {}, onNoteChange = {}, onCapture = {}
    )
}

// endregion
