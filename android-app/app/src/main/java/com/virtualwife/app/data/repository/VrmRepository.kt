package com.virtualwife.app.data.repository

import com.virtualwife.app.data.local.PreferencesManager
import com.virtualwife.app.data.remote.dto.AvatarConfigDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class VrmRepository(private val prefs: PreferencesManager) {

    val vrmModelUrl: Flow<String> = prefs.vrmModelUrl
    val avatarName: Flow<String> = prefs.avatarName

    suspend fun setVrmModel(url: String) {
        prefs.saveVrmModelUrl(url)
    }

    suspend fun setAvatarName(name: String) {
        prefs.saveAvatarName(name)
    }

    suspend fun getCurrentModelUrl(): String {
        return prefs.vrmModelUrl.first()
    }

    suspend fun getCurrentAvatarName(): String {
        return prefs.avatarName.first()
    }

    fun mapEmotionToExpression(emotion: String): String {
        return when (emotion.lowercase()) {
            "happiness", "happy" -> "happy"
            "sadness", "sad" -> "sad"
            "anger", "angry" -> "angry"
            "surprise" -> "surprised"
            "encouragement", "relaxed" -> "relaxed"
            "greeting" -> "happy"
            "thinking" -> "relaxed"
            "explaining" -> "happy"
            else -> "neutral"
        }
    }

    fun mapEmotionToAction(emotion: String): String? {
        return when (emotion.lowercase()) {
            "happiness", "happy" -> "nod"
            "sadness", "sad" -> "bow"
            "greeting" -> "wave"
            "encouragement" -> "wave"
            else -> null
        }
    }
}
