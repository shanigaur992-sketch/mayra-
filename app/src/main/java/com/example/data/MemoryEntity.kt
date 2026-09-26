package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MemoryCategory(val displayName: String) {
    PREFERENCE("Preference"),
    FACT("User Fact"),
    ROUTINE("Routine"),
    ASSISTANT("Assistant Behavior"),
    NOTE("Important Note")
}

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String = MemoryCategory.PREFERENCE.name,
    val key: String,
    val value: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isEnabled: Boolean = true
)
