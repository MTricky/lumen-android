package com.app.lumen.features.calendar.service

import android.content.Context
import com.app.lumen.features.calendar.model.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Service for generating liturgical calendar data for display.
 * Generates LiturgicalDay objects for any date range, combining
 * season calculations with celebration data from JSON.
 */
class LiturgicalCalendarService(context: Context) {

    private val celebrationDataService = CelebrationDataService(context)
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    // In-memory cache: key = "year-region"
    private val yearCache = mutableMapOf<String, Map<String, LiturgicalDay>>()

    fun generateCalendar(year: Int, region: LiturgicalRegion): Map<String, LiturgicalDay> {
        val cacheKey = "$year-${region.rawValue}"
        yearCache[cacheKey]?.let { return it }

        val celebrationData = celebrationDataService.loadCelebrations()
        val days = mutableMapOf<String, LiturgicalDay>()

        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, Calendar.JANUARY)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 12)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        while (cal.get(Calendar.YEAR) == year) {
            val date = cal.time
            val liturgicalDay = createLiturgicalDay(date, region, year, celebrationData)
            days[dateFormatter.format(date)] = liturgicalDay
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        yearCache[cacheKey] = days
        return days
    }

    fun ensureCalendarExists(year: Int, region: LiturgicalRegion) {
        val cacheKey = "$year-${region.rawValue}"
        if (!yearCache.containsKey(cacheKey)) {
            generateCalendar(year, region)
        }
    }

    fun getLiturgicalDay(date: Date, region: LiturgicalRegion): LiturgicalDay? {
        val cal = Calendar.getInstance()
        cal.time = date
        val year = cal.get(Calendar.YEAR)
        ensureCalendarExists(year, region)
        val cacheKey = "$year-${region.rawValue}"
        return yearCache[cacheKey]?.get(dateFormatter.format(date))
    }

    fun getDaysForMonth(year: Int, month: Int, region: LiturgicalRegion): List<LiturgicalDay> {
        ensureCalendarExists(year, region)
        val cacheKey = "$year-${region.rawValue}"
        val allDays = yearCache[cacheKey] ?: return emptyList()

        return allDays.values
            .filter {
                val cal = Calendar.getInstance()
                cal.time = it.date
                cal.get(Calendar.MONTH) == month - 1 && cal.get(Calendar.YEAR) == year
            }
            .sortedBy { it.date }
    }

    fun clearCache() {
        yearCache.clear()
    }

    private fun createLiturgicalDay(
        date: Date,
        region: LiturgicalRegion,
        year: Int,
        celebrationData: CelebrationData
    ): LiturgicalDay {
        val cal = Calendar.getInstance()
        cal.time = date
        val month = cal.get(Calendar.MONTH) + 1 // 1-based
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val weekday = cal.get(Calendar.DAY_OF_WEEK)

        val seasonInfo = LiturgicalSeasonCalculator.seasonInfo(date)
        val dateKey = dateFormatter.format(date)
        val id = "$dateKey-${region.rawValue}"

        // Check for fixed celebration
        val fixedCelebration = celebrationDataService.fixedCelebration(month, day, region)
        if (fixedCelebration != null) {
            val allNames = celebrationDataService.allLanguageNames(fixedCelebration.key, month, day, region)
            return LiturgicalDay(
                id = id,
                date = date,
                region = region,
                season = seasonInfo.season,
                seasonWeek = seasonInfo.week,
                color = fixedCelebration.liturgicalColor,
                celebrationKey = fixedCelebration.key,
                celebrationNames = allNames,
                rank = fixedCelebration.celebrationRank,
                isHolyDayOfObligation = fixedCelebration.isHolyDay ?: false,
                isMoveable = false
            )
        }

        // Check for moveable celebration
        val moveableCelebration = findMoveableCelebration(date, region, year, celebrationData.moveable)
        if (moveableCelebration != null) {
            val allNames = celebrationDataService.allLanguageNamesForMoveable(moveableCelebration.key)
            return LiturgicalDay(
                id = id,
                date = date,
                region = region,
                season = seasonInfo.season,
                seasonWeek = seasonInfo.week,
                color = moveableCelebration.liturgicalColor,
                celebrationKey = moveableCelebration.key,
                celebrationNames = allNames,
                rank = moveableCelebration.celebrationRank,
                isHolyDayOfObligation = moveableCelebration.isHolyDay ?: false,
                isMoveable = true
            )
        }

        // Default weekday
        val isAshWeek = seasonInfo.season == LiturgicalSeason.LENT && seasonInfo.week == 0
        val weekdayNames = celebrationDataService.formatSeasonWeekAllLanguages(
            season = seasonInfo.season,
            week = seasonInfo.week,
            weekday = weekday,
            isAshWeek = isAshWeek
        )

        return LiturgicalDay(
            id = id,
            date = date,
            region = region,
            season = seasonInfo.season,
            seasonWeek = seasonInfo.week,
            color = seasonInfo.color,
            celebrationKey = "weekday",
            celebrationNames = weekdayNames,
            rank = CelebrationRank.WEEKDAY,
            isHolyDayOfObligation = false,
            isMoveable = false
        )
    }

    private fun findMoveableCelebration(
        date: Date,
        region: LiturgicalRegion,
        year: Int,
        moveableData: List<MoveableCelebrationJSON>
    ): MoveableCelebrationJSON? {
        val cal = Calendar.getInstance()
        val easter = EasterCalculator.easterSunday(year)

        for (feast in moveableData) {
            // Handle special calculations
            if (feast.specialCalculation != null) {
                if (feast.specialCalculation == "lastSundayBeforeAdvent") {
                    val christKing = EasterCalculator.christTheKing(year)
                    if (LiturgicalSeasonCalculator.isSameDay(date, christKing)) {
                        return feast
                    }
                }
                continue
            }

            // Calculate date from Easter
            var feastDate = EasterCalculator.addDays(easter, feast.daysFromEaster)

            // Handle transfer to Sunday
            if (feast.shouldTransferToSunday(region)) {
                cal.time = feastDate
                val weekday = cal.get(Calendar.DAY_OF_WEEK)
                if (weekday != Calendar.SUNDAY) {
                    val daysUntilSunday = (8 - weekday) % 7
                    feastDate = EasterCalculator.addDays(feastDate, if (daysUntilSunday == 0) 7 else daysUntilSunday)
                }
            }

            if (LiturgicalSeasonCalculator.isSameDay(date, feastDate)) {
                return feast
            }
        }

        return null
    }
}
