package com.skillswap.auth

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.google.gson.Gson
import com.skillswap.model.User
import com.skillswap.network.ChatSocketClient
import com.skillswap.security.SecureStorage
import com.skillswap.services.SocketService
import com.skillswap.util.TokenUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthenticationManager private constructor(private val context: Context) {
    private val securePrefs = SecureStorage.getInstance(context)
    private val legacyPrefs = context.getSharedPreferences("SkillSwapPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    val isAuthenticated = mutableStateOf(false)

    init {
        migrateLegacyAuth()
        // Restore auth state from persisted data
        restoreAuthState()
    }

    private fun restoreAuthState() {
        val token = getToken()
        val userJson = securePrefs.getString("cached_user", null)
        
        if (token.isNullOrEmpty() || TokenUtils.isTokenExpired(token)) {
            clearAuthData()
            return
        }

        if (userJson != null) {
            try {
                val user = gson.fromJson(userJson, User::class.java)
                _currentUser.value = user
            } catch (e: Exception) {
                clearCachedUser()
            }
        }

        isAuthenticated.value = true
    }

    fun setUser(user: User?) {
        _currentUser.value = user
        isAuthenticated.value = user != null
        
        // Persist user data
        if (user != null) {
            try {
                val userJson = gson.toJson(user)
                securePrefs.edit().putString("cached_user", userJson).apply()
                user.id?.let { saveUserId(it) }
                // Consider profile complete when core profile fields exist
                val isProfileComplete = !(user.skillsTeach.isNullOrEmpty() && user.skillsLearn.isNullOrEmpty())
                securePrefs.edit().putBoolean("profile_completed", isProfileComplete).apply()
                user.username?.let { securePrefs.edit().putString("username", it).apply() }
                user.email?.let { securePrefs.edit().putString("user_email", it).apply() }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            clearCachedUser()
        }
    }

    fun setToken(token: String) {
        securePrefs.edit().putString("auth_token", token).apply()
        isAuthenticated.value = true
    }

    fun getToken(): String? {
        return securePrefs.getString("auth_token", null)
    }

    fun getUserId(): String? {
        return _currentUser.value?.id ?: securePrefs.getString("user_id", null)
    }

    fun logout() {
        clearAuthData()
    }

    fun hasValidSession(): Boolean {
        val token = getToken()
        val valid = !token.isNullOrEmpty() && !TokenUtils.isTokenExpired(token)
        if (!valid && (!token.isNullOrEmpty() || isAuthenticated.value)) {
            clearAuthData()
        }
        return valid
    }

    private fun clearAuthData() {
        _currentUser.value = null
        isAuthenticated.value = false
        securePrefs.edit().apply {
            remove("auth_token")
            remove("user_id")
            remove("cached_user")
            remove("username")
            remove("user_email")
            putBoolean("profile_completed", false)
        }.apply()

        runCatching { ChatSocketClient.getInstance(context).disconnect() }
        runCatching { SocketService.getInstance(context).disconnect() }
    }

    fun saveUserId(userId: String) {
        securePrefs.edit().putString("user_id", userId).apply()
    }

    fun markProfileComplete() {
        securePrefs.edit().putBoolean("profile_completed", true).apply()
    }
    
    suspend fun refreshUserProfile(fetchProfile: suspend (String) -> User?) {
        val token = getToken()
        if (token != null && !TokenUtils.isTokenExpired(token)) {
            try {
                val user = fetchProfile(token)
                if (user != null) {
                    setUser(user)
                } else {
                    clearAuthData()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            clearAuthData()
        }
    }

    private fun migrateLegacyAuth() {
        val legacyToken = legacyPrefs.getString("auth_token", null)
        val existingToken = securePrefs.getString("auth_token", null)
        if (legacyToken != null && existingToken.isNullOrEmpty()) {
            securePrefs.edit().putString("auth_token", legacyToken).apply()
        }

        legacyPrefs.getString("user_id", null)?.let {
            if (securePrefs.getString("user_id", null).isNullOrEmpty()) {
                securePrefs.edit().putString("user_id", it).apply()
            }
        }

        legacyPrefs.getString("cached_user", null)?.let {
            if (securePrefs.getString("cached_user", null).isNullOrEmpty()) {
                securePrefs.edit().putString("cached_user", it).apply()
            }
        }

        val legacyProfileCompleted = legacyPrefs.getBoolean("profile_completed", false)
        if (legacyProfileCompleted && !securePrefs.getBoolean("profile_completed", false)) {
            securePrefs.edit().putBoolean("profile_completed", true).apply()
        }
        val legacyOnboarding = legacyPrefs.getBoolean("onboarding_done", false)
        if (legacyOnboarding && !securePrefs.getBoolean("onboarding_done", false)) {
            securePrefs.edit().putBoolean("onboarding_done", true).apply()
        }

        // Remove sensitive data from unencrypted prefs
        legacyPrefs.edit()
            .remove("auth_token")
            .remove("user_id")
            .remove("cached_user")
            .remove("onboarding_done")
            .apply()
    }

    private fun clearCachedUser() {
        securePrefs.edit().remove("cached_user").apply()
    }

    companion object {
        @Volatile
        private var instance: AuthenticationManager? = null

        fun getInstance(context: Context): AuthenticationManager {
            return instance ?: synchronized(this) {
                instance ?: AuthenticationManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
