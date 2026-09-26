package com.example.model

enum class CharacterState(val description: String) {
    IDLE("Subtle breathing, natural presence"),
    LISTENING("Attentive, voice reactive waveform active"),
    THINKING("Neural holographic processing state"),
    SPEAKING("Jaw movement & voice-reactive pulses"),
    PROCESSING("Executing tool action"),
    SUCCESS("Action completed successfully"),
    ERROR("Issue encountered"),
    OFFLINE("System disconnected / standby")
}

data class CharacterStateInfo(
    val state: CharacterState = CharacterState.IDLE,
    val statusText: String = "Ready for instructions",
    val activeToolName: String? = null,
    val audioAmplitude: Float = 0f, // 0.0 to 1.0 for voice reactivity
    val modelLoaded: Boolean = false,
    val currentAnimation: String = "Idle",
    val detectedAnimations: List<String> = emptyList(),
    val customModelPath: String? = null
)
