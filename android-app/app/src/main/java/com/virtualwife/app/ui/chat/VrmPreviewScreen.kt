package com.virtualwife.app.ui.chat

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Log
import android.view.MotionEvent
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.virtualwife.app.viewmodel.VrmViewModel

private const val TAG = "VrmPreview"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VrmPreviewScreen(
    vrmViewModel: VrmViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by vrmViewModel.uiState.collectAsState()
    // 记录WebView是否已加载完成
    var isPageLoaded by remember { mutableStateOf(false) }
    // 记录WebView引用
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // 当模型URL变化且WebView已加载完成时，加载模型
    LaunchedEffect(uiState.modelUrl, isPageLoaded) {
        Log.d(TAG, "State changed: modelUrl=${uiState.modelUrl}, isPageLoaded=$isPageLoaded")
        if (isPageLoaded && uiState.modelUrl.isNotEmpty()) {
            Log.d(TAG, "Loading VRM model: ${uiState.modelUrl}")
            webViewRef?.evaluateJavascript("loadModel('${uiState.modelUrl}')", null)
        }
    }

    // 超时处理：如果10秒后模型仍未加载，显示2D降级
    LaunchedEffect(isPageLoaded) {
        if (isPageLoaded) {
            kotlinx.coroutines.delay(10000)
            if (!uiState.isModelLoaded) {
                Log.w(TAG, "VRM model load timeout, showing 2D fallback")
                webViewRef?.evaluateJavascript("show2DFallback()", null)
            }
        }
    }

    // 当avatarName变化时，更新显示
    LaunchedEffect(uiState.avatarName) {
        if (isPageLoaded) {
            webViewRef?.evaluateJavascript(
                "document.getElementById('avatar-name').textContent='${uiState.avatarName}'",
                null
            )
        }
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                // 设置WebView透明背景
                setBackgroundColor(Color.TRANSPARENT)
                setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
                background = null

                // 禁用WebView交互，让触摸事件传递给Compose层
                isClickable = false
                isFocusable = false
                setOnTouchListener { v, event ->
                    // 不消费任何触摸事件
                    v.parent?.requestDisallowInterceptTouchEvent(false)
                    false
                }

                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onVrmLoaded(modelName: String) {
                        Log.d(TAG, "VRM model loaded: $modelName")
                        vrmViewModel.onModelLoaded()
                    }

                    @JavascriptInterface
                    fun onVrmClick(expression: String) {
                        vrmViewModel.setExpression(expression)
                    }

                    @JavascriptInterface
                    fun onLibsReady() {
                        Log.d(TAG, "Three.js libraries ready")
                    }

                    @JavascriptInterface
                    fun onError(error: String) {
                        Log.e(TAG, "WebView error: $error")
                    }
                }, "AndroidBridge")

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        Log.d(TAG, "WebView page finished loading")
                        isPageLoaded = true
                        webViewRef = view

                        val bridge: (String) -> Unit = { jsCode: String ->
                            view?.evaluateJavascript(jsCode, null)
                            Unit
                        }
                        vrmViewModel.setWebViewBridge(bridge)

                        // 注意：不在这里加载模型，由LaunchedEffect处理
                    }

                    override fun onReceivedError(
                        view: WebView?, errorCode: Int,
                        description: String?, failingUrl: String?
                    ) {
                        Log.e(TAG, "WebView error: $errorCode $description at $failingUrl")
                        super.onReceivedError(view, errorCode, description, failingUrl)
                    }
                }

                loadUrl("file:///android_asset/web/index.html")
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
