package com.example.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.presentation.screens.*
import com.example.ui.theme.*

data class NavItem(
    val id: String,
    val title: String,
    val icon: ImageVector
)

@Composable
fun TimePassApp(viewModel: MainViewModel) {
    val activeScreen by viewModel.activeScreen.collectAsState()
    val context = LocalContext.current

    val navItems = listOf(
        NavItem("HUD", "HUD Core", Icons.Default.Radar),
        NavItem("CHAT", "Chat", Icons.Default.Chat),
        NavItem("DIAGNOSTICS", "Telemetry", Icons.Default.Analytics),
        NavItem("CALLS", "Calls", Icons.Default.Phone),
        NavItem("MEMORY", "Memory", Icons.Default.Psychology),
        NavItem("REMINDERS", "Reminders", Icons.Default.Alarm),
        NavItem("SECURITY", "Security", Icons.Default.Shield),
        NavItem("SETTINGS", "Settings", Icons.Default.Settings)
    )

    // Dynamic Runtime Permissions Request on First Launch
    val permissionsToRequest = buildList {
        add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
        add(Manifest.permission.READ_PHONE_STATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            add(Manifest.permission.ANSWER_PHONE_CALLS)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        viewModel.refreshSecurityAudit()
    }

    LaunchedEffect(Unit) {
        val ungranted = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (ungranted.isNotEmpty()) {
            permissionLauncher.launch(ungranted.toTypedArray())
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                contentColor = TextPrimary,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .border(androidx.compose.foundation.BorderStroke(1.dp, CardBorder))
                    .testTag("main_bottom_nav")
            ) {
                // Show first 5 primary or scrollable items
                navItems.forEach { item ->
                    val isSelected = activeScreen == item.id
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.setActiveScreen(item.id) },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = if (isSelected) CyanPrimary else TextMuted
                            )
                        },
                        label = {
                            Text(
                                text = item.title,
                                color = if (isSelected) CyanPrimary else TextMuted,
                                fontSize = 9.sp,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyanPrimary,
                            selectedTextColor = CyanPrimary,
                            indicatorColor = SurfaceElevated
                        ),
                        modifier = Modifier.testTag("nav_${item.id.lowercase()}")
                    )
                }
            }
        },
        containerColor = VoidDark
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(VoidDark)
        ) {
            when (activeScreen) {
                "HUD" -> HudScreen(viewModel)
                "CHAT" -> ChatScreen(viewModel)
                "DIAGNOSTICS" -> DeviceDiagnosticsScreen(viewModel)
                "CALLS" -> CallCenterScreen(viewModel)
                "MEMORY" -> MemoryScreen(viewModel)
                "REMINDERS" -> RemindersScreen(viewModel)
                "SECURITY" -> SecurityScreen(viewModel)
                "SETTINGS" -> SettingsScreen(viewModel)
                else -> HudScreen(viewModel)
            }
        }
    }
}
