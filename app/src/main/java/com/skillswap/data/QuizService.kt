package com.skillswap.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@Serializable
data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String
)

@Serializable
data class QuizResult(
    val id: String,
    val subject: String,
    val level: Int,
    val score: Int,
    val totalQuestions: Int,
    val date: Long
)

@Serializable
private data class OpenAIRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double
) {
    @Serializable
    data class Message(
        val role: String,
        val content: String
    )
}

@Serializable
private data class OpenAIResponse(
    val choices: List<Choice>
) {
    @Serializable
    data class Choice(
        val message: Message
    ) {
        @Serializable
        data class Message(
            val content: String
        )
    }
}

class QuizService {
    // ⚠️ PUT YOUR OPENAI API KEY HERE
    private val apiKey = BuildConfig.OPENAI_API_KEY
    
    private val baseURL = "https://api.openai.com/v1/chat/completions"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    suspend fun generateQuiz(subject: String, level: Int): List<QuizQuestion> = withContext(Dispatchers.IO) {
        val prompt = """
            Generate a quiz about "$subject" for level $level (where 1 is beginner and 10 is expert).
            Create 5 multiple choice questions.
            Return ONLY a JSON array of objects with this structure:
            [
                {
                    "question": "Question text",
                    "options": ["Option A", "Option B", "Option C", "Option D"],
                    "correctAnswerIndex": 0,
                    "explanation": "Brief explanation of the correct answer"
                }
            ]
            Do not include markdown formatting like ```json.
        """.trimIndent()
        
        val requestBody = OpenAIRequest(
            model = "gpt-3.5-turbo",
            messages = listOf(
                OpenAIRequest.Message("system", "You are a helpful quiz generator."),
                OpenAIRequest.Message("user", prompt)
            ),
            temperature = 0.7
        )
        
        val requestJson = json.encodeToString(OpenAIRequest.serializer(), requestBody)
        
        val request = Request.Builder()
            .url(baseURL)
            .post(requestJson.toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .build()
        
        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw Exception("Failed to generate quiz: ${response.code} - ${response.message}")
        }
        
        val responseBody = response.body?.string() 
            ?: throw Exception("Empty response from OpenAI")
        
        val apiResponse = json.decodeFromString(OpenAIResponse.serializer(), responseBody)
        val content = apiResponse.choices.firstOrNull()?.message?.content
            ?: throw Exception("No content in response")
        
        // Clean up markdown if present
        val cleanContent = content
            .replace("```json", "")
            .replace("```", "")
            .trim()
        
        json.decodeFromString<List<QuizQuestion>>(cleanContent)
    }
    
    companion object {
        val instance = QuizService()
    }
}
