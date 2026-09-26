package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MyraaApplication
import com.example.ai.AiRouter
import com.example.ai.AiServiceResult
import com.example.ai.DeepSeekService
import com.example.ai.GeminiService
import com.example.ai.ParsedToolCall
import com.example.data.ChatMessageEntity
import com.example.data.DiagnosticLogEntity
import com.example.data.MemoryCategory
import com.example.data.MemoryEntity
import com.example.model.AiProviderMode
import com.example.model.CharacterState
import com.example.model.ProviderState
import com.example.model.ProviderStatus
import com.example.scene3d.CharacterStateManager
import com.example.scene3d.GlbModelInfo
import com.example.scene3d.GlbParser
import com.example.service.MyraaForegroundService
import com.example.tools.AndroidToolExecutor
import com.example.tools.PendingConfirmationAction
import com.example.tools.ToolExecutionResult
import com.example.voice.VoiceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MyraaApplication
    private val db = app.database
    val keyManager = app.secureKeyManager

    private val geminiService = GeminiService()
    private val deepSeekService = DeepSeekService()
    private val aiRouter = AiRouter(keyManager, geminiService, deepSeekService)
    val toolExecutor = AndroidToolExecutor(application)
    val characterManager = CharacterStateManager()

    val voiceManager = VoiceManager(
        context = application,
        onInterruptionDetected = {
            characterManager.setState(CharacterState.LISTENING, "Interrupted • Listening...")
        }
    )

    // Room Database Flows
    val chatMessages: StateFlow<List<ChatMessageEntity>> = db.chatDao()
        .getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memories: StateFlow<List<MemoryEntity>> = db.memoryDao()
        .getAllMemories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val diagnosticLogs: StateFlow<List<DiagnosticLogEntity>> = db.diagnosticLogDao()
        .getRecentLogs(60)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI States
    private val _providerState = MutableStateFlow(ProviderState())
    val providerState: StateFlow<ProviderState> = _providerState.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<PendingConfirmationAction?>(null)
    val pendingConfirmation: StateFlow<PendingConfirmationAction?> = _pendingConfirmation.asStateFlow()

    private val _isScreenVisionActive = MutableStateFlow(false)
    val isScreenVisionActive: StateFlow<Boolean> = _isScreenVisionActive.asStateFlow()

    private val _isFirstLaunch = MutableStateFlow(false)
    val isFirstLaunch: StateFlow<Boolean> = _isFirstLaunch.asStateFlow()

    val isBackgroundServiceRunning: StateFlow<Boolean> = MyraaForegroundService.isRunning

    init {
        val prefs = app.getSharedPreferences("myraa_app_prefs", Application.MODE_PRIVATE)
        _isFirstLaunch.value = prefs.getBoolean("is_first_launch", true)

        updateProviderStatuses()

        // Sync voice audio amplitude to character 3D animation
        viewModelScope.launch {
            voiceManager.audioAmplitude.collect { amplitude ->
                characterManager.updateAudioAmplitude(amplitude)
            }
        }

        // Sync voice speaking state to character state
        viewModelScope.launch {
            voiceManager.isSpeaking.collect { speaking ->
                if (speaking && characterManager.characterState.value != CharacterState.SPEAKING) {
                    characterManager.setState(CharacterState.SPEAKING)
                } else if (!speaking && characterManager.characterState.value == CharacterState.SPEAKING) {
                    characterManager.setState(CharacterState.IDLE)
                }
            }
        }
    }

    fun completeFirstLaunch() {
        val prefs = app.getSharedPreferences("myraa_app_prefs", Application.MODE_PRIVATE)
        prefs.edit().putBoolean("is_first_launch", false).apply()
        _isFirstLaunch.value = false
    }

    fun updateProviderStatuses() {
        val geminiConfigured = keyManager.isGeminiConfigured()
        val deepSeekConfigured = keyManager.isDeepSeekConfigured()

        _providerState.value = _providerState.value.copy(
            geminiStatus = if (geminiConfigured) ProviderStatus.CONNECTED else ProviderStatus.DISCONNECTED,
            deepSeekStatus = if (deepSeekConfigured) ProviderStatus.CONNECTED else ProviderStatus.DISCONNECTED,
            activeProviderName = when (_providerState.value.mode) {
                AiProviderMode.GEMINI -> "Gemini Live"
                AiProviderMode.DEEPSEEK -> "DeepSeek"
                AiProviderMode.AUTO -> if (geminiConfigured) "Auto (Gemini Live)" else "Auto (DeepSeek)"
            }
        )
    }

    fun setAiMode(mode: AiProviderMode) {
        _providerState.value = _providerState.value.copy(mode = mode)
        updateProviderStatuses()
    }

    fun setGeminiModel(model: String) {
        _providerState.value = _providerState.value.copy(geminiModel = model)
    }

    fun setDeepSeekModel(model: String) {
        _providerState.value = _providerState.value.copy(deepSeekModel = model)
    }

    fun startVoiceInteraction() {
        characterManager.setState(CharacterState.LISTENING, "Listening to you... (बोलिए)")
        voiceManager.startListening(
            onResult = { userSpokenText ->
                processUserPrompt(userSpokenText, isVoice = true)
            },
            onPartial = { partialText ->
                characterManager.setState(CharacterState.LISTENING, "Hearing: \"$partialText\"")
            },
            onError = { errMsg ->
                characterManager.setState(CharacterState.ERROR, errMsg)
            }
        )
    }

    fun stopVoiceInteraction() {
        voiceManager.stopListening()
        voiceManager.stopSpeaking()
        characterManager.setState(CharacterState.IDLE)
    }

    fun cancelActiveResponse() {
        voiceManager.cancelAll()
        characterManager.setState(CharacterState.IDLE, "Response canceled")
    }

    fun processUserPrompt(prompt: String, isVoice: Boolean = false) {
        val trimmed = prompt.trim()
        if (trimmed.isBlank()) return

        characterManager.setState(CharacterState.THINKING, "MYRAA is processing...")

        viewModelScope.launch {
            // 1. Insert user message to chat DB
            val userMsg = ChatMessageEntity(
                role = "user",
                content = trimmed,
                provider = _providerState.value.mode.label,
                isVoice = isVoice
            )
            db.chatDao().insertMessage(userMsg)

            // 2. Check for offline native commands (e.g., Flashlight, Camera, Settings, Greetings)
            val localMatch = tryHandleLocalIntent(trimmed)
            if (localMatch != null) {
                val (toolCall, responseText) = localMatch
                if (toolCall != null) {
                    handleToolCall(toolCall, responseText, isVoice)
                } else {
                    val assistantMsg = ChatMessageEntity(
                        role = "myraa",
                        content = responseText,
                        provider = "MYRAA Native",
                        isVoice = isVoice
                    )
                    db.chatDao().insertMessage(assistantMsg)
                    if (isVoice) {
                        characterManager.setState(CharacterState.SPEAKING)
                        voiceManager.speak(responseText, onDone = {
                            characterManager.setState(CharacterState.IDLE)
                        })
                    } else {
                        characterManager.setState(CharacterState.IDLE, "Ready")
                    }
                }
                return@launch
            }

            // 3. Check if an AI provider API key is configured
            val geminiKeyConfigured = keyManager.isGeminiConfigured()
            val deepSeekKeyConfigured = keyManager.isDeepSeekConfigured()

            if (!geminiKeyConfigured && !deepSeekKeyConfigured) {
                val isHindi = trimmed.any { it in '\u0900'..'\u097F' }
                val guidance = if (isHindi) {
                    "नमस्ते! मुझे आपकी आवाज़ सुनाई दे रही है। बातचीत और AI सवालों के जवाब के लिए, कृपया Settings में जाकर अपनी Gemini या DeepSeek API Key जोड़ें। आप टॉर्च, कैमरा, यूट्यूब और सेटिंग्स जैसे कमांड्स अभी भी चला सकते हैं।"
                } else {
                    "Hello! I can hear you clearly. To enable full AI conversational intelligence, please configure your Gemini or DeepSeek API key in Settings. You can also use native commands like flashlight, camera, YouTube, and settings right now."
                }

                val assistantMsg = ChatMessageEntity(
                    role = "myraa",
                    content = guidance,
                    provider = "System",
                    isVoice = isVoice
                )
                db.chatDao().insertMessage(assistantMsg)

                if (isVoice) {
                    characterManager.setState(CharacterState.SPEAKING)
                    voiceManager.speak(guidance, onDone = {
                        characterManager.setState(CharacterState.IDLE, "Configure API key in Settings")
                    })
                } else {
                    characterManager.setState(CharacterState.IDLE, "Configure API key in Settings")
                }
                return@launch
            }

            // 4. Fetch active persistent memories
            val activeMemories = db.memoryDao().getActiveMemories()

            // 5. Dispatch to AI Router
            val result = aiRouter.routeRequest(
                userMessage = trimmed,
                mode = _providerState.value.mode,
                isVoiceRequest = isVoice,
                geminiModel = _providerState.value.geminiModel,
                deepSeekModel = _providerState.value.deepSeekModel,
                history = chatMessages.value,
                memories = activeMemories
            )

            when (result) {
                is AiServiceResult.Success -> {
                    _providerState.value = _providerState.value.copy(latencyMs = result.latencyMs)

                    // Log telemetry
                    db.diagnosticLogDao().insertLog(
                        DiagnosticLogEntity(
                            event = "AI_QUERY_SUCCESS",
                            provider = result.providerName,
                            status = "SUCCESS",
                            latencyMs = result.latencyMs
                        )
                    )

                    // Check if tool call requested
                    if (result.toolCall != null) {
                        handleToolCall(result.toolCall, result.text, isVoice)
                    } else {
                        // Natural text response
                        val assistantMsg = ChatMessageEntity(
                            role = "myraa",
                            content = result.text,
                            provider = result.providerName,
                            isVoice = isVoice
                        )
                        db.chatDao().insertMessage(assistantMsg)

                        if (isVoice) {
                            characterManager.setState(CharacterState.SPEAKING)
                            voiceManager.speak(result.text, onDone = {
                                characterManager.setState(CharacterState.IDLE)
                            })
                        } else {
                            characterManager.setState(CharacterState.IDLE, "Ready")
                        }
                    }
                }
                is AiServiceResult.Error -> {
                    characterManager.setState(CharacterState.ERROR, result.message)

                    val errorMsg = ChatMessageEntity(
                        role = "myraa",
                        content = "⚠️ ${result.message}",
                        provider = _providerState.value.mode.label,
                        isVoice = isVoice
                    )
                    db.chatDao().insertMessage(errorMsg)

                    db.diagnosticLogDao().insertLog(
                        DiagnosticLogEntity(
                            event = "AI_QUERY_ERROR",
                            provider = _providerState.value.activeProviderName,
                            status = "ERROR",
                            errorType = result.message,
                            latencyMs = result.latencyMs
                        )
                    )

                    if (isVoice) {
                        val isHindi = trimmed.any { it in '\u0900'..'\u097F' }
                        val spokenErr = if (isHindi) {
                            "माफ़ कीजिए, AI सर्वर से जवाब नहीं मिला: ${result.message}। कृपया सेटिंग्स में API Key जांचें।"
                        } else {
                            "Sorry, I encountered an issue: ${result.message}. Please check your API key in Settings."
                        }
                        voiceManager.speak(spokenErr)
                    }
                }
            }
        }
    }

    private fun tryHandleLocalIntent(prompt: String): Pair<ParsedToolCall?, String>? {
        val lower = prompt.lowercase().trim()

        // Flashlight / टॉर्च
        if (lower.contains("torch") || lower.contains("flashlight") || lower.contains("टॉर्च") || lower.contains("फ्लैशलाइट")) {
            val enable = !lower.contains("off") && !lower.contains("बंद")
            return Pair(
                ParsedToolCall("device_flashlight", mapOf("enable" to enable.toString())),
                if (enable) "Turning on the flashlight." else "Turning off the flashlight."
            )
        }

        // Camera / कैमरा
        if (lower.contains("camera") || lower.contains("कैमरा") || lower.contains("photo") || lower.contains("फोटो")) {
            return Pair(
                ParsedToolCall("apps_camera", emptyMap()),
                "Opening the camera."
            )
        }

        // YouTube / यूट्यूब
        if (lower.contains("youtube") || lower.contains("यूट्यूब")) {
            val query = lower.replace("open youtube", "").replace("search youtube for", "").replace("यूट्यूब खोलो", "").trim()
            return Pair(
                ParsedToolCall("youtube_search", if (query.isNotBlank()) mapOf("query" to query) else emptyMap()),
                if (query.isNotBlank()) "Searching YouTube for $query." else "Opening YouTube."
            )
        }

        // Wi-Fi / वाई-फाई
        if (lower.contains("wifi") || lower.contains("wi-fi") || lower.contains("वाईफाई") || lower.contains("वाई-फाई")) {
            return Pair(
                ParsedToolCall("settings_wifi", emptyMap()),
                "Opening Wi-Fi settings."
            )
        }

        // Bluetooth / ब्लूटूथ
        if (lower.contains("bluetooth") || lower.contains("ब्लूटूथ")) {
            return Pair(
                ParsedToolCall("settings_bluetooth", emptyMap()),
                "Opening Bluetooth settings."
            )
        }

        // Settings / सेटिंग्स
        if (lower.contains("setting") || lower.contains("सेटिंग")) {
            return Pair(
                ParsedToolCall("settings_system", emptyMap()),
                "Opening system settings."
            )
        }

        // Battery / बैटरी
        if (lower.contains("battery") || lower.contains("बैटरी")) {
            return Pair(
                ParsedToolCall("settings_battery", emptyMap()),
                "Opening battery settings."
            )
        }

        // Greetings / नमस्ते
        if (lower in listOf("hello", "hi", "hey", "नमस्ते", "नमस्कार", "हैलो", "हेलो", "how are you", "तुम कौन हो", "who are you", "आप कौन हैं")) {
            val responseText = if (lower.contains("नमस") || lower.contains("हैल") || lower.contains("कौन") || lower.contains("आप")) {
                "नमस्ते! मैं MYRAA हूँ, आपकी निजी AI सहायक। मैं आपकी आवाज़ सुन सकती हूँ। कहिए, मैं आपकी क्या सहायता करूँ?"
            } else {
                "Hello! I am MYRAA, your personal AI assistant. I can hear you loud and clear. How can I help you today?"
            }
            return Pair(null, responseText)
        }

        return null
    }

    private suspend fun handleToolCall(toolCall: ParsedToolCall, companionText: String, isVoice: Boolean) {
        characterManager.setState(CharacterState.PROCESSING, "Executing ${toolCall.toolId}...", tool = toolCall.toolId)

        val executionResult: ToolExecutionResult = toolExecutor.execute(toolCall.toolId, toolCall.parameters, isConfirmed = false)

        if (executionResult.requiresConfirmation && executionResult.pendingConfirmationData != null) {
            _pendingConfirmation.value = executionResult.pendingConfirmationData
            characterManager.setState(CharacterState.LISTENING, "Confirmation required")

            val confirmationMsg = ChatMessageEntity(
                role = "myraa",
                content = "$companionText\n\n[Action Requires Confirmation]: ${executionResult.pendingConfirmationData.prompt}",
                toolName = toolCall.toolId,
                toolStatus = "pending",
                isVoice = isVoice
            )
            db.chatDao().insertMessage(confirmationMsg)
            return
        }

        finalizeToolResult(executionResult, companionText, isVoice)
    }

    fun confirmPendingAction() {
        val action = _pendingConfirmation.value ?: return
        _pendingConfirmation.value = null
        characterManager.setState(CharacterState.PROCESSING, "Executing ${action.toolId}...")

        viewModelScope.launch {
            val result = toolExecutor.execute(action.toolId, action.parameters, isConfirmed = true)
            finalizeToolResult(result, "Action confirmed and executed.", isVoice = false)
        }
    }

    fun cancelPendingAction() {
        _pendingConfirmation.value = null
        characterManager.setState(CharacterState.IDLE, "Action cancelled")
        viewModelScope.launch {
            db.chatDao().insertMessage(
                ChatMessageEntity(
                    role = "myraa",
                    content = "Action cancelled as requested.",
                    provider = "System"
                )
            )
        }
    }

    private suspend fun finalizeToolResult(result: ToolExecutionResult, companionText: String, isVoice: Boolean) {
        val combinedResponse = if (result.success) {
            characterManager.setState(CharacterState.SUCCESS, result.message)
            "$companionText\n\n✓ ${result.message}"
        } else {
            characterManager.setState(CharacterState.ERROR, result.message)
            "$companionText\n\n⚠️ ${result.message}"
        }

        db.chatDao().insertMessage(
            ChatMessageEntity(
                role = "myraa",
                content = combinedResponse,
                provider = _providerState.value.activeProviderName,
                toolName = result.toolId,
                toolStatus = if (result.success) "success" else "error",
                isVoice = isVoice
            )
        )

        db.diagnosticLogDao().insertLog(
            DiagnosticLogEntity(
                event = "TOOL_EXECUTION",
                provider = _providerState.value.activeProviderName,
                tool = result.toolId,
                status = if (result.success) "SUCCESS" else "ERROR",
                sanitizedDetails = result.message
            )
        )

        if (isVoice) {
            voiceManager.speak(result.message)
        }
    }

    fun loadCustomGlbModel(uri: Uri) {
        viewModelScope.launch {
            try {
                characterManager.setState(CharacterState.PROCESSING, "Importing 3D Character...")
                val savedFile = GlbParser.copyGlbToInternalStorage(app, uri)
                savedFile.inputStream().use { stream ->
                    val info = GlbParser.parseGlbHeader(stream, savedFile.name, savedFile.length())
                    characterManager.setModelInfo(info)
                    characterManager.setState(CharacterState.SUCCESS, "Character loaded: ${info.animationNames.size} animations detected")

                    db.diagnosticLogDao().insertLog(
                        DiagnosticLogEntity(
                            event = "3D_MODEL_LOADED",
                            provider = "OpenGL/Filament",
                            status = "SUCCESS",
                            sanitizedDetails = "Model: ${savedFile.name}, Animations: ${info.animationNames.joinToString()}"
                        )
                    )
                }
            } catch (e: Exception) {
                characterManager.setState(CharacterState.ERROR, "Failed to load model: ${e.message}")
            }
        }
    }

    fun toggleScreenVision() {
        _isScreenVisionActive.value = !_isScreenVisionActive.value
        if (_isScreenVisionActive.value) {
            characterManager.setState(CharacterState.LISTENING, "Screen Vision Active • Ready to inspect screen")
        } else {
            characterManager.setState(CharacterState.IDLE, "Screen Vision Stopped")
        }
    }

    fun toggleBackgroundMode() {
        if (isBackgroundServiceRunning.value) {
            MyraaForegroundService.stop(app)
        } else {
            MyraaForegroundService.start(app)
        }
    }

    fun toggleBackgroundService() = toggleBackgroundMode()

    fun addMemory(category: MemoryCategory, key: String, value: String) {
        if (key.isBlank() || value.isBlank()) return
        viewModelScope.launch {
            db.memoryDao().insertMemory(
                MemoryEntity(
                    category = category.name,
                    key = key.trim(),
                    value = value.trim()
                )
            )
        }
    }

    fun toggleMemory(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            db.memoryDao().setEnabled(id, enabled)
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            db.memoryDao().deleteMemory(id)
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            db.memoryDao().clearAllMemories()
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            db.chatDao().clearAllMessages()
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            db.diagnosticLogDao().clearLogs()
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
    }
}
