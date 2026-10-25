package com.andeva.atelier.mobile.features.diagnostics.infrastructure.session

import com.andeva.atelier.mobile.features.attendance.domain.DeviceLocation
import com.andeva.atelier.mobile.features.attendance.domain.LocationProvider
import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticSession
import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsRepository
import com.andeva.atelier.mobile.features.diagnostics.domain.LiveTelemetry
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdConnectionInfo
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdConnectionLostException
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdLink
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdLinkState
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdNotConnectedException
import com.andeva.atelier.mobile.features.diagnostics.domain.TelemetrySnapshot
import com.andeva.atelier.mobile.features.diagnostics.domain.TelemetryThresholds
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.bluetooth.ObdForegroundController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Sesión de diagnóstico en bahía (docs/06-offline-sync.md §6, "US16–US19 — OBD-II en foso"):
 * - muestrea cada 2 s (docs/04-mobile-integration.md §4) y guarda en `telemetry_readings` lo que se puede enviar;
 * - cada 200 lecturas arma un `TELEMETRY_BATCH`; al iniciar y al cerrar, también el resto (así salen las lecturas de
 *   una sesión que se cortó sin cerrarse);
 * - ante una caída de señal reintenta [MAX_RECONNECT_ATTEMPTS] veces y, si no vuelve, queda en [ObdLinkState.LOST].
 *
 * El ELM327 es un solo canal serie: el muestreo y los escaneos se turnan con [mutex].
 */
class LiveDiagnosticSession(
    private val link: ObdLink,
    private val repository: DiagnosticsRepository,
    private val locationProvider: LocationProvider,
    private val foreground: ObdForegroundController,
    private val scope: CoroutineScope,
    private val sampleIntervalMillis: Long = SAMPLE_INTERVAL_MILLIS
) : DiagnosticSession {

    private val mutex = Mutex()
    private val _state = MutableStateFlow(LiveTelemetry())
    override val state: StateFlow<LiveTelemetry> = _state.asStateFlow()

    @Volatile
    override var sessionId: String? = null
        private set

    @Volatile private var streamingJob: Job? = null

    /** La ubicación se toma una vez por sesión y es opcional: sin permiso o sin señal las lecturas van sin ella. */
    @Volatile private var location: DeviceLocation? = null

    override suspend fun connect(address: String): Result<ObdConnectionInfo> {
        stopStreaming()
        _state.value = LiveTelemetry(state = ObdLinkState.CONNECTING)
        val result = mutex.withLock { link.connect(address) }
        if (result.isSuccess) {
            sessionId = UUID.randomUUID().toString()
            _state.value = LiveTelemetry(state = ObdLinkState.CONNECTED)
        } else {
            sessionId = null
            _state.value = LiveTelemetry(state = ObdLinkState.DISCONNECTED)
        }
        return result
    }

    override suspend fun startStreaming(vehicleId: String): Result<Unit> {
        if (!link.isConnected) return Result.failure(ObdNotConnectedException())
        stopStreaming()
        repository.purgeConfirmedReadings()
        repository.enqueueUnbatchedReadings(onlyFull = false)
        foreground.start()
        _state.value = LiveTelemetry(state = ObdLinkState.STREAMING, vehicleId = vehicleId)
        streamingJob = scope.launch {
            launch { location = locationProvider.currentLocation().getOrNull() }
            stream(vehicleId)
        }
        return Result.success(Unit)
    }

    private suspend fun stream(vehicleId: String) {
        while (currentCoroutineContext().isActive) {
            val result = mutex.withLock { link.readSample() }
            val sample = result.getOrNull()
            if (sample != null) {
                val coordinates = location
                repository.saveReading(vehicleId, sample, coordinates?.latitude, coordinates?.longitude)
                repository.enqueueUnbatchedReadings(onlyFull = true)
                _state.update { it.copy(state = ObdLinkState.STREAMING, sample = sample, warnings = TelemetryThresholds.warningsFor(sample)) }
            } else if (result.exceptionOrNull() is ObdConnectionLostException || !link.isConnected) {
                if (!recover()) {
                    lose()
                    return
                }
            }
            delay(sampleIntervalMillis)
        }
    }

    private suspend fun recover(): Boolean {
        repeat(MAX_RECONNECT_ATTEMPTS) {
            _state.update { it.copy(state = ObdLinkState.RECONNECTING) }
            if (mutex.withLock { link.reconnect() }.isSuccess) {
                _state.update { it.copy(state = ObdLinkState.STREAMING) }
                return true
            }
            delay(sampleIntervalMillis)
        }
        return false
    }

    /** Sin señal: lo capturado queda en Room y sale por la cola. */
    private suspend fun lose() {
        repository.enqueueUnbatchedReadings(onlyFull = false)
        foreground.stop()
        _state.update { it.copy(state = ObdLinkState.LOST) }
    }

    override suspend fun stopStreaming() {
        val job = streamingJob ?: return
        streamingJob = null
        job.cancelAndJoin()
        repository.enqueueUnbatchedReadings(onlyFull = false)
        foreground.stop()
        _state.update {
            when {
                link.isConnected -> it.copy(state = ObdLinkState.CONNECTED)
                it.state == ObdLinkState.LOST -> it
                else -> it.copy(state = ObdLinkState.DISCONNECTED)
            }
        }
    }

    override suspend fun disconnect() {
        stopStreaming()
        mutex.withLock { link.close() }
        sessionId = null
        location = null
        _state.value = LiveTelemetry(state = ObdLinkState.DISCONNECTED)
    }

    override fun freeze(): TelemetrySnapshot? {
        val current = _state.value
        val vehicleId = current.vehicleId ?: return null
        val sample = current.sample ?: return null
        return TelemetrySnapshot(vehicleId, sample, current.warnings)
    }

    override suspend fun <T> withLink(block: suspend (ObdLink) -> Result<T>): Result<T> {
        if (!link.isConnected) return Result.failure(ObdNotConnectedException())
        return mutex.withLock { block(link) }
    }

    companion object {
        const val SAMPLE_INTERVAL_MILLIS = 2_000L
        const val MAX_RECONNECT_ATTEMPTS = 3
    }
}
