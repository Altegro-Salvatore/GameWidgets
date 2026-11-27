package com.altegro.gamewidgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * Централизованное обновление всех виджетов.
 * Вызывается после изменения данных (квесты, мана, шаги) и после синхронизации.
 */
object WidgetUpdater {

    fun refreshQuests(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, QuestWidgetProvider::class.java))
        if (ids.isNotEmpty()) {
            // Сначала обновляем данные RemoteViewsFactory, затем перерисовываем
            context.sendBroadcast(Intent(context, QuestWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            })
            manager.notifyAppWidgetViewDataChanged(ids, R.id.quest_list)
        }
    }

    fun refreshStats(context: Context) {
        StatsWidgetProvider.updateAll(context)
    }

    fun refreshClock(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, ClockWidgetProvider::class.java))
        if (ids.isNotEmpty()) {
            context.sendBroadcast(Intent(context, ClockWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            })
        }
    }

    fun refreshAll(context: Context) {
        refreshQuests(context)
        refreshStats(context)
        refreshClock(context)
    }
}
