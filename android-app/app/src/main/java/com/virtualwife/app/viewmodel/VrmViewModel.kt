package com.virtualwife.app.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.virtualwife.app.MainApplication
import com.virtualwife.app.data.remote.RetrofitClient
import com.virtualwife.app.data.repository.VrmRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private const val TAG = "VrmViewModel"

data class VrmUiState(
    val currentExpression: String = "neutral",
    val currentAction: String? = null,
    val lipSyncValue: Float = 0f,
    val modelUrl: String = "",
    val backgroundUrl: String = "",
    val thumbnailUrl: String = "",
    val avatarName: String = "AI导游",
    val isModelLoaded: Boolean = false,
    // 动作配置
    val armAngle: Int = 50,
    val idleAnimation: String = "idle",
    val talkAnimation: String = "natural",
    val animationIntensity: Double = 0.7
)

class VrmViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MainApplication
    private val vrmRepo = VrmRepository(app.preferencesManager)
    private val resourceManager = app.resourceManager

    private val _uiState = MutableStateFlow(VrmUiState())
    val uiState: StateFlow<VrmUiState> = _uiState.asStateFlow()

    private var webViewBridge: ((String) -> Unit)? = null

    init {
        // 1. 先从DataStore加载本地缓存（立即可用）
        viewModelScope.launch {
            vrmRepo.vrmModelUrl.collect { url ->
                if (url.isNotEmpty()) {
                    _uiState.update { it.copy(modelUrl = url) }
                }
            }
        }
        viewModelScope.launch {
            vrmRepo.avatarName.collect { name ->
                if (name.isNotEmpty()) {
                    _uiState.update { it.copy(avatarName = name) }
                }
            }
        }

        // 2. 使用预加载数据（App启动时已加载）
        usePreloadedData()

        // 3. 如果预加载未完成，轮询等待
        waitForPreload()
    }

    /**
     * 使用ResourceManager预加载的数据
     */
    private fun usePreloadedData() {
        if (resourceManager.avatarLoaded) {
            val avatar = resourceManager.defaultAvatar ?: return
            val name = avatar.avatarName ?: "AI导游"
            val vrmUrl = resourceManager.getAvatarDisplayUrl() ?: ""
            val bgUrl = avatar.backgroundDisplayUrl ?: avatar.backgroundUrl ?: ""
            val thumbUrl = avatar.thumbnailDisplayUrl ?: avatar.thumbnailUrl ?: ""

            Log.d(TAG, "Using preloaded avatar: name=$name, thumb=$thumbUrl")
            _uiState.update {
                it.copy(
                    avatarName = name,
                    modelUrl = vrmUrl,
                    backgroundUrl = bgUrl,
                    thumbnailUrl = thumbUrl,
                    // 动作配置
                    armAngle = avatar.armAngle,
                    idleAnimation = avatar.idleAnimation ?: "idle",
                    talkAnimation = avatar.talkAnimation ?: "natural",
                    animationIntensity = avatar.animationIntensity
                )
            }

            // 将配置传递给WebView
            webViewBridge?.invoke("updateConfig(${avatar.armAngle}, '${avatar.idleAnimation}', '${avatar.talkAnimation}', ${avatar.animationIntensity})")
        }
    }

    /**
     * 如果预加载未完成，等待并重试
     */
    private fun waitForPreload() {
        viewModelScope.launch {
            var retries = 0
            while (!resourceManager.avatarLoaded && retries < 20) {
                delay(300)
                retries++
            }
            if (resourceManager.avatarLoaded) {
                usePreloadedData()
            } else {
                // 预加载超时，直接调用API
                Log.w(TAG, "Preload timeout, loading directly")
                loadAvatarDirectly()
            }
        }
    }

    /**
     * 直接调用API加载（备用方案）
     */
    private fun loadAvatarDirectly() {
        viewModelScope.launch {
            try {
                // 使用adminApi（带认证）
                val res = RetrofitClient.adminApi.getDefaultAvatar()
                if (res.isSuccessful && res.body()?.isSuccess == true) {
                    val avatar = res.body()!!.data
                    if (avatar != null) {
                        val name = avatar.avatarName ?: "AI导游"
                        val vrmUrl = avatar.vrmDisplayUrl?.takeIf { it.isNotEmpty() }
                            ?: avatar.vrmModelUrl ?: ""

                        Log.d(TAG, "Direct load: name=$name")
                        _uiState.update { it.copy(avatarName = name, modelUrl = vrmUrl) }

                        vrmRepo.setAvatarName(name)
                        if (vrmUrl.isNotEmpty()) vrmRepo.setVrmModel(vrmUrl)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Direct load failed: ${e.message}")
            }
        }
    }

    fun setWebViewBridge(bridge: (String) -> Unit) {
        webViewBridge = bridge
        // Bridge就绪后立即加载模型
        val url = _uiState.value.modelUrl
        if (url.isNotEmpty()) {
            bridge("loadModel('$url')")
        }
        // 传递动画配置
        val avatar = resourceManager.defaultAvatar
        if (avatar != null) {
            bridge("updateConfig(${avatar.armAngle}, '${avatar.idleAnimation}', '${avatar.talkAnimation}', ${avatar.animationIntensity})")
        }
    }

    fun setExpression(emotion: String) {
        val expression = vrmRepo.mapEmotionToExpression(emotion)
        _uiState.update { it.copy(currentExpression = expression) }
        webViewBridge?.invoke("setExpression('$expression')")
    }

    fun playAction(emotion: String) {
        val action = vrmRepo.mapEmotionToAction(emotion)
        if (action != null) {
            _uiState.update { it.copy(currentAction = action) }
            webViewBridge?.invoke("playAction('$action')")
        }
    }

    fun setLipSync(value: Float) {
        val clampedValue = value.coerceIn(0f, 1f)
        _uiState.update { it.copy(lipSyncValue = clampedValue) }
        webViewBridge?.invoke("setLipSync($clampedValue)")
    }

    fun stopLipSync() {
        _uiState.update { it.copy(lipSyncValue = 0f) }
        webViewBridge?.invoke("setLipSync(0)")
    }

    fun loadModel(url: String) {
        viewModelScope.launch {
            vrmRepo.setVrmModel(url)
            _uiState.update { it.copy(modelUrl = url, isModelLoaded = false) }
            webViewBridge?.invoke("loadModel('$url')")
        }
    }

    fun onModelLoaded() {
        _uiState.update { it.copy(isModelLoaded = true) }
    }

    fun triggerBlink() {
        webViewBridge?.invoke("triggerBlink()")
    }

    fun setAvatarName(name: String) {
        viewModelScope.launch {
            vrmRepo.setAvatarName(name)
        }
    }

    fun handleEmotionChange(emotion: String) {
        setExpression(emotion)
        playAction(emotion)
    }

    fun startTalking(durationMs: Long = 3000) {
        webViewBridge?.invoke("startTalking($durationMs)")
    }

    fun stopTalking() {
        webViewBridge?.invoke("stopTalking()")
    }

    fun playGuideAction(action: String) {
        webViewBridge?.invoke("playGuideAction('$action')")
    }

    /**
     * 随机播放动作（参考duix-mobile的startRandomMotion）
     */
    fun playRandomAction() {
        webViewBridge?.invoke("playRandomAction()")
    }

    /**
     * 获取可用动作列表（参考duix-mobile的ModelInfo.motionRegions）
     */
    fun getAvailableActions() {
        webViewBridge?.invoke("getAvailableActions()")
    }

    /**
     * 播放指定动画
     */
    fun playAnimation(animName: String) {
        webViewBridge?.invoke("playAnimation('$animName')")
    }

    /**
     * 停止当前动画
     */
    fun stopAnimation() {
        webViewBridge?.invoke("stopAnimation()")
    }

    /**
     * 获取所有可用动画名称
     */
    fun getAnimationNames() {
        webViewBridge?.invoke("getAnimationNames()")
    }
}
