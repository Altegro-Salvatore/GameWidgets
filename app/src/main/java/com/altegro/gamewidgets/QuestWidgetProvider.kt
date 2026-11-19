package com.altegro.gamewidgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews

class QuestWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "ACTION_REFRESH_WIDGET" || intent.action == "ACTION_DONE_QUEST") {
            if (intent.action == "ACTION_DONE_QUEST") {
                val taskId = intent.getStringExtra("task_id")
                if (taskId != null) TaskManager.removeTask(context, taskId)
            }
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = android.content.ComponentName(context, QuestWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            onUpdate(context, appWidgetManager, allWidgetIds)
        }
    }

    companion object {
        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_quests)
            val tasks = TaskManager.getTasks(context)

            // Кнопка "+"
            val addIntent = Intent(context, AddQuestActivity::class.java)
            val addPendingIntent = PendingIntent.getActivity(context, 0, addIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.btn_add_quest, addPendingIntent)

            // Отображение первых 3 задач
            val taskViews = intArrayOf(R.id.task_1, R.id.task_2, R.id.task_3)
            for (i in taskViews.indices) {
                if (i < tasks.size) {
                    val task = tasks[i]
                    views.setTextViewText(taskViews[i], "• ${task.title}")
                    views.setViewVisibility(taskViews[i], View.VISIBLE)
                    
                    // По клику на задачу - удаляем её
                    val doneIntent = Intent(context, QuestWidgetProvider::class.java).apply {
                        action = "ACTION_DONE_QUEST"
                        putExtra("task_id", task.id)
                    }
                    val donePendingIntent = PendingIntent.getBroadcast(context, task.id.hashCode(), doneIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    views.setOnClickPendingIntent(taskViews[i], donePendingIntent)
                } else {
                    views.setViewVisibility(taskViews[i], View.GONE)
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
