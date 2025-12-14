package com.skillswap.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.model.ReferralCodeResponse
import com.skillswap.security.SecureStorage
import com.skillswap.model.ReferralsMeResponse
import com.skillswap.network.NetworkService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReferralViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = SecureStorage.getInstance(application)

    private val _state = MutableStateFlow<ReferralsMeResponse?>(null)
    val state: StateFlow<ReferralsMeResponse?> = _state.asStateFlow()

    private val _generatedCode = MutableStateFlow<ReferralCodeResponse?>(null)
    val generatedCode: StateFlow<ReferralCodeResponse?> = _generatedCode.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _creatingCode = MutableStateFlow(false)
    val creatingCode: StateFlow<Boolean> = _creatingCode.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    fun clearError() {
        _error.value = null
    }

    fun clearGeneratedCode() {
        _generatedCode.value = null
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

    fun createCode(usageLimit: Int = 0, expiresAt: String? = null, campaign: String? = null) {
        val token = prefs.getString("auth_token", null) ?: return
        viewModelScope.launch {
            _creatingCode.value = true
            _error.value = null
            _message.value = null
            try {
                val payload = mutableMapOf<String, Any>(
                    "usageLimit" to usageLimit
                )
                expiresAt?.let { payload["expiresAt"] = it }
                campaign?.let { payload["campaign"] = it }

                val response = NetworkService.api.createReferralCode(
                    "Bearer $token",
                    payload
                )
                _generatedCode.value = response
                _message.value = "Code de parrainage créé avec succès!"
                loadReferrals()
            } catch (e: Exception) {
                _error.value = e.message ?: "Impossible de créer le code"
            } finally {
                _creatingCode.value = false
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
