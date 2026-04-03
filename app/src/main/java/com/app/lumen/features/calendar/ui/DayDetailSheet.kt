package com.app.lumen.features.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.lumen.R
import com.app.lumen.features.calendar.model.*
import com.app.lumen.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DayDetailSheet(
    day: CalendarDayDisplay,
    existingReminders: List<Reminder>,
    existingNotes: List<Note>,
    onSetReminder: (ReminderType) -> Unit,
    onEditReminder: (Reminder) -> Unit,
    onAddNote: (NoteType) -> Unit,
    onEditNote: (Note) -> Unit
) {
    val isFutureDate = remember(day.date) {
        val cal = Calendar.getInstance()
        val today = Calendar.getInstance()
        cal.time = day.date
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        today.set(Calendar.HOUR_OF_DAY, 0); today.set(Calendar.MINUTE, 0); today.set(Calendar.SECOND, 0); today.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis > today.timeInMillis
    }

    val celebrationName = day.liturgicalDay?.localizedCelebrationName() ?: ""
    val isObligatory = day.liturgicalDay?.isHolyDayOfObligation == true
    val dateFormatted = remember(day.date) {
        SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(day.date)
    }
    val liturgicalColor = day.color.color

    val isFirstFriday = remember(day.date) {
        val cal = Calendar.getInstance()
        cal.time = day.date
        val weekday = cal.get(Calendar.DAY_OF_WEEK)
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        weekday == Calendar.FRIDAY && dayOfMonth <= 7
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        // Header: colored dot + celebration name
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(liturgicalColor)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = celebrationName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Subtitle: obligatory badge + date
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isObligatory) {
                Text(text = "\u2605", fontSize = 12.sp, color = SoftGold)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.calendar_day_obligatory),
                    fontSize = 13.sp,
                    color = SoftGold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "\u2022", fontSize = 13.sp, color = Slate)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = dateFormatted, fontSize = 13.sp, color = Slate)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(Color.White.copy(alpha = 0.12f))
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isFutureDate) {
            // FUTURE DATE: Show existing reminders + reminder type picker

            // Existing reminders
            if (existingReminders.isNotEmpty()) {
                SectionLabel(
                    icon = Icons.Filled.Notifications,
                    text = stringResource(R.string.day_detail_existing_reminders)
                )
                Spacer(modifier = Modifier.height(8.dp))
                existingReminders.forEach { reminder ->
                    ExistingReminderRow(
                        reminder = reminder,
                        onTap = { onEditReminder(reminder) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Set reminder section
            SectionLabel(
                icon = Icons.Filled.NotificationAdd,
                text = stringResource(R.string.day_detail_set_reminders)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Contextual types first
            if (isFirstFriday) {
                ActionTypeButton(
                    icon = Icons.Filled.Favorite,
                    title = reminderTypeLabel(ReminderType.FIRST_FRIDAY),
                    description = reminderTypeDescription(ReminderType.FIRST_FRIDAY),
                    onClick = { onSetReminder(ReminderType.FIRST_FRIDAY) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (day.liturgicalDay?.isHolyDayOfObligation == true) {
                ActionTypeButton(
                    icon = Icons.Filled.Star,
                    title = reminderTypeLabel(ReminderType.HOLY_DAY),
                    description = reminderTypeDescription(ReminderType.HOLY_DAY),
                    onClick = { onSetReminder(ReminderType.HOLY_DAY) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Standard types
            val standardTypes = listOf(
                ReminderType.MASS, ReminderType.CONFESSION, ReminderType.FASTING,
                ReminderType.PRAYER, ReminderType.CUSTOM
            )
            standardTypes.forEach { type ->
                ActionTypeButtonContent(
                    iconContent = {
                        ReminderTypeIcon(
                            type = type,
                            tint = SoftGold,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    title = reminderTypeLabel(type),
                    description = reminderTypeDescription(type),
                    onClick = { onSetReminder(type) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        } else {
            // PAST/TODAY: Show existing notes + note type picker

            // Existing notes
            if (existingNotes.isNotEmpty()) {
                SectionLabel(
                    icon = Icons.Filled.Description,
                    text = stringResource(R.string.day_detail_existing_notes)
                )
                Spacer(modifier = Modifier.height(8.dp))
                existingNotes.forEach { note ->
                    ExistingNoteRow(
                        note = note,
                        onTap = { onEditNote(note) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Add note section
            SectionLabel(
                icon = Icons.Filled.Add,
                text = stringResource(R.string.day_detail_add_note)
            )
            Spacer(modifier = Modifier.height(8.dp))

            ActionTypeButton(
                icon = Icons.Filled.AccountBalance,
                title = noteTypeLabel(NoteType.MASS),
                description = stringResource(R.string.note_type_mass_desc),
                onClick = { onAddNote(NoteType.MASS) }
            )
            Spacer(modifier = Modifier.height(8.dp))

            ActionTypeButton(
                icon = Icons.Filled.FavoriteBorder,
                title = noteTypeLabel(NoteType.CONFESSION),
                description = stringResource(R.string.note_type_confession_desc),
                onClick = { onAddNote(NoteType.CONFESSION) }
            )
            Spacer(modifier = Modifier.height(8.dp))

            ActionTypeButton(
                icon = Icons.Filled.Description,
                title = noteTypeLabel(NoteType.CUSTOM),
                description = stringResource(R.string.note_type_custom_desc),
                onClick = { onAddNote(NoteType.CUSTOM) }
            )
        }
    }
}

@Composable
private fun SectionLabel(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SoftGold,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Slate
        )
    }
}

@Composable
private fun ActionTypeButton(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    ActionTypeButtonContent(
        iconContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SoftGold,
                modifier = Modifier.size(24.dp)
            )
        },
        title = title,
        description = description,
        onClick = onClick
    )
}

@Composable
private fun ActionTypeButtonContent(
    iconContent: @Composable () -> Unit,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        iconContent()
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = Slate,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Slate,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun ExistingReminderRow(
    reminder: Reminder,
    onTap: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .clickable(onClick = onTap)
            .padding(12.dp)
    ) {
        ReminderTypeIcon(
            type = reminder.type,
            tint = SoftGold,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = reminder.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = Slate,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = reminder.formattedTime(),
                    fontSize = 12.sp,
                    color = Slate
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Slate,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun ExistingNoteRow(
    note: Note,
    onTap: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .clickable(onClick = onTap)
            .padding(12.dp)
    ) {
        Icon(
            imageVector = noteTypeIcon(note.type),
            contentDescription = null,
            tint = SoftGold,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = note.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!note.content.isNullOrBlank()) {
                Text(
                    text = note.content,
                    fontSize = 12.sp,
                    color = Slate,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            text = noteTypeLabel(note.type),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = Slate,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.1f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Slate,
            modifier = Modifier.size(16.dp)
        )
    }
}
