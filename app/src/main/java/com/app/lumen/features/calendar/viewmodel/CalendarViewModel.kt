package com.app.lumen.features.calendar.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.lumen.features.calendar.model.*
import com.app.lumen.features.calendar.service.LiturgicalCalendarService
import com.app.lumen.features.calendar.service.LumenNotificationManager
import com.app.lumen.features.calendar.service.NotesStorageService
import com.app.lumen.features.calendar.service.ReminderPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val calendarService = LiturgicalCalendarService(application)
    private val prefs = application.getSharedPreferences("calendar_settings", Context.MODE_PRIVATE)
    val storageService = NotesStorageService(application)
    val reminderPreferences = ReminderPreferences(application)
    val notificationManager = LumenNotificationManager(application)

    private val _monthsData = MutableStateFlow<List<MonthData>>(emptyList())
    val monthsData: StateFlow<List<MonthData>> = _monthsData.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _region = MutableStateFlow(loadRegion())
    val region: StateFlow<LiturgicalRegion> = _region.asStateFlow()

    // Notes & Reminders state
    private val _allNotes = MutableStateFlow<List<Note>>(emptyList())
    val allNotes: StateFlow<List<Note>> = _allNotes.asStateFlow()

    private val _upcomingReminders = MutableStateFlow<List<Reminder>>(emptyList())
    val upcomingReminders: StateFlow<List<Reminder>> = _upcomingReminders.asStateFlow()

    private val _notesLoading = MutableStateFlow(true)
    val notesLoading: StateFlow<Boolean> = _notesLoading.asStateFlow()

    // Day detail data
    private val _dayNotes = MutableStateFlow<List<Note>>(emptyList())
    val dayNotes: StateFlow<List<Note>> = _dayNotes.asStateFlow()

    private val _dayReminders = MutableStateFlow<List<Reminder>>(emptyList())
    val dayReminders: StateFlow<List<Reminder>> = _dayReminders.asStateFlow()

    // Months before = current month index + 11 (back to Jan of previous year)
    private val monthsBefore: Int
        get() {
            val currentMonth = Calendar.getInstance().get(Calendar.MONTH) // 0-based
            return currentMonth + 12 // +12 to go back to Jan of prev year
        }
    private val monthsAfter = 24

    val todayMonthIndex: Int
        get() = monthsBefore

    init {
        loadCalendarData()
        loadNotesData()
    }

    fun loadCalendarData() {
        viewModelScope.launch(Dispatchers.Default) {
            _isLoading.value = true
            val region = _region.value

            // Generate calendar data for needed years
            val today = Date()
            val cal = Calendar.getInstance()
            val yearsNeeded = mutableSetOf<Int>()

            for (offset in -monthsBefore..monthsAfter) {
                cal.time = today
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.add(Calendar.MONTH, offset)
                yearsNeeded.add(cal.get(Calendar.YEAR))
            }

            for (year in yearsNeeded.sorted()) {
                calendarService.ensureCalendarExists(year, region)
            }

            // Generate month data
            val months = mutableListOf<MonthData>()
            cal.time = today
            cal.set(Calendar.DAY_OF_MONTH, 1)
            val startOfCurrentMonth = cal.time

            for (offset in -monthsBefore..monthsAfter) {
                cal.time = startOfCurrentMonth
                cal.add(Calendar.MONTH, offset)

                val year = cal.get(Calendar.YEAR)
                val month = cal.get(Calendar.MONTH) + 1 // 1-based for our service
                val monthDate = cal.time

                val liturgicalDays = calendarService.getDaysForMonth(year, month, region)
                val displayDays = buildDisplayDays(year, month, liturgicalDays)

                val monthId = "${year}-${String.format("%02d", month)}"
                months.add(
                    MonthData(
                        id = monthId,
                        date = monthDate,
                        year = year,
                        month = month,
                        days = displayDays
                    )
                )
            }

            _monthsData.value = months
            _isLoading.value = false
        }
    }

    fun setRegion(region: LiturgicalRegion) {
        _region.value = region
        prefs.edit().putString("region", region.displayName).apply()
        calendarService.clearCache()
        loadCalendarData()
    }

    /**
     * Re-read region from SharedPreferences (e.g. after Settings changes it).
     * If region changed, reload calendar data.
     */
    fun refreshRegionFromPrefs() {
        val current = loadRegion()
        if (current != _region.value) {
            _region.value = current
            calendarService.clearCache()
            loadCalendarData()
        }
    }

    // MARK: - Notes & Reminders

    fun loadNotesData() {
        viewModelScope.launch(Dispatchers.IO) {
            _notesLoading.value = true
            _allNotes.value = storageService.allNotes()
            _upcomingReminders.value = storageService.upcomingReminders()
            storageService.cleanupPastReminders()
            _notesLoading.value = false
        }
    }

    fun loadDayData(date: Date) {
        viewModelScope.launch(Dispatchers.IO) {
            _dayNotes.value = storageService.notesForDate(date)
            _dayReminders.value = storageService.remindersForDate(date)
        }
    }

    fun createNote(type: NoteType, date: Date, title: String, content: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            storageService.createNote(date, type, title, content)
            loadNotesData()
            loadDayData(date)
        }
    }

    fun updateNote(id: String, title: String, content: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            storageService.updateNote(id, title, content)
            loadNotesData()
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            storageService.deleteNote(note.id)
            loadNotesData()
        }
    }

    fun createReminder(
        date: Date,
        type: ReminderType,
        title: String,
        message: String?,
        triggerTime: Date,
        notes: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val reminder = storageService.createReminder(date, type, title, message, triggerTime, notes)
            reminderPreferences.saveLastUsedTime(triggerTime, type)
            notificationManager.scheduleNotification(reminder)
            loadNotesData()
            loadDayData(date)
        }
    }

    fun updateReminder(
        id: String,
        title: String,
        message: String?,
        triggerTime: Date,
        notes: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = storageService.updateReminder(id, title, message, triggerTime, notes)
            if (updated != null) {
                reminderPreferences.saveLastUsedTime(triggerTime, updated.type)
                notificationManager.updateNotification(updated)
            }
            loadNotesData()
        }
    }

    fun getLiturgicalDay(date: Date): LiturgicalDay? {
        return calendarService.getLiturgicalDay(date, _region.value)
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch(Dispatchers.IO) {
            storageService.deleteReminder(reminder.id)
            notificationManager.cancelNotification(reminder.notificationId)
            loadNotesData()
        }
    }

    private fun loadRegion(): LiturgicalRegion {
        val regionName = prefs.getString("region", "Poland") ?: "Poland"
        return LiturgicalRegion.fromDisplayName(regionName)
    }

    private fun buildDisplayDays(
        year: Int,
        month: Int,
        liturgicalDays: List<LiturgicalDay>
    ): List<CalendarDayDisplay> {
        val cal = Calendar.getInstance()
        val today = Calendar.getInstance()

        // Create a lookup map for liturgical days
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayMap = liturgicalDays.associateBy { dateFormatter.format(it.date) }

        // Get first day of month
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        cal.set(Calendar.DAY_OF_MONTH, 1)

        // Monday-based: Monday=0, Tuesday=1, ..., Sunday=6
        val firstWeekday = cal.get(Calendar.DAY_OF_WEEK)
        val mondayBasedOffset = (firstWeekday + 5) % 7 // Convert Sunday=1..Saturday=7 to Monday=0..Sunday=6

        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val days = mutableListOf<CalendarDayDisplay>()

        // Add placeholders for days before the 1st
        for (i in 0 until mondayBasedOffset) {
            days.add(CalendarDayDisplay.placeholder())
        }

        // Add actual days
        for (day in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            cal.set(Calendar.HOUR_OF_DAY, 12)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val date = cal.time

            val isToday = cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)

            val dateKey = dateFormatter.format(date)
            val liturgicalDay = dayMap[dateKey]

            days.add(
                CalendarDayDisplay(
                    id = dateKey,
                    date = date,
                    dayNumber = day,
                    isCurrentMonth = true,
                    isToday = isToday,
                    liturgicalDay = liturgicalDay
                )
            )
        }

        return days
    }
}
