package com.skillswap.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.skillswap.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import com.skillswap.network.NetworkService
import com.skillswap.auth.AuthenticationManager
import com.skillswap.security.SecureStorage
import com.skillswap.util.TokenUtils

class WeeklyObjectiveWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // First update with cached data for immediate display
        val securePrefs = SecureStorage.getInstance(context)
        updateAll(context, appWidgetManager, appWidgetIds, securePrefs)
        
        // Fetch fresh data from backend
        val prefs = securePrefs
        val token = AuthenticationManager.getInstance(context).getToken()?.takeUnless { TokenUtils.isTokenExpired(it) }
        
        if (token != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val objective = NetworkService.api.getCurrentWeeklyObjective("Bearer $token")
                    val progress = if (objective.targetHours > 0) {
                        ((objective.completedHours / objective.targetHours) * 100).toInt()
                    } else 0
                    
                    // Update cache
                    prefs.edit()
                        .putString("widget_objective_title", objective.title)
                        .putInt("widget_objective_progress", progress)
                        .putLong("widget_last_update", System.currentTimeMillis())
                        .apply()
                    
                    // Update widget immediately
                    updateAll(context, appWidgetManager, appWidgetIds, prefs)
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Fallback to cached data (already displayed)
                }
            }
        } else {
            // No auth token, show default message
            prefs.edit()
                .putString("widget_objective_title", "Connectez-vous")
                .putInt("widget_objective_progress", 0)
                .apply()
            updateAll(context, appWidgetManager, appWidgetIds, prefs)
        }
    }

    companion object {
        fun updateAll(context: Context, manager: AppWidgetManager, ids: IntArray? = null, prefs: android.content.SharedPreferences? = null) {
            val widgetIds = ids ?: manager.getAppWidgetIds(ComponentName(context, WeeklyObjectiveWidgetProvider::class.java))
            val sharedPrefs = prefs ?: SecureStorage.getInstance(context)
            val title = sharedPrefs.getString("widget_objective_title", "Objectif hebdo")
            val progress = sharedPrefs.getInt("widget_objective_progress", 0)

            widgetIds.forEach { appWidgetId ->
                val views = RemoteViews(context.packageName, R.layout.widget_weekly_objective).apply {
                    setTextViewText(R.id.widget_title, title)
                    setTextViewText(R.id.widget_progress, "$progress%")
                    setProgressBar(R.id.widget_progress_bar, 100, progress, false)
                }
                manager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}
