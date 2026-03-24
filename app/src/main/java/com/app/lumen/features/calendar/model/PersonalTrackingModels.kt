package com.app.lumen.features.calendar.model

import kotlinx.serialization.Serializable

// MARK: - Note Type

enum class NoteType(val rawValue: String) {
    MASS("mass"),
    CONFESSION("confession"),
    CUSTOM("custom");

    val icon: String
        get() = when (this) {
            MASS -> "building.columns"
            CONFESSION -> "heart.circle"
            CUSTOM -> "note.text"
        }

    companion object {
        fun fromRawValue(value: String): NoteType =
            entries.find { it.rawValue == value } ?: CUSTOM
    }
}

// MARK: - Reminder Type

enum class ReminderType(val rawValue: String) {
    MASS("mass"),
    CONFESSION("confession"),
    HOLY_DAY("holyDay"),
    FIRST_FRIDAY("firstFriday"),
    FASTING("fasting"),
    PRAYER("prayer"),
    CUSTOM("custom");

    val icon: String
        get() = when (this) {
            MASS -> "building.columns"
            CONFESSION -> "heart.circle"
            HOLY_DAY -> "star.circle"
            FIRST_FRIDAY -> "1.circle.fill"
            FASTING -> "leaf"
            PRAYER -> "hands.sparkles"
            CUSTOM -> "bell"
        }

    val defaultHour: Int
        get() = when (this) {
            MASS -> 8
            CONFESSION -> 17
            HOLY_DAY -> 8
            FIRST_FRIDAY -> 8
            FASTING -> 7
            PRAYER -> 9
            CUSTOM -> 9
        }

    val defaultMinute: Int
        get() = 0

    fun defaultTitle(liturgicalDay: LiturgicalDay? = null): String = when (this) {
        MASS -> {
            val base = "Attend Mass"
            if (liturgicalDay != null) "$base - ${liturgicalDay.localizedCelebrationName()}" else base
        }
        CONFESSION -> "Go to Confession"
        HOLY_DAY -> {
            val base = "Holy Day of Obligation"
            if (liturgicalDay != null) "$base - ${liturgicalDay.localizedCelebrationName()}" else base
        }
        FIRST_FRIDAY -> "First Friday Devotion"
        FASTING -> "Day of Fasting"
        PRAYER -> "Prayer Time"
        CUSTOM -> ""
    }

    fun defaultMessage(liturgicalDay: LiturgicalDay? = null): String = when (this) {
        MASS -> "Time to prepare for Holy Mass"
        CONFESSION -> "Remember to examine your conscience"
        HOLY_DAY -> "Obligatory Mass attendance today"
        FIRST_FRIDAY -> "Receive Holy Communion in reparation"
        FASTING -> "Abstain from meat and limit meals"
        PRAYER -> "Take a moment for prayer"
        CUSTOM -> ""
    }

    companion object {
        fun fromRawValue(value: String): ReminderType =
            entries.find { it.rawValue == value } ?: CUSTOM
    }
}

// MARK: - Note

@Serializable
data class Note(
    val id: String = java.util.UUID.randomUUID().toString(),
    val dateLong: Long = System.currentTimeMillis(),
    val typeRaw: String = NoteType.CUSTOM.rawValue,
    val title: String = "",
    val content: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
) {
    val type: NoteType get() = NoteType.fromRawValue(typeRaw)

    val date: java.util.Date get() = java.util.Date(dateLong)

    fun formattedDate(): String {
        val formatter = java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault())
        return formatter.format(date)
    }

    fun shortFormattedDate(): String {
        val formatter = java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault())
        return formatter.format(date)
    }
}

// MARK: - Reminder

@Serializable
data class Reminder(
    val id: String = java.util.UUID.randomUUID().toString(),
    val dateLong: Long = System.currentTimeMillis(),
    val triggerTimeLong: Long = System.currentTimeMillis(),
    val typeRaw: String = ReminderType.CUSTOM.rawValue,
    val title: String = "",
    val message: String? = null,
    val notes: String? = null,
    val notificationId: String = java.util.UUID.randomUUID().toString(),
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
) {
    val type: ReminderType get() = ReminderType.fromRawValue(typeRaw)

    val date: java.util.Date get() = java.util.Date(dateLong)
    val triggerTime: java.util.Date get() = java.util.Date(triggerTimeLong)

    fun formattedTime(): String {
        val formatter = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        return formatter.format(triggerTime)
    }

    fun formattedDate(): String {
        val formatter = java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault())
        return formatter.format(date)
    }
}
