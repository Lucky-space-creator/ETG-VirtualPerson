package com.virtualwife.app.ui.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.virtualwife.app.data.local.entity.ChatMessageEntity
import com.virtualwife.app.data.remote.dto.WsSource
import com.virtualwife.app.ui.theme.*
import com.virtualwife.app.util.toDateString

@Composable
fun ChatBubble(
    message: ChatMessageEntity,
    avatarThumbnailUrl: String? = null,
    userName: String = "用户",
    aiName: String = "AI导游",
    modifier: Modifier = Modifier
) {
    val isUser = message.messageType == "user" || message.messageType == "text"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            AIAvatar(emotion = message.emotion, thumbnailUrl = avatarThumbnailUrl)
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            // 名称标签
            Text(
                text = if (isUser) userName else aiName,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF9E98A6),
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 2.dp)
            )
            // 气泡（半透明背景）
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = if (isUser) 20.dp else 6.dp,
                            bottomEnd = if (isUser) 6.dp else 20.dp
                        )
                    )
                    .background(
                        if (isUser)
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFF8FAB).copy(alpha = 0.85f),
                                    Color(0xFFFFA0C0).copy(alpha = 0.85f)
                                )
                            )
                        else
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.8f),
                                    Color.White.copy(alpha = 0.8f)
                                )
                            )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 20.sp
                    ),
                    color = if (isUser) Color.White else Color(0xFF1C1B1F)
                )
            }

            // 引用溯源
            if (!isUser && !message.sourcesJson.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                SourcesCard(sourcesJson = message.sourcesJson!!)
            }

            // 时间
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = message.timestamp.toDateString("HH:mm"),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFCAC4D0)
            )
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            UserAvatar()
        }
    }
}

@Composable
private fun AIAvatar(emotion: String, thumbnailUrl: String? = null) {
    val emoji = when (emotion.lowercase()) {
        "happy", "happiness" -> "😊"
        "sad", "sadness" -> "😢"
        "angry", "anger" -> "😤"
        "surprise" -> "😲"
        "relaxed", "encouragement" -> "😌"
        "greeting" -> "👋"
        else -> "🌸"
    }

    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFFFD1DC), Color(0xFFE8DEFF))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!thumbnailUrl.isNullOrEmpty()) {
            // 加载数字人缩略图
            val painter = coil.compose.rememberAsyncImagePainter(model = thumbnailUrl)
            Image(
                painter = painter,
                contentDescription = "AI头像",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // 默认AI图标
            Text(emoji, fontSize = 16.sp)
        }
    }
}

@Composable
private fun UserAvatar() {
    // 从本地缓存加载用户头像URL
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { com.virtualwife.app.data.local.PreferencesManager(context) }
    val userAvatarUrl by prefs.userAvatarUrl.collectAsState(initial = "")

    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color(0xFFD4E5FF)),
        contentAlignment = Alignment.Center
    ) {
        if (userAvatarUrl.isNotEmpty()) {
            // 从URL加载头像
            val painter = coil.compose.rememberAsyncImagePainter(model = userAvatarUrl)
            Image(
                painter = painter,
                contentDescription = "用户头像",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text("😊", fontSize = 16.sp)
        }
    }
}

@Composable
private fun SourcesCard(sourcesJson: String) {
    val sources = try {
        val type = object : TypeToken<List<WsSource>>() {}.type
        Gson().fromJson<List<WsSource>>(sourcesJson, type)
    } catch (e: Exception) { null }

    if (sources.isNullOrEmpty()) return

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFD4E5FF).copy(alpha = 0.3f))
            .padding(10.dp)
    ) {
        Column {
            Text(
                "📚 引用来源",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color(0xFF79747E)
            )
            Spacer(modifier = Modifier.height(4.dp))
            sources.forEach { source ->
                Text(
                    "· ${source.docName}${if (source.page > 0) " p.${source.page}" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9E98A6)
                )
            }
        }
    }
}

@Composable
fun StreamingBubble(
    content: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        AIAvatar(emotion = "neutral")
        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFFF5F0F7), Color(0xFFF0EBF5))
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF1C1B1F)
                )
                Spacer(modifier = Modifier.width(6.dp))
                TypingIndicator()
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Text("●", style = MaterialTheme.typography.bodySmall, color = PrimaryLight.copy(alpha = alpha))
}
