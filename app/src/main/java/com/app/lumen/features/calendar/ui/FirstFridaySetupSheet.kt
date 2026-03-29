package com.app.lumen.features.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.lumen.features.calendar.model.firstFridayLeadTimeOptions
import com.app.lumen.R
import com.app.lumen.features.onboarding.ui.components.SheetCapsuleButton
import com.app.lumen.ui.theme.*
import java.util.Calendar

private val CardBorder = Color.White.copy(alpha = 0.10f)
private val CardBg = Color(0xFF1A1A29)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirstFridaySetupSheet(
    onDismiss: () -> Unit,
    onCreate: (
        isNotificationEnabled: Boolean,
        notificationHour: Int,
        notificationMinute: Int,
        notificationLeadTimeMinutes: Int,
        initialConsecutiveCount: Int
    ) -> Unit
) {
    var initialCount by remember { mutableIntStateOf(0) }
    var isNotificationEnabled by remember { mutableStateOf(true) }
    var notificationHour by remember { mutableIntStateOf(8) }
    var notificationMinute by remember { mutableIntStateOf(0) }
    var selectedLeadTimeIndex by remember { mutableIntStateOf(2) } // Default: 1 hour before
    var showTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SheetCapsuleButton(text = stringResource(R.string.cancel), onClick = onDismiss)
            SheetCapsuleButton(
                text = stringResource(R.string.save),
                onClick = {
                    onCreate(
                        isNotificationEnabled,
                        notificationHour,
                        notificationMinute,
                        firstFridayLeadTimeOptions[selectedLeadTimeIndex].minutes,
                        initialCount
                    )
                }
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))

                // Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(SoftGold),
                    contentAlignment = Alignment.Center
                ) {
                    Text("1", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    stringResource(R.string.first_friday_title),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.onboarding_first_friday_subtitle),
                    color = Slate,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Initial consecutive count
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.onboarding_first_friday_question),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.onboarding_first_friday_tracking_note),
                            color = Slate,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = { if (initialCount > 0) initialCount-- }
                            ) {
                                Icon(Icons.Filled.Remove, null, tint = SoftGold)
                            }
                            Text(
                                "$initialCount",
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                            IconButton(
                                onClick = { if (initialCount < 100) initialCount++ }
                            ) {
                                Icon(Icons.Filled.Add, null, tint = SoftGold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Notification settings
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Notifications, null, tint = SoftGold)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(stringResource(R.string.first_friday_setup_monthly_reminder), color = Color.White, modifier = Modifier.weight(1f))
                            Switch(
                                checked = isNotificationEnabled,
                                onCheckedChange = { isNotificationEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = SoftGold,
                                    checkedThumbColor = Color.Black
                                )
                            )
                        }

                        if (isNotificationEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))

                            // Time picker
                            Text(stringResource(R.string.first_friday_setup_reminder_time), color = Slate, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = CardBg.copy(alpha = 0.5f)),
                                onClick = { showTimePicker = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Schedule, null, tint = SoftGold, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(formatTime(notificationHour, notificationMinute), color = Color.White, fontSize = 14.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(stringResource(R.string.routine_remind_me), color = Slate, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    if (isNotificationEnabled) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            items(firstFridayLeadTimeOptions.size) { index ->
                                val option = firstFridayLeadTimeOptions[index]
                                FilterChip(
                                    selected = selectedLeadTimeIndex == index,
                                    onClick = { selectedLeadTimeIndex = index },
                                    label = { Text(stringResource(option.labelRes), fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SoftGold.copy(alpha = 0.2f),
                                        selectedLabelColor = SoftGold,
                                        containerColor = Color.Transparent,
                                        labelColor = Slate
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = CardBorder,
                                        selectedBorderColor = SoftGold.copy(alpha = 0.5f),
                                        enabled = true,
                                        selected = selectedLeadTimeIndex == index
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Time picker dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = notificationHour,
            initialMinute = notificationMinute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    notificationHour = timePickerState.hour
                    notificationMinute = timePickerState.minute
                    showTimePicker = false
                }) { Text(stringResource(R.string.done), color = SoftGold) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.cancel), color = Slate) }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }
}

private fun formatTime(hour: Int, minute: Int): String {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    return java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(cal.time)
}
