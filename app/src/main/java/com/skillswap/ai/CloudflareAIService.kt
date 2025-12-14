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
import android.util.Base64

object CloudflareAIService {
    // These should be set from environment variables or secure storage
    private var CLOUDFLARE_ACCOUNT_ID: String? = null
    private var CLOUDFLARE_WORKERS_AI_API_KEY: String? = null
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun initialize(accountId: String, apiKey: String) {
        CLOUDFLARE_ACCOUNT_ID = accountId
        CLOUDFLARE_WORKERS_AI_API_KEY = apiKey
    }

    /**
     * Generate text using Cloudflare Workers AI (gpt-oss-120b model)
     * https://developers.cloudflare.com/workers-ai/models/gpt-oss-120b/
     */
    suspend fun generateText(
        prompt: String,
        systemPrompt: String? = null,
        maxTokens: Int = 1024,
        temperature: Double = 0.7
    ): String = withContext(Dispatchers.IO) {
        require(CLOUDFLARE_ACCOUNT_ID != null) { "Cloudflare Account ID not initialized" }
        require(CLOUDFLARE_WORKERS_AI_API_KEY != null) { "Cloudflare Workers AI API Key not initialized" }

        val url = "https://api.cloudflare.com/client/v4/accounts/$CLOUDFLARE_ACCOUNT_ID/ai/run/@cf/openchat/openchat-3.5-0106"
        
        // Build messages array
        val messages = JSONArray()
        if (systemPrompt != null) {
            messages.put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })
        }
        messages.put(JSONObject().apply {
            put("role", "user")
            put("content", prompt)
        })

        val requestBody = JSONObject().apply {
            put("messages", messages)
            put("max_tokens", maxTokens)
            put("temperature", temperature)
        }

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $CLOUDFLARE_WORKERS_AI_API_KEY")
            .header("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw Exception("Cloudflare AI request failed: ${response.code} - ${response.message}")
        }

        val responseBody = response.body?.string() ?: throw Exception("Empty response from Cloudflare AI")
        val json = JSONObject(responseBody)
        
        // Extract response text
        if (json.getBoolean("success")) {
            val result = json.getJSONObject("result")
            return@withContext result.getString("response")
        } else {
            val errors = json.optJSONArray("errors")
            throw Exception("Cloudflare AI error: ${errors?.toString() ?: "Unknown error"}")
        }
    }

    /**
     * Generate image using Cloudflare Workers AI (flux-2-dev model)
     * https://developers.cloudflare.com/workers-ai/models/flux-2-dev/
     */
    suspend fun generateImage(
        prompt: String,
        numSteps: Int = 4,
        guidance: Double = 3.5
    ): ByteArray = withContext(Dispatchers.IO) {
        require(CLOUDFLARE_ACCOUNT_ID != null) { "Cloudflare Account ID not initialized" }
        require(CLOUDFLARE_WORKERS_AI_API_KEY != null) { "Cloudflare Workers AI API Key not initialized" }

        val url = "https://api.cloudflare.com/client/v4/accounts/$CLOUDFLARE_ACCOUNT_ID/ai/run/@cf/black-forest-labs/flux-1-schnell"
        
        val requestBody = JSONObject().apply {
            put("prompt", prompt)
            put("num_steps", numSteps)
            put("guidance", guidance)
        }

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $CLOUDFLARE_WORKERS_AI_API_KEY")
            .header("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw Exception("Cloudflare AI image generation failed: ${response.code} - ${response.message}")
        }

        val responseBody = response.body?.string() ?: throw Exception("Empty response from Cloudflare AI")
        val json = JSONObject(responseBody)
        
        if (json.getBoolean("success")) {
            val result = json.getJSONObject("result")
            // Image is returned as base64
            val imageBase64 = result.getString("image")
            return@withContext Base64.decode(imageBase64, Base64.DEFAULT)
        } else {
            val errors = json.optJSONArray("errors")
            throw Exception("Cloudflare AI image error: ${errors?.toString() ?: "Unknown error"}")
        }
    }

    /**
     * Generate quiz questions using text-to-text model
     */
    suspend fun generateQuizQuestions(
        subject: String,
        level: Int,
        numQuestions: Int = 5
    ): String {
        val systemPrompt = """
            You are a quiz generator. Create multiple-choice questions for educational purposes.
            Format your response as a JSON array with this structure:
            [
              {
                "question": "Question text?",
                "options": ["Option A", "Option B", "Option C", "Option D"],
                "correctAnswer": 0
              }
            ]
            Make questions relevant to the subject and appropriate for the difficulty level (1-10).
            Generate exactly $numQuestions questions.
        """.trimIndent()

        val prompt = "Generate $numQuestions quiz questions about $subject at difficulty level $level."
        
        return generateText(prompt, systemPrompt, maxTokens = 2048, temperature = 0.8)
    }

    /**
     * Generate lesson plan using text-to-text model
     */
    suspend fun generateLessonPlan(
        skill: String,
        duration: Int,
        level: String = "intermediate"
    ): String {
        val systemPrompt = """
            You are an expert educator creating structured lesson plans.
            Create a detailed lesson plan with:
            - Learning objectives
            - Topics to cover
            - Activities and exercises
            - Assessment methods
            Keep it practical and engaging.
        """.trimIndent()

        val prompt = "Create a $duration-minute lesson plan for teaching $skill to $level learners."
        
        return generateText(prompt, systemPrompt, maxTokens = 2048, temperature = 0.7)
    }

    /**
     * Generate promotional content using text-to-text model
     */
    suspend fun generatePromoContent(
        productName: String,
        discount: Int,
        targetAudience: String = "general"
    ): String {
        val systemPrompt = """
            You are a marketing copywriter. Create compelling promotional content.
            Include:
            - Catchy title
            - Engaging description
            - Call to action
            Keep it concise and persuasive.
        """.trimIndent()

        val prompt = "Create promotional content for '$productName' with a $discount% discount, targeting $targetAudience audience."
        
        return generateText(prompt, systemPrompt, maxTokens = 512, temperature = 0.9)
    }

    /**
     * Moderate content using text analysis
     */
    suspend fun moderateText(content: String): Boolean {
        val systemPrompt = """
            You are a content moderator. Analyze the provided text and determine if it contains:
            - Offensive language
            - Hate speech
            - Spam
            - Inappropriate content
            
            Respond with only "SAFE" or "UNSAFE" followed by a brief reason if unsafe.
        """.trimIndent()

        val prompt = "Analyze this content: $content"
        val result = generateText(prompt, systemPrompt, maxTokens = 100, temperature = 0.3)
        
        return result.trim().startsWith("SAFE", ignoreCase = true)
    }
}
