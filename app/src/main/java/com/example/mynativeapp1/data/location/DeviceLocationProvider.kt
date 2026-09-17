package com.example.mynativeapp1.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume

data class Coordinates(
    val latitude: Double,
    val longitude: Double,
)

interface DeviceLocationProvider {
    suspend fun getCurrentCoordinates(): Result<Coordinates>
}

interface PlaceNameResolver {
    suspend fun resolveName(latitude: Double, longitude: Double): String?
}

class FusedDeviceLocationProvider(
    context: Context,
) : DeviceLocationProvider {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentCoordinates(): Result<Coordinates> =
        suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()
            continuation.invokeOnCancellation { cancellationTokenSource.cancel() }
            client.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellationTokenSource.token,
            ).addOnSuccessListener { location ->
                if (location == null) {
                    continuation.resume(Result.failure(LocationUnavailableException()))
                } else {
                    continuation.resume(
                        Result.success(
                            Coordinates(
                                latitude = location.latitude,
                                longitude = location.longitude,
                            ),
                        ),
                    )
                }
            }.addOnFailureListener { error ->
                continuation.resume(Result.failure(error))
            }
        }
}

class AndroidPlaceNameResolver(
    context: Context,
) : PlaceNameResolver {
    private val geocoder = Geocoder(context, Locale.CHINA)

    @Suppress("DEPRECATION")
    override suspend fun resolveName(latitude: Double, longitude: Double): String? {
        if (!Geocoder.isPresent()) return null
        return try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            addresses?.firstOrNull()?.locality
                ?: addresses?.firstOrNull()?.subAdminArea
                ?: addresses?.firstOrNull()?.adminArea
        } catch (_: IOException) {
            null
        }
    }
}

class LocationUnavailableException : Exception()
