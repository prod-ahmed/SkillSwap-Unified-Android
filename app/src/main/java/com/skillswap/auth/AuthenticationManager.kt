package com.skillswap.auth

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.skillswap.model.User
import com.skillswap.util.TokenUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthenticationManager private constructor(private val context: Context) {
    private val prefs = context.getSharedPreferences("SkillSwapPrefs", Context.MODE_PRIVATE)
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    val isAuthenticated = mutableStateOf(false)

    init {
        // Check for existing valid token on initialization
        val token = getToken()
        if (token != null && !TokenUtils.isTokenExpired(token)) {
            isAuthenticated.value = true
            // Load user data if needed
        }
    }

    fun setUser(user: User?) {
        _currentUser.value = user
        isAuthenticated.value = user != null
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
        _currentUser.value = null
        isAuthenticated.value = false
        prefs.edit().apply {
            remove("auth_token")
            remove("user_id")
            putBoolean("profile_completed", false)
        }.apply()
    }

    fun saveUserId(userId: String) {
        prefs.edit().putString("user_id", userId).apply()
    }

    fun markProfileComplete() {
        prefs.edit().putBoolean("profile_completed", true).apply()
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
