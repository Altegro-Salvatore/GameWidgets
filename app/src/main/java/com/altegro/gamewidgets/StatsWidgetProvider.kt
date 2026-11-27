package com.altegro.gamewidgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.widget.RemoteViews

// --- ПРОВАЙДЕР СТАТОВ ---
class StatsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            render(context, appWidgetManager, appWidgetId)
        }
        // Фоновая попытка подтянуть пульс/калории из Health Connect (если подключен)
        HealthConnectSync.refreshAsync(context)
    }

    override fun onEnabled(context: Context) {
        DailyResetScheduler.schedule(context)
    }

    companion object {
        private val BAR_HEIGHT_PX = 24 // фиксированная высота полоски для bitmap-заливки

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, StatsWidgetProvider::class.java))
            if (ids.isEmpty()) return
            for (id in ids) render(context, manager, id)
        }

        fun render(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_stats)

            val hp = StatsManager.getHp(context)
            val mp = StatsManager.getMp(context)
            val steps = StatsManager.getSteps(context)
            val burned = StatsManager.getBurned(context)

            // Числа в сферах
            views.setTextViewText(R.id.tv_hp_value, hp?.toString() ?: "--")
            views.setTextViewText(R.id.tv_mp_value, if (mp >= 1000) "${"%.1f".format(mp / 1000.0)}k" else mp.toString())
            views.setTextViewText(R.id.tv_steps_value, if (steps >= 10000) "${steps / 1000}k" else steps.toString())

            // Подписи
            views.setTextViewText(R.id.tv_hp, "HP (Пульс): ${hp?.toString() ?: "?"}")
            views.setTextViewText(R.id.tv_mp, "MP (Калории): $mp  (сожжено: $burned)")
            views.setTextViewText(R.id.tv_steps, "Шаги: $steps / ${StatsManager.STEP_GOAL}")

            // Полоски-заливки рисуем битмапом — единственный способ менять «процент» в RemoteViews
            views.setImageViewBitmap(R.id.bar_hp, makeBar(hpPercent(hp), 0xFFD9534F.toInt(), 0xFF8F2B28.toInt()))
            views.setImageViewBitmap(R.id.bar_mp, makeBar(mp.toFloat() / StatsManager.MP_CAP, 0xFF5BC0DE.toInt(), 0xFF2A6C80.toInt()))
            views.setImageViewBitmap(R.id.bar_steps, makeBar(steps.toFloat() / StatsManager.STEP_GOAL, 0xFFC59B4B.toInt(), 0xFF7A5C26.toInt()))

            // Тап по «Пополнить ману» — всплывающее окно ввода съеденных калорий
            val addMpIntent = Intent(context, AddMpActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            views.setOnClickPendingIntent(
                R.id.btn_add_mp,
                PendingIntent.getActivity(
                    context, 2000 + appWidgetId, addMpIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            manager.updateAppWidget(appWidgetId, views)
        }

        /** Условная шкала «жизни»: пульс 40..180 => чем ближе к покою, тем полнее HP */
        private fun hpPercent(hp: Int?): Float {
            if (hp == null) return 0f
            return (1f - (hp - 40).coerceIn(0, 140) / 140f).coerceIn(0.05f, 1f)
        }

        private fun makeBar(ratio: Float, colorEnd: Int, colorStart: Int): Bitmap? {
            val widthPx = 600
            val bmp = Bitmap.createBitmap(widthPx, BAR_HEIGHT_PX, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val clamped = ratio.coerceIn(0f, 1f)
            if (clamped <= 0.01f) return bmp // пустая полоска
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.shader = LinearGradient(
                0f, 0f, widthPx.toFloat(), 0f,
                colorStart, colorEnd, Shader.TileMode.CLAMP
            )
            val rect = RectF(2f, 2f, (widthPx * clamped).coerceAtLeast(12f), BAR_HEIGHT_PX - 2f)
            canvas.drawRoundRect(rect, 10f, 10f, paint)
            return bmp
        }
    }
}
