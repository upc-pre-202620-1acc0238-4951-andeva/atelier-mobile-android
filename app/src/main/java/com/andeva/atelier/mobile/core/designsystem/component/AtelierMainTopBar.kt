package com.andeva.atelier.mobile.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierBlue
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierError
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme

/**
 * Cabecera limpia de un destino de la barra inferior: título en Satoshi Black sobre el lienzo, subtítulo
 * opcional y acciones en medallones circulares elevados (sincronización pendiente, notificaciones y cuenta).
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
                .padding(start = 16.dp, end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = scheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.inkSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            PanelActions(onOpenNotifications, unreadNotificationsCount, onOpenSync, pendingSyncCount, onOpenProfile)
        }
        HorizontalDivider(thickness = 1.dp, color = colors.outlineSoft.copy(alpha = 0.50f))
    }
}

/** Acciones globales de cabecera: sincronización pendiente, notificaciones y acceso a cuenta. */
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
            badgeColor = AtelierBlue,
            onClick = onOpenSync
        )
    }
    if (onOpenNotifications != null) {
        PanelIconButton(
            icon = Icons.Outlined.Notifications,
            contentDescription = stringResource(R.string.notifications_title),
            badgeCount = unreadNotificationsCount,
            badgeColor = AtelierError,
            onClick = onOpenNotifications
        )
    }
    // Acceso rápido a la cuenta, junto a las notificaciones.
    if (onOpenProfile != null) {
        PanelIconButton(
            icon = Icons.Outlined.AccountCircle,
            contentDescription = stringResource(R.string.more_h_profile),
            badgeCount = 0,
            badgeColor = AtelierBlue,
            onClick = onOpenProfile
        )
    }
}

@Composable
private fun PanelIconButton(
    icon: ImageVector,
    contentDescription: String,
    badgeCount: Int,
    badgeColor: Color,
    onClick: () -> Unit
) {
    AtelierCircularIconButton(
        icon = icon,
        contentDescription = contentDescription,
        onClick = onClick,
        badgeCount = badgeCount,
        badgeColor = badgeColor
    )
}

@Preview(name = "Barra principal · claro", widthDp = 390, showBackground = true, backgroundColor = 0xFFF8F8FA)
@Composable
private fun MainTopBarPreviewLight() = AtelierWorkshopTheme(darkTheme = false) {
    AtelierMainTopBar(
        title = "Taller Mecánico Norte",
        subtitle = "Carlos Mendoza · Chorrillos",
        onOpenNotifications = {},
        unreadNotificationsCount = 3,
        onOpenSync = {},
        pendingSyncCount = 2,
        onOpenProfile = {}
    )
}

@Preview(
    name = "Barra principal · oscuro",
    widthDp = 390,
    showBackground = true,
    backgroundColor = 0xFF262626,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun MainTopBarPreviewDark() = AtelierWorkshopTheme(darkTheme = true) {
    AtelierMainTopBar(
        title = "Taller Mecánico Norte",
        subtitle = "Carlos Mendoza · Chorrillos",
        onOpenNotifications = {},
        unreadNotificationsCount = 3,
        onOpenSync = {},
        pendingSyncCount = 2,
        onOpenProfile = {}
    )
}
