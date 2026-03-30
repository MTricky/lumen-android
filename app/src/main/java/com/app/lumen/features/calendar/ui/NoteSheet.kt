package com.app.lumen.features.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FavoriteBorder
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
import com.app.lumen.features.calendar.model.Note
import com.app.lumen.features.calendar.model.NoteType
import com.app.lumen.services.AnalyticsEvent
import com.app.lumen.services.AnalyticsManager
import com.app.lumen.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteSheet(
    date: Date,
    noteType: NoteType,
    existingNote: Note?,
    onSave: (NoteType, Date, String, String?) -> Unit,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val isEditing = existingNote != null

    var selectedType by remember { mutableStateOf(existingNote?.type ?: noteType) }
    var selectedDate by remember { mutableStateOf(existingNote?.date ?: date) }
    var title by remember { mutableStateOf(existingNote?.title ?: "") }
    var content by remember { mutableStateOf(existingNote?.content ?: "") }
    var isSaving by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.note_delete_title)) },
            text = { Text(stringResource(R.string.note_delete_message)) },
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

    val canSave = title.isNotBlank() && !isSaving

    val formattedDate = remember(selectedDate) {
        SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(selectedDate)
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
            Icon(
                imageVector = noteTypeIcon(selectedType),
                contentDescription = null,
                tint = SoftGold,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isEditing) stringResource(R.string.note_edit_title) else stringResource(R.string.note_add_title),
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

        // Type Selector
        Text(
            text = stringResource(R.string.note_field_type),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Slate,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            NoteType.entries.forEach { type ->
                NoteTypeButton(
                    type = type,
                    isSelected = selectedType == type,
                    onClick = { selectedType = type },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Date picker
        Text(
            text = stringResource(R.string.note_field_date),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Slate,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        var showDatePicker by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .clickable { showDatePicker = true }
                .padding(12.dp)
        ) {
            Text(
                text = SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(selectedDate),
                fontSize = 15.sp,
                color = Color.White
            )
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = selectedDate.time,
                selectableDates = object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        val todayEnd = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                        }.timeInMillis
                        return utcTimeMillis <= todayEnd
                    }
                }
            )

            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDate = Date(it) }
                        showDatePicker = false
                    }) { Text(stringResource(R.string.done)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Title input
        Text(
            text = stringResource(R.string.note_field_title),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Slate,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = {
                Text(
                    text = titlePlaceholder(selectedType),
                    color = Slate.copy(alpha = 0.6f)
                )
            },
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

        Spacer(modifier = Modifier.height(20.dp))

        // Content input
        Text(
            text = stringResource(R.string.note_field_content),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Slate,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            placeholder = {
                Text(
                    text = contentPlaceholder(selectedType),
                    color = Slate.copy(alpha = 0.6f)
                )
            },
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
            minLines = 4,
            maxLines = 10,
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
                    // Track note created (matching iOS)
                    AnalyticsManager.trackEvent(
                        AnalyticsEvent.NOTE_CREATED,
                        mapOf("note_type" to selectedType.rawValue)
                    )
                    onSave(selectedType, selectedDate, title, content.ifBlank { null })
                },
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SoftGold,
                    contentColor = NearBlack
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(R.string.save),
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
                Text(stringResource(R.string.note_delete_action), fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun NoteTypeButton(
    type: NoteType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) SoftGold else Color.White.copy(alpha = 0.1f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.dp)
    ) {
        Icon(
            imageVector = noteTypeIcon(type),
            contentDescription = null,
            tint = if (isSelected) NearBlack else SoftGold,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = noteTypeLabel(type),
            fontSize = 11.sp,
            color = if (isSelected) NearBlack else Color.White
        )
    }
}

internal fun noteTypeIcon(type: NoteType): ImageVector = when (type) {
    NoteType.MASS -> Icons.Filled.AccountBalance
    NoteType.CONFESSION -> Icons.Filled.FavoriteBorder
    NoteType.CUSTOM -> Icons.Filled.Description
}

@Composable
internal fun noteTypeLabel(type: NoteType): String = when (type) {
    NoteType.MASS -> stringResource(R.string.note_type_mass)
    NoteType.CONFESSION -> stringResource(R.string.note_type_confession)
    NoteType.CUSTOM -> stringResource(R.string.note_type_custom)
}

@Composable
private fun titlePlaceholder(type: NoteType): String = when (type) {
    NoteType.MASS -> stringResource(R.string.note_placeholder_mass_title)
    NoteType.CONFESSION -> stringResource(R.string.note_placeholder_confession_title)
    NoteType.CUSTOM -> stringResource(R.string.note_placeholder_custom_title)
}

@Composable
private fun contentPlaceholder(type: NoteType): String = when (type) {
    NoteType.MASS -> stringResource(R.string.note_placeholder_mass_content)
    NoteType.CONFESSION -> stringResource(R.string.note_placeholder_confession_content)
    NoteType.CUSTOM -> stringResource(R.string.note_placeholder_custom_content)
}
