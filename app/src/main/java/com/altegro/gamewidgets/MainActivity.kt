package com.altegro.gamewidgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth

val ColorBg = Color(0xFF1A1512)
val ColorCardBg = Color(0xFF2C231E)
val ColorGold = Color(0xFFC59B4B)
val ColorParchment = Color(0xFFE8D5B5)

data class WidgetItem(val title: String, val description: String, val providerClass: Class<*>)

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val auth = FirebaseAuth.getInstance()
            var currentUser by remember { mutableStateOf(auth.currentUser) }
            
            // Состояния для окон входа
            var showAuthDialog by remember { mutableStateOf(false) }

            // Если пользователь вошел, синхронизируем данные
            LaunchedEffect(currentUser) {
                if (currentUser != null) {
                    TaskManager.syncFromCloud(context) {
                        // Обновляем виджеты после синхронизации
                        val intent = android.content.Intent(context, QuestWidgetProvider::class.java).apply { action = AppWidgetManager.ACTION_APPWIDGET_UPDATE }
                        val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, QuestWidgetProvider::class.java))
                        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                        context.sendBroadcast(intent)
                    }
                }
            }

            if (showAuthDialog) {
                AuthDialog(
                    onDismiss = { showAuthDialog = false },
                    onAuthSuccess = { 
                        currentUser = auth.currentUser
                        showAuthDialog = false 
                    }
                )
            }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("GameWidgets", color = ColorGold, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold) },
                        actions = {
                            Button(
                                onClick = { 
                                    if (currentUser != null) {
                                        auth.signOut()
                                        currentUser = null
                                    } else {
                                        showAuthDialog = true
                                    }
                                }, 
                                colors = ButtonDefaults.buttonColors(containerColor = ColorCardBg), 
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(if (currentUser != null) "Выйти" else "Войти", color = ColorGold, fontFamily = FontFamily.Serif)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorBg)
                    )
                },
                containerColor = ColorBg
            ) { paddingValues ->
                Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                    if (currentUser == null) {
                        Text("Войдите в аккаунт, чтобы синхронизировать квесты между устройствами.", color = ColorParchment, fontSize = 12.sp, modifier = Modifier.padding(16.dp))
                    } else {
                        Text("Вы вошли как: ${currentUser?.email}", color = ColorGold, fontSize = 12.sp, modifier = Modifier.padding(16.dp))
                    }
                    
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(listOf(
                            WidgetItem("Квесты", "Список ваших задач", QuestWidgetProvider::class.java),
                            WidgetItem("Статы", "HP, MP и Шаги", StatsWidgetProvider::class.java),
                            WidgetItem("Часы", "Текущее время", ClockWidgetProvider::class.java)
                        )) { widget ->
                            Card(colors = CardDefaults.cardColors(containerColor = ColorCardBg), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(widget.title, color = ColorGold, fontFamily = FontFamily.Serif, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    Text(widget.description, color = ColorParchment, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(onClick = {
                                        val appWidgetManager = AppWidgetManager.getInstance(context)
                                        if (appWidgetManager.isRequestPinAppWidgetSupported) {
                                            val provider = ComponentName(context, widget.providerClass)
                                            appWidgetManager.requestPinAppWidget(provider, null, null)
                                        }
                                    }, colors = ButtonDefaults.buttonColors(containerColor = ColorGold), modifier = Modifier.fillMaxWidth()) {
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

// Диалоговое окно Входа/Регистрации
@Composable
fun AuthDialog(onDismiss: () -> Unit, onAuthSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoginMode by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val auth = FirebaseAuth.getInstance()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ColorCardBg,
        title = { Text(if (isLoginMode) "Вход" else "Регистрация", color = ColorGold, fontFamily = FontFamily.Serif) },
        text = {
            Column {
                OutlinedTextField(
                    value = email, onValueChange = { email = it }, label = { Text("Email", color = ColorParchment) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(focusedTextColor = ColorParchment, unfocusedTextColor = ColorParchment, focusedContainerColor = ColorBg, unfocusedContainerColor = ColorBg)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password, onValueChange = { password = it }, label = { Text("Пароль", color = ColorParchment) },
                    singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(focusedTextColor = ColorParchment, unfocusedTextColor = ColorParchment, focusedContainerColor = ColorBg, unfocusedContainerColor = ColorBg)
                )
                if (errorMessage != null) {
                    Text(errorMessage!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) { errorMessage = "Заполните все поля"; return@Button }
                    isLoading = true
                    errorMessage = null
                    if (isLoginMode) {
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                isLoading = false
                                if (task.isSuccessful) onAuthSuccess() else errorMessage = task.exception?.message
                            }
                    } else {
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                isLoading = false
                                if (task.isSuccessful) onAuthSuccess() else errorMessage = task.exception?.message
                            }
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = ColorGold)
            ) { if (isLoading) CircularProgressIndicator(color = ColorBg, modifier = Modifier.size(20.dp)) else Text(if (isLoginMode) "Войти" else "Создать", color = ColorBg) }
        },
        dismissButton = {
            TextButton(onClick = { isLoginMode = !isLoginMode }) { Text(if (isLoginMode) "Нет аккаунта? Зарегистрироваться" else "Уже есть аккаунт? Войти", color = ColorGold) }
        }
    )
}
