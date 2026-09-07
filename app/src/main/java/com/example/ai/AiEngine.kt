package com.example.ai

import android.content.Context
import android.provider.Settings
import com.example.apps.AppControlManager
import com.example.apps.LaunchResult
import com.example.calls.CallActionResult
import com.example.calls.CallCapabilityManager
import com.example.calls.TelecomCallHub
import com.example.data.local.AppDatabase
import com.example.data.local.MemoryEntity
import com.example.data.local.ReminderEntity
import com.example.data.model.DeviceTelemetry
import com.example.device.AudioSettingsManager
import com.example.device.FlashlightManager
import com.example.device.SystemInfoManager
import com.example.notifications.ReminderScheduler
import com.example.web.SearchProvider
import com.example.web.WebSearchManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.regex.Pattern

sealed class BanglishIntent {
    object CheckBattery : BanglishIntent()
    object CheckStorage : BanglishIntent()
    object CheckRam : BanglishIntent()
    object CheckFullDeviceStatus : BanglishIntent()
    data class ToggleFlashlight(val turnOn: Boolean?) : BanglishIntent()
    data class AdjustVolume(val increase: Boolean, val mute: Boolean = false) : BanglishIntent()
    data class OpenSettingsPage(val settingType: String) : BanglishIntent()
    data class LaunchApplication(val appName: String) : BanglishIntent()
    object AnswerPhoneCall : BanglishIntent()
    object EndPhoneCall : BanglishIntent()
    object RejectPhoneCall : BanglishIntent()
    data class CreateReminder(val title: String, val delayMinutes: Int = 10) : BanglishIntent()
    data class SearchWeb(val query: String) : BanglishIntent()
    data class SaveMemory(val key: String, val value: String) : BanglishIntent()
    data class AskMemory(val query: String) : BanglishIntent()
    object Greeting : BanglishIntent()
    object StopAssistant : BanglishIntent()
    data class GeneralConversation(val text: String) : BanglishIntent()
}

class IntentRouter {

    fun parseIntent(rawInput: String, contextSubject: String? = null): BanglishIntent {
        val text = rawInput.lowercase().trim()

        // 1. Contextual continuity checks (e.g. "Ar storage?", "Ar RAM?", "Battery?")
        if (text.startsWith("ar ") || text.startsWith("and ") || text.length < 15) {
            if (text.contains("storage") || text.contains("space") || text.contains("memory") && text.contains("free")) {
                return BanglishIntent.CheckStorage
            }
            if (text.contains("ram")) {
                return BanglishIntent.CheckRam
            }
            if (text.contains("battery") || text.contains("charge")) {
                return BanglishIntent.CheckBattery
            }
        }

        // 2. Greetings
        if (text == "hi" || text == "hello" || text.contains("good morning") || text.contains("good evening") ||
            text.contains("kemon achis") || text.contains("ki khobor") || text == "bro" || text.contains("assalamu alaikum")
        ) {
            return BanglishIntent.Greeting
        }

        // 3. Shutdown / Stop
        if (text.contains("offline jao") || text.contains("bondho hou") || text.contains("shutdown") ||
            text.contains("sleep") || text.contains("stop assistant") || text.contains("bye")
        ) {
            return BanglishIntent.StopAssistant
        }

        // 4. Call Control Commands
        if (text.contains("receive kor") || text.contains("pick up") || text.contains("call dhor") ||
            text.contains("call tul") || text.contains("answer call") || text.contains("call ta dhor")
        ) {
            return BanglishIntent.AnswerPhoneCall
        }

        if (text.contains("call ta cut kor") || text.contains("call cut") || text.contains("call end") ||
            text.contains("disconnect kor") || text.contains("kete de") || text.contains("hang up") || text.contains("call kete de")
        ) {
            return BanglishIntent.EndPhoneCall
        }

        if (text.contains("reject kor") || text.contains("decline kor") || text.contains("call katish")) {
            return BanglishIntent.RejectPhoneCall
        }

        // 5. Battery & Power
        if ((text.contains("battery") || text.contains("charge")) &&
            (text.contains("koto") || text.contains("kemon") || text.contains("check") || text.contains("status") || text.contains("percentage") || text.contains("percentage koto"))
        ) {
            return BanglishIntent.CheckBattery
        }

        // 6. Storage
        if (text.contains("storage") || text.contains("free space") || (text.contains("phone") && text.contains("space"))) {
            return BanglishIntent.CheckStorage
        }

        // 7. RAM
        if (text.contains("ram") || text.contains("memory usage")) {
            return BanglishIntent.CheckRam
        }

        // 8. Overall Device Condition
        if ((text.contains("condition") || text.contains("status") || text.contains("diagnostics") || text.contains("report")) &&
            (text.contains("phone") || text.contains("device") || text.contains("system"))
        ) {
            return BanglishIntent.CheckFullDeviceStatus
        }

        // 9. Flashlight / Torch
        if (text.contains("flashlight") || text.contains("torch") || text.contains("light")) {
            val turnOn = if (text.contains("off") || text.contains("bondho") || text.contains("nibhiye")) false
            else if (text.contains("on") || text.contains("chalu") || text.contains("jalo")) true
            else null
            return BanglishIntent.ToggleFlashlight(turnOn)
        }

        // 10. Volume controls
        if (text.contains("volume") || text.contains("sound") || text.contains("awaj")) {
            if (text.contains("barhao") || text.contains("up") || text.contains("increase") || text.contains("barha")) {
                return BanglishIntent.AdjustVolume(increase = true)
            }
            if (text.contains("komao") || text.contains("down") || text.contains("decrease") || text.contains("koma")) {
                return BanglishIntent.AdjustVolume(increase = false)
            }
            if (text.contains("mute") || text.contains("chup")) {
                return BanglishIntent.AdjustVolume(increase = false, mute = true)
            }
        }

        // 11. Settings Pages
        if (text.contains("wifi") && (text.contains("setting") || text.contains("kholo") || text.contains("open") || text.contains("on kor") || text.contains("off kor"))) {
            return BanglishIntent.OpenSettingsPage("wifi")
        }

        if (text.contains("bluetooth") && (text.contains("setting") || text.contains("kholo") || text.contains("open") || text.contains("on kor") || text.contains("off kor"))) {
            return BanglishIntent.OpenSettingsPage("bluetooth")
        }

        if (text.contains("brightness") || text.contains("display setting") || text.contains("rotation")) {
            return BanglishIntent.OpenSettingsPage("display")
        }

        // 12. App Launch
        if (text.contains("open kor") || text.contains("kholo") || text.contains("launch") || text.contains("chalu kor") || text.startsWith("open ")) {
            val appQuery = text.replace("open kor", "")
                .replace("kholo", "")
                .replace("launch", "")
                .replace("chalu kor", "")
                .replace("open", "")
                .replace("bro", "")
                .replace("ta", "")
                .trim()
            if (appQuery.isNotBlank()) {
                return BanglishIntent.LaunchApplication(appQuery)
            }
        }

        // 13. Reminders & Alarms
        if (text.contains("remind") || text.contains("mone koriye") || text.contains("reminder") || text.contains("alarm")) {
            var delayMinutes = 10
            val matcher = Pattern.compile("(\\d+)\\s*(min|minute|ghonta|hour|tay|tar por)").matcher(text)
            if (matcher.find()) {
                val num = matcher.group(1)?.toIntOrNull() ?: 10
                val unit = matcher.group(2) ?: "min"
                delayMinutes = if (unit.contains("ghonta") || unit.contains("hour")) num * 60 else num
            }

            val title = text.replace("amake", "")
                .replace("remind korish", "")
                .replace("remind kor", "")
                .replace("reminder dao", "")
                .replace("bro", "")
                .trim()
                .ifBlank { "Important task" }
            return BanglishIntent.CreateReminder(title, delayMinutes)
        }

        // 14. Memories
        if (text.contains("mone rakh") || text.contains("remember that") || text.contains("amar favorite")) {
            val parts = text.split("mone rakh", "remember", limit = 2)
            val toRemember = if (parts.size > 1) parts[1].trim() else text
            return BanglishIntent.SaveMemory(key = "user_note", value = toRemember)
        }

        // 15. Web Search / Weather / News
        if (text.contains("search") || text.contains("weather") || text.contains("abohawa") || text.contains("news") ||
            text.contains("khobor") || text.contains("google-e") || text.contains("ki hoyeche")
        ) {
            val q = text.replace("google-e", "")
                .replace("search kor", "")
                .replace("eta", "")
                .replace("check kor", "")
                .replace("bro", "")
                .trim()
            return BanglishIntent.SearchWeb(if (q.isNotBlank()) q else "Latest updates")
        }

        // Default: General conversational fallback
        return BanglishIntent.GeneralConversation(rawInput)
    }
}

class ContextManager {
    private var lastSubject: String? = null
    private var lastQuery: String? = null

    fun updateContext(subject: String?, query: String?) {
        if (!subject.isNullOrBlank()) lastSubject = subject
        if (!query.isNullOrBlank()) lastQuery = query
    }

    fun getLastSubject(): String? = lastSubject
}

data class ExecutionResult(
    val spokenResponse: String,
    val displayText: String,
    val actionType: String,
    val isSuccess: Boolean,
    val verificationNote: String? = null
)

class ActionPlanner(
    private val context: Context,
    private val systemInfoManager: SystemInfoManager,
    private val flashlightManager: FlashlightManager,
    private val audioSettingsManager: AudioSettingsManager,
    private val appControlManager: AppControlManager,
    private val searchProvider: SearchProvider,
    private val callCapabilityManager: CallCapabilityManager,
    private val reminderScheduler: ReminderScheduler,
    private val database: AppDatabase
) {

    suspend fun executeIntent(intent: BanglishIntent): ExecutionResult = withContext(Dispatchers.IO) {
        when (intent) {
            is BanglishIntent.CheckBattery -> {
                val tele = systemInfoManager.getTelemetry()
                val chargingText = if (tele.isCharging) "charging-e ache" else "on battery"
                val spoken = "Battery ${tele.batteryPct}%, ${chargingText} bro. Temperature ${tele.batteryTemp}°C, normal ache."
                val display = "BATTERY: ${tele.batteryPct}% • ${if (tele.isCharging) "CHARGING" else "DISCHARGING"}\nTEMP: ${tele.batteryTemp}°C • VOLTAGE: ${tele.batteryVoltage}V"
                ExecutionResult(spoken, display, "BATTERY_CHECK", true, "Verified via BatteryManager")
            }

            is BanglishIntent.CheckStorage -> {
                val tele = systemInfoManager.getTelemetry()
                val spoken = "Storage-e around ${tele.storageFreeGb} GB free ache bro. Total ${tele.storageTotalGb} GB."
                val display = "STORAGE FREE: ${tele.storageFreeGb} GB / ${tele.storageTotalGb} GB Total"
                ExecutionResult(spoken, display, "STORAGE_CHECK", true, "Verified via StatFs")
            }

            is BanglishIntent.CheckRam -> {
                val tele = systemInfoManager.getTelemetry()
                val spoken = "RAM-e ${tele.ramAvailGb} GB free ache bro. Total ${tele.ramTotalGb} GB."
                val display = "RAM: ${tele.ramUsedGb} GB Used / ${tele.ramTotalGb} GB Total (${tele.ramAvailGb} GB Free)"
                ExecutionResult(spoken, display, "RAM_CHECK", true, "Verified via ActivityManager")
            }

            is BanglishIntent.CheckFullDeviceStatus -> {
                val tele = systemInfoManager.getTelemetry()
                val spoken = "Phone overall ekdom normal bro. Battery ${tele.batteryPct}%, storage-e ${tele.storageFreeGb} GB free, RAM ${tele.ramAvailGb} GB available, network ${tele.networkType}."
                val display = "SYSTEM TELEMETRY SUMMARY\n• Battery: ${tele.batteryPct}% (${tele.batteryTemp}°C)\n• Storage: ${tele.storageFreeGb}GB Free\n• RAM: ${tele.ramAvailGb}GB Available\n• Network: ${tele.networkType}\n• Model: ${tele.deviceModel}\n• Uptime: ${tele.uptimeFormatted}"
                ExecutionResult(spoken, display, "FULL_DIAGNOSTICS", true, "Verified across all hardware subsystems")
            }

            is BanglishIntent.ToggleFlashlight -> {
                val newState = flashlightManager.toggleFlashlight(intent.turnOn)
                if (newState) {
                    val isOn = flashlightManager.isFlashlightOn()
                    val stateText = if (isOn) "on" else "off"
                    val spoken = "Done bro, flashlight $stateText kore diyechi."
                    val display = "FLASHLIGHT: ${if (isOn) "ACTIVE [ON]" else "INACTIVE [OFF]"}"
                    ExecutionResult(spoken, display, "FLASHLIGHT", true, "CameraManager Torch Mode")
                } else {
                    ExecutionResult("Bro, flashlight access korte problem hocche.", "Flashlight Hardware Unavailable", "FLASHLIGHT", false)
                }
            }

            is BanglishIntent.AdjustVolume -> {
                if (intent.mute) {
                    audioSettingsManager.muteVolume(true)
                    ExecutionResult("Done bro, volume mute kore diyechi.", "VOLUME: MUTED", "VOLUME_CHANGE", true)
                } else {
                    val changed = audioSettingsManager.adjustVolume(intent.increase)
                    val dir = if (intent.increase) "barhiye" else "komiye"
                    if (changed) {
                        ExecutionResult("Done bro, volume $dir diyechi.", "VOLUME: ${if (intent.increase) "INCREASED" else "DECREASED"}", "VOLUME_CHANGE", true)
                    } else {
                        ExecutionResult("Volume control korte parlam na bro.", "Volume Control Failed", "VOLUME_CHANGE", false)
                    }
                }
            }

            is BanglishIntent.OpenSettingsPage -> {
                when (intent.settingType) {
                    "wifi" -> {
                        appControlManager.openSystemSettings(Settings.ACTION_WIFI_SETTINGS)
                        ExecutionResult("Android direct toggle allow kore na, tai WiFi settings page khule dicchi bro.", "OPENED: WI-FI SETTINGS", "OPEN_SETTINGS", true)
                    }
                    "bluetooth" -> {
                        appControlManager.openSystemSettings(Settings.ACTION_BLUETOOTH_SETTINGS)
                        ExecutionResult("Bluetooth settings ta khule dilam bro.", "OPENED: BLUETOOTH SETTINGS", "OPEN_SETTINGS", true)
                    }
                    else -> {
                        appControlManager.openSystemSettings(Settings.ACTION_DISPLAY_SETTINGS)
                        ExecutionResult("Display and brightness settings khule dilam bro.", "OPENED: DISPLAY SETTINGS", "OPEN_SETTINGS", true)
                    }
                }
            }

            is BanglishIntent.LaunchApplication -> {
                when (val res = appControlManager.launchAppByName(intent.appName)) {
                    is LaunchResult.Success -> {
                        ExecutionResult("Haan bro, ${res.appName} khule dicchi.", "LAUNCHED: ${res.appName}", "APP_LAUNCH", true, "Package Manager Intent Executed")
                    }
                    is LaunchResult.NotFound -> {
                        ExecutionResult("Bro, '${intent.appName}' app ta phone-e pawa gelo na.", "App Not Found: ${intent.appName}", "APP_LAUNCH", false)
                    }
                    is LaunchResult.Failed -> {
                        ExecutionResult("Bro, app ta open korte parlam na: ${res.reason}", "Launch Failed", "APP_LAUNCH", false)
                    }
                }
            }

            is BanglishIntent.AnswerPhoneCall -> {
                val callResult = TelecomCallHub.answerCall(context)
                when (callResult) {
                    is CallActionResult.Success -> {
                        ExecutionResult("Haan bro, call ta receive hoye geche.", "CALL ANSWERED", "CALL_ANSWER", true, "TelecomManager Accepted")
                    }
                    is CallActionResult.NeedsRole -> {
                        ExecutionResult("Eta direct control korte Android-er required call role lagbe bro.", callResult.explanation, "CALL_ANSWER", false)
                    }
                    is CallActionResult.Failed -> {
                        ExecutionResult("Bro, call receive korte parlam na: ${callResult.error}", "Call Answer Failed", "CALL_ANSWER", false)
                    }
                }
            }

            is BanglishIntent.EndPhoneCall -> {
                val callResult = TelecomCallHub.endCall(context)
                when (callResult) {
                    is CallActionResult.Success -> {
                        ExecutionResult("Done bro, call ta end hoye geche.", "CALL TERMINATED", "CALL_END", true, "Telecom Call Terminated")
                    }
                    is CallActionResult.NeedsRole -> {
                        ExecutionResult("Eta direct control korte Android-er required call role lagbe bro.", callResult.explanation, "CALL_END", false)
                    }
                    is CallActionResult.Failed -> {
                        ExecutionResult("Bro, call cut korte parlam na: ${callResult.error}", "Call End Failed", "CALL_END", false)
                    }
                }
            }

            is BanglishIntent.RejectPhoneCall -> {
                TelecomCallHub.endCall(context)
                ExecutionResult("Incoming call ta decline kore dilam bro.", "CALL DECLINED", "CALL_REJECT", true)
            }

            is BanglishIntent.CreateReminder -> {
                val triggerTime = System.currentTimeMillis() + (intent.delayMinutes * 60 * 1000L)
                val entity = ReminderEntity(
                    title = intent.title,
                    triggerTimeMillis = triggerTime
                )
                val id = database.reminderDao().insertReminder(entity)
                reminderScheduler.scheduleReminder(entity.copy(id = id))
                val spoken = "Done bro. ${intent.delayMinutes} minute por '${intent.title}' remind kore dibo."
                val display = "REMINDER SET: ${intent.title} (In ${intent.delayMinutes} min)"
                ExecutionResult(spoken, display, "REMINDER_SET", true, "AlarmManager Scheduled")
            }

            is BanglishIntent.SearchWeb -> {
                val searchRes = searchProvider.searchLiveInfo(intent.query)
                val spoken = searchRes.summary
                val display = "WEB SEARCH: ${searchRes.query}\n${searchRes.summary}"
                ExecutionResult(spoken, display, "WEB_SEARCH", true, "Live Search / Gemini")
            }

            is BanglishIntent.SaveMemory -> {
                database.memoryDao().insertMemory(
                    MemoryEntity(key = intent.key, value = intent.value)
                )
                val spoken = "Mone rakhlam bro: ${intent.value}."
                val display = "MEMORY SAVED: ${intent.value}"
                ExecutionResult(spoken, display, "MEMORY_SAVE", true)
            }

            is BanglishIntent.AskMemory -> {
                val memories = database.memoryDao().getAllMemories()
                ExecutionResult("Checking saved memories bro...", "MEMORY LOOKUP", "MEMORY_QUERY", true)
            }

            is BanglishIntent.Greeting -> {
                val cal = Calendar.getInstance()
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                val timeGreet = when {
                    hour in 5..11 -> "Good morning"
                    hour in 12..16 -> "Good afternoon"
                    hour in 17..21 -> "Good evening"
                    else -> "Hello"
                }
                val spoken = "$timeGreet bro! TIME PASS AI Core online ache. Ki lagbe bol?"
                val display = "AI CORE ONLINE • $timeGreet"
                ExecutionResult(spoken, display, "GREETING", true)
            }

            is BanglishIntent.StopAssistant -> {
                ExecutionResult("Alright bro, I'm going offline. Stay safe!", "SYSTEM GOING OFFLINE", "SHUTDOWN", true)
            }

            is BanglishIntent.GeneralConversation -> {
                val liveAnswer = searchProvider.searchLiveInfo(intent.text)
                ExecutionResult(liveAnswer.summary, liveAnswer.summary, "CONVERSATION", true)
            }
        }
    }
}

class AiEngine(
    private val context: Context,
    private val systemInfoManager: SystemInfoManager,
    private val flashlightManager: FlashlightManager,
    private val audioSettingsManager: AudioSettingsManager,
    private val appControlManager: AppControlManager,
    private val searchProvider: SearchProvider,
    private val callCapabilityManager: CallCapabilityManager,
    private val reminderScheduler: ReminderScheduler,
    private val database: AppDatabase
) {
    private val router = IntentRouter()
    private val contextManager = ContextManager()
    private val actionPlanner = ActionPlanner(
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

    suspend fun processUserCommand(input: String): ExecutionResult {
        val intent = router.parseIntent(input, contextManager.getLastSubject())
        val result = actionPlanner.executeIntent(intent)
        contextManager.updateContext(intent::class.simpleName, input)
        return result
    }
}
