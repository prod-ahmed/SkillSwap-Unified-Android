package com.skillswap.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.google.gson.Gson

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferences = application.getSharedPreferences("SkillSwapPrefs", Context.MODE_PRIVATE)

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    fun loadProfile() {
        val token = sharedPreferences.getString("auth_token", null)
        val username = sharedPreferences.getString("username", "Utilisateur") ?: "Utilisateur"
        val id = sharedPreferences.getString("user_id", "") ?: ""

        if (token == null) {
            _user.value = buildFallbackUser(id, username)
            return
        }

        viewModelScope.launch {
            try {
                _user.value = com.skillswap.network.NetworkService.api.getMe("Bearer $token")
            } catch (e: Exception) {
                _user.value = buildFallbackUser(id, username)
            }
        }
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
