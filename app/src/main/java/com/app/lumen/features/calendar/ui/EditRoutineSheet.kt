package com.app.lumen.features.calendar.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.lumen.R
import com.app.lumen.features.calendar.data.WeeklyRoutineEntity
import com.app.lumen.features.calendar.model.RoutineItemType
import com.app.lumen.features.calendar.model.leadTimeOptions
import com.app.lumen.features.onboarding.ui.components.SheetCapsuleButton
import com.app.lumen.ui.theme.*
import kotlinx.serialization.json.Json
import java.util.Calendar

private val CardBorder = Color.White.copy(alpha = 0.10f)
private val CardBg = Color(0xFF1A1A29)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRoutineSheet(
    routine: WeeklyRoutineEntity,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        selectedDays: Set<Int>,
        hour: Int,
        minute: Int,
        isNotificationEnabled: Boolean,
        isLoggingEnabled: Boolean,
        notificationLeadTimeMinutes: Int
    ) -> Unit
) {
    val json = remember { Json { ignoreUnknownKeys = true } }
    val type = RoutineItemType.fromRawValue(routine.typeRaw)
    val initialDays: Set<Int> = remember {
        try { json.decodeFromString<List<Int>>(routine.selectedDaysJson).toSet() }
        catch (e: Exception) { emptySet() }
    }

    var title by remember { mutableStateOf(routine.title) }
    var selectedDays by remember { mutableStateOf(initialDays) }
    var hour by remember { mutableIntStateOf(routine.hour) }
    var minute by remember { mutableIntStateOf(routine.minute) }
    var isNotificationEnabled by remember { mutableStateOf(routine.isNotificationEnabled) }
    var isLoggingEnabled by remember { mutableStateOf(routine.isLoggingEnabled) }
    var selectedLeadTimeIndex by remember {
        mutableIntStateOf(
            leadTimeOptions.indexOfFirst { it.minutes == routine.notificationLeadTimeMinutes }
                .coerceAtLeast(0)
        )
    }
    var showTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SheetCapsuleButton(text = stringResource(R.string.cancel)) { onDismiss() }
            SheetCapsuleButton(
                text = stringResource(R.string.save),
                isPrimary = true
            ) {
                if (title.isNotBlank()) {
                    onSave(
                        title,
                        selectedDays,
                        hour,
                        minute,
                        isNotificationEnabled,
                        isLoggingEnabled,
                        leadTimeOptions[selectedLeadTimeIndex].minutes
                    )
                }
            }
        }

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            item {
                // Type header
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(type.icon, null, tint = SoftGold, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.routine_edit_title), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Title
            item {
                Text(stringResource(R.string.routine_field_title), color = Slate, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SoftGold,
                        unfocusedBorderColor = CardBorder,
                        cursorColor = SoftGold,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Days
            item {
                Text(stringResource(R.string.routine_field_days), color = Slate, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                DaysPicker(selectedDays = selectedDays, onDaysChange = { selectedDays = it })
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Time
            item {
                Text(stringResource(R.string.routine_field_time), color = Slate, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    onClick = { showTimePicker = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Schedule, null, tint = SoftGold)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(formatTime(hour, minute), color = Color.White, fontSize = 16.sp)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Notification
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Notifications, null, tint = SoftGold)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(stringResource(R.string.routine_notifications), color = Color.White, modifier = Modifier.weight(1f))
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
                            Text(
                                stringResource(R.string.routine_remind_me),
                                color = Slate,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                items(leadTimeOptions.size) { index ->
                                    val option = leadTimeOptions[index]
                                    FilterChip(
                                        selected = selectedLeadTimeIndex == index,
                                        onClick = { selectedLeadTimeIndex = index },
                                        label = { Text(option.label, fontSize = 11.sp) },
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
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Track completion
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.CheckCircle, null, tint = SoftGold)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(R.string.routine_track_completion), color = Color.White, modifier = Modifier.weight(1f))
                        Switch(
                            checked = isLoggingEnabled,
                            onCheckedChange = { isLoggingEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = SoftGold,
                                checkedThumbColor = Color.Black
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = hour,
            initialMinute = minute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    hour = timePickerState.hour
                    minute = timePickerState.minute
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
