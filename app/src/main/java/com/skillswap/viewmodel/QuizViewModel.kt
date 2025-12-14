package com.skillswap.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.data.QuizQuestion
import com.skillswap.security.SecureStorage
import com.skillswap.data.QuizResult
import com.skillswap.data.QuizService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

class QuizViewModel(application: Application) : AndroidViewModel(application) {
    private val quizService = QuizService.instance
    private val prefs = SecureStorage.getInstance(application)
    private val gson = Gson()
    
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()
    
    private val _quizQuestions = MutableStateFlow<List<QuizQuestion>>(emptyList())
    val quizQuestions: StateFlow<List<QuizQuestion>> = _quizQuestions.asStateFlow()
    
    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()
    
    private val _selectedAnswers = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val selectedAnswers: StateFlow<Map<Int, Int>> = _selectedAnswers.asStateFlow()
    
    private val _showResults = MutableStateFlow(false)
    val showResults: StateFlow<Boolean> = _showResults.asStateFlow()
    
    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _unlockedLevel = MutableStateFlow<Map<String, Int>>(emptyMap())
    val unlockedLevel: StateFlow<Map<String, Int>> = _unlockedLevel.asStateFlow()
    
    private val _subjects = MutableStateFlow<List<String>>(emptyList())
    val subjects: StateFlow<List<String>> = _subjects.asStateFlow()
    
    private val _selectedLevel = MutableStateFlow<Int?>(null)
    val selectedLevel: StateFlow<Int?> = _selectedLevel.asStateFlow()
    
    private val _quizHistory = MutableStateFlow<List<QuizResult>>(emptyList())
    val quizHistory: StateFlow<List<QuizResult>> = _quizHistory.asStateFlow()
    
    init {
        loadProgress()
        loadHistory()
        loadUserSkills()
    }
    
    private fun loadUserSkills() {
        viewModelScope.launch {
            try {
                // Load user profile to get skills
                val authManager = com.skillswap.auth.AuthenticationManager.getInstance(getApplication())
                val token = authManager.getToken()
                if (!token.isNullOrEmpty()) {
                    val apiService = com.skillswap.network.NetworkService.api
                    val response = apiService.getMe("Bearer $token")
                    val skills = mutableSetOf<String>()
                    response.skillsTeach?.let { skills.addAll(it) }
                    response.skillsLearn?.let { skills.addAll(it) }
                    
                    // Add default subjects if no skills
                    if (skills.isEmpty()) {
                        skills.addAll(listOf("Général", "Culture Générale", "Mathématiques"))
                    }
                    
                    _subjects.value = skills.toList()
            } catch (e: Exception) {
                // Fallback to default subjects
                _subjects.value = listOf("Général", "Culture Générale", "Mathématiques", "Sciences", "Technologie")
            }
        }
    }
    
    private fun loadProgress() {
        val progressJson = prefs.getString("quiz_progress", null)
        if (progressJson != null) {
            val type = object : TypeToken<Map<String, Int>>() {}.type
            val progress = gson.fromJson<Map<String, Int>>(progressJson, type)
            _unlockedLevel.value = progress
        } else {
            _unlockedLevel.value = emptyMap()
        }
    }
    
    private fun loadHistory() {
        val historyJson = prefs.getString("quiz_history", null)
        if (historyJson != null) {
            val type = object : TypeToken<List<QuizResult>>() {}.type
            val history = gson.fromJson<List<QuizResult>>(historyJson, type)
            _quizHistory.value = history.sortedByDescending { it.date }
        }
    }
    
    private fun saveProgress() {
        val progressJson = gson.toJson(_unlockedLevel.value)
        prefs.edit().putString("quiz_progress", progressJson).apply()
    }
    
    private fun saveHistory() {
        val historyJson = gson.toJson(_quizHistory.value)
        prefs.edit().putString("quiz_history", historyJson).apply()
    }
    
    fun selectLevel(level: Int) {
        _selectedLevel.value = level
    }
    
    fun clearSelectedLevel() {
        _selectedLevel.value = null
    }
    
    fun getUnlockedLevel(subject: String): Int {
        return _unlockedLevel.value[subject] ?: 1
    }
    
    fun unlockNextLevel(subject: String, currentLevel: Int) {
        val current = _unlockedLevel.value[subject] ?: 1
        if (currentLevel >= current && current < 10) {
            _unlockedLevel.value = _unlockedLevel.value.toMutableMap().apply {
                put(subject, currentLevel + 1)
            }
            saveProgress()
        }
    }
    
    fun generateQuiz(subject: String, level: Int) {
        viewModelScope.launch {
            _isGenerating.value = true
            _errorMessage.value = null
            
            try {
                // Use Cloudflare AI for quiz generation
                val aiResponse = com.skillswap.ai.CloudflareAIService.generateQuizQuestions(
                    subject = subject,
                    level = level,
                    numQuestions = 5
                )
                
                // Parse the AI response (it should be JSON)
                val questions = try {
                    quizService.parseQuizResponse(aiResponse)
                } catch (e: Exception) {
                    // Fallback to existing service if parsing fails
                    quizService.generateQuiz(subject, level)
                }
                
                _quizQuestions.value = questions
                _currentQuestionIndex.value = 0
                _selectedAnswers.value = emptyMap()
                _showResults.value = false
                _score.value = 0
            } catch (e: Exception) {
                _errorMessage.value = "Erreur lors de la génération: ${e.message}"
                _quizQuestions.value = emptyList()
            } finally {
                _isGenerating.value = false
            }
        }
    }
    
    fun selectAnswer(answerIndex: Int) {
        _selectedAnswers.value = _selectedAnswers.value.toMutableMap().apply {
            put(_currentQuestionIndex.value, answerIndex)
        }
    }
    
    fun nextQuestion() {
        if (_currentQuestionIndex.value < _quizQuestions.value.size - 1) {
            _currentQuestionIndex.value += 1
        }
    }
    
    fun submitQuiz(subject: String, level: Int) {
        var correctAnswers = 0
        _quizQuestions.value.forEachIndexed { index, question ->
            val selectedAnswer = _selectedAnswers.value[index]
            if (selectedAnswer == question.correctAnswerIndex) {
                correctAnswers++
            }
        }
        _score.value = correctAnswers
        _showResults.value = true
        
        // Save to history
        val result = QuizResult(
            id = UUID.randomUUID().toString(),
            subject = subject,
            level = level,
            score = correctAnswers,
            totalQuestions = _quizQuestions.value.size,
            date = System.currentTimeMillis()
        )
        _quizHistory.value = listOf(result) + _quizHistory.value
        saveHistory()
        
        // Unlock next level if passed (>= 50%)
        if (correctAnswers.toDouble() / _quizQuestions.value.size >= 0.5) {
            unlockNextLevel(subject, level)
        }
    }
    
    fun resetQuiz() {
        _quizQuestions.value = emptyList()
        _currentQuestionIndex.value = 0
        _selectedAnswers.value = emptyMap()
        _showResults.value = false
        _score.value = 0
        _errorMessage.value = null
    }
    
    fun clearHistory() {
        _quizHistory.value = emptyList()
        prefs.edit().remove("quiz_history").apply()
    }
}
