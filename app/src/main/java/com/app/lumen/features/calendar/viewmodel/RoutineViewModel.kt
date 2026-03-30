package com.app.lumen.features.calendar.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.lumen.features.calendar.data.FirstFridayRoutineEntity
import com.app.lumen.features.calendar.data.WeeklyRoutineEntity
import com.app.lumen.features.calendar.model.RoutineItemType
import com.app.lumen.features.calendar.model.RoutineSuggestion
import com.app.lumen.features.calendar.service.*
import com.app.lumen.services.AnalyticsEvent
import com.app.lumen.services.AnalyticsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

// Unified item to display both routine types in one list
sealed class UnifiedRoutineItem {
    abstract val sortOrder: Int
    abstract val isActive: Boolean

    data class Weekly(val entity: WeeklyRoutineEntity) : UnifiedRoutineItem() {
        override val sortOrder: Int get() = entity.sortOrder
        override val isActive: Boolean get() = entity.isActive
    }

    data class FirstFriday(val entity: FirstFridayRoutineEntity) : UnifiedRoutineItem() {
        override val sortOrder: Int get() = entity.sortOrder
        override val isActive: Boolean get() = entity.isActive
    }
}

class RoutineViewModel(application: Application) : AndroidViewModel(application) {

    val routineService = RoutineStorageService(application)
    val firstFridayService = FirstFridayRoutineService(application)
    val notificationManager = LumenNotificationManager(application)

    private val _activeRoutines = MutableStateFlow<List<UnifiedRoutineItem>>(emptyList())
    val activeRoutines: StateFlow<List<UnifiedRoutineItem>> = _activeRoutines.asStateFlow()

    private val _pausedRoutines = MutableStateFlow<List<UnifiedRoutineItem>>(emptyList())
    val pausedRoutines: StateFlow<List<UnifiedRoutineItem>> = _pausedRoutines.asStateFlow()

    private val _suggestions = MutableStateFlow<List<RoutineSuggestion>>(emptyList())
    val suggestions: StateFlow<List<RoutineSuggestion>> = _suggestions.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Streak cache
    private val _streaks = MutableStateFlow<Map<String, Int>>(emptyMap())
    val streaks: StateFlow<Map<String, Int>> = _streaks.asStateFlow()

    // Month progress cache (completed, scheduled)
    private val _monthProgress = MutableStateFlow<Map<String, Pair<Int, Int>>>(emptyMap())
    val monthProgress: StateFlow<Map<String, Pair<Int, Int>>> = _monthProgress.asStateFlow()

    // Week progress cache
    private val _weekProgress = MutableStateFlow<Map<String, List<WeekProgress>>>(emptyMap())
    val weekProgress: StateFlow<Map<String, List<WeekProgress>>> = _weekProgress.asStateFlow()

    // First Friday consecutive count
    private val _firstFridayCount = MutableStateFlow(0)
    val firstFridayCount: StateFlow<Int> = _firstFridayCount.asStateFlow()

    // First Friday year progress
    private val _firstFridayYearProgress = MutableStateFlow<List<FirstFridayYearProgress>>(emptyList())
    val firstFridayYearProgress: StateFlow<List<FirstFridayYearProgress>> = _firstFridayYearProgress.asStateFlow()

    // Completion state for today (routineId -> isCompleted)
    private val _todayCompletions = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val todayCompletions: StateFlow<Map<String, Boolean>> = _todayCompletions.asStateFlow()

    // Has First Friday routine
    private val _hasFirstFriday = MutableStateFlow(false)
    val hasFirstFriday: StateFlow<Boolean> = _hasFirstFriday.asStateFlow()

    init {
        loadRoutines()
    }

    fun loadRoutines() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val weeklyActive = routineService.activeRoutines()
                val weeklyPaused = routineService.pausedRoutines()
                val ffRoutine = firstFridayService.getRoutine()

                val activeItems = mutableListOf<UnifiedRoutineItem>()
                val pausedItems = mutableListOf<UnifiedRoutineItem>()

                weeklyActive.forEach { activeItems.add(UnifiedRoutineItem.Weekly(it)) }
                weeklyPaused.forEach { pausedItems.add(UnifiedRoutineItem.Weekly(it)) }

                if (ffRoutine != null) {
                    if (ffRoutine.isActive) {
                        activeItems.add(UnifiedRoutineItem.FirstFriday(ffRoutine))
                    } else {
                        pausedItems.add(UnifiedRoutineItem.FirstFriday(ffRoutine))
                    }
                    _hasFirstFriday.value = true
                } else {
                    _hasFirstFriday.value = false
                }

                _activeRoutines.value = activeItems.sortedBy { it.sortOrder }
                _pausedRoutines.value = pausedItems.sortedBy { it.sortOrder }

                // Load progress data
                loadProgressData(weeklyActive, ffRoutine)

                // Compute suggestions
                computeSuggestions(weeklyActive, ffRoutine)
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadProgressData(
        weeklyRoutines: List<WeeklyRoutineEntity>,
        ffRoutine: FirstFridayRoutineEntity?
    ) {
        val todayStart = RoutineStorageService.startOfDay(System.currentTimeMillis())
        val streakMap = mutableMapOf<String, Int>()
        val monthMap = mutableMapOf<String, Pair<Int, Int>>()
        val weekMap = mutableMapOf<String, List<WeekProgress>>()
        val completionMap = mutableMapOf<String, Boolean>()

        for (routine in weeklyRoutines) {
            streakMap[routine.id] = routineService.currentStreak(routine)
            monthMap[routine.id] = routineService.monthProgress(routine)
            weekMap[routine.id] = routineService.weekProgress(routine)
            completionMap[routine.id] = routineService.isCompleted(routine, todayStart)
        }

        _streaks.value = streakMap
        _monthProgress.value = monthMap
        _weekProgress.value = weekMap
        _todayCompletions.value = completionMap

        if (ffRoutine != null) {
            _firstFridayCount.value = firstFridayService.currentConsecutiveCount(ffRoutine)
            _firstFridayYearProgress.value = firstFridayService.yearProgress(ffRoutine)
            completionMap[ffRoutine.id] = firstFridayService.isCompleted(ffRoutine, todayStart)
            _todayCompletions.value = completionMap
        }
    }

    private fun computeSuggestions(
        weeklyRoutines: List<WeeklyRoutineEntity>,
        ffRoutine: FirstFridayRoutineEntity?
    ) {
        val existingTypes = weeklyRoutines.map { RoutineItemType.fromRawValue(it.typeRaw) }.toSet()
        val suggestions = mutableListOf<RoutineSuggestion>()

        for (suggestion in RoutineSuggestion.all) {
            if (suggestion.type !in existingTypes) {
                suggestions.add(suggestion)
            }
        }

        _suggestions.value = suggestions
    }

    // MARK: - Routine CRUD

    fun createRoutine(
        title: String,
        type: RoutineItemType,
        selectedDays: Set<Int>,
        hour: Int,
        minute: Int,
        isNotificationEnabled: Boolean,
        isLoggingEnabled: Boolean,
        notificationLeadTimeMinutes: Int
    ) {
        viewModelScope.launch {
            routineService.createRoutine(
                title = title,
                type = type,
                selectedDays = selectedDays,
                hour = hour,
                minute = minute,
                isNotificationEnabled = isNotificationEnabled,
                isLoggingEnabled = isLoggingEnabled,
                notificationLeadTimeMinutes = notificationLeadTimeMinutes
            )
            loadRoutines()
        }
    }

    fun updateRoutine(
        routine: WeeklyRoutineEntity,
        title: String,
        selectedDays: Set<Int>,
        hour: Int,
        minute: Int,
        isNotificationEnabled: Boolean,
        isLoggingEnabled: Boolean,
        notificationLeadTimeMinutes: Int
    ) {
        viewModelScope.launch {
            routineService.updateRoutine(
                routine = routine,
                title = title,
                selectedDays = selectedDays,
                hour = hour,
                minute = minute,
                isNotificationEnabled = isNotificationEnabled,
                isLoggingEnabled = isLoggingEnabled,
                notificationLeadTimeMinutes = notificationLeadTimeMinutes
            )
            loadRoutines()
        }
    }

    fun deleteRoutine(item: UnifiedRoutineItem) {
        viewModelScope.launch {
            when (item) {
                is UnifiedRoutineItem.Weekly -> routineService.deleteRoutine(item.entity)
                is UnifiedRoutineItem.FirstFriday -> firstFridayService.deleteRoutine(item.entity)
            }
            loadRoutines()
        }
    }

    fun pauseRoutine(item: UnifiedRoutineItem) {
        viewModelScope.launch {
            when (item) {
                is UnifiedRoutineItem.Weekly -> routineService.pauseRoutine(item.entity)
                is UnifiedRoutineItem.FirstFriday -> firstFridayService.pauseRoutine(item.entity)
            }
            loadRoutines()
        }
    }

    fun resumeRoutine(item: UnifiedRoutineItem) {
        viewModelScope.launch {
            when (item) {
                is UnifiedRoutineItem.Weekly -> routineService.resumeRoutine(item.entity)
                is UnifiedRoutineItem.FirstFriday -> firstFridayService.resumeRoutine(item.entity)
            }
            loadRoutines()
        }
    }

    // MARK: - Completion

    fun toggleCompletion(item: UnifiedRoutineItem) {
        viewModelScope.launch {
            val todayStart = RoutineStorageService.startOfDay(System.currentTimeMillis())

            when (item) {
                is UnifiedRoutineItem.Weekly -> {
                    val routine = item.entity
                    val isCompleted = routineService.isCompleted(routine, todayStart)
                    if (isCompleted) {
                        routineService.unmarkCompleted(routine, todayStart)
                    } else {
                        routineService.markCompleted(routine, todayStart)
                        // Track routine completed (matching iOS)
                        AnalyticsManager.trackEvent(
                            AnalyticsEvent.ROUTINE_COMPLETED,
                            mapOf("routine_type" to routine.typeRaw)
                        )
                        // Cancel today's pending notification since the routine is done
                        if (routine.isNotificationEnabled) {
                            notificationManager.cancelTodayNotificationForRoutine(routine.id)
                        }
                    }
                }
                is UnifiedRoutineItem.FirstFriday -> {
                    val routine = item.entity
                    if (!FirstFridayRoutineService.isFirstFridayToday()) return@launch
                    val isCompleted = firstFridayService.isCompleted(routine, todayStart)
                    if (isCompleted) {
                        firstFridayService.unmarkCompleted(routine, todayStart)
                    } else {
                        firstFridayService.markCompleted(routine, todayStart)
                        // Track routine completed (matching iOS)
                        AnalyticsManager.trackEvent(
                            AnalyticsEvent.ROUTINE_COMPLETED,
                            mapOf("routine_type" to "firstFriday")
                        )
                    }
                }
            }
            loadRoutines()
        }
    }

    fun toggleCompletionForDate(routine: WeeklyRoutineEntity, dateLong: Long) {
        viewModelScope.launch {
            val isCompleted = routineService.isCompleted(routine, dateLong)
            if (isCompleted) {
                routineService.unmarkCompleted(routine, dateLong)
            } else {
                routineService.markCompleted(routine, dateLong)
                // Cancel today's pending notification if marking today as complete
                val todayStart = RoutineStorageService.startOfDay(System.currentTimeMillis())
                if (dateLong == todayStart && routine.isNotificationEnabled) {
                    notificationManager.cancelTodayNotificationForRoutine(routine.id)
                }
            }
            loadRoutines()
        }
    }

    fun toggleFirstFridayCompletionForDate(routine: FirstFridayRoutineEntity, dateLong: Long) {
        viewModelScope.launch {
            val isCompleted = firstFridayService.isCompleted(routine, dateLong)
            if (isCompleted) {
                firstFridayService.unmarkCompleted(routine, dateLong)
            } else {
                firstFridayService.markCompleted(routine, dateLong)
            }
            loadRoutines()
        }
    }

    // MARK: - First Friday

    fun createFirstFridayRoutine(
        isNotificationEnabled: Boolean,
        notificationHour: Int,
        notificationMinute: Int,
        notificationLeadTimeMinutes: Int,
        initialConsecutiveCount: Int
    ) {
        viewModelScope.launch {
            firstFridayService.createRoutine(
                isNotificationEnabled = isNotificationEnabled,
                notificationHour = notificationHour,
                notificationMinute = notificationMinute,
                notificationLeadTimeMinutes = notificationLeadTimeMinutes,
                initialConsecutiveCount = initialConsecutiveCount
            )
            loadRoutines()
        }
    }

    fun updateFirstFridayRoutine(
        routine: FirstFridayRoutineEntity,
        isNotificationEnabled: Boolean,
        notificationHour: Int,
        notificationMinute: Int,
        notificationLeadTimeMinutes: Int
    ) {
        viewModelScope.launch {
            firstFridayService.updateRoutine(
                routine = routine,
                isNotificationEnabled = isNotificationEnabled,
                notificationHour = notificationHour,
                notificationMinute = notificationMinute,
                notificationLeadTimeMinutes = notificationLeadTimeMinutes
            )
            loadRoutines()
        }
    }

    // MARK: - Detail View Data

    suspend fun getMonthProgressDetailed(routine: WeeklyRoutineEntity, monthOffset: Int): List<MonthProgressDay> {
        return routineService.monthProgressDetailed(routine, monthOffset)
    }

    suspend fun getWeekProgress(routine: WeeklyRoutineEntity, weekOffset: Int): List<WeekProgress> {
        return routineService.weekProgress(routine, weekOffset)
    }

    suspend fun getFirstFridayYearProgress(routine: FirstFridayRoutineEntity, yearOffset: Int): List<FirstFridayYearProgress> {
        return firstFridayService.yearProgress(routine, yearOffset)
    }

    // MARK: - Helpers

    fun isScheduledToday(routine: WeeklyRoutineEntity): Boolean {
        val todayDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val selectedDays = routineService.parseSelectedDays(routine)
        return todayDayOfWeek in selectedDays
    }

    fun scheduleSummary(routine: WeeklyRoutineEntity): String {
        return routineService.scheduleSummary(routine)
    }
}
