package com.andeva.atelier.mobile.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.PanelStatusBarEffect

/**
 * Pinta [color] detrás del componente, lo sangra hacia arriba por debajo de la barra de estado y reserva
 * ese alto dentro del panel, así el contenido nunca queda bajo el reloj. Si la pantalla ya aplicó el inset
 * superior, no se duplica.
 */
@Composable
fun Modifier.panelBleed(color: Color): Modifier {
    val top = WindowInsets.statusBars.getTop(LocalDensity.current).toFloat()
    return drawBehind {
        drawRect(color, topLeft = Offset(0f, -top), size = Size(size.width, size.height + top))
    }.windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
}

/**
 * Panel de cabecera en tamaño Barra (DESIGN.md → Panel de cabecera): Azul Señal, Atrás y título.
 * Para formularios y listas largas. [actions] recibe íconos en tinta blanca.
 */
@Composable
fun AtelierTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val colors = AtelierTheme.colors
    PanelStatusBarEffect()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .panelBleed(colors.panel)
            .height(64.dp)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.onPanel) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.common_back))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() }
            )
            actions()
        }
    }
}
