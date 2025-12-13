package com.skillswap.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.model.CreateWeeklyObjectiveRequest
import com.skillswap.model.DailyTaskRequest
import com.skillswap.model.TaskUpdateRequest
import com.skillswap.model.UpdateWeeklyObjectiveRequest
import com.skillswap.model.WeeklyObjective
import com.skillswap.network.NetworkService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import com.skillswap.widget.WeeklyObjectiveWidgetProvider

class WeeklyObjectiveViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("SkillSwapPrefs", Context.MODE_PRIVATE)
    private val appContext = application.applicationContext

    private val _current = MutableStateFlow<WeeklyObjective?>(null)
    val current: StateFlow<WeeklyObjective?> = _current.asStateFlow()

    private val _history = MutableStateFlow<List<WeeklyObjective>>(emptyList())
    val history: StateFlow<List<WeeklyObjective>> = _history.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun load() {
        val token = prefs.getString("auth_token", null) ?: return
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                _current.value = NetworkService.api.getCurrentWeeklyObjective("Bearer $token")
                _current.value?.let { saveWidgetState(it) }
                val hist = NetworkService.api.getWeeklyObjectiveHistory("Bearer $token", page = 1, limit = 10)
                _history.value = hist.objectives
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun clearError() {
        _error.value = null
    }

    fun createObjective(title: String, targetHours: Int, startDate: String, endDate: String, tasks: List<String>) {
        val token = prefs.getString("auth_token", null) ?: return
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val daily = tasks.mapIndexed { idx, task -> DailyTaskRequest(day = "Day ${idx + 1}", task = task) }
                val created = NetworkService.api.createWeeklyObjective(
                    "Bearer $token",
                    CreateWeeklyObjectiveRequest(
                        title = title,
                        targetHours = targetHours,
                        startDate = startDate,
                        endDate = endDate,
                        dailyTasks = daily
                )
            )
            _current.value = created
            saveWidgetState(created)
            _message.value = "Objectif créé"
        } catch (e: Exception) {
            _error.value = e.message
        } finally {
            _loading.value = false
            }
        }
    }

    fun toggleTask(index: Int, isCompleted: Boolean) {
        val token = prefs.getString("auth_token", null) ?: return
        val objectiveId = _current.value?.id ?: return
        viewModelScope.launch {
            try {
                val updated = NetworkService.api.updateWeeklyObjective(
                    "Bearer $token",
                    objectiveId,
                    UpdateWeeklyObjectiveRequest(taskUpdates = listOf(TaskUpdateRequest(index = index, isCompleted = isCompleted)))
                )
                _current.value = updated
                saveWidgetState(updated)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun completeObjective() {
        val token = prefs.getString("auth_token", null) ?: return
        val objectiveId = _current.value?.id ?: return
        viewModelScope.launch {
            try {
                val updated = NetworkService.api.completeWeeklyObjective("Bearer $token", objectiveId)
                _current.value = updated
                saveWidgetState(updated)
                _message.value = "Objectif marqué comme terminé"
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun deleteObjective() {
        val token = prefs.getString("auth_token", null) ?: return
        val objectiveId = _current.value?.id ?: return
        viewModelScope.launch {
            try {
                NetworkService.api.deleteWeeklyObjective("Bearer $token", objectiveId)
                _current.value = null
                clearWidgetState()
                _message.value = "Objectif supprimé"
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    private fun saveWidgetState(objective: WeeklyObjective) {
        prefs.edit()
            .putString("widget_objective_title", objective.title)
            .putInt("widget_objective_progress", objective.progressPercent)
            .apply()
        notifyWidget()
    }

    private fun clearWidgetState() {
        prefs.edit()
            .remove("widget_objective_title")
            .remove("widget_objective_progress")
            .apply()
        notifyWidget()
    }

    private fun notifyWidget() {
        val manager = AppWidgetManager.getInstance(appContext)
        val ids = manager.getAppWidgetIds(ComponentName(appContext, WeeklyObjectiveWidgetProvider::class.java))
        // Trigger update directly
        WeeklyObjectiveWidgetProvider.updateAll(appContext, manager, ids, prefs)
    }
}
