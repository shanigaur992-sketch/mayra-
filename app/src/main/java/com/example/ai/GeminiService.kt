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

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    // Priority model fallback list
    private val supportedCandidateModels = listOf(
        "gemini-3.5-flash",
        "gemini-flash-latest",
        "gemini-3.1-flash-lite-preview",
        "gemini-3.1-pro-preview"
    )

    suspend fun generateResponse(
        apiKey: String,
        userMessage: String,
        model: String = "gemini-3.5-flash",
        history: List<ChatMessageEntity> = emptyList(),
        memories: List<MemoryEntity> = emptyList()
    ): AiServiceResult = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank() || cleanKey == "MY_GEMINI_API_KEY") {
            return@withContext AiServiceResult.Error(
                "Gemini API Key नहीं मिली। कृपया Settings में अपनी Gemini API Key दर्ज करें। (Gemini API key is missing. Please configure it in Settings.)"
            )
        }

        val startTime = System.currentTimeMillis()
        val modelsToTry = mutableListOf<String>().apply {
            add(model)
            supportedCandidateModels.forEach { m ->
                if (!contains(m)) add(m)
            }
        }

        var lastErrorMsg = "Unknown failure"

        for (activeModel in modelsToTry) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$activeModel:generateContent?key=$cleanKey"
                val systemPrompt = SystemPrompts.buildSystemPrompt(memories)

                val rootJson = JSONObject().apply {
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", systemPrompt) })
                        })
                    })

                    val contentsArray = JSONArray()

                    // Sanitize multi-turn history to strictly alternate: user, model, user, model...
                    val sanitizedTurns = buildSanitizedHistory(history)
                    for (turn in sanitizedTurns) {
                        contentsArray.put(JSONObject().apply {
                            put("role", turn.first)
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply { put("text", turn.second) })
                            })
                        })
                    }

                    // Add current user turn
                    contentsArray.put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", userMessage) })
                        })
                    })

                    put("contents", contentsArray)

                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.7)
                        put("topP", 0.95)
                        put("maxOutputTokens", 2048)
                    })
                }

                val requestBody = rootJson.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""
                    val latency = System.currentTimeMillis() - startTime

                    if (!response.isSuccessful) {
                        val parsedError = parseApiError(response.code, responseBody)
                        lastErrorMsg = parsedError

                        // If 404 (model not found), automatically retry with next candidate model
                        if (response.code == 404 || responseBody.contains("not found", ignoreCase = true)) {
                            return@use // continue to next candidate model in loop
                        }

                        // For auth / quota / key errors, no need to retry other models as key is invalid or quota exhausted
                        if (response.code == 400 || response.code == 403 || response.code == 429) {
                            return@withContext AiServiceResult.Error(parsedError, latency)
                        }

                        return@use
                    }

                    val jsonResponse = JSONObject(responseBody)
                    val candidates = jsonResponse.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val contentObj = firstCandidate?.optJSONObject("content")
                    val parts = contentObj?.optJSONArray("parts")
                    val text = parts?.optJSONObject(0)?.optString("text") ?: ""

                    if (text.isBlank()) {
                        lastErrorMsg = "Gemini returned empty response"
                        return@use
                    }

                    val toolCall = extractToolCall(text)
                    val cleanText = stripToolCallJson(text)

                    return@withContext AiServiceResult.Success(
                        text = cleanText,
                        toolCall = toolCall,
                        latencyMs = latency,
                        providerName = "Gemini ($activeModel)"
                    )
                }
            } catch (e: Exception) {
                lastErrorMsg = e.localizedMessage ?: "Network connection error"
            }
        }

        val latency = System.currentTimeMillis() - startTime
        AiServiceResult.Error("Gemini error: $lastErrorMsg", latency)
    }

    suspend fun testConnection(apiKey: String, model: String = "gemini-3.5-flash"): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {
            val cleanKey = apiKey.trim()
            if (cleanKey.isBlank() || cleanKey == "MY_GEMINI_API_KEY") {
                return@withContext Pair(false, "API key is empty. Please enter a valid Gemini API key.")
            }

            val testModels = listOf(model, "gemini-3.5-flash", "gemini-flash-latest")
            var lastErr = ""

            for (m in testModels) {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$m:generateContent?key=$cleanKey"
                val payload = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply { put("text", "ping") })
                            })
                        })
                    })
                }

                try {
                    val req = Request.Builder()
                        .url(url)
                        .post(payload.toString().toRequestBody("application/json".toMediaType()))
                        .build()

                    client.newCall(req).execute().use { res ->
                        if (res.isSuccessful) {
                            return@withContext Pair(true, "Connected successfully to $m (200 OK)")
                        }
                        val body = res.body?.string() ?: ""
                        lastErr = parseApiError(res.code, body)
                        if (res.code == 404) {
                            // Retry next model
                            return@use
                        }
                        return@withContext Pair(false, lastErr)
                    }
                } catch (e: Exception) {
                    lastErr = "Connection failed: ${e.message}"
                }
            }

            Pair(false, lastErr)
        }

    /**
     * Sanitizes chat history so it strictly alternates:
     * user -> model -> user -> model
     * This avoids Gemini's HTTP 400 "multiturn talk must alternate" error.
     */
    private fun buildSanitizedHistory(history: List<ChatMessageEntity>): List<Pair<String, String>> {
        val sanitized = mutableListOf<Pair<String, String>>()
        val recent = history.takeLast(10)

        for (msg in recent) {
            val role = if (msg.role == "user") "user" else "model"
            val text = msg.content.trim()
            if (text.isBlank()) continue

            if (sanitized.isNotEmpty() && sanitized.last().first == role) {
                // Combine consecutive same-role messages
                val lastTurn = sanitized.removeAt(sanitized.size - 1)
                sanitized.add(role to "${lastTurn.second}\n$text")
            } else {
                sanitized.add(role to text)
            }
        }

        // Must start with "user"
        while (sanitized.isNotEmpty() && sanitized.first().first != "user") {
            sanitized.removeAt(0)
        }

        // Must end with "model" so the upcoming current message ("user") alternates cleanly
        if (sanitized.isNotEmpty() && sanitized.last().first == "user") {
            sanitized.removeAt(sanitized.size - 1)
        }

        return sanitized
    }

    private fun parseApiError(code: Int, responseBody: String): String {
        return try {
            val json = JSONObject(responseBody)
            val errObj = json.optJSONObject("error")
            val message = errObj?.optString("message") ?: "HTTP $code"

            when {
                message.contains("API key not valid", ignoreCase = true) || message.contains("API_KEY_INVALID", ignoreCase = true) ->
                    "API Key अमान्य है। कृपया Settings में सही Gemini API Key दर्ज करें। (Invalid Gemini API Key)"
                message.contains("Resource has been exhausted", ignoreCase = true) || code == 429 ->
                    "API कोटा समाप्त हो गया है (Quota Exceeded)। कृपया कुछ देर बाद प्रयास करें या नई Key का उपयोग करें।"
                message.contains("multiturn talk", ignoreCase = true) ->
                    "Chat format issue resolved. Please try asking again."
                message.contains("User location is not supported", ignoreCase = true) ->
                    "User region not supported by this Gemini endpoint."
                code == 404 ->
                    "Model not found ($code). Trying alternate preview model..."
                else -> message
            }
        } catch (_: Exception) {
            "HTTP $code: $responseBody"
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
            val params = mutableMapOf<String, String>()
            paramsObj?.keys()?.forEach { key ->
                params[key] = paramsObj.optString(key)
            }
            if (toolId.isNotBlank()) ParsedToolCall(toolId, params) else null
        } catch (_: Exception) {
            null
        }
    }

    private fun stripToolCallJson(rawText: String): String {
        val jsonPattern = Regex("```(?:json)?\\s*\\{[\\s\\S]*?\"tool\"[\\s\\S]*?\\}\\s*```")
        return rawText.replace(jsonPattern, "").trim()
    }
}
