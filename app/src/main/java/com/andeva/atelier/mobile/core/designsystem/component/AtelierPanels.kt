package com.andeva.atelier.mobile.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.ControlShape
import com.andeva.atelier.mobile.core.designsystem.theme.PanelShape
import com.andeva.atelier.mobile.core.designsystem.theme.PanelStatusBarEffect

/** Cifra secundaria del Parte. Con [onClick] lleva a su lista filtrada. */
@Immutable
data class PanelFigure(val value: String, val label: String, val onClick: (() -> Unit)? = null)

/**
 * Panel de cabecera en tamaño Parte, para pantallas de supervisión: Azul Señal a sangre con filete blanco,
 * una línea de contexto (sede y fecha), la leyenda en Display y hasta tres cifras secundarias.
 *
 * Es la única leyenda en Display de la pantalla. Las cifras no se animan al cambiar: cambian.
 * Con [loading] las cifras son huesos y lo fijo (contexto, rótulos) se ve desde el primer cuadro.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AtelierReportPanel(
    context: String,
    legend: String,
    caption: String,
    modifier: Modifier = Modifier,
    figures: List<PanelFigure> = emptyList(),
    loading: Boolean = false,
    onContextClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val colors = AtelierTheme.colors
    PanelStatusBarEffect()
    CompositionLocalProvider(LocalContentColor provides colors.onPanel) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .panelBleed(colors.panel)
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, colors.onPanel, PanelShape)
                    .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .then(
                                if (onContextClick != null) Modifier.clickable(role = Role.Button, onClick = onContextClick)
                                else Modifier
                            ),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = context,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (onContextClick != null) {
                            Icon(Icons.Outlined.ExpandMore, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    }
                    actions()
                }
                Column(modifier = Modifier.padding(end = 8.dp)) {
                    Text(
                        text = legend,
                        style = MaterialTheme.typography.displaySmall,
                        modifier = Modifier
                            .semantics { heading() }
                            .bone(loading, onPanel = true, widthFraction = 0.6f)
                    )
                    Text(text = caption, style = MaterialTheme.typography.titleMedium)
                    if (figures.isNotEmpty()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 16.dp),
                            thickness = 2.dp,
                            color = colors.onPanel
                        )
                        // FlowRow: a fuente 1.3 las cifras bajan a una columna en vez de recortarse.
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            figures.forEach { figure ->
                                Column(
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .then(
                                            if (figure.onClick != null) {
                                                Modifier.clickable(role = Role.Button, onClick = figure.onClick)
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .padding(top = 8.dp)
                                ) {
                                    Text(
                                        text = figure.value,
                                        style = MaterialTheme.typography.headlineMedium,
                                        modifier = Modifier.bone(loading, onPanel = true)
                                    )
                                    Text(text = figure.label, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Panel de cabecera en tamaño Ficha, para el detalle de un objeto (una orden, un vehículo, un colaborador):
 * Atrás, la identidad en [identity] (la placa en grande, o el nombre) y su estado en [status]. Sin filete.
 */
@Composable
fun AtelierFichaPanel(
    onBack: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    identity: @Composable (() -> Unit)? = null,
    status: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val colors = AtelierTheme.colors
    PanelStatusBarEffect()
    CompositionLocalProvider(LocalContentColor provides colors.onPanel) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .panelBleed(colors.panel)
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.common_back))
                }
                Box(modifier = Modifier.weight(1f))
                actions()
            }
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    identity?.invoke()
                    Box(modifier = Modifier.weight(1f))
                    status?.invoke()
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() }
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

/** Título de sección. Con [count] se escribe "Esperan tu decisión · 3": el conteo siempre va. */
@Composable
fun AtelierSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    action: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (count != null) stringResource(R.string.section_with_count, title, count) else title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        action?.invoke()
    }
}

/**
 * Panel de decisión: algo espera a una persona (una propuesta por aprobar, una proforma sin respuesta,
 * un descargo por revisar, una suspensión por validar). El ámbar es solo para esto.
 *
 * [plate] a la izquierda (o [leading] cuando la decisión es sobre una persona), qué se decide y quién lo
 * originó al centro, el monto a la derecha. Toda la fila abre la decisión en una hoja inferior.
 * Con [resolved] muestra el resultado ("Aprobada", "Rechazada") antes de que la lista lo retire.
 */
@Composable
fun AtelierDecisionRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    plate: String? = null,
    subtitle: String? = null,
    amount: String? = null,
    resolved: String? = null,
    enabled: Boolean = true,
    leading: @Composable (() -> Unit)? = null
) {
    val colors = AtelierTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val container = when {
        resolved != null -> colors.decisionSoft
        pressed -> colors.decisionPressed
        else -> colors.decision
    }
    CompositionLocalProvider(LocalContentColor provides colors.onDecision) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .clip(ControlShape)
                .background(container)
                .clickable(
                    interactionSource = interaction,
                    indication = ripple(),
                    enabled = enabled && resolved == null,
                    role = Role.Button,
                    onClick = onClick
                )
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                plate != null -> AtelierPlate(plate, size = PlateSize.L)
                leading != null -> leading()
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                if (!subtitle.isNullOrBlank()) {
                    Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (resolved != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(text = resolved, style = MaterialTheme.typography.labelLarge)
                }
            } else if (amount != null) {
                Text(text = amount, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            }
        }
    }
}

@Preview(name = "Parte y decisión · claro", widthDp = 390, showBackground = true, backgroundColor = 0xFFF8F8FA)
@Composable
private fun PanelsPreviewLight() = AtelierWorkshopTheme(darkTheme = false) { PanelsPreview() }

@Preview(name = "Parte y decisión · oscuro", widthDp = 390, showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun PanelsPreviewDark() = AtelierWorkshopTheme(darkTheme = true) { PanelsPreview() }

@Composable
private fun PanelsPreview() {
    Column {
        AtelierReportPanel(
            context = "Sede Chorrillos · lunes 16 de noviembre",
            legend = "7 autos",
            caption = "en el taller hoy",
            figures = listOf(
                PanelFigure("3", "en bahía"),
                PanelFigure("2", "listos para entregar"),
                PanelFigure("5/6", "en turno")
            )
        )
        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AtelierSectionHeader("Esperan tu decisión", count = 2)
            AtelierDecisionRow(
                title = "Propuesta: pastillas de freno",
                subtitle = "Hallazgo de Andrés V. · hace 12 min",
                plate = "BKL482",
                amount = "S/ 380.00",
                onClick = {}
            )
            AtelierDecisionRow(title = "Proforma sin respuesta", plate = "F4T119", resolved = "Aprobada", onClick = {})
        }
    }
}
