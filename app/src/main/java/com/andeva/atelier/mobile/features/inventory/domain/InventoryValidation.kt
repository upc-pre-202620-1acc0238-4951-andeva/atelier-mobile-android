package com.andeva.atelier.mobile.features.inventory.domain

import java.math.BigDecimal
import java.time.Instant

/** Restricciones de docs/api/inventory.md. Solo evitan una llamada inútil; el backend vuelve a validar. */
object InventoryValidation {
    private val SKU = Regex("^[A-Z0-9_-]{3,50}$")
    private val TAX_ID = Regex("^\\d{8,20}$")
    private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    private val MIN_POSITIVE_QTY = BigDecimal("0.01")

    /** El backend pasa el SKU a mayúsculas y responde 400 si no cumple el formato. */
    fun normalizeSku(sku: String) = sku.trim().uppercase()

    fun isSku(sku: String) = SKU.matches(normalizeSku(sku))

    fun isReceiptNumber(value: String) = value.isNotBlank()

    fun isReceivedAt(value: Instant?, now: Instant) = value == null || !value.isAfter(now)

    fun isNonEmptyText(value: String) = value.trim().isNotEmpty()

    fun isNonNegative(value: BigDecimal) = value >= BigDecimal.ZERO

    fun isPositiveQty(value: BigDecimal) = value >= MIN_POSITIVE_QTY

    fun normalizeTaxId(value: String) = value.trim()

    fun isTaxId(value: String) = TAX_ID.matches(normalizeTaxId(value))

    fun isOptionalEmail(value: String?): Boolean {
        val trimmed = value?.trim().orEmpty()
        return trimmed.isEmpty() || EMAIL.matches(trimmed)
    }
}

/** Un dato no cumple [InventoryValidation]; [field] dice cuál (`sku`, `receiptNumber`, `photo`…). */
class InvalidInventoryInputException(val field: String) : Exception("Invalid inventory input: $field")
