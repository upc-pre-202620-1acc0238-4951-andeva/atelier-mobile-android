package com.andeva.atelier.mobile.features.diagnostics.domain

/**
 * Decodifica las respuestas del ELM327 (docs/04-mobile-integration.md §4). Con `ATE0 ATL0 ATS0 ATH0` las respuestas
 * llegan sin eco, sin espacios y sin cabeceras; en CAN, un mensaje de varias tramas llega como `0:…`, `1:…` precedido
 * de su longitud (`00A`).
 */
object ObdResponseParser {
    private val FRAME = Regex("^[0-9A-F]:([0-9A-F]+)$")
    private val HEX = Regex("^(?:[0-9A-F]{2})+$")
    private val VOLTAGE = Regex("^(\\d+(?:\\.\\d+)?)V$")
    private val VIN_CHAR = Regex("[A-HJ-NPR-Z0-9]")
    private const val VIN_LENGTH = 17

    /** Respuestas del ELM327 que significan "la computadora del vehículo no contestó". */
    private val NO_RESPONSE = listOf("NODATA", "UNABLETOCONNECT", "CANERROR", "BUSINIT", "BUSERROR", "BUSBUSY", "STOPPED", "ERROR", "?")

    private val PROTOCOLS = mapOf(
        "1" to "SAE J1850 PWM",
        "2" to "SAE J1850 VPW",
        "3" to "ISO 9141-2",
        "4" to "ISO 14230-4 KWP (5 baud)",
        "5" to "ISO 14230-4 KWP (fast init)",
        "6" to "ISO 15765-4 CAN (11 bit, 500 kbaud)",
        "7" to "ISO 15765-4 CAN (29 bit, 500 kbaud)",
        "8" to "ISO 15765-4 CAN (11 bit, 250 kbaud)",
        "9" to "ISO 15765-4 CAN (29 bit, 250 kbaud)",
        "A" to "SAE J1939 CAN"
    )

    /** Un mensaje de la ECU; [isMultiFrame] = llegó en varias tramas CAN (trae byte de cantidad y relleno). */
    private data class Message(val hex: String, val isMultiFrame: Boolean)

    /** Líneas útiles: en mayúsculas, sin espacios, sin el prompt `>` y sin `SEARCHING...`. */
    fun lines(raw: String): List<String> = raw.replace(">", "")
        .split('\r', '\n')
        .map { it.replace(" ", "").trim().uppercase() }
        .filter { it.isNotEmpty() && !it.startsWith("SEARCHING") }

    fun isOk(raw: String) = lines(raw).any { it == "OK" }

    /** `NO DATA`: la ECU contestó, pero no tiene ese dato (p. ej. modo 03 sin códigos o un PID no soportado). */
    fun isNoData(raw: String) = lines(raw).any { it == "NODATA" }

    /** Sin líneas o con un error del ELM327 (`NO DATA`, `UNABLE TO CONNECT`, `?`…). */
    fun isNoResponse(raw: String): Boolean {
        val lines = lines(raw)
        return lines.isEmpty() || lines.any { line -> NO_RESPONSE.any { line.contains(it) } }
    }

    private fun messages(raw: String): List<Message> {
        val result = mutableListOf<Message>()
        val frames = StringBuilder()
        for (line in lines(raw)) {
            val frame = FRAME.matchEntire(line)
            when {
                frame != null -> frames.append(frame.groupValues[1])
                HEX.matches(line) -> result += Message(line, isMultiFrame = false)
            }
        }
        if (frames.isNotEmpty()) result += Message(frames.toString(), isMultiFrame = true)
        return result
    }

    private fun bytes(hex: String): List<Int> = hex.chunked(2).filter { it.length == 2 }.map { it.toInt(16) }

    /** Bytes de datos del primer mensaje que empieza con [prefix] (p. ej. `410C`), sin el prefijo. */
    fun data(raw: String, prefix: String): List<Int>? =
        messages(raw).firstOrNull { it.hex.startsWith(prefix) }?.let { bytes(it.hex.substring(prefix.length)) }

    /** Modo 03 (y la re-lectura de US20): códigos almacenados, sin repetir. */
    fun dtcs(raw: String): List<Dtc> = messages(raw)
        .filter { it.hex.startsWith("43") }
        .flatMap { message ->
            val data = bytes(message.hex).drop(1)
            // CAN antepone la cantidad de códigos (y en varias tramas rellena con ceros); los demás protocolos no.
            val pairs = if (message.isMultiFrame || data.size % 2 == 1) {
                data.drop(1).take((data.firstOrNull() ?: 0) * 2)
            } else {
                data
            }
            pairs.chunked(2).filter { it.size == 2 && (it[0] != 0 || it[1] != 0) }.map { decodeDtc(it[0], it[1]) }
        }
        .distinct()

    /** Dos bytes → `P0xxx` / `Cxxxx` / `Bxxxx` / `Uxxxx` (SAE J2012). */
    fun decodeDtc(a: Int, b: Int): Dtc {
        val letter = "PCBU"[(a shr 6) and 0x3]
        val code = "$letter${(a shr 4) and 0x3}${(a and 0xF).toString(16)}${"%02X".format(b and 0xFF)}"
        return checkNotNull(Dtc.of(code))
    }

    /** `0105`: A − 40 °C. */
    fun coolantTemp(raw: String): Double? = data(raw, "4105")?.firstOrNull()?.let { it - 40.0 }

    /** `010C`: (256A + B) / 4 rpm. */
    fun engineRpm(raw: String): Int? = data(raw, "410C")?.takeIf { it.size >= 2 }?.let { (it[0] * 256 + it[1]) / 4 }

    /** `010D`: A km/h. */
    fun speed(raw: String): Int? = data(raw, "410D")?.firstOrNull()

    /** `012F`: A × 100 / 255 %. */
    fun fuelLevel(raw: String): Double? = data(raw, "412F")?.firstOrNull()?.let { it * 100.0 / 255.0 }

    /** `010B`: A kPa (presión absoluta del múltiple de admisión). */
    fun intakePressure(raw: String): Int? = data(raw, "410B")?.firstOrNull()

    /** `ATRV`: "12.6V". */
    fun voltage(raw: String): Double? = lines(raw).firstNotNullOfOrNull { VOLTAGE.matchEntire(it)?.groupValues?.get(1)?.toDouble() }

    /** `0902`: los 17 caracteres del VIN; null si la ECU no lo informa. */
    fun vin(raw: String): String? {
        val text = messages(raw)
            .filter { it.hex.startsWith("4902") }
            .flatMap { bytes(it.hex).drop(3) }             // 49 02 + cantidad (CAN) o secuencia (otros protocolos)
            .map { it.toChar().toString() }
            .filter { VIN_CHAR.matches(it) }
            .joinToString("")
        return text.takeIf { it.length >= VIN_LENGTH }?.takeLast(VIN_LENGTH)
    }

    /** `ATDPN`: "A6" (automático) o "6" → nombre del protocolo. */
    fun protocol(raw: String): String? {
        val code = lines(raw).firstOrNull()?.removePrefix("A") ?: return null
        return PROTOCOLS[code]
    }

    /** Cuadro congelado (modo 02, cuadro 00): los datos van después del número de cuadro. */
    fun freezeFrameData(raw: String, pid: String): List<Int>? = data(raw, "42$pid")?.drop(1)

    fun freezeFrameDtc(raw: String): Dtc? =
        freezeFrameData(raw, "02")?.takeIf { it.size >= 2 && (it[0] != 0 || it[1] != 0) }?.let { decodeDtc(it[0], it[1]) }
}
