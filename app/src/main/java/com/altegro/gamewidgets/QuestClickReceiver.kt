package com.altegro.gamewidgets

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Принимает клик по задаче в ListView виджета: помечает выполненной (переносит в историю)
 * либо возвращает из истории в активные.
 */
class QuestClickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != QuestWidgetProvider.ACTION_TOGGLE) return
        val id = intent.getStringExtra(QuestWidgetProvider.EXTRA_TASK_ID) ?: return
        TaskManager.toggleTask(context, id)
        WidgetUpdater.refreshQuests(context)
    }
}
