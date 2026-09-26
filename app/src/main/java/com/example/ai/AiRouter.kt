package com.example.ai

import com.example.data.ChatMessageEntity
import com.example.data.MemoryEntity
import com.example.model.AiProviderMode
import com.example.security.SecureKeyManager

class AiRouter(
    private val keyManager: SecureKeyManager,
    private val geminiService: GeminiService = GeminiService(),
    private val deepSeekService: DeepSeekService = DeepSeekService()
) {

    suspend fun routeRequest(
        userMessage: String,
        mode: AiProviderMode,
        isVoiceRequest: Boolean,
        geminiModel: String = "gemini-3.5-flash",
        deepSeekModel: String = "deepseek-chat",
        history: List<ChatMessageEntity> = emptyList(),
        memories: List<MemoryEntity> = emptyList()
    ): AiServiceResult {
        val geminiKey = keyManager.getGeminiKey()
        val deepSeekKey = keyManager.getDeepSeekKey()

        val targetProvider = when (mode) {
            AiProviderMode.GEMINI -> AiProviderMode.GEMINI
            AiProviderMode.DEEPSEEK -> AiProviderMode.DEEPSEEK
            AiProviderMode.AUTO -> {
                // In Auto mode: Voice requests go to Gemini Live/Realtime
                if (isVoiceRequest) {
                    AiProviderMode.GEMINI
                } else if (isReasoningOrCodingQuery(userMessage) && deepSeekKey.isNotBlank()) {
                    // DeepSeek handles heavy reasoning or coding when configured
                    AiProviderMode.DEEPSEEK
                } else if (geminiKey.isNotBlank()) {
                    AiProviderMode.GEMINI
                } else if (deepSeekKey.isNotBlank()) {
                    AiProviderMode.DEEPSEEK
                } else {
                    AiProviderMode.GEMINI
                }
            }
        }

        return when (targetProvider) {
            AiProviderMode.GEMINI -> {
                val result = geminiService.generateResponse(
                    apiKey = geminiKey,
                    userMessage = userMessage,
                    model = geminiModel,
                    history = history,
                    memories = memories
                )
                // Fallback to DeepSeek if Auto mode and Gemini failed with network/quota error
                if (result is AiServiceResult.Error && mode == AiProviderMode.AUTO && deepSeekKey.isNotBlank()) {
                    deepSeekService.generateResponse(
                        apiKey = deepSeekKey,
                        userMessage = userMessage,
                        model = deepSeekModel,
                        history = history,
                        memories = memories
                    )
                } else {
                    result
                }
            }
            AiProviderMode.DEEPSEEK -> {
                val result = deepSeekService.generateResponse(
                    apiKey = deepSeekKey,
                    userMessage = userMessage,
                    model = deepSeekModel,
                    history = history,
                    memories = memories
                )
                // Fallback to Gemini if Auto mode and DeepSeek failed
                if (result is AiServiceResult.Error && mode == AiProviderMode.AUTO && geminiKey.isNotBlank()) {
                    geminiService.generateResponse(
                        apiKey = geminiKey,
                        userMessage = userMessage,
                        model = geminiModel,
                        history = history,
                        memories = memories
                    )
                } else {
                    result
                }
            }
            AiProviderMode.AUTO -> AiServiceResult.Error("Routing error")
        }
    }

    private fun isReasoningOrCodingQuery(query: String): Boolean {
        val lower = query.lowercase()
        val keywords = listOf(
            "code", "algorithm", "function", "debug", "solve", "math",
            "proof", "why does", "explain step by step", "analyze", "complexity",
            "python", "kotlin", "java", "sql", "logic", "reason"
        )
        return keywords.any { lower.contains(it) }
    }
}
