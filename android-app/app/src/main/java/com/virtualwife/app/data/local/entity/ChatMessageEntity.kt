package com.virtualwife.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_message")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String,
    val messageType: String,
    val content: String,
    val emotion: String = "neutral",
    val avatarName: String = "",
    val ttsAudioUrl: String? = null,
    val ttsBase64: String? = null,
    val messageId: String = "",
    val sourcesJson: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
