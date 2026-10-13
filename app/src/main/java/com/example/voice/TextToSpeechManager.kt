package com.example.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale
import java.util.UUID

class TextToSpeechManager(
    private val context: Context,
    private val onSpeechStart: () -> Unit,
    private val onSpeechDone: () -> Unit,
    private val onAmplitudeChanged: (Float) -> Unit,
    private val onError: (String) -> Unit
) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var isSpeaking = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var amplitudeRunnable: Runnable? = null

    // Voice personality parameters
    var pitch: Float = 1.15f
        set(value) {
            field = value
            tts?.setPitch(value)
        }
    var speechRate: Float = 1.05f
        set(value) {
            field = value
            tts?.setSpeechRate(value)
        }

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                configureVoice()
            } else {
                Log.e("TextToSpeechManager", "TTS initialization failed: $status")
                onError("Text-to-speech could not be initialized")
            }
        }
    }

    private fun configureVoice() {
        val engine = tts ?: return
        engine.language = Locale.US
        engine.setPitch(pitch)
        engine.setSpeechRate(speechRate)

        // Find a natural female voice if available
        try {
            val voices = engine.voices
            if (!voices.isNullOrEmpty()) {
                val femaleVoice = voices.firstOrNull { voice ->
                    val name = voice.name.lowercase(Locale.ROOT)
                    (name.contains("female") || name.contains("en-us-x-sfg") || name.contains("en-us-x-iol") || name.contains("en-us-x-tpd")) &&
                            voice.locale.language == Locale.US.language
                } ?: voices.firstOrNull { it.locale.language == Locale.US.language && !it.isNetworkConnectionRequired }

                if (femaleVoice != null) {
                    engine.voice = femaleVoice
                }
            }
        } catch (e: Exception) {
            Log.w("TextToSpeechManager", "Custom voice selection error: ${e.message}")
        }

        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
                mainHandler.post {
                    onSpeechStart()
                    startAmplitudeSimulation()
                }
            }

            override fun onDone(utteranceId: String?) {
                isSpeaking = false
                mainHandler.post {
                    stopAmplitudeSimulation()
                    onSpeechDone()
                }
            }

            override fun onError(utteranceId: String?) {
                isSpeaking = false
                mainHandler.post {
                    stopAmplitudeSimulation()
                    onSpeechDone()
                }
            }
        })
    }

    fun speak(text: String) {
        if (!isInitialized) {
            Log.w("TextToSpeechManager", "TTS not initialized yet")
            return
        }

        stop()

        val utteranceId = UUID.randomUUID().toString()
        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        if (result == TextToSpeech.ERROR) {
            onError("TTS speech error")
        }
    }

    fun stop() {
        tts?.stop()
        isSpeaking = false
        stopAmplitudeSimulation()
        mainHandler.post {
            onSpeechDone()
        }
    }

    private fun startAmplitudeSimulation() {
        stopAmplitudeSimulation()
        var phase = 0f
        amplitudeRunnable = object : Runnable {
            override fun run() {
                if (isSpeaking) {
                    phase += 0.25f
                    // Create natural cadence wave oscillation (0.3 to 0.95)
                    val base = (Math.sin(phase.toDouble()).toFloat() * 0.35f + 0.55f).coerceIn(0.1f, 1f)
                    val jitter = (Math.random().toFloat() * 0.25f - 0.12f)
                    val amplitude = (base + jitter).coerceIn(0.15f, 1f)
                    onAmplitudeChanged(amplitude)
                    mainHandler.postDelayed(this, 50)
                } else {
                    onAmplitudeChanged(0f)
                }
            }
        }
        amplitudeRunnable?.let { mainHandler.post(it) }
    }

    private fun stopAmplitudeSimulation() {
        amplitudeRunnable?.let { mainHandler.removeCallbacks(it) }
        amplitudeRunnable = null
        onAmplitudeChanged(0f)
    }

    fun shutdown() {
        stopAmplitudeSimulation()
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
