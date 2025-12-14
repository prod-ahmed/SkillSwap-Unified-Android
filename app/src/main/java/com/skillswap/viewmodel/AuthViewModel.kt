package com.skillswap.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.skillswap.auth.GoogleSignInHelper
import com.skillswap.model.ForgotPasswordRequest
import com.skillswap.model.ReferralPreview
import com.skillswap.network.NetworkService
import com.skillswap.security.SecureStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Base64
import org.json.JSONObject

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

    private val _referralPreview = MutableStateFlow<ReferralPreview?>(null)
    val referralPreview: StateFlow<ReferralPreview?> = _referralPreview.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val sharedPreferences = SecureStorage.getInstance(application)
    val googleSignInHelper = GoogleSignInHelper(application.applicationContext)

    fun onEmailChange(newValue: String) { _email.value = newValue }
    fun onPasswordChange(newValue: String) { _password.value = newValue }
    fun onFullNameChange(newValue: String) { _fullName.value = newValue }
    fun onReferralCodeChange(newValue: String) { 
        _referralCode.value = newValue 
        if (newValue.isBlank()) _referralPreview.value = null
    }

    fun validateReferralCode() {
        val code = _referralCode.value
        if (code.isBlank()) return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val preview = NetworkService.api.validateReferral(mapOf("codeParainnage" to code))
                _referralPreview.value = preview
                _errorMessage.value = null // Clear error if valid
            } catch (e: Exception) {
                _referralPreview.value = null
                _errorMessage.value = "Code de parrainage invalide"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun checkSession(onValid: () -> Unit, onInvalid: () -> Unit) {
        val token = sharedPreferences.getString("auth_token", null)
        if (token != null && !isTokenExpired(token)) {
            onValid()
        } else {
            logout()
            onInvalid()
        }
    }

    private fun isTokenExpired(token: String): Boolean {
        try {
            val parts = token.split(".")
            if (parts.size != 3) return true
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
            val json = JSONObject(payload)
            val exp = json.optLong("exp")
            if (exp == 0L) return false
            return System.currentTimeMillis() / 1000 >= exp
        } catch (e: Exception) {
            return true
        }
    }

    fun logout() {
        sharedPreferences.edit().clear().apply()
        _email.value = ""
        _password.value = ""
        _fullName.value = ""
        _referralCode.value = ""
        _referralPreview.value = null
    }

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
