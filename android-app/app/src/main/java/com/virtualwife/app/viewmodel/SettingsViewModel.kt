package com.virtualwife.app.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.virtualwife.app.MainApplication
import com.virtualwife.app.data.remote.RetrofitClient
import com.virtualwife.app.data.remote.dto.AvatarConfigDto
import com.virtualwife.app.data.remote.dto.VoiceItem
import com.virtualwife.app.data.repository.VoiceRepository
import com.virtualwife.app.data.repository.VrmRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull

data class SettingsUiState(
    val avatarName: String = "小莉",
    val vrmModelUrl: String = "",
    val ttsVoice: String = "zh-CN-XiaoyiNeural",
    val llmProvider: String = "",
    val availableVoices: List<VoiceItem> = emptyList(),
    val availableAvatars: List<AvatarConfigDto> = emptyList(),
    val userAvatarUrl: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

private const val TAG = "SettingsViewModel"

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MainApplication
    private val vrmRepo = VrmRepository(app.preferencesManager)
    private val voiceRepo = VoiceRepository(app.preferencesManager)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                vrmRepo.avatarName,
                vrmRepo.vrmModelUrl,
                app.preferencesManager.ttsVoice,
                app.preferencesManager.llmProvider,
                app.preferencesManager.userAvatarUrl
            ) { name, modelUrl, voice, provider, userAvatar ->
                SettingsUiState(
                    avatarName = name,
                    vrmModelUrl = modelUrl,
                    ttsVoice = voice,
                    llmProvider = provider,
                    userAvatarUrl = userAvatar
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun loadVoices() {
        viewModelScope.launch {
            val result = voiceRepo.getAvailableVoices()
            result.fold(
                onSuccess = { voices ->
                    _uiState.update { it.copy(availableVoices = voices) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
            )
        }
    }

    fun setTtsVoice(voiceId: String) {
        viewModelScope.launch {
            voiceRepo.saveVoicePreference(voiceId)
            _uiState.update { it.copy(ttsVoice = voiceId) }
        }
    }

    fun setAvatarName(name: String) {
        viewModelScope.launch {
            vrmRepo.setAvatarName(name)
        }
    }

    fun setVrmModel(url: String) {
        viewModelScope.launch {
            vrmRepo.setVrmModel(url)
        }
    }

    fun setLlmProvider(provider: String) {
        viewModelScope.launch {
            app.preferencesManager.saveLlmProvider(provider)
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            try {
                // 删除本地所有聊天记录（用户+AI）
                app.chatDatabase.chatMessageDao().deleteAll()
                Log.d(TAG, "Chat history cleared: all user and AI messages deleted")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear chat history: ${e.message}")
                _uiState.update { it.copy(error = "清除失败: ${e.message}") }
            }
        }
    }

    fun loadAvatars() {
        viewModelScope.launch {
            try {
                // 使用adminApi（带认证）
                val res = RetrofitClient.adminApi.getAvatars(pageNum = 1, pageSize = 50, mobile = true)
                if (res.isSuccessful && res.body()?.isSuccess == true) {
                    val avatars = res.body()!!.data?.records ?: emptyList()
                    _uiState.update { it.copy(availableAvatars = avatars) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "加载数字人列表失败: ${e.message}") }
            }
        }
    }

    fun selectAvatar(avatar: AvatarConfigDto) {
        viewModelScope.launch {
            val name = avatar.avatarName ?: "AI导游"
            val vrmUrl = avatar.vrmDisplayUrl?.takeIf { it.isNotEmpty() }
                ?: avatar.vrmModelUrl ?: ""
            vrmRepo.setAvatarName(name)
            if (vrmUrl.isNotEmpty()) vrmRepo.setVrmModel(vrmUrl)
            _uiState.update { it.copy(avatarName = name, vrmModelUrl = vrmUrl) }
        }
    }

    fun uploadUserAvatar(imageBytes: ByteArray) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val mediaType = "image/jpeg".toMediaTypeOrNull()
                val fileBody = okhttp3.RequestBody.create(mediaType, imageBytes)
                val part = okhttp3.MultipartBody.Part.createFormData(
                    "file", "avatar.jpg", fileBody
                )
                // 使用adminApi（带认证）
                val res = RetrofitClient.adminApi.uploadUserAvatar(part)
                if (res.isSuccessful && res.body()?.isSuccess == true) {
                    val data = res.body()!!.data
                    val url = data?.get("displayUrl") ?: ""
                    val path = data?.get("path") ?: ""
                    app.preferencesManager.saveUserAvatar(url, path)
                    _uiState.update { it.copy(userAvatarUrl = url, isLoading = false) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "上传失败") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "上传失败: ${e.message}") }
            }
        }
    }

    fun clearUserAvatar() {
        viewModelScope.launch {
            app.preferencesManager.clearUserAvatar()
            _uiState.update { it.copy(userAvatarUrl = "") }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
