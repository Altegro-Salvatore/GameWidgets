package com.altegro.gamewidgets

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class TaskItem(val id: String, val title: String)

object TaskManager {
    private const val PREFS_NAME = "game_widget_prefs"
    private const val KEY_TASKS = "tasks_list"
    
    // Наши облачные инструменты
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun getTasks(context: Context): List<TaskItem> {
        // Читаем из локальной памяти (для виджета)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_TASKS, "[]") ?: "[]"
        val list = mutableListOf<TaskItem>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(TaskItem(obj.getString("id"), obj.getString("title")))
            }
        } catch (e: Exception) {}
        return list
    }

    fun addTask(context: Context, title: String) {
        val newTask = TaskItem(UUID.randomUUID().toString(), title)
        val currentTasks = getTasks(context).toMutableList()
        currentTasks.add(newTask)
        saveLocally(context, currentTasks)
        
        // Отправляем в облако, если пользователь вошел
        val user = auth.currentUser
        if (user != null) {
            db.collection("users").document(user.uid).collection("tasks")
                .document(newTask.id)
                .set(mapOf("title" to newTask.title))
        }
    }

    fun removeTask(context: Context, id: String) {
        val currentTasks = getTasks(context).toMutableList()
        currentTasks.removeAll { it.id == id }
        saveLocally(context, currentTasks)
        
        // Удаляем из облака
        val user = auth.currentUser
        if (user != null) {
            db.collection("users").document(user.uid).collection("tasks")
                .document(id).delete()
        }
    }

    // Синхронизация: загружаем задачи из облака и сохраняем в память телефона
    fun syncFromCloud(context: Context, onComplete: () -> Unit) {
        val user = auth.currentUser
        if (user == null) { onComplete(); return }
        
        db.collection("users").document(user.uid).collection("tasks")
            .get()
            .addOnSuccessListener { result ->
                val cloudTasks = result.map { doc ->
                    TaskItem(doc.id, doc.getString("title") ?: "")
                }
                saveLocally(context, cloudTasks)
                onComplete()
            }
            .addOnFailureListener {
                onComplete() // Если ошибка сети, оставляем локальные
            }
    }

    private fun saveLocally(context: Context, tasks: List<TaskItem>) {
        val jsonArray = JSONArray()
        tasks.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("title", it.title)
            jsonArray.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_TASKS, jsonArray.toString()).apply()
    }
}
