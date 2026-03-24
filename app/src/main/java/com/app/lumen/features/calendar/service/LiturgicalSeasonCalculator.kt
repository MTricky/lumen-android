package com.app.lumen.features.calendar.service

import com.app.lumen.features.calendar.model.LiturgicalColor
import com.app.lumen.features.calendar.model.LiturgicalSeason
import com.app.lumen.features.calendar.model.SeasonInfo
import java.util.Calendar
import java.util.Date

/**
 * Determines liturgical season, week number, and default color for any date.
 */
object LiturgicalSeasonCalculator {

    fun seasonInfo(date: Date): SeasonInfo {
        val cal = Calendar.getInstance()
        cal.time = date
        val year = cal.get(Calendar.YEAR)

        val adventStart = startOfDay(EasterCalculator.adventStart(year))
        val christmas = startOfDay(christmasDate(year))
        val baptismOfLord = startOfDay(EasterCalculator.baptismOfTheLord(year))
        val ashWednesday = startOfDay(EasterCalculator.ashWednesday(year))
        val easter = startOfDay(EasterCalculator.easterSunday(year))
        val pentecost = startOfDay(EasterCalculator.pentecost(year))
        val prevChristmas = startOfDay(christmasDate(year - 1))

        val dateStart = startOfDay(date)

        // ADVENT (current year)
        if (!dateStart.before(adventStart) && dateStart.before(christmas)) {
            val week = weeksBetween(adventStart, date) + 1
            val color = if (week == 3) LiturgicalColor.ROSE else LiturgicalColor.PURPLE
            return SeasonInfo(LiturgicalSeason.ADVENT, week.coerceAtMost(4), color)
        }

        // CHRISTMAS (from previous year - early January)
        val prevBaptism = startOfDay(EasterCalculator.baptismOfTheLord(year))
        if (!dateStart.before(prevChristmas) && !dateStart.after(prevBaptism)) {
            return christmasSeasonInfo(date, prevChristmas, year - 1)
        }

        // CHRISTMAS (current year - late December)
        if (!dateStart.before(christmas)) {
            val nextYearBaptism = startOfDay(EasterCalculator.baptismOfTheLord(year + 1))
            if (!dateStart.after(nextYearBaptism)) {
                return christmasSeasonInfo(date, christmas, year)
            }
        }

        // ORDINARY TIME I (after Baptism, before Ash Wednesday)
        if (dateStart.after(baptismOfLord) && dateStart.before(ashWednesday)) {
            val week = ordinaryTimeWeek(baptismOfLord, date)
            return SeasonInfo(LiturgicalSeason.ORDINARY_TIME, week, LiturgicalColor.GREEN)
        }

        // LENT (Ash Wednesday to Holy Thursday)
        val holyThursday = startOfDay(EasterCalculator.addDays(easter, -3))
        if (!dateStart.before(ashWednesday) && dateStart.before(holyThursday)) {
            val week = lentWeek(ashWednesday, date)
            val color = if (week == 4) LiturgicalColor.ROSE else LiturgicalColor.PURPLE
            return SeasonInfo(LiturgicalSeason.LENT, week, color)
        }

        // EASTER TRIDUUM (Holy Thursday to Easter)
        if (!dateStart.before(holyThursday) && dateStart.before(easter)) {
            return triduumInfo(date, easter)
        }

        // EASTER SEASON (Easter to Pentecost)
        if (!dateStart.before(easter) && !dateStart.after(pentecost)) {
            val week = weeksBetween(easter, date) + 1
            val isPentecost = isSameDay(date, pentecost)
            return SeasonInfo(
                LiturgicalSeason.EASTER,
                week.coerceAtMost(8),
                if (isPentecost) LiturgicalColor.RED else LiturgicalColor.WHITE
            )
        }

        // ORDINARY TIME II (after Pentecost to Advent)
        if (dateStart.after(pentecost) && dateStart.before(adventStart)) {
            val otIWeeks = ordinaryTimeWeek(baptismOfLord, ashWednesday) - 1
            val weeksAfterPentecost = weeksBetween(pentecost, date)
            val week = otIWeeks + weeksAfterPentecost + 1

            val christKing = EasterCalculator.christTheKing(year)
            if (isSameDay(date, christKing)) {
                return SeasonInfo(LiturgicalSeason.ORDINARY_TIME, 34, LiturgicalColor.WHITE)
            }

            return SeasonInfo(LiturgicalSeason.ORDINARY_TIME, week.coerceAtMost(34), LiturgicalColor.GREEN)
        }

        return SeasonInfo(LiturgicalSeason.ORDINARY_TIME, 1, LiturgicalColor.GREEN)
    }

    private fun christmasSeasonInfo(date: Date, christmasDate: Date, year: Int): SeasonInfo {
        val maryMotherOfGod = newYearsDay(year + 1)

        if (!date.after(maryMotherOfGod)) {
            return SeasonInfo(LiturgicalSeason.CHRISTMAS, 1, LiturgicalColor.WHITE)
        }

        val cal = Calendar.getInstance()
        cal.time = christmasDate
        val christmasMillis = cal.timeInMillis
        cal.time = date
        val dateMillis = cal.timeInMillis
        val daysFromChristmas = ((dateMillis - christmasMillis) / (24 * 60 * 60 * 1000)).toInt()
        val week = (daysFromChristmas / 7) + 1

        return SeasonInfo(LiturgicalSeason.CHRISTMAS, week.coerceAtMost(2), LiturgicalColor.WHITE)
    }

    private fun triduumInfo(date: Date, easter: Date): SeasonInfo {
        val holyThursday = EasterCalculator.addDays(easter, -3)
        val goodFriday = EasterCalculator.addDays(easter, -2)
        val holySaturday = EasterCalculator.addDays(easter, -1)

        return when {
            isSameDay(date, holyThursday) -> SeasonInfo(LiturgicalSeason.LENT, 6, LiturgicalColor.WHITE, "Holy Thursday")
            isSameDay(date, goodFriday) -> SeasonInfo(LiturgicalSeason.LENT, 6, LiturgicalColor.RED, "Good Friday")
            isSameDay(date, holySaturday) -> SeasonInfo(LiturgicalSeason.LENT, 6, LiturgicalColor.WHITE, "Holy Saturday")
            else -> SeasonInfo(LiturgicalSeason.LENT, 6, LiturgicalColor.PURPLE)
        }
    }

    private fun weeksBetween(start: Date, end: Date): Int {
        val startSunday = previousSunday(start)
        val cal = Calendar.getInstance()
        cal.time = startSunday
        val startMillis = cal.timeInMillis
        cal.time = end
        val endMillis = cal.timeInMillis
        val diffDays = ((endMillis - startMillis) / (24 * 60 * 60 * 1000)).toInt()
        return maxOf(0, diffDays / 7)
    }

    private fun lentWeek(ashWednesday: Date, date: Date): Int {
        val cal = Calendar.getInstance()
        cal.time = ashWednesday
        val ashWedWeekday = cal.get(Calendar.DAY_OF_WEEK)
        val daysToSunday = (8 - ashWedWeekday) % 7
        val firstSundayOfLent = EasterCalculator.addDays(ashWednesday, if (daysToSunday == 0) 7 else daysToSunday)

        if (date.before(firstSundayOfLent)) return 0

        val weeks = weeksBetween(firstSundayOfLent, date) + 1
        return weeks.coerceAtMost(6)
    }

    private fun ordinaryTimeWeek(start: Date, date: Date): Int {
        val cal = Calendar.getInstance()
        cal.time = start
        val startWeekday = cal.get(Calendar.DAY_OF_WEEK)
        val daysToSunday = if (startWeekday == Calendar.SUNDAY) 0 else (8 - startWeekday)
        val firstSunday = EasterCalculator.addDays(start, daysToSunday)

        if (date.before(firstSunday)) return 1

        val weeks = weeksBetween(firstSunday, date) + 1
        return maxOf(1, weeks)
    }

    private fun previousSunday(date: Date): Date {
        val cal = Calendar.getInstance()
        cal.time = date
        val weekday = cal.get(Calendar.DAY_OF_WEEK)
        val daysToSubtract = if (weekday == Calendar.SUNDAY) 0 else weekday - Calendar.SUNDAY
        return EasterCalculator.addDays(date, -daysToSubtract)
    }

    private fun christmasDate(year: Int): Date = EasterCalculator.createDate(year, Calendar.DECEMBER, 25)

    private fun newYearsDay(year: Int): Date = EasterCalculator.createDate(year, Calendar.JANUARY, 1)

    fun startOfDay(date: Date): Date {
        val cal = Calendar.getInstance()
        cal.time = date
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
