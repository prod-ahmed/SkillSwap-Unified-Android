package com.skillswap.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.model.*
import com.skillswap.network.NetworkService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProgressViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferences = application.getSharedPreferences("SkillSwapPrefs", Context.MODE_PRIVATE)

    private val _dashboard = MutableStateFlow<ProgressDashboardResponse?>(null)
    val dashboard: StateFlow<ProgressDashboardResponse?> = _dashboard.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadDashboard() {
        val token = sharedPreferences.getString("auth_token", null) ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _dashboard.value = NetworkService.api.getProgressDashboard("Bearer $token")
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createGoal(title: String, targetHours: Double, period: String, dueDate: String?) {
        val token = sharedPreferences.getString("auth_token", null) ?: return
        viewModelScope.launch {
            try {
                val created = NetworkService.api.createGoal(
                    "Bearer $token",
                    CreateGoalRequest(title = title, targetHours = targetHours, period = period, dueDate = dueDate)
                )
                val current = _dashboard.value
                if (current != null) {
                    _dashboard.value = current.copy(goals = current.goals + created)
                } else {
                    loadDashboard()
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun deleteGoal(goalId: String) {
        val token = sharedPreferences.getString("auth_token", null) ?: return
        viewModelScope.launch {
            try {
                NetworkService.api.deleteGoal("Bearer $token", goalId)
                val current = _dashboard.value
                if (current != null) {
                    _dashboard.value = current.copy(goals = current.goals.filterNot { it.id == goalId })
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}
