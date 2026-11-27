package com.altegro.gamewidgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews

// --- ПРОВАЙДЕР КВЕСТОВ ---
class QuestWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // Освобождаем сохраненные PendingIntent
        for (id in appWidgetIds) {
            PendingIntent.getBroadcast(
                context, id, Intent(context, QuestClickReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )?.cancel()
        }
    }

    companion object {
        const val ACTION_TOGGLE = "com.altegro.gamewidgets.TOGGLE_QUEST"
        const val EXTRA_TASK_ID = "task_id"

        fun updateWidget(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_quests)

            // Кнопка "+" — открывает всплывающее окно ввода задачи
            val addIntent = Intent(context, AddQuestActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val addPendingIntent = PendingIntent.getActivity(
                context, 1000 + appWidgetId, addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_add_quest, addPendingIntent)

            // Настройка прокручиваемого списка (плавная прокрутка через RemoteViewsService)
            val listIntent = Intent(context, QuestWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.quest_list, listIntent)

            // Шаблон для кликов по элементам списка (данные приходят из фабрики через fill-in intent)
            val toggleIntent = Intent(context, QuestClickReceiver::class.java).apply { action = ACTION_TOGGLE }
            val togglePi = PendingIntent.getBroadcast(
                context, appWidgetId, toggleIntent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setPendingIntentTemplate(R.id.quest_list, togglePi)

            manager.updateAppWidget(appWidgetId, views)
            manager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.quest_list)
        }
    }
}
