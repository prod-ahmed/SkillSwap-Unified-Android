package com.skillswap.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.skillswap.R

class WeeklyObjectiveWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAll(context, appWidgetManager, appWidgetIds, context.getSharedPreferences("SkillSwapPrefs", Context.MODE_PRIVATE))
    }

    companion object {
        fun updateAll(context: Context, manager: AppWidgetManager, ids: IntArray? = null, prefs: android.content.SharedPreferences? = null) {
            val widgetIds = ids ?: manager.getAppWidgetIds(ComponentName(context, WeeklyObjectiveWidgetProvider::class.java))
            val sharedPrefs = prefs ?: context.getSharedPreferences("SkillSwapPrefs", Context.MODE_PRIVATE)
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
