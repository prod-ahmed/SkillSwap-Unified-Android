package com.skillswap.data

import android.content.Context
import com.skillswap.model.LessonPlan
import com.skillswap.model.LessonPlanGenerateRequest
import com.skillswap.network.NetworkService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LessonPlanService(private val context: Context) {

    private fun getToken(): String {
        val prefs = context.getSharedPreferences("SkillSwapPrefs", Context.MODE_PRIVATE)
        return prefs.getString("auth_token", "") ?: ""
    }

    suspend fun getLessonPlan(sessionId: String): LessonPlan? = withContext(Dispatchers.IO) {
        try {
            val response = NetworkService.api.getLessonPlan("Bearer ${getToken()}", sessionId)
            response.data
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 404) {
                null
            } else {
                throw e
            }
        }
    }

    suspend fun generateLessonPlan(
        sessionId: String,
        level: String? = null,
        goal: String? = null
    ): LessonPlan = withContext(Dispatchers.IO) {
        val request = LessonPlanGenerateRequest(level = level, goal = goal)
        val response = NetworkService.api.generateLessonPlan("Bearer ${getToken()}", sessionId, request)
        response.data ?: throw Exception(response.error ?: "Failed to generate lesson plan")
    }

    suspend fun regenerateLessonPlan(
        sessionId: String,
        level: String? = null,
        goal: String? = null
    ): LessonPlan = withContext(Dispatchers.IO) {
        val request = LessonPlanGenerateRequest(level = level, goal = goal)
        val response = NetworkService.api.regenerateLessonPlan("Bearer ${getToken()}", sessionId, request)
        response.data ?: throw Exception(response.error ?: "Failed to regenerate lesson plan")
    }

    suspend fun updateProgress(
        sessionId: String,
        checklistIndex: Int,
        completed: Boolean
    ): LessonPlan = withContext(Dispatchers.IO) {
        val body = mapOf(
            "checklistIndex" to checklistIndex,
            "completed" to completed
        )
        val response = NetworkService.api.updateLessonPlanProgress("Bearer ${getToken()}", sessionId, body)
        response.data ?: throw Exception(response.error ?: "Failed to update progress")
    }

    companion object {
        @Volatile
        private var instance: LessonPlanService? = null

        fun getInstance(context: Context): LessonPlanService {
            return instance ?: synchronized(this) {
                instance ?: LessonPlanService(context.applicationContext).also { instance = it }
            }
        }
    }
}
