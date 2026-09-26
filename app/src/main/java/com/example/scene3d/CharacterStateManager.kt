package com.example.scene3d

import com.example.model.CharacterState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CharacterStateManager {

    private val _characterState = MutableStateFlow(CharacterState.IDLE)
    val characterState: StateFlow<CharacterState> = _characterState.asStateFlow()

    private val _statusText = MutableStateFlow("Ready for instructions")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _activeToolName = MutableStateFlow<String?>(null)
    val activeToolName: StateFlow<String?> = _activeToolName.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private val _modelInfo = MutableStateFlow<GlbModelInfo?>(null)
    val modelInfo: StateFlow<GlbModelInfo?> = _modelInfo.asStateFlow()

    private val _modelScale = MutableStateFlow(1.0f)
    val modelScale: StateFlow<Float> = _modelScale.asStateFlow()

    private val _modelRotationY = MutableStateFlow(0.0f)
    val modelRotationY: StateFlow<Float> = _modelRotationY.asStateFlow()

    fun setState(state: CharacterState, statusMessage: String? = null, tool: String? = null) {
        _characterState.value = state
        _activeToolName.value = tool
        _statusText.value = statusMessage ?: when (state) {
            CharacterState.IDLE -> "MYRAA is listening and ready"
            CharacterState.LISTENING -> "Listening to your voice..."
            CharacterState.THINKING -> "Analyzing and processing..."
            CharacterState.SPEAKING -> "Responding..."
            CharacterState.PROCESSING -> tool?.let { "Executing $it..." } ?: "Executing action..."
            CharacterState.SUCCESS -> "Action completed"
            CharacterState.ERROR -> "Encountered an issue"
            CharacterState.OFFLINE -> "MYRAA is offline"
        }
    }

    fun updateAudioAmplitude(amplitude: Float) {
        _audioAmplitude.value = amplitude.coerceIn(0f, 1f)
    }

    fun setModelInfo(info: GlbModelInfo) {
        _modelInfo.value = info
    }

    fun setModelScale(scale: Float) {
        _modelScale.value = scale.coerceIn(0.5f, 2.5f)
    }

    fun rotateModel(deltaDegrees: Float) {
        _modelRotationY.value = (_modelRotationY.value + deltaDegrees) % 360f
    }

    fun resetRotation() {
        _modelRotationY.value = 0f
    }

    fun getActiveAnimationName(): String {
        val detected = _modelInfo.value?.animationNames ?: emptyList()
        val state = _characterState.value

        return when (state) {
            CharacterState.IDLE -> findMatchingAnimation(detected, listOf("idle", "stand", "breathe", "neutral")) ?: "Procedural Idle"
            CharacterState.LISTENING -> findMatchingAnimation(detected, listOf("listen", "attentive", "look", "alert")) ?: "Procedural Listening"
            CharacterState.THINKING -> findMatchingAnimation(detected, listOf("think", "ponder", "process", "float")) ?: "Procedural Thinking"
            CharacterState.SPEAKING -> findMatchingAnimation(detected, listOf("talk", "speak", "mouth", "dialogue")) ?: "Procedural Viseme Sync"
            CharacterState.PROCESSING -> findMatchingAnimation(detected, listOf("action", "interact", "cast", "work")) ?: "Procedural Processing"
            CharacterState.SUCCESS -> findMatchingAnimation(detected, listOf("happy", "nod", "success", "wave", "cheer")) ?: "Procedural Success"
            CharacterState.ERROR -> findMatchingAnimation(detected, listOf("shake", "no", "sad", "glitch", "error")) ?: "Procedural Error"
            CharacterState.OFFLINE -> findMatchingAnimation(detected, listOf("sleep", "rest", "off", "deactivate")) ?: "Procedural Offline"
        }
    }

    private fun findMatchingAnimation(detected: List<String>, keywords: List<String>): String? {
        for (anim in detected) {
            val lower = anim.lowercase()
            if (keywords.any { lower.contains(it) }) {
                return anim
            }
        }
        return detected.firstOrNull()
    }
}
