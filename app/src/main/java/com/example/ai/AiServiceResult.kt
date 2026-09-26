package com.example.ai

data class ParsedToolCall(
    val toolId: String,
    val parameters: Map<String, String>
)

sealed class AiServiceResult {
    data class Success(
        val text: String,
        val toolCall: ParsedToolCall? = null,
        val latencyMs: Long = 0,
        val providerName: String = "",
        val reasoning: String? = null
    ) : AiServiceResult()

    data class Error(
        val message: String,
        val latencyMs: Long = 0
    ) : AiServiceResult()
}
