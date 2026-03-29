package com.app.lumen.features.onboarding.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.lumen.R
import com.app.lumen.ui.HapticManager
import com.app.lumen.features.calendar.model.RoutineItemType
import com.app.lumen.features.calendar.model.leadTimeOptions
import com.app.lumen.features.calendar.ui.DaysPicker
import com.app.lumen.features.onboarding.OnboardingRoutineSelection
import com.app.lumen.ui.theme.SoftGold
import com.app.lumen.ui.theme.Slate
import java.util.Calendar

private val CardBorder = Color.White.copy(alpha = 0.10f)
private val CardBg = Color(0xFF1A1A29)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingRoutineEditSheet(
    routine: OnboardingRoutineSelection,
    onDismiss: () -> Unit,
    onSave: (OnboardingRoutineSelection) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val view = LocalView.current
    val type = routine.type

    var selectedDays by remember { mutableStateOf(routine.selectedDays) }
    var hour by remember { mutableIntStateOf(routine.selectedHour) }
    var minute by remember { mutableIntStateOf(routine.selectedMinute) }
    var isNotificationEnabled by remember { mutableStateOf(routine.isNotificationEnabled) }
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
            SheetCapsuleButton(text = stringResource(R.string.save), isPrimary = true) {
                HapticManager.lightImpact(view)
                onSave(
                    routine.copy(
                        selectedDays = selectedDays,
                        selectedHour = hour,
                        selectedMinute = minute,
                        isNotificationEnabled = isNotificationEnabled,
                        notificationLeadTimeMinutes = leadTimeOptions[selectedLeadTimeIndex].minutes
                    )
                )
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
                    Text(
                        stringResource(R.string.onboarding_routine_edit_title),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        routine.title,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Days
            item {
                Text(stringResource(R.string.onboarding_routine_edit_days), color = Slate, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                DaysPicker(selectedDays = selectedDays, onDaysChange = { HapticManager.softImpact(view); selectedDays = it })

                Spacer(modifier = Modifier.height(8.dp))
                // Quick select buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val allDays = (Calendar.SUNDAY..Calendar.SATURDAY).toSet()
                    val weekdays = setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)
                    val weekends = setOf(Calendar.SATURDAY, Calendar.SUNDAY)

                    QuickDayChip(stringResource(R.string.onboarding_routine_edit_every_day), selectedDays == allDays) { HapticManager.softImpact(view); selectedDays = allDays }
                    QuickDayChip(stringResource(R.string.onboarding_routine_edit_weekdays), selectedDays == weekdays) { HapticManager.softImpact(view); selectedDays = weekdays }
                    QuickDayChip(stringResource(R.string.onboarding_routine_edit_weekends), selectedDays == weekends) { HapticManager.softImpact(view); selectedDays = weekends }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Time
            item {
                Text(stringResource(R.string.onboarding_routine_edit_time), color = Slate, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = BorderStroke(1.dp, CardBorder),
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
                    border = BorderStroke(1.dp, CardBorder)
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
                            Text(stringResource(R.string.onboarding_routine_edit_notifications), color = Color.White, modifier = Modifier.weight(1f))
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
                                stringResource(R.string.onboarding_routine_edit_remind_me),
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
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Delete button (for custom routines)
            if (onDelete != null) {
                item {
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            null,
                            tint = Color(0xFFEF5350),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.onboarding_routine_edit_delete), color = Color(0xFFEF5350))
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
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
                }) { Text(stringResource(R.string.onboarding_routine_edit_done), color = SoftGold) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.cancel), color = Slate) }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }
}

@Composable
private fun QuickDayChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp) },
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
            selected = isSelected
        )
    )
}

@Composable
internal fun SheetCapsuleButton(
    text: String,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isPrimary) SoftGold.copy(alpha = 0.15f)
                else Color.White.copy(alpha = 0.08f)
            )
            .border(
                1.dp,
                if (isPrimary) SoftGold.copy(alpha = 0.3f)
                else Color.White.copy(alpha = 0.12f),
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (isPrimary) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isPrimary) SoftGold else Color.White.copy(alpha = 0.8f)
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
