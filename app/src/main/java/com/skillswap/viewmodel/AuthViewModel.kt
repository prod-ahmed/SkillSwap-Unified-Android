package com.skillswap.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.skillswap.auth.GoogleSignInHelper
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

    private val _referralCode = MutableStateFlow("")
    val referralCode: StateFlow<String> = _referralCode.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val sharedPreferences = application.getSharedPreferences("SkillSwapPrefs", Context.MODE_PRIVATE)
    val googleSignInHelper = GoogleSignInHelper(application.applicationContext)

    fun onEmailChange(newValue: String) { _email.value = newValue }
    fun onPasswordChange(newValue: String) { _password.value = newValue }
    fun onFullNameChange(newValue: String) { _fullName.value = newValue }
    fun onReferralCodeChange(newValue: String) { _referralCode.value = newValue }

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
                val payload: MutableMap<String, String> = mutableMapOf(
                    "username" to _fullName.value,
                    "email" to _email.value,
                    "password" to _password.value,
                    "role" to "client"
                )
                if (_referralCode.value.isNotBlank()) {
                    payload["referralCode"] = _referralCode.value
                }
                
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

    fun handleGoogleSignIn(account: GoogleSignInAccount?, onSuccess: () -> Unit) {
        if (account == null) {
            _errorMessage.value = "Google Sign-In failed"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                // Send Google ID token to backend for verification and session creation
                val payload = mapOf(
                    "idToken" to (account.idToken ?: ""),
                    "email" to (account.email ?: ""),
                    "name" to (account.displayName ?: "")
                )
                
                // TODO: Backend needs a /auth/google endpoint
                // For now, use standard registration with Google email
                val response = NetworkService.api.register(
                    mapOf(
                        "username" to (account.displayName ?: account.email ?: "User"),
                        "email" to (account.email ?: ""),
                        "password" to "GOOGLE_AUTH_${account.id}",
                        "role" to "client"
                    )
                )
                
                val token = response.accessToken
                if (response.user != null && token != null) {
                    saveSession(token, response.user)
                    onSuccess()
                } else {
                    _errorMessage.value = response.message ?: "Google Sign-In failed"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Google auth error: ${e.message}"
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
