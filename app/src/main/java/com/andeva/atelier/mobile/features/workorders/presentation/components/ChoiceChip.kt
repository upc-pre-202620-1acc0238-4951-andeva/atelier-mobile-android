package com.andeva.atelier.mobile.features.workorders.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.andeva.atelier.mobile.core.designsystem.theme.ChipShape

/** Chip de elección de los formularios de órdenes: la elección lleva una marca y no depende solo del color. */
@Composable
internal fun ChoiceChip(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        shape = ChipShape,
        leadingIcon = if (selected) {
            { Icon(Icons.Outlined.Check, contentDescription = null) }
        } else null
    )
}
