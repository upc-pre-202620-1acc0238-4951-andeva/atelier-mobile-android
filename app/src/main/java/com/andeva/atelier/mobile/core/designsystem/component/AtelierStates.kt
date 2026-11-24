package com.andeva.atelier.mobile.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme

/**
 * Hay caché en pantalla y la actualización corre detrás: barra fina bajo el panel. Reserva su alto siempre,
 * así no empuja el contenido al aparecer.
 */
@Composable
fun AtelierRefreshBar(visible: Boolean, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().height(2.dp)) {
        AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent
            )
        }
    }
}

/** Vacío: una línea que dice qué habría aquí y, si aplica, la acción para crearlo. Sin ilustraciones. */
@Composable
fun AtelierEmptyState(
    text: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = AtelierTheme.colors.inkSecondary)
        if (actionLabel != null && onAction != null) {
            AtelierButton(text = actionLabel, onClick = onAction, style = AtelierButtonStyle.Outlined)
        }
    }
}

/** Error: el `message` del backend tal cual y "Reintentar". TalkBack lo anuncia al aparecer. */
@Composable
fun AtelierErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StateLine(icon = Icons.Outlined.ErrorOutline, text = message, tint = AtelierTheme.colors.bad)
        if (onRetry != null) {
            AtelierButton(
                text = stringResource(R.string.common_retry),
                onClick = onRetry,
                style = AtelierButtonStyle.Outlined
            )
        }
    }
}

/** Sin red y sin caché. Con caché no se usa: se pinta Room y se avisa de cuándo es el dato. */
@Composable
fun AtelierOfflineState(modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StateLine(
            icon = Icons.Outlined.CloudOff,
            text = stringResource(R.string.state_offline_no_cache),
            tint = AtelierTheme.colors.idle
        )
        if (onRetry != null) {
            AtelierButton(
                text = stringResource(R.string.common_retry),
                onClick = onRetry,
                style = AtelierButtonStyle.Outlined
            )
        }
    }
}

/** Sin permiso: el destino no debería mostrarse; si se llega por un enlace, una línea que lo explica. */
@Composable
fun AtelierNoPermissionState(text: String, modifier: Modifier = Modifier) {
    StateLine(
        icon = Icons.Outlined.Lock,
        text = text,
        tint = AtelierTheme.colors.idle,
        modifier = modifier.padding(vertical = 16.dp)
    )
}

/** Bloqueado por plan: se dice qué plan lo incluye, sin tono de venta. */
@Composable
fun AtelierPlanLockedState(text: String, modifier: Modifier = Modifier) {
    StateLine(
        icon = Icons.Outlined.WorkspacePremium,
        text = text,
        tint = AtelierTheme.colors.idle,
        modifier = modifier.padding(vertical = 16.dp)
    )
}

/** Motivo visible junto a una acción deshabilitada por falta de red ("Requiere conexión"). */
@Composable
fun AtelierRequiresConnection(modifier: Modifier = Modifier) {
    StateLine(
        icon = Icons.Outlined.CloudOff,
        text = stringResource(R.string.error_requires_connection),
        tint = AtelierTheme.colors.idle,
        modifier = modifier
    )
}

@Composable
private fun StateLine(icon: ImageVector, text: String, tint: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.padding(top = 2.dp).size(20.dp), tint = tint)
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
    }
}
