package com.virtualwife.app.data.repository

import android.util.Log
import android.util.LruCache
import com.virtualwife.app.data.local.PreferencesManager
import com.virtualwife.app.data.remote.RetrofitClient
import com.virtualwife.app.data.remote.dto.TtsRequest
import com.virtualwife.app.data.remote.dto.VoiceItem
import kotlinx.coroutines.flow.first

private const val TAG = "VoiceRepository"

class VoiceRepository(private val prefs: PreferencesManager) {

    // TTS缓存，减少重复请求
    private val ttsCache = LruCache<String, String>(50)

    suspend fun generateTts(text: String, voiceId: String? = null): Result<String> {
        return try {
            val voice = voiceId ?: prefs.ttsVoice.first()

            // 检查缓存
            val cacheKey = "${voice}_${text}"
            ttsCache.get(cacheKey)?.let { cached ->
                Log.d(TAG, "TTS cache hit: ${cached.take(20)}...")
                return Result.success(cached)
            }

            Log.d(TAG, "TTS request: voice=$voice, text=${text.take(30)}...")
            // 使用adminApi（带认证）
            val response = RetrofitClient.adminApi.generateTts(
                TtsRequest(text = text, voiceId = voice)
            )
            Log.d(TAG, "TTS response: code=${response.code()}, success=${response.isSuccessful}")
            if (response.isSuccessful) {
                val body = response.body()
                // 解析嵌套格式: {code:200, data:{audio_base64:"...", status:"ok"}}
                val data = body?.data
                val audioBase64 = data?.audioBase64
                val audioUrl = data?.audioUrl

                when {
                    !audioBase64.isNullOrEmpty() -> {
                        Log.d(TAG, "TTS generated: base64 ${audioBase64.length} chars")
                        val result = "base64:$audioBase64"
                        ttsCache.put(cacheKey, result)
                        Result.success(result)
                    }
                    !audioUrl.isNullOrEmpty() -> {
                        Log.d(TAG, "TTS generated: url=$audioUrl")
                        ttsCache.put(cacheKey, audioUrl)
                        Result.success(audioUrl)
                    }
                    else -> {
                        Log.w(TAG, "TTS returned no audio data")
                        Result.failure(Exception("TTS返回无音频数据"))
                    }
                }
            } else {
                Log.e(TAG, "TTS failed: ${response.code()}")
                Result.failure(Exception("TTS生成失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "TTS error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getAvailableVoices(): Result<List<VoiceItem>> {
        return try {
            // 使用adminApi（带认证）
            val response = RetrofitClient.adminApi.getVoices()
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("获取语音列表失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveVoicePreference(voiceId: String) {
        prefs.saveTtsVoice(voiceId)
    }
}
