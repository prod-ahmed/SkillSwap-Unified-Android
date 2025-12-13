package com.skillswap.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.skillswap.data.QuizRepository
import com.skillswap.model.QuizQuestion
import com.skillswap.model.QuizResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class QuizState(
    val subject: String = "",
    val unlockedLevel: Int = 1,
    val history: List<QuizResult> = emptyList(),
    val questions: List<QuizQuestion> = emptyList(),
    val currentLevel: Int? = null,
    val score: Int = 0,
    val currentIndex: Int = 0,
    val finished: Boolean = false,
    val message: String? = null
)

class QuizViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = QuizRepository(application.getSharedPreferences("SkillSwapPrefs", Context.MODE_PRIVATE))
    private val _state = MutableStateFlow(QuizState())
    val state: StateFlow<QuizState> = _state.asStateFlow()

    fun setSubject(subject: String) {
        val level = repo.unlockedLevel(subject)
        _state.value = _state.value.copy(subject = subject, unlockedLevel = level, history = repo.history(), finished = false, message = null)
    }

    fun startLevel(level: Int) {
        val subject = _state.value.subject
        if (subject.isBlank()) return
        // Backend-backed quizzes not yet available on Android; surface message instead of synthetic data
        _state.value = _state.value.copy(
            message = "Les quiz seront disponibles dès que le backend sera exposé. Merci de revenir bientôt.",
            questions = emptyList(),
            currentLevel = null,
            finished = false,
            score = 0,
            currentIndex = 0
        )
    }

    fun answer(optionIndex: Int) {
        val current = _state.value
        val questions = current.questions
        val idx = current.currentIndex
        if (idx >= questions.size) return
        val isCorrect = questions[idx].correctAnswerIndex == optionIndex
        val newScore = current.score + if (isCorrect) 1 else 0
        val nextIndex = idx + 1
        if (nextIndex >= questions.size) {
            val result = QuizResult(
                subject = current.subject,
                level = current.currentLevel ?: 1,
                score = newScore,
                totalQuestions = questions.size
            )
            repo.saveResult(result)
            val level = repo.unlockedLevel(current.subject)
            _state.value = current.copy(
                score = newScore,
                currentIndex = nextIndex,
                finished = true,
                unlockedLevel = level,
                history = repo.history()
            )
        } else {
            _state.value = current.copy(score = newScore, currentIndex = nextIndex)
        }
    }

    fun resetQuiz() {
        _state.value = _state.value.copy(questions = emptyList(), currentLevel = null, finished = false, currentIndex = 0, score = 0, message = null)
    }
}
