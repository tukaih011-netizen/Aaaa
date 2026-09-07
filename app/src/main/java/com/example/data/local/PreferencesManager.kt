package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "time_pass_preferences")

class PreferencesManager(private val context: Context) {

    companion object {
        val ASSISTANT_NAME = stringPreferencesKey("assistant_name")
        val USER_CALLSIGN = stringPreferencesKey("user_callsign")
        val VOICE_SPEED = floatPreferencesKey("voice_speed")
        val VOICE_PITCH = floatPreferencesKey("voice_pitch")
        val VOICE_VOLUME = floatPreferencesKey("voice_volume")
        val WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        val CONTINUOUS_CONVO = booleanPreferencesKey("continuous_convo")
        val BACKGROUND_MODE = booleanPreferencesKey("background_mode")
        val STARTUP_SOUND_ENABLED = booleanPreferencesKey("startup_sound_enabled")
        val SHUTDOWN_SOUND_ENABLED = booleanPreferencesKey("shutdown_sound_enabled")
        val SOUND_FX_ENABLED = booleanPreferencesKey("sound_fx_enabled")
        val ANNOUNCE_MORNING = booleanPreferencesKey("announce_morning")
        val ANNOUNCE_BATTERY = booleanPreferencesKey("announce_battery")
        val ANNOUNCE_REMINDERS = booleanPreferencesKey("announce_reminders")
        val ANNOUNCE_NOTIFICATIONS = booleanPreferencesKey("announce_notifications")
        val REDUCE_ANIMATIONS = booleanPreferencesKey("reduce_animations")
        val DEFAULT_UI_MODE = stringPreferencesKey("default_ui_mode") // "HUD" or "CHAT"
        val MEMORY_ENABLED = booleanPreferencesKey("memory_enabled")
    }

    val assistantName: Flow<String> = context.dataStore.data.map { it[ASSISTANT_NAME] ?: "TIME PASS" }
    val userCallsign: Flow<String> = context.dataStore.data.map { it[USER_CALLSIGN] ?: "bro" }
    val voiceSpeed: Flow<Float> = context.dataStore.data.map { it[VOICE_SPEED] ?: 1.0f }
    val voicePitch: Flow<Float> = context.dataStore.data.map { it[VOICE_PITCH] ?: 0.92f } // Natural masculine resonance
    val voiceVolume: Flow<Float> = context.dataStore.data.map { it[VOICE_VOLUME] ?: 1.0f }
    val wakeWordEnabled: Flow<Boolean> = context.dataStore.data.map { it[WAKE_WORD_ENABLED] ?: true }
    val continuousConvo: Flow<Boolean> = context.dataStore.data.map { it[CONTINUOUS_CONVO] ?: false }
    val backgroundMode: Flow<Boolean> = context.dataStore.data.map { it[BACKGROUND_MODE] ?: true }
    val startupSoundEnabled: Flow<Boolean> = context.dataStore.data.map { it[STARTUP_SOUND_ENABLED] ?: true }
    val shutdownSoundEnabled: Flow<Boolean> = context.dataStore.data.map { it[SHUTDOWN_SOUND_ENABLED] ?: true }
    val soundFxEnabled: Flow<Boolean> = context.dataStore.data.map { it[SOUND_FX_ENABLED] ?: true }
    val announceMorning: Flow<Boolean> = context.dataStore.data.map { it[ANNOUNCE_MORNING] ?: true }
    val announceBattery: Flow<Boolean> = context.dataStore.data.map { it[ANNOUNCE_BATTERY] ?: true }
    val announceReminders: Flow<Boolean> = context.dataStore.data.map { it[ANNOUNCE_REMINDERS] ?: true }
    val announceNotifications: Flow<Boolean> = context.dataStore.data.map { it[ANNOUNCE_NOTIFICATIONS] ?: false }
    val reduceAnimations: Flow<Boolean> = context.dataStore.data.map { it[REDUCE_ANIMATIONS] ?: false }
    val defaultUiMode: Flow<String> = context.dataStore.data.map { it[DEFAULT_UI_MODE] ?: "HUD" }
    val memoryEnabled: Flow<Boolean> = context.dataStore.data.map { it[MEMORY_ENABLED] ?: true }

    suspend fun setAssistantName(name: String) = context.dataStore.edit { it[ASSISTANT_NAME] = name }
    suspend fun setUserCallsign(callsign: String) = context.dataStore.edit { it[USER_CALLSIGN] = callsign }
    suspend fun setVoiceSpeed(speed: Float) = context.dataStore.edit { it[VOICE_SPEED] = speed }
    suspend fun setVoicePitch(pitch: Float) = context.dataStore.edit { it[VOICE_PITCH] = pitch }
    suspend fun setVoiceVolume(volume: Float) = context.dataStore.edit { it[VOICE_VOLUME] = volume }
    suspend fun setWakeWordEnabled(enabled: Boolean) = context.dataStore.edit { it[WAKE_WORD_ENABLED] = enabled }
    suspend fun setContinuousConvo(enabled: Boolean) = context.dataStore.edit { it[CONTINUOUS_CONVO] = enabled }
    suspend fun setBackgroundMode(enabled: Boolean) = context.dataStore.edit { it[BACKGROUND_MODE] = enabled }
    suspend fun setStartupSoundEnabled(enabled: Boolean) = context.dataStore.edit { it[STARTUP_SOUND_ENABLED] = enabled }
    suspend fun setShutdownSoundEnabled(enabled: Boolean) = context.dataStore.edit { it[SHUTDOWN_SOUND_ENABLED] = enabled }
    suspend fun setSoundFxEnabled(enabled: Boolean) = context.dataStore.edit { it[SOUND_FX_ENABLED] = enabled }
    suspend fun setAnnounceMorning(enabled: Boolean) = context.dataStore.edit { it[ANNOUNCE_MORNING] = enabled }
    suspend fun setAnnounceBattery(enabled: Boolean) = context.dataStore.edit { it[ANNOUNCE_BATTERY] = enabled }
    suspend fun setAnnounceReminders(enabled: Boolean) = context.dataStore.edit { it[ANNOUNCE_REMINDERS] = enabled }
    suspend fun setAnnounceNotifications(enabled: Boolean) = context.dataStore.edit { it[ANNOUNCE_NOTIFICATIONS] = enabled }
    suspend fun setReduceAnimations(reduce: Boolean) = context.dataStore.edit { it[REDUCE_ANIMATIONS] = reduce }
    suspend fun setDefaultUiMode(mode: String) = context.dataStore.edit { it[DEFAULT_UI_MODE] = mode }
    suspend fun setMemoryEnabled(enabled: Boolean) = context.dataStore.edit { it[MEMORY_ENABLED] = enabled }
}
