package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.example.ai.AiEngine
import com.example.apps.AppControlManager
import com.example.calls.CallCapabilityManager
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesManager
import com.example.data.model.AssistantState
import com.example.device.AudioSettingsManager
import com.example.device.FlashlightManager
import com.example.device.SystemInfoManager
import com.example.notifications.AssistantNotificationManager
import com.example.notifications.ReminderScheduler
import com.example.sound.SoundEffect
import com.example.sound.SoundFxManager
import com.example.voice.SpeechRecognizerManager
import com.example.voice.TextToSpeechManager
import com.example.voice.WakeWordManager
import com.example.web.WebSearchManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AssistantForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var notifManager: AssistantNotificationManager
    private lateinit var ttsManager: TextToSpeechManager
    private lateinit var soundFxManager: SoundFxManager
    private lateinit var prefsManager: PreferencesManager
    private lateinit var aiEngine: AiEngine
    private val wakeWordManager = WakeWordManager()

    companion object {
        const val ACTION_START = "ACTION_START_ASSISTANT_SERVICE"
        const val ACTION_STOP = "ACTION_STOP_ASSISTANT_SERVICE"
        const val ACTION_TRIGGER_MIC = "ACTION_TRIGGER_MIC"
        const val NOTIFICATION_ID = 9001

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _serviceAssistantState = MutableStateFlow(AssistantState.IDLE)
        val serviceAssistantState: StateFlow<AssistantState> = _serviceAssistantState.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, AssistantForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AssistantForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        notifManager = AssistantNotificationManager(this)
        soundFxManager = SoundFxManager()
        prefsManager = PreferencesManager(this)
        ttsManager = TextToSpeechManager(this)

        val database = AppDatabase.getDatabase(this)
        val systemInfoManager = SystemInfoManager(this)
        val flashlightManager = FlashlightManager(this)
        val audioSettingsManager = AudioSettingsManager(this)
        val appControlManager = AppControlManager(this)
        val searchProvider = WebSearchManager()
        val callCapabilityManager = CallCapabilityManager(this)
        val reminderScheduler = ReminderScheduler(this)

        aiEngine = AiEngine(
            context = this,
            systemInfoManager = systemInfoManager,
            flashlightManager = flashlightManager,
            audioSettingsManager = audioSettingsManager,
            appControlManager = appControlManager,
            searchProvider = searchProvider,
            callCapabilityManager = callCapabilityManager,
            reminderScheduler = reminderScheduler,
            database = database
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                shutdownService()
                return START_NOT_STICKY
            }
            ACTION_TRIGGER_MIC -> {
                // Trigger quick listening
            }
            else -> {
                startForeground(NOTIFICATION_ID, notifManager.buildForegroundNotification("AI CORE ONLINE • Active Monitoring"))
                _isRunning.value = true
                _serviceAssistantState.value = AssistantState.IDLE
            }
        }
        return START_STICKY
    }

    private fun shutdownService() {
        _isRunning.value = false
        _serviceAssistantState.value = AssistantState.OFFLINE
        soundFxManager.playSound(SoundEffect.SHUTDOWN)
        ttsManager.shutdown()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        _isRunning.value = false
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
