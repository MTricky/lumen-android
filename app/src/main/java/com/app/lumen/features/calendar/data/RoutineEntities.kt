package com.app.lumen.features.calendar.data

import kotlinx.serialization.Serializable

@Serializable
data class WeeklyRoutineEntity(
    val id: String,
    val title: String,
    val typeRaw: String,
    val selectedDaysJson: String, // JSON array of Int (1=Sun..7=Sat)
    val hour: Int,
    val minute: Int,
    val isNotificationEnabled: Boolean,
    val notificationIdentifiersJson: String, // JSON array of String
    val isActive: Boolean,
    val isLoggingEnabled: Boolean,
    val notificationLeadTimeMinutes: Int,
    val sortOrder: Int,
    val createdAt: Long
)

@Serializable
data class RoutineCompletionEntity(
    val id: String,
    val routineId: String,
    val dateLong: Long, // start of day
    val completedAt: Long
)

@Serializable
data class FirstFridayRoutineEntity(
    val id: String,
    val isActive: Boolean,
    val isNotificationEnabled: Boolean,
    val notificationHour: Int,
    val notificationMinute: Int,
    val notificationLeadTimeMinutes: Int,
    val notificationIdentifiersJson: String,
    val initialConsecutiveCount: Int,
    val sortOrder: Int,
    val createdAt: Long
)

@Serializable
data class FirstFridayCompletionEntity(
    val id: String,
    val routineId: String,
    val dateLong: Long,
    val notes: String? = null,
    val completedAt: Long
)
