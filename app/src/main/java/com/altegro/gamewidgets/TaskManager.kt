package com.altegro.gamewidgets

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class TaskItem(
    val id: String,
    val title: String,
    val done: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

/**
 * Хранение квестов: локально (SharedPreferences + JSON) с последующей
 * синхронизацией в Firebase Firestore, привязанной к uid пользователя.
 * Оффлайн-режим: изменения копятся в pending-очереди и отправляются при появлении сети/входа.
 */
object TaskManager {
    private const val PREFS_NAME = "game_widget_prefs"
    private const val KEY_TASKS = "tasks_list"
    private const val DONE_DISPLAY_LIMIT = 15 // сколько выполненных показываем в истории виджета

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun getTasks(context: Context): List<TaskItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_TASKS, "[]") ?: "[]"
        val list = mutableListOf<TaskItem>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    TaskItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        done = obj.optBoolean("done", false),
                        createdAt = obj.optLong("createdAt", 0L),
                        completedAt = if (obj.has("completedAt")) obj.getLong("completedAt") else null
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    /** Активные (невыполненные) квесты */
    fun getActiveTasks(context: Context): List<TaskItem> = getTasks(context).filter { !it.done }

    /** История выполненных (ограниченная, для «переноса в историю» внутри виджета) */
    fun getDoneTasks(context: Context): List<TaskItem> =
        getTasks(context).filter { it.done }.sortedByDescending { it.completedAt ?: 0L }

    fun addTask(context: Context, title: String) {
        val newTask = TaskItem(UUID.randomUUID().toString(), title.trim())
        val currentTasks = getTasks(context).toMutableList()
        currentTasks.add(newTask)
        saveLocally(context, currentTasks)
        pushTaskToCloud(newTask)
    }

    /**
     * Клик по задаче в виджете: невыполненная -> выполненная (уходит в историю),
     * выполненная -> возвращена в активные.
     */
    fun toggleTask(context: Context, id: String) {
        val currentTasks = getTasks(context).toMutableList()
        val index = currentTasks.indexOfFirst { it.id == id }
        if (index < 0) return
        val task = currentTasks[index]
        val updated = task.copy(
            done = !task.done,
            completedAt = if (!task.done) System.currentTimeMillis() else null
        )
        currentTasks[index] = updated
        saveLocally(context, currentTasks)
        pushTaskToCloud(updated)
    }

    fun removeTask(context: Context, id: String) {
        val currentTasks = getTasks(context).toMutableList()
        currentTasks.removeAll { it.id == id }
        saveLocally(context, currentTasks)

        auth.currentUser?.let { user ->
            db.collection("users").document(user.uid).collection("tasks")
                .document(id).delete()
        }
    }

    private fun pushTaskToCloud(task: TaskItem) {
        val user = auth.currentUser ?: return // оффлайн/без аккаунта — только локально
        db.collection("users").document(user.uid).collection("tasks")
            .document(task.id)
            .set(
                mapOf(
                    "title" to task.title,
                    "done" to task.done,
                    "createdAt" to task.createdAt,
                    "completedAt" to (task.completedAt ?: 0L)
                ),
                SetOptions.merge()
            )
    }

    /**
     * Синхронизация с облаком: подтягиваем задачи из Firestore в локальную память,
     * а локальные (созданные оффлайн) — отправляем в облако.
     */
    fun syncFromCloud(context: Context, onComplete: () -> Unit = {}) {
        val user = auth.currentUser ?: run { onComplete(); return }

        db.collection("users").document(user.uid).collection("tasks")
            .get()
            .addOnSuccessListener { result ->
                val cloudTasks = result.documents.mapNotNull { doc ->
                    val title = doc.getString("title") ?: return@mapNotNull null
                    TaskItem(
                        id = doc.id,
                        title = title,
                        done = doc.getBoolean("done") ?: false,
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        completedAt = doc.getLong("completedAt")?.takeIf { it > 0 }
                    )
                }
                // Merge: локальные + облачные (по id, облако приоритетнее по статусу)
                val merged = LinkedHashMap<String, TaskItem>()
                getTasks(context).forEach { merged[it.id] = it }
                cloudTasks.forEach { merged[it.id] = it }
                val mergedList = merged.values.sortedBy { it.createdAt }
                saveLocally(context, mergedList)

                // Отправка того, чего нет в облаке (оффлайн-созданные)
                val cloudIds = cloudTasks.map { it.id }.toSet()
                mergedList.filter { it.id !in cloudIds }.forEach { pushTaskToCloud(it) }

                onComplete()
            }
            .addOnFailureListener {
                onComplete() // если ошибка сети — оставляем локальные данные
            }
    }

    private fun saveLocally(context: Context, tasks: List<TaskItem>) {
        val trimmed = ArrayList(tasks)
        // В виджете показываем не всю историю выполненных, а последние N
        val done = trimmed.filter { it.done }.sortedByDescending { it.completedAt ?: 0L }
        if (done.size > DONE_DISPLAY_LIMIT) {
            val keep = done.take(DONE_DISPLAY_LIMIT).map { it.id }.toSet()
            trimmed.removeAll { it.done && it.id !in keep }
        }

        val jsonArray = JSONArray()
        trimmed.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("title", it.title)
            obj.put("done", it.done)
            obj.put("createdAt", it.createdAt)
            it.completedAt?.let { c -> obj.put("completedAt", c) }
            jsonArray.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_TASKS, jsonArray.toString()).apply()
    }

    fun formatDate(ts: Long?): String {
        if (ts == null || ts <= 0) return ""
        return SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(ts))
    }
}
