package com.app.lumen.features.calendar.service

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.app.lumen.R
import com.app.lumen.features.calendar.model.Reminder
import java.util.Calendar

class LumenNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "lumen_reminders_v2"
        const val CHANNEL_NAME = "Reminders"
        const val ROUTINE_CHANNEL_ID = "lumen_routines_v2"
        const val ROUTINE_CHANNEL_NAME = "Prayer Routines"

        // Old channel IDs to clean up
        private const val OLD_CHANNEL_ID = "lumen_reminders"
        private const val OLD_ROUTINE_CHANNEL_ID = "lumen_routines"

        private const val EXTRA_NOTIFICATION_ID = "notification_id"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_BODY = "body"
        private const val EXTRA_REMINDER_ID = "reminder_id"
        private const val EXTRA_IS_ROUTINE = "is_routine"
        private const val EXTRA_IDENTIFIER = "identifier"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val bellSoundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.notification_bell}")
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val reminderChannel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders for liturgical events and personal notes"
            enableVibration(true)
            setSound(bellSoundUri, audioAttributes)
        }

        val routineChannel = NotificationChannel(
            ROUTINE_CHANNEL_ID,
            ROUTINE_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for prayer routines"
            enableVibration(true)
            setSound(bellSoundUri, audioAttributes)
        }

        manager.createNotificationChannel(reminderChannel)
        manager.createNotificationChannel(routineChannel)

        // Clean up old channels
        manager.deleteNotificationChannel(OLD_CHANNEL_ID)
        manager.deleteNotificationChannel(OLD_ROUTINE_CHANNEL_ID)
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun canScheduleExactAlarms(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            return alarmManager.canScheduleExactAlarms()
        }
        return true
    }

    // MARK: - One-time Reminder Notification (existing)

    fun scheduleNotification(reminder: Reminder): Boolean {
        if (!hasNotificationPermission()) return false

        return scheduleOneTimeNotification(
            identifier = reminder.notificationId,
            title = reminder.title,
            body = reminder.message ?: "Don't forget!",
            triggerTimeMillis = reminder.triggerTimeLong
        )
    }

    // MARK: - One-time Notification (for reminders and First Friday)

    fun scheduleOneTimeNotification(
        identifier: String,
        title: String,
        body: String,
        triggerTimeMillis: Long,
        channelId: String = ROUTINE_CHANNEL_ID
    ): Boolean {
        if (!hasNotificationPermission()) return false
        if (triggerTimeMillis <= System.currentTimeMillis()) return false

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(EXTRA_NOTIFICATION_ID, identifier.hashCode())
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_BODY, body)
            putExtra(EXTRA_IS_ROUTINE, true)
            putExtra(EXTRA_IDENTIFIER, identifier)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            identifier.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMillis,
                pendingIntent
            )
            true
        } catch (e: SecurityException) {
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
                true
            } catch (e2: Exception) {
                false
            }
        }
    }

    // MARK: - Weekly Repeating Notification (for routines)

    fun scheduleWeeklyNotification(
        identifier: String,
        title: String,
        body: String,
        weekday: Int, // Calendar.SUNDAY..SATURDAY (1-7)
        hour: Int,
        minute: Int,
        leadTimeMinutes: Int = 0,
        routineTypeRaw: String = ""
    ): Boolean {
        if (!hasNotificationPermission()) return false

        // Calculate the next occurrence of this weekday at the given time minus lead time
        val triggerTime = calculateNextWeeklyTrigger(weekday, hour, minute, leadTimeMinutes)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(EXTRA_NOTIFICATION_ID, identifier.hashCode())
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_BODY, body)
            putExtra(EXTRA_IS_ROUTINE, true)
            putExtra(EXTRA_IDENTIFIER, identifier)
            putExtra("routine_type", routineTypeRaw)
            // Store weekday info for rescheduling after fire
            putExtra("weekday", weekday)
            putExtra("hour", hour)
            putExtra("minute", minute)
            putExtra("lead_time", leadTimeMinutes)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            identifier.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Use setExactAndAllowWhileIdle for the next occurrence, then reschedule in the receiver
        return try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
            true
        } catch (e: SecurityException) {
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                true
            } catch (e2: Exception) {
                false
            }
        }
    }

    private fun calculateNextWeeklyTrigger(
        weekday: Int,
        hour: Int,
        minute: Int,
        leadTimeMinutes: Int
    ): Long {
        val cal = Calendar.getInstance()

        // Calculate effective hour/minute after subtracting lead time
        var effectiveHour = hour
        var effectiveMinute = minute - leadTimeMinutes
        var effectiveWeekday = weekday

        while (effectiveMinute < 0) {
            effectiveMinute += 60
            effectiveHour--
        }
        while (effectiveHour < 0) {
            effectiveHour += 24
            effectiveWeekday--
            if (effectiveWeekday < Calendar.SUNDAY) {
                effectiveWeekday = Calendar.SATURDAY
            }
        }

        // Set to the target day/time
        cal.set(Calendar.DAY_OF_WEEK, effectiveWeekday)
        cal.set(Calendar.HOUR_OF_DAY, effectiveHour)
        cal.set(Calendar.MINUTE, effectiveMinute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        // If the calculated time is in the past, move to next week
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.WEEK_OF_YEAR, 1)
        }

        return cal.timeInMillis
    }

    fun cancelNotification(notificationId: String) {
        val intent = Intent(context, ReminderAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }

        NotificationManagerCompat.from(context).cancel(notificationId.hashCode())
    }

    fun updateNotification(reminder: Reminder): Boolean {
        cancelNotification(reminder.notificationId)
        return scheduleNotification(reminder)
    }

    fun cancelTodayNotificationForRoutine(routineId: String) {
        val todayWeekday = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val identifier = "$routineId-day$todayWeekday"
        cancelNotification(identifier)
    }

    fun clearBadge() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancelAll()
    }
}

class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra("notification_id", 0)
        val title = intent.getStringExtra("title") ?: "Reminder"
        val body = intent.getStringExtra("body") ?: "Don't forget!"
        val isRoutine = intent.getBooleanExtra("is_routine", false)
        val identifier = intent.getStringExtra("identifier")

        // Determine channel
        val channelId = if (isRoutine) {
            LumenNotificationManager.ROUTINE_CHANNEL_ID
        } else {
            LumenNotificationManager.CHANNEL_ID
        }

        // Ensure channels exist (LumenNotificationManager constructor creates them)
        LumenNotificationManager(context)

        // Determine navigation destination based on routine type
        val routineType = intent.getStringExtra("routine_type") ?: ""

        // Build content intent so tapping the notification opens the app
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (isRoutine) {
                // Route based on routine type: rosary -> prayers/rosary, divineMercy -> prayers/chaplets, others -> calendar/routine
                when (routineType) {
                    "rosary" -> putExtra("open_rosary", true)
                    "divineMercy" -> putExtra("open_chaplets", true)
                    else -> putExtra("open_calendar_routine", true)
                }
            }
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent ?: Intent(),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Missing notification permission
        }

        // If this is a weekly routine notification, reschedule for next week
        val weekday = intent.getIntExtra("weekday", -1)
        val hour = intent.getIntExtra("hour", -1)
        val minute = intent.getIntExtra("minute", -1)
        val leadTime = intent.getIntExtra("lead_time", 0)

        if (weekday > 0 && hour >= 0 && minute >= 0 && identifier != null) {
            val notifManager = LumenNotificationManager(context)
            notifManager.scheduleWeeklyNotification(
                identifier = identifier,
                title = title,
                body = body,
                weekday = weekday,
                hour = hour,
                minute = minute,
                leadTimeMinutes = leadTime,
                routineTypeRaw = routineType
            )
        }
    }
}
