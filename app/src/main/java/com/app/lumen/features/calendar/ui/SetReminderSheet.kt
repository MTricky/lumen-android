package com.app.lumen.features.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.lumen.R
import com.app.lumen.features.calendar.model.LiturgicalDay
import com.app.lumen.features.calendar.model.Reminder
import com.app.lumen.features.calendar.model.ReminderType
import com.app.lumen.services.AnalyticsEvent
import com.app.lumen.services.AnalyticsManager
import com.app.lumen.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetReminderSheet(
    date: Date,
    reminderType: ReminderType,
    liturgicalDay: LiturgicalDay?,
    existingReminder: Reminder?,
    initialTriggerTime: Date,
    onSave: (String, String?, Date, String?) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isEditing = existingReminder != null

    var title by remember {
        mutableStateOf(
            existingReminder?.title ?: reminderType.defaultTitle(context, liturgicalDay)
        )
    }
    var message by remember {
        mutableStateOf(
            existingReminder?.message ?: reminderType.defaultMessage(context)
        )
    }
    var notes by remember { mutableStateOf(existingReminder?.notes ?: "") }
    var isSaving by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Time picker state
    var selectedHour by remember {
        val cal = Calendar.getInstance()
        cal.time = existingReminder?.triggerTime ?: initialTriggerTime
        mutableIntStateOf(cal.get(Calendar.HOUR_OF_DAY))
    }
    var selectedMinute by remember {
        val cal = Calendar.getInstance()
        cal.time = existingReminder?.triggerTime ?: initialTriggerTime
        mutableIntStateOf(cal.get(Calendar.MINUTE))
    }
    var showTimePicker by remember { mutableStateOf(false) }

    val formattedDate = remember(date) {
        SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(date)
    }

    val formattedTime = remember(selectedHour, selectedMinute) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, selectedHour)
        cal.set(Calendar.MINUTE, selectedMinute)
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(cal.time)
    }

    if (showDeleteConfirmation && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.reminder_delete_title)) },
            text = { Text(stringResource(R.string.reminder_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedHour,
            initialMinute = selectedMinute
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedHour = timePickerState.hour
                    selectedMinute = timePickerState.minute
                    showTimePicker = false
                }) { Text(stringResource(R.string.done)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 40.dp)
    ) {
        // Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 20.dp)
        ) {
            ReminderTypeIcon(
                type = reminderType,
                tint = SoftGold,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = reminderTypeLabel(reminderType),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formattedDate,
                fontSize = 14.sp,
                color = Slate
            )
        }

        // Title input
        Text(
            text = stringResource(R.string.reminder_field_title),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Slate,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = { Text(stringResource(R.string.reminder_title_placeholder), color = Slate.copy(alpha = 0.6f)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SoftGold,
                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                cursorColor = SoftGold,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(10.dp),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Message input
        Text(
            text = stringResource(R.string.reminder_field_message),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Slate,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = message ?: "",
            onValueChange = { message = it },
            placeholder = { Text(stringResource(R.string.reminder_msg_default), color = Slate.copy(alpha = 0.6f)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SoftGold,
                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                cursorColor = SoftGold,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(10.dp),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Time picker
        Text(
            text = stringResource(R.string.reminder_field_time),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Slate,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .clickable { showTimePicker = true }
                .padding(horizontal = 12.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = SoftGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = formattedTime,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Notes input
        Text(
            text = stringResource(R.string.reminder_field_notes),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Slate,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            placeholder = { Text(stringResource(R.string.reminder_notes_placeholder), color = Slate.copy(alpha = 0.6f)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SoftGold,
                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                cursorColor = SoftGold,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(10.dp),
            minLines = 3,
            maxLines = 6,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.cancel))
            }

            Button(
                onClick = {
                    isSaving = true
                    // Track reminder created (matching iOS)
                    if (!isEditing) {
                        AnalyticsManager.trackEvent(
                            AnalyticsEvent.REMINDER_CREATED,
                            mapOf("reminder_type" to reminderType.rawValue)
                        )
                    }
                    val cal = Calendar.getInstance()
                    val dateCal = Calendar.getInstance().apply { time = date }
                    cal.set(Calendar.YEAR, dateCal.get(Calendar.YEAR))
                    cal.set(Calendar.MONTH, dateCal.get(Calendar.MONTH))
                    cal.set(Calendar.DAY_OF_MONTH, dateCal.get(Calendar.DAY_OF_MONTH))
                    cal.set(Calendar.HOUR_OF_DAY, selectedHour)
                    cal.set(Calendar.MINUTE, selectedMinute)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    onSave(
                        title,
                        message?.ifBlank { null },
                        cal.time,
                        notes.ifBlank { null }
                    )
                },
                enabled = title.isNotBlank() && !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SoftGold,
                    contentColor = NearBlack
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (isEditing) stringResource(R.string.save) else stringResource(R.string.set),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Delete button (editing only)
        if (isEditing && onDelete != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(Color.White.copy(alpha = 0.12f))
            )
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                onClick = { showDeleteConfirmation = true },
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Red),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.reminder_delete_action), fontWeight = FontWeight.Medium)
            }
        }
    }
}

internal fun reminderTypeIcon(type: ReminderType): ImageVector = when (type) {
    ReminderType.MASS -> Icons.Filled.AccountBalance
    ReminderType.CONFESSION -> Icons.Filled.FavoriteBorder
    ReminderType.HOLY_DAY -> Icons.Filled.Star
    ReminderType.FIRST_FRIDAY -> Icons.Filled.Favorite
    ReminderType.FASTING -> Icons.Filled.Eco
    ReminderType.PRAYER -> Icons.Filled.Notifications // fallback, overridden by composable
    ReminderType.CUSTOM -> Icons.Filled.Notifications
}

/**
 * Renders the correct icon for a reminder type.
 * Uses a custom drawable for PRAYER (praying hands) and Material icons for all others.
 */
@Composable
internal fun ReminderTypeIcon(
    type: ReminderType,
    tint: Color,
    modifier: Modifier = Modifier
) {
    if (type == ReminderType.PRAYER) {
        Icon(
            painter = painterResource(R.drawable.ic_prayer_hands),
            contentDescription = null,
            tint = tint,
            modifier = modifier
        )
    } else {
        Icon(
            imageVector = reminderTypeIcon(type),
            contentDescription = null,
            tint = tint,
            modifier = modifier
        )
    }
}

@Composable
internal fun reminderTypeLabel(type: ReminderType): String = when (type) {
    ReminderType.MASS -> stringResource(R.string.reminder_type_mass)
    ReminderType.CONFESSION -> stringResource(R.string.reminder_type_confession)
    ReminderType.HOLY_DAY -> stringResource(R.string.reminder_type_holy_day)
    ReminderType.FIRST_FRIDAY -> stringResource(R.string.reminder_type_first_friday)
    ReminderType.FASTING -> stringResource(R.string.reminder_type_fasting)
    ReminderType.PRAYER -> stringResource(R.string.reminder_type_prayer)
    ReminderType.CUSTOM -> stringResource(R.string.reminder_type_custom)
}

@Composable
internal fun reminderTypeDescription(type: ReminderType): String = when (type) {
    ReminderType.MASS -> stringResource(R.string.reminder_desc_mass)
    ReminderType.CONFESSION -> stringResource(R.string.reminder_desc_confession)
    ReminderType.HOLY_DAY -> stringResource(R.string.reminder_desc_holy_day)
    ReminderType.FIRST_FRIDAY -> stringResource(R.string.reminder_desc_first_friday)
    ReminderType.FASTING -> stringResource(R.string.reminder_desc_fasting)
    ReminderType.PRAYER -> stringResource(R.string.reminder_desc_prayer)
    ReminderType.CUSTOM -> stringResource(R.string.reminder_desc_custom)
}
