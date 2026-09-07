package com.example.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

enum class SoundEffect {
    BOOT,
    SHUTDOWN,
    WAKE,
    LISTEN,
    THINK,
    SUCCESS,
    ERROR,
    CALL_RING,
    NOTIFICATION
}

class SoundFxManager {

    private val sampleRate = 44100
    private val scope = CoroutineScope(Dispatchers.Default)

    fun playSound(effect: SoundEffect, enabled: Boolean = true) {
        if (!enabled) return
        scope.launch {
            try {
                val samples = when (effect) {
                    SoundEffect.BOOT -> generateBootTone()
                    SoundEffect.SHUTDOWN -> generateShutdownTone()
                    SoundEffect.WAKE -> generateWakeChirp()
                    SoundEffect.LISTEN -> generateListenBeep()
                    SoundEffect.THINK -> generateThinkClick()
                    SoundEffect.SUCCESS -> generateSuccessChime()
                    SoundEffect.ERROR -> generateErrorTone()
                    SoundEffect.CALL_RING -> generateCallRing()
                    SoundEffect.NOTIFICATION -> generateNotificationChirp()
                }
                playPcm(samples)
            } catch (e: Exception) {
                // Ignore audio hardware error gracefully
            }
        }
    }

    private fun playPcm(samples: ShortArray) {
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(samples.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(samples, 0, samples.size)
        audioTrack.play()
    }

    // Original futuristic boot: ascending power hum into dual high-tech cyber resonance
    private fun generateBootTone(): ShortArray {
        val durationMs = 450
        val numSamples = (sampleRate * durationMs / 1000)
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val freq = 220.0 + (progress * 660.0) // 220Hz -> 880Hz
            val envelope = if (progress < 0.2) progress / 0.2 else (1.0 - progress)
            val wave = sin(2.0 * Math.PI * freq * t) + 0.3 * sin(4.0 * Math.PI * freq * t)
            buffer[i] = (wave * envelope * 0.4 * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    // Original futuristic shutdown: descending resonant frequency power-down
    private fun generateShutdownTone(): ShortArray {
        val durationMs = 400
        val numSamples = (sampleRate * durationMs / 1000)
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val freq = 600.0 - (progress * 420.0) // 600Hz -> 180Hz
            val envelope = (1.0 - progress)
            val wave = sin(2.0 * Math.PI * freq * t) + 0.2 * sin(3.0 * Math.PI * freq * t)
            buffer[i] = (wave * envelope * 0.35 * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    // Original wake chirp: quick double sci-fi harmonic blip
    private fun generateWakeChirp(): ShortArray {
        val durationMs = 180
        val numSamples = (sampleRate * durationMs / 1000)
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val freq = if (progress < 0.5) 880.0 else 1320.0
            val subProgress = if (progress < 0.5) progress * 2.0 else (progress - 0.5) * 2.0
            val env = sin(subProgress * Math.PI)
            val wave = sin(2.0 * Math.PI * freq * t)
            buffer[i] = (wave * env * 0.35 * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    // Original listening start tone
    private fun generateListenBeep(): ShortArray {
        val durationMs = 100
        val numSamples = (sampleRate * durationMs / 1000)
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val freq = 980.0
            val env = sin(progress * Math.PI)
            val wave = sin(2.0 * Math.PI * freq * t)
            buffer[i] = (wave * env * 0.25 * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    // Original subtle scanner click
    private fun generateThinkClick(): ShortArray {
        val durationMs = 80
        val numSamples = (sampleRate * durationMs / 1000)
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val freq = 1400.0 + sin(progress * Math.PI * 4.0) * 200.0
            val env = (1.0 - progress)
            val wave = sin(2.0 * Math.PI * freq * t)
            buffer[i] = (wave * env * 0.2 * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    // Original success: crystal tech dual ascending note
    private fun generateSuccessChime(): ShortArray {
        val durationMs = 280
        val numSamples = (sampleRate * durationMs / 1000)
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val freq = if (progress < 0.45) 784.0 else 1174.6 // G5 to D6
            val subProg = if (progress < 0.45) progress / 0.45 else (progress - 0.45) / 0.55
            val env = (1.0 - subProg)
            val wave = sin(2.0 * Math.PI * freq * t) + 0.25 * sin(4.0 * Math.PI * freq * t)
            buffer[i] = (wave * env * 0.35 * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    // Original error: dual low discord blip
    private fun generateErrorTone(): ShortArray {
        val durationMs = 250
        val numSamples = (sampleRate * durationMs / 1000)
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val freq = 240.0
            val env = (1.0 - progress)
            val wave = sin(2.0 * Math.PI * freq * t) + 0.4 * sin(2.0 * Math.PI * (freq + 35.0) * t)
            buffer[i] = (wave * env * 0.35 * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    // Original incoming sci-fi call warble
    private fun generateCallRing(): ShortArray {
        val durationMs = 600
        val numSamples = (sampleRate * durationMs / 1000)
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val warble = sin(2.0 * Math.PI * 18.0 * t) * 120.0
            val freq = 850.0 + warble
            val env = sin(progress * Math.PI)
            val wave = sin(2.0 * Math.PI * freq * t)
            buffer[i] = (wave * env * 0.4 * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    // Original notification pulse
    private fun generateNotificationChirp(): ShortArray {
        val durationMs = 200
        val numSamples = (sampleRate * durationMs / 1000)
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val freq = 1046.5 // C6
            val env = (1.0 - progress) * (1.0 - progress)
            val wave = sin(2.0 * Math.PI * freq * t)
            buffer[i] = (wave * env * 0.3 * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }
}
