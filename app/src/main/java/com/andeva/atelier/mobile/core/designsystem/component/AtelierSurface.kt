package com.andeva.atelier.mobile.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierBlueSoft
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardLargeShape
import com.andeva.atelier.mobile.core.designsystem.theme.ControlShape
import com.andeva.atelier.mobile.core.designsystem.theme.HeroShape

/**
 * Planos de profundidad del sistema visual «Precisión Automotriz».
 *
 * - [Recessed]: pozo hundido para pistas, contenedores de métricas y barras de progreso.
 * - [Card]: plano base de lectura sobre el lienzo, con borde especular vertical y sombra ambiental sutil.
 * - [Elevated]: plano elevado para cápsulas activas y tarjetas destacadas.
 * - [Floating]: plano superior para barras flotantes y menús despegados del contenido.
 * - [HeroNavy]: cabecera o tarjeta protagonista con degradado direccional `#031A6B → #08349C → #0071EB` y arco de luz.
 * - [DecisionAmber]: superficie ámbar refinada reservada exclusivamente para decisiones humanas pendientes.
 */
enum class SurfaceTier {
    Recessed,
    Card,
    Elevated,
    Floating,
    HeroNavy,
    DecisionAmber
}

/**
 * Contenedor físico multi-plano de Atelier con iluminación especular, sombra ambiental teñida
 * y respuesta táctil con resorte críticamente amortiguado cuando [onClick] no es nulo.
 */
@Composable
fun AtelierSurface(
    modifier: Modifier = Modifier,
    tier: SurfaceTier = SurfaceTier.Card,
    shape: Shape = CardLargeShape,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val colors = AtelierTheme.colors
    val scheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }

    val elevation: Dp = when (tier) {
        SurfaceTier.Recessed -> 0.dp
        SurfaceTier.Card -> 3.dp
        SurfaceTier.Elevated -> 8.dp
        SurfaceTier.Floating -> 14.dp
        SurfaceTier.HeroNavy -> 12.dp
        SurfaceTier.DecisionAmber -> 4.dp
    }

    val specularBrush = remember(colors) {
        Brush.verticalGradient(listOf(colors.specularTop, colors.specularBottom))
    }

    val border: BorderStroke? = when (tier) {
        SurfaceTier.Recessed -> BorderStroke(1.dp, colors.outlineSoft.copy(alpha = if (colors.isDark) 0.55f else 0.70f))
        SurfaceTier.Card,
        SurfaceTier.Elevated,
        SurfaceTier.Floating -> BorderStroke(1.dp, specularBrush)
        SurfaceTier.HeroNavy -> BorderStroke(1.dp, AtelierBlueSoft.copy(alpha = 0.28f))
        SurfaceTier.DecisionAmber -> BorderStroke(1.dp, colors.decisionBorder)
    }

    val backgroundModifier = when (tier) {
        SurfaceTier.Recessed -> Modifier.background(colors.surfaceRecessed, shape)
        SurfaceTier.Card -> Modifier.background(colors.card, shape)
        SurfaceTier.Elevated -> Modifier.background(colors.surfaceElevated, shape)
        SurfaceTier.Floating -> Modifier.background(colors.surfaceFloating, shape)
        SurfaceTier.HeroNavy -> Modifier.background(
            brush = Brush.linearGradient(listOf(colors.heroStart, colors.heroMid, colors.heroEnd)),
            shape = shape
        )
        SurfaceTier.DecisionAmber -> Modifier
            .background(colors.decisionSurface, shape)
            .background(
                brush = Brush.verticalGradient(listOf(colors.decisionWash, Color.Transparent)),
                shape = shape
            )
    }

    val resolvedContentColor = when (tier) {
        SurfaceTier.HeroNavy -> colors.onPanel
        else -> scheme.onSurface
    }

    val surfaceModifier = modifier
        .then(if (onClick != null && enabled) Modifier.springPressScale(interactionSource) else Modifier)
        .then(if (!enabled) Modifier.alpha(0.38f) else Modifier)
        .then(
            if (elevation > 0.dp) {
                Modifier.shadow(
                    elevation = elevation,
                    shape = shape,
                    clip = false,
                    ambientColor = colors.shadowAmbient,
                    spotColor = colors.shadowSpot
                )
            } else {
                Modifier
            }
        )
        .clip(shape)
        .then(backgroundModifier)
        .then(if (border != null) Modifier.border(border, shape) else Modifier)
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick
                )
            } else {
                Modifier
            }
        )

    CompositionLocalProvider(LocalContentColor provides resolvedContentColor) {
        Box(
            modifier = surfaceModifier,
            propagateMinConstraints = true
        ) {
            if (tier == SurfaceTier.HeroNavy) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val arcTint = AtelierBlueSoft.copy(alpha = 0.08f)
                    val radius = size.maxDimension * 0.62f
                    val center = Offset(x = size.width * 0.92f, y = size.height * 0.12f)
                    drawCircle(color = arcTint, radius = radius, center = center)
                    drawCircle(
                        color = arcTint,
                        radius = radius * 0.72f,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
            Box(
                modifier = Modifier.padding(contentPadding),
                content = content
            )
        }
    }
}

// region Previews

@Composable
private fun SurfaceTiersPreviewBody() {
    val colors = AtelierTheme.colors
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AtelierSurface(
            tier = SurfaceTier.HeroNavy,
            shape = HeroShape,
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("HeroNavy · Parte del taller", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Degradado direccional + arco geométrico",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onPanel.copy(alpha = 0.82f)
                )
            }
        }

        AtelierSurface(
            tier = SurfaceTier.DecisionAmber,
            shape = ControlShape,
            onClick = {},
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "DecisionAmber · Esperan tu decisión",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.decisionAccent
                )
                Text(
                    "BKL-482 · Cambio de pastillas delanteras",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        AtelierSurface(
            tier = SurfaceTier.Card,
            onClick = {},
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Card · Plano base con borde especular", style = MaterialTheme.typography.bodyLarge)
        }

        AtelierSurface(
            tier = SurfaceTier.Elevated,
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Elevated · Superficie despegada (8 dp)", style = MaterialTheme.typography.bodyLarge)
        }

        AtelierSurface(
            tier = SurfaceTier.Floating,
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Floating · Plano superior (14 dp)", style = MaterialTheme.typography.bodyLarge)
        }

        AtelierSurface(
            tier = SurfaceTier.Recessed,
            shape = ControlShape,
            contentPadding = PaddingValues(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Recessed · Pozo hundido (0 dp)",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkSecondary
            )
        }
    }
}

@Preview(name = "Superficies · claro", widthDp = 390, showBackground = true, backgroundColor = 0xFFF8F8FA)
@Composable
private fun AtelierSurfacePreviewLight() = AtelierWorkshopTheme(darkTheme = false) {
    SurfaceTiersPreviewBody()
}

@Preview(name = "Superficies · oscuro", widthDp = 390, showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun AtelierSurfacePreviewDark() = AtelierWorkshopTheme(darkTheme = true) {
    SurfaceTiersPreviewBody()
}

// endregion
