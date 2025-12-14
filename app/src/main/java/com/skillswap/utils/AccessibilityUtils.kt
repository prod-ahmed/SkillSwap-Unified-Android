package com.skillswap.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Accessibility utilities for SkillSwap
 * Ensures WCAG 2.1 AA compliance
 */
object AccessibilityUtils {
    
    /**
     * Add content description to any composable
     * Usage: Modifier.accessibilityLabel("Button to send message")
     */
    fun Modifier.accessibilityLabel(label: String): Modifier {
        return this.semantics {
            contentDescription = label
        }
    }
    
    /**
     * Provide haptic feedback for user actions
     */
    fun provideHapticFeedback(context: Context, type: HapticFeedbackType = HapticFeedbackType.CLICK) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        if (!vibrator.hasVibrator()) return
        
        when (type) {
            HapticFeedbackType.CLICK -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(10)
                }
            }
            HapticFeedbackType.LONG_PRESS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
            }
            HapticFeedbackType.SUCCESS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 10, 10, 10), -1)
                }
            }
            HapticFeedbackType.ERROR -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 50, 50, 50), -1)
                }
            }
        }
    }
    
    /**
     * Format text for screen readers
     */
    fun formatForScreenReader(text: String, context: String = ""): String {
        return if (context.isNotEmpty()) {
            "$context: $text"
        } else {
            text
        }
    }
    
    /**
     * Get accessibility announcement for match action
     */
    fun getMatchAnnouncement(userName: String, action: String): String {
        return when (action) {
            "like" -> "Interested in $userName"
            "pass" -> "Passed on $userName"
            "match" -> "It's a match with $userName!"
            else -> action
        }
    }
    
    /**
     * Get accessibility label for navigation items
     */
    fun getNavigationLabel(route: String): String {
        return when (route) {
            "discover" -> "Discover tab. Browse users to connect with"
            "sessions" -> "Sessions tab. View your learning sessions"
            "chat" -> "Messages tab. View conversations"
            "profile" -> "Profile tab. Manage your account"
            else -> route
        }
    }
    
    /**
     * Get accessibility label for buttons
     */
    fun getButtonLabel(action: String, context: String = ""): String {
        val label = when (action) {
            "send" -> "Send"
            "call" -> "Start call"
            "video_call" -> "Start video call"
            "end_call" -> "End call"
            "mute" -> "Mute microphone"
            "unmute" -> "Unmute microphone"
            "camera_on" -> "Turn camera on"
            "camera_off" -> "Turn camera off"
            "like" -> "Like this user"
            "pass" -> "Pass on this user"
            else -> action
        }
        
        return if (context.isNotEmpty()) {
            "$label for $context"
        } else {
            "$label button"
        }
    }
}

enum class HapticFeedbackType {
    CLICK,        // Light tap for button presses
    LONG_PRESS,   // Stronger feedback for long press
    SUCCESS,      // Pattern for successful actions
    ERROR         // Pattern for errors
}
