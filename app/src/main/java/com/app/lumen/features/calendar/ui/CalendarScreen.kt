package com.app.lumen.features.calendar.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import com.app.lumen.R
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.lumen.features.calendar.model.*
import com.app.lumen.features.calendar.viewmodel.CalendarViewModel
import com.app.lumen.ui.components.GlassButtonSize
import com.app.lumen.ui.components.GlassIconButton
import com.app.lumen.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Tab modes for the segmented picker
enum class CalendarTabMode(@androidx.annotation.StringRes val labelRes: Int) {
    CALENDAR(R.string.calendar_tab_calendar),
    ROUTINE(R.string.calendar_tab_routine),
    NOTES(R.string.calendar_tab_notes)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    calendarViewModel: CalendarViewModel = viewModel()
) {
    val viewModel = calendarViewModel
    val monthsData by viewModel.monthsData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val region by viewModel.region.collectAsState()

    val listState = rememberLazyListState()

    // Track visible month title - initialize with current month to avoid flash
    var visibleMonthTitle by remember {
        val formatter = SimpleDateFormat("LLLL yyyy", Locale.getDefault())
        mutableStateOf(formatter.format(Date()).replaceFirstChar { it.uppercase() })
    }

    // Segmented picker state
    var selectedTab by remember { mutableStateOf(CalendarTabMode.CALENDAR) }
    var previousTabOrdinal by remember { mutableIntStateOf(0) }

    // Notes & Reminders state
    val allNotes by viewModel.allNotes.collectAsState()
    val upcomingReminders by viewModel.upcomingReminders.collectAsState()
    val notesLoading by viewModel.notesLoading.collectAsState()
    val dayNotes by viewModel.dayNotes.collectAsState()
    val dayReminders by viewModel.dayReminders.collectAsState()

    // Bottom sheet state
    var selectedDay by remember { mutableStateOf<CalendarDayDisplay?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Info sheet state
    var showInfoSheet by remember { mutableStateOf(false) }
    val infoSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Note/Reminder sub-sheets
    var showNoteSheet by remember { mutableStateOf(false) }
    var noteSheetDate by remember { mutableStateOf(Date()) }
    var noteSheetType by remember { mutableStateOf(NoteType.CUSTOM) }
    var editingNote by remember { mutableStateOf<Note?>(null) }

    var showReminderSheet by remember { mutableStateOf(false) }
    var reminderSheetDate by remember { mutableStateOf(Date()) }
    var reminderSheetType by remember { mutableStateOf(ReminderType.CUSTOM) }
    var editingReminder by remember { mutableStateOf<Reminder?>(null) }
    var reminderSheetLiturgicalDay by remember { mutableStateOf<LiturgicalDay?>(null) }

    // Re-read region from SharedPreferences each time screen appears
    LaunchedEffect(Unit) {
        viewModel.refreshRegionFromPrefs()
    }

    // Reload notes data when switching to the Notes tab
    LaunchedEffect(selectedTab) {
        if (selectedTab == CalendarTabMode.NOTES) {
            viewModel.loadNotesData()
        }
    }

    // Always scroll to current month when data is ready
    var hasScrolledToToday by remember { mutableStateOf(false) }

    LaunchedEffect(monthsData, isLoading) {
        if (!isLoading && monthsData.isNotEmpty() && !hasScrolledToToday) {
            val todayIndex = viewModel.todayMonthIndex
            if (todayIndex in monthsData.indices) {
                listState.scrollToItem(todayIndex)
                visibleMonthTitle = monthsData[todayIndex].monthYearTitle
            }
            hasScrolledToToday = true
        }
    }

    // Update visible month title based on scroll position
    LaunchedEffect(listState.firstVisibleItemIndex) {
        if (monthsData.isNotEmpty()) {
            val index = listState.firstVisibleItemIndex
            if (index in monthsData.indices) {
                val newTitle = monthsData[index].monthYearTitle
                if (newTitle != visibleMonthTitle) {
                    visibleMonthTitle = newTitle
                }
            }
        }
    }

    // Info sheet — full screen with top spacing to mimic iOS
    if (showInfoSheet) {
        ModalBottomSheet(
            onDismissRequest = { showInfoSheet = false },
            sheetState = infoSheetState,
            containerColor = NearBlack,
            sheetMaxWidth = Dp.Unspecified,
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 40.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.3f))
                )
            },
            contentWindowInsets = { WindowInsets(0) },
        ) {
            CalendarInfoSheet(
                regionName = region.displayName,
                onDismiss = { showInfoSheet = false }
            )
        }
    }

    // Load day data when a day is selected
    LaunchedEffect(selectedDay) {
        selectedDay?.let { viewModel.loadDayData(it.date) }
    }

    // Day detail bottom sheet
    if (selectedDay != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedDay = null },
            sheetState = sheetState,
            containerColor = CardBackground,
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 24.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.3f))
                )
            },
        ) {
            selectedDay?.let { day ->
                DayDetailSheet(
                    day = day,
                    existingReminders = dayReminders,
                    existingNotes = dayNotes,
                    onSetReminder = { type ->
                        reminderSheetDate = day.date
                        reminderSheetType = type
                        reminderSheetLiturgicalDay = day.liturgicalDay
                        editingReminder = null
                        showReminderSheet = true
                    },
                    onEditReminder = { reminder ->
                        reminderSheetDate = reminder.date
                        reminderSheetType = reminder.type
                        reminderSheetLiturgicalDay = day.liturgicalDay
                        editingReminder = reminder
                        showReminderSheet = true
                    },
                    onAddNote = { type ->
                        noteSheetDate = day.date
                        noteSheetType = type
                        editingNote = null
                        showNoteSheet = true
                    },
                    onEditNote = { note ->
                        noteSheetDate = note.date
                        noteSheetType = note.type
                        editingNote = note
                        showNoteSheet = true
                    }
                )
            }
        }
    }

    // Note sheet
    if (showNoteSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showNoteSheet = false
                editingNote = null
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = CardBackground,
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 24.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.3f))
                )
            },
        ) {
            NoteSheet(
                date = noteSheetDate,
                noteType = noteSheetType,
                existingNote = editingNote,
                onSave = { type, date, title, content ->
                    if (editingNote != null) {
                        viewModel.updateNote(editingNote!!.id, title, content)
                    } else {
                        viewModel.createNote(type, date, title, content)
                    }
                    showNoteSheet = false
                    editingNote = null
                },
                onDismiss = {
                    showNoteSheet = false
                    editingNote = null
                }
            )
        }
    }

    // Reminder sheet
    if (showReminderSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showReminderSheet = false
                editingReminder = null
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = CardBackground,
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 24.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.3f))
                )
            },
        ) {
            SetReminderSheet(
                date = reminderSheetDate,
                reminderType = reminderSheetType,
                liturgicalDay = reminderSheetLiturgicalDay,
                existingReminder = editingReminder,
                initialTriggerTime = viewModel.reminderPreferences.triggerTime(reminderSheetDate, reminderSheetType),
                onSave = { title, message, triggerTime, notes ->
                    if (editingReminder != null) {
                        viewModel.updateReminder(editingReminder!!.id, title, message, triggerTime, notes)
                    } else {
                        viewModel.createReminder(reminderSheetDate, reminderSheetType, title, message, triggerTime, notes)
                    }
                    showReminderSheet = false
                    editingReminder = null
                },
                onDelete = if (editingReminder != null) {
                    {
                        viewModel.deleteReminder(editingReminder!!)
                        showReminderSheet = false
                        editingReminder = null
                    }
                } else null,
                onDismiss = {
                    showReminderSheet = false
                    editingReminder = null
                }
            )
        }
    }

    var showAddMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NearBlack)
    ) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Toolbar with title and context-dependent button
        CalendarToolbar(
            selectedTab = selectedTab,
            onInfoTapped = { showInfoSheet = true },
            onPlusTapped = { showAddMenu = true }
        )

        // Segmented picker
        CalendarSegmentedPicker(
            selectedTab = selectedTab,
            onTabSelected = { newTab ->
                previousTabOrdinal = selectedTab.ordinal
                selectedTab = newTab
            }
        )

        // Animated content switching for inner tabs
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                val goingRight = targetState.ordinal > previousTabOrdinal
                val enter = slideInHorizontally(
                    initialOffsetX = { fullWidth -> if (goingRight) fullWidth else -fullWidth },
                    animationSpec = tween(300)
                ) + fadeIn(tween(300))
                val exit = slideOutHorizontally(
                    targetOffsetX = { fullWidth -> if (goingRight) -fullWidth else fullWidth },
                    animationSpec = tween(300)
                ) + fadeOut(tween(300))
                enter togetherWith exit
            },
            label = "calendarTabContent"
        ) { tab ->
            when (tab) {
                CalendarTabMode.CALENDAR -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        CalendarStickyHeader(
                            monthTitle = visibleMonthTitle,
                            regionName = region.displayName
                        )

                        val calendarAlpha by animateFloatAsState(
                            targetValue = if (hasScrolledToToday) 1f else 0f,
                            animationSpec = tween(durationMillis = 500),
                            label = "calendarAlpha"
                        )

                        Box(modifier = Modifier.fillMaxSize()) {
                            if (calendarAlpha < 1f) {
                                CalendarSkeletonView(
                                    modifier = Modifier.graphicsLayer { alpha = 1f - calendarAlpha }
                                )
                            }

                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { alpha = calendarAlpha }
                            ) {
                                itemsIndexed(
                                    items = monthsData,
                                    key = { _, month -> month.id }
                                ) { _, monthData ->
                                    MonthContentView(
                                        monthData = monthData,
                                        onDayTapped = { day -> selectedDay = day }
                                    )
                                }

                                item {
                                    Spacer(modifier = Modifier.height(120.dp))
                                }
                            }
                        }
                    }
                }
                CalendarTabMode.ROUTINE -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = stringResource(R.string.coming_soon), color = Slate, fontSize = 16.sp)
                    }
                }
                CalendarTabMode.NOTES -> {
                    NotesTabView(
                        notes = allNotes,
                        reminders = upcomingReminders,
                        isLoading = notesLoading,
                        getLiturgicalDay = { date -> viewModel.getLiturgicalDay(date) },
                        onReminderTap = { reminder ->
                            reminderSheetDate = reminder.date
                            reminderSheetType = reminder.type
                            reminderSheetLiturgicalDay = null
                            editingReminder = reminder
                            showReminderSheet = true
                        },
                        onNoteTap = { note ->
                            noteSheetDate = note.date
                            noteSheetType = note.type
                            editingNote = note
                            showNoteSheet = true
                        },
                        onDeleteReminder = { viewModel.deleteReminder(it) },
                        onDeleteNote = { viewModel.deleteNote(it) }
                    )
                }
            }
        }
    } // end Column

    // Add menu overlay (scrim + panel, must be sibling to Column inside the top-level Box)
    AnimatedVisibility(
        visible = showAddMenu,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(150)),
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.2f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { showAddMenu = false },
                ),
        )
    }

    AnimatedVisibility(
        visible = showAddMenu,
        enter = fadeIn(tween(150)) + scaleIn(
            initialScale = 0.4f,
            transformOrigin = TransformOrigin(0.9f, 0f),
            animationSpec = tween(150),
        ),
        exit = fadeOut(tween(100)) + scaleOut(
            targetScale = 0.4f,
            transformOrigin = TransformOrigin(0.9f, 0f),
            animationSpec = tween(100),
        ),
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 52.dp, end = 16.dp)
                    .width(220.dp)
                    .shadow(16.dp, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .background(MenuPanelBg)
                    .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
            ) {
                // Section header: Add Note
                Text(
                    text = stringResource(R.string.menu_add_note).uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                )

                AddMenuItem(
                    icon = { Icon(noteTypeIcon(NoteType.MASS), null, tint = SoftGold, modifier = Modifier.size(18.dp)) },
                    label = noteTypeLabel(NoteType.MASS),
                    onClick = {
                        showAddMenu = false
                        noteSheetDate = Date()
                        noteSheetType = NoteType.MASS
                        editingNote = null
                        showNoteSheet = true
                    }
                )
                AddMenuDivider()
                AddMenuItem(
                    icon = { Icon(noteTypeIcon(NoteType.CONFESSION), null, tint = SoftGold, modifier = Modifier.size(18.dp)) },
                    label = noteTypeLabel(NoteType.CONFESSION),
                    onClick = {
                        showAddMenu = false
                        noteSheetDate = Date()
                        noteSheetType = NoteType.CONFESSION
                        editingNote = null
                        showNoteSheet = true
                    }
                )
                AddMenuDivider()
                AddMenuItem(
                    icon = { Icon(noteTypeIcon(NoteType.CUSTOM), null, tint = SoftGold, modifier = Modifier.size(18.dp)) },
                    label = noteTypeLabel(NoteType.CUSTOM),
                    onClick = {
                        showAddMenu = false
                        noteSheetDate = Date()
                        noteSheetType = NoteType.CUSTOM
                        editingNote = null
                        showNoteSheet = true
                    }
                )

                // Section divider
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Section header: Add Reminder
                Text(
                    text = stringResource(R.string.menu_add_reminder).uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp)
                )

                val reminderTypes = listOf(
                    ReminderType.MASS, ReminderType.CONFESSION, ReminderType.FASTING,
                    ReminderType.PRAYER, ReminderType.FIRST_FRIDAY, ReminderType.CUSTOM
                )
                reminderTypes.forEachIndexed { index, type ->
                    AddMenuItem(
                        icon = { ReminderTypeIcon(type = type, tint = SoftGold, modifier = Modifier.size(18.dp)) },
                        label = reminderTypeLabel(type),
                        onClick = {
                            showAddMenu = false
                            reminderSheetDate = Date()
                            reminderSheetType = type
                            reminderSheetLiturgicalDay = null
                            editingReminder = null
                            showReminderSheet = true
                        }
                    )
                    if (index < reminderTypes.size - 1) {
                        AddMenuDivider()
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }

    } // end outer Box
}

// MARK: - Toolbar

private val GlassBg = Color(0xFF191927)
private val MenuPanelBg = Color(0xFF23233D)

@Composable
private fun CalendarToolbar(
    selectedTab: CalendarTabMode,
    onInfoTapped: () -> Unit,
    onPlusTapped: () -> Unit
) {
    val title = when (selectedTab) {
        CalendarTabMode.NOTES -> stringResource(R.string.calendar_tab_notes)
        else -> stringResource(R.string.tab_calendar)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 16.dp)
            .defaultMinSize(minHeight = 34.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.align(Alignment.Center)
        )

        when (selectedTab) {
            CalendarTabMode.NOTES -> {
                GlassIconButton(
                    onClick = onPlusTapped,
                    size = GlassButtonSize.SMALL,
                    modifier = Modifier.align(Alignment.CenterEnd),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.calendar_day_add_note),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            else -> {
                IconButton(
                    onClick = onInfoTapped,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(34.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = stringResource(R.string.cd_calendar_info),
                        tint = SoftGold,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AddMenuItem(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp)
    ) {
        icon()
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            color = Color.White,
        )
    }
}

@Composable
private fun AddMenuDivider() {
    HorizontalDivider(
        color = Color.White.copy(alpha = 0.08f),
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

// MARK: - Segmented Picker

@Composable
private fun CalendarSegmentedPicker(
    selectedTab: CalendarTabMode,
    onTabSelected: (CalendarTabMode) -> Unit
) {
    val tabs = CalendarTabMode.entries

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp)
            .height(36.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.08f)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            val isSelected = tab == selectedTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isSelected) SoftGold else Color.Transparent
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onTabSelected(tab) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(tab.labelRes),
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) NearBlack else Color.White
                )
            }
        }
    }
}

// MARK: - Sticky Header

@Composable
private fun CalendarStickyHeader(
    monthTitle: String,
    regionName: String
) {
    val weekdaySymbols = remember {
        val symbols = java.text.DateFormatSymbols(Locale.getDefault()).shortWeekdays
        listOf(
            symbols[Calendar.MONDAY],
            symbols[Calendar.TUESDAY],
            symbols[Calendar.WEDNESDAY],
            symbols[Calendar.THURSDAY],
            symbols[Calendar.FRIDAY],
            symbols[Calendar.SATURDAY],
            symbols[Calendar.SUNDAY]
        ).map { it.take(1).uppercase() }
    }

    Column(
        modifier = Modifier.background(NearBlack)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedContent(
                targetState = monthTitle,
                transitionSpec = {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                },
                label = "monthTitle"
            ) { title ->
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Text(
                text = regionName,
                fontSize = 10.sp,
                color = Slate
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 8.dp)
        ) {
            weekdaySymbols.forEach { symbol ->
                Text(
                    text = symbol,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate
                )
            }
        }
    }
}

// MARK: - Month Content View

@Composable
private fun MonthContentView(
    monthData: MonthData,
    onDayTapped: (CalendarDayDisplay) -> Unit
) {
    val cellHeight = 54.dp
    val rowSpacing = 8.dp
    val monthLabelHeight = 16.dp

    val dayRowCount = (monthData.days.size + 6) / 7

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(top = 4.dp, bottom = 12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            for (column in 0 until 7) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(monthLabelHeight),
                    contentAlignment = Alignment.Center
                ) {
                    if (column == monthData.firstDayColumn) {
                        Text(
                            text = monthData.shortMonthName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(rowSpacing))

        for (rowIndex in 0 until dayRowCount) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (columnIndex in 0 until 7) {
                    val dayIndex = rowIndex * 7 + columnIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(cellHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        if (dayIndex < monthData.days.size) {
                            val day = monthData.days[dayIndex]
                            if (!day.isPlaceholder) {
                                CalendarDayCell(
                                    day = day,
                                    onTap = { onDayTapped(day) }
                                )
                            }
                        }
                    }
                }
            }
            if (rowIndex < dayRowCount - 1) {
                Spacer(modifier = Modifier.height(rowSpacing))
            }
        }
    }
}

// MARK: - Day Cell

@Composable
private fun CalendarDayCell(
    day: CalendarDayDisplay,
    onTap: () -> Unit
) {
    val indicatorColor = day.color.color
    val isSolemnity = day.rank == CelebrationRank.SOLEMNITY
    val isFeastOfTheLord = day.rank == CelebrationRank.FEAST_OF_THE_LORD

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap
            ),
        contentAlignment = Alignment.Center
    ) {
        if (day.isToday) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SoftGold)
            )
        } else if (isSolemnity || isFeastOfTheLord) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, SoftGold.copy(alpha = 0.4f), CircleShape)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${day.dayNumber}",
                fontSize = 17.sp,
                fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (day.isToday) Color.Black else Color.White
            )

            Spacer(modifier = Modifier.height(2.dp))

            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (day.isToday) Color.Black.copy(alpha = 0.5f) else indicatorColor)
            )
        }
    }
}

// MARK: - Skeleton Loading View

@Composable
private fun CalendarSkeletonView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 16.dp)
    ) {
        repeat(6) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                repeat(7) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                    )
                }
            }
        }
    }
}
