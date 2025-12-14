package com.skillswap.util

import android.content.Context
import androidx.compose.runtime.mutableStateOf

enum class ThemePreference(val value: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromValue(value: String): ThemePreference = 
            values().find { it.value == value } ?: SYSTEM
    }
}

class ThemeManager private constructor(private val context: Context) {
    private val prefs = context.getSharedPreferences("SkillSwapPrefs", Context.MODE_PRIVATE)
    
    var themePreference = mutableStateOf(
        ThemePreference.fromValue(prefs.getString("theme_preference", "system") ?: "system")
    )
        private set

    fun setTheme(theme: ThemePreference) {
        themePreference.value = theme
        prefs.edit().putString("theme_preference", theme.value).apply()
    }

    companion object {
        @Volatile
        private var instance: ThemeManager? = null

        fun getInstance(context: Context): ThemeManager {
            return instance ?: synchronized(this) {
                instance ?: ThemeManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
