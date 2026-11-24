package com.andeva.atelier.mobile.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierBlueSoft
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.ControlShape
import java.io.File

/** Estado de sincronización de una evidencia fotográfica. */
enum class PhotoSyncState { SYNCED, PENDING, FAILED }

/** Elemento de [AtelierPhotoGrid]: soporta URL remota o archivo local en cola offline. */
@Immutable
data class AtelierPhotoItem(
    val id: String,
    val model: Any,
    val caption: String? = null,
    val syncState: PhotoSyncState = PhotoSyncState.SYNCED,
    val capturedAtLabel: String? = null
)

/**
 * Grilla de evidencias fotográficas (recepción o desmontaje/montaje de tarea).
 * Muestra el estado de sincronización con ícono + texto y reserva el primer o último espacio para capturar foto.
 */
@Composable
fun AtelierPhotoGrid(
    items: List<AtelierPhotoItem>,
    modifier: Modifier = Modifier,
    columns: Int = 2,
    addLabel: String? = null,
    onAddPhoto: (() -> Unit)? = null,
    onPhotoClick: ((AtelierPhotoItem) -> Unit)? = null
) {
    val cells: List<AtelierPhotoItem?> = buildList {
        addAll(items)
        if (onAddPhoto != null && addLabel != null) add(null)
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        cells.chunked(columns.coerceAtLeast(1)).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { item ->
                    if (item == null) {
                        AddPhotoTile(
                            label = addLabel.orEmpty(),
                            onClick = { onAddPhoto?.invoke() },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        PhotoTile(
                            item = item,
                            onClick = onPhotoClick?.let { cb -> { cb(item) } },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PhotoTile(
    item: AtelierPhotoItem,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colors = AtelierTheme.colors
    val scheme = MaterialTheme.colorScheme
    val imageModel = remember(item.model) {
        val raw = item.model
        if (raw is String && !raw.startsWith("http", ignoreCase = true)) File(raw) else raw
    }

    AtelierSurface(
        modifier = modifier,
        tier = SurfaceTier.Card,
        shape = ControlShape,
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .background(colors.surfaceRecessed)
            ) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = item.caption,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                val (badgeKind, badgeIcon, badgeText) = when (item.syncState) {
                    PhotoSyncState.SYNCED -> Triple(ChipKind.Ok, Icons.Outlined.CloudDone, "Guardada")
                    PhotoSyncState.PENDING -> Triple(ChipKind.Idle, Icons.Outlined.Sync, "Pendiente")
                    PhotoSyncState.FAILED -> Triple(ChipKind.Bad, Icons.Outlined.ErrorOutline, "Falló")
                }
                if (item.syncState != PhotoSyncState.SYNCED) {
                    AtelierChip(
                        text = badgeText,
                        kind = badgeKind,
                        icon = badgeIcon,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    )
                }
            }
            if (!item.caption.isNullOrBlank() || !item.capturedAtLabel.isNullOrBlank()) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (!item.caption.isNullOrBlank()) {
                        Text(
                            text = item.caption,
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!item.capturedAtLabel.isNullOrBlank()) {
                        Text(
                            text = item.capturedAtLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.inkSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddPhotoTile(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AtelierTheme.colors
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val tileGradient = remember(scheme.primaryContainer) {
        Brush.verticalGradient(listOf(scheme.primaryContainer, scheme.primaryContainer.copy(alpha = 0.72f)))
    }
    val borderGradient = remember(scheme.primary) {
        Brush.verticalGradient(listOf(AtelierBlueSoft.copy(alpha = 0.65f), scheme.primary.copy(alpha = 0.45f)))
    }
    Column(
        modifier = modifier
            .aspectRatio(4f / 3f)
            .springPressScale(interaction)
            .clip(ControlShape)
            .background(colors.card)
            .background(tileGradient)
            .border(1.dp, borderGradient, ControlShape)
            .clickable(
                interactionSource = interaction,
                indication = ripple(),
                role = Role.Button,
                onClick = onClick
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
    ) {
        Icon(
            imageVector = Icons.Outlined.AddAPhoto,
            contentDescription = null,
            tint = scheme.onPrimaryContainer,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = scheme.onPrimaryContainer,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(name = "Evidencias · claro", widthDp = 390, showBackground = true)
@Composable
private fun PhotoGridPreviewLight() = AtelierWorkshopTheme(darkTheme = false) {
    AtelierPhotoGrid(
        items = listOf(
            AtelierPhotoItem("1", "", "Desmontaje: pastillas gastadas", PhotoSyncState.SYNCED),
            AtelierPhotoItem("2", "", "Montaje: discos nuevos", PhotoSyncState.PENDING)
        ),
        addLabel = "Tomar foto",
        onAddPhoto = {},
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    )
}

@Preview(
    name = "Evidencias · oscuro",
    widthDp = 390,
    showBackground = true,
    backgroundColor = 0xFF262626,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun PhotoGridPreviewDark() = AtelierWorkshopTheme(darkTheme = true) {
    AtelierPhotoGrid(
        items = listOf(
            AtelierPhotoItem("1", "", "Desmontaje: pastillas gastadas", PhotoSyncState.FAILED)
        ),
        addLabel = "Tomar foto",
        onAddPhoto = {},
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    )
}
