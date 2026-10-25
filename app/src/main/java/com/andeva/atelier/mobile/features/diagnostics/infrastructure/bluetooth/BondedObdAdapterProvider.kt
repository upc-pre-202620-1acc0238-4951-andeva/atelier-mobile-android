package com.andeva.atelier.mobile.features.diagnostics.infrastructure.bluetooth

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.andeva.atelier.mobile.features.diagnostics.domain.BluetoothDisabledException
import com.andeva.atelier.mobile.features.diagnostics.domain.BluetoothPermissionDeniedException
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdAdapter
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdAdapterProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Dispositivos ya emparejados en los ajustes de Android. Sin escaneo, así no hace falta `BLUETOOTH_SCAN`. */
class BondedObdAdapterProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : ObdAdapterProvider {

    @Suppress("MissingPermission")    // revisado antes de leer los emparejados
    override fun pairedAdapters(): Result<List<ObdAdapter>> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.failure(BluetoothPermissionDeniedException())
        }
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
        if (adapter == null || !adapter.isEnabled) return Result.failure(BluetoothDisabledException())
        return try {
            Result.success(adapter.bondedDevices.orEmpty().map { ObdAdapter(it.name.orEmpty(), it.address) }.sortedBy { it.name })
        } catch (_: SecurityException) {
            Result.failure(BluetoothPermissionDeniedException())
        }
    }
}
