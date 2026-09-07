package com.example.notifications

import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.TimePassApplication
import com.example.data.local.AppDatabase
import com.example.data.local.ReminderEntity
import com.example.sound.SoundEffect
import com.example.sound.SoundFxManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleReminder(reminder: ReminderEntity) {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = "com.example.timepass.ACTION_REMINDER"
            putExtra("EXTRA_REMINDER_ID", reminder.id)
            putExtra("EXTRA_REMINDER_TITLE", reminder.title)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminder.triggerTimeMillis,
                    pendingIntent
                )
            } catch (e: SecurityException) {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    reminder.triggerTimeMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                reminder.triggerTimeMillis,
                pendingIntent
            )
        }
    }

    fun cancelReminder(reminderId: Long) {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = "com.example.timepass.ACTION_REMINDER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("EXTRA_REMINDER_TITLE") ?: "Scheduled reminder"
        val reminderId = intent.getLongExtra("EXTRA_REMINDER_ID", 0L)

        // Mark as completed in database
        val db = AppDatabase.getDatabase(context)
        CoroutineScope(Dispatchers.IO).launch {
            if (reminderId > 0) {
                db.reminderDao().markCompleted(reminderId)
            }
        }

        // Play sci-fi reminder chime
        SoundFxManager().playSound(SoundEffect.NOTIFICATION)

        // Build notification
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAV_DEST", "reminders")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, TimePassApplication.CHANNEL_REMINDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("TIME PASS • Reminder Alert")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(reminderId.toInt().coerceAtLeast(1001), notification)
        } catch (e: SecurityException) {
            // Notification permission might be revoked
        }
    }
}

class AssistantNotificationManager(private val context: Context) {

    fun buildForegroundNotification(statusText: String = "AI CORE ONLINE • Monitoring"): Notification {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, TimePassApplication.CHANNEL_ASSISTANT)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("TIME PASS Assistant")
            .setContentText(statusText)
            .setContentIntent(pendingOpen)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}

class TimePassNotificationListener : NotificationListenerService() {
    companion object {
        private val _latestNotificationText = MutableStateFlow<String?>(null)
        val latestNotificationText: StateFlow<String?> = _latestNotificationText.asStateFlow()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null || sbn.packageName == packageName) return
        val extras = sbn.notification.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        if (!title.isNullOrBlank() || !text.isNullOrBlank()) {
            _latestNotificationText.value = "${title ?: "App"}: ${text ?: ""}"
        }
    }
}
