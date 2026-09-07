package com.example.device

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import com.example.data.model.DeviceTelemetry
import java.io.File
import java.text.DecimalFormat

class BatteryInfoManager(private val context: Context) {
    fun getBatterySnapshot(): BatterySnapshot {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level != -1 && scale != -1) (level * 100 / scale) else 0

        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val tempTenths = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val tempCelsius = tempTenths / 10.0f

        val voltageMv = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val voltage = voltageMv / 1000.0f

        val health = when (intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            else -> "Normal"
        }

        return BatterySnapshot(
            percentage = pct,
            isCharging = isCharging,
            temperatureC = tempCelsius,
            voltage = voltage,
            health = health
        )
    }
}

data class BatterySnapshot(
    val percentage: Int,
    val isCharging: Boolean,
    val temperatureC: Float,
    val voltage: Float,
    val health: String
)

class StorageInfoManager {
    fun getStorageSnapshot(): StorageSnapshot {
        val path: File = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong

        val totalGb = (totalBlocks * blockSize).toDouble() / (1024 * 1024 * 1024)
        val freeGb = (availableBlocks * blockSize).toDouble() / (1024 * 1024 * 1024)
        val usedGb = totalGb - freeGb

        val df = DecimalFormat("#.#")
        return StorageSnapshot(
            totalGb = df.format(totalGb).toFloatOrNull() ?: totalGb.toFloat(),
            freeGb = df.format(freeGb).toFloatOrNull() ?: freeGb.toFloat(),
            usedGb = df.format(usedGb).toFloatOrNull() ?: usedGb.toFloat(),
            usedPct = if (totalGb > 0) ((usedGb / totalGb) * 100).toInt() else 0
        )
    }
}

data class StorageSnapshot(
    val totalGb: Float,
    val freeGb: Float,
    val usedGb: Float,
    val usedPct: Int
)

class RamInfoManager(private val context: Context) {
    fun getRamSnapshot(): RamSnapshot {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)

        val totalGb = memInfo.totalMem.toDouble() / (1024 * 1024 * 1024)
        val availGb = memInfo.availMem.toDouble() / (1024 * 1024 * 1024)
        val usedGb = totalGb - availGb

        val df = DecimalFormat("#.##")
        return RamSnapshot(
            totalGb = df.format(totalGb).toFloatOrNull() ?: totalGb.toFloat(),
            availGb = df.format(availGb).toFloatOrNull() ?: availGb.toFloat(),
            usedGb = df.format(usedGb).toFloatOrNull() ?: usedGb.toFloat(),
            isLowMemory = memInfo.lowMemory
        )
    }
}

data class RamSnapshot(
    val totalGb: Float,
    val availGb: Float,
    val usedGb: Float,
    val isLowMemory: Boolean
)

class NetworkInfoManager(private val context: Context) {
    fun getNetworkSnapshot(): NetworkSnapshot {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNetwork)

        val isOnline = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true

        var wifiSsid = "Disconnected"
        var wifiEnabled = false
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (wm != null) {
            wifiEnabled = wm.isWifiEnabled
            if (isWifi) {
                val info = wm.connectionInfo
                if (info != null && info.ssid != null && info.ssid != "<unknown ssid>") {
                    wifiSsid = info.ssid.replace("\"", "")
                } else {
                    wifiSsid = "Connected"
                }
            }
        }

        val typeName = when {
            isWifi -> "Wi-Fi ($wifiSsid)"
            isCellular -> "Cellular (Mobile Data)"
            isOnline -> "Active Network"
            else -> "Offline"
        }

        return NetworkSnapshot(
            isOnline = isOnline,
            isWifi = isWifi,
            isCellular = isCellular,
            wifiEnabled = wifiEnabled,
            wifiSsid = wifiSsid,
            networkName = typeName
        )
    }
}

data class NetworkSnapshot(
    val isOnline: Boolean,
    val isWifi: Boolean,
    val isCellular: Boolean,
    val wifiEnabled: Boolean,
    val wifiSsid: String,
    val networkName: String
)

class SystemInfoManager(private val context: Context) {
    private val batteryManager = BatteryInfoManager(context)
    private val storageManager = StorageInfoManager()
    private val ramManager = RamInfoManager(context)
    private val networkManager = NetworkInfoManager(context)

    fun getTelemetry(): DeviceTelemetry {
        val battery = batteryManager.getBatterySnapshot()
        val storage = storageManager.getStorageSnapshot()
        val ram = ramManager.getRamSnapshot()
        val network = networkManager.getNetworkSnapshot()

        val uptimeMs = SystemClock.elapsedRealtime()
        val hours = uptimeMs / (1000 * 60 * 60)
        val minutes = (uptimeMs % (1000 * 60 * 60)) / (1000 * 60)
        val uptimeFormatted = "${hours}h ${minutes}m"

        return DeviceTelemetry(
            batteryPct = battery.percentage,
            isCharging = battery.isCharging,
            batteryTemp = battery.temperatureC,
            batteryVoltage = battery.voltage,
            ramTotalGb = ram.totalGb,
            ramUsedGb = ram.usedGb,
            ramAvailGb = ram.availGb,
            storageTotalGb = storage.totalGb,
            storageFreeGb = storage.freeGb,
            wifiEnabled = network.wifiEnabled,
            wifiSsid = network.wifiSsid,
            isOnline = network.isOnline,
            networkType = network.networkName,
            uptimeFormatted = uptimeFormatted,
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
            cpuCores = Runtime.getRuntime().availableProcessors()
        )
    }
}

class FlashlightManager(private val context: Context) {
    private var isTorchOn = false

    fun toggleFlashlight(on: Boolean? = null): Boolean {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return false
        try {
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return false
            val target = on ?: !isTorchOn
            cameraManager.setTorchMode(cameraId, target)
            isTorchOn = target
            return true
        } catch (e: CameraAccessException) {
            return false
        } catch (e: Exception) {
            return false
        }
    }

    fun isFlashlightOn(): Boolean = isTorchOn
}

class AudioSettingsManager(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    fun adjustVolume(increase: Boolean): Boolean {
        audioManager?.let {
            val direction = if (increase) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
            it.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
            return true
        }
        return false
    }

    fun muteVolume(mute: Boolean): Boolean {
        audioManager?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val direction = if (mute) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE
                it.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
            } else {
                it.setStreamMute(AudioManager.STREAM_MUSIC, mute)
            }
            return true
        }
        return false
    }
}
