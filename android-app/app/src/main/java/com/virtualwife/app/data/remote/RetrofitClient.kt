package com.virtualwife.app.data.remote

import com.virtualwife.app.data.local.PreferencesManager
import com.virtualwife.app.util.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // 基础 OkHttpClient（无认证）
    private val baseClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    // Django API 不需要 JWT 认证
    val djangoApi: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(baseClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    // 带 Token 自动刷新的 adminApi（延迟初始化）
    private var _adminApi: ApiService? = null

    fun initAdminApi(prefs: PreferencesManager): ApiService {
        if (_adminApi != null) return _adminApi!!

        val authClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(AuthInterceptor(prefs))
            .build()

        _adminApi = Retrofit.Builder()
            .baseUrl(Constants.ADMIN_BASE_URL)
            .client(authClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

        return _adminApi!!
    }

    val adminApi: ApiService
        get() = _adminApi ?: throw IllegalStateException(
            "RetrofitClient.adminApi 未初始化，请先调用 initAdminApi()"
        )
}
