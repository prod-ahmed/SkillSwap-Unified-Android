package com.skillswap.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.model.Recommendation
import com.skillswap.model.User
import com.skillswap.network.NetworkService
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecommendationsViewModel(application: Application) : AndroidViewModel(application) {
    private val api = NetworkService.api
    private val sharedPreferences = application.getSharedPreferences("SkillSwapPrefs", Context.MODE_PRIVATE)
    
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
                
                val response = api.getSessionRecommendations("Bearer $token")
                _recommendations.value = response.data
                generateCoordinates(response.data)
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

    private fun generateCoordinates(items: List<Recommendation>) {
        val baseLat = 36.8065
        val baseLng = 10.1815
        val coords = mutableMapOf<String, LatLng>()
        
        items.forEachIndexed { index, item ->
            // Simple offset logic to scatter them around Tunis
            val angle = (index * (2 * Math.PI / items.size.coerceAtLeast(1)))
            val radius = 0.02 + (index % 3) * 0.01 // roughly 2-5km
            
            val lat = baseLat + radius * Math.cos(angle)
            val lng = baseLng + radius * Math.sin(angle)
            
            coords[item.id] = LatLng(lat, lng)
        }
        _recommendationCoordinates.value = coords
    }
}
