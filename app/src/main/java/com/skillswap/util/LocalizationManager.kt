package com.skillswap.util

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale

enum class AppLanguage(val code: String, val displayName: String, val flag: String) {
    FRENCH("fr", "Français", "🇫🇷"),
    ENGLISH("en", "English", "🇬🇧"),
    ARABIC("ar", "العربية", "🇹🇳");

    val isRTL: Boolean get() = this == ARABIC

    companion object {
        fun fromCode(code: String): AppLanguage = values().find { it.code == code } ?: FRENCH
    }
}

class LocalizationManager private constructor(private val context: Context) {
    private val prefs = context.getSharedPreferences("SkillSwapPrefs", Context.MODE_PRIVATE)
    
    var currentLanguage = mutableStateOf(
        AppLanguage.fromCode(prefs.getString("app_language", "fr") ?: "fr")
    )
        private set

    val layoutDirection: LayoutDirection
        get() = if (currentLanguage.value.isRTL) LayoutDirection.Rtl else LayoutDirection.Ltr

    fun setLanguage(language: AppLanguage) {
        currentLanguage.value = language
        prefs.edit().putString("app_language", language.code).apply()
    }

    companion object {
        @Volatile
        private var instance: LocalizationManager? = null

        fun getInstance(context: Context): LocalizationManager {
            return instance ?: synchronized(this) {
                instance ?: LocalizationManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

// Localization strings helper (extensible for future full i18n)
object LocalizedStrings {
    // Profile
    const val PROFILE = "Profil"
    const val DARK_MODE = "Mode sombre"
    const val LANGUAGE = "Langue"
    const val SESSIONS_FOR_YOU = "Sessions pour vous"
    const val REFER_FRIEND = "Référez un ami"
    const val SHARE_PROFILE = "Partager mon profil"
    const val MY_ANNOUNCEMENTS = "Mes annonces"
    const val MY_PROMOS = "Mes promos"
    const val SETTINGS = "Paramètres"
    const val LOGOUT = "Se déconnecter"
    
    // Common
    const val CANCEL = "Annuler"
    const val SAVE = "Enregistrer"
    const val ERROR = "Erreur"
    const val SUCCESS = "Succès"
    const val LOADING = "Chargement..."
    
    // Tabs
    const val DISCOVER = "Découvrir"
    const val MESSAGES = "Messages"
    const val SESSIONS = "Sessions"
    const val PROGRESS = "Progrès"
    const val MAP = "Carte"
    
    // Auth
    const val EMAIL = "Email"
    const val PASSWORD = "Mot de passe"
    const val FORGOT_PASSWORD = "Mot de passe oublié ?"
    const val SIGN_UP = "S'inscrire"
    const val SIGN_IN = "Se connecter"
    const val LOGIN = "Se connecter"
    
    // Notifications
    const val NOTIFICATIONS = "Notifications"
    const val MARK_ALL_READ = "Tout marquer comme lu"
    const val NO_NOTIFICATIONS = "Aucune notification"
}
