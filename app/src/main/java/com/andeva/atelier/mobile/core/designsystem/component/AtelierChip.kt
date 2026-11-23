package com.andeva.atelier.mobile.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.ChipShape

/**
 * Vocabulario fijo (DESIGN.md → Chips y marcas de estado): [Info] en curso · [Ok] listo, aprobada,
 * sincronizado · [Wait] espera decisión · [Bad] rechazada, falló · [Warn] tardanza, stock mínimo,
 * suspendida · [Idle] pendiente de sincronizar, sin permiso.
 */
enum class ChipKind { Ok, Info, Wait, Bad, Idle, Warn }

/**
 * Marca de estado: ícono y texto sobre el fondo tenue del estado. Nunca solo color: si no se pasa
 * [icon], lleva el de su [kind]. Va siempre al extremo derecho de la fila.
 */
@Composable
fun AtelierChip(
    text: String,
    modifier: Modifier = Modifier,
    kind: ChipKind = ChipKind.Idle,
    icon: ImageVector? = null
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
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(mark, contentDescription = null, modifier = Modifier.size(16.dp), tint = content)
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = content,
            maxLines = 1,
            softWrap = false
        )
    }
}
