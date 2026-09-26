package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diagnostic_logs")
data class DiagnosticLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val event: String,
    val provider: String,
    val tool: String? = null,
    val status: String, // "SUCCESS", "ERROR", "INFO", "WARN"
    val latencyMs: Long? = null,
    val errorType: String? = null,
    val sanitizedDetails: String? = null
)
