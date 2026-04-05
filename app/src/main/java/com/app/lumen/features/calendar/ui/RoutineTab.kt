package com.app.lumen.features.calendar.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.app.lumen.R
import com.app.lumen.features.calendar.data.FirstFridayRoutineEntity
import com.app.lumen.features.calendar.data.WeeklyRoutineEntity
import com.app.lumen.features.calendar.model.RoutineItemType
import com.app.lumen.features.calendar.model.RoutineSuggestion
import com.app.lumen.features.calendar.service.FirstFridayRoutineService
import com.app.lumen.features.calendar.service.FirstFridayYearProgress
import com.app.lumen.features.calendar.service.RoutineStorageService
import com.app.lumen.features.calendar.service.WeekProgress
import com.app.lumen.features.calendar.viewmodel.RoutineViewModel
import com.app.lumen.features.calendar.viewmodel.UnifiedRoutineItem
import com.app.lumen.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val CardBorder = Color.White.copy(alpha = 0.10f)
private val CardBg = Color(0xFF1A1A29)

@Composable
fun RoutineTab(
    viewModel: RoutineViewModel,
    onCreateRoutine: () -> Unit,
    onSuggestionTap: (RoutineSuggestion) -> Unit,
    onRoutineDetail: (UnifiedRoutineItem) -> Unit,
    onEditRoutine: (UnifiedRoutineItem) -> Unit,
    onFirstFridaySetup: () -> Unit
) {
    val activeRoutines by viewModel.activeRoutines.collectAsState()
    val pausedRoutines by viewModel.pausedRoutines.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val streaks by viewModel.streaks.collectAsState()
    val monthProgress by viewModel.monthProgress.collectAsState()
    val weekProgress by viewModel.weekProgress.collectAsState()
    val todayCompletions by viewModel.todayCompletions.collectAsState()
    val firstFridayCount by viewModel.firstFridayCount.collectAsState()
    val firstFridayYearProgress by viewModel.firstFridayYearProgress.collectAsState()
    val hasFirstFriday by viewModel.hasFirstFriday.collectAsState()
    var routineToDelete by remember { mutableStateOf<UnifiedRoutineItem?>(null) }

    // Delete confirmation dialog
    routineToDelete?.let { item ->
        val title = when (item) {
            is UnifiedRoutineItem.Weekly -> item.entity.title
            is UnifiedRoutineItem.FirstFriday -> stringResource(R.string.first_friday_title)
        }
        AlertDialog(
            onDismissRequest = { routineToDelete = null },
            title = { Text(stringResource(R.string.routine_delete_title)) },
            text = { Text(stringResource(R.string.routine_delete_message, title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRoutine(item)
                        routineToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { routineToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = SoftGold, modifier = Modifier.size(32.dp))
        }
        return
    }

    if (activeRoutines.isEmpty() && pausedRoutines.isEmpty()) {
        // Empty state
        EmptyRoutineState(
            suggestions = suggestions,
            hasFirstFriday = hasFirstFriday,
            onCreateRoutine = onCreateRoutine,
            onSuggestionTap = onSuggestionTap,
            onFirstFridaySetup = onFirstFridaySetup
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Active Routines
        if (activeRoutines.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.routine_section_active),
                    color = SoftGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
                )
            }

            items(activeRoutines, key = {
                when (it) {
                    is UnifiedRoutineItem.Weekly -> it.entity.id
                    is UnifiedRoutineItem.FirstFriday -> it.entity.id
                }
            }) { item ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    when (item) {
                        is UnifiedRoutineItem.Weekly -> {
                            WeeklyRoutineCard(
                                routine = item.entity,
                                streak = streaks[item.entity.id] ?: 0,
                                monthProgress = monthProgress[item.entity.id],
                                weekProgress = weekProgress[item.entity.id] ?: emptyList(),
                                isCompletedToday = todayCompletions[item.entity.id] ?: false,
                                isScheduledToday = viewModel.isScheduledToday(item.entity),
                                scheduleSummary = viewModel.scheduleSummary(item.entity),
                                onTap = { onRoutineDetail(item) },
                                onComplete = { viewModel.toggleCompletion(item) },
                                onPause = { viewModel.pauseRoutine(item) },
                                onDelete = { routineToDelete = item },
                                onEdit = { onEditRoutine(item) }
                            )
                        }
                        is UnifiedRoutineItem.FirstFriday -> {
                            FirstFridayRoutineCard(
                                routine = item.entity,
                                consecutiveCount = firstFridayCount,
                                yearProgress = firstFridayYearProgress,
                                isCompletedToday = todayCompletions[item.entity.id] ?: false,
                                isFirstFridayToday = FirstFridayRoutineService.isFirstFridayToday(),
                                onTap = { onRoutineDetail(item) },
                                onComplete = { viewModel.toggleCompletion(item) },
                                onPause = { viewModel.pauseRoutine(item) },
                                onDelete = { routineToDelete = item }
                            )
                        }
                    }
                }
            }
        }

        // Paused Routines
        if (pausedRoutines.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.routine_section_paused),
                    color = Slate,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
                )
            }

            items(pausedRoutines, key = {
                when (it) {
                    is UnifiedRoutineItem.Weekly -> "paused-${it.entity.id}"
                    is UnifiedRoutineItem.FirstFriday -> "paused-${it.entity.id}"
                }
            }) { item ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    PausedRoutineCard(
                        item = item,
                        scheduleSummary = when (item) {
                            is UnifiedRoutineItem.Weekly -> viewModel.scheduleSummary(item.entity)
                            is UnifiedRoutineItem.FirstFriday -> stringResource(R.string.first_friday_schedule)
                        },
                        onResume = { viewModel.resumeRoutine(item) },
                        onDelete = { routineToDelete = item }
                    )
                }
            }
        }

        // Suggestions
        if (suggestions.isNotEmpty() || !hasFirstFriday) {
            item {
                Text(
                    text = stringResource(R.string.routine_section_suggested),
                    color = Slate,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
                )
            }

            item {
                // Edge-to-edge LazyRow with internal contentPadding
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    if (!hasFirstFriday) {
                        item {
                            FirstFridaySuggestionCard(onClick = onFirstFridaySetup)
                        }
                    }
                    items(suggestions) { suggestion ->
                        SuggestionCard(
                            suggestion = suggestion,
                            onClick = { onSuggestionTap(suggestion) }
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(120.dp)) }
    }
}

@Composable
private fun EmptyRoutineState(
    suggestions: List<RoutineSuggestion>,
    hasFirstFriday: Boolean,
    onCreateRoutine: () -> Unit,
    onSuggestionTap: (RoutineSuggestion) -> Unit,
    onFirstFridaySetup: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(40.dp))

            // Icon
            Icon(
                imageVector = Icons.Filled.Autorenew,
                contentDescription = null,
                tint = Slate,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.routine_empty_title),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.routine_empty_subtitle),
                color = Slate,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Suggestion grid (2 columns) — matches iOS: First Friday first, then all suggestions
        item {
            val gridItems = buildList<@Composable (Modifier) -> Unit> {
                if (!hasFirstFriday) {
                    add { mod ->
                        FirstFridaySuggestionCardLarge(
                            modifier = mod,
                            onClick = onFirstFridaySetup
                        )
                    }
                }
                for (suggestion in RoutineSuggestion.all) {
                    add { mod ->
                        EmptySuggestionCard(
                            suggestion = suggestion,
                            modifier = mod,
                            onClick = { onSuggestionTap(suggestion) }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                gridItems.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEachIndexed { _, card ->
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                card(Modifier.fillMaxWidth().fillMaxHeight())
                            }
                        }
                        if (row.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(120.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WeeklyRoutineCard(
    routine: WeeklyRoutineEntity,
    streak: Int,
    monthProgress: Pair<Int, Int>?,
    weekProgress: List<WeekProgress>,
    isCompletedToday: Boolean,
    isScheduledToday: Boolean,
    scheduleSummary: String,
    onTap: () -> Unit,
    onComplete: () -> Unit,
    onPause: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val type = RoutineItemType.fromRawValue(routine.typeRaw)
    val isMassType = type == RoutineItemType.MASS
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onTap() },
                    onLongClick = { showMenu = true }
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header row
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = type.icon,
                        contentDescription = null,
                        tint = SoftGold,
                        modifier = Modifier.size(24.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = routine.title,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 20.sp
                        )
                        Text(
                            text = scheduleSummary,
                            color = Slate,
                            fontSize = 12.sp
                        )
                    }

                    // Streak badge
                    if (streak > 0) {
                        StreakBadge(streak)
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Month count
                    if (monthProgress != null) {
                        Text(
                            text = stringResource(R.string.routine_month_count, monthProgress.first, monthProgress.second),
                            color = Slate,
                            fontSize = 11.sp,
                            textAlign = TextAlign.End,
                            lineHeight = 14.sp,
                            maxLines = 2,
                            modifier = Modifier.widthIn(max = 110.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress visualization
                if (isMassType && monthProgress != null) {
                    // Monthly progress dots for Mass
                    MonthlyProgressDots(weekProgress)
                } else {
                    // Weekly progress dots for other types
                    WeeklyProgressDots(weekProgress)
                }

                // Completion checkbox (only if scheduled today)
                if (isScheduledToday) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onComplete() }
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isCompletedToday) SoftGold else Color.Transparent
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = if (isCompletedToday) SoftGold else Slate,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompletedToday) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = stringResource(R.string.routine_completed_today),
                                    tint = Color.Black,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isCompletedToday) stringResource(R.string.routine_completed_today) else stringResource(R.string.routine_mark_completed),
                            color = if (isCompletedToday) SoftGold else Slate,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Context menu
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.edit)) },
                onClick = { showMenu = false; onEdit() },
                leadingIcon = { Icon(Icons.Filled.Edit, null) }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.routine_pause)) },
                onClick = { showMenu = false; onPause() },
                leadingIcon = { Icon(Icons.Filled.Pause, null) }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete), color = Color(0xFFEF5350)) },
                onClick = { showMenu = false; onDelete() },
                leadingIcon = { Icon(Icons.Filled.Delete, null, tint = Color(0xFFEF5350)) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FirstFridayRoutineCard(
    routine: FirstFridayRoutineEntity,
    consecutiveCount: Int,
    yearProgress: List<FirstFridayYearProgress>,
    isCompletedToday: Boolean,
    isFirstFridayToday: Boolean,
    onTap: () -> Unit,
    onComplete: () -> Unit,
    onPause: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onTap() },
                onLongClick = { showMenu = true }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // First Friday icon
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(SoftGold),
                    contentAlignment = Alignment.Center
                ) {
                    Text("1", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.first_friday_title),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        stringResource(R.string.first_friday_subtitle),
                        color = Slate,
                        fontSize = 12.sp
                    )
                }
                if (consecutiveCount > 0) {
                    ConsecutiveCountBadge(consecutiveCount)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Year progress - 12 month icons
            YearProgressRow(yearProgress)

            // Complete button on First Friday
            if (isFirstFridayToday) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onComplete() }
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (isCompletedToday) SoftGold else Color.Transparent)
                            .border(1.5.dp, if (isCompletedToday) SoftGold else Slate, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompletedToday) {
                            Icon(Icons.Filled.Check, null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCompletedToday) stringResource(R.string.routine_completed_today) else stringResource(R.string.routine_mark_completed),
                        color = if (isCompletedToday) SoftGold else Slate,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }

        // Context menu
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.routine_pause)) },
                onClick = { showMenu = false; onPause() },
                leadingIcon = { Icon(Icons.Filled.Pause, null) }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete), color = Color(0xFFEF5350)) },
                onClick = { showMenu = false; onDelete() },
                leadingIcon = { Icon(Icons.Filled.Delete, null, tint = Color(0xFFEF5350)) }
            )
        }
    }
}

@Composable
fun PausedRoutineCard(
    item: UnifiedRoutineItem,
    scheduleSummary: String,
    onResume: () -> Unit,
    onDelete: () -> Unit
) {
    val title = when (item) {
        is UnifiedRoutineItem.Weekly -> item.entity.title
        is UnifiedRoutineItem.FirstFriday -> stringResource(R.string.first_friday_title)
    }
    val icon = when (item) {
        is UnifiedRoutineItem.Weekly -> RoutineItemType.fromRawValue(item.entity.typeRaw).icon
        is UnifiedRoutineItem.FirstFriday -> Icons.Filled.Favorite
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(0.6f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Slate, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(scheduleSummary, color = Slate, fontSize = 12.sp, lineHeight = 17.sp)
            }
            IconButton(onClick = onResume) {
                Icon(Icons.Filled.PlayArrow, stringResource(R.string.routine_resume), tint = SoftGold)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, stringResource(R.string.delete), tint = Color(0xFFEF5350))
            }
        }
    }
}

@Composable
private fun StreakBadge(streak: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SoftGold.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Icon(
            Icons.Filled.LocalFireDepartment,
            null,
            tint = SoftGold,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text("$streak", color = SoftGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ConsecutiveCountBadge(count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SoftGold.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Icon(Icons.Filled.LocalFireDepartment, null, tint = SoftGold, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(3.dp))
        Text("$count", color = SoftGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun WeeklyProgressDots(weekProgress: List<WeekProgress>) {
    val symbols = java.text.DateFormatSymbols.getInstance(java.util.Locale.getDefault())
    val shortWeekdays = symbols.shortWeekdays
    val dayLabels = listOf(
        shortWeekdays[java.util.Calendar.MONDAY].first().uppercaseChar().toString(),
        shortWeekdays[java.util.Calendar.TUESDAY].first().uppercaseChar().toString(),
        shortWeekdays[java.util.Calendar.WEDNESDAY].first().uppercaseChar().toString(),
        shortWeekdays[java.util.Calendar.THURSDAY].first().uppercaseChar().toString(),
        shortWeekdays[java.util.Calendar.FRIDAY].first().uppercaseChar().toString(),
        shortWeekdays[java.util.Calendar.SATURDAY].first().uppercaseChar().toString(),
        shortWeekdays[java.util.Calendar.SUNDAY].first().uppercaseChar().toString()
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        weekProgress.forEachIndexed { index, day ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Dot
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                day.isBeforeCreation -> Color.Transparent
                                !day.isScheduled -> Color.Transparent
                                day.isCompleted -> SoftGold
                                else -> Slate.copy(alpha = 0.3f)
                            }
                        )
                        .then(
                            if (day.isScheduled && !day.isCompleted && !day.isBeforeCreation) {
                                Modifier.border(1.dp, Slate.copy(alpha = 0.5f), CircleShape)
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (day.isCompleted && day.isScheduled) {
                        Icon(Icons.Filled.Check, null, tint = Color.Black, modifier = Modifier.size(12.dp))
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (index < dayLabels.size) dayLabels[index] else "",
                    color = Slate,
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
fun MonthlyProgressDots(weekProgress: List<WeekProgress>) {
    // Show as compact weekly view but with X marks for missed
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        weekProgress.forEach { day ->
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            day.isBeforeCreation -> Color.Transparent
                            !day.isScheduled -> Color.Transparent
                            day.isCompleted -> SoftGold
                            else -> Slate.copy(alpha = 0.3f)
                        }
                    )
                    .then(
                        if (day.isScheduled && !day.isCompleted && !day.isBeforeCreation) {
                            Modifier.border(1.dp, Slate.copy(alpha = 0.5f), CircleShape)
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    day.isCompleted && day.isScheduled -> {
                        Icon(Icons.Filled.Check, null, tint = Color.Black, modifier = Modifier.size(12.dp))
                    }
                    day.isScheduled && !day.isCompleted && !day.isBeforeCreation &&
                            day.date < RoutineStorageService.startOfDay(System.currentTimeMillis()) -> {
                        Icon(Icons.Filled.Close, null, tint = Color(0xFFEF5350), modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun YearProgressRow(yearProgress: List<FirstFridayYearProgress>) {
    val symbols = java.text.DateFormatSymbols.getInstance(java.util.Locale.getDefault())
    val monthLabels = (0..11).map { symbols.shortMonths[it].first().uppercaseChar().toString() }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        yearProgress.forEachIndexed { index, month ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                month.isBeforeTracking -> Color.Transparent
                                month.isCompleted || month.isPreChecked -> SoftGold
                                month.isFuture -> Slate.copy(alpha = 0.2f)
                                month.isPast -> Color.White.copy(alpha = 0.1f)
                                else -> Slate.copy(alpha = 0.2f)
                            }
                        )
                        .then(
                            if (!month.isBeforeTracking && !month.isCompleted && !month.isPreChecked && month.isFuture) {
                                Modifier.border(1.dp, Slate.copy(alpha = 0.3f), CircleShape)
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        month.isBeforeTracking -> {}
                        month.isCompleted || month.isPreChecked -> {
                            Icon(Icons.Filled.Check, null, tint = Color.Black, modifier = Modifier.size(12.dp))
                        }
                        month.isPast -> {
                            Icon(Icons.Filled.Close, null, tint = Slate, modifier = Modifier.size(12.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (index < monthLabels.size) monthLabels[index] else "",
                    color = Slate,
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
fun SuggestionCard(
    suggestion: RoutineSuggestion,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                suggestion.type.icon,
                null,
                tint = SoftGold,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(suggestion.titleRes),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )
                Text(stringResource(suggestion.subtitleRes), color = Slate, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Filled.AddCircleOutline, null, tint = SoftGold, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun FirstFridaySuggestionCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(SoftGold),
                contentAlignment = Alignment.Center
            ) {
                Text("1", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.first_friday_title),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )
                Text(stringResource(R.string.first_friday_subtitle), color = Slate, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Filled.AddCircleOutline, null, tint = SoftGold, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun EmptySuggestionCard(
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
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxHeight()
        ) {
            Icon(
                suggestion.type.icon,
                null,
                tint = SoftGold,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                stringResource(suggestion.titleRes),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                stringResource(suggestion.subtitleRes),
                color = Slate,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Add, null, tint = SoftGold, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.routine_add), color = SoftGold, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun FirstFridaySuggestionCardLarge(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxHeight()) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(SoftGold),
                contentAlignment = Alignment.Center
            ) {
                Text("1", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.first_friday_title), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp)
            Spacer(modifier = Modifier.height(5.dp))
            Text(stringResource(R.string.first_friday_subtitle), color = Slate, fontSize = 12.sp, lineHeight = 16.sp)
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Add, null, tint = SoftGold, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.routine_add), color = SoftGold, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

