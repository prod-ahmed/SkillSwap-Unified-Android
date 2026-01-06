package com.skillswap

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {
    
    companion object {
        // Deep link state
        val deepLinkType = mutableStateOf<String?>(null)
        val deepLinkData = mutableStateOf<Map<String, String>>(emptyMap())
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // Request notification permission for Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(permission), 101)
            }
        }
        
        // Handle deep link from notification
        handleDeepLink(intent)

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                SkillSwapApp()
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }
    
    private fun handleDeepLink(intent: Intent?) {
        intent?.extras?.let { extras ->
            val type = extras.getString("type")
            val data = mutableMapOf<String, String>()
            
            when (type) {
                "chat", "message" -> {
                    extras.getString("threadId")?.let { data["threadId"] = it }
                }
                "call" -> {
                    extras.getString("callerId")?.let { data["callerId"] = it }
                    extras.getString("callType")?.let { data["callType"] = it }
                }
                "session", "new_session", "session_reminder" -> {
                    extras.getString("sessionId")?.let { data["sessionId"] = it }
                }
                "notification" -> {
                    extras.getString("notificationId")?.let { data["notificationId"] = it }
                }
                "promo", "promotion" -> {
                    extras.getString("promoId")?.let { data["promoId"] = it }
                    extras.getString("skillCategory")?.let { data["skillCategory"] = it }
                }
                "announce", "announcement" -> {
                    extras.getString("announcementId")?.let { data["announcementId"] = it }
                    extras.getString("targetSkills")?.let { data["targetSkills"] = it }
                }
                "skill_match" -> {
                    extras.getString("userId")?.let { data["userId"] = it }
                    extras.getString("matchedSkill")?.let { data["matchedSkill"] = it }
                }
            }
            
            if (type != null) {
                deepLinkType.value = type
                deepLinkData.value = data
            }
        }
    }
    
    fun clearDeepLink() {
        deepLinkType.value = null
        deepLinkData.value = emptyMap()
    }
}
