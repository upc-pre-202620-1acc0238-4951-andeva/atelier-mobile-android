package com.andeva.atelier.mobile.features.diagnostics.domain

/**
 * Descripción y severidad de cada DTC que se envía en `POST /iot/faults` (US17 E1). El backend exige `severity` y no
 * tiene catálogo de códigos, así que lo decide la app (hueco de docs/03-traceability.md): códigos genéricos SAE
 * conocidos y, para el resto, una regla por familia.
 */
object DtcCatalog {
    data class Entry(val description: String, val severity: FaultSeverity)

    private val KNOWN = mapOf(
        "P0100" to Entry("Circuito del sensor de flujo de aire (MAF)", FaultSeverity.MEDIUM),
        "P0101" to Entry("Rango o rendimiento del sensor de flujo de aire (MAF)", FaultSeverity.MEDIUM),
        "P0110" to Entry("Circuito del sensor de temperatura del aire de admisión", FaultSeverity.LOW),
        "P0115" to Entry("Circuito del sensor de temperatura del refrigerante", FaultSeverity.MEDIUM),
        "P0116" to Entry("Rango o rendimiento del sensor de temperatura del refrigerante", FaultSeverity.MEDIUM),
        "P0117" to Entry("Señal baja del sensor de temperatura del refrigerante", FaultSeverity.MEDIUM),
        "P0118" to Entry("Señal alta del sensor de temperatura del refrigerante", FaultSeverity.MEDIUM),
        "P0120" to Entry("Circuito del sensor de posición del acelerador", FaultSeverity.MEDIUM),
        "P0130" to Entry("Circuito del sensor de oxígeno (banco 1, sensor 1)", FaultSeverity.LOW),
        "P0171" to Entry("Mezcla demasiado pobre (banco 1)", FaultSeverity.MEDIUM),
        "P0172" to Entry("Mezcla demasiado rica (banco 1)", FaultSeverity.MEDIUM),
        "P0174" to Entry("Mezcla demasiado pobre (banco 2)", FaultSeverity.MEDIUM),
        "P0175" to Entry("Mezcla demasiado rica (banco 2)", FaultSeverity.MEDIUM),
        "P0217" to Entry("Sobrecalentamiento del motor", FaultSeverity.CRITICAL),
        "P0300" to Entry("Falla de encendido aleatoria o en varios cilindros", FaultSeverity.CRITICAL),
        "P0325" to Entry("Circuito del sensor de detonación", FaultSeverity.MEDIUM),
        "P0335" to Entry("Circuito del sensor de posición del cigüeñal", FaultSeverity.CRITICAL),
        "P0340" to Entry("Circuito del sensor de posición del árbol de levas", FaultSeverity.MEDIUM),
        "P0401" to Entry("Flujo insuficiente de recirculación de gases (EGR)", FaultSeverity.LOW),
        "P0420" to Entry("Eficiencia del catalizador bajo el umbral (banco 1)", FaultSeverity.MEDIUM),
        "P0430" to Entry("Eficiencia del catalizador bajo el umbral (banco 2)", FaultSeverity.MEDIUM),
        "P0440" to Entry("Sistema de control de emisiones evaporativas", FaultSeverity.LOW),
        "P0442" to Entry("Fuga pequeña en el sistema de emisiones evaporativas", FaultSeverity.LOW),
        "P0455" to Entry("Fuga grande en el sistema de emisiones evaporativas", FaultSeverity.LOW),
        "P0500" to Entry("Sensor de velocidad del vehículo", FaultSeverity.MEDIUM),
        "P0505" to Entry("Sistema de control de ralentí", FaultSeverity.MEDIUM),
        "P0520" to Entry("Circuito del sensor de presión de aceite", FaultSeverity.MEDIUM),
        "P0524" to Entry("Presión de aceite del motor demasiado baja", FaultSeverity.CRITICAL),
        "P0562" to Entry("Voltaje del sistema bajo", FaultSeverity.MEDIUM),
        "P0563" to Entry("Voltaje del sistema alto", FaultSeverity.MEDIUM),
        "P0600" to Entry("Falla en el enlace de comunicación serial", FaultSeverity.MEDIUM),
        "P0700" to Entry("Falla en el sistema de control de la transmisión", FaultSeverity.MEDIUM),
        "U0100" to Entry("Comunicación perdida con el módulo de control del motor (ECM/PCM)", FaultSeverity.CRITICAL)
    )

    private val MISFIRE = Regex("^P030[1-9]$|^P031[0-2]$")

    fun describe(dtc: Dtc): Entry {
        KNOWN[dtc.code]?.let { return it }
        if (MISFIRE.matches(dtc.code)) {
            return Entry("Falla de encendido en el cilindro ${dtc.code.takeLast(2).trimStart('0')}", FaultSeverity.CRITICAL)
        }
        return Entry("${familyName(dtc)} ${dtc.code}", familySeverity(dtc))
    }

    private fun familyName(dtc: Dtc) = when (dtc.code.first()) {
        'P' -> "Falla de tren motriz"
        'C' -> "Falla de chasis"
        'B' -> "Falla de carrocería"
        else -> "Falla de red de comunicación"
    }

    /** Emisiones (P04xx) y carrocería (Bxxxx) no comprometen la marcha; el resto, `MEDIUM` por defecto. */
    private fun familySeverity(dtc: Dtc) = when {
        dtc.code.startsWith("P04") -> FaultSeverity.LOW
        dtc.code.startsWith("B") -> FaultSeverity.LOW
        else -> FaultSeverity.MEDIUM
    }
}
