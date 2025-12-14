package com.skillswap.data

import android.content.Context
import com.skillswap.model.ModerationResult
import com.skillswap.network.NetworkService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Base64

class ModerationService private constructor(private val context: Context) {
    
    /**
     * Check if an image is safe using the backend moderation API
     */
    suspend fun checkImage(imageBytes: ByteArray): ModerationResult = withContext(Dispatchers.IO) {
        try {
            // Get auth token
            val prefs = context.getSharedPreferences("SkillSwapPrefs", Context.MODE_PRIVATE)
            val token = prefs.getString("auth_token", null)
            
            if (token.isNullOrEmpty()) {
                // If no token, allow the image (moderation requires auth)
                return@withContext ModerationResult(safe = true)
            }
            
            // Convert image to base64
            val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            
            // Call moderation API
            val response = NetworkService.api.checkImage(
                token = "Bearer $token",
                body = mapOf("image" to base64Image)
            )
            
            response
        } catch (e: Exception) {
            // On error, log and allow the image (fail open to not block users)
            android.util.Log.e("ModerationService", "Error checking image: ${e.message}", e)
            ModerationResult(safe = true)
        }
    }
    
    companion object {
        @Volatile
        private var instance: ModerationService? = null
        
        fun getInstance(context: Context): ModerationService {
            return instance ?: synchronized(this) {
                instance ?: ModerationService(context.applicationContext).also { instance = it }
            }
        }
    }
}
