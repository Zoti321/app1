package com.example.mynativeapp1.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
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

class FusedDeviceLocationProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : DeviceLocationProvider {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentCoordinates(): Result<Coordinates> {
        if (!isDeviceLocationEnabled()) {
            return Result.failure(LocationServicesDisabledException())
        }
        fetchCurrentLocation()?.let { return Result.success(it) }
        return fetchLastKnownLocation()
    }

    @SuppressLint("MissingPermission")
    private suspend fun fetchCurrentLocation(): Coordinates? =
        suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()
            continuation.invokeOnCancellation { cancellationTokenSource.cancel() }
            client.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token,
            ).addOnCompleteListener { task ->
                if (continuation.isCancelled) return@addOnCompleteListener
                val location = if (task.isSuccessful) task.result else null
                continuation.resume(location?.toCoordinates())
            }
        }

    @SuppressLint("MissingPermission")
    private suspend fun fetchLastKnownLocation(): Result<Coordinates> =
        suspendCancellableCoroutine { continuation ->
            client.lastLocation.addOnCompleteListener { task ->
                if (continuation.isCancelled) return@addOnCompleteListener
                val location = if (task.isSuccessful) task.result else null
                if (location != null) {
                    continuation.resume(Result.success(location.toCoordinates()))
                } else {
                    continuation.resume(Result.failure(LocationUnavailableException()))
                }
            }
        }

    private fun isDeviceLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            @Suppress("DEPRECATION")
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    }

    private fun android.location.Location.toCoordinates() = Coordinates(
        latitude = latitude,
        longitude = longitude,
    )
}

class AndroidPlaceNameResolver @Inject constructor(
    @param:ApplicationContext context: Context,
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

class LocationServicesDisabledException : Exception()
