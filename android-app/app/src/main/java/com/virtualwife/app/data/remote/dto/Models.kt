package com.virtualwife.app.data.remote.dto

import com.google.gson.annotations.SerializedName

// ============ 通用后端返回格式 (Spring Boot Result) ============

data class AdminResult<T>(
    val code: Int = 0,
    val message: String? = null,
    val data: T? = null,
    val timestamp: Long = 0
) {
    val isSuccess: Boolean get() = code == 200
}

// ============ 聊天相关 (Django) ============

data class ChatRequest(
    val query: String,
    @SerializedName("you_name")
    val youName: String = "游客",
    @SerializedName("avatar_name")
    val avatarName: String = "小莉",
    @SerializedName("session_id")
    val sessionId: String = "",
    @SerializedName("user_interest")
    val userInterest: String = ""
)

data class ChatResponse(
    val message: String? = null,
    val result: String? = null,
    val status: String? = null,
    val error: String? = null
)

// ============ WebSocket 消息 ============

data class WsChatMessage(
    val type: String = "chat.message",
    val content: String,
    @SerializedName("avatar_name")
    val avatarName: String = "小莉",
    @SerializedName("session_id")
    val sessionId: String = ""
)

data class WsImageRecognize(
    val type: String = "image.recognize",
    @SerializedName("image_base64")
    val imageBase64: String,
    @SerializedName("session_id")
    val sessionId: String = "",
    val location: WsLocation? = null
)

data class WsLocation(
    val lat: Double,
    val lng: Double
)

data class WsStreamChunk(
    val type: String = "chat.stream_chunk",
    val content: String,
    @SerializedName("is_final")
    val isFinal: Boolean = false,
    @SerializedName("message_id")
    val messageId: String = ""
)

data class WsMessageComplete(
    val type: String = "chat.message.complete",
    val content: String,
    val emotion: String = "neutral",
    @SerializedName("facial_expression")
    val facialExpression: String = "neutral",
    @SerializedName("tts_audio_url")
    val ttsAudioUrl: String? = null,
    @SerializedName("tts_base64")
    val ttsBase64: String? = null,
    @SerializedName("message_id")
    val messageId: String = "",
    @SerializedName("session_id")
    val sessionId: String = "",
    val timestamp: Long = 0,
    val sources: List<WsSource>? = null
)

data class WsSource(
    @SerializedName("chunk_id")
    val chunkId: Int = 0,
    @SerializedName("doc_name")
    val docName: String = "",
    val page: Int = 0
)

data class WsImageRecognizeResult(
    val type: String = "image.recognize.result",
    @SerializedName("spot_name")
    val spotName: String = "",
    val description: String = "",
    val confidence: Double = 0.0,
    @SerializedName("message_id")
    val messageId: String = ""
)

data class WsPing(val type: String = "ping")
data class WsPong(val type: String = "pong")

data class RealtimeMessage(
    val type: String = "user",
    @SerializedName("user_name")
    val userName: String? = null,
    val content: String = "",
    val emote: String = "neutral",
    val action: String? = null,
    val expand: String? = null
)

// ============ 登录/认证 (Spring Boot) ============
// 修复: 后端实际返回 {token, user: {id, username, ...}}

data class LoginRequest(
    val username: String,
    val password: String
)

data class RefreshTokenRequest(
    val token: String
)

data class LoginData(
    val token: String? = null,
    val user: LoginUserDto? = null
)

data class LoginUserDto(
    val id: Long = 0,
    val username: String? = null,
    val nickname: String? = null,
    val avatar: String? = null,
    val role: String? = null,
    val status: Int = 1
)

// ============ TTS ============

data class TtsRequest(
    val text: String,
    val voiceId: String = "zh-CN-XiaoyiNeural",
    val type: String = "edge"
)

data class TtsResponse(
    val code: Int = 0,
    val message: String? = null,
    val data: TtsData? = null,
    val status: String? = null,
    val error: String? = null
)

data class TtsData(
    @SerializedName("audio_url")
    val audioUrl: String? = null,
    @SerializedName("audio_base64")
    val audioBase64: String? = null,
    val status: String? = null
)

data class VoiceItem(
    val id: String = "",
    val name: String = "",
    val locale: String = ""
)

// ============ 路线推荐 (Spring Boot) ============

data class RoutePageData(
    val records: List<RouteDto>? = null,
    val total: Long = 0,
    val size: Int = 10,
    val current: Int = 1,
    val pages: Int = 0
)

data class RouteDto(
    val id: Long = 0,
    val kbId: Long? = null,
    val routeName: String = "",
    val interestTags: String? = null,
    val timeBudget: Int? = null,
    val energyLevel: Int? = null,
    val description: String? = null,
    val sortOrder: Int = 0,
    val createTime: String? = null
)

data class SpotPageData(
    val records: List<SpotDto>? = null,
    val total: Long = 0,
    val size: Int = 50,
    val current: Int = 1,
    val pages: Int = 0
)

data class SpotDto(
    val id: Long = 0,
    val routeId: Long = 0,
    val spotName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val geoRadius: Int = 50,
    val narrateText: String? = null,
    val imageUrl: String? = null,
    val spotOrder: Int = 0
)

// ============ 数字人形象 (Spring Boot) ============

data class AvatarPageData(
    val records: List<AvatarConfigDto>? = null,
    val total: Long = 0,
    val size: Int = 10,
    val current: Int = 1,
    val pages: Int = 0
)

data class AvatarConfigDto(
    val id: Long = 0,
    val avatarName: String = "",
    val vrmModelUrl: String = "",
    val thumbnailUrl: String? = null,
    val persona: String? = null,
    val personality: String? = null,
    val voiceType: String? = null,
    val emotionConfig: String? = null,
    val isSystem: Int = 0,
    val isDefault: Int = 0,
    val sortOrder: Int = 0,
    val status: Int = 1,
    val backgroundUrl: String? = null,
    val vrmDisplayUrl: String? = null,
    val thumbnailDisplayUrl: String? = null,
    val backgroundDisplayUrl: String? = null,
    // 动作配置
    val speechRate: Double = 1.0,
    val armAngle: Int = 50,
    val idleAnimation: String = "idle",
    val talkAnimation: String = "natural",
    val animationIntensity: Double = 0.7
)

// ============ 版本检查 ============

data class VersionCheckResponse(
    @SerializedName("latest_version")
    val latestVersion: String = "",
    @SerializedName("min_version")
    val minVersion: String = "",
    @SerializedName("download_url")
    val downloadUrl: String = "",
    @SerializedName("force_update")
    val forceUpdate: Boolean = false
)

// ============ 系统配置 ============

data class SysConfigResponse(
    val config: Map<String, Any>? = null,
    val status: String? = null
)

// ============ 知识库 (Spring Boot) ============

data class KnowledgePageData(
    val records: List<KnowledgeBaseDto>? = null,
    val total: Long = 0,
    val size: Int = 10,
    val current: Int = 1,
    val pages: Int = 0
)

data class KnowledgeBaseDto(
    val id: Long = 0,
    val kbName: String = "",
    val description: String? = null,
    val embeddingModel: String? = null,
    val vectorDbType: String? = null,
    val itemCount: Int = 0,
    val status: Int = 1
)

data class KnowledgeItemPageData(
    val records: List<KnowledgeItemDto>? = null,
    val total: Long = 0,
    val size: Int = 10,
    val current: Int = 1,
    val pages: Int = 0
)

data class KnowledgeItemDto(
    val id: Long = 0,
    val kbId: Long = 0,
    val title: String = "",
    val content: String = "",
    val vectorStatus: Int = 0
)

// ============ RAG 文档 (Spring Boot) ============

data class RagDocumentPageData(
    val records: List<RagDocumentDto>? = null,
    val total: Long = 0,
    val size: Int = 10,
    val current: Int = 1,
    val pages: Int = 0
)

data class RagDocumentDto(
    val id: Long = 0,
    val kbId: Long = 0,
    val docName: String = "",
    val docType: String = "",
    val fileSize: Long = 0,
    val chunkCount: Int = 0,
    val processStatus: Int = 0
)

// ============ LLM 配置 (Spring Boot) ============

data class LlmConfigPageData(
    val records: List<LlmConfigDto>? = null,
    val total: Long = 0,
    val size: Int = 10,
    val current: Int = 1,
    val pages: Int = 0
)

data class LlmConfigDto(
    val id: Long = 0,
    val configName: String = "",
    val provider: String = "",
    val apiUrl: String = "",
    val modelName: String = "",
    val isDefault: Int = 0,
    val connectStatus: Int = 0
)

// ============ 统计数据 (Spring Boot) ============

data class DashboardData(
    val totalUsers: Int = 0,
    val totalMessages: Int = 0,
    val activeUsers: Int = 0,
    val todayMessages: Int = 0
)

// ============ 聊天记录 (Spring Boot) ============

data class ChatRecordPageData(
    val records: List<ChatRecordDto>? = null,
    val total: Long = 0,
    val size: Int = 10,
    val current: Int = 1,
    val pages: Int = 0
)

data class ChatRecordDto(
    val id: Long = 0,
    val userId: Long = 0,
    val sessionId: String = "",
    val avatarName: String = "",
    val messageType: String = "",
    val content: String = "",
    val emotion: String? = null,
    val createTime: String? = null
)
