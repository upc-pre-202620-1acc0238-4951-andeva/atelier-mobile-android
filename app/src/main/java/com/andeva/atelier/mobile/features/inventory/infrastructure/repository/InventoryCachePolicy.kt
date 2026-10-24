package com.andeva.atelier.mobile.features.inventory.infrastructure.repository

/** Alcances de `cache_metadata` y vigencias de docs/06-offline-sync.md §4 para las cachés de inventario. */
object InventoryCachePolicy {
    const val SCOPE_CATALOG = "parts:catalog"
    const val CATALOG_TTL_MILLIS = 60 * 60 * 1000L
    const val BATCHES_TTL_MILLIS = 60 * 60 * 1000L

    /** Páginas de `GET /inventory/items` para recargar el catálogo; el tope evita un bucle si el backend no corta. */
    const val PAGE_SIZE = 100
    const val MAX_PAGES = 50

    fun batchesScope(itemId: String) = "batches:$itemId"
}
