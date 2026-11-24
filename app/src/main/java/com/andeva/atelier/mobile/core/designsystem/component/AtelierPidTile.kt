package com.andeva.atelier.mobile.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierBlue
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardLargeShape

/** Nivel semántico de una lectura telemétrica OBD-II (DESIGN.md → La Regla del Código Vial). */
enum class PidSeverity { NORMAL, WARNING, CRITICAL }

/**
 * Ficha calibrada de lectura OBD-II sobre [SurfaceTier.Card]: cifra tabular en Satoshi Black,
 * unidad, estado con ícono y texto cuando sale de rango, y barra métrica calibrada opcional ([level]).
 */
@Composable
fun AtelierPidTile(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    severity: PidSeverity = PidSeverity.NORMAL,
    statusText: String? = null,
    loading: Boolean = false,
    level: Float? = null
) {
    val colors = AtelierTheme.colors
    val scheme = MaterialTheme.colorScheme
    val borderColor = when (severity) {
        PidSeverity.NORMAL -> scheme.outlineVariant
        PidSeverity.WARNING -> colors.warnMark
        PidSeverity.CRITICAL -> colors.badMark
    }
    val valueColor = when (severity) {
        PidSeverity.NORMAL -> scheme.onSurface
        PidSeverity.WARNING -> colors.warn
        PidSeverity.CRITICAL -> colors.bad
    }
    val barColor = when (severity) {
        PidSeverity.NORMAL -> AtelierBlue
        PidSeverity.WARNING -> colors.warnMark
        PidSeverity.CRITICAL -> colors.badMark
    }
    val statusIcon = when (severity) {
        PidSeverity.NORMAL -> Icons.Outlined.CheckCircle
        PidSeverity.WARNING -> Icons.Outlined.WarningAmber
        PidSeverity.CRITICAL -> Icons.Outlined.ErrorOutline
    }
    val semanticOverlay = when (severity) {
        PidSeverity.NORMAL -> Modifier
        PidSeverity.WARNING -> Modifier
            .background(colors.warnContainer.copy(alpha = if (colors.isDark) 0.55f else 0.72f))
            .border(1.5.dp, borderColor, CardLargeShape)
        PidSeverity.CRITICAL -> Modifier
            .background(colors.badContainer.copy(alpha = if (colors.isDark) 0.55f else 0.72f))
            .border(1.5.dp, borderColor, CardLargeShape)
    }

    AtelierSurface(
        modifier = modifier.heightIn(min = 104.dp),
        tier = SurfaceTier.Card,
        shape = CardLargeShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(semanticOverlay)
                .padding(12.dp)
                .semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = colors.inkSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    color = valueColor,
                    maxLines = 1,
                    modifier = Modifier.bone(loading, widthFraction = 0.55f)
                )
                if (unit.isNotBlank() && !loading) {
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.inkSecondary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
            if (!statusText.isNullOrBlank() && !loading) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = valueColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        color = valueColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (level != null && !loading) {
                AtelierMetricBar(
                    progress = level,
                    color = barColor,
                    height = 5.dp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun PidPreviewBody() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AtelierPidTile(
            label = "RPM motor",
            value = "840",
            unit = "rpm",
            severity = PidSeverity.NORMAL,
            statusText = "Ralentí estable",
            level = 0.24f,
            modifier = Modifier.weight(1f)
        )
        AtelierPidTile(
            label = "Temp. motor",
            value = "108",
            unit = "°C",
            severity = PidSeverity.CRITICAL,
            statusText = "Sobrecalentamiento",
            level = 0.92f,
            modifier = Modifier.weight(1f)
        )
    }
}

@Preview(name = "PID · claro", widthDp = 390, showBackground = true)
@Composable
private fun PidPreviewLight() = AtelierWorkshopTheme(darkTheme = false) { PidPreviewBody() }

@Preview(
    name = "PID · oscuro",
    widthDp = 390,
    showBackground = true,
    backgroundColor = 0xFF262626,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun PidPreviewDark() = AtelierWorkshopTheme(darkTheme = true) { PidPreviewBody() }
