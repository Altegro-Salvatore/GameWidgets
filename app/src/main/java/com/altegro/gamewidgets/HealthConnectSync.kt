package com.altegro.gamewidgets

import android.content.Context
import android.content.Intent
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthConnectPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/**
 * Интеграция с Health Connect (Mi Fitness / Zepp и др. пишут туда же).
 * HP (Пульс) — последний пульс за сегодня; калории ходьбы — дистанция за сегодня.
 * Если Health Connect не установлен — тихо деградируем: шаги берем с датчика телефона,
 * HP можно ввести вручную.
 */
object HealthConnectSync {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    const val ACTION_HEALTH_CONNECT_NOT_AVAILABLE = "androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE"

    val REQUIRED_PERMISSIONS: Set<String> = setOf(
        HealthConnectPermission.PERMISSION_READ_HEART_RATE,
        HealthConnectPermission.PERMISSION_READ_DISTANCE,
        HealthConnectPermission.PERMISSION_READ_STEPS,
        HealthConnectPermission.PERMISSION_READ_CALORIES_BURNED
    )

    fun isAvailable(context: Context): Boolean {
        val packageName = HealthConnectClient.getHealthConnectDeviceInfo(context)?.packageName ?: return false
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun hasPermissions(context: Context): Boolean {
        if (!isAvailable(context)) return false
        return try {
            val client = HealthConnectClient.getSdkClient(context)
            client.permissionController.getGrantedPermissions().containsAll(REQUIRED_PERMISSIONS)
        } catch (_: Exception) {
            false
        }
    }

    /** Открывает экран настроек разрешений Health Connect */
    fun openPermissionSettings(context: Context) {
        if (!isAvailable(context)) return
        val intent = Intent(ACTION_HEALTH_CONNECT_NOT_AVAILABLE).apply {
            putExtra("extra_permissions", REQUIRED_PERMISSIONS.toTypedArray())
        }
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {}
    }

    fun refreshAsync(context: Context) {
        scope.launch {
            try {
                refreshHealthData(context)
            } catch (_: Exception) {}
        }
    }

    suspend fun refreshHealthData(context: Context) {
        if (!hasPermissions(context)) return
        val client = HealthConnectClient.getSdkClient(context)
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now().atStartOfDay(zone).toInstant()
        val end = java.time.Instant.now()
        val filter = TimeRangeFilter.between(start, end)

        // --- HP: последний пульс за сегодня ---
        try {
            val hrResponse = client.readRecords(
                ReadRecordsRequest(recordType = HeartRateRecord::class.java, timeRangeFilter = filter)
            )
            val last = hrResponse.records.maxByOrNull { it.startTime }
            last?.let { record ->
                val avgBpm = record.samples.sumOf { it.beatsPerMinute.toInt() } / record.samples.size
                if (avgBpm > 0) StatsManager.setHp(context, avgBpm)
            }
        } catch (_: Exception) {}

        // --- Шаги из Health Connect (если есть, точнее чем локальный датчик) ---
        try {
            val stepsResponse = client.readRecords(
                ReadRecordsRequest(recordType = StepsRecord::class.java, timeRangeFilter = filter)
            )
            val totalSteps = stepsResponse.records.sumOf { it.count.toInt() }
            if (totalSteps > StatsManager.getSteps(context)) {
                StatsManager.overrideSteps(context, totalSteps)
            }
        } catch (_: Exception) {}

        // --- Трата маны: дистанция/калории активности ---
        try {
            val distResponse = client.readRecords(
                ReadRecordsRequest(recordType = DistanceRecord::class.java, timeRangeFilter = filter)
            )
            val meters = distResponse.records.sumOf { it.distance.inMeters.toFloat() }
            if (meters > 0f) StatsManager.onDistance(context, meters)
        } catch (_: Exception) {}
    }
}
