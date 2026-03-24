package com.app.lumen.features.calendar.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesTabView(
    notes: List<Note>,
    reminders: List<Reminder>,
    isLoading: Boolean,
    getLiturgicalDay: (java.util.Date) -> LiturgicalDay?,
    onReminderTap: (Reminder) -> Unit,
    onNoteTap: (Note) -> Unit,
    onDeleteReminder: (Reminder) -> Unit,
    onDeleteNote: (Note) -> Unit,
) {
    var searchText by remember { mutableStateOf("") }
    var isPlansExpanded by remember { mutableStateOf(true) }
    var isNotesExpanded by remember { mutableStateOf(true) }
    var reminderToDelete by remember { mutableStateOf<Reminder?>(null) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }

    val filteredNotes = remember(notes, searchText) {
        if (searchText.isBlank()) notes
        else notes.filter { note ->
            note.title.contains(searchText, ignoreCase = true) ||
                    (note.content?.contains(searchText, ignoreCase = true) == true)
        }
    }

    val groupedNotes = remember(filteredNotes) {
        filteredNotes
            .groupBy { startOfDay(it.date) }
            .toSortedMap(compareByDescending { it })
            .map { (date, notes) -> date to notes.sortedByDescending { it.createdAt } }
    }

    // Delete confirmation dialogs
    reminderToDelete?.let { reminder ->
        AlertDialog(
            onDismissRequest = { reminderToDelete = null },
            title = { Text(stringResource(R.string.reminder_delete_title)) },
            text = { Text(stringResource(R.string.reminder_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteReminder(reminder)
                        reminderToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { reminderToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    noteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text(stringResource(R.string.note_delete_title)) },
            text = { Text(stringResource(R.string.note_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteNote(note)
                        noteToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        if (notes.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        tint = Slate,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchText.isEmpty()) {
                            Text(
                                stringResource(R.string.notes_search_placeholder),
                                color = Slate.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        }
                        BasicTextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 13.sp,
                                color = Color.White
                            ),
                            cursorBrush = SolidColor(SoftGold),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        when {
            isLoading -> NotesLoadingSkeleton()
            reminders.isEmpty() && notes.isEmpty() -> NotesEmptyState()
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    // Future Plans section
                    if (reminders.isNotEmpty() && searchText.isBlank()) {
                        item {
                            CollapsibleSectionHeader(
                                title = stringResource(R.string.notes_section_plans),
                                icon = Icons.Filled.DateRange,
                                count = reminders.size,
                                isExpanded = isPlansExpanded,
                                onToggle = { isPlansExpanded = !isPlansExpanded }
                            )
                        }

                        if (isPlansExpanded) {
                            items(reminders, key = { it.id }) { reminder ->
                                ReminderRow(
                                    reminder = reminder,
                                    onTap = { onReminderTap(reminder) },
                                    onDelete = { reminderToDelete = reminder }
                                )
                            }
                        }
                    }

                    // Notes section
                    if (filteredNotes.isNotEmpty()) {
                        item {
                            CollapsibleSectionHeader(
                                title = stringResource(R.string.notes_section_notes),
                                icon = Icons.Filled.Description,
                                count = filteredNotes.size,
                                isExpanded = isNotesExpanded,
                                onToggle = { isNotesExpanded = !isNotesExpanded }
                            )
                        }

                        if (isNotesExpanded) {
                            groupedNotes.forEach { (date, notesForDate) ->
                                item(key = "header_${date.time}") {
                                    NoteDateHeader(date = date)
                                }
                                items(notesForDate, key = { it.id }) { note ->
                                    NoteRow(
                                        note = note,
                                        liturgicalDay = getLiturgicalDay(note.date),
                                        onTap = { onNoteTap(note) },
                                        onDelete = { noteToDelete = note }
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(120.dp)) }
                }
            }
        }
    }
}

// MARK: - Collapsible Section Header

@Composable
private fun CollapsibleSectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        animationSpec = tween(200),
        label = "chevronRotation"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(NearBlack)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle
            )
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SoftGold,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Slate
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "($count)",
            fontSize = 11.sp,
            color = Slate.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Slate,
            modifier = Modifier
                .size(16.dp)
                .rotate(rotation)
        )
    }
}

// MARK: - Note Date Header

@Composable
private fun NoteDateHeader(date: Date) {
    val cal = Calendar.getInstance()
    val today = Calendar.getInstance()

    val isToday = run {
        cal.time = date
        cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    }

    val isYesterday = run {
        val yesterday = Calendar.getInstance()
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        cal.time = date
        cal.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)
    }

    val headerText = when {
        isToday -> stringResource(R.string.today)
        isYesterday -> stringResource(R.string.notes_yesterday)
        else -> SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(date)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NearBlack.copy(alpha = 0.5f))
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            text = headerText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (isToday) SoftGold else Slate
        )
    }
}

// MARK: - Reminder Row

@Composable
private fun ReminderRow(
    reminder: Reminder,
    onTap: () -> Unit,
    onDelete: () -> Unit
) {
    val cal = Calendar.getInstance()
    val today = Calendar.getInstance()

    val isToday = run {
        cal.time = reminder.date
        cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    }

    val isTomorrow = run {
        val tomorrow = Calendar.getInstance()
        tomorrow.add(Calendar.DAY_OF_YEAR, 1)
        cal.time = reminder.date
        cal.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == tomorrow.get(Calendar.DAY_OF_YEAR)
    }

    val dateLabel = when {
        isToday -> stringResource(R.string.today)
        isTomorrow -> stringResource(R.string.tomorrow)
        else -> SimpleDateFormat("d MMM", Locale.getDefault()).format(reminder.date)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        ReminderTypeIcon(
            type = reminder.type,
            tint = SoftGold,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = reminder.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = dateLabel,
                    fontSize = 12.sp,
                    color = if (isToday) SoftGold else Slate
                )
                Spacer(modifier = Modifier.width(8.dp))
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
                if (!reminder.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Filled.Description,
                        contentDescription = null,
                        tint = Slate.copy(alpha = 0.8f),
                        modifier = Modifier.size(12.dp)
                    )
                }
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

// MARK: - Note Row

@Composable
private fun NoteRow(
    note: Note,
    liturgicalDay: LiturgicalDay? = null,
    onTap: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Icon(
            imageVector = noteTypeIcon(note.type),
            contentDescription = null,
            tint = SoftGold,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = note.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (!note.content.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = note.content,
                    fontSize = 12.sp,
                    color = Slate,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Type badge
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

                // Liturgical season badge
                if (liturgicalDay != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(liturgicalDay.color.color)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = liturgicalDay.season.rawValue,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate
                        )
                    }
                }
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

// MARK: - Empty State

@Composable
private fun NotesEmptyState() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 50.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Description,
                contentDescription = null,
                tint = Slate,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.notes_empty_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.notes_empty_message),
                fontSize = 14.sp,
                color = Slate,
                modifier = Modifier.padding(horizontal = 40.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

// MARK: - Loading Skeleton

@Composable
private fun NotesLoadingSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        repeat(5) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Box(
                        modifier = Modifier
                            .width(140.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                    )
                }
            }
        }
    }
}

// MARK: - Helpers

private fun startOfDay(date: Date): Date {
    val cal = Calendar.getInstance()
    cal.time = date
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.time
}
