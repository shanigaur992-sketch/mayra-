package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceManager(
    private val context: Context,
    private val onInterruptionDetected: () -> Unit = {}
) : TextToSpeech.OnInitListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private var currentSpeechResultCallback: ((String) -> Unit)? = null
    private var currentErrorCallback: ((String) -> Unit)? = null
    private var currentPartialCallback: ((String) -> Unit)? = null

    // Supported locales
    private val hindiLocale = Locale("hi", "IN")
    private val englishLocale = Locale.US

    init {
        try {
            textToSpeech = TextToSpeech(context.applicationContext, this)
        } catch (_: Exception) {
            isTtsInitialized = false
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.let { tts ->
                // Try setting language to Hindi or default
                val defaultLocale = Locale.getDefault()
                val langResult = tts.setLanguage(defaultLocale)
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(englishLocale)
                }

                tts.setPitch(1.05f) // Warm, pleasant assistant pitch
                tts.setSpeechRate(1.0f)
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        _audioAmplitude.value = 0f
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        _audioAmplitude.value = 0f
                    }
                })
                isTtsInitialized = true
            }
        }
    }

    fun isSpeechRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun createSystemSpeechIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("hi-IN", "hi", "en-IN", "en-US"))
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to MYRAA (English or हिन्दी)")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
    }

    fun startListening(
        onResult: (String) -> Unit,
        onPartial: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        // If speaking when listening is initiated, interrupt TTS immediately
        if (_isSpeaking.value) {
            stopSpeaking()
            onInterruptionDetected()
        }

        currentSpeechResultCallback = onResult
        currentPartialCallback = onPartial
        currentErrorCallback = onError
        _partialText.value = ""

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("RECOGNIZER_UNAVAILABLE")
            return
        }

        stopListening()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                        _partialText.value = ""
                    }

                    override fun onBeginningOfSpeech() {
                        if (_isSpeaking.value) {
                            stopSpeaking()
                            onInterruptionDetected()
                        }
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                        _audioAmplitude.value = normalized
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isListening.value = false
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        _audioAmplitude.value = 0f
                        val errMsg = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Speech recognizer client error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                            SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Please speak again."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer busy, retrying..."
                            SpeechRecognizer.ERROR_SERVER -> "Voice server error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard. Tap mic and try again."
                            else -> "Speech recognition error ($error)"
                        }

                        currentErrorCallback?.invoke(errMsg)
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        _audioAmplitude.value = 0f
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim() ?: ""
                        if (text.isNotBlank()) {
                            _partialText.value = text
                            currentSpeechResultCallback?.invoke(text)
                        } else {
                            currentErrorCallback?.invoke("No speech detected. Please speak again.")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull()?.trim() ?: ""
                        if (partial.isNotBlank()) {
                            _partialText.value = partial
                            currentPartialCallback?.invoke(partial)
                            if (_isSpeaking.value) {
                                stopSpeaking()
                                onInterruptionDetected()
                            }
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                // Use default or multi-language hinting so Hindi and English both work seamlessly
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("hi-IN", "hi", "en-IN", "en-US"))
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _isListening.value = false
            onError("Failed to start speech recognition: ${e.message}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        _isListening.value = false
        _audioAmplitude.value = 0f
    }

    fun speak(
        text: String,
        onStart: () -> Unit = {},
        onDone: () -> Unit = {}
    ) {
        val cleanText = text
            .replace(Regex("```[\\s\\S]*?```"), "") // Remove code blocks from speech
            .replace(Regex("[*#_`>]"), "") // Remove markdown characters
            .trim()

        if (cleanText.isBlank()) {
            onDone()
            return
        }

        stopSpeaking()

        val tts = textToSpeech
        if (tts == null || !isTtsInitialized) {
            onDone()
            return
        }

        try {
            // Check if text has Hindi / Devanagari characters (\u0900 - \u097F)
            val hasHindi = cleanText.any { it in '\u0900'..'\u097F' }
            val targetLocale = if (hasHindi) hindiLocale else englishLocale

            val langCheck = tts.setLanguage(targetLocale)
            if (langCheck == TextToSpeech.LANG_MISSING_DATA || langCheck == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to default locale
                tts.setLanguage(Locale.getDefault())
            }

            val utteranceId = "myraa_utterance_${System.currentTimeMillis()}"
            onStart()
            tts.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } catch (_: Exception) {
            _isSpeaking.value = false
            onDone()
        }
    }

    fun stopSpeaking() {
        if (_isSpeaking.value) {
            try {
                textToSpeech?.stop()
            } catch (_: Exception) {}
            _isSpeaking.value = false
            _audioAmplitude.value = 0f
        }
    }

    fun cancelAll() {
        stopSpeaking()
        stopListening()
    }

    fun destroy() {
        cancelAll()
        try {
            textToSpeech?.shutdown()
        } catch (_: Exception) {}
        textToSpeech = null
    }
}
