package com.andeva.atelier.mobile.features.diagnostics.domain

/**
 * Código de falla OBD-II (`P0301`, `U0100`…). Se normaliza a mayúsculas; el formato es el de
 * `RegisterVehicleFaultRequest.dtcCode` en docs/api/iot.md (`^[PBUC][0-9A-Fa-f]{4}$`).
 */
@JvmInline
value class Dtc private constructor(val code: String) {
    companion object {
        private val FORMAT = Regex("^[PBUC][0-9A-F]{4}$")

        fun normalize(code: String) = code.trim().uppercase()

        fun isValid(code: String) = FORMAT.matches(normalize(code))

        /** null si [code] no es un DTC válido. */
        fun of(code: String): Dtc? = normalize(code).takeIf { FORMAT.matches(it) }?.let(::Dtc)
    }

    override fun toString() = code
}

/** `FaultSeverity` del backend: `POST /iot/faults` solo acepta estos tres valores. */
enum class FaultSeverity { LOW, MEDIUM, CRITICAL }
