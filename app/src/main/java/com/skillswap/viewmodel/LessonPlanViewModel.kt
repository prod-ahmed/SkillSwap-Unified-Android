package com.skillswap.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.data.LessonPlanService
import com.skillswap.model.LessonPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LessonPlanViewModel(application: Application) : AndroidViewModel(application) {
    
    private val service = LessonPlanService.getInstance(application)
    
    private val _lessonPlan = MutableStateFlow<LessonPlan?>(null)
    val lessonPlan: StateFlow<LessonPlan?> = _lessonPlan.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    fun loadLessonPlan(sessionId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val plan = service.getLessonPlan(sessionId)
                _lessonPlan.value = plan
            } catch (e: Exception) {
                _errorMessage.value = "Impossible de charger le plan de cours: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun generateLessonPlan(
        sessionId: String,
        level: String? = null,
        goal: String? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            
            try {
                val plan = service.generateLessonPlan(sessionId, level, goal)
                _lessonPlan.value = plan
                _successMessage.value = "Plan de cours généré avec succès"
            } catch (e: Exception) {
                _errorMessage.value = "Impossible de générer le plan: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun regenerateLessonPlan(
        sessionId: String,
        level: String? = null,
        goal: String? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            
            try {
                val plan = service.regenerateLessonPlan(sessionId, level, goal)
                _lessonPlan.value = plan
                _successMessage.value = "Plan de cours régénéré avec succès"
            } catch (e: Exception) {
                _errorMessage.value = "Impossible de régénérer le plan: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateProgress(
        sessionId: String,
        checklistIndex: Int,
        completed: Boolean
    ) {
        viewModelScope.launch {
            try {
                val updatedPlan = service.updateProgress(sessionId, checklistIndex, completed)
                _lessonPlan.value = updatedPlan
            } catch (e: Exception) {
                _errorMessage.value = "Impossible de mettre à jour: ${e.message}"
            }
        }
    }
    
    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
