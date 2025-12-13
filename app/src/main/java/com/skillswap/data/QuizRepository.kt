package com.skillswap.data

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.skillswap.model.QuizQuestion
import com.skillswap.model.QuizResult
import java.util.UUID

class QuizRepository(private val prefs: SharedPreferences) {
    private val gson = Gson()
    private val historyKey = "quiz_history"
    private val progressKey = "quiz_progress" // subject -> level

    fun history(): List<QuizResult> {
        val json = prefs.getString(historyKey, null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<QuizResult>>() {}.type
            gson.fromJson<List<QuizResult>>(json, type)
        }.getOrDefault(emptyList()).sortedByDescending { it.date }
    }

    fun saveResult(result: QuizResult) {
        val updated = history() + result
        prefs.edit().putString(historyKey, gson.toJson(updated)).apply()
        if (result.score.toDouble() / result.totalQuestions >= 0.5) {
            unlockNextLevel(result.subject, result.level)
        }
    }

    fun unlockedLevel(subject: String): Int {
        val json = prefs.getString(progressKey, null) ?: return 1
        val type = object : TypeToken<Map<String, Int>>() {}.type
        val progress = runCatching { gson.fromJson<Map<String, Int>>(json, type) }.getOrDefault(emptyMap())
        return progress[subject] ?: 1
    }

    private fun unlockNextLevel(subject: String, currentLevel: Int) {
        val json = prefs.getString(progressKey, null)
        val type = object : TypeToken<MutableMap<String, Int>>() {}.type
        val progress = runCatching { gson.fromJson<MutableMap<String, Int>>(json, type) }.getOrDefault(mutableMapOf())
        val maxLevel = progress[subject] ?: 1
        if (currentLevel >= maxLevel && maxLevel < 10) {
            progress[subject] = currentLevel + 1
            prefs.edit().putString(progressKey, gson.toJson(progress)).apply()
        }
    }

    fun generateQuiz(subject: String, level: Int): List<QuizQuestion> {
        val seed = subject.ifBlank { "SkillSwap" }.hashCode() + level
        val topics = listOf("Concepts", "Usage", "Best Practices", "Common Pitfalls", "Examples")
        val questions = (1..5).map { idx ->
            val topic = topics[idx % topics.size]
            val options = listOf(
                "$subject $topic option A",
                "$subject $topic option B",
                "$subject $topic option C",
                "$subject $topic option D"
            )
            QuizQuestion(
                id = UUID.nameUUIDFromBytes("$seed-$idx".toByteArray()).toString(),
                question = "Q$idx ($subject L$level): What about $topic?",
                options = options,
                correctAnswerIndex = idx % 4,
                explanation = "In $subject at level $level, $topic is addressed by option ${'A' + (idx % 4)}."
            )
        }
        return questions
    }
}
