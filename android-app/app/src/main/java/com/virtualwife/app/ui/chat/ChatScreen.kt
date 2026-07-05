package com.virtualwife.app.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import android.webkit.JavascriptInterface
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.imageLoader
import coil.request.ImageRequest
import com.virtualwife.app.ui.components.ErrorDialog
import com.virtualwife.app.ui.theme.*
import com.virtualwife.app.viewmodel.ChatViewModel
import com.virtualwife.app.viewmodel.VrmViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 聊天主界面 — 数字人全屏 + 悬浮UI
 *
 * Box (全屏)
 *   ├── AndroidView  (VRM + 背景图，全屏底层)
 *   └── Column (全屏透明悬浮)
 *        ├── TopBar (顶部栏)
 *        ├── Box (中间区域，weight 1f)
 *        │    ├── TourProgressCard (游览进度，悬浮)
 *        │    └── MapButton (查看地图按钮，右下角)
 *        └── ChatSection (底部聊天)
 */
@Composable
fun ChatScreen(
    chatViewModel: ChatViewModel,
    vrmViewModel: VrmViewModel = viewModel(),
    onNavigateToSettings: () -> Unit,
    onNavigateToRoute: () -> Unit,
    onNavigateToImageCapture: () -> Unit,
    onNavigateToMap: () -> Unit = {}
) {
    val uiState by chatViewModel.uiState.collectAsStateWithLifecycle()
    val vrmState by vrmViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val app = context.applicationContext as com.virtualwife.app.MainApplication

    VrmAnimationEffect(uiState, vrmViewModel)
    VrmLipSyncEffect(uiState, vrmViewModel)

    if (uiState.error != null) {
        ErrorDialog(message = uiState.error!!) { chatViewModel.clearError() }
    }

    val currentAvatarName = vrmState.avatarName.ifEmpty { "AI导游" }
    val interestTags by app.preferencesManager.interestTags.collectAsState(initial = emptySet())
    val currentSpotId by app.preferencesManager.scenicSpotId.collectAsState(initial = 0L)
    val currentSpotName by app.preferencesManager.scenicSpotName.collectAsState(initial = "")
    var isRecording by remember { mutableStateOf(false) }
    var scenicSpots by remember { mutableStateOf<List<com.virtualwife.app.data.remote.dto.ScenicSpotDto>>(emptyList()) }
    var showScenicPicker by remember { mutableStateOf(false) }

    // 加载景区列表
    LaunchedEffect(Unit) {
        try {
            val res = com.virtualwife.app.data.remote.RetrofitClient.djangoApi.getScenicSpots()
            if (res.isSuccessful && res.body()?.isSuccess == true) {
                scenicSpots = res.body()!!.data ?: emptyList()
            }
        } catch (_: Exception) {}
    }

    // 景区选择弹窗
    if (showScenicPicker) {
        val scope = rememberCoroutineScope()
        AlertDialog(
            onDismissRequest = { showScenicPicker = false },
            title = { Text("选择景区") },
            text = {
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(scenicSpots) { spot ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (spot.id == currentSpotId) Color(0xFFE91E63).copy(alpha = 0.1f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                        app.preferencesManager.saveScenicSpot(spot.id, spot.spotName)
                                    }
                                    showScenicPicker = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Place, null,
                                tint = if (spot.id == currentSpotId) Color(0xFFE91E63) else Color.Gray,
                                modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(spot.spotName,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (spot.id == currentSpotId) Color(0xFFE91E63) else Color(0xFF1C1B1F))
                                if (!spot.description.isNullOrBlank()) {
                                    Text(spot.description!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showScenicPicker = false }) { Text("取消") }
            }
        )
    }
    var isPageLoaded by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // 模型URL变化时才加载（JS层有缓存，不会重复下载）
    LaunchedEffect(vrmState.modelUrl, isPageLoaded) {
        if (isPageLoaded && vrmState.modelUrl.isNotEmpty()) {
            Log.d("ChatScreen", "Loading VRM model: ${vrmState.modelUrl}")
            webViewRef?.evaluateJavascript("loadModel('${vrmState.modelUrl}')", null)
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) isRecording = true }

    // 是否正在游览
    val isTourActive = uiState.isTourActive

    // 调试日志
    LaunchedEffect(uiState.selectedRouteId, uiState.selectedRouteName, isTourActive) {
        Log.d("ChatScreen", "RouteState: id=${uiState.selectedRouteId}, name=${uiState.selectedRouteName}, active=$isTourActive")
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ====== 底层：数字人VRM + 背景 ======
        AndroidView(
            factory = {
                FrameLayout(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    val bgImageView = ImageView(context).apply {
                        tag = "bgImageView"
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        setBackgroundColor(AndroidColor.parseColor("#E8F0FE"))
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                    }
                    addView(bgImageView)

                    val webView = WebView(context).apply {
                        setBackgroundColor(AndroidColor.TRANSPARENT)
                        setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
                        background = null
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun onVrmLoaded(modelName: String) {
                                Log.d("ChatScreen", "VRM loaded: $modelName")
                                vrmViewModel.onModelLoaded()
                            }
                            @JavascriptInterface
                            fun onVrmClick(expression: String) {
                                vrmViewModel.setExpression(expression)
                            }
                            @JavascriptInterface
                            fun onLibsReady() {}
                            @JavascriptInterface
                            fun onError(error: String) {
                                Log.e("ChatScreen", "WebView error: $error")
                            }
                        }, "AndroidBridge")

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isPageLoaded = true
                                webViewRef = view
                                vrmViewModel.setWebViewBridge { jsCode ->
                                    view?.evaluateJavascript(jsCode, null)
                                }
                            }
                        }
                        loadUrl("file:///android_asset/web/index.html")
                    }
                    addView(webView, FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    ))
                }
            },
            update = { rootView ->
                val bgImageView = rootView.findViewWithTag<ImageView>("bgImageView")
                if (vrmState.backgroundUrl.isNotEmpty()) {
                    val request = ImageRequest.Builder(context)
                        .data(vrmState.backgroundUrl)
                        .target(bgImageView!!)
                        .build()
                    context.imageLoader.enqueue(request)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // ====== 悬浮层 ======
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部栏
            TopBar(
                avatarName = currentAvatarName,
                scenicSpotName = currentSpotName.ifEmpty { null },
                routeName = uiState.selectedRouteName,
                onScenicClick = { showScenicPicker = true },
                onRouteClick = onNavigateToRoute,
                onSettingsClick = onNavigateToSettings
            )

            // 路线就绪卡片（已选路线未开始游览）
            if (uiState.selectedRouteId != null && !isTourActive) {
                RouteReadyCard(
                    routeName = uiState.selectedRouteName ?: "未知路线",
                    onStartTour = {
                        chatViewModel.startTour()
                        onNavigateToMap()
                    },
                    onViewMap = onNavigateToMap,
                    onCancel = { chatViewModel.stopTour() },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // 游览进度卡片（游览中）
            if (isTourActive) {
                TourProgressOverlay(
                    uiState = uiState,
                    onCancelTour = { chatViewModel.stopTour() },
                    onViewMap = onNavigateToMap,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // 中间留空（展示数字人）
            Spacer(modifier = Modifier.weight(1f))

            // 底部聊天
            ChatSection(
                uiState = uiState,
                vrmState = vrmState,
                chatViewModel = chatViewModel,
                interestTags = interestTags,
                isRecording = isRecording,
                onVoiceStart = {
                    if (ContextCompat.checkSelfPermission(
                            context, Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    ) isRecording = true
                    else audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                onVoiceEnd = { isRecording = false },
                onCameraClick = onNavigateToImageCapture
            )
        }
    }
}

// ==================== 顶部栏 ====================

@Composable
private fun TopBar(
    avatarName: String,
    scenicSpotName: String? = null,
    routeName: String? = null,
    onScenicClick: () -> Unit = {},
    onRouteClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onRouteClick) {
            Icon(Icons.Filled.Map, "路线", tint = Color.White)
        }
        Column(
            modifier = Modifier.weight(1f).clickable { onScenicClick() },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(avatarName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White)
            // 显示景区名或路线名
            if (routeName != null) {
                Text("🗺️ $routeName",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF1C1B1F))
            } else if (scenicSpotName != null) {
                Text("📍 $scenicSpotName",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White.copy(alpha = 0.9f))
            } else {
                Text("点击选择景区",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f))
            }
        }
        IconButton(onClick = onSettingsClick) {
            Icon(Icons.Filled.Settings, "设置", tint = Color.White)
        }
    }
}

// ==================== 游览进度悬浮卡片 ====================

@Composable
private fun TourProgressOverlay(
    uiState: com.virtualwife.app.viewmodel.ChatUiState,
    onCancelTour: () -> Unit = {},
    onViewMap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val spots = uiState.tourSpots
    val visited = uiState.visitedSpots
    val currentIdx = uiState.currentSpotIndex
    val currentSpot = if (currentIdx in spots.indices) spots[currentIdx] else null
    val nextIdx = spots.indices.firstOrNull { !visited.contains(it) && it != currentIdx }
    val nextSpot = if (nextIdx != null) spots[nextIdx] else null

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 进度标题
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Timeline, null,
                    tint = Color(0xFFE91E63), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("游览进度",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.weight(1f))
                Text("${visited.size}/${spots.size}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold, color = Color(0xFFE91E63)))
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onViewMap,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Filled.Map, "查看地图",
                        tint = Color(0xFF1C1B1F), modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = onCancelTour,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Filled.Close, "取消游览",
                        tint = Color(0xFF1C1B1F), modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = visited.size.toFloat() / spots.size.coerceAtLeast(1).toFloat(),
                modifier = Modifier.fillMaxWidth().height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color(0xFFE91E63)
            )

            // 当前景点
            if (currentSpot != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, null,
                        tint = Color(0xFFE91E63), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("当前：${currentSpot.name}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                }
            }

            // 下一景点
            if (nextSpot != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Flag, null,
                        tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("下一：${nextSpot.name}",
                        style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }

            // 进度点
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                spots.forEachIndexed { index, spot ->
                    val isVisited = visited.contains(index)
                    val isCurrent = index == currentIdx
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isCurrent -> Color(0xFFE91E63)
                                    isVisited -> Color(0xFF4CAF50)
                                    else -> Color.LightGray
                                }
                            )
                    )
                }
            }
        }
    }
}

// ==================== 路线就绪卡片（未开始游览）====================

@Composable
private fun RouteReadyCard(
    routeName: String,
    onStartTour: () -> Unit,
    onViewMap: () -> Unit = {},
    onCancel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Card(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE91E63).copy(alpha = 0.1f))
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Route, null, tint = Color(0xFFE91E63), modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(routeName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1C1B1F))
                    Text("路线已就绪，开始游览吧！",
                        style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Filled.Close, "取消",
                        tint = Color(0xFF1C1B1F), modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onStartTour,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
                ) {
                    Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("开始游览", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                OutlinedButton(
                    onClick = onViewMap,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Map, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("查看地图", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

// ==================== 底部聊天区域 ====================

@Composable
private fun ChatSection(
    uiState: com.virtualwife.app.viewmodel.ChatUiState,
    vrmState: com.virtualwife.app.viewmodel.VrmUiState,
    chatViewModel: ChatViewModel,
    interestTags: Set<String>,
    isRecording: Boolean,
    onVoiceStart: () -> Unit,
    onVoiceEnd: () -> Unit,
    onCameraClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val app = LocalContext.current.applicationContext as com.virtualwife.app.MainApplication
    val userName by app.preferencesManager.username.collectAsState(initial = "用户")
    val displayName = userName.ifEmpty { "用户" }
    val aiName = vrmState.avatarName.ifEmpty { "AI导游" }
    val avatarThumb = vrmState.thumbnailUrl.ifEmpty { null }

    LaunchedEffect(uiState.messages.size, uiState.isAiTyping) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth().navigationBarsPadding()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items = uiState.messages, key = { it.id }) { message ->
                ChatBubble(
                    message = message,
                    avatarThumbnailUrl = avatarThumb,
                    userName = displayName,
                    aiName = aiName
                )
            }
            if (uiState.isAiTyping) {
                item(key = "streaming") { StreamingBubble(content = "思考中...") }
            }
            if (uiState.messages.isEmpty() && !uiState.isAiTyping) {
                item(key = "welcome") { WelcomeMessage(interestTags = interestTags) }
            }
        }

        ChatInputBar(
            onSendText = { chatViewModel.sendTextMessage(it) },
            onVoiceStart = onVoiceStart,
            onVoiceEnd = onVoiceEnd,
            onCameraClick = onCameraClick,
            isRecording = isRecording
        )
    }
}

// ==================== 欢迎消息 ====================

@Composable
private fun WelcomeMessage(interestTags: Set<String> = emptySet()) {
    val greetingText = if (interestTags.isNotEmpty()) {
        val tags = interestTags.take(3).joinToString("、")
        "我看到你对${tags}感兴趣～\n让我为你推荐相关景点和路线吧！"
    } else {
        "我是你的AI导游小助手\n可以为你介绍景点、推荐路线\n有什么想问的尽管说～"
    }
    val quickTags = when {
        interestTags.contains("历史文化") -> listOf("🏛️ 历史古迹", "📜 文化故事", "🗺️ 人文路线")
        interestTags.contains("自然风光") -> listOf("🌿 自然景观", "🌸 赏花攻略", "🥾 徒步路线")
        interestTags.contains("美食体验") -> listOf("🍜 特色美食", "🍵 茶文化", "🛒 美食街")
        interestTags.contains("拍照打卡") -> listOf("📸 网红打卡", "🌅 最佳机位", "🎨 创意拍照")
        interestTags.contains("亲子互动") -> listOf("👨‍👩‍👧 亲子活动", "🎮 互动体验", "🧒 儿童乐园")
        else -> listOf("🏛️ 景点", "🗺️ 路线", "🍜 美食")
    }

    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.7f))
                .padding(24.dp)
        ) {
            Text("🌸", fontSize = 36.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text("你好呀～",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF1C1B1F))
            Spacer(modifier = Modifier.height(8.dp))
            Text(greetingText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF79747E),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 22.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                quickTags.forEach { tag ->
                    Surface(shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 0.dp) {
                        Text(tag,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = PrimaryLight)
                    }
                }
            }
        }
    }
}

// ==================== 数字人动画副作用 ====================

@Composable
private fun VrmAnimationEffect(
    uiState: com.virtualwife.app.viewmodel.ChatUiState,
    vrmViewModel: VrmViewModel
) {
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            val lastMsg = uiState.messages.last()
            val isAi = lastMsg.messageType == "ai" || lastMsg.messageType == "ai_reply"
            if (isAi) {
                val emotion = lastMsg.emotion ?: "happy"
                vrmViewModel.handleEmotionChange(emotion)
                when (emotion) {
                    "happy", "greeting" -> vrmViewModel.playGuideAction("open_arms")
                    "surprised" -> vrmViewModel.playGuideAction("nod")
                    "sad" -> vrmViewModel.playGuideAction("bow")
                    "thinking" -> vrmViewModel.playGuideAction("think")
                    "explaining" -> vrmViewModel.playGuideAction("explain")
                    else -> vrmViewModel.playGuideAction("point_right")
                }
            }
            val isUser = lastMsg.messageType == "user" || lastMsg.messageType == "text"
            if (isUser) vrmViewModel.playGuideAction("nod")
        }
    }
}

@Composable
private fun VrmLipSyncEffect(
    uiState: com.virtualwife.app.viewmodel.ChatUiState,
    vrmViewModel: VrmViewModel
) {
    LaunchedEffect(uiState.isAiTyping) {
        if (uiState.isAiTyping) {
            val lastMsg = uiState.messages.lastOrNull()
            val textLength = lastMsg?.content?.length ?: 0
            val duration = (textLength * 100L).coerceIn(2000L, 10000L)
            vrmViewModel.startTalking(duration)
        } else {
            vrmViewModel.stopTalking()
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { uiState.isTtsPlaying }
            .collect { isPlaying ->
                if (isPlaying) {
                    vrmViewModel.startTalking(300000L)
                    vrmViewModel.playGuideAction("explain")
                    delay(3000)
                    while (uiState.isTtsPlaying) {
                        vrmViewModel.playRandomAction()
                        delay(5000)
                    }
                } else {
                    vrmViewModel.stopTalking()
                }
            }
    }
}
