package com.virtualwife.app.data.repository

import com.virtualwife.app.data.local.PreferencesManager
import com.virtualwife.app.data.remote.RetrofitClient
import com.virtualwife.app.data.remote.dto.LoginRequest
import com.virtualwife.app.data.remote.dto.RefreshTokenRequest
import kotlinx.coroutines.flow.first

class AuthRepository(private val prefs: PreferencesManager) {

    suspend fun login(username: String, password: String): Result<String> {
        return try {
            val response = RetrofitClient.adminApi.login(
                LoginRequest(username, password)
            )
            if (response.isSuccessful && response.body()?.isSuccess == true) {
                val data = response.body()!!.data
                val token = data?.token
                    ?: return Result.failure(Exception("Token为空"))
                // 修复: 从 user 子对象获取用户信息
                val user = data.user
                prefs.saveToken(token)
                prefs.saveUserInfo(
                    user?.username ?: username,
                    user?.id ?: 0L
                )
                Result.success(token)
            } else {
                val msg = response.body()?.message ?: "登录失败"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshToken(): Result<String> {
        return try {
            val currentToken = prefs.token.first()
            if (currentToken.isEmpty()) {
                return Result.failure(Exception("无Token可刷新"))
            }
            val response = RetrofitClient.adminApi.refreshToken(
                RefreshTokenRequest(currentToken)
            )
            if (response.isSuccessful && response.body()?.isSuccess == true) {
                val newToken = response.body()!!.data?.token
                    ?: return Result.failure(Exception("刷新Token为空"))
                prefs.saveToken(newToken)
                Result.success(newToken)
            } else {
                Result.failure(Exception("Token刷新失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        prefs.clearAll()
    }

    suspend fun isLoggedIn(): Boolean {
        return prefs.isLoggedIn.first()
    }

    suspend fun getToken(): String {
        return prefs.token.first()
    }

    suspend fun saveRememberMe(remember: Boolean, username: String, password: String) {
        prefs.saveRememberMe(remember, username, password)
    }

    suspend fun getSavedCredentials(): Pair<String, String> {
        val username = prefs.savedUsername.first()
        val password = prefs.savedPassword.first()
        return Pair(username, password)
    }

    suspend fun isRememberMe(): Boolean {
        return prefs.rememberMe.first()
    }
}
