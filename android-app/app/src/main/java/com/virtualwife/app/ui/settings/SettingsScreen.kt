package com.virtualwife.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.virtualwife.app.ui.components.ErrorDialog
import com.virtualwife.app.ui.theme.*
import com.virtualwife.app.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCharacter: () -> Unit,
    onLogout: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showAvatarDialog by remember { mutableStateOf(false) }

    // 相机拍照
    val cameraLauncher = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val baos = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, baos)
            settingsViewModel.uploadUserAvatar(baos.toByteArray())
        }
    }

    // 相册选择
    val galleryLauncher = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    val baos = java.io.ByteArrayOutputStream()
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, baos)
                    settingsViewModel.uploadUserAvatar(baos.toByteArray())
                }
            } catch (e: Exception) {
                android.util.Log.e("Settings", "Failed to load image: ${e.message}")
            }
        }
    }

    if (uiState.error != null) {
        ErrorDialog(message = uiState.error!!) { settingsViewModel.clearError() }
    }

    if (showLogoutDialog) {
        KawaiiDialog(
            emoji = "👋",
            title = "要走了吗？",
            message = "确定要退出登录吗？",
            confirmText = "退出",
            confirmColor = Color(0xFFFF6B8A),
            onConfirm = { showLogoutDialog = false; onLogout() },
            onDismiss = { showLogoutDialog = false }
        )
    }

    if (showClearHistoryDialog) {
        KawaiiDialog(
            emoji = "🗑️",
            title = "清除历史记录",
            message = "确定要清除所有聊天历史吗？\n此操作不可撤销。",
            confirmText = "清除",
            confirmColor = Color(0xFFFF6B8A),
            onConfirm = { showClearHistoryDialog = false; settingsViewModel.clearChatHistory() },
            onDismiss = { showClearHistoryDialog = false }
        )
    }

    // 头像选择对话框
    if (showAvatarDialog) {
        AlertDialog(
            onDismissRequest = { showAvatarDialog = false },
            title = {
                Text("更换头像",
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1C1B1F),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center)
            },
            text = {
                Column {
                    // 拍照
                    Surface(
                        onClick = {
                            showAvatarDialog = false
                            cameraLauncher.launch(null)
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF5F0F7)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.CameraAlt, null, tint = Color(0xFFB8A9E8))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("拍照", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // 相册
                    Surface(
                        onClick = {
                            showAvatarDialog = false
                            galleryLauncher.launch("image/*")
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF5F0F7)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, null, tint = Color(0xFFB8A9E8))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("从相册选择", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    // 清除头像
                    if (uiState.userAvatarUrl.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            onClick = {
                                showAvatarDialog = false
                                settingsViewModel.clearUserAvatar()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFFD1DC).copy(alpha = 0.3f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Delete, null, tint = Color(0xFFFF6B8A))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("恢复默认头像", style = MaterialTheme.typography.bodyLarge, color = Color(0xFFFF6B8A))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }

    Scaffold(
        containerColor = Color(0xFFF8F5FA),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(Color(0xFFFFFBFE))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF5F0F7))
                ) {
                    Icon(Icons.Filled.ArrowBack, "返回",
                        modifier = Modifier.size(18.dp), tint = Color(0xFF79747E))
                }
                Spacer(modifier = Modifier.weight(1f))
                Text("设置",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF1C1B1F))
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.size(36.dp))
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 用户头像
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 头像
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF5F0F7))
                                .clickable { showAvatarDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.userAvatarUrl.isNotEmpty()) {
                                // 从URL加载头像
                                val painter = coil.compose.rememberAsyncImagePainter(model = uiState.userAvatarUrl)
                                androidx.compose.foundation.Image(
                                    painter = painter,
                                    contentDescription = "用户头像",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = "默认头像",
                                    modifier = Modifier.size(36.dp),
                                    tint = Color(0xFFB8A9E8)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                uiState.avatarName.ifEmpty { "用户" },
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF1C1B1F)
                            )
                            Text(
                                "点击头像更换",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCAC4D0)
                            )
                        }
                    }
                }
            }

            // 数字人
            item {
                SettingsSection("🎭 数字人") {
                    SettingsItem(Icons.Outlined.Person, "当前形象", uiState.avatarName, {})
                    SettingsItem(Icons.Outlined.ViewInAr, "选择形象", "点击切换数字人",
                        { settingsViewModel.loadAvatars() }, showDivider = false)
                }
            }

            // 数字人选择列表
            if (uiState.availableAvatars.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            uiState.availableAvatars.forEach { avatar ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (avatar.avatarName == uiState.avatarName)
                                                Color(0xFFFFD1DC).copy(alpha = 0.3f)
                                            else Color.Transparent
                                        )
                                        .clickable {
                                            settingsViewModel.selectAvatar(avatar)
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF5F0F7)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🎭", fontSize = 20.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(avatar.avatarName ?: "未命名",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium)
                                        Text(avatar.personality ?: "暂无描述",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFCAC4D0))
                                    }
                                    if (avatar.avatarName == uiState.avatarName) {
                                        Icon(Icons.Filled.CheckCircle, null,
                                            tint = Color(0xFFFF8FAB),
                                            modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // 语音
            item {
                SettingsSection("🎤 语音") {
                    // 当前语音显示
                    val voiceName = when (uiState.ttsVoice) {
                        "zh-CN-XiaoyiNeural" -> "小艺 (女声)"
                        "zh-CN-YunxiNeural" -> "云希 (男声)"
                        "zh-CN-XiaoxiaoNeural" -> "小晓 (女声)"
                        "zh-CN-YunjianNeural" -> "云健 (男声)"
                        "zh-CN-XiaochenNeural" -> "小辰 (女声)"
                        "zh-CN-YunzeNeural" -> "云泽 (男声)"
                        else -> uiState.ttsVoice
                    }
                    SettingsItem(Icons.Outlined.RecordVoiceOver, "当前语音", voiceName, {})
                }
            }

            // 语音选择列表
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        val voices = listOf(
                            "zh-CN-XiaoyiNeural" to "小艺 (女声)",
                            "zh-CN-YunxiNeural" to "云希 (男声)",
                            "zh-CN-XiaoxiaoNeural" to "小晓 (女声)",
                            "zh-CN-YunjianNeural" to "云健 (男声)",
                            "zh-CN-XiaochenNeural" to "小辰 (女声)",
                            "zh-CN-YunzeNeural" to "云泽 (男声)"
                        )
                        voices.forEach { (id, name) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (id == uiState.ttsVoice)
                                            Color(0xFFE8DEFF).copy(alpha = 0.4f)
                                        else Color.Transparent
                                    )
                                    .clickable { settingsViewModel.setTtsVoice(id) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF5F0F7)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🎤", fontSize = 18.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium)
                                    Text(id,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFCAC4D0))
                                }
                                if (id == uiState.ttsVoice) {
                                    Icon(Icons.Filled.CheckCircle, null,
                                        tint = Color(0xFFB8A9E8),
                                        modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
            // 对话
            item {
                SettingsSection("💬 对话") {
                    SettingsItem(Icons.Outlined.DeleteSweep, "清除历史记录", "删除所有本地聊天记录",
                        { showClearHistoryDialog = true }, showDivider = false)
                }
            }
            // 关于
            item {
                SettingsSection("ℹ️ 关于") {
                    SettingsItem(Icons.Outlined.Info, "版本", "v1.0.0", {}, showDivider = false)
                }
            }
            // 退出
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B8A)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(Icons.Outlined.Logout, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("退出登录", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFF79747E),
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showDivider: Boolean = true
) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF5F0F7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = Color(0xFFB8A9E8), modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1C1B1F))
                    Text(subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCAC4D0))
                }
                Icon(Icons.Filled.ChevronRight, null,
                    tint = Color(0xFFCAC4D0),
                    modifier = Modifier.size(20.dp))
            }
            if (showDivider) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 68.dp, end = 16.dp)
                        .height(0.5.dp)
                        .background(Color(0xFFF5F0F7))
                )
            }
        }
    }
}

@Composable
private fun KawaiiDialog(
    emoji: String,
    title: String,
    message: String,
    confirmText: String,
    confirmColor: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFD1DC).copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) { Text(emoji, fontSize = 28.sp) }
        },
        title = {
            Text(title,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1C1B1F),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center)
        },
        text = {
            Text(message,
                color = Color(0xFF79747E),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = confirmColor),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) { Text(confirmText, fontWeight = FontWeight.SemiBold) }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White
    )
}
