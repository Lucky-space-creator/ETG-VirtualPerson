package com.virtualwife.app.data.repository

import com.virtualwife.app.data.local.PreferencesManager
import com.virtualwife.app.data.local.dao.ChatMessageDao
import com.virtualwife.app.data.local.entity.ChatMessageEntity
import com.virtualwife.app.data.remote.RetrofitClient
import com.virtualwife.app.data.remote.WebSocketManager
import com.virtualwife.app.data.remote.WsEvent
import com.virtualwife.app.data.remote.dto.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ChatRepository(
    private val dao: ChatMessageDao,
    private val prefs: PreferencesManager,
    val wsManager: WebSocketManager
) {

    val wsEvents: Flow<WsEvent> = wsManager.events

    fun getMessages(sessionId: String): Flow<List<ChatMessageEntity>> {
        return dao.getMessagesBySession(sessionId)
    }

    suspend fun getRecentContext(sessionId: String, limit: Int = 6): List<ChatMessageEntity> {
        return dao.getRecentMessages(sessionId, limit)
    }

    suspend fun sendTextMessage(content: String): Result<String> {
        return try {
            val sessionId = prefs.getOrCreateSessionId()
            val avatarName = prefs.avatarName.first()

            // 保存用户消息到本地
            val userMsg = ChatMessageEntity(
                sessionId = sessionId,
                messageType = "user",
                content = content,
                avatarName = avatarName
            )
            dao.insert(userMsg)

            // 通过HTTP发送并获取AI回复
            val httpResult = chatViaHttp(content)
            if (httpResult.isSuccess) {
                val response = httpResult.getOrNull()
                val reply = response?.message ?: response?.result ?: "暂无回复"
                // 保存AI回复到本地
                saveAiMessage(content = reply, emotion = "neutral")
                Result.success(reply)
            } else {
                Result.failure(httpResult.exceptionOrNull() ?: Exception("发送失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveAiMessage(
        content: String,
        emotion: String = "neutral",
        ttsAudioUrl: String? = null,
        ttsBase64: String? = null,
        messageId: String = "",
        sourcesJson: String? = null
    ): Long {
        val sessionId = prefs.getOrCreateSessionId()
        val avatarName = prefs.avatarName.first()
        val entity = ChatMessageEntity(
            sessionId = sessionId,
            messageType = "ai",
            content = content,
            emotion = emotion,
            avatarName = avatarName,
            ttsAudioUrl = ttsAudioUrl,
            ttsBase64 = ttsBase64,
            messageId = messageId,
            sourcesJson = sourcesJson
        )
        return dao.insert(entity)
    }

    suspend fun updateAiMessage(id: Long, content: String, emotion: String) {
        val messages = dao.getMessagesBySessionSync(prefs.getOrCreateSessionId())
        val msg = messages.find { it.id == id }
        if (msg != null) {
            dao.update(msg.copy(content = content, emotion = emotion))
        }
    }

    suspend fun sendImageRecognize(imageBase64: String, lat: Double? = null, lng: Double? = null) {
        val sessionId = prefs.getOrCreateSessionId()
        wsManager.sendImageRecognize(imageBase64, sessionId, lat, lng)
    }

    fun connectWebSocket() {
        wsManager.connect()
    }

    fun disconnectWebSocket() {
        wsManager.disconnect()
    }

    suspend fun clearHistory(sessionId: String) {
        dao.deleteBySession(sessionId)
    }

    /**
     * 从服务器同步聊天记录到本地
     * 本地缓存优先显示，后台同步最新数据
     */
    suspend fun syncFromServer() {
        try {
            val sessionId = prefs.getOrCreateSessionId()
            val localCount = dao.getMessageCount(sessionId)

            // 从服务器获取最新记录（使用adminApi带认证）
            val res = RetrofitClient.adminApi.getChatRecords(pageNum = 1, pageSize = 50)
            if (res.isSuccessful && res.body()?.isSuccess == true) {
                val serverRecords = res.body()!!.data?.records ?: emptyList()

                // 过滤当前会话的记录
                val sessionRecords = serverRecords.filter { it.sessionId == sessionId }

                if (sessionRecords.isNotEmpty() && sessionRecords.size > localCount) {
                    // 服务器记录比本地多，同步到本地
                    val entities = sessionRecords.map { record ->
                        ChatMessageEntity(
                            sessionId = record.sessionId,
                            messageType = record.messageType,
                            content = record.content,
                            avatarName = record.avatarName,
                            emotion = record.emotion ?: "neutral"
                        )
                    }
                    dao.insertAll(entities)
                }
            }
        } catch (e: Exception) {
            // 同步失败不影响本地使用
        }
    }

    suspend fun chatViaHttp(query: String): Result<ChatResponse> {
        return try {
            val sessionId = prefs.getOrCreateSessionId()
            val avatarName = prefs.avatarName.first()
            val interestTags = prefs.interestTags.first()
            val userInterest = interestTags.joinToString(", ")
            val scenicSpotId = prefs.scenicSpotId.first()

            // 使用adminApi（带认证）而不是djangoApi
            val response = RetrofitClient.adminApi.chat(
                ChatRequest(
                    query = query,
                    avatarName = avatarName,
                    sessionId = sessionId,
                    userInterest = userInterest,
                    scenicSpotId = if (scenicSpotId > 0) scenicSpotId else null
                )
            )
            if (response.isSuccessful) {
                Result.success(response.body() ?: ChatResponse())
            } else {
                Result.failure(Exception("HTTP聊天请求失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
