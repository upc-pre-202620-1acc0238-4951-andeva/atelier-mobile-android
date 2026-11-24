package com.andeva.atelier.mobile.features.inventory.application

import com.andeva.atelier.mobile.features.inventory.domain.InventoryRepository
import com.andeva.atelier.mobile.features.inventory.domain.Supplier
import javax.inject.Inject

/** Listado y búsqueda de proveedores homologados (`inventory:suppliers:manage`). */
class GetSuppliersUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {
    suspend operator fun invoke(search: String? = null, status: String? = null): Result<List<Supplier>> =
        inventoryRepository.getSuppliers(search, status)
}
