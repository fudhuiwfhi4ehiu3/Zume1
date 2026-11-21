package com.zume.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth

private val ZumeBlue = Color(0xFF2496E8)
private val PaleBlue = Color(0xFFEAF6FF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(
                primary = ZumeBlue,
                background = PaleBlue,
                surface = Color.White
            )) {
                ZumeWelcome()
            }
        }
    }
}

@Composable
fun ZumeWelcome() {
    val auth = remember { FirebaseAuth.getInstance() }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegister by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Добро пожаловать в Zume!") }

    fun showError(error: Exception?) {
        isLoading = false
        message = when {
            error == null -> "Не удалось выполнить действие. Проверь интернет и попробуй ещё раз."
            error.message?.contains("already in use", ignoreCase = true) == true ->
                "Этот адрес уже зарегистрирован. Попробуй войти."
            error.message?.contains("badly formatted", ignoreCase = true) == true ->
                "Проверь правильность адреса электронной почты."
            error.message?.contains("password", ignoreCase = true) == true ->
                "Пароль должен содержать не менее 6 символов."
            error.message?.contains("network", ignoreCase = true) == true ->
                "Нет подключения к интернету. Попробуй ещё раз."
            else -> "Не получилось выполнить действие. Проверь данные и настройки Firebase."
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = PaleBlue) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Zume", color = ZumeBlue, fontSize = 44.sp, fontWeight = FontWeight.Bold)
            Text("Общайся. Делись. Будь на связи.", color = Color(0xFF52738A), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(28.dp))
            Column(
                modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(24.dp)).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isRegister) "Создать аккаунт" else "Войти",
                    fontSize = 23.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF17364A)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.trim() },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Электронная почта") },
                    singleLine = true,
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Пароль") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (email.isBlank() || !email.contains("@")) {
                            message = "Введи правильный адрес электронной почты."
                        } else if (password.length < 6) {
                            message = "Пароль должен содержать не менее 6 символов."
                        } else {
                            isLoading = true
                            message = if (isRegister) "Создаём аккаунт..." else "Проверяем данные..."
                            if (isRegister) {
                                auth.createUserWithEmailAndPassword(email, password)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            val user = auth.currentUser
                                            if (user != null) {
                                                user.sendEmailVerification().addOnCompleteListener { verifyTask ->
                                                    isLoading = false
                                                    message = if (verifyTask.isSuccessful) {
                                                        "Аккаунт создан! Открой письмо от Firebase и нажми ссылку подтверждения. Затем вернись сюда и войди."
                                                    } else {
                                                        "Аккаунт создан, но письмо не отправлено. Проверь настройки Firebase и попробуй снова."
                                                    }
                                                }
                                            } else {
                                                showError(null)
                                            }
                                        } else {
                                            showError(task.exception)
                                        }
                                    }
                            } else {
                                auth.signInWithEmailAndPassword(email, password)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            val user = auth.currentUser
                                            if (user != null) {
                                                user.reload().addOnCompleteListener { reloadTask ->
                                                    isLoading = false
                                                    if (reloadTask.isSuccessful && auth.currentUser?.isEmailVerified == true) {
                                                        message = "Почта подтверждена! Ты вошёл в Zume."
                                                    } else {
                                                        auth.signOut()
                                                        message = "Сначала подтверди почту по ссылке в письме, затем войди снова."
                                                    }
                                                }
                                            } else {
                                                showError(null)
                                            }
                                        } else {
                                            showError(task.exception)
                                        }
                                    }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = ZumeBlue)
                ) {
                    Text(if (isLoading) "Подожди..." else if (isRegister) "Зарегистрироваться" else "Войти")
                }
                TextButton(
                    enabled = !isLoading,
                    onClick = {
                        isRegister = !isRegister
                        message = if (isRegister) "Создание аккаунта" else "Вход в аккаунт"
                    }
                ) {
                    Text(if (isRegister) "Уже есть аккаунт? Войти" else "Нет аккаунта? Регистрация")
                }
                Text(message, color = Color(0xFF52738A), fontSize = 12.sp)
            }
        }
    }
}
