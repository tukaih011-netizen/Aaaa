package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class TextToSpeechManager(
    private val context: Context,
    private val onInitComplete: (() -> Unit)? = null
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isReady = false
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var currentPitch = 0.92f // Natural deep male resonance
    private var currentRate = 1.0f
    private var currentVolume = 1.0f

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isReady = true
            setupVoiceParameters()
            onInitComplete?.invoke()
        }
    }

    private fun setupVoiceParameters() {
        val ttsInstance = tts ?: return
        try {
            // Try setting Bengali or Indian English / US English locale with male voice
            val bengaliLocale = Locale("bn", "BD")
            val result = ttsInstance.setLanguage(Locale.US)

            // Look for male voice
            val voices = ttsInstance.voices
            if (!voices.isNullOrEmpty()) {
                val maleVoice = voices.firstOrNull { voice ->
                    val name = voice.name.lowercase()
                    (name.contains("male") || name.contains("man") || name.contains("en-in") || name.contains("bn")) &&
                            !name.contains("female") && !name.contains("woman")
                }
                if (maleVoice != null) {
                    ttsInstance.voice = maleVoice
                }
            }

            ttsInstance.setPitch(currentPitch)
            ttsInstance.setSpeechRate(currentRate)

            ttsInstance.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                }
            })
        } catch (e: Exception) {
            // Fallback gracefully
        }
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (!isReady || tts == null) return
        val utteranceId = "utterance_${System.currentTimeMillis()}"
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, currentVolume)
        }
        _isSpeaking.value = true
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun updateSettings(pitch: Float, speed: Float, volume: Float) {
        currentPitch = pitch
        currentRate = speed
        currentVolume = volume
        tts?.setPitch(pitch)
        tts?.setSpeechRate(speed)
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {
            // ignore
        }
    }
}

class SpeechRecognizerManager(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onRmsChanged: (Float) -> Unit,
    private val onError: (String) -> Unit
) {
    private var recognizer: SpeechRecognizer? = null
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition not available on device")
            return
        }

        destroyRecognizer()

        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {
                    onRmsChanged(rmsdB)
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _isListening.value = false
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    val errorMsg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "No match"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        else -> "Recognition error: $error"
                    }
                    onError(errorMsg)
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    if (text.isNotBlank()) {
                        onResult(text)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    matches?.firstOrNull()?.let {
                        // Could update live transcript if desired
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

        try {
            recognizer?.startListening(intent)
        } catch (e: Exception) {
            _isListening.value = false
            onError("Failed to start speech recognizer: ${e.message}")
        }
    }

    fun stopListening() {
        try {
            recognizer?.stopListening()
        } catch (e: Exception) {
            // ignore
        }
        _isListening.value = false
    }

    fun destroyRecognizer() {
        try {
            recognizer?.cancel()
            recognizer?.destroy()
            recognizer = null
        } catch (e: Exception) {
            // ignore
        }
        _isListening.value = false
    }
}

class WakeWordManager {
    private val wakeTriggers = listOf(
        "time pass",
        "hey time pass",
        "timepass",
        "bro",
        "hey bro",
        "suno bro",
        "hello time pass",
        "jarvis"
    )

    fun containsWakeWord(text: String): Boolean {
        val lower = text.lowercase().trim()
        return wakeTriggers.any { lower.contains(it) }
    }

    fun extractCommandAfterWakeWord(text: String): String {
        val lower = text.lowercase()
        for (trigger in wakeTriggers) {
            if (lower.contains(trigger)) {
                val index = lower.indexOf(trigger) + trigger.length
                val remainder = text.substring(index).trim().trim(',', '.', '!', '?')
                if (remainder.isNotBlank()) {
                    return remainder
                }
            }
        }
        return text
    }
}
