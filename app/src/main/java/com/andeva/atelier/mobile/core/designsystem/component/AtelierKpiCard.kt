package com.andeva.atelier.mobile.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row

/**
 * Indicador del panel de la sede: título, una cifra tabular, una línea de contexto y, si hace falta, una marca de
 * estado. Sin icono en cajita: la cifra es lo que se lee (DESIGN.md → Don't). Toda la tarjeta es un solo objetivo
 * táctil que lleva al módulo; con [loading] la cifra y el contexto son huesos.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AtelierKpiCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    chip: (@Composable () -> Unit)? = null,
    loading: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .heightIn(min = 120.dp)
            .then(if (onClick != null) Modifier.pressScale(interaction) else Modifier)
            .clip(CardShape)
            .background(AtelierTheme.colors.card)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CardShape)
            .then(
                if (onClick != null && !loading) {
                    Modifier.clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
                } else Modifier
            )
            .padding(16.dp)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.bone(loading, widthFraction = 0.4f)
            )
            if (chip != null && !loading) chip()
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = AtelierTheme.colors.inkSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.bone(loading, widthFraction = 0.8f)
        )
    }
}

@Composable
private fun KpiPreview() {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AtelierKpiCard(
                title = "Órdenes activas", value = "3", subtitle = "1 en proforma · 1 en bahía",
                modifier = Modifier.weight(1f),
                chip = { AtelierChip(text = "1 proforma", kind = ChipKind.Warn, icon = Icons.Outlined.WarningAmber) },
                onClick = {}
            )
            AtelierKpiCard(
                title = "Bahías de trabajo", value = "2/2", subtitle = "0 bahías disponibles",
                modifier = Modifier.weight(1f), onClick = {}
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AtelierKpiCard(
                title = "Clientes y vehículos", value = "5 · 4", subtitle = "2 citas agendadas hoy",
                modifier = Modifier.weight(1f), onClick = {}
            )
            AtelierKpiCard(
                title = "Alertas de stock", value = "–", subtitle = "Cargando", loading = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview(name = "Indicadores · claro", widthDp = 390, showBackground = true, backgroundColor = 0xFFF8F8FA)
@Composable
private fun KpiPreviewLight() = AtelierWorkshopTheme(darkTheme = false) { KpiPreview() }

@Preview(
    name = "Indicadores · oscuro", widthDp = 390, showBackground = true, backgroundColor = 0xFF262626,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun KpiPreviewDark() = AtelierWorkshopTheme(darkTheme = true) { KpiPreview() }
