package com.altegro.gamewidgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.Intent

/**
 * Фоновый сборщик шагов с датчика телефона (TYPE_STEP_COUNTER / TYPE_STEP_DETECTOR).
 * Запускается из MainActivity после получения разрешения ACTIVITY_RECOGNITION.
 * На HyperOS счетчик может «спать» — поэтому параллельно тянем шаги из Health Connect.
 */
object StepSensor : SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var running = false

    fun start(context: Context) {
        if (running) return
        val sm = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
        sensorManager = sm
        val counter = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        val detector = sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        var registered = false
        counter?.let {
            sm.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            registered = true
        }
        if (!registered) detector?.let {
            sm.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            registered = true
        }
        running = registered
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
        sensorManager = null
        running = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> StatsManager.onStepCounter(
                sensorManager!!.legacyMode(), // placeholder, replaced below
                event.values[0].toInt()
            )
            Sensor.TYPE_STEP_DETECTOR -> StatsManager.onStepDetectorTick()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
