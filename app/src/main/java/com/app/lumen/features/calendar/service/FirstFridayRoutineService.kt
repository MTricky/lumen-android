package com.app.lumen.features.calendar.service

import android.content.Context
import com.app.lumen.features.calendar.data.FirstFridayCompletionEntity
import com.app.lumen.features.calendar.data.FirstFridayRoutineEntity
import com.app.lumen.features.calendar.data.RoutineDataStore
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar
import java.util.UUID

data class FirstFridayYearProgress(
    val date: Long,
    val monthIndex: Int, // 0-11
    val isCompleted: Boolean,
    val isPreChecked: Boolean, // from initialConsecutiveCount
    val isPast: Boolean,
    val isFuture: Boolean
)

class FirstFridayRoutineService(context: Context) {

    private val store = RoutineDataStore.getInstance(context)
    private val json = Json { ignoreUnknownKeys = true }
    private val notificationManager = LumenNotificationManager(context)

    // MARK: - CRUD

    suspend fun createRoutine(
        isNotificationEnabled: Boolean,
        notificationHour: Int,
        notificationMinute: Int,
        notificationLeadTimeMinutes: Int,
        initialConsecutiveCount: Int
    ): FirstFridayRoutineEntity {
        // Only one First Friday routine allowed
        store.getFirstFridayRoutine()?.let { existing ->
            store.deleteFirstFridayRoutine(existing)
        }

        val id = UUID.randomUUID().toString()
        val notificationIds = (0 until 12).map { "$id-firstfriday-$it" }

        val entity = FirstFridayRoutineEntity(
            id = id,
            isActive = true,
            isNotificationEnabled = isNotificationEnabled,
            notificationHour = notificationHour,
            notificationMinute = notificationMinute,
            notificationLeadTimeMinutes = notificationLeadTimeMinutes,
            notificationIdentifiersJson = json.encodeToString(notificationIds),
            initialConsecutiveCount = initialConsecutiveCount,
            sortOrder = 0,
            createdAt = System.currentTimeMillis()
        )
        store.insertFirstFridayRoutine(entity)

        if (isNotificationEnabled) {
            scheduleNotifications(entity)
        }

        return entity
    }

    suspend fun getRoutine(): FirstFridayRoutineEntity? = store.getFirstFridayRoutine()

    suspend fun hasRoutine(): Boolean = store.getFirstFridayRoutine() != null

    suspend fun updateRoutine(
        routine: FirstFridayRoutineEntity,
        isNotificationEnabled: Boolean,
        notificationHour: Int,
        notificationMinute: Int,
        notificationLeadTimeMinutes: Int
    ): FirstFridayRoutineEntity {
        cancelNotifications(routine)

        val updated = routine.copy(
            isNotificationEnabled = isNotificationEnabled,
            notificationHour = notificationHour,
            notificationMinute = notificationMinute,
            notificationLeadTimeMinutes = notificationLeadTimeMinutes
        )
        store.updateFirstFridayRoutine(updated)

        if (isNotificationEnabled && updated.isActive) {
            scheduleNotifications(updated)
        }

        return updated
    }

    suspend fun deleteRoutine(routine: FirstFridayRoutineEntity) {
        cancelNotifications(routine)
        store.deleteFirstFridayRoutine(routine)
    }

    suspend fun pauseRoutine(routine: FirstFridayRoutineEntity) {
        cancelNotifications(routine)
        store.updateFirstFridayRoutine(routine.copy(isActive = false))
    }

    suspend fun resumeRoutine(routine: FirstFridayRoutineEntity) {
        val updated = routine.copy(isActive = true)
        store.updateFirstFridayRoutine(updated)
        if (updated.isNotificationEnabled) {
            scheduleNotifications(updated)
        }
    }

    // MARK: - Completion Tracking

    suspend fun markCompleted(routine: FirstFridayRoutineEntity, dateLong: Long, notes: String? = null) {
        val completion = FirstFridayCompletionEntity(
            id = UUID.randomUUID().toString(),
            routineId = routine.id,
            dateLong = dateLong,
            notes = notes,
            completedAt = System.currentTimeMillis()
        )
        store.insertFirstFridayCompletion(completion)
    }

    suspend fun unmarkCompleted(routine: FirstFridayRoutineEntity, dateLong: Long) {
        store.deleteFirstFridayCompletion(routine.id, dateLong)
    }

    suspend fun isCompleted(routine: FirstFridayRoutineEntity, dateLong: Long): Boolean {
        return store.firstFridayCompletionForDate(routine.id, dateLong) != null
    }

    // MARK: - Consecutive Count

    suspend fun currentConsecutiveCount(routine: FirstFridayRoutineEntity): Int {
        val completions = store.firstFridayCompletions(routine.id)
            .sortedBy { it.dateLong }

        if (completions.isEmpty()) return routine.initialConsecutiveCount

        var count = routine.initialConsecutiveCount
        var expectedDate: Long? = null

        for (completion in completions) {
            if (expectedDate != null) {
                val expected = RoutineStorageService.startOfDay(expectedDate)
                val actual = RoutineStorageService.startOfDay(completion.dateLong)

                if (actual != expected) {
                    // Chain broken - reset
                    count = 1
                    expectedDate = nextFirstFridayAfter(completion.dateLong)
                    continue
                }
            }

            count++
            expectedDate = nextFirstFridayAfter(completion.dateLong)
        }

        return count
    }

    // MARK: - Year Progress

    suspend fun yearProgress(routine: FirstFridayRoutineEntity, yearOffset: Int = 0): List<FirstFridayYearProgress> {
        val completions = store.firstFridayCompletions(routine.id)
        val completionDates = completions.map { RoutineStorageService.startOfDay(it.dateLong) }.toSet()
        val todayStart = RoutineStorageService.startOfDay(System.currentTimeMillis())

        val cal = Calendar.getInstance()
        cal.add(Calendar.YEAR, yearOffset)
        val year = cal.get(Calendar.YEAR)

        val result = mutableListOf<FirstFridayYearProgress>()

        for (month in 0..11) {
            val firstFriday = firstFridayOfMonth(year, month)
            val firstFridayStart = RoutineStorageService.startOfDay(firstFriday)

            // Determine if this was pre-checked based on initialConsecutiveCount
            // Pre-checked First Fridays are the ones before the routine creation
            val isPreChecked = firstFridayStart < RoutineStorageService.startOfDay(routine.createdAt)
                    && routine.initialConsecutiveCount > 0

            result.add(
                FirstFridayYearProgress(
                    date = firstFridayStart,
                    monthIndex = month,
                    isCompleted = completionDates.contains(firstFridayStart),
                    isPreChecked = isPreChecked,
                    isPast = firstFridayStart < todayStart,
                    isFuture = firstFridayStart > todayStart
                )
            )
        }

        return result
    }

    // MARK: - Notifications

    fun scheduleNotifications(routine: FirstFridayRoutineEntity) {
        val upcomingFridays = upcomingFirstFridays(12)

        upcomingFridays.forEachIndexed { index, fridayMillis ->
            val identifier = "${routine.id}-firstfriday-$index"

            // Calculate notification time: First Friday date at notification hour/minute, minus lead time
            val cal = Calendar.getInstance()
            cal.timeInMillis = fridayMillis
            cal.set(Calendar.HOUR_OF_DAY, routine.notificationHour)
            cal.set(Calendar.MINUTE, routine.notificationMinute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            // Subtract lead time
            cal.add(Calendar.MINUTE, -routine.notificationLeadTimeMinutes)

            val triggerTime = cal.timeInMillis
            if (triggerTime > System.currentTimeMillis()) {
                notificationManager.scheduleOneTimeNotification(
                    identifier = identifier,
                    title = "First Friday Devotion",
                    body = "Receive Holy Communion in reparation to the Sacred Heart",
                    triggerTimeMillis = triggerTime
                )
            }
        }
    }

    fun cancelNotifications(routine: FirstFridayRoutineEntity) {
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
        val routine = store.getFirstFridayRoutine() ?: return
        if (routine.isActive && routine.isNotificationEnabled) {
            scheduleNotifications(routine)
        }
    }

    // MARK: - Static Helpers

    companion object {
        fun isFirstFriday(millis: Long): Boolean {
            val cal = Calendar.getInstance()
            cal.timeInMillis = millis
            return cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY && cal.get(Calendar.DAY_OF_MONTH) <= 7
        }

        fun isFirstFridayToday(): Boolean = isFirstFriday(System.currentTimeMillis())

        fun nextFirstFridayAfter(millis: Long): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = millis
            cal.add(Calendar.DAY_OF_YEAR, 1) // start from next day

            // Find next month's first Friday
            cal.set(Calendar.DAY_OF_MONTH, 1)
            if (cal.get(Calendar.DAY_OF_MONTH) > 7 || !isFirstFriday(cal.timeInMillis)) {
                // Move to next month
                cal.add(Calendar.MONTH, 1)
                cal.set(Calendar.DAY_OF_MONTH, 1)
            }

            // Find first Friday of that month
            while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }

            return RoutineStorageService.startOfDay(cal.timeInMillis)
        }

        fun upcomingFirstFridays(count: Int = 12): List<Long> {
            val result = mutableListOf<Long>()
            val cal = Calendar.getInstance()

            for (i in 0 until count + 2) { // overshoot to ensure enough
                val month = cal.get(Calendar.MONTH) + i
                val year = cal.get(Calendar.YEAR) + (month / 12)
                val actualMonth = month % 12

                val ff = firstFridayOfMonth(year, actualMonth)
                if (ff > System.currentTimeMillis() - 86400000) { // include today
                    result.add(ff)
                }
                if (result.size >= count) break
            }

            return result.take(count)
        }

        fun firstFridayOfMonth(year: Int, month: Int): Long {
            val cal = Calendar.getInstance()
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }

            return cal.timeInMillis
        }
    }
}
