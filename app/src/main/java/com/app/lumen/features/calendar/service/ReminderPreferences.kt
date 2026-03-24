package com.app.lumen.features.calendar.service

import android.content.Context
import com.app.lumen.features.calendar.model.ReminderType
import java.util.Calendar
import java.util.Date

class ReminderPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("reminder_preferences", Context.MODE_PRIVATE)

    fun lastUsedTime(type: ReminderType): Date {
        val key = "reminder.lastTime.${type.rawValue}"
        val savedMillis = prefs.getLong(key, -1L)

        if (savedMillis != -1L) {
            val savedDate = Date(savedMillis)
            val cal = Calendar.getInstance()
            val savedCal = Calendar.getInstance()
            savedCal.time = savedDate

            cal.set(Calendar.HOUR_OF_DAY, savedCal.get(Calendar.HOUR_OF_DAY))
            cal.set(Calendar.MINUTE, savedCal.get(Calendar.MINUTE))
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.time
        }

        // Return default time for this type
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, type.defaultHour)
        cal.set(Calendar.MINUTE, type.defaultMinute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    fun saveLastUsedTime(time: Date, type: ReminderType) {
        val key = "reminder.lastTime.${type.rawValue}"
        prefs.edit().putLong(key, time.time).apply()
    }

    fun triggerTime(date: Date, type: ReminderType): Date {
        val lastTime = lastUsedTime(type)
        val timeCal = Calendar.getInstance().apply { time = lastTime }
        val dateCal = Calendar.getInstance().apply { this.time = date }

        dateCal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY))
        dateCal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE))
        dateCal.set(Calendar.SECOND, 0)
        dateCal.set(Calendar.MILLISECOND, 0)
        return dateCal.time
    }
}
