package com.example.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.MainViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val prefs = viewModel.prefsManager
    val scope = rememberCoroutineScope()

    val assistantName by prefs.assistantName.collectAsState(initial = "TIME PASS")
    val userCallsign by prefs.userCallsign.collectAsState(initial = "bro")
    val voicePitch by prefs.voicePitch.collectAsState(initial = 0.92f)
    val voiceSpeed by prefs.voiceSpeed.collectAsState(initial = 1.0f)
    val wakeWordEnabled by prefs.wakeWordEnabled.collectAsState(initial = true)
    val startupSound by prefs.startupSoundEnabled.collectAsState(initial = true)
    val soundFx by prefs.soundFxEnabled.collectAsState(initial = true)
    val announceMorning by prefs.announceMorning.collectAsState(initial = true)
    val announceBattery by prefs.announceBattery.collectAsState(initial = true)
    val defaultUiMode by prefs.defaultUiMode.collectAsState(initial = "HUD")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SYSTEM PREFERENCES", color = CyanPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Voice Calibration & Assistant Behavior", color = TextMuted, fontSize = 11.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = VoidDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .testTag("settings_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Assistant Identity
            SettingsGroupCard(title = "ASSISTANT IDENTITY & CALLSIGN") {
                OutlinedTextField(
                    value = assistantName,
                    onValueChange = { scope.launch { prefs.setAssistantName(it) } },
                    label = { Text("Assistant Core Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = userCallsign,
                    onValueChange = { scope.launch { prefs.setUserCallsign(it) } },
                    label = { Text("User Callsign / Nickname") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 2. Male Voice Calibration
            SettingsGroupCard(title = "MALE VOICE CALIBRATION (TTS)") {
                Text(
                    text = "Pitch Tuning (Masculine Resonance): ${(voicePitch * 100).toInt()}%",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Slider(
                    value = voicePitch,
                    onValueChange = { scope.launch { prefs.setVoicePitch(it) } },
                    valueRange = 0.7f..1.3f,
                    colors = SliderDefaults.colors(thumbColor = CyanPrimary, activeTrackColor = CyanPrimary)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Speech Rate: ${(voiceSpeed * 100).toInt()}%",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Slider(
                    value = voiceSpeed,
                    onValueChange = { scope.launch { prefs.setVoiceSpeed(it) } },
                    valueRange = 0.8f..1.5f,
                    colors = SliderDefaults.colors(thumbColor = CyanPrimary, activeTrackColor = CyanPrimary)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = { viewModel.ttsManager?.speak("Hello bro, TIME PASS AI Core voice test complete.") },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated)
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "Test", tint = CyanPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test Male Voice Tone", color = CyanPrimary)
                }
            }

            // 3. Audio & Futuristic Sound Design
            SettingsGroupCard(title = "SOUND DESIGN & SYNTHESIZER") {
                SettingsSwitchRow(
                    title = "Futuristic Sound Effects",
                    subtitle = "Real-time synthetic tones for wake, execute, success & errors",
                    checked = soundFx,
                    onCheckedChange = { scope.launch { prefs.setSoundFxEnabled(it) } }
                )

                SettingsSwitchRow(
                    title = "Startup Arc Reactor Boot Chime",
                    subtitle = "Play original ascending power chime on start",
                    checked = startupSound,
                    onCheckedChange = { scope.launch { prefs.setStartupSoundEnabled(it) } }
                )
            }

            // 4. Wake Word & Assistant Features
            SettingsGroupCard(title = "WAKE WORD & PROACTIVE ANNOUNCEMENTS") {
                SettingsSwitchRow(
                    title = "Wake Word Detection ('Time Pass' / 'Bro')",
                    subtitle = "Listen for hands-free wake word triggers",
                    checked = wakeWordEnabled,
                    onCheckedChange = { scope.launch { prefs.setWakeWordEnabled(it) } }
                )

                SettingsSwitchRow(
                    title = "Morning Briefing Announcement",
                    subtitle = "Proactive greeting and daily status",
                    checked = announceMorning,
                    onCheckedChange = { scope.launch { prefs.setAnnounceMorning(it) } }
                )

                SettingsSwitchRow(
                    title = "Low Battery Voice Warning",
                    subtitle = "Alert when battery drops below 15%",
                    checked = announceBattery,
                    onCheckedChange = { scope.launch { prefs.setAnnounceBattery(it) } }
                )
            }

            // 5. Default Interface Mode
            SettingsGroupCard(title = "INTERFACE PREFERENCE") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = defaultUiMode == "HUD",
                        onClick = { scope.launch { prefs.setDefaultUiMode("HUD") } },
                        label = { Text("Fullscreen HUD (Sci-Fi)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = defaultUiMode == "CHAT",
                        onClick = { scope.launch { prefs.setDefaultUiMode("CHAT") } },
                        label = { Text("Chat Stream") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsGroupCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = CyanPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = TextMuted, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CyanPrimary,
                checkedTrackColor = SurfaceElevated,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = SurfaceDark
            )
        )
    }
}
