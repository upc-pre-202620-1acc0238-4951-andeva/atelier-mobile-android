package com.andeva.atelier.mobile.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme

/**
 * Fila de lista con divisor (se prefiere a la pila de tarjetas). Orden fijo: [plate] o [leading] primero,
 * título y segunda línea al centro, la marca de estado en [trailing] al extremo derecho.
 * 64 dp con una línea, 72 dp con dos; crece en alto con la fuente del sistema en vez de truncar la placa.
 * Con fuente grande la marca de estado baja bajo el texto: a la derecha le quitaría el ancho al título.
 */
@Composable
fun AtelierListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    plate: String? = null,
    loading: Boolean = false,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    val stacked = trailing != null && LocalDensity.current.fontScale >= 1.15f
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (subtitle != null) 72.dp else 64.dp)
                .then(
                    if (onClick != null && !loading) Modifier.clickable(role = Role.Button, onClick = onClick)
                    else Modifier
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                plate != null -> AtelierPlate(plate, modifier = Modifier.bone(loading))
                leading != null -> leading()
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.bone(loading, widthFraction = 0.7f)
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AtelierTheme.colors.inkSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.bone(loading, widthFraction = 0.5f)
                    )
                }
                if (stacked && !loading) Box(modifier = Modifier.padding(top = 4.dp)) { trailing?.invoke() }
            }
            if (!stacked && !loading) trailing?.invoke()
        }
        if (showDivider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
