package com.skillswap.network

import com.google.gson.Gson
import com.skillswap.model.ReferencedMessage
import com.skillswap.model.ThreadMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class ChatService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    private val baseURL = NetworkService.baseUrl
    
    /**
     * Add a reaction to a message
     * @param messageId The message ID to react to
     * @param emoji The emoji reaction (e.g., "👍", "❤️", "😂")
     * @param accessToken Bearer token
     * @return Updated message with reactions
     */
    suspend fun reactToMessage(
        messageId: String,
        emoji: String,
        accessToken: String
    ): ThreadMessage = withContext(Dispatchers.IO) {
        val url = "$baseURL/chat/messages/$messageId/react"
        
        val requestBody = mapOf("reaction" to emoji)
        val json = gson.toJson(requestBody)
        
        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .build()
        
        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw Exception("Failed to react to message: ${response.code} - ${response.message}")
        }
        
        val responseBody = response.body?.string() 
            ?: throw Exception("Empty response from server")
        
        gson.fromJson(responseBody, ThreadMessage::class.java)
    }
    
    /**
     * Delete a message
     * @param messageId The message ID to delete
     * @param accessToken Bearer token
     * @return Updated message (marked as deleted)
     */
    suspend fun deleteMessage(
        messageId: String,
        accessToken: String
    ): ThreadMessage = withContext(Dispatchers.IO) {
        val url = "$baseURL/chat/messages/$messageId"
        
        val request = Request.Builder()
            .url(url)
            .delete()
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Accept", "application/json")
            .build()
        
        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw Exception("Failed to delete message: ${response.code} - ${response.message}")
        }
        
        val responseBody = response.body?.string() 
            ?: throw Exception("Empty response from server")
        
        gson.fromJson(responseBody, ThreadMessage::class.java)
    }
    
    /**
     * Send a message with reply-to reference
     * @param threadId The thread ID
     * @param content The message content
     * @param replyToId The message ID being replied to (optional)
     * @param accessToken Bearer token
     * @return The created message
     */
    suspend fun sendMessage(
        threadId: String,
        content: String,
        replyToId: String? = null,
        accessToken: String
    ): ThreadMessage = withContext(Dispatchers.IO) {
        val url = "$baseURL/chat/threads/$threadId/messages"
        
        val requestBody = mutableMapOf(
            "content" to content,
            "type" to "text"
        )
        
        if (replyToId != null) {
            requestBody["replyTo"] = replyToId
        }
        
        val json = gson.toJson(requestBody)
        
        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .build()
        
        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw Exception("Failed to send message: ${response.code} - ${response.message}")
        }
        
        val responseBody = response.body?.string() 
            ?: throw Exception("Empty response from server")
        
        gson.fromJson(responseBody, ThreadMessage::class.java)
    }
    
    companion object {
        val instance = ChatService()
    }
}
