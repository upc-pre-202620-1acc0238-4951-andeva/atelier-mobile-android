package com.andeva.atelier.mobile.features.inventory.presentation.catalog

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.inventory.domain.ItemCategory
import com.andeva.atelier.mobile.features.inventory.domain.Part

/**
 * US31: catálogo de repuestos. Sin texto se ve la caché Room completa (por categoría); con texto, la búsqueda por
 * nombre o SKU. [category] null = todas.
 */
data class PartsCatalogUiState(
    val query: String = "",
    val category: ItemCategory? = null,
    /** Marca elegida y marcas disponibles; sin marcas (H35) no se muestra el filtro. */
    val brand: String? = null,
    val brands: List<String> = emptyList(),
    val parts: List<Part> = emptyList(),
    val isLoaded: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSearching: Boolean = false,
    /** La última recarga falló (sin red): se sigue con la caché ("Actualizado hace N min"). */
    val showingCache: Boolean = false,
    val error: UiText? = null,
    val canRead: Boolean = true
) {
    val hasFilters: Boolean get() = query.isNotBlank() || category != null || brand != null
}
