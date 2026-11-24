package com.andeva.atelier.mobile.features.inventory.application

import com.andeva.atelier.mobile.features.inventory.domain.InvalidInventoryInputException
import com.andeva.atelier.mobile.features.inventory.domain.InventoryRepository
import com.andeva.atelier.mobile.features.inventory.domain.InventoryValidation
import com.andeva.atelier.mobile.features.inventory.domain.Supplier
import javax.inject.Inject

/** Alta de proveedor homologado con RUC/documento (`inventory:suppliers:manage`). */
class CreateSupplierUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository
) {
    suspend operator fun invoke(
        businessName: String,
        taxId: String,
        contactName: String? = null,
        phone: String? = null,
        email: String? = null,
        address: String? = null
    ): Result<Supplier> {
        val invalid = when {
            !InventoryValidation.isNonEmptyText(businessName) -> "businessName"
            !InventoryValidation.isTaxId(taxId) -> "taxId"
            !InventoryValidation.isOptionalEmail(email) -> "email"
            else -> null
        }
        if (invalid != null) return Result.failure(InvalidInventoryInputException(invalid))
        return inventoryRepository.createSupplier(businessName, taxId, contactName, phone, email, address)
    }
}
