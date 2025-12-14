package com.skillswap.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.network.NetworkService
import com.skillswap.security.SecureStorage
import kotlinx.coroutines.launch

data class ProfileSetupState(
    val isLoading: Boolean = false,
    val error: String? = null
)

class ProfileSetupViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = SecureStorage.getInstance(application)
    var state by mutableStateOf(ProfileSetupState())
        private set

    fun completeProfile(skillsTeach: String, skillsLearn: String, city: String, marketingOptIn: Boolean, onDone: () -> Unit) {
        val token = prefs.getString("auth_token", null) ?: return
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            try {
                val body = mapOf(
                    "skillsTeach" to skillsTeach.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                    "skillsLearn" to skillsLearn.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                    "location" to mapOf("city" to city),
                    "marketingOptIn" to marketingOptIn
                )
                NetworkService.api.updateProfile("Bearer $token", body)
                prefs.edit().putBoolean("profile_completed", true).apply()
                onDone()
            } catch (e: Exception) {
                state = state.copy(error = e.message)
            } finally {
                state = state.copy(isLoading = false)
            }
        }
    }
}
