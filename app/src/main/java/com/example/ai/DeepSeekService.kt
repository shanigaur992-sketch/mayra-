package com.example.ai

import com.example.data.ChatMessageEntity
import com.example.data.MemoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class DeepSeekService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateResponse(
        apiKey: String,
        userMessage: String,
        model: String = "deepseek-chat",
        history: List<ChatMessageEntity> = emptyList(),
        memories: List<MemoryEntity> = emptyList()
    ): AiServiceResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext AiServiceResult.Error("DeepSeek API key is not configured.")
        }

        val startTime = System.currentTimeMillis()
        try {
            val url = "https://api.deepseek.com/chat/completions"
            val systemPrompt = SystemPrompts.buildSystemPrompt(memories)

            val messagesArray = JSONArray()
            messagesArray.put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })

            val contextMessages = history.takeLast(8)
            for (msg in contextMessages) {
                val role = if (msg.role == "user") "user" else "assistant"
                messagesArray.put(JSONObject().apply {
                    put("role", role)
                    put("content", msg.content)
                })
            }

            messagesArray.put(JSONObject().apply {
                put("role", "user")
                put("content", userMessage)
            })

            val rootJson = JSONObject().apply {
                put("model", model)
                put("messages", messagesArray)
                put("temperature", if (model.contains("reasoner")) 0.0 else 0.7)
            }

            val requestBody = rootJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                val latency = System.currentTimeMillis() - startTime

                if (!response.isSuccessful) {
                    val errorMsg = try {
                        val errObj = JSONObject(responseBody)
                        errObj.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                    } catch (_: Exception) {
                        "HTTP ${response.code}: $responseBody"
                    }
                    return@withContext AiServiceResult.Error("DeepSeek error ($errorMsg)", latency)
                }

                val jsonResponse = JSONObject(responseBody)
                val choices = jsonResponse.optJSONArray("choices")
                val firstChoice = choices?.optJSONObject(0)
                val messageObj = firstChoice?.optJSONObject("message")
                val text = messageObj?.optString("content") ?: ""
                val reasoning = messageObj?.optString("reasoning_content")

                if (text.isBlank()) {
                    return@withContext AiServiceResult.Error("Empty response from DeepSeek", latency)
                }

                val toolCall = extractToolCall(text)
                val cleanText = stripToolCallJson(text)

                AiServiceResult.Success(
                    text = cleanText,
                    toolCall = toolCall,
                    latencyMs = latency,
                    providerName = "DeepSeek ($model)",
                    reasoning = reasoning
                )
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            AiServiceResult.Error("Connection error: ${e.localizedMessage ?: "Unknown failure"}", latency)
        }
    }

    suspend fun testConnection(apiKey: String, model: String = "deepseek-chat"): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) return@withContext Pair(false, "API key is empty")
            val url = "https://api.deepseek.com/chat/completions"
            val payload = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", "ping")
                    })
                })
            }
            try {
                val req = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .post(payload.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(req).execute().use { res ->
                    if (res.isSuccessful) {
                        Pair(true, "DeepSeek connected successfully (${res.code} OK)")
                    } else {
                        val body = res.body?.string() ?: ""
                        Pair(false, "HTTP ${res.code}: $body")
                    }
                }
            } catch (e: Exception) {
                Pair(false, "DeepSeek connection error: ${e.message}")
            }
        }

    private fun extractToolCall(rawText: String): ParsedToolCall? {
        val jsonPattern = Regex("```(?:json)?\\s*\\{([\\s\\S]*?\"tool\"[\\s\\S]*?)\\}\\s*```")
        val match = jsonPattern.find(rawText) ?: return null
        return try {
            val fullBlock = match.value.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val obj = JSONObject(fullBlock)
            val toolId = obj.optString("tool")
            val paramsObj = obj.optJSONObject("parameters")
            val paramsMap = mutableMapOf<String, String>()
            if (paramsObj != null) {
                val keys = paramsObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    paramsMap[key] = paramsObj.optString(key)
                }
            }
            if (toolId.isNotBlank()) ParsedToolCall(toolId, paramsMap) else null
        } catch (_: Exception) {
            null
        }
    }

    private fun stripToolCallJson(rawText: String): String {
        val jsonPattern = Regex("```(?:json)?\\s*\\{[\\s\\S]*?\"tool\"[\\s\\S]*?\\}\\s*```")
        val cleaned = jsonPattern.replace(rawText, "").trim()
        return if (cleaned.isBlank()) "Executing requested command..." else cleaned
    }
}
