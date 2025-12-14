package com.skillswap.auth

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.google.gson.Gson
import com.skillswap.model.User
import com.skillswap.util.TokenUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthenticationManager private constructor(private val context: Context) {
    private val prefs = context.getSharedPreferences("SkillSwapPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    val isAuthenticated = mutableStateOf(false)

    init {
        // Restore auth state from persisted data
        restoreAuthState()
    }

    private fun restoreAuthState() {
        val token = getToken()
        val userJson = prefs.getString("cached_user", null)
        
        if (token != null && !TokenUtils.isTokenExpired(token) && userJson != null) {
            try {
                val user = gson.fromJson(userJson, User::class.java)
                _currentUser.value = user
                isAuthenticated.value = true
            } catch (e: Exception) {
                // Invalid cached data, clear it
                clearAuthData()
            }
        } else {
            clearAuthData()
        }
    }

    fun setUser(user: User?) {
        _currentUser.value = user
        isAuthenticated.value = user != null
        
        // Persist user data
        if (user != null) {
            try {
                val userJson = gson.toJson(user)
                prefs.edit().putString("cached_user", userJson).apply()
                user.id?.let { saveUserId(it) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            prefs.edit().remove("cached_user").apply()
        }
    }

    fun setToken(token: String) {
        prefs.edit().putString("auth_token", token).apply()
        isAuthenticated.value = true
    }

    fun getToken(): String? {
        return prefs.getString("auth_token", null)
    }

    fun getUserId(): String? {
        return _currentUser.value?.id ?: prefs.getString("user_id", null)
    }

    fun logout() {
        clearAuthData()
    }

    private fun clearAuthData() {
        _currentUser.value = null
        isAuthenticated.value = false
        prefs.edit().apply {
            remove("auth_token")
            remove("user_id")
            remove("cached_user")
            putBoolean("profile_completed", false)
        }.apply()
    }

    fun saveUserId(userId: String) {
        prefs.edit().putString("user_id", userId).apply()
    }

    fun markProfileComplete() {
        prefs.edit().putBoolean("profile_completed", true).apply()
    }
    
    suspend fun refreshUserProfile(fetchProfile: suspend (String) -> User?) {
        val token = getToken()
        if (token != null && !TokenUtils.isTokenExpired(token)) {
            try {
                val user = fetchProfile(token)
                if (user != null) {
                    setUser(user)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
