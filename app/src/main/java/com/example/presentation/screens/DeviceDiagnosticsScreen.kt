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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeviceTelemetry
import com.example.presentation.MainViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDiagnosticsScreen(viewModel: MainViewModel) {
    val telemetry by viewModel.telemetry.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SYSTEM TELEMETRY", color = CyanPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Real-Time Hardware Diagnostics", color = TextMuted, fontSize = 11.sp)
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
                .testTag("diagnostics_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Device Core Info
            DiagnosticSectionCard(title = "CORE HARDWARE & OS") {
                DiagnosticRow("Device Model", telemetry.deviceModel, Icons.Default.PhoneAndroid)
                DiagnosticRow("OS Platform", telemetry.androidVersion, Icons.Default.Android)
                DiagnosticRow("CPU Architecture", "${telemetry.cpuCores} Cores Active", Icons.Default.Speed)
                DiagnosticRow("System Uptime", telemetry.uptimeFormatted, Icons.Default.Timer)
            }

            // 2. Battery & Power Subsystem
            DiagnosticSectionCard(title = "BATTERY & POWER SUBSYSTEM") {
                DiagnosticRow("Battery Level", "${telemetry.batteryPct}%", if (telemetry.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd)
                DiagnosticRow("Charging Status", if (telemetry.isCharging) "Charging via USB/AC" else "Discharging on Battery", Icons.Default.Power)
                DiagnosticRow("Core Temperature", "${telemetry.batteryTemp} °C", Icons.Default.Thermostat)
                DiagnosticRow("Cell Voltage", "${telemetry.batteryVoltage} V", Icons.Default.Bolt)
            }

            // 3. Storage & Memory Telemetry
            DiagnosticSectionCard(title = "MEMORY & STORAGE ALLOCATION") {
                DiagnosticRow("RAM Free", "${telemetry.ramAvailGb} GB / ${telemetry.ramTotalGb} GB Total", Icons.Default.Memory)
                DiagnosticRow("RAM Used", "${telemetry.ramUsedGb} GB", Icons.Default.PieChart)
                DiagnosticRow("Internal Storage Free", "${telemetry.storageFreeGb} GB / ${telemetry.storageTotalGb} GB", Icons.Default.Storage)
            }

            // 4. Network & Radio
            DiagnosticSectionCard(title = "CONNECTIVITY & RADIO SUBSYSTEM") {
                DiagnosticRow("Internet State", if (telemetry.isOnline) "Connected & Validated" else "Disconnected", Icons.Default.Language)
                DiagnosticRow("Active Network", telemetry.networkType, Icons.Default.NetworkCheck)
                DiagnosticRow("Wi-Fi Status", if (telemetry.wifiEnabled) "Radio Active (${telemetry.wifiSsid})" else "Radio Inactive", Icons.Default.Wifi)
            }

            // 5. Hardware Quick Actions
            DiagnosticSectionCard(title = "QUICK HARDWARE ACTIONS") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.flashlightManager.toggleFlashlight() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Toggle Torch", color = CyanPrimary, fontSize = 12.sp)
                    }

                    Button(
                        onClick = { viewModel.sendTextMessage("Volume barhao") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Volume +", color = CyanPrimary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DiagnosticSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
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
fun DiagnosticRow(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = label, tint = CyanSecondary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, color = TextSecondary, fontSize = 13.sp)
        }
        Text(text = value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
