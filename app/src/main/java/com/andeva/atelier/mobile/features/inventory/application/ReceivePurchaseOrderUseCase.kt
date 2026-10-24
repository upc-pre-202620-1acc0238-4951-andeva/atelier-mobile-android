package com.andeva.atelier.mobile.features.inventory.application

import com.andeva.atelier.mobile.features.inventory.domain.InvalidInventoryInputException
import com.andeva.atelier.mobile.features.inventory.domain.InventoryRepository
import com.andeva.atelier.mobile.features.inventory.domain.InventoryValidation
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrder
import java.io.File
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * Recibir una orden de compra emitida con el número y la foto del comprobante (US34 E2): crea un lote por línea, sube
 * el stock y la alerta sale de la bandeja. Si ya estaba recibida responde éxito sin subir la foto (H15). En línea:
 * sin red falla con `IOException` y no se encola.
 */
class ReceivePurchaseOrderUseCase @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(id: String, receiptNumber: String, photo: File?, receivedAt: Instant? = null): Result<PurchaseOrder> {
        if (photo == null) return Result.failure(InvalidInventoryInputException("photo"))
        val invalid = when {
            !InventoryValidation.isReceiptNumber(receiptNumber) -> "receiptNumber"
            !InventoryValidation.isReceivedAt(receivedAt, clock.instant()) -> "receivedAt"
            else -> null
        }
        if (invalid != null) return Result.failure(InvalidInventoryInputException(invalid))
        return inventoryRepository.receivePurchaseOrder(id, receiptNumber, photo, receivedAt)
    }
}
