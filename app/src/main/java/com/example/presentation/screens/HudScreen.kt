package com.example.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AssistantState
import com.example.data.model.CallInfo
import com.example.data.model.DeviceTelemetry
import com.example.data.model.PhoneCallStatus
import com.example.hud.AudioWaveform
import com.example.hud.JarvisCore
import com.example.presentation.MainViewModel
import com.example.ui.theme.*

@Composable
fun HudScreen(viewModel: MainViewModel) {
    val state by viewModel.assistantState.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val audioRms by viewModel.audioRms.collectAsState()
    val callInfo by viewModel.callInfo.collectAsState()
    val lastResult by viewModel.lastExecutionResult.collectAsState()

    val quickCommands = listOf(
        "Battery koto?",
        "Storage kemon?",
        "WiFi settings kholo",
        "Flashlight on kor",
        "Chrome kholo",
        "Ajker weather check kor",
        "Bro 10 min por remind korish",
        "Simulate Call"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidDark)
            .testTag("hud_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top HUD Header
            HudTopBar(state, telemetry)

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Telemetry Quick Bar
            TelemetryQuickGrid(telemetry)

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Central AI Core Arc Reactor
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .testTag("jarvis_core_container"),
                contentAlignment = Alignment.Center
            ) {
                JarvisCore(
                    state = state,
                    audioRms = audioRms,
                    modifier = Modifier.fillMaxSize()
                )

                // Central State Label
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "TIME PASS",
                        color = CyanPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = state.name,
                        color = when (state) {
                            AssistantState.THINKING -> TechAmber
                            AssistantState.EXECUTING, AssistantState.SUCCESS -> NeonGreen
                            AssistantState.ERROR -> LaserCrimson
                            else -> TextPrimary
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Banglish Status & Spoken feedback banner
            StatusFeedbackCard(state, lastResult?.displayText)

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Live Audio Waveform
            AudioWaveform(
                isSpeaking = state == AssistantState.SPEAKING,
                isListening = state == AssistantState.LISTENING,
                audioRms = audioRms
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 6. Voice Action Controller Button
            VoiceActionBar(
                state = state,
                onStartListening = { viewModel.startListening() },
                onStopListening = { viewModel.stopListening() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 7. Instant Banglish Command Chips
            Text(
                text = "INSTANT BANGLISH COMMANDS",
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickCommands) { cmd ->
                    QuickCommandChip(cmd) {
                        if (cmd == "Simulate Call") {
                            viewModel.simulateIncomingCall()
                        } else {
                            viewModel.sendTextMessage(cmd)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Active In-Call Floating HUD Overlay
        if (callInfo.status != PhoneCallStatus.IDLE) {
            InCallHudOverlay(
                callInfo = callInfo,
                onAnswer = { viewModel.answerActiveCall() },
                onEnd = { viewModel.endActiveCall() },
                onDismiss = { viewModel.dismissCall() }
            )
        }
    }
}

@Composable
fun HudTopBar(state: AssistantState, telemetry: DeviceTelemetry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Brand & Status
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (telemetry.isOnline) NeonGreen else TechAmber)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = "TIME PASS AI HUD",
                    color = CyanPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = telemetry.deviceModel,
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
        }

        // Uptime & Status Chip
        Surface(
            color = SurfaceElevated,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = "Uptime",
                    tint = CyanSecondary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = telemetry.uptimeFormatted,
                    color = TextPrimary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun TelemetryQuickGrid(telemetry: DeviceTelemetry) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TelemetryCard(
            title = "BATTERY",
            value = "${telemetry.batteryPct}%",
            subValue = if (telemetry.isCharging) "Charging" else "${telemetry.batteryTemp}°C",
            icon = if (telemetry.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
            color = if (telemetry.batteryPct < 20) LaserCrimson else CyanPrimary,
            modifier = Modifier.weight(1f)
        )
        TelemetryCard(
            title = "STORAGE",
            value = "${telemetry.storageFreeGb}GB",
            subValue = "Free Space",
            icon = Icons.Default.Storage,
            color = CyanSecondary,
            modifier = Modifier.weight(1f)
        )
        TelemetryCard(
            title = "RAM",
            value = "${telemetry.ramAvailGb}GB",
            subValue = "Available",
            icon = Icons.Default.Memory,
            color = NeonGreen,
            modifier = Modifier.weight(1f)
        )
        TelemetryCard(
            title = "NETWORK",
            value = if (telemetry.isOnline) "ONLINE" else "OFFLINE",
            subValue = telemetry.wifiSsid.take(8),
            icon = Icons.Default.Wifi,
            color = if (telemetry.isOnline) NeonGreen else TechAmber,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun TelemetryCard(
    title: String,
    value: String,
    subValue: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = SurfaceDark,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = TextMuted,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subValue,
                color = TextSecondary,
                fontSize = 9.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun StatusFeedbackCard(state: AssistantState, lastDisplayText: String?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceElevated,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = state.BanglishStatus,
                color = CyanPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            if (!lastDisplayText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = lastDisplayText,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 3
                )
            }
        }
    }
}

@Composable
fun VoiceActionBar(
    state: AssistantState,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit
) {
    val isListening = state == AssistantState.LISTENING

    Button(
        onClick = {
            if (isListening) onStopListening() else onStartListening()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("voice_action_button"),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isListening) LaserCrimson else CyanPrimary,
            contentColor = VoidDark
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = if (isListening) "Stop Listening" else "Start Voice Command",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isListening) "TAP TO STOP LISTENING" else "HOLD / TAP TO TALK (BANGLISH)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun QuickCommandChip(text: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .testTag("chip_$text"),
        color = SurfaceElevated,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Text(
            text = text,
            color = TextPrimary,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun InCallHudOverlay(
    callInfo: CallInfo,
    onAnswer: () -> Unit,
    onEnd: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidDark.copy(alpha = 0.92f))
            .padding(24.dp)
            .testTag("incall_hud_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SurfaceElevated,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, CyanPrimary)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "TELECOM IN-CALL HUD",
                    color = CyanPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Icon(
                    imageVector = Icons.Default.PhoneInTalk,
                    contentDescription = "Active Call",
                    tint = if (callInfo.status == PhoneCallStatus.RINGING) TechAmber else NeonGreen,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = callInfo.callerName ?: "Incoming Call",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (callInfo.status == PhoneCallStatus.RINGING) "INCOMING CALL RINGING..." else "CALL IN PROGRESS",
                    color = if (callInfo.status == PhoneCallStatus.RINGING) TechAmber else NeonGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (callInfo.status == PhoneCallStatus.RINGING) {
                        Button(
                            onClick = onAnswer,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                            shape = CircleShape,
                            modifier = Modifier.size(60.dp)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Answer", tint = VoidDark)
                        }
                    }

                    Button(
                        onClick = onEnd,
                        colors = ButtonDefaults.buttonColors(containerColor = LaserCrimson),
                        shape = CircleShape,
                        modifier = Modifier.size(60.dp)
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = "End Call", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onDismiss) {
                    Text("Dismiss HUD Overlay", color = TextMuted)
                }
            }
        }
    }
}
