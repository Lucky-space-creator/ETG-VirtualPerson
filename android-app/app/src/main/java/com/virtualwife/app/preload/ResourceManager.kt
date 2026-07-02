package com.virtualwife.app.preload

import android.util.Log
import com.virtualwife.app.data.local.PreferencesManager
import com.virtualwife.app.data.remote.RetrofitClient
import com.virtualwife.app.data.remote.dto.AvatarConfigDto
import kotlinx.coroutines.*

private const val TAG = "ResourceManager"

/**
 * 资源预加载管理器
 * App启动时立即加载数字人配置、路线数据等，避免使用时才加载
 */
class ResourceManager(
    private val prefs: PreferencesManager
) {
    // 预加载的数据
    var defaultAvatar: AvatarConfigDto? = null
        private set
    var avatarLoaded = false
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * App启动时调用，立即开始预加载
     */
    fun preload() {
        scope.launch {
            preloadAvatar()
        }
    }

    /**
     * 预加载默认数字人配置
     */
    private suspend fun preloadAvatar() {
        try {
            Log.d(TAG, "Preloading avatar config...")
            // 使用adminApi（带认证）而不是djangoApi
            val api = RetrofitClient.initAdminApi(prefs)
            val res = api.getDefaultAvatar()
            if (res.isSuccessful && res.body()?.isSuccess == true) {
                defaultAvatar = res.body()!!.data
                avatarLoaded = true
                Log.d(TAG, "Avatar preloaded: ${defaultAvatar?.avatarName}")

                // 同时更新本地缓存
                defaultAvatar?.let { avatar ->
                    prefs.saveAvatarName(avatar.avatarName ?: "AI导游")
                    val vrmUrl = avatar.vrmDisplayUrl?.takeIf { it.isNotEmpty() }
                        ?: avatar.vrmModelUrl ?: ""
                    if (vrmUrl.isNotEmpty()) {
                        prefs.saveVrmModelUrl(vrmUrl)
                    }
                }
            } else {
                Log.w(TAG, "Avatar preload failed: ${res.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Avatar preload error: ${e.message}")
        }
    }

    /**
     * 获取预加载的头像URL（用于快速显示）
     */
    fun getAvatarDisplayUrl(): String? {
        return defaultAvatar?.vrmDisplayUrl
            ?: defaultAvatar?.vrmModelUrl
    }

    /**
     * 获取预加载的缩略图URL
     */
    fun getThumbnailUrl(): String? {
        return defaultAvatar?.thumbnailDisplayUrl
            ?: defaultAvatar?.thumbnailUrl
    }

    fun destroy() {
        scope.cancel()
    }
}
