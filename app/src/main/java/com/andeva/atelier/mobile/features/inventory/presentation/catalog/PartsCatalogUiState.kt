package com.andeva.atelier.mobile.features.inventory.presentation.catalog

import androidx.annotation.StringRes
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
    val parts: List<Part> = emptyList(),
    val isLoaded: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSearching: Boolean = false,
    val isOnline: Boolean = true,
    /** La última recarga falló (sin red): se sigue con la caché ("Actualizado hace N min"). */
    val showingCache: Boolean = false,
    val error: UiText? = null,
    val canRead: Boolean = true,
    val canManage: Boolean = false,
    val bannerMessage: UiText? = null,
    val createSheetOpen: Boolean = false,
    val formName: String = "",
    val formSku: String = "",
    val formCategory: ItemCategory = ItemCategory.LUBRICANTS,
    val formBasePrice: String = "",
    val formMinimumStock: String = "",
    @StringRes val nameError: Int? = null,
    @StringRes val skuError: Int? = null,
    @StringRes val priceError: Int? = null,
    @StringRes val minStockError: Int? = null,
    val createError: UiText? = null,
    val isCreating: Boolean = false,
    val hapticEvent: Int = 0,
    val hapticIsError: Boolean = false
) {
    val hasFilters: Boolean get() = query.isNotBlank() || category != null
}
