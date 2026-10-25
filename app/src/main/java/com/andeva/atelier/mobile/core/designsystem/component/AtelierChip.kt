package com.andeva.atelier.mobile.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.ChipShape

enum class ChipKind { Ok, Info, Wait, Bad, Idle }

/** Etiqueta de estado de 28 dp. El color nunca va solo: siempre lleva texto. */
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
    }
    Row(
        modifier = modifier
            .height(28.dp)
            .clip(ChipShape)
            .background(container)
            .padding(start = if (icon != null) 8.dp else 10.dp, end = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = content)
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = content
        )
    }
}
