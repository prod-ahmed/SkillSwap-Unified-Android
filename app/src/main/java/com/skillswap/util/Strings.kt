package com.skillswap.util

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import com.skillswap.R

/**
 * Utility object for accessing string resources in a type-safe way.
 * Use stringResource(R.string.xxx) in Composable functions,
 * or Strings.get(context, R.string.xxx) outside of Composable context.
 */
object Strings {
    fun get(context: Context, @StringRes resId: Int): String {
        return context.getString(resId)
    }
    
    fun get(context: Context, @StringRes resId: Int, vararg formatArgs: Any): String {
        return context.getString(resId, *formatArgs)
    }
}

/**
 * Common string resource IDs for easy access
 */
object StringRes {
    // Common
    val ok = R.string.ok
    val cancel = R.string.cancel
    val delete = R.string.delete
    val edit = R.string.edit
    val save = R.string.save
    val add = R.string.add
    val close = R.string.close
    val back = R.string.back
    val search = R.string.search
    val loading = R.string.loading
    val error = R.string.error
    val success = R.string.success
    val refresh = R.string.refresh
    val apply = R.string.apply
    val confirm = R.string.confirm
    
    // Auth
    val login = R.string.login
    val signup = R.string.signup
    val logout = R.string.logout
    val email = R.string.email
    val password = R.string.password
    
    // Navigation
    val navHome = R.string.nav_home
    val navDiscover = R.string.nav_discover
    val navMessages = R.string.nav_messages
    val navCalendar = R.string.nav_calendar
    val navProfile = R.string.nav_profile
    val navProgress = R.string.nav_progress
    val navNotifications = R.string.nav_notifications
    val navSettings = R.string.nav_settings
    
    // Messages
    val typeMessage = R.string.type_message
    val send = R.string.send
    val planSession = R.string.plan_session
    
    // Calendar
    val newEvent = R.string.new_event
    val deleteEvent = R.string.delete_event
    val deleteEventConfirm = R.string.delete_event_confirm
    
    // Profile
    val editProfile = R.string.edit_profile
    val mySkills = R.string.my_skills
    
    // Progress
    val goals = R.string.goals
    val deleteGoal = R.string.delete_goal
    val deleteGoalConfirm = R.string.delete_goal_confirm
    val weeklyObjective = R.string.weekly_objective
    
    // Settings
    val settingsTitle = R.string.settings_title
    val appearance = R.string.appearance
    val language = R.string.language
    val darkMode = R.string.dark_mode
    val lightMode = R.string.light_mode
    val systemDefault = R.string.system_default
    val logoutConfirm = R.string.logout_confirm
    
    // Promos
    val myPromos = R.string.my_promos
    val createPromo = R.string.create_promo
    val deletePromo = R.string.delete_promo
    val deletePromoConfirm = R.string.delete_promo_confirm
    
    // Annonces
    val myAnnonces = R.string.my_annonces
    val createAnnonce = R.string.create_annonce
    val deleteAnnonce = R.string.delete_annonce
    val deleteAnnonceConfirm = R.string.delete_annonce_confirm
    
    // AI
    val generateWithAi = R.string.generate_with_ai
    val aiGenerating = R.string.ai_generating
    val aiError = R.string.ai_error
    
    // Errors
    val networkError = R.string.network_error
    val unknownError = R.string.unknown_error
    val fieldRequired = R.string.field_required
}
