package com.app.lumen.features.calendar.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.lumen.features.calendar.model.*
import com.app.lumen.features.calendar.service.LiturgicalCalendarService
import com.app.lumen.features.calendar.service.LiturgicalSeasonCalculator
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

    private val _monthsData = MutableStateFlow<List<MonthData>>(emptyList())
    val monthsData: StateFlow<List<MonthData>> = _monthsData.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _region = MutableStateFlow(loadRegion())
    val region: StateFlow<LiturgicalRegion> = _region.asStateFlow()

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
