package com.skillswap.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.model.CreateWeeklyObjectiveRequest
import com.skillswap.security.SecureStorage
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class WeeklyObjectiveViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = SecureStorage.getInstance(application)
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

    private val _isGeneratingAI = MutableStateFlow(false)
    val isGeneratingAI: StateFlow<Boolean> = _isGeneratingAI.asStateFlow()
    
    private val _aiGeneratedData = MutableStateFlow<AIGeneratedObjective?>(null)
    val aiGeneratedData: StateFlow<AIGeneratedObjective?> = _aiGeneratedData.asStateFlow()

    data class AIGeneratedObjective(
        val title: String,
        val targetHours: Int,
        val suggestion: String,
        val tasks: List<String>
    )

    fun generateWithAI(skill: String, level: String, hoursPerWeek: Int) {
        viewModelScope.launch {
            _isGeneratingAI.value = true
            _error.value = null
            try {
                val response = callGeminiAPI(skill, level, hoursPerWeek)
                val parsed = parseAIResponse(response)
                _aiGeneratedData.value = parsed
            } catch (e: Exception) {
                _error.value = "AI generation failed: ${e.message}"
                // Set default fallback
                _aiGeneratedData.value = AIGeneratedObjective(
                    title = "Master $skill",
                    targetHours = hoursPerWeek,
                    suggestion = "Practice regularly and track your progress",
                    tasks = List(7) { "Practice $skill - Day ${it + 1}" }
                )
            } finally {
                _isGeneratingAI.value = false
            }
        }
    }

    private suspend fun callGeminiAPI(skill: String, level: String, hoursPerWeek: Int): String = withContext(Dispatchers.IO) {
        val apiKey = com.skillswap.BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            throw Exception("Gemini API key not configured")
        }

        val prompt = """
            Create a weekly learning plan for:
            - Skill: $skill
            - Current Level: $level
            - Available Hours per Week: $hoursPerWeek
            
            Please provide:
            1. A motivating title for this week's objective
            2. Realistic target hours
            3. A brief suggestion or tip
            4. A 7-day learning plan with specific daily tasks
            
            Format your response EXACTLY as:
            Title: [your title]
            Hours: [number]
            Suggestion: [your suggestion]
            Day 1: [task]
            Day 2: [task]
            Day 3: [task]
            Day 4: [task]
            Day 5: [task]
            Day 6: [task]
            Day 7: [task]
        """.trimIndent()

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp:generateContent?key=$apiKey"
        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val requestBody = """
            {
                "contents": [{
                    "parts": [{"text": "${prompt.replace("\"", "\\\"")}"}]
                }],
                "generationConfig": {
                    "temperature": 0.7,
                    "maxOutputTokens": 1024
                }
            }
        """.trimIndent()

        val request = okhttp3.Request.Builder()
            .url(url)
            .post(
                requestBody.toRequestBody("application/json; charset=utf-8".toMediaType())
            )
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("API request failed with status ${response.code}")
        }

        val responseBody = response.body?.string() ?: throw Exception("Empty response")
        parseGeminiResponse(responseBody)
    }

    private fun parseGeminiResponse(jsonResponse: String): String {
        val json = org.json.JSONObject(jsonResponse)
        val candidates = json.getJSONArray("candidates")
        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.getJSONObject("content")
        val parts = content.getJSONArray("parts")
        val firstPart = parts.getJSONObject(0)
        return firstPart.getString("text")
    }

    private fun parseAIResponse(response: String): AIGeneratedObjective {
        val lines = response.lines().map { it.trim() }.filter { it.isNotEmpty() }
        
        var title = "Weekly Learning Goal"
        var hours = 10
        var suggestion = "Stay consistent and track your progress"
        val tasks = mutableListOf<String>()
        
        for (line in lines) {
            when {
                line.startsWith("Title:", ignoreCase = true) -> {
                    title = line.substring(6).trim()
                }
                line.startsWith("Hours:", ignoreCase = true) -> {
                    val hoursStr = line.substring(6).trim()
                    hours = hoursStr.filter { it.isDigit() }.toIntOrNull() ?: 10
                }
                line.startsWith("Suggestion:", ignoreCase = true) -> {
                    suggestion = line.substring(11).trim()
                }
                line.matches(Regex("Day \\d+:.*", RegexOption.IGNORE_CASE)) -> {
                    val colonIndex = line.indexOf(':')
                    if (colonIndex != -1) {
                        val task = line.substring(colonIndex + 1).trim()
                        if (task.isNotEmpty()) {
                            tasks.add(task)
                        }
                    }
                }
            }
        }
        
        // Ensure we have 7 tasks
        while (tasks.size < 7) {
            tasks.add("Practice and review - Day ${tasks.size + 1}")
        }
        
        return AIGeneratedObjective(
            title = title,
            targetHours = hours,
            suggestion = suggestion,
            tasks = tasks.take(7)
        )
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
