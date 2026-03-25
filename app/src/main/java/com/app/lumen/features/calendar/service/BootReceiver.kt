package com.app.lumen.features.calendar.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            // Reschedule all routine and reminder notifications
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val routineService = RoutineStorageService(context)
                    routineService.rescheduleAllNotifications()

                    val firstFridayService = FirstFridayRoutineService(context)
                    firstFridayService.rescheduleAllNotifications()

                    // Reschedule one-time reminders
                    val storageService = NotesStorageService(context)
                    val notificationManager = LumenNotificationManager(context)
                    val reminders = storageService.upcomingReminders()
                    for (reminder in reminders) {
                        if (reminder.isActive && reminder.triggerTimeLong > System.currentTimeMillis()) {
                            notificationManager.scheduleNotification(reminder)
                        }
                    }
                } catch (e: Exception) {
                    // Silently handle - notifications will be rescheduled next app launch
                }
            }
        }
    }
}
