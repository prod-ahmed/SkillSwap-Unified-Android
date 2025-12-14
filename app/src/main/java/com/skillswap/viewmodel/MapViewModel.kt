package com.skillswap.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.model.User
import com.skillswap.security.SecureStorage
import com.skillswap.network.NetworkService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UserLocationPin(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val city: String?
)

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferences = SecureStorage.getInstance(application)

    private val _pins = MutableStateFlow<List<UserLocationPin>>(emptyList())
    val pins: StateFlow<List<UserLocationPin>> = _pins.asStateFlow()
    private val _cities = MutableStateFlow<List<String>>(emptyList())
    val cities: StateFlow<List<String>> = _cities.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Search functionality
    private val _searchResults = MutableStateFlow<List<UserLocationPin>>(emptyList())
    val searchResults: StateFlow<List<UserLocationPin>> = _searchResults.asStateFlow()
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    private val _selectedLocation = MutableStateFlow<UserLocationPin?>(null)
    val selectedLocation: StateFlow<UserLocationPin?> = _selectedLocation.asStateFlow()

    fun loadPins() {
        val token = sharedPreferences.getString("auth_token", null) ?: return
        viewModelScope.launch {
            try {
                val users = NetworkService.api.getUsers("Bearer $token")
                _pins.value = users.mapNotNull { it.toPin() }
                _cities.value = NetworkService.api.getCities("Bearer $token")
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
    
    fun searchLocation(query: String) {
        if (query.length < 2) {
            _searchResults.value = emptyList()
            return
        }
        _isSearching.value = true
        viewModelScope.launch {
            try {
                // Search within existing pins first
                val localResults = _pins.value.filter { pin ->
                    pin.name.contains(query, ignoreCase = true) ||
                    pin.city?.contains(query, ignoreCase = true) == true
                }
                _searchResults.value = localResults
            } catch (e: Exception) {
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }
    
    fun selectSearchResult(result: UserLocationPin) {
        _selectedLocation.value = result
    }
    
    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }
}

private fun User.toPin(): UserLocationPin? {
    val loc = location ?: return null
    val lat = loc.lat ?: return null
    val lon = loc.lon ?: return null
    return UserLocationPin(
        id = id,
        name = username,
        lat = lat,
        lon = lon,
        city = loc.city
    )
}
