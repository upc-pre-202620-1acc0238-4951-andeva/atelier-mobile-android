package com.andeva.atelier.mobile.features.main.presentation.obd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.rise
import com.andeva.atelier.mobile.core.designsystem.icon.bluetooth_searching
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme

/**
 * Lugar de la pestaña OBD-II (E1) hasta que `diagnostics` tenga su capa de presentación. Muestra el estado
 * "escáner no conectado" del diseño sin inventar conexiones.
 */
@Composable
fun ObdScreen() {
    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = bluetooth_searching,
            contentDescription = null,
            tint = AtelierTheme.colors.muted,
            modifier = Modifier.size(48.dp).rise(0)
        )
        Text(
            text = stringResource(R.string.main_obd_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.rise(1)
        )
        Text(
            text = stringResource(R.string.main_obd_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = AtelierTheme.colors.inkSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.rise(2)
        )
    }
}
