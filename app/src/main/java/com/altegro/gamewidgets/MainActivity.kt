package com.altegro.gamewidgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Наша палитра
val ColorBg = Color(0xFF1A1512)
val ColorCardBg = Color(0xFF2C231E)
val ColorGold = Color(0xFFC59B4B)
val ColorParchment = Color(0xFFE8D5B5)

// Модель данных для списка виджетов
data class WidgetItem(val title: String, val description: String, val providerClass: Class<*>)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            
            // Пока это просто заглушка. (Позже мы свяжем это с Firebase)
            var isLoggedIn by remember { mutableStateOf(false) }

            // Список наших виджетов (сюда будем добавлять новые)
            val widgets = listOf(
                WidgetItem("Квесты", "Список ваших задач", QuestWidgetProvider::class.java),
                WidgetItem("Статы", "HP, MP и Шаги", StatsWidgetProvider::class.java),
                WidgetItem("Часы", "Текущее время", ClockWidgetProvider::class.java)
            )

            Scaffold(
                topBar = {
                    // --- ВЕРХНЯЯ ПАНЕЛЬ ---
                    TopAppBar(
                        title = { Text("GameWidgets", color = ColorGold, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold) },
                        actions = {
                            // Кнопка Войти / Выйти
                            Button(
                                onClick = { isLoggedIn = !isLoggedIn }, 
                                colors = ButtonDefaults.buttonColors(containerColor = ColorCardBg),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(if (isLoggedIn) "Выйти" else "Войти", color = ColorGold, fontFamily = FontFamily.Serif)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorBg)
                    )
                },
                containerColor = ColorBg // Цвет фона всего приложения
            ) { paddingValues ->
                // --- ОСНОВНОЙ СПИСОК ---
                Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                    
                    // Текст-подсказка, если не вошел в аккаунт
                    if (!isLoggedIn) {
                        Text(
                            "Войдите в аккаунт, чтобы синхронизировать квесты между устройствами.",
                            color = ColorParchment, fontSize = 12.sp, modifier = Modifier.padding(16.dp)
                        )
                    }

                    // Список карточек с виджетами
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp), 
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(widgets) { widget ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = ColorCardBg),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(widget.title, color = ColorGold, fontFamily = FontFamily.Serif, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    Text(widget.description, color = ColorParchment, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // Кнопка добавления виджета на экран
                                    Button(
                                        onClick = {
                                            val appWidgetManager = AppWidgetManager.getInstance(context)
                                            // Проверяем, поддерживает ли телефон автоматическое добавление
                                            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                                                val provider = ComponentName(context, widget.providerClass)
                                                appWidgetManager.requestPinAppWidget(provider, null, null)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ColorGold),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Добавить на экран", color = ColorBg, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
