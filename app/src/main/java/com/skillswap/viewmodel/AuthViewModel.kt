package com.skillswap.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.model.ForgotPasswordRequest
import com.skillswap.network.NetworkService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()
    
    // Register fields
    private val _fullName = MutableStateFlow("")
    val fullName: StateFlow<String> = _fullName.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val sharedPreferences = application.getSharedPreferences("SkillSwapPrefs", Context.MODE_PRIVATE)

    fun onEmailChange(newValue: String) { _email.value = newValue }
    fun onPasswordChange(newValue: String) { _password.value = newValue }
    fun onFullNameChange(newValue: String) { _fullName.value = newValue }

    fun login(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val body = mapOf("email" to _email.value, "password" to _password.value)
                val response = NetworkService.api.login(body)

                val token = response.accessToken
                if (response.user != null && token != null) {
                    saveSession(token, response.user)
                    onSuccess()
                } else {
                    _errorMessage.value = response.message ?: "Login failed"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun forgotPassword(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = NetworkService.api.forgotPassword(ForgotPasswordRequest(email = _email.value))
                onResult(response.message ?: "Email envoyé")
            } catch (e: Exception) {
                _errorMessage.value = "Reset échoué: ${e.message}"
                onResult(null)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Basic register implementation (without image upload for now)
    fun register(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val payload: Map<String, String> = mapOf(
                    "username" to _fullName.value,
                    "email" to _email.value,
                    "password" to _password.value,
                    "role" to "client"
                )
                val response = NetworkService.api.register(payload)
                val token = response.accessToken
                if (response.user != null && token != null) {
                    saveSession(token, response.user)
                    onSuccess()
                } else {
                    _errorMessage.value = response.message ?: "Inscription échouée"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Register failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun saveSession(token: String, user: com.skillswap.model.User) {
        with(sharedPreferences.edit()) {
            putString("auth_token", token)
            putString("user_id", user.id)
            putString("username", user.username)
            apply()
        }
    }
}
