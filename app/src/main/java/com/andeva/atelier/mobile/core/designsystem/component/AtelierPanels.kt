package com.andeva.atelier.mobile.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierAccent
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierAmberDark
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierAmberSoft
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardLargeShape
import com.andeva.atelier.mobile.core.designsystem.theme.ControlShape
import com.andeva.atelier.mobile.core.designsystem.theme.FloatingBarShape
import com.andeva.atelier.mobile.core.designsystem.theme.HeroShape

/** Cifra secundaria del Parte. Con [onClick] lleva a su lista filtrada. */
@Immutable
data class PanelFigure(val value: String, val label: String, val onClick: (() -> Unit)? = null)

/**
 * Tarjeta Hero Navy esculpida en tamaño Parte, para pantallas de supervisión: vive bajo la cabecera
 * limpia con margen lateral, degradado direccional, arco de luz, leyenda única en Display y cifras
 * secundarias en sub-cápsulas translúcidas.
 *
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
    AtelierSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        tier = SurfaceTier.HeroNavy,
        shape = HeroShape,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val contextInteraction = remember { MutableInteractionSource() }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = if (onContextClick != null) 48.dp else 28.dp)
                        .then(
                            if (onContextClick != null) {
                                Modifier
                                    .springPressScale(contextInteraction)
                                    .clip(ControlShape)
                                    .clickable(
                                        interactionSource = contextInteraction,
                                        indication = ripple(color = Color.White),
                                        role = Role.Button,
                                        onClick = onContextClick
                                    )
                            } else {
                                Modifier
                            }
                        ),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = context,
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onPanel.copy(alpha = 0.85f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (onContextClick != null) {
                        Icon(
                            imageVector = Icons.Outlined.ExpandMore,
                            contentDescription = null,
                            tint = colors.onPanel.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                actions()
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = legend,
                style = MaterialTheme.typography.displaySmall,
                color = colors.onPanel,
                modifier = Modifier
                    .semantics { heading() }
                    .bone(loading, onPanel = true, widthFraction = 0.6f)
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onPanel.copy(alpha = 0.90f)
            )

            if (figures.isNotEmpty()) {
                HorizontalDivider(
                    modifier = Modifier.padding(top = 14.dp, bottom = 12.dp),
                    thickness = 1.dp,
                    color = Color.White.copy(alpha = 0.16f)
                )
                // FlowRow: a fuente 1.3 las cifras bajan a una columna en vez de recortarse.
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    figures.forEach { figure ->
                        ReportFigureCapsule(figure = figure, loading = loading)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportFigureCapsule(figure: PanelFigure, loading: Boolean) {
    val colors = AtelierTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .then(
                if (figure.onClick != null) Modifier.springPressScale(interaction) else Modifier
            )
            .clip(ControlShape)
            .background(Color.White.copy(alpha = 0.10f), ControlShape)
            .border(1.dp, Color.White.copy(alpha = 0.14f), ControlShape)
            .then(
                if (figure.onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = ripple(color = Color.White),
                        role = Role.Button,
                        onClick = figure.onClick
                    )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = figure.value,
            style = MaterialTheme.typography.headlineMedium,
            color = colors.onPanel,
            modifier = Modifier.bone(loading, onPanel = true)
        )
        Text(
            text = figure.label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onPanel.copy(alpha = 0.85f)
        )
    }
}

/**
 * Cabecera de detalle en capas (Ficha): barra superior limpia sobre el lienzo (Atrás en medallón circular
 * elevado + [actions]) y debajo una tarjeta [SurfaceTier.HeroNavy] con [identity], [status], [title] y [subtitle].
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
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(scheme.background)
            .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
            .padding(bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AtelierCircularIconButton(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.common_back),
                onClick = onBack
            )
            Spacer(modifier = Modifier.weight(1f))
            CompositionLocalProvider(LocalContentColor provides scheme.onBackground) {
                actions()
            }
        }
        AtelierSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            tier = SurfaceTier.HeroNavy,
            shape = HeroShape,
            contentPadding = PaddingValues(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (identity != null || status != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        identity?.invoke()
                        Spacer(modifier = Modifier.weight(1f))
                        status?.invoke()
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onPanel,
                    modifier = Modifier.semantics { heading() }
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onPanel.copy(alpha = 0.85f)
                    )
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
 * Panel de decisión refinado («Precisión Automotriz»): superficie [SurfaceTier.DecisionAmber] con barra
 * acento interior redondeada, placa mecanizada y respuesta táctil con resorte. El ámbar es solo para
 * decisiones humanas pendientes.
 *
 * Con [resolved] muestra un chip verde elegante con el resultado ("Aprobada", "Rechazada") antes de retirarse.
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
    val scheme = MaterialTheme.colorScheme

    AtelierSurface(
        modifier = modifier.fillMaxWidth(),
        tier = SurfaceTier.DecisionAmber,
        shape = CardLargeShape,
        onClick = if (resolved == null) onClick else null,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
    ) {
        AtelierAdaptiveRow(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            horizontalSpacing = 12.dp,
            verticalSpacing = 8.dp
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    plate != null -> AtelierPlate(plate, size = PlateSize.L)
                    leading != null -> leading()
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurface
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.inkSecondary
                        )
                    }
                }
            }
            if (resolved != null) {
                AtelierChip(
                    text = resolved,
                    kind = ChipKind.Ok,
                    icon = Icons.Outlined.CheckCircle
                )
            } else if (amount != null) {
                Text(
                    text = amount,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.decisionAccent,
                    maxLines = 1
                )
            }
        }
    }
}

// region Previews

@Preview(name = "Parte y decisión · claro", widthDp = 390, showBackground = true, backgroundColor = 0xFFF8F8FA)
@Composable
private fun PanelsPreviewLight() = AtelierWorkshopTheme(darkTheme = false) { PanelsPreview() }

@Preview(
    name = "Parte y decisión · oscuro",
    widthDp = 390,
    showBackground = true,
    backgroundColor = 0xFF262626,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun PanelsPreviewDark() = AtelierWorkshopTheme(darkTheme = true) { PanelsPreview() }

@Composable
private fun PanelsPreview() {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AtelierReportPanel(
            context = "Sede Chorrillos · lunes 16 de noviembre",
            legend = "7 autos",
            caption = "en el taller hoy",
            figures = listOf(
                PanelFigure("3", "en bahía", onClick = {}),
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
            AtelierDecisionRow(
                title = "Proforma sin respuesta",
                plate = "F4T119",
                resolved = "Aprobada",
                onClick = {}
            )
        }
    }
}

@Preview(name = "Ficha · claro", widthDp = 390, showBackground = true, backgroundColor = 0xFFF8F8FA)
@Composable
private fun FichaPreviewLight() = AtelierWorkshopTheme(darkTheme = false) {
    AtelierFichaPanel(
        onBack = {},
        title = "Toyota Corolla 2021",
        subtitle = "OT #1042 · Bahía 2 · S/ 640.00",
        identity = { AtelierPlate("BKL482", size = PlateSize.L) },
        status = { AtelierChip(text = "En curso", kind = ChipKind.Info, live = true) }
    )
}

@Preview(
    name = "Ficha · oscuro",
    widthDp = 390,
    showBackground = true,
    backgroundColor = 0xFF262626,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun FichaPreviewDark() = AtelierWorkshopTheme(darkTheme = true) {
    AtelierFichaPanel(
        onBack = {},
        title = "Toyota Corolla 2021",
        subtitle = "OT #1042 · Bahía 2 · S/ 640.00",
        identity = { AtelierPlate("BKL482", size = PlateSize.L) },
        status = { AtelierChip(text = "En curso", kind = ChipKind.Info, live = true) }
    )
}

// endregion
