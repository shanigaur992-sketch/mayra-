package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val role: String, // "user", "myraa", "tool", "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val provider: String = "Gemini",
    val toolName: String? = null,
    val toolStatus: String? = null, // "success", "error", "pending"
    val isVoice: Boolean = false
)
