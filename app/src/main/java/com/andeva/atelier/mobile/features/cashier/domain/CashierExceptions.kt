package com.andeva.atelier.mobile.features.cashier.domain

/** Un dato no cumple [CashierValidation]; [field] dice cuál (`taxId`, `serie`, `amount`…). */
class InvalidCashierInputException(val field: String) : Exception("Invalid cashier input: $field")

/** La sede no tiene una serie activa para el tipo de comprobante. */
class MissingFiscalSeriesException : Exception("No active fiscal series for the voucher type")

/**
 * No se sabe cómo terminó un cobro: el envío quedó sin respuesta, con un 5xx o con un 2xx sin cuerpo legible, o el
 * backend lo registró pero la relectura del comprobante falló. Hay que revisar el historial antes de volver a cobrar.
 */
class PaymentOutcomeUnknownException(cause: Throwable? = null) : Exception("Payment outcome unknown", cause)
