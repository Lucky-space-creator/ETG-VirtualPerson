package com.virtualwife.app.data.remote

import com.virtualwife.app.data.local.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val prefs: PreferencesManager
) : Interceptor {

    @Volatile
    private var isRefreshing = false

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        // 登录/刷新接口不需要 Token
        val path = original.url.encodedPath
        if (path.contains("auth/login") || path.contains("auth/refresh")) {
            return chain.proceed(original.newBuilder()
                .header("Content-Type", "application/json")
                .build())
        }

        val token = runBlocking { prefs.token.first() }
        if (token.isEmpty()) {
            return chain.proceed(original.newBuilder()
                .header("Content-Type", "application/json")
                .build())
        }

        val authenticated = original.newBuilder()
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .build()

        val response = chain.proceed(authenticated)

        // Token 过期自动刷新（避免并发刷新）
        if (response.code == 401 && !isRefreshing) {
            response.close()
            isRefreshing = true
            val newToken = refreshToken(token)
            isRefreshing = false

            if (newToken != null) {
                val retried = original.newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .header("Content-Type", "application/json")
                    .build()
                return chain.proceed(retried)
            }
        }

        return response
    }

    private fun refreshToken(expiredToken: String): String? {
        return try {
            // 使用独立的 OkHttpClient 避免循环调用
            val plainClient = okhttp3.OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val retrofit = retrofit2.Retrofit.Builder()
                .baseUrl(com.virtualwife.app.util.Constants.ADMIN_BASE_URL)
                .client(plainClient)
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                .build()

            val api = retrofit.create(ApiService::class.java)
            val result = runBlocking {
                api.refreshToken(
                    com.virtualwife.app.data.remote.dto.RefreshTokenRequest(expiredToken)
                )
            }

            if (result.isSuccessful && result.body()?.isSuccess == true) {
                val newToken = result.body()!!.data?.token
                if (newToken != null) {
                    runBlocking { prefs.saveToken(newToken) }
                }
                newToken
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
