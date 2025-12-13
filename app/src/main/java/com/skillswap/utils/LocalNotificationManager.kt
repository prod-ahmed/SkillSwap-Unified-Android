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
        private const val CHANNEL_ID = "skillswap_notifications"
        private const val CHANNEL_NAME = "SkillSwap Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for SkillSwap app"
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
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
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
        data: Map<String, String>? = null
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
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
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
            title = senderName,
            body = messageText,
            data = mapOf(
                "type" to "chat",
                "threadId" to threadId
            )
        )
    }
    
    fun showCallNotification(
        callerId: String,
        callerName: String,
        callType: String
    ) {
        showNotification(
            title = "Appel entrant",
            body = "$callerName vous appelle ($callType)",
            data = mapOf(
                "type" to "call",
                "callerId" to callerId,
                "callType" to callType
            )
        )
    }
    
    fun cancelNotification(id: Int) {
        notificationManager.cancel(id)
    }
    
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
}
