package com.virtualwife.app.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.virtualwife.app.MainApplication
import com.virtualwife.app.data.local.entity.ChatMessageEntity
import com.virtualwife.app.data.remote.RetrofitClient
import com.virtualwife.app.data.repository.ChatRepository
import com.virtualwife.app.data.repository.VoiceRepository
import com.virtualwife.app.voice.VoiceManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

private const val TAG = "ChatViewModel"

data class ChatUiState(
    val messages: List<ChatMessageEntity> = emptyList(),
    val isAiTyping: Boolean = false,
    val currentEmotion: String = "neutral",
    val error: String? = null,
    val isTtsPlaying: Boolean = false,
    val selectedRouteId: Long? = null,
    val selectedRouteName: String? = null,
    val isTourActive: Boolean = false,
    val tourSpots: List<TourSpot> = emptyList(),
    val currentSpotIndex: Int = -1,
    val visitedSpots: Set<Int> = emptySet()
)

data class TourSpot(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val geoRadius: Int,
    val narrateText: String?,
    val spotOrder: Int
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MainApplication
    private val chatRepo = ChatRepository(
        app.chatDatabase.chatMessageDao(),
        app.preferencesManager,
        com.virtualwife.app.data.remote.WebSocketManager()
    )
    private val voiceRepo = VoiceRepository(app.preferencesManager)
    private val voiceManager = VoiceManager(application)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var ttsJob: Job? = null

    init {
        observeMessages()
        syncServerHistory()
    }

    private fun syncServerHistory() {
        viewModelScope.launch {
            try { chatRepo.syncFromServer() } catch (e: Exception) { }
        }
    }

    private fun observeMessages() {
        viewModelScope.launch {
            val sessionId = app.preferencesManager.getOrCreateSessionId()
            chatRepo.getMessages(sessionId).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    fun sendTextMessage(content: String) {
        if (content.isBlank()) return

        // 取消上一段TTS播放
        ttsJob?.cancel()
        voiceManager.stopTts()

        viewModelScope.launch {
            _uiState.update { it.copy(isAiTyping = true, error = null, isTtsPlaying = false) }

            val result = chatRepo.sendTextMessage(content)

            if (result.isSuccess) {
                val reply = result.getOrNull() ?: ""

                val emotion = when {
                    reply.contains("开心") || reply.contains("欢迎") || reply.contains("嗨") -> "happy"
                    reply.contains("抱歉") || reply.contains("遗憾") -> "sad"
                    reply.contains("哇") || reply.contains("惊讶") -> "surprised"
                    reply.contains("思考") || reply.contains("让我想想") -> "thinking"
                    reply.contains("解释") || reply.contains("说明") -> "explaining"
                    else -> "happy"
                }
                _uiState.update { it.copy(currentEmotion = emotion, isAiTyping = false) }

                // 异步TTS，可被取消
                ttsJob = launch { playTts(reply) }
            } else {
                _uiState.update {
                    it.copy(error = result.exceptionOrNull()?.message ?: "发送失败", isAiTyping = false)
                }
            }
        }
    }

    /**
     * 调用TTS API生成语音并播放
     * 支持长文本分段播放
     */
    private fun playTts(text: String) {
        viewModelScope.launch {
            try {
                // 过滤emoji和特殊符号，避免TTS读出表情
                val cleanText = text
                    .replace(Regex("[\\p{So}\\p{Cn}]"), "") // emoji
                    .replace(Regex("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+"), "") // surrogate pairs
                    .replace(Regex("[*#~`>\\-─═╔╗╚╝│├┤┬┴┼]"), "") // markdown符号
                    .replace(Regex("\\s+"), " ") // 多余空格
                    .trim()

                if (cleanText.isBlank()) {
                    Log.d(TAG, "TTS skipped: text is empty after cleaning")
                    return@launch
                }

                // 将长文本分段
                val segments = splitTextForTts(cleanText)
                Log.d(TAG, "TTS segments: ${segments.size}, total length: ${cleanText.length}")

                _uiState.update { it.copy(isTtsPlaying = true) }

                // 按顺序播放每一段
                for ((index, segment) in segments.withIndex()) {
                    if (!_uiState.value.isTtsPlaying) {
                        // 如果被用户停止，退出循环
                        break
                    }

                    Log.d(TAG, "TTS segment ${index + 1}/${segments.size}: ${segment.take(30)}...")

                    val ttsResult = voiceRepo.generateTts(segment)
                    if (ttsResult.isSuccess) {
                        val audioSource = ttsResult.getOrNull() ?: ""
                        Log.d(TAG, "TTS audio: len=${audioSource.length}")

                        // 播放当前段，等待完成
                        val completed = suspendCancellableCoroutine<Boolean> { cont ->
                            voiceManager.playTtsAudio(
                                audioSource,
                                onComplete = {
                                    if (cont.isActive) {
                                        cont.resume(true) {}
                                    }
                                },
                                onAmplitude = { amplitude ->
                                    // 口型同步振幅值（可用于后续扩展）
                                }
                            )
                            // 注册取消回调
                            cont.invokeOnCancellation {
                                voiceManager.stopTts()
                            }
                        }

                        if (!completed) {
                            Log.w(TAG, "TTS segment failed")
                            break
                        }
                    } else {
                        Log.e(TAG, "TTS failed for segment ${index + 1}: ${ttsResult.exceptionOrNull()?.message}")
                    }
                }

                _uiState.update { it.copy(isTtsPlaying = false) }
                Log.d(TAG, "TTS playback completed")
            } catch (e: Exception) {
                Log.e(TAG, "TTS exception: ${e.message}", e)
                _uiState.update { it.copy(isTtsPlaying = false) }
            }
        }
    }

    /**
     * 将长文本分段，每段不超过100字符
     * 按句子边界分割，保持语义完整
     */
    private fun splitTextForTts(text: String): List<String> {
        if (text.length <= 100) return listOf(text)

        val sentenceEnders = Regex("[。！？；.!?;]")
        val segments = mutableListOf<String>()
        var start = 0

        while (start < text.length) {
            val end = minOf(start + 100, text.length)
            if (end >= text.length) {
                segments.add(text.substring(start).trim())
                break
            }

            // 在[start, end]范围内找最后一个句子结尾
            val chunk = text.substring(start, end)
            val lastEnder = sentenceEnders.findAll(chunk).lastOrNull()

            if (lastEnder != null) {
                val splitAt = start + lastEnder.range.last + 1
                val segment = text.substring(start, splitAt).trim()
                if (segment.isNotEmpty()) segments.add(segment)
                start = splitAt
            } else {
                // 没有句子结尾，强制在100字符处截断
                segments.add(chunk.trim())
                start = end
            }
        }

        return segments.ifEmpty { listOf(text) }
    }

    fun stopTts() {
        ttsJob?.cancel()
        voiceManager.stopTts()
        _uiState.update { it.copy(isTtsPlaying = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun connectWebSocket() {
        // WebSocket已禁用，改用HTTP聊天
    }

    fun disconnectWebSocket() {
        // WebSocket已禁用
    }

    fun sendImageForRecognition(imageBase64: String, lat: Double? = null, lng: Double? = null) {
        // 拍照识别功能（待实现）
    }

    fun selectRoute(routeId: Long, routeName: String) {
        Log.d(TAG, "selectRoute: routeId=$routeId, routeName=$routeName")
        _uiState.update { it.copy(selectedRouteId = routeId, selectedRouteName = routeName) }
        loadRouteSpots(routeId)
    }

    private fun loadRouteSpots(routeId: Long) {
        viewModelScope.launch {
            try {
                val res = RetrofitClient.djangoApi.getSpotsByRoute(routeId = routeId)
                if (res.isSuccessful && res.body()?.isSuccess == true) {
                    val spots = res.body()!!.data?.records?.map { dto ->
                        TourSpot(
                            id = dto.id,
                            name = dto.spotName,
                            latitude = dto.latitude,
                            longitude = dto.longitude,
                            geoRadius = dto.geoRadius,
                            narrateText = dto.narrateText,
                            spotOrder = dto.spotOrder
                        )
                    }?.sortedBy { it.spotOrder } ?: emptyList()
                    _uiState.update { it.copy(tourSpots = spots) }
                    Log.d(TAG, "Loaded ${spots.size} spots for route")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load spots: ${e.message}")
            }
        }
    }

    fun startTour() {
        val routeName = _uiState.value.selectedRouteName ?: return
        val spots = _uiState.value.tourSpots
        _uiState.update { it.copy(isTourActive = true, visitedSpots = emptySet(), currentSpotIndex = -1) }

        // AI先做总体介绍
        val spotNames = spots.joinToString("、") { it.name }
        val introMsg = "欢迎来到${routeName}！本次游览共经过${spots.size}个景点：${spotNames}。请跟随我的脚步，我会为您逐一介绍每个景点的历史文化故事。准备好了吗？我们出发吧！"

        viewModelScope.launch {
            _uiState.update { it.copy(isAiTyping = true) }
            val sessionId = app.preferencesManager.getOrCreateSessionId()
            val avatarName = app.preferencesManager.avatarName.first()
            val entity = ChatMessageEntity(
                sessionId = sessionId,
                messageType = "ai",
                content = introMsg,
                emotion = "happy",
                avatarName = avatarName
            )
            app.chatDatabase.chatMessageDao().insert(entity)
            _uiState.update { it.copy(isAiTyping = false, currentEmotion = "happy") }
            playTts(introMsg)
        }
    }

    /**
     * GPS位置更新 - 检测是否进入景点围栏
     */
    fun onLocationUpdate(latitude: Double, longitude: Double) {
        if (!_uiState.value.isTourActive) return

        val spots = _uiState.value.tourSpots
        val visited = _uiState.value.visitedSpots

        for ((index, spot) in spots.withIndex()) {
            if (visited.contains(index)) continue

            val distance = calculateDistance(latitude, longitude, spot.latitude, spot.longitude)
            if (distance <= spot.geoRadius) {
                Log.d(TAG, "Entered geo-fence: ${spot.name} (distance=${distance}m)")
                _uiState.update {
                    it.copy(
                        currentSpotIndex = index,
                        visitedSpots = it.visitedSpots + index
                    )
                }
                narrateSpot(spot)
                break
            }
        }
    }

    /**
     * 讲解景点
     */
    private fun narrateSpot(spot: TourSpot) {
        val narration = spot.narrateText
            ?: "我们现在来到了${spot.name}，这里是灵山胜境的重要景点之一。"

        viewModelScope.launch {
            _uiState.update { it.copy(isAiTyping = true) }
            val sessionId = app.preferencesManager.getOrCreateSessionId()
            val avatarName = app.preferencesManager.avatarName.first()
            val entity = ChatMessageEntity(
                sessionId = sessionId,
                messageType = "ai",
                content = "📍 ${spot.name}\n\n$narration",
                emotion = "happy",
                avatarName = avatarName
            )
            app.chatDatabase.chatMessageDao().insert(entity)
            _uiState.update { it.copy(isAiTyping = false, currentEmotion = "happy") }
            playTts(narration)
        }
    }

    /**
     * 计算两点之间的距离（米）
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // 地球半径（米）
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    fun stopTour() {
        _uiState.update {
            it.copy(isTourActive = false, selectedRouteId = null, selectedRouteName = null,
                tourSpots = emptyList(), currentSpotIndex = -1, visitedSpots = emptySet())
        }
        sendTextMessage("本次游览结束，感谢您的陪伴！期待下次再见！")
    }

    fun clearHistory() {
        viewModelScope.launch {
            val sessionId = app.preferencesManager.getOrCreateSessionId()
            chatRepo.clearHistory(sessionId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.release()
    }
}
