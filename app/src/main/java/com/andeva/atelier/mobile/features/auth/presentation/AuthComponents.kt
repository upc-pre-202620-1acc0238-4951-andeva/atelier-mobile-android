package com.andeva.atelier.mobile.features.auth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.core.designsystem.component.AtelierLogo
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSurface
import com.andeva.atelier.mobile.core.designsystem.component.SurfaceTier
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.HeroShape

/**
 * Cabecera atmosférica común del flujo de acceso: [SurfaceTier.HeroNavy] con el imagotipo en
 * contraste blanco sobre azul profundo, título y contexto breve.
 */
@Composable
internal fun AuthHeroHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    badge: (@Composable () -> Unit)? = null
) {
    val colors = AtelierTheme.colors
    AtelierSurface(
        modifier = modifier.fillMaxWidth(),
        tier = SurfaceTier.HeroNavy,
        shape = HeroShape,
        contentPadding = PaddingValues(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // AtelierLogo usa onBackground; sobre HeroNavy forzamos tinta clara para preservar contraste AA.
            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme.copy(onBackground = colors.onPanel)
            ) {
                AtelierLogo(modifier = Modifier.height(36.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onPanel,
                    modifier = Modifier.semantics { heading() }
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onPanel.copy(alpha = 0.84f)
                    )
                }
            }
            badge?.invoke()
        }
    }
}
