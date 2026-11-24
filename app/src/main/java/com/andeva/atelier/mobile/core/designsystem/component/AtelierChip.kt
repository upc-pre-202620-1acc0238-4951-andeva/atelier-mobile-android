package com.andeva.atelier.mobile.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierBlue
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.ChipShape

/**
 * Vocabulario fijo (DESIGN.md → Chips y marcas de estado): [Info] en curso · [Ok] listo, aprobada,
 * sincronizado · [Wait] espera decisión · [Bad] rechazada, falló · [Warn] tardanza, stock mínimo,
 * suspendida · [Idle] pendiente de sincronizar, sin permiso.
 */
enum class ChipKind { Ok, Info, Wait, Bad, Idle, Warn }

/**
 * Marca de estado calibrada: fondo semántico translúcido, borde fino del mismo tono al 28 % y señal
 * acompañada de ícono o [AtelierLiveBeacon] cuando [live] está activo. Nunca solo color.
 */
@Composable
fun AtelierChip(
    text: String,
    modifier: Modifier = Modifier,
    kind: ChipKind = ChipKind.Idle,
    icon: ImageVector? = null,
    live: Boolean = false
) {
    val colors = AtelierTheme.colors
    val (container, content) = when (kind) {
        ChipKind.Ok -> colors.okContainer to colors.ok
        ChipKind.Info -> colors.infoContainer to colors.info
        ChipKind.Wait -> colors.waitContainer to colors.wait
        ChipKind.Bad -> colors.badContainer to colors.bad
        ChipKind.Idle -> colors.idleContainer to colors.idle
        ChipKind.Warn -> colors.warnContainer to colors.warn
    }
    val beaconColor = when (kind) {
        ChipKind.Ok -> colors.okMark
        ChipKind.Info -> AtelierBlue
        ChipKind.Wait -> colors.decision
        ChipKind.Bad -> colors.badMark
        ChipKind.Warn -> colors.warnMark
        ChipKind.Idle -> colors.idle
    }
    val mark = icon ?: when (kind) {
        ChipKind.Ok -> Icons.Outlined.CheckCircle
        ChipKind.Info -> Icons.Outlined.PlayCircle
        ChipKind.Wait -> Icons.Outlined.HourglassEmpty
        ChipKind.Bad -> Icons.Outlined.ErrorOutline
        ChipKind.Idle -> Icons.Outlined.MoreHoriz
        ChipKind.Warn -> Icons.Outlined.WarningAmber
    }
    Row(
        modifier = modifier
            .heightIn(min = 28.dp)
            .clip(ChipShape)
            .background(container)
            .border(width = 0.75.dp, color = content.copy(alpha = 0.28f), shape = ChipShape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (live) {
            AtelierLiveBeacon(color = beaconColor, size = 7.dp, pulsing = true)
        } else {
            Icon(mark, contentDescription = null, modifier = Modifier.size(16.dp), tint = content)
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = content,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun ChipPreviewBody() {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AtelierChip("En curso", kind = ChipKind.Info, live = true)
            AtelierChip("Listo", kind = ChipKind.Ok)
            AtelierChip("Espera", kind = ChipKind.Wait)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AtelierChip("Suspendida", kind = ChipKind.Warn)
            AtelierChip("Rechazada", kind = ChipKind.Bad)
            AtelierChip("Pendiente", kind = ChipKind.Idle)
        }
    }
}

@Preview(name = "Chips · claro", showBackground = true, backgroundColor = 0xFFF8F8FA)
@Composable
private fun ChipPreviewLight() = AtelierWorkshopTheme(darkTheme = false) { ChipPreviewBody() }

@Preview(
    name = "Chips · oscuro",
    showBackground = true,
    backgroundColor = 0xFF262626,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ChipPreviewDark() = AtelierWorkshopTheme(darkTheme = true) { ChipPreviewBody() }
