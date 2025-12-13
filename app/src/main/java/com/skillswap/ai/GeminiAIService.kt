package com.skillswap.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiAIService {
    private const val API_KEY = "AIzaSyB0A6A2pYTsmQd80XIZeeKRMpr4BipHrEs"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp:generateContent"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    data class WeeklyPlanSuggestion(
        val title: String,
        val hours: Int,
        val suggestion: String,
        val tasks: List<String>
    )

    suspend fun generateWeeklyPlan(userGoal: String): WeeklyPlanSuggestion = withContext(Dispatchers.IO) {
        val systemPrompt = """
            You are a helpful assistant that creates weekly learning plans.
            Based on the user's goal, suggest a concise title and realistic hours.
            Provide a brief 1-sentence suggestion.
            Provide a daily breakdown for 7 days.
            Format your response EXACTLY like this (no markdown, no extra text):
            Title: [Title]
            Hours: [Number]
            Suggestion: [Text]
            Day 1: [Task description]
            Day 2: [Task description]
            Day 3: [Task description]
            Day 4: [Task description]
            Day 5: [Task description]
            Day 6: [Task description]
            Day 7: [Task description]
        """.trimIndent()

        val fullPrompt = "$systemPrompt\n\nUser Goal: $userGoal"

        val requestBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", fullPrompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 1024)
            })
        }

        var lastException: Exception? = null
        repeat(3) { attempt ->
            try {
                if (attempt > 0) {
                    val delayMs = when (attempt) {
                        1 -> 2000L
                        2 -> 5000L
                        else -> 10000L
                    }
                    Thread.sleep(delayMs)
                }

                val request = Request.Builder()
                    .url("$BASE_URL?key=$API_KEY")
                    .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    if (response.code == 429) {
                        throw Exception("Rate limit exceeded")
                    }
                    throw Exception("API request failed: ${response.code}")
                }

                val responseBody = response.body?.string() ?: throw Exception("Empty response")
                val json = JSONObject(responseBody)
                
                val text = json
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                return@withContext parseAIResponse(text)
                
            } catch (e: Exception) {
                lastException = e
                if (attempt == 2) throw e
            }
        }
        
        throw lastException ?: Exception("AI generation failed")
    }

    private fun parseAIResponse(response: String): WeeklyPlanSuggestion {
        val lines = response.lines().map { it.trim() }.filter { it.isNotEmpty() }
        
        var title = "Weekly Learning Goal"
        var hours = 10
        var suggestion = "Follow the daily tasks to achieve your goal"
        val tasks = mutableListOf<String>()

        for (line in lines) {
            when {
                line.startsWith("Title:", ignoreCase = true) -> {
                    title = line.substring(6).trim()
                }
                line.startsWith("Hours:", ignoreCase = true) -> {
                    val hoursStr = line.substring(6).trim()
                    hours = hoursStr.filter { it.isDigit() }.toIntOrNull() ?: 10
                    hours = hours.coerceIn(1, 50)
                }
                line.startsWith("Suggestion:", ignoreCase = true) -> {
                    suggestion = line.substring(11).trim()
                }
                line.startsWith("Day ", ignoreCase = true) -> {
                    val colonIndex = line.indexOf(':')
                    if (colonIndex > 0 && colonIndex < line.length - 1) {
                        val task = line.substring(colonIndex + 1).trim()
                        if (task.isNotEmpty()) {
                            tasks.add(task)
                        }
                    }
                }
            }
        }

        // Ensure we have exactly 7 tasks
        while (tasks.size < 7) {
            tasks.add("Continue learning - Day ${tasks.size + 1}")
        }
        if (tasks.size > 7) {
            tasks.subList(7, tasks.size).clear()
        }

        return WeeklyPlanSuggestion(
            title = title,
            hours = hours,
            suggestion = suggestion,
            tasks = tasks
        )
    }
}
