package com.andeva.atelier.mobile.core.designsystem.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import kotlinx.coroutines.delay

/**
 * El esqueleto espera 150 ms antes de aparecer: si los datos llegan antes, no se muestra (DESIGN.md → Carga).
 * Úsalo con `isLoading && noHayCaché`; con caché se pinta Room y basta [AtelierRefreshBar].
 */
@Composable
fun rememberDelayedLoading(loading: Boolean): Boolean {
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(loading) {
        if (loading) delay(150)
        show = loading
    }
    return show && loading
}

/**
 * Pinta este dato como un hueso mientras [loading]: mismo tamaño y márgenes que el contenido real, así al
 * llegar los datos nada se mueve. Se aplica a cada hoja de datos (texto, cifra, placa, foto) de un layout
 * renderizado con un `UiState` de muestra; lo fijo (títulos, barras, FAB) se deja sin hueso.
 *
 * [widthFraction] entre 0.4 y 0.8 del ancho de la línea. Late en opacidad (1 → 0.55, 1.2 s), sin barrido;
 * con "quitar animaciones" queda fijo. Los huesos no existen para TalkBack: el contenedor anuncia la carga.
 */
@Composable
fun Modifier.bone(loading: Boolean, onPanel: Boolean = false, widthFraction: Float = 1f): Modifier {
    if (!loading) return this
    val colors = AtelierTheme.colors
    val color = if (onPanel) colors.boneOnPanel else colors.bone
    val pulse = if (rememberReducedMotion()) remember { mutableStateOf(1f) } else bonePulse()
    return clearAndSetSemantics { }.drawWithContent {
        val radius = 6.dp.toPx()
        drawRoundRect(
            color = color,
            size = Size(size.width * widthFraction, size.height),
            cornerRadius = CornerRadius(radius, radius),
            alpha = pulse.value
        )
    }
}

@Composable
private fun bonePulse(): State<Float> = rememberInfiniteTransition(label = "bone").animateFloat(
    initialValue = 1f,
    targetValue = 0.55f,
    animationSpec = infiniteRepeatable(tween(durationMillis = 600), RepeatMode.Reverse),
    label = "bonePulse"
)
