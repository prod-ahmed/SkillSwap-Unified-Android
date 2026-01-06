package com.skillswap.service

import android.util.Log
import com.skillswap.utils.LocalNotificationManager
import com.skillswap.security.SecureStorage
import com.skillswap.network.NetworkService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Firebase Messaging Service for handling push notifications.
 * This service extends FirebaseMessagingService when Firebase is configured,
 * otherwise it provides a fallback stub implementation.
 */
class SkillSwapMessagingService : com.google.firebase.messaging.FirebaseMessagingService() {
    
    companion object {
        private const val TAG = "SkillSwapFCM"
        
        /**
         * Check if Firebase is properly configured
         */
        fun isFirebaseAvailable(): Boolean {
            return try {
                com.google.firebase.FirebaseApp.getInstance()
                true
            } catch (e: Exception) {
                false
            }
        }
        
        /**
         * Request FCM token if Firebase is available
         */
        fun requestToken(onToken: (String?) -> Unit) {
            if (!isFirebaseAvailable()) {
                Log.w(TAG, "Firebase not configured, skipping FCM token request")
                onToken(null)
                return
            }
            
            try {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                    .addOnSuccessListener { token ->
                        Log.d(TAG, "FCM Token retrieved: ${token.take(20)}...")
                        onToken(token)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to get FCM token: ${e.message}")
                        onToken(null)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error requesting FCM token: ${e.message}")
                onToken(null)
            }
        }
    }
    
    private val notificationManager by lazy { LocalNotificationManager.getInstance(this) }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        sendTokenToServer(token)
    }
    
    override fun onMessageReceived(remoteMessage: com.google.firebase.messaging.RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Message received from: ${remoteMessage.from}")
        
        val data = remoteMessage.data
        val notificationType = data["type"] ?: "general"
        
        when (notificationType) {
            "message", "chat" -> handleMessageNotification(data)
            "session", "new_session" -> handleSessionNotification(data)
            "session_reminder" -> handleSessionReminderNotification(data)
            "promo", "promotion" -> handlePromoNotification(data)
            "announce", "announcement" -> handleAnnouncementNotification(data)
            "skill_match" -> handleSkillMatchNotification(data)
            else -> handleGeneralNotification(remoteMessage)
        }
    }
    
    private fun handleMessageNotification(data: Map<String, String>) {
        val threadId = data["threadId"] ?: return
        val senderName = data["senderName"] ?: "Nouveau message"
        val messageText = data["message"] ?: data["body"] ?: ""
        
        notificationManager.showMessageNotification(
            threadId = threadId,
            senderName = senderName,
            messageText = messageText
        )
    }
    
    private fun handleSessionNotification(data: Map<String, String>) {
        val sessionId = data["sessionId"] ?: ""
        val title = data["title"] ?: "Nouvelle session"
        val body = data["body"] ?: data["message"] ?: "Vous avez une nouvelle demande de session"
        val partnerName = data["partnerName"] ?: ""
        val skill = data["skill"] ?: ""
        
        val displayBody = if (partnerName.isNotEmpty() && skill.isNotEmpty()) {
            "$partnerName souhaite une session sur $skill"
        } else {
            body
        }
        
        notificationManager.showSessionNotification(
            sessionId = sessionId,
            title = title,
            body = displayBody
        )
    }
    
    private fun handleSessionReminderNotification(data: Map<String, String>) {
        val sessionId = data["sessionId"] ?: ""
        val title = data["title"] ?: "Rappel de session"
        val body = data["body"] ?: "Votre session commence bientôt"
        val scheduledTime = data["scheduledTime"] ?: ""
        
        notificationManager.showSessionReminderNotification(
            sessionId = sessionId,
            title = title,
            body = body,
            scheduledTime = scheduledTime
        )
    }
    
    private fun handlePromoNotification(data: Map<String, String>) {
        val title = data["title"] ?: "Offre spéciale"
        val body = data["body"] ?: data["message"] ?: ""
        val promoId = data["promoId"] ?: ""
        val skillCategory = data["skillCategory"] ?: ""
        
        notificationManager.showPromoNotification(
            promoId = promoId,
            title = title,
            body = body,
            skillCategory = skillCategory
        )
    }
    
    private fun handleAnnouncementNotification(data: Map<String, String>) {
        val title = data["title"] ?: "Annonce"
        val body = data["body"] ?: data["message"] ?: ""
        val announcementId = data["announcementId"] ?: ""
        val targetSkills = data["targetSkills"] ?: ""
        
        notificationManager.showAnnouncementNotification(
            announcementId = announcementId,
            title = title,
            body = body,
            targetSkills = targetSkills
        )
    }
    
    private fun handleSkillMatchNotification(data: Map<String, String>) {
        val title = data["title"] ?: "Nouveau match de compétence!"
        val body = data["body"] ?: "Un utilisateur correspond à vos intérêts"
        val userId = data["userId"] ?: ""
        val matchedSkill = data["matchedSkill"] ?: ""
        
        notificationManager.showSkillMatchNotification(
            userId = userId,
            title = title,
            body = body,
            matchedSkill = matchedSkill
        )
    }
    
    private fun handleGeneralNotification(remoteMessage: com.google.firebase.messaging.RemoteMessage) {
        remoteMessage.notification?.let { notification ->
            notificationManager.showNotification(
                title = notification.title ?: "SkillSwap",
                body = notification.body ?: ""
            )
        }
    }
    
    private fun sendTokenToServer(token: String) {
        val secureStorage = SecureStorage.getInstance(this)
        val authToken = secureStorage.getString("auth_token", null) ?: return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                NetworkService.api.registerFCMToken(
                    "Bearer $authToken",
                    mapOf("token" to token, "platform" to "android")
                )
                Log.d(TAG, "FCM token registered with server")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register FCM token: ${e.message}")
            }
        }
    }
}
