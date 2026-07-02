package com.virtualwife.app.data.remote

import com.virtualwife.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ============ 聊天相关 (公开接口) ============

    @POST("chatbot/chat")
    suspend fun chat(@Body request: ChatRequest): Response<ChatResponse>

    @POST("speech/tts/generate")
    suspend fun generateTts(@Body request: TtsRequest): Response<TtsResponse>

    @GET("speech/tts/voices")
    suspend fun getVoices(): Response<List<VoiceItem>>

    @GET("chatbot/config/get")
    suspend fun getConfig(): Response<SysConfigResponse>

    // ============ 用户头像 (公开接口) ============

    @Multipart
    @POST("user/avatar/upload")
    suspend fun uploadUserAvatar(@Part file: okhttp3.MultipartBody.Part): Response<AdminResult<Map<String, String>>>

    // ============ 认证相关 (公开接口) ============

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AdminResult<LoginData>>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<AdminResult<LoginData>>

    // ============ 公开GET接口（无需Token） ============

    @GET("avatar/page")
    suspend fun getAvatars(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 10,
        @Query("mobile") mobile: Boolean = true
    ): Response<AdminResult<AvatarPageData>>

    @GET("avatar/{id}")
    suspend fun getAvatarById(
        @Path("id") id: Long,
        @Query("mobile") mobile: Boolean = true
    ): Response<AdminResult<AvatarConfigDto>>

    @GET("avatar/default")
    suspend fun getDefaultAvatar(
        @Query("mobile") mobile: Boolean = true
    ): Response<AdminResult<AvatarConfigDto>>

    @GET("route/page")
    suspend fun getRoutes(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 10,
        @Query("keyword") keyword: String? = null
    ): Response<AdminResult<RoutePageData>>

    @GET("route/{routeId}/spot/page")
    suspend fun getSpotsByRoute(
        @Path("routeId") routeId: Long,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 50
    ): Response<AdminResult<SpotPageData>>

    @GET("kb/page")
    suspend fun getKnowledgeBases(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 10,
        @Query("keyword") keyword: String? = null
    ): Response<AdminResult<KnowledgePageData>>

    @GET("kb/{kbId}/item/page")
    suspend fun getKnowledgeItems(
        @Path("kbId") kbId: Long,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): Response<AdminResult<KnowledgeItemPageData>>

    @GET("rag/page")
    suspend fun getRagDocuments(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): Response<AdminResult<RagDocumentPageData>>

    @GET("llm/page")
    suspend fun getLlmConfigs(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): Response<AdminResult<LlmConfigPageData>>

    @GET("statistics/dashboard")
    suspend fun getDashboard(): Response<AdminResult<DashboardData>>

    @GET("chat/page")
    suspend fun getChatRecords(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): Response<AdminResult<ChatRecordPageData>>
}
