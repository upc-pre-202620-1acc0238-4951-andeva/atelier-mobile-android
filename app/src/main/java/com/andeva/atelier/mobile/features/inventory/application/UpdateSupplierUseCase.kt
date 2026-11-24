package com.andeva.atelier.mobile.features.inventory.application

import com.andeva.atelier.mobile.features.inventory.domain.InvalidInventoryInputException
import com.andeva.atelier.mobile.features.inventory.domain.InventoryRepository
import com.andeva.atelier.mobile.features.inventory.domain.InventoryValidation
import com.andeva.atelier.mobile.features.inventory.domain.Supplier
import javax.inject.Inject

/** Actualización de proveedor (`inventory:suppliers:manage`). */
class UpdateSupplierUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {
    suspend operator fun invoke(
        id: String,
        businessName: String? = null,
        contactName: String? = null,
        phone: String? = null,
        email: String? = null,
        address: String? = null,
        active: Boolean? = null
    ): Result<Supplier> {
        val invalid = when {
            !InventoryValidation.isNonEmptyText(id) -> "id"
            businessName != null && !InventoryValidation.isNonEmptyText(businessName) -> "businessName"
            !InventoryValidation.isOptionalEmail(email) -> "email"
            else -> null
        }
        if (invalid != null) return Result.failure(InvalidInventoryInputException(invalid))
        return inventoryRepository.updateSupplier(id, businessName, contactName, phone, email, address, active)
    }
}
