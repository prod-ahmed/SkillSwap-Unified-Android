package com.skillswap.notifications

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

class LocalNotificationManager(private val context: Context) {
    
    companion object {
        private const val CHANNEL_ID = "skillswap_channel"
        private const val CHANNEL_NAME = "SkillSwap Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for messages, sessions, and updates"
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun showNotification(
        id: Int,
        title: String,
        message: String,
        autoCancel: Boolean = true
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(autoCancel)
            .setContentIntent(pendingIntent)
        
        with(NotificationManagerCompat.from(context)) {
            notify(id, builder.build())
        }
    }
    
    fun showMessageNotification(senderName: String, message: String) {
        showNotification(
            id = System.currentTimeMillis().toInt(),
            title = senderName,
            message = message
        )
    }
    
    fun showCallNotification(callerName: String, isVideo: Boolean) {
        showNotification(
            id = System.currentTimeMillis().toInt(),
            title = "Appel ${if (isVideo) "vidéo" else "audio"} entrant",
            message = "De $callerName",
            autoCancel = false
        )
    }
    
    fun cancelNotification(id: Int) {
        NotificationManagerCompat.from(context).cancel(id)
    }
    
    fun cancelAll() {
        NotificationManagerCompat.from(context).cancelAll()
    }
}
