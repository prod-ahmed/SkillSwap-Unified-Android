package com.skillswap.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val city: String? = null
)

class LocationManager private constructor(private val context: Context) {
    
    companion object {
        @Volatile
        private var instance: LocationManager? = null
        private const val TAG = "LocationManager"
        
        fun getInstance(context: Context): LocationManager {
            return instance ?: synchronized(this) {
                instance ?: LocationManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    
    private val _currentLocation = MutableStateFlow<LocationData?>(null)
    val currentLocation: StateFlow<LocationData?> = _currentLocation.asStateFlow()
    
    private val _locationPermissionGranted = MutableStateFlow(false)
    val locationPermissionGranted: StateFlow<Boolean> = _locationPermissionGranted.asStateFlow()
    
    private var locationCallback: LocationCallback? = null
    
    init {
        checkLocationPermission()
    }
    
    fun checkLocationPermission(): Boolean {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        _locationPermissionGranted.value = granted
        return granted
    }
    
    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }
    
    suspend fun getCurrentLocation(): LocationData? {
        if (!checkLocationPermission()) {
            Log.w(TAG, "Location permission not granted")
            return null
        }
        
        try {
            val location = fusedLocationClient.lastLocation.await()
            return location?.let { loc ->
                val address = getAddressFromLocation(loc.latitude, loc.longitude)
                LocationData(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    address = address?.getAddressLine(0),
                    city = address?.locality
                ).also {
                    _currentLocation.value = it
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location", e)
            return null
        }
    }
    
    fun startLocationUpdates() {
        if (!checkLocationPermission()) {
            Log.w(TAG, "Location permission not granted")
            return
        }
        
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000L // 10 seconds
        ).apply {
            setMinUpdateIntervalMillis(5000L) // 5 seconds
        }.build()
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    val address = getAddressFromLocation(location.latitude, location.longitude)
                    _currentLocation.value = LocationData(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        address = address?.getAddressLine(0),
                        city = address?.locality
                    )
                    Log.d(TAG, "Location updated: ${location.latitude}, ${location.longitude}")
                }
            }
        }
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                null
            )
            Log.d(TAG, "Location updates started")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception starting location updates", e)
        }
    }
    
    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
            Log.d(TAG, "Location updates stopped")
        }
    }
    
    private fun getAddressFromLocation(latitude: Double, longitude: Double): android.location.Address? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // For Android 13+, would need to use async API
                null
            } else {
                @Suppress("DEPRECATION")
                val geocoder = Geocoder(context, Locale.getDefault())
                geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error geocoding location", e)
            null
        }
    }
    
    suspend fun getLocationFromAddress(address: String): LocationData? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // For Android 13+, would need to use async API
                null
            } else {
                @Suppress("DEPRECATION")
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocationName(address, 1)
                addresses?.firstOrNull()?.let { addr ->
                    LocationData(
                        latitude = addr.latitude,
                        longitude = addr.longitude,
                        address = addr.getAddressLine(0),
                        city = addr.locality
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding location from address", e)
            null
        }
    }
}

// Extension function to convert Tasks to coroutines
private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T? {
    return try {
        kotlinx.coroutines.tasks.await(this)
    } catch (e: Exception) {
        Log.e("LocationManager", "Task failed", e)
        null
    }
}
