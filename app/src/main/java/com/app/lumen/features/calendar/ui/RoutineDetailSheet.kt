package com.app.lumen.features.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.app.lumen.R
import com.app.lumen.features.calendar.data.FirstFridayRoutineEntity
import com.app.lumen.features.calendar.data.WeeklyRoutineEntity
import com.app.lumen.features.calendar.model.RoutineItemType
import com.app.lumen.features.calendar.service.FirstFridayYearProgress
import com.app.lumen.features.calendar.service.MonthProgressDay
import com.app.lumen.features.calendar.service.RoutineStorageService
import com.app.lumen.features.calendar.service.WeekProgress
import com.app.lumen.features.calendar.viewmodel.RoutineViewModel
import com.app.lumen.features.calendar.viewmodel.UnifiedRoutineItem
import com.app.lumen.ui.components.GlassIconButton
import com.app.lumen.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val CardBorder = Color.White.copy(alpha = 0.10f)
private val CardBg = Color(0xFF1A1A29)
private val GlassMenuBg = Color(0xFF1E1E30)

@Composable
fun RoutineDetailSheet(
    item: UnifiedRoutineItem,
    viewModel: RoutineViewModel,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    when (item) {
        is UnifiedRoutineItem.Weekly -> WeeklyRoutineDetailContent(
            routine = item.entity,
            viewModel = viewModel,
            onDismiss = onDismiss,
            onEdit = onEdit,
            onDelete = onDelete
        )
        is UnifiedRoutineItem.FirstFriday -> FirstFridayDetailContent(
            routine = item.entity,
            viewModel = viewModel,
            onDismiss = onDismiss,
            onDelete = onDelete
        )
    }
}

@Composable
private fun WeeklyRoutineDetailContent(
    routine: WeeklyRoutineEntity,
    viewModel: RoutineViewModel,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val type = RoutineItemType.fromRawValue(routine.typeRaw)
    val streaks by viewModel.streaks.collectAsState()
    val monthProgress by viewModel.monthProgress.collectAsState()
    val streak = streaks[routine.id] ?: 0
    val mp = monthProgress[routine.id]
    val isMassType = type == RoutineItemType.MASS

    var monthOffset by remember { mutableIntStateOf(0) }
    var weekOffset by remember { mutableIntStateOf(0) }
    var detailedProgress by remember { mutableStateOf<List<Any>>(emptyList()) }
    var refreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(monthOffset, weekOffset, refreshKey) {
        detailedProgress = if (isMassType) {
            viewModel.getMonthProgressDetailed(routine, monthOffset)
        } else {
            viewModel.getWeekProgress(routine, weekOffset)
        }
    }

    var showOptionsMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.routine_close), color = SoftGold) }

            Box {
                GlassIconButton(onClick = { showOptionsMenu = true }) {
                    Icon(
                        Icons.Filled.MoreHoriz,
                        contentDescription = stringResource(R.string.routine_options),
                        tint = SoftGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(
                    expanded = showOptionsMenu,
                    onDismissRequest = { showOptionsMenu = false },
                    containerColor = GlassMenuBg
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit), color = Color.White) },
                        onClick = { showOptionsMenu = false; onEdit() },
                        leadingIcon = { Icon(Icons.Filled.Edit, null, tint = SoftGold) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete), color = Color(0xFFEF5350)) },
                        onClick = { showOptionsMenu = false; onDelete() },
                        leadingIcon = { Icon(Icons.Filled.Delete, null, tint = Color(0xFFEF5350)) }
                    )
                }
            }
        }

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            item {
                // Icon and title
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(type.icon, null, tint = SoftGold, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(routine.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    // Type badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Slate.copy(alpha = 0.2f))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(stringResource(type.displayNameRes), color = Slate, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        icon = Icons.Filled.LocalFireDepartment,
                        value = "$streak",
                        label = stringResource(R.string.routine_day_streak),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.Filled.CalendarMonth,
                        value = "${mp?.first ?: 0}/${mp?.second ?: 0}",
                        label = stringResource(R.string.routine_this_month),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Schedule
                Text(stringResource(R.string.routine_schedule), color = SoftGold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CalendarMonth, null, tint = SoftGold, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                viewModel.scheduleSummary(routine),
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Schedule, null, tint = SoftGold, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(formatTime(routine.hour, routine.minute), color = Color.White, fontSize = 14.sp)
                        }
                        if (routine.isNotificationEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Notifications, null, tint = SoftGold, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                val leadOption = com.app.lumen.features.calendar.model.leadTimeOptions
                                    .find { it.minutes == routine.notificationLeadTimeMinutes }
                                val leadLabel = if (leadOption != null) stringResource(leadOption.labelRes) else stringResource(R.string.routine_lead_at_time)
                                Text(leadLabel, color = Color.White, fontSize = 14.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Progress
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.routine_progress), color = SoftGold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Row {
                        IconButton(
                            onClick = {
                                if (isMassType) monthOffset-- else weekOffset--
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Filled.ChevronLeft, null, tint = Slate)
                        }
                        IconButton(
                            onClick = {
                                if (isMassType) monthOffset++ else weekOffset++
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Filled.ChevronRight, null, tint = Slate)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (isMassType) {
                            // Monthly progress header
                            val cal = Calendar.getInstance()
                            cal.add(Calendar.MONTH, monthOffset)
                            val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
                            Text(monthName, color = SoftGold, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(12.dp))

                            // Month grid
                            val monthDays = detailedProgress.filterIsInstance<MonthProgressDay>()
                            MonthProgressGrid(
                                days = monthDays,
                                onToggle = { day ->
                                    val todayStart = RoutineStorageService.startOfDay(System.currentTimeMillis())
                                    if (day.date <= todayStart && !day.isBeforeCreation) {
                                        viewModel.toggleCompletionForDate(routine, day.date)
                                        refreshKey++
                                    }
                                }
                            )

                            val completed = monthDays.count { it.isCompleted }
                            val total = monthDays.count { !it.isBeforeCreation && !it.isFuture }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(stringResource(R.string.routine_month_count, completed, total), color = Slate, fontSize = 12.sp)
                        } else {
                            // Weekly progress header
                            val weekDays = detailedProgress.filterIsInstance<WeekProgress>()
                            if (weekDays.isNotEmpty()) {
                                val startDate = SimpleDateFormat("d MMM", Locale.getDefault())
                                    .format(Date(weekDays.first().date))
                                val endDate = SimpleDateFormat("d MMM", Locale.getDefault())
                                    .format(Date(weekDays.last().date))
                                Text("$startDate - $endDate", color = SoftGold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                TappableWeeklyProgressDots(
                                    weekProgress = weekDays,
                                    onToggle = { day ->
                                        val todayStart = RoutineStorageService.startOfDay(System.currentTimeMillis())
                                        if (day.date <= todayStart && !day.isBeforeCreation && day.isScheduled) {
                                            viewModel.toggleCompletionForDate(routine, day.date)
                                            refreshKey++
                                        }
                                    }
                                )
                            }
                        }

                        // Created date
                        Spacer(modifier = Modifier.height(12.dp))
                        val createdDate = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                            .format(Date(routine.createdAt))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CalendarToday, null, tint = Slate, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.routine_created_date, createdDate), color = Slate, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Settings
                Text(stringResource(R.string.routine_settings), color = SoftGold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
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
                        Icon(Icons.Filled.CheckCircle, null, tint = SoftGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.routine_track_completion), color = Color.White, modifier = Modifier.weight(1f), fontSize = 14.sp)
                        Text(
                            if (routine.isLoggingEnabled) stringResource(R.string.routine_enabled) else stringResource(R.string.routine_disabled),
                            color = Slate,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun FirstFridayDetailContent(
    routine: FirstFridayRoutineEntity,
    viewModel: RoutineViewModel,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val count by viewModel.firstFridayCount.collectAsState()
    val yearProgress by viewModel.firstFridayYearProgress.collectAsState()
    var yearOffset by remember { mutableIntStateOf(0) }
    var detailedYearProgress by remember { mutableStateOf<List<FirstFridayYearProgress>>(emptyList()) }

    var ffRefreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(yearOffset, ffRefreshKey) {
        detailedYearProgress = viewModel.getFirstFridayYearProgress(routine, yearOffset)
    }

    // Use the default year progress when offset is 0 (but respect refresh)
    val displayProgress = if (yearOffset == 0 && ffRefreshKey == 0) yearProgress else detailedYearProgress

    var showOptionsMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.routine_close), color = SoftGold) }

            Box {
                GlassIconButton(onClick = { showOptionsMenu = true }) {
                    Icon(
                        Icons.Filled.MoreHoriz,
                        contentDescription = stringResource(R.string.routine_options),
                        tint = SoftGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(
                    expanded = showOptionsMenu,
                    onDismissRequest = { showOptionsMenu = false },
                    containerColor = GlassMenuBg
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete), color = Color(0xFFEF5350)) },
                        onClick = { showOptionsMenu = false; onDelete() },
                        leadingIcon = { Icon(Icons.Filled.Delete, null, tint = Color(0xFFEF5350)) }
                    )
                }
            }
        }

        LazyColumn {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(SoftGold),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("1", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.first_friday_title), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.first_friday_consecutive_count, count), color = SoftGold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Year progress
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.routine_progress), color = SoftGold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Row {
                        IconButton(onClick = { yearOffset-- }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.ChevronLeft, null, tint = Slate)
                        }
                        IconButton(onClick = { yearOffset++ }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.ChevronRight, null, tint = Slate)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val cal = Calendar.getInstance()
                        cal.add(Calendar.YEAR, yearOffset)
                        Text(
                            "${cal.get(Calendar.YEAR)}",
                            color = SoftGold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TappableYearProgressRow(
                            yearProgress = displayProgress,
                            onToggle = { month ->
                                val todayStart = RoutineStorageService.startOfDay(System.currentTimeMillis())
                                if (month.date <= todayStart && !month.isPreChecked) {
                                    viewModel.toggleFirstFridayCompletionForDate(routine, month.date)
                                    ffRefreshKey++
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        val createdDate = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                            .format(Date(routine.createdAt))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CalendarToday, null, tint = Slate, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.routine_created_date, createdDate), color = Slate, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = SoftGold, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Text(label, color = Slate, fontSize = 12.sp)
        }
    }
}

@Composable
fun MonthProgressGrid(
    days: List<MonthProgressDay>,
    onToggle: (MonthProgressDay) -> Unit
) {
    val todayStart = RoutineStorageService.startOfDay(System.currentTimeMillis())

    // Display as a row of circles with day numbers
    val rows = days.chunked(7)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (day in row) {
                    val canToggle = day.date <= todayStart && !day.isBeforeCreation
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    day.isBeforeCreation -> Color.Transparent
                                    day.isCompleted -> SoftGold
                                    day.isPast -> Color(0xFFEF5350).copy(alpha = 0.2f)
                                    else -> Slate.copy(alpha = 0.15f)
                                }
                            )
                            .then(
                                if (!day.isCompleted && !day.isBeforeCreation) {
                                    Modifier.border(
                                        1.dp,
                                        if (day.date == todayStart) SoftGold.copy(alpha = 0.5f)
                                        else Slate.copy(alpha = 0.3f),
                                        CircleShape
                                    )
                                } else Modifier
                            )
                            .clickable(enabled = canToggle) { onToggle(day) },
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            day.isBeforeCreation -> {}
                            day.isCompleted -> {
                                Icon(Icons.Filled.Check, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            }
                            day.isPast -> {
                                Icon(Icons.Filled.Close, null, tint = Color(0xFFEF5350), modifier = Modifier.size(16.dp))
                            }
                            else -> {
                                Text("${day.dayOfMonth}", color = Slate, fontSize = 11.sp)
                            }
                        }
                    }
                }
                // Pad remaining cells
                repeat(7 - row.size) {
                    Spacer(modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

@Composable
private fun TappableYearProgressRow(
    yearProgress: List<FirstFridayYearProgress>,
    onToggle: (FirstFridayYearProgress) -> Unit
) {
    val monthLabels = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
    val todayStart = RoutineStorageService.startOfDay(System.currentTimeMillis())

    val rows = yearProgress.chunked(6)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (row in rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { month ->
                    val index = month.monthIndex
                    val canToggle = !month.isBeforeTracking && !month.isPreChecked && month.date <= todayStart

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
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
                                )
                                .clickable(enabled = canToggle) { onToggle(month) },
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                month.isBeforeTracking -> {}
                                month.isCompleted || month.isPreChecked -> {
                                    Icon(Icons.Filled.Check, null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                }
                                month.isPast -> {
                                    Icon(Icons.Filled.Close, null, tint = Slate, modifier = Modifier.size(14.dp))
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
    }
}

@Composable
private fun TappableWeeklyProgressDots(
    weekProgress: List<WeekProgress>,
    onToggle: (WeekProgress) -> Unit
) {
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
    val todayStart = RoutineStorageService.startOfDay(System.currentTimeMillis())

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        weekProgress.forEachIndexed { index, day ->
            val canToggle = day.isScheduled && !day.isBeforeCreation && day.date <= todayStart

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                day.isBeforeCreation -> Color.Transparent
                                !day.isScheduled -> Color.Transparent
                                day.isCompleted -> SoftGold
                                day.date < todayStart -> Color(0xFFEF5350).copy(alpha = 0.2f)
                                else -> Slate.copy(alpha = 0.15f)
                            }
                        )
                        .then(
                            if (day.isScheduled && !day.isCompleted && !day.isBeforeCreation) {
                                Modifier.border(
                                    1.dp,
                                    if (day.date == todayStart) SoftGold.copy(alpha = 0.5f)
                                    else Slate.copy(alpha = 0.5f),
                                    CircleShape
                                )
                            } else Modifier
                        )
                        .clickable(enabled = canToggle) { onToggle(day) },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        day.isBeforeCreation || !day.isScheduled -> {}
                        day.isCompleted -> {
                            Icon(Icons.Filled.Check, null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        }
                        day.date < todayStart -> {
                            Icon(Icons.Filled.Close, null, tint = Color(0xFFEF5350), modifier = Modifier.size(14.dp))
                        }
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

private fun formatTime(hour: Int, minute: Int): String {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(cal.time)
}
