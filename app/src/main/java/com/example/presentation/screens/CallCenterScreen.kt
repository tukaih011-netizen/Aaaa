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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PhoneCallStatus
import com.example.presentation.MainViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallCenterScreen(viewModel: MainViewModel) {
    val callCapability by viewModel.callCapability.collectAsState()
    val callInfo by viewModel.callInfo.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("TELECOM CALL CENTER", color = CyanPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("InCallService & Voice Call Control", color = TextMuted, fontSize = 11.sp)
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
                .testTag("call_center_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Telecom Integration Status
            Surface(
                color = SurfaceDark,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PhoneCallback,
                            contentDescription = "Status",
                            tint = if (callCapability.isDefaultDialer) NeonGreen else TechAmber
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TELECOM INTEGRATION CAPABILITY",
                            color = CyanPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = callCapability.statusSummary,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!callCapability.isDefaultDialer) {
                        Button(
                            onClick = {
                                val intent = viewModel.callCapabilityManager.requestDefaultDialerIntent()
                                if (intent != null) {
                                    context.startActivity(intent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Set as Telecom Dialer / Role", color = VoidDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. Interactive Call Simulator
            Surface(
                color = SurfaceDark,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "CALL COMMAND TESTING (BANGLISH)",
                        color = CyanPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Simulate an incoming phone call to test real-time HUD voice commands ('Call ta receive kor', 'Call ta cut kor').",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { viewModel.simulateIncomingCall() },
                            colors = ButtonDefaults.buttonColors(containerColor = TechAmber),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Simulate Incoming Call", color = VoidDark, fontWeight = FontWeight.Bold)
                        }

                        if (callInfo.status != PhoneCallStatus.IDLE) {
                            OutlinedButton(
                                onClick = { viewModel.dismissCall() },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Clear Simulation", color = TextPrimary)
                            }
                        }
                    }
                }
            }

            // 3. Supported Voice Call Commands
            Surface(
                color = SurfaceDark,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SUPPORTED BANGLISH CALL COMMANDS",
                        color = CyanPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("• 'Call ta receive kor' / 'Call pick up kor'", color = TextPrimary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• 'Call ta cut kor' / 'Call end kor' / 'Kete de'", color = TextPrimary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• 'Call ta decline kor' / 'Reject kor'", color = TextPrimary, fontSize = 13.sp)
                }
            }
        }
    }
}
