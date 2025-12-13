package com.skillswap.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skillswap.data.QuizQuestion
import com.skillswap.data.QuizService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class QuizViewModel : ViewModel() {
    private val quizService = QuizService.instance
    
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
    
    fun generateQuiz(subject: String, level: Int) {
        viewModelScope.launch {
            _isGenerating.value = true
            _errorMessage.value = null
            
            try {
                val questions = quizService.generateQuiz(subject, level)
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
    
    fun submitQuiz() {
        var correctAnswers = 0
        _quizQuestions.value.forEachIndexed { index, question ->
            val selectedAnswer = _selectedAnswers.value[index]
            if (selectedAnswer == question.correctAnswerIndex) {
                correctAnswers++
            }
        }
        _score.value = correctAnswers
        _showResults.value = true
    }
    
    fun resetQuiz() {
        _quizQuestions.value = emptyList()
        _currentQuestionIndex.value = 0
        _selectedAnswers.value = emptyMap()
        _showResults.value = false
        _score.value = 0
        _errorMessage.value = null
    }
}
