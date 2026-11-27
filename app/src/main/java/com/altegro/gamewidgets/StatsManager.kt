package com.altegro.gamewidgets

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Игровая механика «Статов»:
 *  - HP (Пульс): из Health Connect / Mi Fitness (или вручную),
 *  - MP (Калории): утром = 0, пополняется вручную (съеденные калории),
 *    тратится автоматически в зависимости от пройденных шагов,
 *  - Шаги: реальные данные с датчика/шагомера телефона за текущий день, сброс в 00:00.
 *
 * Данные храним в SharedPreferences (ключи совместимы со старыми: launcher_prefs/manna_points).
 */
object StatsManager {
    private const val PREFS = "launcher_prefs"
    private const val KEY_MP = "manna_points"            // текущая мана (калории)
    private const val KEY_MP_EATEN = "manna_eaten"       // сколько съедено за сегодня
    private const val KEY_STEPS = "steps_today"          // шаги за сегодня
    private const val KEY_HP = "hp_pulse"                // пульс (Health Connect / ручной ввод)
    private const val KEY_DAY = "stats_day"              // дата последнего сброса
    private const val KEY_STEP_BASELINE = "step_baseline" // базовое значение счетчика шагов
    private const val KEY_DISTANCE_KCAL = "distance_kcal" // калории, сожженные ходьбой (Health Connect)

    const val CALORIES_PER_1000_STEPS = 40               // трата: ~40 ккал на 1000 шагов
    const val STEP_GOAL = 10000                          // дневная цель по шагам
    const val MP_CAP = 2500                              // «макс. мана» для шкалы

    /**
     * Обновление шагов от датчика. TYPE_STEP_COUNTER отдает кумулятивное значение с момента
     * загрузки телефона — запоминаем базовое, чтобы показывать реальные шаги за текущий день.
     */
    fun onStepCounter(context: Context, totalSinceBoot: Int) {
        ensureDay(context)
        val p = prefs(context)
        var baseline = p.getInt(KEY_STEP_BASELINE, -1)
        if (baseline < 0) {
            baseline = totalSinceBoot // первое значение после включения телефона/приложения
            p.edit().putInt(KEY_STEP_BASELINE, baseline).apply()
        }
        val steps = (totalSinceBoot - baseline).coerceAtLeast(0)
        if (steps != readSteps(context)) {
            p.edit().putInt(KEY_STEPS, steps).apply()
            WidgetUpdater.refreshStats(context)
        }
    }

    /** Приблизительная трата калорий по пройденной дистанции */
    fun onDistance(context: Context, metersToday: Float) {
        ensureDay(context)
        val kcal = (metersToday / 1000f * 50f).toInt() // ~50 ккал/км
        prefs(context).edit().putInt(KEY_DISTANCE_KCAL, kcal).apply()
        WidgetUpdater.refreshStats(context)
    }

    /** Шаги из Health Connect точнее локального датчика — разрешаем перезапись вверх */
    fun overrideSteps(context: Context, steps: Int) {
        ensureDay(context)
        if (steps >= readSteps(context)) {
            prefs(context).edit().putInt(KEY_STEPS, steps).apply()
            WidgetUpdater.refreshStats(context)
        }
    }

    /** MP = съедено - трата (шаги + калории ходьбы из Health Connect), не ниже нуля */
    fun getMp(context: Context): Int {
        ensureDay(context)
        val eaten = prefs(context).getInt(KEY_MP_EATEN, 0)
        val stepBurn = (readSteps(context) / 1000) * CALORIES_PER_1000_STEPS
        val distanceBurn = prefs(context).getInt(KEY_DISTANCE_KCAL, 0)
        return (eaten - maxOf(stepBurn, distanceBurn)).coerceAtLeast(0)
    }

    /** Сколько уже сожжено сегодня (для отображения) */
    fun getBurned(context: Context): Int {
        ensureDay(context)
        val stepBurn = (readSteps(context) / 1000) * CALORIES_PER_1000_STEPS
        val distanceBurn = prefs(context).getInt(KEY_DISTANCE_KCAL, 0)
        return maxOf(stepBurn, distanceBurn)
    }

    fun addMp(context: Context, calories: Int) {
        ensureDay(context)
        val eaten = prefs(context).getInt(KEY_MP_EATEN, 0) + calories
        prefs(context).edit().putInt(KEY_MP_EATEN, eaten.coerceAtLeast(0)).apply()
        WidgetUpdater.refreshStats(context)
    }

    fun getSteps(context: Context): Int {
        ensureDay(context)
        return readSteps(context)
    }

    /** Пульс: сначала Health Connect (SyncManager пишет сюда), иначе ручной запасной ввод */
    fun getHp(context: Context): Int? {
        val v = prefs(context).getInt(KEY_HP, -1)
        return if (v > 0) v else null
    }

    fun setHp(context: Context, pulse: Int) {
        prefs(context).edit().putInt(KEY_HP, pulse).apply()
        WidgetUpdater.refreshStats(context)
    }

    /** Явный сброс нового дня (вызывается будильником в 00:00) */
    fun resetForNewDay(context: Context) {
        prefs(context).edit()
            .putString(KEY_DAY, "") // заставит ensureDay выполнить полный сброс
            .apply()
        ensureDay(context)
    }

    /** Сброс в 00:00: мана обнуляется (утром 0), шаги тоже */
    private fun ensureDay(context: Context): Boolean {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val p = prefs(context)
        if (p.getString(KEY_DAY, "") != today) {
            p.edit()
                .putString(KEY_DAY, today)
                .putInt(KEY_MP, 0)
                .putInt(KEY_MP_EATEN, 0)
                .putInt(KEY_STEPS, 0)
                .putInt(KEY_DISTANCE_KCAL, 0)
                .remove(KEY_STEP_BASELINE) // после сброса дня базовая точка счетчика пересоздается
                .apply()
            return false // сигнал, что данные только что сброшены
        }
        return true
    }

    private fun readSteps(context: Context): Int = prefs(context).getInt(KEY_STEPS, 0)

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Проверка разрешения ACTIVITY_RECOGNITION (на Android 10+) */
    fun hasStepPermission(context: Context): Boolean {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        // Если датчика нет — шагов не будет, но разрешение не требуется
        if (sensor == null) return true
        return context.checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}
