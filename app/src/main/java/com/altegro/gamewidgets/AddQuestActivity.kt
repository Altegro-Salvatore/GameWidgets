package com.altegro.gamewidgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class AddQuestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var taskText by remember { mutableStateOf("") }

            Box(modifier = Modifier.fillMaxSize().background(Color(0xAA000000)), contentAlignment = Alignment.Center) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C231E)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Новый квест", color = Color(0xFFC59B4B), fontFamily = FontFamily.Serif, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = taskText,
                            onValueChange = { taskText = it },
                            label = { Text("Название", color = Color(0xFFE8D5B5)) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color(0xFFE8D5B5), unfocusedTextColor = Color(0xFFE8D5B5),
                                focusedContainerColor = Color(0xFF1A1512), unfocusedContainerColor = Color(0xFF1A1512),
                                focusedIndicatorColor = Color(0xFFC59B4B), unfocusedIndicatorColor = Color(0xFFC59B4B)
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { finish() }) { Text("Отмена", color = Color(0xFFE8D5B5)) }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (taskText.isNotBlank()) {
                                        TaskManager.addTask(this@AddQuestActivity, taskText)
                                        // Обновляем виджет
                                        val intent = Intent(this@AddQuestActivity, QuestWidgetProvider::class.java).apply { action = "ACTION_REFRESH_WIDGET" }
                                        sendBroadcast(intent)
                                    }
                                    finish()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC59B4B))
                            ) {
                                Text("Добавить", color = Color(0xFF1A1512))
                            }
                        }
                    }
                }
            }
        }
    }
}
