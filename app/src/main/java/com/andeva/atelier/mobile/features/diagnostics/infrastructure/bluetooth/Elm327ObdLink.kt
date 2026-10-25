package com.andeva.atelier.mobile.features.diagnostics.infrastructure.bluetooth

import com.andeva.atelier.mobile.features.diagnostics.domain.FreezeFrame
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdAdapterUnreachableException
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdConnectionInfo
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdConnectionLostException
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdLink
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdNoResponseException
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdNotConnectedException
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdResponseParser
import com.andeva.atelier.mobile.features.diagnostics.domain.TelemetrySample
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.time.Clock
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Protocolo ELM327 (docs/04-mobile-integration.md §4) sobre un [ObdTransport]:
 * `ATZ ATE0 ATL0 ATS0 ATH0 ATSP0` → `0100` (¿responde la ECU?) → `ATDPN` (protocolo) → `0902` (VIN).
 * Un `IOException` del canal a mitad de la sesión es [ObdConnectionLostException].
 */
@Singleton
class Elm327ObdLink @Inject constructor(
    private val transport: ObdTransport,
    private val clock: Clock
) : ObdLink {

    @Volatile private var lastAddress: String? = null

    override val isConnected: Boolean get() = transport.isOpen

    override suspend fun connect(address: String): Result<ObdConnectionInfo> {
        lastAddress = address
        return try {
            transport.open(address)
            send("ATZ", RESET_TIMEOUT_MILLIS)
            INIT_COMMANDS.forEach { send(it) }
            // Con ATSP0 el primer pedido busca el protocolo: puede tardar varios segundos.
            val probe = send("0100", SEARCH_TIMEOUT_MILLIS)
            if (ObdResponseParser.isNoResponse(probe) || ObdResponseParser.data(probe, "4100") == null) {
                transport.close()
                return Result.failure(ObdNoResponseException())
            }
            val protocol = ObdResponseParser.protocol(send("ATDPN"))
            // El VIN es opcional: varias ECU anteriores a CAN no lo informan o tardan demasiado.
            val vin = try {
                ObdResponseParser.vin(send("0902", VIN_TIMEOUT_MILLIS))
            } catch (_: IOException) {
                null
            }
            Result.success(ObdConnectionInfo(protocol, vin))
        } catch (e: CancellationException) {
            transport.close()
            throw e
        } catch (e: IOException) {
            transport.close()
            Result.failure(ObdAdapterUnreachableException(e))
        } catch (e: Exception) {
            transport.close()
            Result.failure(e)
        }
    }

    override suspend fun reconnect(): Result<ObdConnectionInfo> {
        val address = lastAddress ?: return Result.failure(ObdNotConnectedException())
        transport.close()
        return connect(address)
    }

    /** `NO DATA` en el modo 03 = sin códigos (US17 E2); otro error = la ECU no contestó. */
    override suspend fun readDtcs() = query {
        val raw = send("03")
        when {
            ObdResponseParser.isNoData(raw) -> emptyList()
            ObdResponseParser.isNoResponse(raw) -> throw ObdNoResponseException()
            else -> ObdResponseParser.dtcs(raw)
        }
    }

    override suspend fun readFreezeFrame() = query {
        val dtc = ObdResponseParser.freezeFrameDtc(send("0202")) ?: return@query null
        val rpm = ObdResponseParser.freezeFrameData(send("020C00"), "0C")?.takeIf { it.size >= 2 }?.let { (it[0] * 256 + it[1]) / 4 }
        FreezeFrame(
            dtc = dtc,
            engineTempCelsius = ObdResponseParser.freezeFrameData(send("020500"), "05")?.firstOrNull()?.let { it - 40.0 },
            engineRpm = rpm,
            speedKmh = ObdResponseParser.freezeFrameData(send("020D00"), "0D")?.firstOrNull()
        )
    }

    /** La ECU confirma el borrado con `44`. */
    override suspend fun clearDtcs() = query {
        val raw = send("04", CLEAR_TIMEOUT_MILLIS)
        if (ObdResponseParser.data(raw, "44") == null) throw ObdNoResponseException()
    }

    /** Un PID sin soporte (`NO DATA`) queda en null; la muestra sin temperatura no se envía. */
    override suspend fun readSample() = query {
        TelemetrySample(
            timestamp = clock.instant().truncatedTo(ChronoUnit.MILLIS),
            engineTempCelsius = ObdResponseParser.coolantTemp(send("0105")),
            engineRpm = ObdResponseParser.engineRpm(send("010C")),
            speedKmh = ObdResponseParser.speed(send("010D")),
            batteryVoltage = ObdResponseParser.voltage(send("ATRV")),
            fuelPercentage = ObdResponseParser.fuelLevel(send("012F")),
            intakePressureKpa = ObdResponseParser.intakePressure(send("010B"))
        )
    }

    override fun close() {
        transport.close()
    }

    private suspend fun send(command: String, timeoutMillis: Long = COMMAND_TIMEOUT_MILLIS) =
        transport.command(command, timeoutMillis)

    private suspend fun <T> query(block: suspend () -> T): Result<T> {
        if (!transport.isOpen) return Result.failure(ObdNotConnectedException())
        return try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            transport.close()
            Result.failure(ObdConnectionLostException(e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private companion object {
        val INIT_COMMANDS = listOf("ATE0", "ATL0", "ATS0", "ATH0", "ATSP0")
        const val COMMAND_TIMEOUT_MILLIS = 3_000L
        const val RESET_TIMEOUT_MILLIS = 5_000L
        const val SEARCH_TIMEOUT_MILLIS = 15_000L
        const val VIN_TIMEOUT_MILLIS = 5_000L
        const val CLEAR_TIMEOUT_MILLIS = 5_000L
    }
}
