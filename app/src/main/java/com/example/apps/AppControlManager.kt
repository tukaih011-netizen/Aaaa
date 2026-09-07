package com.example.apps

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings

data class AppItem(
    val appName: String,
    val packageName: String,
    val isSystemApp: Boolean
)

class AppControlManager(private val context: Context) {

    fun getInstalledApps(): List<AppItem> {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        return apps.mapNotNull { appInfo ->
            val label = pm.getApplicationLabel(appInfo).toString()
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            if (pm.getLaunchIntentForPackage(appInfo.packageName) != null) {
                AppItem(
                    appName = label,
                    packageName = appInfo.packageName,
                    isSystemApp = isSystem
                )
            } else null
        }.sortedBy { it.appName.lowercase() }
    }

    fun launchAppByName(query: String): LaunchResult {
        val q = query.lowercase().trim()
        val pm = context.packageManager

        // Special predefined aliases
        when {
            q.contains("camera") || q.contains("chobi") -> {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (intent.resolveActivity(pm) != null) {
                    context.startActivity(intent)
                    return LaunchResult.Success("Camera")
                }
            }
            q.contains("chrome") || q.contains("browser") || q.contains("google") -> {
                val chromeIntent = pm.getLaunchIntentForPackage("com.android.chrome")
                if (chromeIntent != null) {
                    chromeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chromeIntent)
                    return LaunchResult.Success("Chrome")
                }
            }
            q.contains("youtube") -> {
                val ytIntent = pm.getLaunchIntentForPackage("com.google.android.youtube")
                if (ytIntent != null) {
                    ytIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(ytIntent)
                    return LaunchResult.Success("YouTube")
                }
            }
            q.contains("whatsapp") -> {
                val waIntent = pm.getLaunchIntentForPackage("com.whatsapp")
                if (waIntent != null) {
                    waIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(waIntent)
                    return LaunchResult.Success("WhatsApp")
                }
            }
            q.contains("setting") -> {
                val settingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
                return LaunchResult.Success("Settings")
            }
            q.contains("wifi") -> {
                val wifiIntent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(wifiIntent)
                return LaunchResult.Success("Wi-Fi Settings")
            }
            q.contains("bluetooth") -> {
                val btIntent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(btIntent)
                return LaunchResult.Success("Bluetooth Settings")
            }
            q.contains("alarm") || q.contains("ghori") -> {
                val alarmIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (alarmIntent.resolveActivity(pm) != null) {
                    context.startActivity(alarmIntent)
                    return LaunchResult.Success("Clock & Alarms")
                }
            }
        }

        // Fuzzy search through installed packages
        val installed = getInstalledApps()
        val match = installed.firstOrNull {
            it.appName.lowercase().contains(q) || q.contains(it.appName.lowercase())
        }

        return if (match != null) {
            val launchIntent = pm.getLaunchIntentForPackage(match.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                LaunchResult.Success(match.appName)
            } else {
                LaunchResult.Failed("Could not find launch intent for ${match.appName}")
            }
        } else {
            LaunchResult.NotFound(query)
        }
    }

    fun openSystemSettings(action: String): Boolean {
        return try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openWebUrl(url: String): Boolean {
        return try {
            val formatted = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else url
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formatted)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}

sealed class LaunchResult {
    data class Success(val appName: String) : LaunchResult()
    data class NotFound(val query: String) : LaunchResult()
    data class Failed(val reason: String) : LaunchResult()
}
