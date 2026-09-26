package com.example.model

enum class AiProviderMode(val label: String) {
    GEMINI("Google Gemini"),
    DEEPSEEK("DeepSeek"),
    AUTO("Auto Router")
}

enum class ProviderStatus(val label: String) {
    CONNECTED("Connected"),
    CONNECTING("Connecting"),
    DISCONNECTED("Disconnected"),
    ERROR("Error"),
    OFFLINE("Offline")
}

data class ProviderState(
    val mode: AiProviderMode = AiProviderMode.AUTO,
    val geminiStatus: ProviderStatus = ProviderStatus.DISCONNECTED,
    val deepSeekStatus: ProviderStatus = ProviderStatus.DISCONNECTED,
    val activeProviderName: String = "Gemini Live",
    val latencyMs: Long = 0,
    val geminiModel: String = "gemini-3.5-flash",
    val deepSeekModel: String = "deepseek-chat"
)
