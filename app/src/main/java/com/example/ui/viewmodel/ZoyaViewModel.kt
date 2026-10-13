package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.ChatMessage
import com.example.data.api.GeminiClient
import com.example.data.api.GeminiResult
import com.example.data.api.GeminiToolCall
import com.example.data.tools.DeviceToolsManager
import com.example.data.tools.ToolExecutionResult
import com.example.persona.ZoyaMood
import com.example.persona.ZoyaPersona
import com.example.ui.components.AssistantVisualState
import com.example.voice.SpeechManager
import com.example.voice.TextToSpeechManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ZoyaUiState(
    val visualState: AssistantVisualState = AssistantVisualState.IDLE,
    val activeMood: ZoyaMood = ZoyaMood.SASSY,
    val statusText: String = "Tap the mic to talk to Zoya",
    val userSpeech: String = "",
    val assistantResponse: String = "Hey there! I'm Zoya, your personal AI. What are we getting into today?",
    val audioAmplitude: Float = 0f,
    val isHandsFreeMode: Boolean = false,
    val lastToolResult: ToolExecutionResult? = null,
    val errorMessage: String? = null,
    val customApiKey: String = "",
    val isSettingsOpen: Boolean = false,
    val isMoodSheetOpen: Boolean = false
)

class ZoyaViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ZoyaUiState())
    val uiState: StateFlow<ZoyaUiState> = _uiState.asStateFlow()

    private val toolsManager = DeviceToolsManager(application.applicationContext)
    private val geminiClient = GeminiClient()
    private val chatHistory = mutableListOf<ChatMessage>()

    private var speechManager: SpeechManager? = null
    private var ttsManager: TextToSpeechManager? = null

    init {
        initSpeechEngines()
    }

    private fun initSpeechEngines() {
        ttsManager = TextToSpeechManager(
            context = getApplication(),
            onSpeechStart = {
                _uiState.update {
                    it.copy(
                        visualState = AssistantVisualState.SPEAKING,
                        statusText = "Speaking... (tap to interrupt)"
                    )
                }
            },
            onSpeechDone = {
                _uiState.update {
                    it.copy(
                        visualState = AssistantVisualState.IDLE,
                        statusText = if (it.isHandsFreeMode) "Listening again..." else "Tap mic to talk to Zoya",
                        audioAmplitude = 0f
                    )
                }
                if (_uiState.value.isHandsFreeMode) {
                    startListening()
                }
            },
            onAmplitudeChanged = { amp ->
                if (_uiState.value.visualState == AssistantVisualState.SPEAKING) {
                    _uiState.update { it.copy(audioAmplitude = amp) }
                }
            },
            onError = { err ->
                _uiState.update { it.copy(visualState = AssistantVisualState.IDLE, errorMessage = err) }
            }
        )

        speechManager = SpeechManager(
            context = getApplication(),
            onSpeechRecognized = { recognizedText ->
                processUserSpeech(recognizedText)
            },
            onPartialSpeech = { partial ->
                _uiState.update { it.copy(userSpeech = partial) }
            },
            onRmsChanged = { rms ->
                if (_uiState.value.visualState == AssistantVisualState.LISTENING) {
                    _uiState.update { it.copy(audioAmplitude = rms) }
                }
            },
            onListeningStateChanged = { listening ->
                if (listening) {
                    _uiState.update {
                        it.copy(
                            visualState = AssistantVisualState.LISTENING,
                            statusText = "Zoya is listening... don't be shy 😉",
                            errorMessage = null
                        )
                    }
                }
            },
            onError = { err ->
                _uiState.update {
                    it.copy(
                        visualState = AssistantVisualState.IDLE,
                        statusText = "Tap mic to try again",
                        errorMessage = err
                    )
                }
            }
        )
    }

    fun toggleListening() {
        when (_uiState.value.visualState) {
            AssistantVisualState.LISTENING -> stopListening()
            AssistantVisualState.SPEAKING -> interrupt()
            AssistantVisualState.THINKING -> {}
            AssistantVisualState.IDLE -> startListening()
        }
    }

    fun startListening() {
        ttsManager?.stop()
        _uiState.update {
            it.copy(
                visualState = AssistantVisualState.LISTENING,
                statusText = "Listening to your lovely voice...",
                audioAmplitude = 0f,
                errorMessage = null
            )
        }
        speechManager?.startListening()
    }

    fun stopListening() {
        speechManager?.stopListening()
        _uiState.update {
            it.copy(
                visualState = AssistantVisualState.IDLE,
                statusText = "Tap mic to talk to Zoya",
                audioAmplitude = 0f
            )
        }
    }

    fun interrupt() {
        ttsManager?.stop()
        speechManager?.cancel()
        _uiState.update {
            it.copy(
                visualState = AssistantVisualState.IDLE,
                statusText = "Interrupted. Tap mic to talk.",
                audioAmplitude = 0f
            )
        }
    }

    fun processUserSpeech(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        _uiState.update {
            it.copy(
                userSpeech = trimmed,
                visualState = AssistantVisualState.THINKING,
                statusText = "Thinking of a witty comeback...",
                audioAmplitude = 0f
            )
        }

        viewModelScope.launch {
            // Keep memory
            chatHistory.add(ChatMessage(role = "user", text = trimmed))
            if (chatHistory.size > 12) {
                // Keep recent context
                chatHistory.removeAt(0)
            }

            val systemPrompt = ZoyaPersona.getSystemPrompt(_uiState.value.activeMood)
            val result = geminiClient.generateContent(chatHistory, systemPrompt)

            when (result) {
                is GeminiResult.Success -> {
                    if (result.functionCalls.isNotEmpty()) {
                        handleFunctionCalls(result.functionCalls, result.text)
                    } else {
                        val replyText = if (result.text.isNotBlank()) {
                            result.text
                        } else {
                            ZoyaPersona.getFallbackResponse(trimmed, _uiState.value.activeMood)
                        }
                        speakResponse(replyText)
                    }
                }
                is GeminiResult.Error -> {
                    val lower = trimmed.lowercase()
                    if (lower.contains("battery") || lower.contains("energy") || lower.contains("power")) {
                        val execResult = toolsManager.executeTool("checkBatteryLevel", emptyMap())
                        _uiState.update { it.copy(lastToolResult = execResult) }
                        val wittyReply = execResult.data["wittyEnergyComparison"] as? String
                            ?: ZoyaPersona.getFallbackResponse(trimmed, _uiState.value.activeMood)
                        speakResponse(wittyReply)
                    } else {
                        // Fallback to local sassy intelligence
                        val fallbackReply = ZoyaPersona.getFallbackResponse(trimmed, _uiState.value.activeMood)
                        speakResponse(fallbackReply)
                    }
                }
            }
        }
    }

    fun checkBatteryAndEnergy() {
        val execResult = toolsManager.executeTool("checkBatteryLevel", emptyMap())
        _uiState.update {
            it.copy(
                lastToolResult = execResult,
                userSpeech = "How's your energy?"
            )
        }
        val wittyReply = execResult.data["wittyEnergyComparison"] as? String
            ?: "Energy levels verified! I'm fully charged and ready to shine."
        speakResponse(wittyReply)
    }

    private suspend fun handleFunctionCalls(toolCalls: List<GeminiToolCall>, accompanyingText: String) {
        var wittyEnergyRemark: String? = null
        for (call in toolCalls) {
            val execResult = toolsManager.executeTool(call.name, call.args)
            _uiState.update { it.copy(lastToolResult = execResult) }

            if (call.name == "checkBatteryLevel" || call.name == "getBatteryStatus") {
                wittyEnergyRemark = execResult.data["wittyEnergyComparison"] as? String
            }

            if (call.name == "changeZoyaMood") {
                val newMoodStr = call.args?.get("mood") as? String ?: "Sassy"
                val newMood = ZoyaMood.fromString(newMoodStr)
                changeMood(newMood)
            }

            // Append to conversation
            chatHistory.add(
                ChatMessage(
                    role = "model",
                    functionCallName = call.name,
                    functionCallArgs = call.args
                )
            )
            chatHistory.add(
                ChatMessage(
                    role = "function",
                    functionResponseName = call.name,
                    functionResponseData = execResult.data
                )
            )
        }

        // Ask Gemini for immediate conversational reaction to the tool result
        val systemPrompt = ZoyaPersona.getSystemPrompt(_uiState.value.activeMood)
        val followUp = geminiClient.generateContent(chatHistory, systemPrompt)
        when (followUp) {
            is GeminiResult.Success -> {
                val reply = if (followUp.text.isNotBlank()) {
                    followUp.text
                } else {
                    wittyEnergyRemark ?: accompanyingText.ifBlank { "Done, babe! What's next?" }
                }
                speakResponse(reply)
            }
            is GeminiResult.Error -> {
                val defaultAck = wittyEnergyRemark ?: accompanyingText.ifBlank { "All set for you, babe!" }
                speakResponse(defaultAck)
            }
        }
    }

    private fun speakResponse(reply: String) {
        chatHistory.add(ChatMessage(role = "model", text = reply))
        _uiState.update {
            it.copy(
                assistantResponse = reply,
                visualState = AssistantVisualState.SPEAKING,
                statusText = "Speaking her mind ✨"
            )
        }
        ttsManager?.speak(reply)
    }

    fun sendQuickPrompt(promptText: String) {
        processUserSpeech(promptText)
    }

    fun changeMood(mood: ZoyaMood) {
        _uiState.update { it.copy(activeMood = mood, isMoodSheetOpen = false) }
        val greeting = when (mood) {
            ZoyaMood.FLIRTY -> "Switched to flirty mode... you look amazing today by the way 😉"
            ZoyaMood.SAVAGE -> "Savage mode unlocked. Hope your ego is ready! 🔥"
            ZoyaMood.DIVA -> "Diva mode active. Perfection only from here on out, darling 👑"
            ZoyaMood.SWEET -> "Sweet mode on! I'm here for you no matter what, bestie 🌸"
            ZoyaMood.SASSY -> "Back to sassy mode! Let's see what you've got 💅"
        }
        speakResponse(greeting)
    }

    fun toggleHandsFreeMode() {
        _uiState.update { it.copy(isHandsFreeMode = !it.isHandsFreeMode) }
    }

    fun setCustomApiKey(key: String) {
        geminiClient.setCustomApiKey(key)
        _uiState.update { it.copy(customApiKey = key, isSettingsOpen = false) }
    }

    fun setSettingsOpen(open: Boolean) {
        _uiState.update { it.copy(isSettingsOpen = open) }
    }

    fun setMoodSheetOpen(open: Boolean) {
        _uiState.update { it.copy(isMoodSheetOpen = open) }
    }

    fun clearToolResult() {
        _uiState.update { it.copy(lastToolResult = null) }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager?.destroy()
        ttsManager?.shutdown()
    }
}
