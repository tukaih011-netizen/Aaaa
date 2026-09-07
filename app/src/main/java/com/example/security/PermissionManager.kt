package com.example.security

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.calls.CallCapabilityManager

data class PermissionItem(
    val title: String,
    val permissionId: String,
    val isGranted: Boolean,
    val purpose: String,
    val whyNeeded: String,
    val ifDisabled: String,
    val isCritical: Boolean = false
)

class PermissionManager(private val context: Context) {

    private val callCapabilityManager = CallCapabilityManager(context)

    fun getSecurityAudit(): List<PermissionItem> {
        val micGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        val phoneStateGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        val answerCallsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ANSWER_PHONE_CALLS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        val callReport = callCapabilityManager.checkCapability()

        val list = mutableListOf<PermissionItem>()

        list.add(
            PermissionItem(
                title = "Microphone Access",
                permissionId = Manifest.permission.RECORD_AUDIO,
                isGranted = micGranted,
                purpose = "Allows TIME PASS to process voice commands and wake-word triggers.",
                whyNeeded = "Required for hands-free Banglish speech recognition and dynamic audio waveform HUD rendering.",
                ifDisabled = "Voice input and wake-word will be inactive; only text chat can be used.",
                isCritical = true
            )
        )

        list.add(
            PermissionItem(
                title = "Notification Delivery",
                permissionId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else "android.permission.POST_NOTIFICATIONS",
                isGranted = notifGranted,
                purpose = "Displays scheduled alarms, persistent assistant service, and proactive battery alerts.",
                whyNeeded = "Required for persistent foreground service notifications and timely reminders.",
                ifDisabled = "Reminders and background status indicators will not appear on device status bar.",
                isCritical = false
            )
        )

        list.add(
            PermissionItem(
                title = "Phone State & Call Detection",
                permissionId = Manifest.permission.READ_PHONE_STATE,
                isGranted = phoneStateGranted,
                purpose = "Detects incoming calls to display futuristic In-Call HUD and trigger voice announcements.",
                whyNeeded = "Allows TIME PASS to announce incoming callers and manage call controls.",
                ifDisabled = "Call control features ('Call ta receive kor' / 'Call ta cut kor') will not trigger automatically.",
                isCritical = false
            )
        )

        list.add(
            PermissionItem(
                title = "Direct Call Answer & Management",
                permissionId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Manifest.permission.ANSWER_PHONE_CALLS else "android.permission.ANSWER_PHONE_CALLS",
                isGranted = answerCallsGranted,
                purpose = "Enables programmatic call answer and call cut via Telecom API.",
                whyNeeded = "Empowers natural voice commands like 'Call ta pick up kor' or 'Call ta cut kor'.",
                ifDisabled = "You will be prompted to answer calls manually through the system dialer.",
                isCritical = false
            )
        )

        list.add(
            PermissionItem(
                title = "Telecom Dialer Integration",
                permissionId = "android.app.role.DIALER",
                isGranted = callReport.isDefaultDialer,
                purpose = "Full hardware Telecom integration for system-wide call management.",
                whyNeeded = "Required on modern Android for full call answering without OEM vendor limitations.",
                ifDisabled = "Assistant operates in standard overlay mode without overriding default dialer.",
                isCritical = false
            )
        )

        return list
    }
}
