package com.skillswap.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.model.Recommendation
import com.skillswap.security.SecureStorage
import com.skillswap.model.User
import com.skillswap.network.NetworkService
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecommendationsViewModel(application: Application) : AndroidViewModel(application) {
    private val api = NetworkService.api
    private val sharedPreferences = SecureStorage.getInstance(application)
    
    private val _recommendations = MutableStateFlow<List<Recommendation>>(emptyList())
    val recommendations: StateFlow<List<Recommendation>> = _recommendations.asStateFlow()
    
    private val _recommendationCoordinates = MutableStateFlow<Map<String, LatLng>>(emptyMap())
    val recommendationCoordinates: StateFlow<Map<String, LatLng>> = _recommendationCoordinates.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val locationManager = com.skillswap.services.LocationManager.getInstance(application)

    init {
        loadCurrentUser()
    }
    
    private fun loadCurrentUser() {
        viewModelScope.launch {
            try {
                val token = sharedPreferences.getString("auth_token", null) ?: return@launch
                val user = api.getMe("Bearer $token")
                _currentUser.value = user
            } catch (e: Exception) {
                // Silently fail - user will be null
            }
        }
    }
    
    fun loadRecommendations() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val token = sharedPreferences.getString("auth_token", null)
                if (token.isNullOrBlank()) {
                    _error.value = "Not authenticated"
                    _isLoading.value = false
                    return@launch
                }
                
                // Get current location for map centering
                val location = locationManager.getCurrentLocation()
                val baseLat = location?.latitude ?: 36.8065
                val baseLng = location?.longitude ?: 10.1815
                
                val response = api.getSessionRecommendations("Bearer $token")
                _recommendations.value = response.data
                generateCoordinates(response.data, baseLat, baseLng)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load recommendations"
                _recommendations.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun refresh() {
        loadRecommendations()
    }

    private fun generateCoordinates(items: List<Recommendation>, baseLat: Double, baseLng: Double) {
        val coords = mutableMapOf<String, LatLng>()
        
        items.forEachIndexed { index, item ->
            // Parse distance to determine radius (e.g., "2.5 km" -> 2.5)
            val distanceKm = try {
                item.distance.split(" ").firstOrNull()?.toDoubleOrNull() ?: (2.0 + (index % 5))
            } catch (e: Exception) {
                2.0 + (index % 5) // fallback: 2-6 km
            }
            
            // Convert km to degrees (rough approximation: 1° ≈ 111 km at equator)
            val radiusDegrees = distanceKm / 111.0
            
            // Scatter around user location in a circle pattern
            val angle = (index * (2 * Math.PI / items.size.coerceAtLeast(1))) + 
                        (Math.random() * 0.5 - 0.25) // Add slight randomness
            
            val lat = baseLat + radiusDegrees * Math.cos(angle)
            val lng = baseLng + radiusDegrees * Math.sin(angle)
            
            coords[item.id] = LatLng(lat, lng)
        }
        _recommendationCoordinates.value = coords
    }
}
