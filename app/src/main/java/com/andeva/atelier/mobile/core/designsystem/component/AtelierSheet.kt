package com.andeva.atelier.mobile.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.PanelShape

/**
 * Hoja inferior para decidir sin salir de la lista (aprobar una propuesta, revisar un descargo, elegir sede).
 * Es la de Material 3: sigue al dedo, se suelta o retoma a mitad de camino y se cierra por velocidad.
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
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
 * FAB extendido, uno por pantalla, con verbo y objeto ("Recibir vehículo").
 * Azul Profundo en claro; Azul Suave con tinta Azul Profundo en oscuro.
 */
@Composable
fun AtelierFab(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AtelierTheme.colors
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = PanelShape,
        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
        containerColor = colors.fab,
        contentColor = colors.onFab,
        icon = { Icon(icon, contentDescription = null) },
        text = { Text(text = text, style = MaterialTheme.typography.labelLarge) }
    )
}
