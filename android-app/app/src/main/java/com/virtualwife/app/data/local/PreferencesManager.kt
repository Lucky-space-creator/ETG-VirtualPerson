package com.virtualwife.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "virtualwife_prefs")

class PreferencesManager(private val context: Context) {

    companion object {
        private val KEY_TOKEN = stringPreferencesKey("jwt_token")
        private val KEY_USERNAME = stringPreferencesKey("username")
        private val KEY_USER_ID = longPreferencesKey("user_id")
        private val KEY_SESSION_ID = stringPreferencesKey("session_id")
        private val KEY_AVATAR_NAME = stringPreferencesKey("avatar_name")
        private val KEY_VRM_MODEL_URL = stringPreferencesKey("vrm_model_url")
        private val KEY_TTS_VOICE = stringPreferencesKey("tts_voice")
        private val KEY_LLM_PROVIDER = stringPreferencesKey("llm_provider")
        private val KEY_INTEREST_TAGS = stringSetPreferencesKey("interest_tags")
        private val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        // 记住我
        private val KEY_REMEMBER_ME = booleanPreferencesKey("remember_me")
        private val KEY_SAVED_USERNAME = stringPreferencesKey("saved_username")
        private val KEY_SAVED_PASSWORD = stringPreferencesKey("saved_password")
        // 用户头像（MinIO URL）
        private val KEY_USER_AVATAR_URL = stringPreferencesKey("user_avatar_url")
        private val KEY_USER_AVATAR_PATH = stringPreferencesKey("user_avatar_path")
    }

    val token: Flow<String> = context.dataStore.data.map { it[KEY_TOKEN] ?: "" }
    val username: Flow<String> = context.dataStore.data.map { it[KEY_USERNAME] ?: "" }
    val userId: Flow<Long> = context.dataStore.data.map { it[KEY_USER_ID] ?: 0L }
    val sessionId: Flow<String> = context.dataStore.data.map {
        it[KEY_SESSION_ID] ?: ""
    }

    suspend fun getOrCreateSessionId(): String {
        val existing = context.dataStore.data.first()[KEY_SESSION_ID]
        if (!existing.isNullOrEmpty()) return existing
        val newId = generateSessionId()
        saveSessionId(newId)
        return newId
    }
    val avatarName: Flow<String> = context.dataStore.data.map { it[KEY_AVATAR_NAME] ?: "小莉" }
    val vrmModelUrl: Flow<String> = context.dataStore.data.map { it[KEY_VRM_MODEL_URL] ?: "" }
    val ttsVoice: Flow<String> = context.dataStore.data.map {
        it[KEY_TTS_VOICE] ?: "zh-CN-XiaoyiNeural"
    }
    val llmProvider: Flow<String> = context.dataStore.data.map { it[KEY_LLM_PROVIDER] ?: "" }
    val interestTags: Flow<Set<String>> = context.dataStore.data.map {
        it[KEY_INTEREST_TAGS] ?: emptySet()
    }
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { it[KEY_IS_LOGGED_IN] ?: false }
    val rememberMe: Flow<Boolean> = context.dataStore.data.map { it[KEY_REMEMBER_ME] ?: false }
    val userAvatarUrl: Flow<String> = context.dataStore.data.map { it[KEY_USER_AVATAR_URL] ?: "" }
    val userAvatarPath: Flow<String> = context.dataStore.data.map { it[KEY_USER_AVATAR_PATH] ?: "" }
    val savedUsername: Flow<String> = context.dataStore.data.map { it[KEY_SAVED_USERNAME] ?: "" }
    val savedPassword: Flow<String> = context.dataStore.data.map { it[KEY_SAVED_PASSWORD] ?: "" }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { it[KEY_TOKEN] = token }
    }

    suspend fun saveUserInfo(username: String, userId: Long) {
        context.dataStore.edit {
            it[KEY_USERNAME] = username
            it[KEY_USER_ID] = userId
            it[KEY_IS_LOGGED_IN] = true
        }
    }

    suspend fun saveSessionId(sessionId: String) {
        context.dataStore.edit { it[KEY_SESSION_ID] = sessionId }
    }

    suspend fun saveAvatarName(name: String) {
        context.dataStore.edit { it[KEY_AVATAR_NAME] = name }
    }

    suspend fun saveVrmModelUrl(url: String) {
        context.dataStore.edit { it[KEY_VRM_MODEL_URL] = url }
    }

    suspend fun saveTtsVoice(voice: String) {
        context.dataStore.edit { it[KEY_TTS_VOICE] = voice }
    }

    suspend fun saveLlmProvider(provider: String) {
        context.dataStore.edit { it[KEY_LLM_PROVIDER] = provider }
    }

    suspend fun saveInterestTags(tags: Set<String>) {
        context.dataStore.edit { it[KEY_INTEREST_TAGS] = tags }
    }

    suspend fun saveRememberMe(remember: Boolean, username: String, password: String) {
        context.dataStore.edit {
            it[KEY_REMEMBER_ME] = remember
            if (remember) {
                it[KEY_SAVED_USERNAME] = username
                it[KEY_SAVED_PASSWORD] = password
            } else {
                it.remove(KEY_SAVED_USERNAME)
                it.remove(KEY_SAVED_PASSWORD)
            }
        }
    }

    suspend fun saveUserAvatar(url: String, path: String) {
        context.dataStore.edit {
            it[KEY_USER_AVATAR_URL] = url
            it[KEY_USER_AVATAR_PATH] = path
        }
    }

    suspend fun clearUserAvatar() {
        context.dataStore.edit {
            it.remove(KEY_USER_AVATAR_URL)
            it.remove(KEY_USER_AVATAR_PATH)
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }

    private fun generateSessionId(): String {
        val id = java.util.UUID.randomUUID().toString()
        return id
    }
}
