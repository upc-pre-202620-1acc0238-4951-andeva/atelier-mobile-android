package com.andeva.atelier.mobile.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Sync
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme

/**
 * Barra superior persistente para pantallas principales / pestañas raíz.
 *
 * Mantiene la persistencia estilo Lemon Cash de las acciones globales:
 * - Izquierda: Menú de tres barras (☰) para Gestión del Taller.
 * - Centro: Título de la sección y subtítulo opcional.
 * - Derecha: Sincronización offline (cuando hay pendientes) y bandeja de notificaciones.
 */
@Composable
fun AtelierMainTopBar(
    title: String,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onOpenNotifications: (() -> Unit)? = null,
    unreadNotificationsCount: Int = 0,
    onOpenSync: (() -> Unit)? = null,
    pendingSyncCount: Int = 0
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(start = 4.dp, end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onOpenMenu) {
            Icon(
                imageVector = Icons.Outlined.Menu,
                contentDescription = stringResource(R.string.more_hub_title),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AtelierTheme.colors.inkSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (pendingSyncCount > 0 && onOpenSync != null) {
            HeaderActionIconButton(
                icon = Icons.Outlined.Sync,
                contentDescription = stringResource(R.string.sync_queue_title),
                badgeCount = pendingSyncCount,
                onClick = onOpenSync
            )
        }

        if (onOpenNotifications != null) {
            HeaderActionIconButton(
                icon = Icons.Outlined.Notifications,
                contentDescription = stringResource(R.string.notifications_title),
                badgeCount = unreadNotificationsCount,
                onClick = onOpenNotifications
            )
        }
    }
}

@Composable
private fun HeaderActionIconButton(
    icon: ImageVector,
    contentDescription: String,
    badgeCount: Int,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    IconButton(onClick = onClick) {
        BadgedBox(
            badge = {
                if (badgeCount > 0) {
                    Badge(
                        containerColor = scheme.error,
                        contentColor = scheme.onError
                    ) {
                        Text(badgeCount.coerceAtMost(99).toString())
                    }
                }
            }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = scheme.onBackground
            )
        }
    }
}
