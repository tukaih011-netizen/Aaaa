package com.example.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiEngine
import com.example.ai.BanglishIntent
import com.example.ai.ExecutionResult
import com.example.ai.IntentRouter
import com.example.apps.AppControlManager
import com.example.calls.CallActionResult
import com.example.calls.CallCapabilityManager
import com.example.calls.CallCapabilityReport
import com.example.calls.TelecomCallHub
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.MemoryEntity
import com.example.data.local.PreferencesManager
import com.example.data.local.ReminderEntity
import com.example.data.model.AssistantState
import com.example.data.model.CallInfo
import com.example.data.model.DeviceTelemetry
import com.example.device.AudioSettingsManager
import com.example.device.FlashlightManager
import com.example.device.SystemInfoManager
import com.example.notifications.ReminderScheduler
import com.example.security.PermissionItem
import com.example.security.PermissionManager
import com.example.service.AssistantForegroundService
import com.example.sound.SoundEffect
import com.example.sound.SoundFxManager
import com.example.voice.SpeechRecognizerManager
import com.example.voice.TextToSpeechManager
import com.example.voice.WakeWordManager
import com.example.web.WebSearchManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    val database = AppDatabase.getDatabase(context)
    val prefsManager = PreferencesManager(context)
    val systemInfoManager = SystemInfoManager(context)
    val flashlightManager = FlashlightManager(context)
    val audioSettingsManager = AudioSettingsManager(context)
    val appControlManager = AppControlManager(context)
    val searchProvider = WebSearchManager()
    val callCapabilityManager = CallCapabilityManager(context)
    val reminderScheduler = ReminderScheduler(context)
    val soundFxManager = SoundFxManager()
    val permissionManager = PermissionManager(context)
    val wakeWordManager = WakeWordManager()

    val aiEngine = AiEngine(
        context = context,
        systemInfoManager = systemInfoManager,
        flashlightManager = flashlightManager,
        audioSettingsManager = audioSettingsManager,
        appControlManager = appControlManager,
        searchProvider = searchProvider,
        callCapabilityManager = callCapabilityManager,
        reminderScheduler = reminderScheduler,
        database = database
    )

    // State Holders
    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _telemetry = MutableStateFlow(systemInfoManager.getTelemetry())
    val telemetry: StateFlow<DeviceTelemetry> = _telemetry.asStateFlow()

    val callInfo: StateFlow<CallInfo> = TelecomCallHub.callInfo
    val chatMessages: StateFlow<List<ChatMessageEntity>> = database.chatDao().getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memories: StateFlow<List<MemoryEntity>> = database.memoryDao().getAllMemories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminders: StateFlow<List<ReminderEntity>> = database.reminderDao().getAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    private val _activeScreen = MutableStateFlow("HUD") // HUD, CHAT, DIAGNOSTICS, CALLS, MEMORY, REMINDERS, SECURITY, SETTINGS
    val activeScreen: StateFlow<String> = _activeScreen.asStateFlow()

    private val _securityAudit = MutableStateFlow<List<PermissionItem>>(emptyList())
    val securityAudit: StateFlow<List<PermissionItem>> = _securityAudit.asStateFlow()

    private val _callCapability = MutableStateFlow(callCapabilityManager.checkCapability())
    val callCapability: StateFlow<CallCapabilityReport> = _callCapability.asStateFlow()

    private val _lastExecutionResult = MutableStateFlow<ExecutionResult?>(null)
    val lastExecutionResult: StateFlow<ExecutionResult?> = _lastExecutionResult.asStateFlow()

    var ttsManager: TextToSpeechManager? = null
    var speechRecognizerManager: SpeechRecognizerManager? = null

    init {
        // Init TTS
        ttsManager = TextToSpeechManager(context) {
            viewModelScope.launch {
                val startupSound = prefsManager.startupSoundEnabled.first()
                if (startupSound) {
                    soundFxManager.playSound(SoundEffect.BOOT)
                }
            }
        }

        // Init SpeechRecognizer
        speechRecognizerManager = SpeechRecognizerManager(
            context = context,
            onResult = { text -> handleVoiceInput(text) },
            onRmsChanged = { rms -> _audioRms.value = rms },
            onError = { error ->
                _assistantState.value = AssistantState.IDLE
                _audioRms.value = 0f
            }
        )

        // Observe Preferences to update TTS tuning
        viewModelScope.launch {
            combine(
                prefsManager.voicePitch,
                prefsManager.voiceSpeed,
                prefsManager.voiceVolume
            ) { pitch, speed, volume ->
                ttsManager?.updateSettings(pitch, speed, volume)
            }.collect()
        }

        // Periodically refresh telemetry
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                _telemetry.value = systemInfoManager.getTelemetry()
                delay(3000)
            }
        }

        refreshSecurityAudit()
    }

    fun setActiveScreen(screen: String) {
        _activeScreen.value = screen
    }

    fun refreshSecurityAudit() {
        _securityAudit.value = permissionManager.getSecurityAudit()
        _callCapability.value = callCapabilityManager.checkCapability()
    }

    fun startListening() {
        soundFxManager.playSound(SoundEffect.LISTEN)
        _assistantState.value = AssistantState.LISTENING
        speechRecognizerManager?.startListening()
    }

    fun stopListening() {
        speechRecognizerManager?.stopListening()
        _assistantState.value = AssistantState.IDLE
        _audioRms.value = 0f
    }

    fun handleVoiceInput(recognizedText: String) {
        if (recognizedText.isBlank()) {
            _assistantState.value = AssistantState.IDLE
            return
        }

        val cleanCommand = wakeWordManager.extractCommandAfterWakeWord(recognizedText)
        processUserCommand(cleanCommand)
    }

    fun sendTextMessage(input: String) {
        if (input.isBlank()) return
        processUserCommand(input)
    }

    private fun processUserCommand(input: String) {
        viewModelScope.launch {
            // 1. Store User Message
            database.chatDao().insertMessage(
                ChatMessageEntity(
                    text = input,
                    isUser = true,
                    timestamp = System.currentTimeMillis()
                )
            )

            // 2. State: UNDERSTANDING
            _assistantState.value = AssistantState.UNDERSTANDING
            soundFxManager.playSound(SoundEffect.THINK)
            delay(200)

            // 3. State: THINKING
            _assistantState.value = AssistantState.THINKING
            delay(300)

            // 4. State: EXECUTING
            _assistantState.value = AssistantState.EXECUTING
            val result = aiEngine.processUserCommand(input)
            _lastExecutionResult.value = result

            // 5. State: VERIFYING
            _assistantState.value = AssistantState.VERIFYING
            delay(200)

            // 6. State: SUCCESS / ERROR & Save AI Message
            if (result.isSuccess) {
                soundFxManager.playSound(SoundEffect.SUCCESS)
                _assistantState.value = AssistantState.SPEAKING
            } else {
                soundFxManager.playSound(SoundEffect.ERROR)
                _assistantState.value = AssistantState.ERROR
            }

            database.chatDao().insertMessage(
                ChatMessageEntity(
                    text = result.displayText,
                    isUser = false,
                    timestamp = System.currentTimeMillis(),
                    actionType = result.actionType,
                    verifiedSuccess = result.isSuccess
                )
            )

            // 7. Speak spokenResponse
            ttsManager?.speak(result.spokenResponse)

            // Return to IDLE after a short pause if not speaking continuously
            delay(2000)
            if (_assistantState.value != AssistantState.LISTENING) {
                _assistantState.value = AssistantState.IDLE
            }
        }
    }

    // Call Actions
    fun answerActiveCall() {
        val res = TelecomCallHub.answerCall(context)
        if (res is CallActionResult.Success) {
            soundFxManager.playSound(SoundEffect.SUCCESS)
            ttsManager?.speak("Call received bro.")
        }
    }

    fun endActiveCall() {
        val res = TelecomCallHub.endCall(context)
        if (res is CallActionResult.Success) {
            soundFxManager.playSound(SoundEffect.NOTIFICATION)
            ttsManager?.speak("Call disconnected.")
        }
    }

    fun simulateIncomingCall() {
        soundFxManager.playSound(SoundEffect.CALL_RING)
        TelecomCallHub.simulateIncomingCallForTesting()
    }

    fun dismissCall() {
        TelecomCallHub.dismissCallSimulation()
    }

    // Memory Actions
    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            database.memoryDao().deleteMemoryById(id)
        }
    }

    fun addManualMemory(key: String, value: String) {
        viewModelScope.launch {
            database.memoryDao().insertMemory(MemoryEntity(key = key, value = value))
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            database.memoryDao().clearAllMemories()
        }
    }

    // Reminder Actions
    fun addReminder(title: String, delayMinutes: Int) {
        viewModelScope.launch {
            val triggerTime = System.currentTimeMillis() + (delayMinutes * 60 * 1000L)
            val entity = ReminderEntity(title = title, triggerTimeMillis = triggerTime)
            val id = database.reminderDao().insertReminder(entity)
            reminderScheduler.scheduleReminder(entity.copy(id = id))
            soundFxManager.playSound(SoundEffect.SUCCESS)
        }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch {
            reminderScheduler.cancelReminder(id)
            database.reminderDao().deleteReminderById(id)
        }
    }

    // Chat History Clear
    fun clearChatHistory() {
        viewModelScope.launch {
            database.chatDao().clearAllMessages()
        }
    }

    // Background Service Toggle
    fun toggleBackgroundService(start: Boolean) {
        if (start) {
            AssistantForegroundService.startService(context)
        } else {
            AssistantForegroundService.stopService(context)
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizerManager?.destroyRecognizer()
        ttsManager?.shutdown()
    }
}
