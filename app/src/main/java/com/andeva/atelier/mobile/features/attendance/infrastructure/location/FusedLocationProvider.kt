package com.andeva.atelier.mobile.features.attendance.infrastructure.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.andeva.atelier.mobile.features.attendance.domain.DeviceLocation
import com.andeva.atelier.mobile.features.attendance.domain.LocationPermissionDeniedException
import com.andeva.atelier.mobile.features.attendance.domain.LocationProvider
import com.andeva.atelier.mobile.features.attendance.domain.LocationUnavailableException
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * Ubicación **fresca** de alta precisión (docs/04-mobile-integration.md §7): nunca la última conocida.
 * Solo revisa el permiso; pedirlo con su explicación es trabajo de la UI.
 */
class FusedLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationProvider {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    override suspend fun currentLocation(): Result<DeviceLocation> {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return Result.failure(LocationPermissionDeniedException())

        val locationManager = context.getSystemService(LocationManager::class.java)
        if (locationManager == null || !LocationManagerCompat.isLocationEnabled(locationManager)) {
            return Result.failure(LocationUnavailableException())
        }

        val location = try {
            fetchCurrent()
        } catch (_: SecurityException) {
            return Result.failure(LocationPermissionDeniedException())
        } ?: return Result.failure(LocationUnavailableException())

        return Result.success(
            DeviceLocation(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracyMeters = if (location.hasAccuracy()) location.accuracy else Float.MAX_VALUE,
                isMock = location.isMockLocation()
            )
        )
    }

    @Suppress("MissingPermission")    // revisado en currentLocation()
    private suspend fun fetchCurrent(): Location? = suspendCancellableCoroutine { continuation ->
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(0)
            .setDurationMillis(TIMEOUT_MILLIS)
            .build()
        val cancellation = CancellationTokenSource()
        continuation.invokeOnCancellation { cancellation.cancel() }
        client.getCurrentLocation(request, cancellation.token)
            .addOnSuccessListener { continuation.resume(it) }
            .addOnFailureListener { continuation.resume(null) }
    }

    @Suppress("DEPRECATION")
    private fun Location.isMockLocation(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) isMock else isFromMockProvider

    private companion object {
        const val TIMEOUT_MILLIS = 30_000L
    }
}
