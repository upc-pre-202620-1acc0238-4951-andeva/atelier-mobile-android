package com.andeva.atelier.mobile.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle

/**
 * Acceso provisional, solo en builds debug, a las pantallas de tareas e inventario mientras `main` no tenga su
 * `NavigationBar` con pestañas por permiso (docs/07-roadmap.md, paso 3). Se retira al integrar `main`.
 */
@Composable
internal fun DebugEntryPoints(
    onOpenTasks: () -> Unit,
    onOpenCatalog: () -> Unit,
    onOpenOrders: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .safeDrawingPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AtelierButton(
            text = stringResource(R.string.debug_open_tasks),
            onClick = onOpenTasks,
            style = AtelierButtonStyle.Tonal
        )
        AtelierButton(
            text = stringResource(R.string.debug_open_orders),
            onClick = onOpenOrders,
            style = AtelierButtonStyle.Tonal
        )
        AtelierButton(
            text = stringResource(R.string.debug_open_catalog),
            onClick = onOpenCatalog,
            style = AtelierButtonStyle.Tonal
        )
    }
}
