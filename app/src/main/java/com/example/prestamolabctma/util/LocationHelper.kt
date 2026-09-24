package com.example.prestamolabctma.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

data class UbicacionData(
    val latitud: Double,
    val longitud: Double,
    val timestamp: Long = System.currentTimeMillis()
)

class LocationHelper(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun tienePermisoUbicacion(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation || coarseLocation
    }

    @SuppressLint("MissingPermission")
    suspend fun obtenerUbicacionPuntual(timeoutMs: Long = 5000L): Result<UbicacionData> {
        if (!tienePermisoUbicacion()) {
            return Result.failure(SecurityException("Permiso de ubicación no concedido"))
        }

        return try {
            val ubicacion = withTimeoutOrNull(timeoutMs) {
                suspendCancellableCoroutine<Location?> { continuation ->
                    val cancellationTokenSource = CancellationTokenSource()
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        cancellationTokenSource.token
                    ).addOnSuccessListener { location ->
                        if (location != null) {
                            continuation.resume(location)
                        } else {
                            // Intentar la última ubicación conocida
                            fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                                continuation.resume(lastLoc)
                            }.addOnFailureListener {
                                continuation.resume(null)
                            }
                        }
                    }.addOnFailureListener {
                        continuation.resume(null)
                    }

                    continuation.invokeOnCancellation {
                        cancellationTokenSource.cancel()
                    }
                }
            }

            if (ubicacion != null) {
                Result.success(
                    UbicacionData(
                        latitud = ubicacion.latitude,
                        longitud = ubicacion.longitude,
                        timestamp = ubicacion.time
                    )
                )
            } else {
                Result.failure(Exception("No se pudo obtener la ubicación GPS a tiempo"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
