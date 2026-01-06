package com.skillswap.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.skillswap.auth.AuthenticationManager
import com.skillswap.auth.GoogleSignInHelper
import com.skillswap.model.ForgotPasswordRequest
import com.skillswap.model.ReferralPreview
import com.skillswap.network.NetworkService
import com.skillswap.security.SecureStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

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
    private val authManager = AuthenticationManager.getInstance(application)
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
        if (authManager.hasValidSession()) {
            onValid()
        } else {
            logout()
            onInvalid()
        }
    }

    fun logout() {
        authManager.logout()
        sharedPreferences.edit().apply {
            remove("auth_token")
            remove("user_id")
            remove("username")
        }.apply()
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
                val email = _email.value.trim().lowercase()
                val body = mapOf("email" to email, "password" to _password.value)
                android.util.Log.d("AuthViewModel", "Login attempt with email: $email")
                val response = NetworkService.api.login(body)

                val token = response.accessToken
                if (response.user != null && token != null) {
                    saveSession(token, response.user)
                    onSuccess()
                } else {
                    _errorMessage.value = response.message ?: "Login failed"
                }
            } catch (e: retrofit2.HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                android.util.Log.e("AuthViewModel", "Login HTTP error ${e.code()}: $errorBody")
                _errorMessage.value = when (e.code()) {
                    401 -> "Email ou mot de passe incorrect"
                    400 -> "Requête invalide: $errorBody"
                    else -> "Erreur serveur: ${e.code()}"
                }
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Login error", e)
                _errorMessage.value = "Erreur: ${e.message}"
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
                val textMediaType = "text/plain".toMediaType()
                val usernameBody = _fullName.value.trim().toRequestBody(textMediaType)
                val emailBody = _email.value.trim().lowercase().toRequestBody(textMediaType)
                val passwordBody = _password.value.toRequestBody(textMediaType)
                
                // Only include referral code if it's exactly 5 characters (backend requirement)
                val referralCode = _referralCode.value.trim().uppercase()
                val referralBody = if (referralCode.length == 5) {
                    referralCode.toRequestBody(textMediaType)
                } else null
                
                android.util.Log.d("AuthViewModel", "Register attempt - username: ${_fullName.value}, email: ${_email.value}, referralCode: ${if (referralBody != null) referralCode else "null"}")
                
                // First register the user (returns User object)
                NetworkService.api.register(
                    username = usernameBody,
                    email = emailBody,
                    password = passwordBody,
                    referralCode = referralBody
                )
                
                android.util.Log.d("AuthViewModel", "Registration successful, now logging in...")
                
                // Then login to get the token (like iOS does)
                val loginBody = mapOf("email" to _email.value.trim().lowercase(), "password" to _password.value)
                val loginResponse = NetworkService.api.login(loginBody)
                
                val token = loginResponse.accessToken
                if (loginResponse.user != null && token != null) {
                    saveSession(token, loginResponse.user)
                    onSuccess()
                } else {
                    _errorMessage.value = loginResponse.message ?: "Inscription échouée"
                }
            } catch (e: retrofit2.HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                android.util.Log.e("AuthViewModel", "Register HTTP error ${e.code()}: $errorBody")
                _errorMessage.value = when {
                    e.code() == 400 && errorBody?.contains("Email already in use") == true -> 
                        "Cet email est déjà utilisé"
                    e.code() == 400 && errorBody?.contains("Referral") == true -> 
                        "Code de parrainage invalide"
                    e.code() == 400 && errorBody?.contains("email must be an email") == true ->
                        "Format d'email invalide"
                    e.code() == 400 && errorBody?.contains("password") == true ->
                        "Le mot de passe est requis"
                    e.code() == 400 && errorBody?.contains("username") == true ->
                        "Le nom d'utilisateur est requis"
                    else -> "Inscription échouée: $errorBody"
                }
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Register error", e)
                _errorMessage.value = "Erreur: ${e.message}"
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
                val idToken = account.idToken
                if (idToken.isNullOrEmpty()) {
                    _errorMessage.value = "Google ID token missing"
                    return@launch
                }
                
                // Use the proper /auth/google endpoint
                val payload = mapOf<String, String?>(
                    "idToken" to idToken,
                    "referralCode" to _referralCode.value.takeIf { it.isNotBlank() }?.uppercase()
                )
                
                val response = NetworkService.api.googleAuth(payload)
                
                // Convert GoogleAuthUser to User for session storage
                val user = com.skillswap.model.User(
                    id = response.user.id,
                    username = response.user.username,
                    email = response.user.email,
                    role = "client",
                    credits = null,
                    ratingAvg = null,
                    isVerified = null,
                    image = response.user.profileImageUrl
                )
                
                saveSession(response.accessToken, user)
                onSuccess()
            } catch (e: retrofit2.HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                _errorMessage.value = "Google auth failed: ${errorBody ?: e.message()}"
            } catch (e: Exception) {
                _errorMessage.value = "Google auth error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun saveSession(token: String, user: com.skillswap.model.User) {
        authManager.setToken(token)
        authManager.setUser(user)
        with(sharedPreferences.edit()) {
            putString("auth_token", token)
            putString("user_id", user.id)
            putString("username", user.username)
            apply()
        }
    }
}
