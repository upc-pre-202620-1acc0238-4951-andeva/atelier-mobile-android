package com.andeva.atelier.mobile.features.diagnostics.domain

/** Falta `BLUETOOTH_CONNECT` (API 31+): pedirlo con su explicación es de la UI. */
class BluetoothPermissionDeniedException : Exception("Bluetooth permission denied")

/** El teléfono no tiene Bluetooth o está apagado. */
class BluetoothDisabledException : Exception("Bluetooth is disabled")

/** No se pudo abrir el canal con el adaptador ELM327: apagado, fuera de alcance o sin alimentación (US16 E2). */
class ObdAdapterUnreachableException(cause: Throwable? = null) : Exception("OBD adapter unreachable", cause)

/**
 * El adaptador responde pero la computadora del vehículo no (US16 E2): contacto apagado o caída de tensión en el
 * conector. La UI orienta a revisar la alimentación y la posición del interruptor.
 */
class ObdNoResponseException : Exception("Vehicle ECU did not respond")

/** Se cortó la comunicación con el adaptador a mitad de la sesión. */
class ObdConnectionLostException(cause: Throwable? = null) : Exception("OBD connection lost", cause)

/** La acción necesita una sesión de diagnóstico conectada. */
class ObdNotConnectedException : Exception("OBD adapter not connected")

/**
 * `POST /iot/health-reports/generate` respondió 422 `ERR_INSUFFICIENT_TELEMETRY` (US21 E3): ampliar la ventana o
 * escanear en bahía. [message] es el del backend.
 */
class InsufficientTelemetryException(override val message: String) : Exception(message)

/** Un dato no cumple [DiagnosticsValidation]; [field] dice cuál (`daysToAnalyze`, `odometerKm`…). */
class InvalidDiagnosticsInputException(val field: String) : Exception("Invalid diagnostics input: $field")
