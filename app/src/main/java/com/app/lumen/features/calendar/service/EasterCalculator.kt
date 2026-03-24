package com.app.lumen.features.calendar.service

import java.util.Calendar
import java.util.Date

/**
 * Calculates Easter Sunday and all moveable feasts for any given year.
 * Uses the Anonymous Gregorian algorithm (Computus) used by the Western Church.
 */
object EasterCalculator {

    /**
     * Calculate Easter Sunday for a given year using the Anonymous Gregorian algorithm.
     * Accurate for years 1583 onwards (Gregorian calendar).
     */
    fun easterSunday(year: Int): Date {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1

        return createDate(year, month - 1, day) // Calendar months are 0-based
    }

    fun ashWednesday(year: Int): Date {
        val easter = easterSunday(year)
        return addDays(easter, -46)
    }

    fun pentecost(year: Int): Date {
        val easter = easterSunday(year)
        return addDays(easter, 49)
    }

    fun adventStart(year: Int): Date {
        val christmas = createDate(year, Calendar.DECEMBER, 25)
        val cal = Calendar.getInstance()
        cal.time = christmas

        val christmasWeekday = cal.get(Calendar.DAY_OF_WEEK)
        val daysToSubtract = if (christmasWeekday == Calendar.SUNDAY) 7 else christmasWeekday - Calendar.SUNDAY

        cal.add(Calendar.DAY_OF_MONTH, -daysToSubtract)
        // Sunday before Christmas, now go back 3 more weeks
        cal.add(Calendar.WEEK_OF_YEAR, -3)
        return cal.time
    }

    fun christTheKing(year: Int): Date {
        val advent = adventStart(year)
        return addDays(advent, -7)
    }

    fun baptismOfTheLord(year: Int): Date {
        val epiphany = createDate(year, Calendar.JANUARY, 6)
        val cal = Calendar.getInstance()
        cal.time = epiphany

        val epiphanyWeekday = cal.get(Calendar.DAY_OF_WEEK)

        return if (epiphanyWeekday == Calendar.SUNDAY) {
            // Epiphany is on Sunday, Baptism is Monday
            addDays(epiphany, 1)
        } else {
            // Baptism is the following Sunday
            val daysUntilSunday = (8 - epiphanyWeekday) % 7
            addDays(epiphany, if (daysUntilSunday == 0) 7 else daysUntilSunday)
        }
    }

    fun createDate(year: Int, month: Int, day: Int): Date {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.DAY_OF_MONTH, day)
        cal.set(Calendar.HOUR_OF_DAY, 12)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    fun addDays(date: Date, days: Int): Date {
        val cal = Calendar.getInstance()
        cal.time = date
        cal.add(Calendar.DAY_OF_MONTH, days)
        return cal.time
    }
}
