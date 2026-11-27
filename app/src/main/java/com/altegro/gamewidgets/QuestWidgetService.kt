package com.altegro.gamewidgets

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.widget.RemoteViews
import android.widget.RemoteViewsService

class QuestWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return QuestFactory(applicationContext)
    }
}

/**
 * Фабрика элементов списка квестов.
 * Активные задачи идут первыми, выполненные — в истории (зачеркнутые).
 */
class QuestFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var tasks: List<TaskItem> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        // Сортировка: активные по времени создания, затем выполненные (история)
        tasks = TaskManager.getTasks(context).sortedWith(
            compareBy<TaskItem> { it.done }.thenByDescending { t -> if (t.done) (t.completedAt ?: 0L) else -t.createdAt }
        )
    }

    override fun onDestroy() {
        tasks = emptyList()
    }

    override fun getCount(): Int = tasks.size

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.quest_item)
        if (position >= tasks.size) return views
        val task = tasks[position]

        views.setTextViewText(R.id.quest_title, task.title)

        if (task.done) {
            // Выполненный квест: зачеркнутый, тусклый, с датой в истории
            views.setInt(R.id.quest_title, "setPaintFlags", Paint.STRIKE_THRU_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG)
            views.setTextColor(R.id.quest_title, 0xFF6E5A44.toInt())
            views.setTextViewText(R.id.btn_done, "↺") // вернуть из истории в активные
        } else {
            views.setInt(R.id.quest_title, "setPaintFlags", Paint.ANTI_ALIAS_FLAG)
            views.setTextColor(R.id.quest_title, 0xFFE8D5B5.toInt())
            views.setTextViewText(R.id.btn_done, "✓") // отметить выполненным
        }

        // Fill-in intent: передаёт id задачи в шаблон PendingIntent виджета
        val fillInIntent = Intent().apply {
            putExtra(QuestWidgetProvider.EXTRA_TASK_ID, task.id)
        }
        views.setOnClickFillInIntent(R.id.quest_row, fillInIntent)
        views.setOnClickFillInIntent(R.id.btn_done, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long =
        if (position < tasks.size) tasks[position].hashCode().toLong() else position.toLong()
    override fun hasStableIds(): Boolean = true
}
