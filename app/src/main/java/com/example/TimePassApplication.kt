package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class TimePassApplication : Application() {

    companion object {
        const val CHANNEL_ASSISTANT = "time_pass_assistant_service"
        const val CHANNEL_REMINDERS = "time_pass_reminders"
        const val CHANNEL_ANNOUNCEMENTS = "time_pass_announcements"
        const val CHANNEL_CALLS = "time_pass_calls"
        
        lateinit var instance: TimePassApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val assistantChannel = NotificationChannel(
                CHANNEL_ASSISTANT,
                "TIME PASS Assistant Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent background voice & HUD service status"
                setShowBadge(false)
            }

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "TIME PASS Reminders & Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Scheduled voice alerts and reminders"
                enableVibration(true)
            }

            val announcementChannel = NotificationChannel(
                CHANNEL_ANNOUNCEMENTS,
                "TIME PASS Proactive Announcements",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Battery alerts, morning briefings, system status"
            }

            val callChannel = NotificationChannel(
                CHANNEL_CALLS,
                "TIME PASS Call Control",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Call integration and telecom alerts"
            }

            notificationManager.createNotificationChannels(
                listOf(assistantChannel, reminderChannel, announcementChannel, callChannel)
            )
        }
    }
}
