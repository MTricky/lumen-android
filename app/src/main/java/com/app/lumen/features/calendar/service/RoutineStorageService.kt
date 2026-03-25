package com.app.lumen.features.calendar.service

import android.content.Context
import com.app.lumen.features.calendar.data.RoutineCompletionEntity
import com.app.lumen.features.calendar.data.RoutineDataStore
import com.app.lumen.features.calendar.data.WeeklyRoutineEntity
import com.app.lumen.features.calendar.model.RoutineItemType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar
import java.util.UUID

data class WeekProgress(
    val date: Long, // start of day
    val dayOfWeek: Int, // Calendar.MONDAY..SUNDAY
    val isScheduled: Boolean,
    val isCompleted: Boolean,
    val isBeforeCreation: Boolean
)

data class MonthProgressDay(
    val date: Long,
    val dayOfMonth: Int,
    val isCompleted: Boolean,
    val isPast: Boolean,
    val isFuture: Boolean,
    val isBeforeCreation: Boolean
)

class RoutineStorageService(context: Context) {

    private val store = RoutineDataStore.getInstance(context)
    private val json = Json { ignoreUnknownKeys = true }
    private val notificationManager = LumenNotificationManager(context)

    // MARK: - CRUD

    suspend fun createRoutine(
        title: String,
        type: RoutineItemType,
        selectedDays: Set<Int>,
        hour: Int,
        minute: Int,
        isNotificationEnabled: Boolean,
        isLoggingEnabled: Boolean,
        notificationLeadTimeMinutes: Int
    ): WeeklyRoutineEntity {
        val id = UUID.randomUUID().toString()
        val sortOrder = (store.maxSortOrder() ?: -1) + 1
        val notificationIds = selectedDays.map { "$id-day$it" }

        val entity = WeeklyRoutineEntity(
            id = id,
            title = title,
            typeRaw = type.rawValue,
            selectedDaysJson = json.encodeToString(selectedDays.toList()),
            hour = hour,
            minute = minute,
            isNotificationEnabled = isNotificationEnabled,
            notificationIdentifiersJson = json.encodeToString(notificationIds),
            isActive = true,
            isLoggingEnabled = isLoggingEnabled,
            notificationLeadTimeMinutes = notificationLeadTimeMinutes,
            sortOrder = sortOrder,
            createdAt = System.currentTimeMillis()
        )
        store.insertRoutine(entity)

        if (isNotificationEnabled) {
            scheduleNotifications(entity)
        }

        return entity
    }

    suspend fun updateRoutine(
        routine: WeeklyRoutineEntity,
        title: String,
        selectedDays: Set<Int>,
        hour: Int,
        minute: Int,
        isNotificationEnabled: Boolean,
        isLoggingEnabled: Boolean,
        notificationLeadTimeMinutes: Int
    ): WeeklyRoutineEntity {
        cancelNotifications(routine)

        val notificationIds = selectedDays.map { "${routine.id}-day$it" }
        val updated = routine.copy(
            title = title,
            selectedDaysJson = json.encodeToString(selectedDays.toList()),
            hour = hour,
            minute = minute,
            isNotificationEnabled = isNotificationEnabled,
            notificationIdentifiersJson = json.encodeToString(notificationIds),
            isLoggingEnabled = isLoggingEnabled,
            notificationLeadTimeMinutes = notificationLeadTimeMinutes
        )
        store.updateRoutine(updated)

        if (isNotificationEnabled && updated.isActive) {
            scheduleNotifications(updated)
        }

        return updated
    }

    suspend fun deleteRoutine(routine: WeeklyRoutineEntity) {
        cancelNotifications(routine)
        store.deleteRoutine(routine)
    }

    suspend fun pauseRoutine(routine: WeeklyRoutineEntity) {
        cancelNotifications(routine)
        store.updateRoutine(routine.copy(isActive = false))
    }

    suspend fun resumeRoutine(routine: WeeklyRoutineEntity) {
        val updated = routine.copy(isActive = true)
        store.updateRoutine(updated)
        if (updated.isNotificationEnabled) {
            scheduleNotifications(updated)
        }
    }

    suspend fun allRoutines(): List<WeeklyRoutineEntity> = store.allRoutines()

    suspend fun activeRoutines(): List<WeeklyRoutineEntity> = store.activeRoutines()

    suspend fun pausedRoutines(): List<WeeklyRoutineEntity> = store.pausedRoutines()

    suspend fun routineById(id: String): WeeklyRoutineEntity? = store.routineById(id)

    // MARK: - Completion Tracking

    suspend fun markCompleted(routine: WeeklyRoutineEntity, dateLong: Long) {
        val completion = RoutineCompletionEntity(
            id = UUID.randomUUID().toString(),
            routineId = routine.id,
            dateLong = dateLong,
            completedAt = System.currentTimeMillis()
        )
        store.insertCompletion(completion)
    }

    suspend fun unmarkCompleted(routine: WeeklyRoutineEntity, dateLong: Long) {
        store.deleteCompletion(routine.id, dateLong)
    }

    suspend fun isCompleted(routine: WeeklyRoutineEntity, dateLong: Long): Boolean {
        return store.completionForDate(routine.id, dateLong) != null
    }

    // MARK: - Streak Calculation

    suspend fun currentStreak(routine: WeeklyRoutineEntity): Int {
        val selectedDays = parseSelectedDays(routine)
        if (selectedDays.isEmpty()) return 0

        val completions = store.completionsForRoutine(routine.id)
        val completionDates = completions.map { it.dateLong }.toSet()

        val cal = Calendar.getInstance()
        val todayStart = startOfDay(cal.timeInMillis)
        var streak = 0
        var currentDate = todayStart

        // Check up to 365 days back
        for (i in 0 until 365) {
            cal.timeInMillis = currentDate
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun..7=Sat

            if (dayOfWeek in selectedDays) {
                if (currentDate < routine.createdAt) break // before routine was created

                if (completionDates.contains(currentDate)) {
                    streak++
                } else if (currentDate < todayStart) {
                    // Past scheduled day that was missed - break streak
                    break
                }
                // If today and not yet completed, don't break (can still complete)
            }

            cal.add(Calendar.DAY_OF_YEAR, -1)
            currentDate = startOfDay(cal.timeInMillis)
        }

        return streak
    }

    // MARK: - Progress

    suspend fun weekProgress(routine: WeeklyRoutineEntity, weekOffset: Int = 0): List<WeekProgress> {
        val selectedDays = parseSelectedDays(routine)
        val completions = store.completionsForRoutine(routine.id)
        val completionDates = completions.map { it.dateLong }.toSet()

        val cal = Calendar.getInstance()
        cal.add(Calendar.WEEK_OF_YEAR, weekOffset)
        // Go to Monday of the week
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val todayStart = startOfDay(System.currentTimeMillis())

        return (0 until 7).map { dayOffset ->
            val dayCal = cal.clone() as Calendar
            dayCal.add(Calendar.DAY_OF_YEAR, dayOffset)
            val dayStart = dayCal.timeInMillis
            val dayOfWeek = dayCal.get(Calendar.DAY_OF_WEEK)

            WeekProgress(
                date = dayStart,
                dayOfWeek = dayOfWeek,
                isScheduled = dayOfWeek in selectedDays,
                isCompleted = completionDates.contains(dayStart),
                isBeforeCreation = dayStart < startOfDay(routine.createdAt)
            )
        }
    }

    suspend fun monthProgress(routine: WeeklyRoutineEntity): Pair<Int, Int> {
        val selectedDays = parseSelectedDays(routine)
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)
        val todayStart = startOfDay(cal.timeInMillis)

        val completions = store.completionsForRoutine(routine.id)
        val completionDates = completions.map { it.dateLong }.toSet()

        var completed = 0
        var scheduled = 0

        cal.set(Calendar.DAY_OF_MONTH, 1)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (day in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            val dayStart = startOfDay(cal.timeInMillis)
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

            if (dayOfWeek in selectedDays && dayStart >= startOfDay(routine.createdAt) && dayStart <= todayStart) {
                scheduled++
                if (completionDates.contains(dayStart)) {
                    completed++
                }
            }
        }

        return completed to scheduled
    }

    suspend fun monthProgressDetailed(routine: WeeklyRoutineEntity, monthOffset: Int = 0): List<MonthProgressDay> {
        val selectedDays = parseSelectedDays(routine)
        val completions = store.completionsForRoutine(routine.id)
        val completionDates = completions.map { it.dateLong }.toSet()

        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, monthOffset)
        val todayStart = startOfDay(System.currentTimeMillis())

        val result = mutableListOf<MonthProgressDay>()

        cal.set(Calendar.DAY_OF_MONTH, 1)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (day in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            val dayStart = startOfDay(cal.timeInMillis)
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

            if (dayOfWeek in selectedDays) {
                result.add(
                    MonthProgressDay(
                        date = dayStart,
                        dayOfMonth = day,
                        isCompleted = completionDates.contains(dayStart),
                        isPast = dayStart < todayStart,
                        isFuture = dayStart > todayStart,
                        isBeforeCreation = dayStart < startOfDay(routine.createdAt)
                    )
                )
            }
        }

        return result
    }

    // MARK: - Notifications

    fun scheduleNotifications(routine: WeeklyRoutineEntity) {
        val selectedDays = parseSelectedDays(routine)
        val type = RoutineItemType.fromRawValue(routine.typeRaw)

        for (day in selectedDays) {
            val identifier = "${routine.id}-day$day"
            notificationManager.scheduleWeeklyNotification(
                identifier = identifier,
                title = type.notificationTitle,
                body = routine.title,
                weekday = day,
                hour = routine.hour,
                minute = routine.minute,
                leadTimeMinutes = routine.notificationLeadTimeMinutes
            )
        }
    }

    fun cancelNotifications(routine: WeeklyRoutineEntity) {
        val identifiers: List<String> = try {
            json.decodeFromString(routine.notificationIdentifiersJson)
        } catch (e: Exception) {
            emptyList()
        }
        for (id in identifiers) {
            notificationManager.cancelNotification(id)
        }
    }

    suspend fun rescheduleAllNotifications() {
        val routines = store.activeRoutines()
        for (routine in routines) {
            if (routine.isNotificationEnabled) {
                scheduleNotifications(routine)
            }
        }
    }

    // MARK: - Cleanup

    suspend fun cleanupOldCompletions() {
        val sixtyDaysAgo = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -60)
        }.timeInMillis
        store.deleteOldCompletions(sixtyDaysAgo)
    }

    // MARK: - Sort Order

    suspend fun updateSortOrders(routineIds: List<String>) {
        routineIds.forEachIndexed { index, id ->
            store.routineById(id)?.let { routine ->
                store.updateRoutine(routine.copy(sortOrder = index))
            }
        }
    }

    // MARK: - Helpers

    fun parseSelectedDays(routine: WeeklyRoutineEntity): Set<Int> {
        return try {
            json.decodeFromString<List<Int>>(routine.selectedDaysJson).toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    fun scheduleSummary(routine: WeeklyRoutineEntity): String {
        val days = parseSelectedDays(routine)
        val timeStr = formatTime(routine.hour, routine.minute)

        return when {
            days.size == 7 -> "Every day at $timeStr"
            days == setOf(2, 3, 4, 5, 6) -> "Weekdays at $timeStr"
            days == setOf(1, 7) -> "Weekends at $timeStr"
            days.size == 1 -> {
                val dayName = dayName(days.first())
                "$dayName at $timeStr"
            }
            else -> {
                val dayNames = days.sorted().joinToString(", ") { dayAbbrev(it) }
                "$dayNames at $timeStr"
            }
        }
    }

    private fun dayName(day: Int): String = when (day) {
        Calendar.SUNDAY -> "Sun"
        Calendar.MONDAY -> "Mon"
        Calendar.TUESDAY -> "Tue"
        Calendar.WEDNESDAY -> "Wed"
        Calendar.THURSDAY -> "Thu"
        Calendar.FRIDAY -> "Fri"
        Calendar.SATURDAY -> "Sat"
        else -> ""
    }

    private fun dayAbbrev(day: Int): String = dayName(day)

    private fun formatTime(hour: Int, minute: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        val formatter = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        return formatter.format(cal.time)
    }

    companion object {
        fun startOfDay(millis: Long): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = millis
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }
    }
}
