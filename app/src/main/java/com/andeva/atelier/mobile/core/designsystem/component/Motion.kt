package com.andeva.atelier.mobile.core.designsystem.component

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Ease-out exponencial del diseño: `cubic-bezier(0.23, 1, 0.32, 1)`. */
val EaseOutQuint = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

/** "Quitar animaciones" del sistema: sin movimiento, el contenido aparece directo. */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/**
 * Entrada escalonada de la pantalla: sube 8 dp y aparece en 420 ms; cada [index] se retrasa 50 ms.
 * Es el único momento animado de cada pantalla.
 */
@Composable
fun Modifier.rise(index: Int = 0): Modifier {
    if (rememberReducedMotion()) return this
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 50L)
        progress.animateTo(1f, tween(durationMillis = 420, easing = EaseOutQuint))
    }
    val offsetPx = with(LocalDensity.current) { 8.dp.toPx() }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * offsetPx
    }
}

/** Al presionar el control se encoge a 97 % (160 ms). */
@Composable
fun Modifier.pressScale(interactionSource: InteractionSource): Modifier {
    if (rememberReducedMotion()) return this
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 160, easing = EaseOutQuint),
        label = "pressScale"
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
