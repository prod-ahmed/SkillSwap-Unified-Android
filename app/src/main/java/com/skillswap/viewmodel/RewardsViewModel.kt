package com.skillswap.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.model.ReferralsMeResponse
import com.skillswap.security.SecureStorage
import com.skillswap.model.User
import com.skillswap.network.NetworkService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RewardsViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferences = SecureStorage.getInstance(application)

    private val _referralsData = MutableStateFlow<ReferralsMeResponse?>(null)
    val referralsData: StateFlow<ReferralsMeResponse?> = _referralsData.asStateFlow()

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadRewards() {
        val token = sharedPreferences.getString("auth_token", null) ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Fetch user for points/xp
                val userResponse = NetworkService.api.getMe("Bearer $token")
                _user.value = userResponse

                // Fetch referrals for rewards history
                val referralsResponse = NetworkService.api.getMyReferrals("Bearer $token")
                _referralsData.value = referralsResponse
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
