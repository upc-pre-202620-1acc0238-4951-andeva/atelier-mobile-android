package com.andeva.atelier.mobile.features.cashier.domain

/** Un dato no cumple [CashierValidation]; [field] dice cuál (`taxId`, `serie`, `amount`…). */
class InvalidCashierInputException(val field: String) : Exception("Invalid cashier input: $field")

/** La sede no tiene una serie activa para el tipo de comprobante. */
class MissingFiscalSeriesException : Exception("No active fiscal series for the voucher type")

/** Un cobro terminó sin respuesta: no se sabe si el backend lo registró; hay que revisar el historial antes de reintentar. */
class PaymentOutcomeUnknownException(cause: Throwable? = null) : Exception("Payment outcome unknown", cause)
