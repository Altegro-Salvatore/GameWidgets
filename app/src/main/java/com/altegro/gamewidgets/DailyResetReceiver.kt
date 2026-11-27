package com.altegro.gamewidgets

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * Сброс игровой механики в 00:00: MP обнуляется («утром 0»), шаги тоже.
 * AlarmManager с точным будильником на следующий полночь; после срабатывания
 * планируем следующий (setRepeating на exact-будильники в Android 12+ не работает стабильно).
 */
object DailyResetScheduler {

    fun schedule(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pending(context)

        val nextMidnight = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 1)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextMidnight, pi)
        } catch (_: SecurityException) {
            // На HyperOS SCHEDULE_EXACT_ALARM может быть запрещен — fallback на неточный
            alarmManager.set(AlarmManager.RTC_WAKEUP, nextMidnight, pi)
        }
    }

    private fun pending(context: Context): PendingIntent {
        return PendingIntent.getBroadcast(
            context, 777, Intent(context, DailyResetReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class DailyResetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        StatsManager.resetForNewDay(context)
        WidgetUpdater.refreshStats(context)
        WidgetUpdater.refreshQuests(context)
        WidgetUpdater.refreshClock(context)
        // Планируем следующий сброс
        DailyResetScheduler.schedule(context)
    }
}
