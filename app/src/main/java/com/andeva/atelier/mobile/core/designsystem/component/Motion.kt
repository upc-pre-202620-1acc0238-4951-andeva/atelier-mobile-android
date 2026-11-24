package com.andeva.atelier.mobile.core.designsystem.component

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Ease-out exponencial del diseño: `cubic-bezier(0.23, 1, 0.32, 1)`. */
val EaseOutQuint = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

/** Ease-out expo de la barra flotante: `cubic-bezier(0.16, 1, 0.3, 1)`, el mismo de quina-webapp. */
val EaseOutExpo = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

/** Resortes del sistema (apple-design + DESIGN.md): amortiguación crítica por defecto, rebote solo en gestos con impulso. */
object AtelierSprings {
    fun <T> criticallyDamped() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    fun <T> snappy() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /** Resorte rápido sin rebote para el indicador del dock y segmented control. */
    fun <T> dock() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 750f
    )

    fun <T> flick() = spring<T>(
        dampingRatio = 0.8f,
        stiffness = Spring.StiffnessMediumLow
    )
}

/** Háptica armonizada en el mismo cuadro visual (solo momentos de compromiso: confirmar, error, marcación). */
@Stable
class AtelierHaptics(private val haptic: HapticFeedback) {
    fun confirm() = haptic.performHapticFeedback(HapticFeedbackType.Confirm)
    fun reject() = haptic.performHapticFeedback(HapticFeedbackType.Reject)
    fun toggle() = haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
    fun longPress() = haptic.performHapticFeedback(HapticFeedbackType.LongPress)
}

@Composable
fun rememberAtelierHaptics(): AtelierHaptics {
    val haptic = LocalHapticFeedback.current
    return remember(haptic) { AtelierHaptics(haptic) }
}

/** "Quitar animaciones" del sistema: reactivo en vivo ante cambios en Ajustes de accesibilidad. */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    fun readScale(): Boolean =
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

    var reduced by remember(context) { mutableStateOf(readScale()) }
    DisposableEffect(context) {
        val resolver = context.contentResolver
        val uri = Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reduced = readScale()
            }
        }
        runCatching { resolver.registerContentObserver(uri, false, observer) }
        onDispose { runCatching { resolver.unregisterContentObserver(observer) } }
    }
    return reduced
}

/**
 * Entrada escalonada de la pantalla: sube 8 dp y aparece en 240 ms; cada [index] se retrasa 50 ms.
 * Solo para pantallas ocasionales (acceso, alta): lo que se abre decenas de veces al día no se anima.
 */
@Composable
fun Modifier.rise(index: Int = 0): Modifier {
    if (rememberReducedMotion()) return this
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 50L)
        progress.animateTo(1f, tween(durationMillis = 240, easing = EaseOutQuint))
    }
    val offsetPx = with(LocalDensity.current) { 8.dp.toPx() }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * offsetPx
    }
}

/** Al presionar el control se encoge a [pressed] (97 % por defecto, 120 ms). */
@Composable
fun Modifier.pressScale(interactionSource: InteractionSource, pressed: Float = 0.97f): Modifier {
    if (rememberReducedMotion()) return this
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressed else 1f,
        animationSpec = tween(durationMillis = 120, easing = EaseOutQuint),
        label = "pressScale"
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Al presionar el control se encoge a [pressed] usando un resorte críticamente amortiguado,
 * manteniendo continuidad e interrumpibilidad si el usuario suelta o vuelve a tocar a mitad del gesto.
 */
@Composable
fun Modifier.springPressScale(interactionSource: InteractionSource, pressed: Float = 0.97f): Modifier {
    if (rememberReducedMotion()) return this
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressed else 1f,
        animationSpec = AtelierSprings.snappy(),
        label = "springPressScale"
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
