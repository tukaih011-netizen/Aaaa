package com.example.data.model

enum class AssistantState(val label: String, val BanglishStatus: String) {
    IDLE("SYSTEM ONLINE", "Ready bro, bol..."),
    WAKE_DETECTED("WAKE DETECTED", "Haan bro bolchi..."),
    LISTENING("LISTENING...", "Shunchi bro..."),
    UNDERSTANDING("UNDERSTANDING...", "Bujhte parchi..."),
    THINKING("SCANNING SYSTEM...", "Bhabchi / Process korchi..."),
    EXECUTING("EXECUTING ACTION...", "Kaaj cholche..."),
    VERIFYING("VERIFYING...", "Verify korchi..."),
    SPEAKING("SPEAKING...", "Bolchi..."),
    SUCCESS("SYSTEM READY", "Done bro!"),
    ERROR("SYSTEM ALERT", "Error hoyeche bro"),
    OFFLINE("OFFLINE", "System offline")
}

data class DeviceTelemetry(
    val batteryPct: Int = 0,
    val isCharging: Boolean = false,
    val batteryTemp: Float = 0f,
    val batteryVoltage: Float = 0f,
    val ramTotalGb: Float = 0f,
    val ramUsedGb: Float = 0f,
    val ramAvailGb: Float = 0f,
    val storageTotalGb: Float = 0f,
    val storageFreeGb: Float = 0f,
    val wifiEnabled: Boolean = false,
    val wifiSsid: String = "Disconnected",
    val isOnline: Boolean = false,
    val networkType: String = "No Network",
    val uptimeFormatted: String = "0h 0m",
    val androidVersion: String = "Android",
    val deviceModel: String = "Device",
    val cpuCores: Int = 8
)

enum class PhoneCallStatus {
    IDLE,
    RINGING,
    ACTIVE,
    DISCONNECTED
}

data class CallInfo(
    val status: PhoneCallStatus = PhoneCallStatus.IDLE,
    val callerName: String? = null,
    val callerNumber: String? = null,
    val durationSeconds: Long = 0L,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val canAnswerDirectly: Boolean = false,
    val canEndDirectly: Boolean = false,
    val reasonMessage: String? = null
)
