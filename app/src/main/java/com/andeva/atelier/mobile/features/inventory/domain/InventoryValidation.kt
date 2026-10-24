package com.andeva.atelier.mobile.features.inventory.domain

import java.time.Instant

/** Restricciones de docs/api/inventory.md. Solo evitan una llamada inútil; el backend vuelve a validar. */
object InventoryValidation {
    private val SKU = Regex("^[A-Z0-9_-]{3,50}$")

    /** El backend pasa el SKU a mayúsculas y responde 400 si no cumple el formato. */
    fun normalizeSku(sku: String) = sku.trim().uppercase()

    fun isSku(sku: String) = SKU.matches(normalizeSku(sku))

    fun isReceiptNumber(value: String) = value.isNotBlank()

    fun isReceivedAt(value: Instant?, now: Instant) = value == null || !value.isAfter(now)
}

/** Un dato no cumple [InventoryValidation]; [field] dice cuál (`sku`, `receiptNumber`, `photo`…). */
class InvalidInventoryInputException(val field: String) : Exception("Invalid inventory input: $field")
