package com.skillswap.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.model.ModerationResult
import com.skillswap.security.SecureStorage
import com.skillswap.network.NetworkService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class ModerationViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferences = SecureStorage.getInstance(application)

    val isLoading = MutableStateFlow(false)
    val result = MutableStateFlow<ModerationResult?>(null)
    val error = MutableStateFlow<String?>(null)

    fun checkImage(base64: String) {
        val token = sharedPreferences.getString("auth_token", null) ?: run {
            error.value = "Session expirée"
            return
        }
        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            try {
                result.value = NetworkService.api.checkImage("Bearer $token", mapOf("imageBase64" to base64))
            } catch (e: Exception) {
                error.value = e.message
            } finally {
                isLoading.value = false
            }
        }
    }
}
