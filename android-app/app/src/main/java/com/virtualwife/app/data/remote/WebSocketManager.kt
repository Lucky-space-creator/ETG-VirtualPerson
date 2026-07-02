package com.virtualwife.app.data.remote

import com.google.gson.Gson
import com.virtualwife.app.util.Constants
import com.virtualwife.app.data.remote.dto.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import okhttp3.*

sealed class WsEvent {
    data class StreamChunk(val chunk: WsStreamChunk) : WsEvent()
    data class MessageComplete(val message: WsMessageComplete) : WsEvent()
    data class ImageRecognizeResult(val result: WsImageRecognizeResult) : WsEvent()
    data class RealtimeMessage(val message: com.virtualwife.app.data.remote.dto.RealtimeMessage) : WsEvent()
    data class Error(val throwable: Throwable) : WsEvent()
    data object Connected : WsEvent()
    data object Disconnected : WsEvent()
}

class WebSocketManager {

    private val client = OkHttpClient.Builder()
        .pingInterval(Constants.WS_PING_INTERVAL, java.util.concurrent.TimeUnit.MILLISECONDS)
        .build()

    private val gson = Gson()
    private var webSocket: WebSocket? = null
    @Volatile private var isConnected = false
    private var retryCount = 0
    @Volatile private var shouldReconnect = true

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var reconnectJob: Job? = null

    private val eventChannel = Channel<WsEvent>(Channel.BUFFERED)
    val events: Flow<WsEvent> = eventChannel.receiveAsFlow()

    fun connect(wsUrl: String = Constants.WS_URL) {
        // WebSocket已禁用，改用HTTP聊天
        // 不再尝试连接WebSocket
        return

        shouldReconnect = true
        reconnectJob?.cancel()

        val request = Request.Builder().url(wsUrl).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                retryCount = 0
                eventChannel.trySend(WsEvent.Connected)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = gson.fromJson(text, com.google.gson.JsonObject::class.java)
                    val type = json.get("type")?.asString ?: return

                    when (type) {
                        "chat.stream_chunk" -> {
                            val chunk = gson.fromJson(text, WsStreamChunk::class.java)
                            eventChannel.trySend(WsEvent.StreamChunk(chunk))
                        }
                        "chat.message.complete" -> {
                            val complete = gson.fromJson(text, WsMessageComplete::class.java)
                            eventChannel.trySend(WsEvent.MessageComplete(complete))
                        }
                        "image.recognize.result" -> {
                            val result = gson.fromJson(text, WsImageRecognizeResult::class.java)
                            eventChannel.trySend(WsEvent.ImageRecognizeResult(result))
                        }
                        "user" -> {
                            val msg = gson.fromJson(text, RealtimeMessage::class.java)
                            eventChannel.trySend(WsEvent.RealtimeMessage(msg))
                        }
                        "pong" -> { /* 心跳响应 */ }
                        else -> {
                            try {
                                val msg = gson.fromJson(text, RealtimeMessage::class.java)
                                if (msg.content.isNotEmpty()) {
                                    eventChannel.trySend(WsEvent.RealtimeMessage(msg))
                                }
                            } catch (_: Exception) { }
                        }
                    }
                } catch (e: Exception) {
                    eventChannel.trySend(WsEvent.Error(e))
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                eventChannel.trySend(WsEvent.Error(t))
                scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                eventChannel.trySend(WsEvent.Disconnected)
                scheduleReconnect()
            }
        })
    }

    fun sendChatMessage(content: String, avatarName: String = "小莉", sessionId: String = "") {
        if (!isConnected) return
        val message = WsChatMessage(
            content = content,
            avatarName = avatarName,
            sessionId = sessionId
        )
        webSocket?.send(gson.toJson(message))
    }

    fun sendImageRecognize(imageBase64: String, sessionId: String = "", lat: Double? = null, lng: Double? = null) {
        if (!isConnected) return
        val message = WsImageRecognize(
            imageBase64 = imageBase64,
            sessionId = sessionId,
            location = if (lat != null && lng != null) WsLocation(lat, lng) else null
        )
        webSocket?.send(gson.toJson(message))
    }

    fun sendPing() {
        webSocket?.send(gson.toJson(WsPing()))
    }

    fun disconnect() {
        shouldReconnect = false
        reconnectJob?.cancel()
        scope.coroutineContext.cancelChildren()
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        isConnected = false
    }

    fun isConnected(): Boolean = isConnected

    private fun scheduleReconnect() {
        if (!shouldReconnect) return

        val delay = minOf(
            Constants.WS_RECONNECT_BASE_DELAY * (1 shl retryCount.coerceAtMost(5)),
            Constants.WS_RECONNECT_MAX_DELAY
        )

        reconnectJob = scope.launch {
            delay(delay)
            if (shouldReconnect && !isConnected) {
                retryCount++
                connect()
            }
        }
    }
}
