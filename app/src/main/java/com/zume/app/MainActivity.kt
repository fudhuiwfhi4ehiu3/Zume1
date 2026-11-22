package com.zume.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.DisposableEffect
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth

private val ZumeBlue = Color(0xFF8154D9)
private val PaleBlue = Color(0xFFFFF7FC)
private val RainbowBrush = Brush.horizontalGradient(
    listOf(
        Color(0xFFFF5B8D),
        Color(0xFFFFA64D),
        Color(0xFFFFD84D),
        Color(0xFF55C98B),
        Color(0xFF42BDE8),
        Color(0xFF8154D9)
    )
)

private data class FeedPost(val id: String, val author: String, val text: String, val likedBy: List<String>)

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
    var isSignedIn by remember { mutableStateOf(auth.currentUser?.isEmailVerified == true) }
    var message by remember { mutableStateOf("Добро пожаловать в Zume!") }

    if (isSignedIn) {
        ZumeHome(onSignOut = {
            auth.signOut()
            isSignedIn = false
            message = "Ты вышел из аккаунта."
        })
        return
    }

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
            Text("Zume", style = TextStyle(brush = RainbowBrush), fontSize = 48.sp, fontWeight = FontWeight.ExtraBold)
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
                                                        "Аккаунт создан! Открой письмо и нажми ссылку подтверждения, затем войди."
                                                    } else {
                                                        "Аккаунт создан, но письмо не отправлено. Проверь настройки Firebase."
                                                    }
                                                }
                                            } else showError(null)
                                        } else showError(task.exception)
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
                                                        isSignedIn = true
                                                    } else {
                                                        auth.signOut()
                                                        message = "Сначала подтверди почту по ссылке в письме, затем войди снова."
                                                    }
                                                }
                                            } else showError(null)
                                        } else showError(task.exception)
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


@Composable
private fun FeedScreen() {
    val user = FirebaseAuth.getInstance().currentUser
    val db = remember { FirebaseFirestore.getInstance() }
    var postText by remember { mutableStateOf("") }
    var posts by remember { mutableStateOf<List<FeedPost>>(emptyList()) }
    var status by remember { mutableStateOf("Загружаем ленту...") }

    DisposableEffect(user?.uid) {
        if (user == null) {
            status = "Войди в аккаунт, чтобы смотреть и публиковать посты."
            onDispose { }
        } else {
            val registration = db.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        status = "Не удалось загрузить ленту. Проверь правила Firestore."
                    } else if (snapshot != null) {
                        posts = snapshot.documents.map { doc ->
                            FeedPost(
                                id = doc.id,
                                author = doc.getString("author") ?: "Пользователь Zume",
                                text = doc.getString("text") ?: "",
                                likedBy = (doc.get("likedBy") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                            )
                        }
                        status = if (posts.isEmpty()) "Пока нет постов. Опубликуй первый!" else ""
                    }
                }
            onDispose { registration.remove() }
        }
    }

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Лента Zume", color = Color(0xFF17364A), fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = postText,
            onValueChange = { postText = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("О чём хочешь рассказать?") },
            placeholder = { Text("Напиши новый пост...") }
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                val current = user
                val content = postText.trim()
                if (current != null && content.isNotEmpty()) {
                    db.collection("posts").add(
                        hashMapOf(
                            "author" to (current.email?.substringBefore("@") ?: "Пользователь"),
                            "authorUid" to current.uid,
                            "text" to content,
                            "likedBy" to emptyList<String>(),
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    ).addOnSuccessListener { postText = "" }
                        .addOnFailureListener { status = "Не удалось опубликовать. Проверь настройки Firestore." }
                }
            },
            enabled = user != null && postText.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = ZumeBlue)
        ) { Text("Опубликовать") }
        Spacer(Modifier.height(12.dp))
        if (status.isNotBlank()) Text(status, color = Color(0xFF7893A5), fontSize = 13.sp)
        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(posts, key = { it.id }) { post ->
                PostCard(post = post, userUid = user?.uid, db = db)
            }
        }
    }
}

private data class PostComment(val id: String, val author: String, val text: String)

@Composable
private fun PostCard(post: FeedPost, userUid: String?, db: FirebaseFirestore) {
    var commentText by remember(post.id) { mutableStateOf("") }
    var comments by remember(post.id) { mutableStateOf<List<PostComment>>(emptyList()) }
    var commentsStatus by remember(post.id) { mutableStateOf("") }
    DisposableEffect(post.id) {
        val registration = db.collection("posts").document(post.id)
            .collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    commentsStatus = "Комментарии пока недоступны."
                } else if (snapshot != null) {
                    comments = snapshot.documents.map { doc ->
                        PostComment(
                            id = doc.id,
                            author = doc.getString("author") ?: "Пользователь",
                            text = doc.getString("text") ?: ""
                        )
                    }
                    commentsStatus = ""
                }
            }
        onDispose { registration.remove() }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
            .background(Color.White, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Text("@${post.author}", color = ZumeBlue, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(post.text, color = Color(0xFF17364A), fontSize = 15.sp)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val liked = userUid != null && post.likedBy.contains(userUid)
            TextButton(
                onClick = {
                    val current = userUid ?: return@TextButton
                    val update = if (liked) FieldValue.arrayRemove(current) else FieldValue.arrayUnion(current)
                    db.collection("posts").document(post.id).update("likedBy", update)
                },
                enabled = userUid != null
            ) { Text(if (liked) "♥ Нравится" else "♡ Нравится", color = ZumeBlue) }
            Text("${post.likedBy.size} лайков", color = Color(0xFF7893A5), fontSize = 12.sp)
            Text("  ·  ${comments.size} комментариев", color = Color(0xFF7893A5), fontSize = 12.sp)
        }
        if (comments.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            comments.forEach { comment ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("@${comment.author}", color = ZumeBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(comment.text, color = Color(0xFF36566B), fontSize = 13.sp)
                }
            }
        }
        if (commentsStatus.isNotBlank()) {
            Text(commentsStatus, color = Color(0xFF7893A5), fontSize = 11.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = commentText,
                onValueChange = { if (it.length <= 500) commentText = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Написать комментарий") }
            )
            TextButton(
                enabled = userUid != null && commentText.isNotBlank(),
                onClick = {
                    val current = FirebaseAuth.getInstance().currentUser ?: return@TextButton
                    val text = commentText.trim()
                    if (text.isNotEmpty()) {
                        db.collection("posts").document(post.id).collection("comments").add(
                            hashMapOf(
                                "author" to (current.email?.substringBefore("@") ?: "Пользователь"),
                                "authorUid" to current.uid,
                                "text" to text,
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                        ).addOnSuccessListener { commentText = "" }
                            .addOnFailureListener { commentsStatus = "Не удалось отправить комментарий." }
                    }
                }
            ) { Text("Отправить", color = ZumeBlue) }
        }
    }
}

@Composable
fun ZumeHome(onSignOut: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    var friendQuery by remember { mutableStateOf("") }
    var profileName by remember { mutableStateOf("") }
    var profileBio by remember { mutableStateOf("") }
    var hashtagInput by remember { mutableStateOf("") }
    var hashtags by remember { mutableStateOf(listOf<String>()) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val videoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Toast.makeText(context, "Видео записано", Toast.LENGTH_SHORT).show()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = PaleBlue) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().background(RainbowBrush).padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Zume", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                TextButton(onClick = onSignOut) { Text("Выйти", color = Color.White) }
            }
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (selectedTab) {
                    0 -> FeedScreen()
                    1 -> {
                        Text("Переписки", color = Color(0xFF17364A), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Text("Здесь будут твои чаты.", color = Color(0xFF52738A), fontSize = 16.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Чаты пока не подключены к базе сообщений.", color = Color(0xFF7893A5), fontSize = 12.sp)
                    }
                    2 -> {
                        Text("Снять видео", color = Color(0xFF17364A), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Text("Нажми кнопку, чтобы открыть камеру.", color = Color(0xFF52738A), fontSize = 15.sp)
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = {
                                val intent = Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE)
                                if (intent.resolveActivity(context.packageManager) != null) {
                                    videoLauncher.launch(intent)
                                } else {
                                    Toast.makeText(context, "На устройстве не найдена камера для записи видео", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ZumeBlue)
                        ) { Text("●  Начать запись") }
                    }
                    3 -> {
                        Text("Найти друзей", color = Color(0xFF17364A), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Text("Найди людей по имени пользователя", color = Color(0xFF52738A), fontSize = 14.sp)
                        Spacer(Modifier.height(18.dp))
                        OutlinedTextField(
                            value = friendQuery,
                            onValueChange = { friendQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Имя пользователя") },
                            placeholder = { Text("@username") }
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                Toast.makeText(context, "Каталог профилей ещё не подключён", Toast.LENGTH_LONG).show()
                            },
                            enabled = friendQuery.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = ZumeBlue)
                        ) { Text("Найти") }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Поиск, профили и кнопка «Написать» появятся после подключения базы пользователей.",
                            color = Color(0xFF7893A5),
                            fontSize = 12.sp
                        )
                    }
                    4 -> {
                        Text("Мой профиль", color = Color(0xFF17364A), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Surface(color = Color(0xFFD5ECFC), shape = RoundedCornerShape(48.dp), modifier = Modifier.padding(4.dp)) {
                            Text(
                                text = profileName.trim().firstOrNull()?.uppercase() ?: "Z",
                                modifier = Modifier.padding(horizontal = 26.dp, vertical = 16.dp),
                                color = ZumeBlue,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        OutlinedTextField(
                            value = profileName,
                            onValueChange = { profileName = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Имя пользователя") },
                            placeholder = { Text("@username") }
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = profileBio,
                            onValueChange = { profileBio = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("О себе") },
                            placeholder = { Text("Расскажи немного о себе") }
                        )
                        Spacer(Modifier.height(18.dp))
                        Text("Мои хэштеги", color = Color(0xFF17364A), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = hashtagInput,
                            onValueChange = { hashtagInput = it.replace(" ", "") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Добавить хэштег") },
                            placeholder = { Text("#путешествия") }
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val tag = "#" + hashtagInput.trim().removePrefix("#").lowercase()
                                if (tag.length > 1 && tag !in hashtags) hashtags = hashtags + tag
                                hashtagInput = ""
                            },
                            enabled = hashtagInput.trim().isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = ZumeBlue)
                        ) { Text("Добавить #") }
                        if (hashtags.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Text(hashtags.joinToString("   "), color = ZumeBlue, fontSize = 15.sp)
                            TextButton(onClick = { hashtags = emptyList() }) { Text("Очистить хэштеги") }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Пока профиль и хэштеги хранятся только на этом экране. Сохранение в аккаунте подключим отдельно.",
                            color = Color(0xFF7893A5),
                            fontSize = 12.sp
                        )
                    }
                }
            }
            NavigationBar(containerColor = Color(0xFFFFF0F7)) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Лента") },
                    label = { Text("Лента") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.ChatBubble, contentDescription = "Переписки") },
                    label = { Text("Переписки") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Filled.Videocam, contentDescription = "Снять видео") },
                    label = { Text("Снять видео") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Filled.Search, contentDescription = "Найти друзей") },
                    label = { Text("Найти друзей") }
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Filled.Person, contentDescription = "Профиль") },
                    label = { Text("Профиль") }
                )
            }
        }
    }
}
