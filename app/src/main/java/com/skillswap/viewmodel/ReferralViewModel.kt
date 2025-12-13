package com.skillswap.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.model.ReferralsMeResponse
import com.skillswap.network.NetworkService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReferralViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("SkillSwapPrefs", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow<ReferralsMeResponse?>(null)
    val state: StateFlow<ReferralsMeResponse?> = _state.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    fun clearError() {
        _error.value = null
    }

    fun loadReferrals() {
        val token = prefs.getString("auth_token", null) ?: return
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                _state.value = NetworkService.api.getMyReferrals("Bearer $token")
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun redeem(code: String) {
        val token = prefs.getString("auth_token", null) ?: return
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            _message.value = null
            try {
                val res = NetworkService.api.redeemReferral("Bearer $token", mapOf("code" to code))
                _message.value = "Code appliqué (${res.status})"
                loadReferrals()
            } catch (e: Exception) {
                _error.value = e.message ?: "Impossible d'appliquer le code"
            } finally {
                _loading.value = false
            }
        }
    }
}
