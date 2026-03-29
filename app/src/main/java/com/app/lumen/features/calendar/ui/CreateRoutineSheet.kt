package com.app.lumen.features.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.app.lumen.R
import com.app.lumen.features.calendar.model.*
import com.app.lumen.features.onboarding.ui.components.SheetCapsuleButton
import com.app.lumen.ui.theme.*
import java.util.Calendar

private val CardBorder = Color.White.copy(alpha = 0.10f)
private val CardBg = Color(0xFF1A1A29)

enum class CreateRoutineStep { TYPE_SELECTION, CONFIGURATION }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoutineSheet(
    preselectedType: RoutineItemType? = null,
    preselectedSuggestion: RoutineSuggestion? = null,
    onDismiss: () -> Unit,
    onCreate: (
        title: String,
        type: RoutineItemType,
        selectedDays: Set<Int>,
        hour: Int,
        minute: Int,
        isNotificationEnabled: Boolean,
        isLoggingEnabled: Boolean,
        notificationLeadTimeMinutes: Int
    ) -> Unit
) {
    var step by remember {
        mutableStateOf(
            if (preselectedType != null || preselectedSuggestion != null) {
                CreateRoutineStep.CONFIGURATION
            } else {
                CreateRoutineStep.TYPE_SELECTION
            }
        )
    }

    var selectedType by remember { mutableStateOf(preselectedType ?: preselectedSuggestion?.type) }
    val context = LocalContext.current
    var title by remember { mutableStateOf(
        if (preselectedSuggestion != null) context.getString(preselectedSuggestion.titleRes)
        else if (preselectedType != null) context.getString(preselectedType.defaultTitleRes)
        else ""
    ) }
    var selectedDays by remember {
        mutableStateOf(
            preselectedSuggestion?.defaultDays ?: setOf(1, 2, 3, 4, 5, 6, 7)
        )
    }

    val defaultHour = preselectedSuggestion?.defaultHour ?: preselectedType?.defaultHour ?: 9
    val defaultMinute = preselectedSuggestion?.defaultMinute ?: preselectedType?.defaultMinute ?: 0

    var hour by remember { mutableIntStateOf(defaultHour) }
    var minute by remember { mutableIntStateOf(defaultMinute) }
    var isNotificationEnabled by remember { mutableStateOf(true) }
    var isLoggingEnabled by remember { mutableStateOf(true) }
    var selectedLeadTimeIndex by remember { mutableIntStateOf(0) }
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
            SheetCapsuleButton(
                text = if (step == CreateRoutineStep.CONFIGURATION && preselectedType == null && preselectedSuggestion == null)
                    stringResource(R.string.routine_back) else stringResource(R.string.cancel)
            ) {
                if (step == CreateRoutineStep.CONFIGURATION && preselectedType == null && preselectedSuggestion == null) {
                    step = CreateRoutineStep.TYPE_SELECTION
                } else {
                    onDismiss()
                }
            }
            if (step == CreateRoutineStep.CONFIGURATION) {
                SheetCapsuleButton(
                    text = stringResource(R.string.save),
                    isPrimary = true
                ) {
                    if (title.isNotBlank() && selectedType != null) {
                        onCreate(
                            title,
                            selectedType!!,
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
        }

        when (step) {
            CreateRoutineStep.TYPE_SELECTION -> {
                TypeSelectionContent(
                    onSelectType = { type ->
                        selectedType = type
                        title = context.getString(type.defaultTitleRes)
                        hour = type.defaultHour
                        minute = type.defaultMinute
                        // Set default days based on type
                        selectedDays = when (type) {
                            RoutineItemType.MASS -> setOf(Calendar.SUNDAY)
                            else -> setOf(1, 2, 3, 4, 5, 6, 7)
                        }
                        step = CreateRoutineStep.CONFIGURATION
                    },
                    onQuickStart = { suggestion ->
                        selectedType = suggestion.type
                        title = context.getString(suggestion.titleRes)
                        selectedDays = suggestion.defaultDays
                        hour = suggestion.defaultHour
                        minute = suggestion.defaultMinute
                        step = CreateRoutineStep.CONFIGURATION
                    }
                )
            }

            CreateRoutineStep.CONFIGURATION -> {
                ConfigurationContent(
                    type = selectedType!!,
                    title = title,
                    onTitleChange = { title = it },
                    selectedDays = selectedDays,
                    onDaysChange = { selectedDays = it },
                    hour = hour,
                    minute = minute,
                    onTimeClick = { showTimePicker = true },
                    isNotificationEnabled = isNotificationEnabled,
                    onNotificationToggle = { isNotificationEnabled = it },
                    isLoggingEnabled = isLoggingEnabled,
                    onLoggingToggle = { isLoggingEnabled = it },
                    selectedLeadTimeIndex = selectedLeadTimeIndex,
                    onLeadTimeChange = { selectedLeadTimeIndex = it }
                )
            }
        }
    }

    // Time picker dialog
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

@Composable
private fun TypeSelectionContent(
    onSelectType: (RoutineItemType) -> Unit,
    onQuickStart: (RoutineSuggestion) -> Unit
) {
    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Icon(Icons.Filled.Autorenew, null, tint = SoftGold, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.routine_new_title), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(stringResource(R.string.routine_new_subtitle), color = Slate, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Type grid - 3 columns
        val types = listOf(
            RoutineItemType.MASS,
            RoutineItemType.MORNING_PRAYER,
            RoutineItemType.EVENING_PRAYER,
            RoutineItemType.ROSARY,
            RoutineItemType.ADORATION,
            RoutineItemType.ANGELUS,
            RoutineItemType.DIVINE_MERCY,
            RoutineItemType.CUSTOM
        )
        val rows = types.chunked(3)

        items(rows) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (type in row) {
                    TypeGridItem(
                        type = type,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectType(type) }
                    )
                }
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Quick Start section
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                stringResource(R.string.routine_quick_start),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        val quickStartSuggestions = RoutineSuggestion.all
        val quickStartRows = quickStartSuggestions.chunked(2)

        items(quickStartRows) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (suggestion in row) {
                    QuickStartCard(
                        suggestion = suggestion,
                        modifier = Modifier.weight(1f),
                        onClick = { onQuickStart(suggestion) }
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun TypeGridItem(
    type: RoutineItemType,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(type.icon, null, tint = SoftGold, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            AutoSizeText(
                text = stringResource(type.displayNameRes),
                color = Color.White,
                maxFontSize = 12.sp,
                minFontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun QuickStartCard(
    suggestion: RoutineSuggestion,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(suggestion.type.icon, null, tint = SoftGold, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(suggestion.titleRes), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(stringResource(suggestion.subtitleRes), color = Slate, fontSize = 12.sp, maxLines = 2)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Schedule, null, tint = Slate, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    formatTime(suggestion.defaultHour, suggestion.defaultMinute),
                    color = Slate,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun ConfigurationContent(
    type: RoutineItemType,
    title: String,
    onTitleChange: (String) -> Unit,
    selectedDays: Set<Int>,
    onDaysChange: (Set<Int>) -> Unit,
    hour: Int,
    minute: Int,
    onTimeClick: () -> Unit,
    isNotificationEnabled: Boolean,
    onNotificationToggle: (Boolean) -> Unit,
    isLoggingEnabled: Boolean,
    onLoggingToggle: (Boolean) -> Unit,
    selectedLeadTimeIndex: Int,
    onLeadTimeChange: (Int) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item {
            // Type header
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(type.icon, null, tint = SoftGold, modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(type.displayNameRes), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Title
        item {
            Text(stringResource(R.string.routine_field_title), color = Slate, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SoftGold,
                    unfocusedBorderColor = CardBorder,
                    cursorColor = SoftGold,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.routine_field_title_placeholder), color = Slate) }
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Days of week
        item {
            Text(stringResource(R.string.routine_field_days), color = Slate, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            DaysPicker(selectedDays = selectedDays, onDaysChange = onDaysChange)
            Spacer(modifier = Modifier.height(8.dp))
            // Quick select buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickDayChip(stringResource(R.string.routine_days_every_day), selectedDays == setOf(1, 2, 3, 4, 5, 6, 7)) {
                    onDaysChange(setOf(1, 2, 3, 4, 5, 6, 7))
                }
                QuickDayChip(stringResource(R.string.routine_days_weekdays), selectedDays == setOf(2, 3, 4, 5, 6)) {
                    onDaysChange(setOf(2, 3, 4, 5, 6))
                }
                QuickDayChip(stringResource(R.string.routine_days_weekends), selectedDays == setOf(1, 7)) {
                    onDaysChange(setOf(1, 7))
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Time
        item {
            Text(stringResource(R.string.routine_field_time), color = Slate, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onTimeClick),
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
                    Icon(Icons.Filled.Schedule, null, tint = SoftGold)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(formatTime(hour, minute), color = Color.White, fontSize = 16.sp)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Notification toggle
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
                            onCheckedChange = onNotificationToggle,
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
                                    onClick = { onLeadTimeChange(index) },
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
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Track completion toggle
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
                        onCheckedChange = onLoggingToggle,
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

@Composable
fun DaysPicker(
    selectedDays: Set<Int>,
    onDaysChange: (Set<Int>) -> Unit
) {
    val dayLabels = listOf(
        Calendar.MONDAY to "M",
        Calendar.TUESDAY to "T",
        Calendar.WEDNESDAY to "W",
        Calendar.THURSDAY to "T",
        Calendar.FRIDAY to "F",
        Calendar.SATURDAY to "S",
        Calendar.SUNDAY to "S"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        for ((day, label) in dayLabels) {
            val isSelected = day in selectedDays
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) SoftGold else Color.Transparent)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) SoftGold else Slate.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
                    .clickable {
                        val newDays = selectedDays.toMutableSet()
                        if (isSelected) newDays.remove(day) else newDays.add(day)
                        if (newDays.isNotEmpty()) onDaysChange(newDays)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (isSelected) Color.Black else Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
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

private fun formatTime(hour: Int, minute: Int): String {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    return java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(cal.time)
}

/**
 * Text composable that auto-shrinks font size to fit within bounds.
 * Starts at [maxFontSize] and shrinks down to [minFontSize] in 0.5sp steps.
 */
@Composable
private fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    maxFontSize: TextUnit = 14.sp,
    minFontSize: TextUnit = 8.sp,
    fontWeight: FontWeight? = null,
    maxLines: Int = 1,
    textAlign: TextAlign = TextAlign.Center
) {
    var fontSize by remember { mutableStateOf(maxFontSize) }
    var readyToDraw by remember { mutableStateOf(false) }

    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
        modifier = modifier.drawWithContent {
            if (readyToDraw) drawContent()
        },
        onTextLayout = { result ->
            if (result.didOverflowWidth && fontSize > minFontSize) {
                fontSize = (fontSize.value - 0.5f).sp
            } else {
                readyToDraw = true
            }
        }
    )
}
