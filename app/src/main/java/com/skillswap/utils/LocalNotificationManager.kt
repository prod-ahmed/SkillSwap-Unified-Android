package com.skillswap.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.skillswap.MainActivity
import com.skillswap.R

class LocalNotificationManager(private val context: Context) {
    
    companion object {
        private const val CHANNEL_ID_MESSAGES = "skillswap_messages"
        private const val CHANNEL_ID_SESSIONS = "skillswap_sessions"
        private const val CHANNEL_ID_PROMOS = "skillswap_promos"
        private const val CHANNEL_ID_CALLS = "skillswap_calls"
        private const val CHANNEL_ID_GENERAL = "skillswap_notifications"
        
        private var instance: LocalNotificationManager? = null
        
        fun getInstance(context: Context): LocalNotificationManager {
            if (instance == null) {
                instance = LocalNotificationManager(context.applicationContext)
            }
            return instance!!
        }
    }
    
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    init {
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Messages channel - high priority
            val messagesChannel = NotificationChannel(
                CHANNEL_ID_MESSAGES,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Nouveaux messages de chat"
                enableLights(true)
                enableVibration(true)
            }
            
            // Sessions channel - high priority
            val sessionsChannel = NotificationChannel(
                CHANNEL_ID_SESSIONS,
                "Sessions",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Nouvelles sessions et rappels"
                enableLights(true)
                enableVibration(true)
            }
            
            // Promos & announcements channel - default priority
            val promosChannel = NotificationChannel(
                CHANNEL_ID_PROMOS,
                "Promotions et Annonces",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Offres spéciales et annonces basées sur vos intérêts"
                enableLights(true)
            }
            
            // Calls channel - max priority
            val callsChannel = NotificationChannel(
                CHANNEL_ID_CALLS,
                "Appels",
                NotificationManager.IMPORTANCE_MAX
            ).apply {
                description = "Appels entrants"
                enableLights(true)
                enableVibration(true)
            }
            
            // General channel
            val generalChannel = NotificationChannel(
                CHANNEL_ID_GENERAL,
                "Général",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications générales"
            }
            
            notificationManager.createNotificationChannels(
                listOf(messagesChannel, sessionsChannel, promosChannel, callsChannel, generalChannel)
            )
        }
    }
    
    fun requestPermissionIfNeeded(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    
    fun showNotification(
        id: Int = System.currentTimeMillis().toInt(),
        title: String,
        body: String,
        data: Map<String, String>? = null,
        channelId: String = CHANNEL_ID_GENERAL
    ) {
        if (!requestPermissionIfNeeded()) {
            return
        }
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            data?.forEach { (key, value) ->
                putExtra(key, value)
            }
        }
        
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        
        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notify(id, builder.build())
            }
        }
    }
    
    fun showMessageNotification(
        threadId: String,
        senderName: String,
        messageText: String
    ) {
        showNotification(
            id = threadId.hashCode(),
            title = senderName,
            body = messageText,
            data = mapOf(
                "type" to "chat",
                "threadId" to threadId
            ),
            channelId = CHANNEL_ID_MESSAGES
        )
    }
    
    fun showCallNotification(
        callerId: String,
        callerName: String,
        callType: String
    ) {
        showNotification(
            id = callerId.hashCode(),
            title = "Appel entrant",
            body = "$callerName vous appelle ($callType)",
            data = mapOf(
                "type" to "call",
                "callerId" to callerId,
                "callType" to callType
            ),
            channelId = CHANNEL_ID_CALLS
        )
    }
    
    fun showSessionNotification(
        sessionId: String,
        title: String,
        body: String
    ) {
        showNotification(
            id = sessionId.hashCode(),
            title = title,
            body = body,
            data = mapOf(
                "type" to "session",
                "sessionId" to sessionId
            ),
            channelId = CHANNEL_ID_SESSIONS
        )
    }
    
    fun showSessionReminderNotification(
        sessionId: String,
        title: String,
        body: String,
        scheduledTime: String
    ) {
        val reminderBody = if (scheduledTime.isNotEmpty()) {
            "$body\n⏰ $scheduledTime"
        } else {
            body
        }
        
        showNotification(
            id = "reminder_$sessionId".hashCode(),
            title = "⏰ $title",
            body = reminderBody,
            data = mapOf(
                "type" to "session_reminder",
                "sessionId" to sessionId
            ),
            channelId = CHANNEL_ID_SESSIONS
        )
    }
    
    fun showPromoNotification(
        promoId: String,
        title: String,
        body: String,
        skillCategory: String
    ) {
        val promoTitle = "🎁 $title"
        val promoBody = if (skillCategory.isNotEmpty()) {
            "$body\n📚 Catégorie: $skillCategory"
        } else {
            body
        }
        
        showNotification(
            id = promoId.hashCode(),
            title = promoTitle,
            body = promoBody,
            data = mapOf(
                "type" to "promo",
                "promoId" to promoId,
                "skillCategory" to skillCategory
            ),
            channelId = CHANNEL_ID_PROMOS
        )
    }
    
    fun showAnnouncementNotification(
        announcementId: String,
        title: String,
        body: String,
        targetSkills: String
    ) {
        val announcementTitle = "📢 $title"
        val announcementBody = if (targetSkills.isNotEmpty()) {
            "$body\n🎯 Compétences: $targetSkills"
        } else {
            body
        }
        
        showNotification(
            id = announcementId.hashCode(),
            title = announcementTitle,
            body = announcementBody,
            data = mapOf(
                "type" to "announcement",
                "announcementId" to announcementId,
                "targetSkills" to targetSkills
            ),
            channelId = CHANNEL_ID_PROMOS
        )
    }
    
    fun showSkillMatchNotification(
        userId: String,
        title: String,
        body: String,
        matchedSkill: String
    ) {
        val matchTitle = "🎯 $title"
        val matchBody = if (matchedSkill.isNotEmpty()) {
            "$body\n💡 Compétence: $matchedSkill"
        } else {
            body
        }
        
        showNotification(
            id = "match_$userId".hashCode(),
            title = matchTitle,
            body = matchBody,
            data = mapOf(
                "type" to "skill_match",
                "userId" to userId,
                "matchedSkill" to matchedSkill
            ),
            channelId = CHANNEL_ID_PROMOS
        )
    }
    
    fun cancelNotification(id: Int) {
        notificationManager.cancel(id)
    }
    
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
}
