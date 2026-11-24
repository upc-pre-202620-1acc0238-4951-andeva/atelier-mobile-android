package com.andeva.atelier.mobile.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierError
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme

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
 * Medallón circular elevado (40 dp dentro de un objetivo táctil de 48 dp) para acciones de cabecera
 * sobre lienzo limpio.
 */
@Composable
internal fun AtelierCircularIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
    badgeColor: Color = AtelierError
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(48.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = 24.dp),
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        BadgedBox(
            badge = {
                if (badgeCount > 0) {
                    Badge(containerColor = badgeColor, contentColor = Color.White) {
                        Text(badgeCount.coerceAtMost(99).toString())
                    }
                }
            }
        ) {
            AtelierSurface(
                modifier = Modifier
                    .size(40.dp)
                    .springPressScale(interactionSource),
                tier = SurfaceTier.Card,
                shape = CircleShape
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = contentDescription,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Barra superior limpia para formularios y listas largas: lienzo del tema, botón Atrás en medallón circular
 * elevado, título en Satoshi Black y divisor inferior sutil.
 */
@Composable
fun AtelierTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val colors = AtelierTheme.colors
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(scheme.background)
            .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AtelierCircularIconButton(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.common_back),
                onClick = onBack
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() }
            )
            CompositionLocalProvider(LocalContentColor provides scheme.onBackground) {
                actions()
            }
        }
        HorizontalDivider(thickness = 1.dp, color = colors.outlineSoft.copy(alpha = 0.50f))
    }
}

@Preview(name = "Barra superior · claro", widthDp = 390, showBackground = true, backgroundColor = 0xFFF8F8FA)
@Composable
private fun TopBarPreviewLight() = AtelierWorkshopTheme(darkTheme = false) {
    AtelierTopBar(
        title = "Órdenes de trabajo",
        onBack = {},
        actions = {
            IconButton(onClick = {}) {
                Icon(Icons.Outlined.Search, contentDescription = null)
            }
        }
    )
}

@Preview(
    name = "Barra superior · oscuro",
    widthDp = 390,
    showBackground = true,
    backgroundColor = 0xFF262626,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun TopBarPreviewDark() = AtelierWorkshopTheme(darkTheme = true) {
    AtelierTopBar(
        title = "Órdenes de trabajo",
        onBack = {},
        actions = {
            IconButton(onClick = {}) {
                Icon(Icons.Outlined.Search, contentDescription = null)
            }
        }
    )
}
