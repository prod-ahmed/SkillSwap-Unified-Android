package com.skillswap.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.model.User
import com.skillswap.model.UserLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.google.gson.Gson

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferences = application.getSharedPreferences("SkillSwapPrefs", Context.MODE_PRIVATE)

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _cities = MutableStateFlow<List<String>>(emptyList())
    val cities: StateFlow<List<String>> = _cities.asStateFlow()

    fun loadProfile() {
        val token = sharedPreferences.getString("auth_token", null)
        val username = sharedPreferences.getString("username", "Utilisateur") ?: "Utilisateur"
        val id = sharedPreferences.getString("user_id", "") ?: ""

        if (token == null) {
            _user.value = buildFallbackUser(id, username)
            return
        }

        _isLoading.value = true
        _errorMessage.value = null
        _successMessage.value = null

        viewModelScope.launch {
            try {
                val fetchedUser = com.skillswap.network.NetworkService.api.getMe("Bearer $token")
                _user.value = fetchedUser
                // Save to preferences
                sharedPreferences.edit().apply {
                    putString("username", fetchedUser.username)
                    putString("user_email", fetchedUser.email)
                    apply()
                }
            } catch (e: Exception) {
                _user.value = buildFallbackUser(id, username)
                _errorMessage.value = e.message ?: "Erreur de chargement du profil"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadCities() {
        viewModelScope.launch {
            try {
                val response = com.skillswap.network.NetworkService.api.getCities()
                _cities.value = response
            } catch (e: Exception) {
                // Use default cities if fetch fails
                _cities.value = listOf(
                    "Tunis", "Sfax", "Sousse", "Kairouan", "Bizerte",
                    "Gabès", "Ariana", "Gafsa", "Monastir", "Ben Arous"
                )
            }
        }
    }

    fun updateProfile(
        username: String,
        location: String?,
        skillsTeach: List<String>,
        skillsLearn: List<String>
    ) {
        val token = sharedPreferences.getString("auth_token", null)
        if (token == null) {
            _errorMessage.value = "Non authentifié"
            return
        }

        _isSaving.value = true
        _errorMessage.value = null
        _successMessage.value = null

        viewModelScope.launch {
            try {
                val locationPayload = if (!location.isNullOrBlank()) {
                    mapOf(
                        "lat" to null,
                        "lon" to null,
                        "city" to location
                    )
                } else null

                val payload = mutableMapOf<String, Any>(
                    "username" to username,
                    "skillsTeach" to skillsTeach,
                    "skillsLearn" to skillsLearn
                )
                
                if (locationPayload != null) {
                    payload["location"] = locationPayload
                }

                val updatedUser = com.skillswap.network.NetworkService.api.updateProfile(
                    "Bearer $token",
                    payload
                )
                
                _user.value = updatedUser
                _successMessage.value = "Profil mis à jour avec succès"
                
                // Update shared preferences
                sharedPreferences.edit().apply {
                    putString("username", updatedUser.username)
                    apply()
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Erreur lors de la mise à jour"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    private fun buildFallbackUser(id: String, username: String): User {
        return User(
            id = id,
            username = username,
            email = "",
            role = "client",
            credits = 0,
            ratingAvg = 0.0,
            isVerified = false
        )
    }
}
