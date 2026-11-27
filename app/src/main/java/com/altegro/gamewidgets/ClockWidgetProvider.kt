package com.altegro.gamewidgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- ПРОВАЙДЕР ЧАСОВ ---
// Сам циферблат — TextClock: он обновляется системой автоматически без будильников.
// Провайдер нужен только для первоначальной отрисовки даты и золотой рамки.
class ClockWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            render(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        private val RU_DAYS = mapOf(
            "Mon" to "Понедельник", "Tue" to "Вторник", "Wed" to "Среда",
            "Thu" to "Четверг", "Fri" to "Пятница", "Sat" to "Суббота", "Sun" to "Воскресенье"
        )
        private val RU_MONTHS = mapOf(
            "Jan" to "января", "Feb" to "февраля", "Mar" to "марта", "Apr" to "апреля",
            "May" to "мая", "Jun" to "июня", "Jul" to "июля", "Aug" to "августа",
            "Sep" to "сентября", "Oct" to "октября", "Nov" to "ноября", "Dec" to "декабря"
        )

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ClockWidgetProvider::class.java))
            if (ids.isEmpty()) return
            context.sendBroadcast(Intent(context, ClockWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            })
        }

        fun render(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_clock)
            views.setTextViewText(R.id.tv_clock_date, prettyDate())
            manager.updateAppWidget(appWidgetId, views)
        }

        /** Дата в стиле RPG-дневника: «Суббота, 28 ноября» */
        fun prettyDate(): String {
            val raw = SimpleDateFormat("EEE, d MMM", Locale.US).format(Date())
            val parts = raw.split(", ", limit = 2)
            val day = RU_DAYS[parts[0]] ?: parts[0]
            var rest = if (parts.size > 1) parts[1] else ""
            for ((en, ru) in RU_MONTHS) rest = rest.replace(en, ru)
            return "$day, $rest"
        }
    }
}
