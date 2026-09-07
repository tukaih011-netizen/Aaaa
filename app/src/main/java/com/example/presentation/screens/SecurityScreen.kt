package com.example.presentation.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.MainViewModel
import com.example.security.PermissionItem
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(viewModel: MainViewModel) {
    val auditList by viewModel.securityAudit.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refreshSecurityAudit()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SECURITY & PERMISSION CENTER", color = CyanPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Auditable Android Permission Boundaries", color = TextMuted, fontSize = 11.sp)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshSecurityAudit() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = CyanSecondary)
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
                .testTag("security_screen")
        ) {
            // Header Security Info Card
            Surface(
                color = SurfaceElevated,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Security",
                        tint = NeonGreen,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "ZERO HIDDEN BACKGROUND ACTIVITY",
                            color = CyanPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "TIME PASS uses strictly public Android APIs. You maintain 100% control over microphone, call, and storage permissions.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(auditList) { item ->
                    PermissionAuditCard(item) {
                        // Open app settings
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionAuditCard(item: PermissionItem, onManage: () -> Unit) {
    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (item.isGranted) CardBorder else LaserCrimson.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = if (item.isGranted) NeonGreen.copy(alpha = 0.2f) else LaserCrimson.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (item.isGranted) "GRANTED" else "OFF / DISABLED",
                        color = if (item.isGranted) NeonGreen else LaserCrimson,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Purpose: ${item.purpose}",
                color = TextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Why needed: ${item.whyNeeded}",
                color = TextMuted,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "If disabled: ${item.ifDisabled}",
                color = if (!item.isGranted) LaserCrimson.copy(alpha = 0.8f) else TextMuted,
                fontSize = 11.sp
            )

            if (!item.isGranted) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onManage,
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Grant in App Settings", color = CyanPrimary, fontSize = 11.sp)
                }
            }
        }
    }
}
