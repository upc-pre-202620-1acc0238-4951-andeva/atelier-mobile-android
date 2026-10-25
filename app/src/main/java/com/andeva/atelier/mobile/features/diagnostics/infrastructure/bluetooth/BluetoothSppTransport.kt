package com.andeva.atelier.mobile.features.diagnostics.infrastructure.bluetooth

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.andeva.atelier.mobile.features.diagnostics.domain.BluetoothDisabledException
import com.andeva.atelier.mobile.features.diagnostics.domain.BluetoothPermissionDeniedException
import com.andeva.atelier.mobile.features.diagnostics.domain.InvalidDiagnosticsInputException
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdAdapterUnreachableException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Socket RFCOMM con el perfil serie (SPP) del ELM327 (docs/04-mobile-integration.md §4). Bluetooth clásico, no BLE.
 * Solo revisa el permiso `BLUETOOTH_CONNECT`; pedirlo es de la UI.
 */
@Singleton
class BluetoothSppTransport @Inject constructor(
    @ApplicationContext private val context: Context
) : ObdTransport {

    @Volatile private var socket: BluetoothSocket? = null

    override val isOpen: Boolean get() = socket?.isConnected == true

    @Suppress("MissingPermission")    // revisado en checkPermission()
    override suspend fun open(address: String) {
        checkPermission()
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
        if (adapter == null || !adapter.isEnabled) throw BluetoothDisabledException()
        val device = try {
            adapter.getRemoteDevice(address)
        } catch (_: IllegalArgumentException) {
            throw InvalidDiagnosticsInputException("address")
        }
        close()
        socket = withContext(Dispatchers.IO) {
            try {
                device.createRfcommSocketToServiceRecord(SPP_UUID).also { it.connect() }
            } catch (e: IOException) {
                throw ObdAdapterUnreachableException(e)
            } catch (_: SecurityException) {
                throw BluetoothPermissionDeniedException()
            }
        }
    }

    /**
     * El `InputStream` de Bluetooth no tiene timeout y su `read` no se puede interrumpir: se lee solo lo disponible,
     * así la espera respeta [timeoutMillis] y la cancelación.
     */
    override suspend fun command(command: String, timeoutMillis: Long): String = withContext(Dispatchers.IO) {
        val current = socket ?: throw IOException("Socket closed")
        val input = current.inputStream
        val output = current.outputStream
        while (input.available() > 0) input.read()              // descarta restos de un comando anterior
        output.write("$command\r".toByteArray(Charsets.US_ASCII))
        output.flush()

        val response = StringBuilder()
        val buffer = ByteArray(BUFFER_SIZE)
        withTimeoutOrNull(timeoutMillis) {
            while (!response.endsWith(PROMPT)) {
                if (input.available() > 0) {
                    val read = input.read(buffer, 0, minOf(buffer.size, input.available()))
                    if (read < 0) throw IOException("Socket closed")
                    response.append(String(buffer, 0, read, Charsets.US_ASCII))
                } else {
                    delay(POLL_MILLIS)
                }
            }
        } ?: throw IOException("Timeout waiting for '$command'")
        response.removeSuffix(PROMPT).toString()
    }

    override fun close() {
        runCatching { socket?.close() }
        socket = null
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        if (!granted) throw BluetoothPermissionDeniedException()
    }

    private companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        const val PROMPT = ">"
        const val BUFFER_SIZE = 512
        const val POLL_MILLIS = 10L
    }
}
