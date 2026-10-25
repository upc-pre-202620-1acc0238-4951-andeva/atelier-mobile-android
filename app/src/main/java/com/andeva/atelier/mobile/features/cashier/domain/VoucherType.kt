package com.andeva.atelier.mobile.features.cashier.domain

/** Código SUNAT en [code]: el backend envía `voucherType` como "01"/"03"/"07" (docs/api/README.md §8). */
enum class VoucherType(val code: String) {
    FACTURA("01"),
    BOLETA("03"),
    NOTA_CREDITO("07");

    companion object {
        /** Acepta el código o el nombre; null si no lo reconoce. */
        fun from(value: String?): VoucherType? =
            entries.firstOrNull { it.code == value || it.name.equals(value, ignoreCase = true) }
    }
}

enum class VoucherStatus { DRAFT, ISSUED, ACCEPTED_SUNAT, REJECTED_SUNAT, VOIDED }

enum class VoucherPaymentStatus { PENDING, PARTIAL, PAID }

enum class PaymentMethod {
    CASH, CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER, DIGITAL_WALLET_YAPE, DIGITAL_WALLET_PLIN
}

enum class FiscalDocumentType { DNI, CE, RUC, PASAPORTE }

enum class VoucherItemType { PRODUCT, SERVICE }
