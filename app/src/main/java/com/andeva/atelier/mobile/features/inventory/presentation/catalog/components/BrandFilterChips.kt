package com.andeva.atelier.mobile.features.inventory.presentation.catalog.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** US31 E1: una chip por marca del catálogo; tocar la activa la quita. No se dibuja sin marcas (H35). */
@Composable
internal fun BrandFilterChips(
    brands: List<String>,
    selected: String?,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (brands.isEmpty()) return
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        brands.forEach { brand ->
            FilterChip(
                selected = brand == selected,
                onClick = { onClick(brand) },
                label = { Text(brand) }
            )
        }
    }
}
