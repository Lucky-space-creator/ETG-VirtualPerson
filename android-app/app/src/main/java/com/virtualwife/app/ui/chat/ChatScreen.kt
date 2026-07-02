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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import kotlinx.coroutines.delay

/**
 * 聊天主界面 — 数字人全屏 + 悬浮UI
 *
 * Box (全屏)
 *   ├── AndroidView  (数字人VRM + 背景图，全屏底层)
 *   └── Column (全屏透明悬浮)
 *        ├── TopBar    (透明，顶部)
 *        ├── Spacer    (中间留空，展示数字人)
 *        └── ChatSection (透明，底部聊天)
 */
@Composable
fun ChatScreen(
    chatViewModel: ChatViewModel,
    vrmViewModel: VrmViewModel = viewModel(),
    onNavigateToSettings: () -> Unit,
    onNavigateToRoute: () -> Unit,
    onNavigateToImageCapture: () -> Unit
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
    var isRecording by remember { mutableStateOf(false) }
    var isPageLoaded by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // WebView就绪后加载VRM模型
    LaunchedEffect(vrmState.modelUrl, isPageLoaded) {
        if (isPageLoaded && vrmState.modelUrl.isNotEmpty()) {
            Log.d("ChatScreen", "Loading VRM model: ${vrmState.modelUrl}")
            webViewRef?.evaluateJavascript("loadModel('${vrmState.modelUrl}')", null)
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) isRecording = true }

    Box(modifier = Modifier.fillMaxSize()) {
        // ====== 全屏：数字人VRM + 背景 ======
        AndroidView(
            factory = {
                FrameLayout(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    // 背景图片
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

                    // WebView (VRM)
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

        // ====== 悬浮层：上中下布局 ======
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部栏（透明）
            TopBar(
                avatarName = currentAvatarName,
                onRouteClick = onNavigateToRoute,
                onSettingsClick = onNavigateToSettings
            )

            // 中间留空（展示数字人）
            Spacer(modifier = Modifier.weight(1f))

            // 底部聊天（透明）
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
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                avatarName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color.White
            )
            Text(
                "在线陪你逛",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f)
            )
        }

        IconButton(onClick = onSettingsClick) {
            Icon(Icons.Filled.Settings, "设置", tint = Color.White)
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
    // 提升到外层，避免每个item都创建新的State
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
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        RoutePromptCard(uiState, chatViewModel)

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                items = uiState.messages,
                key = { it.id }
            ) { message ->
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

// ==================== 路线选择提示 ====================

@Composable
private fun RoutePromptCard(
    uiState: com.virtualwife.app.viewmodel.ChatUiState,
    chatViewModel: ChatViewModel
) {
    if (uiState.selectedRouteId != null && !uiState.isTourActive) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Route, null,
                    tint = Color(0xFFE91E63), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    uiState.selectedRouteName ?: "",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1C1B1F),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { chatViewModel.startTour() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("开始游览", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
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

    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.7f))
                .padding(24.dp)
        ) {
            Text("🌸", fontSize = 36.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "你好呀～",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color(0xFF1C1B1F)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                greetingText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF79747E),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                quickTags.forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        shadowElevation = 0.dp
                    ) {
                        Text(
                            tag,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = PrimaryLight
                        )
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
    // AI打字口型
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

    // TTS播放口型+动作（用snapshotFlow避免while循环捕获旧值）
    LaunchedEffect(Unit) {
        snapshotFlow { uiState.isTtsPlaying }
            .collect { isPlaying ->
                if (isPlaying) {
                    vrmViewModel.startTalking(300000L)
                    vrmViewModel.playGuideAction("explain")
                    delay(3000)
                    // 持续触发随机动作直到TTS停止
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
