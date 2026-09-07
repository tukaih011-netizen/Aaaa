package com.example.presentation.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ReminderEntity
import com.example.presentation.MainViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(viewModel: MainViewModel) {
    val reminders by viewModel.reminders.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var titleText by remember { mutableStateOf("") }
    var delayMin by remember { mutableStateOf(10) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SCHEDULED REMINDERS", color = CyanPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("AlarmManager & Voice Notification Engine", color = TextMuted, fontSize = 11.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = CyanPrimary,
                contentColor = VoidDark,
                modifier = Modifier.testTag("add_reminder_fab")
            ) {
                Icon(Icons.Default.AddAlarm, contentDescription = "Add Reminder")
            }
        },
        containerColor = VoidDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .testTag("reminders_screen")
        ) {
            if (reminders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = "Empty",
                            tint = CyanPrimary.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Active Reminders",
                            color = CyanPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Say: 'Bro 15 min por remind korish medicine nite'",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(reminders) { rem ->
                        ReminderItemCard(rem) {
                            viewModel.deleteReminder(rem.id)
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Schedule Reminder", color = CyanPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = titleText,
                            onValueChange = { titleText = it },
                            label = { Text("Reminder Task (e.g. Drink Water)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Schedule Delay (Minutes): $delayMin min", color = TextSecondary, fontSize = 13.sp)
                        Slider(
                            value = delayMin.toFloat(),
                            onValueChange = { delayMin = it.toInt() },
                            valueRange = 1f..120f,
                            steps = 119,
                            colors = SliderDefaults.colors(
                                thumbColor = CyanPrimary,
                                activeTrackColor = CyanPrimary
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (titleText.isNotBlank()) {
                                viewModel.addReminder(titleText, delayMin)
                                titleText = ""
                                showAddDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                    ) {
                        Text("Schedule", color = VoidDark, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancel", color = TextMuted)
                    }
                },
                containerColor = SurfaceDark
            )
        }
    }
}

@Composable
fun ReminderItemCard(reminder: ReminderEntity, onDelete: () -> Unit) {
    val dueStr = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(reminder.triggerTimeMillis))
    val isPast = reminder.triggerTimeMillis <= System.currentTimeMillis() || reminder.isCompleted

    Surface(
        color = SurfaceElevated,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isPast) TextMuted.copy(alpha = 0.3f) else CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isPast) Icons.Default.CheckCircle else Icons.Default.AccessAlarm,
                        contentDescription = "Status",
                        tint = if (isPast) NeonGreen else TechAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPast) "COMPLETED" else "SCHEDULED",
                        color = if (isPast) NeonGreen else TechAmber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = reminder.title,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Alarm set for: $dueStr",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = LaserCrimson.copy(alpha = 0.8f))
            }
        }
    }
}
