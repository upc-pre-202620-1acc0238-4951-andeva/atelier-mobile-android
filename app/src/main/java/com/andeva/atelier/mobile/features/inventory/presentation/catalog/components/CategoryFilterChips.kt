package com.andeva.atelier.mobile.features.inventory.presentation.catalog.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.features.inventory.domain.ItemCategory
import com.andeva.atelier.mobile.features.inventory.presentation.labelRes

/** US31 E1: una chip por cada una de las 9 `ItemCategory`; tocar la activa la quita. */
@Composable
internal fun CategoryFilterChips(
    selected: ItemCategory?,
    onClick: (ItemCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ItemCategory.entries.forEach { category ->
            FilterChip(
                selected = category == selected,
                onClick = { onClick(category) },
                label = { Text(stringResource(category.labelRes())) }
            )
        }
    }
}
