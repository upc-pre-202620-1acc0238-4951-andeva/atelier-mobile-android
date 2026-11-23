package com.andeva.atelier.mobile.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.PanelStatusBarEffect

/**
 * Barra azul de un destino de la barra inferior: título, y a la derecha lo pendiente de sincronizar
 * (solo si hay) y las notificaciones. Sin menú: "Más" es un destino.
 */
@Composable
fun AtelierMainTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onOpenNotifications: (() -> Unit)? = null,
    unreadNotificationsCount: Int = 0,
    onOpenSync: (() -> Unit)? = null,
    pendingSyncCount: Int = 0,
    onOpenProfile: (() -> Unit)? = null
) {
    val colors = AtelierTheme.colors
    PanelStatusBarEffect()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .panelBleed(colors.panel)
            .height(64.dp)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = colors.onPanel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onPanel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        PanelActions(onOpenNotifications, unreadNotificationsCount, onOpenSync, pendingSyncCount, onOpenProfile)
    }
}

/** Acciones globales sobre un panel azul. Las comparten la Barra y el Parte. */
@Composable
fun PanelActions(
    onOpenNotifications: (() -> Unit)?,
    unreadNotificationsCount: Int,
    onOpenSync: (() -> Unit)?,
    pendingSyncCount: Int,
    onOpenProfile: (() -> Unit)? = null
) {
    if (pendingSyncCount > 0 && onOpenSync != null) {
        PanelIconButton(
            icon = Icons.Outlined.CloudOff,
            contentDescription = pluralStringResource(R.plurals.profile_pending_count, pendingSyncCount, pendingSyncCount),
            badgeCount = pendingSyncCount,
            onClick = onOpenSync
        )
    }
    if (onOpenNotifications != null) {
        PanelIconButton(
            icon = Icons.Outlined.Notifications,
            contentDescription = stringResource(R.string.notifications_title),
            badgeCount = unreadNotificationsCount,
            onClick = onOpenNotifications
        )
    }
    // Acceso rápido a la cuenta, junto a las notificaciones.
    if (onOpenProfile != null) {
        PanelIconButton(
            icon = Icons.Outlined.AccountCircle,
            contentDescription = stringResource(R.string.more_h_profile),
            badgeCount = 0,
            onClick = onOpenProfile
        )
    }
}

@Composable
private fun PanelIconButton(
    icon: ImageVector,
    contentDescription: String,
    badgeCount: Int,
    onClick: () -> Unit
) {
    val colors = AtelierTheme.colors
    IconButton(onClick = onClick) {
        BadgedBox(
            badge = {
                // Blanco con tinta: sobre azul, un distintivo rojo no pasa contraste.
                if (badgeCount > 0) {
                    Badge(containerColor = colors.plate, contentColor = colors.onPlate) {
                        Text(badgeCount.coerceAtMost(99).toString())
                    }
                }
            }
        ) {
            Icon(imageVector = icon, contentDescription = contentDescription, tint = colors.onPanel)
        }
    }
}
