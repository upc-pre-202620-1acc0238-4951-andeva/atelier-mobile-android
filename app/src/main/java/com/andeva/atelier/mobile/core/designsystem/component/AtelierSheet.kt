package com.andeva.atelier.mobile.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierBlueSoft
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.FloatingBarShape
import com.andeva.atelier.mobile.core.designsystem.theme.PanelShape
import com.andeva.atelier.mobile.core.designsystem.theme.SheetTopShape

/**
 * Hoja inferior para decidir sin salir de la lista (aprobar una propuesta, revisar un descargo, elegir sede).
 * Es la de Material 3 con [SheetTopShape] (24 dp), plano [AtelierColors.surfaceElevated] y manija táctil
 * estilizada: sigue al dedo, se suelta o retoma a mitad de camino y se cierra por velocidad.
 * El contenido va arriba y las acciones en [actions], al pie y al alcance del pulgar; el cuerpo hace
 * scroll si no cabe y la hoja sube con el teclado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtelierBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = AtelierTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = SheetTopShape,
        containerColor = colors.surfaceElevated,
        tonalElevation = 0.dp,
        contentWindowInsets = { WindowInsets(0) },
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(FloatingBarShape)
                    .background(colors.inkSecondary.copy(alpha = 0.28f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() }
            )
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = actions)
        }
    }
}

/**
 * FAB extendido esculpido, uno por pantalla, con verbo y objeto ("Recibir vehículo"): degradado
 * `#031A6B → #0071EB`, borde especular `#69B1FF @ 35%`, sombra azul real y respuesta [springPressScale].
 */
@Composable
fun AtelierFab(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AtelierTheme.colors
    val fabBrush = remember(colors.heroStart, colors.heroEnd) {
        Brush.linearGradient(listOf(colors.heroStart, colors.heroEnd))
    }

    AtelierSurface(
        modifier = modifier.heightIn(min = 56.dp),
        tier = SurfaceTier.Floating,
        shape = PanelShape,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = 56.dp)
                .background(fabBrush, PanelShape)
                .border(1.dp, AtelierBlueSoft.copy(alpha = 0.35f), PanelShape),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = Color.White)
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
            }
        }
    }
}
