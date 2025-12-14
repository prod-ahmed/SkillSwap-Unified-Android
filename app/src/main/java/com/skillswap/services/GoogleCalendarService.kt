package com.skillswap.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.skillswap.BuildConfig
import com.skillswap.network.NetworkService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service for handling Google Calendar OAuth2 authentication and sync.
 * 
 * This service manages:
 * - OAuth2 authentication flow
 * - Token storage and refresh
 * - Event synchronization with Google Calendar
 */
class GoogleCalendarService(private val context: Context) {
    
    private val sharedPreferences = context.getSharedPreferences("SkillSwapPrefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val PREF_GOOGLE_CONNECTED = "google_calendar_connected"
        private const val PREF_GOOGLE_TOKEN_EXPIRY = "google_token_expiry"
        
        @Volatile
        private var instance: GoogleCalendarService? = null
        
        fun getInstance(context: Context): GoogleCalendarService {
            return instance ?: synchronized(this) {
                instance ?: GoogleCalendarService(context.applicationContext).also { instance = it }
            }
        }
    }
    
    /**
     * Check if Google Calendar is connected
     */
    fun isConnected(): Boolean {
        return sharedPreferences.getBoolean(PREF_GOOGLE_CONNECTED, false)
    }
    
    /**
     * Start OAuth2 authentication flow
     * Opens the browser with Google's authorization URL
     */
    suspend fun startAuthFlow(): String? = withContext(Dispatchers.IO) {
        try {
            val token = sharedPreferences.getString("auth_token", null) ?: return@withContext null
            val response = NetworkService.api.getGoogleCalendarAuthUrl("Bearer $token")
            response.authUrl
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Open Google auth URL in browser
     */
    fun openAuthUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    
    /**
     * Handle OAuth2 callback with authorization code
     */
    suspend fun handleCallback(code: String, redirectUri: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val token = sharedPreferences.getString("auth_token", null) ?: return@withContext false
            val request = com.skillswap.model.GoogleCalendarTokenRequest(
                code = code,
                redirectUri = redirectUri
            )
            NetworkService.api.handleGoogleCalendarCallback("Bearer $token", request)
            
            // Save connection status
            sharedPreferences.edit()
                .putBoolean(PREF_GOOGLE_CONNECTED, true)
                .apply()
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Sync events with Google Calendar
     * @param bidirectional If true, sync both ways (app -> Google and Google -> app)
     */
    suspend fun syncEvents(bidirectional: Boolean = true): SyncResult = withContext(Dispatchers.IO) {
        try {
            val token = sharedPreferences.getString("auth_token", null) 
                ?: return@withContext SyncResult(success = false, error = "Not authenticated")
            
            val result = NetworkService.api.syncWithGoogleCalendar(
                "Bearer $token",
                mapOf("bidirectional" to bidirectional)
            )
            
            SyncResult(
                success = true,
                synced = result.synced,
                failed = result.failed,
                errors = result.errors
            )
        } catch (e: Exception) {
            SyncResult(success = false, error = e.message)
        }
    }
    
    /**
     * Disconnect Google Calendar
     */
    suspend fun disconnect(): Boolean = withContext(Dispatchers.IO) {
        try {
            val token = sharedPreferences.getString("auth_token", null) ?: return@withContext false
            NetworkService.api.disconnectGoogleCalendar("Bearer $token")
            
            // Clear connection status
            sharedPreferences.edit()
                .putBoolean(PREF_GOOGLE_CONNECTED, false)
                .remove(PREF_GOOGLE_TOKEN_EXPIRY)
                .apply()
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check connection status from backend
     */
    suspend fun checkStatus(): Boolean = withContext(Dispatchers.IO) {
        try {
            val token = sharedPreferences.getString("auth_token", null) ?: return@withContext false
            val status = NetworkService.api.getGoogleCalendarStatus("Bearer $token")
            val connected = status["connected"] as? Boolean ?: false
            
            // Update local status
            sharedPreferences.edit()
                .putBoolean(PREF_GOOGLE_CONNECTED, connected)
                .apply()
            
            connected
        } catch (e: Exception) {
            false
        }
    }
    
    data class SyncResult(
        val success: Boolean,
        val synced: Int = 0,
        val failed: Int = 0,
        val errors: List<String>? = null,
        val error: String? = null
    )
}
