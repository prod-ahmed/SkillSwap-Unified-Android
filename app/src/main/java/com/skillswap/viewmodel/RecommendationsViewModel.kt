package com.skillswap.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.model.Recommendation
import com.skillswap.model.User
import com.skillswap.network.NetworkService
import com.skillswap.util.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecommendationsViewModel(application: Application) : AndroidViewModel(application) {
    private val api = NetworkService.api
    private val tokenManager = TokenManager(application)
    
    private val _recommendations = MutableStateFlow<List<Recommendation>>(emptyList())
    val recommendations: StateFlow<List<Recommendation>> = _recommendations.asStateFlow()
    
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
                val token = tokenManager.getToken() ?: return@launch
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
                val token = tokenManager.getToken()
                if (token.isNullOrBlank()) {
                    _error.value = "Not authenticated"
                    _isLoading.value = false
                    return@launch
                }
                
                val response = api.getSessionRecommendations("Bearer $token")
                _recommendations.value = response.data
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
}
