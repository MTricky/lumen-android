package com.app.lumen.features.calendar.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

// MARK: - Routine Item Type

enum class RoutineItemType(val rawValue: String) {
    MASS("mass"),
    MORNING_PRAYER("morningPrayer"),
    EVENING_PRAYER("eveningPrayer"),
    ROSARY("rosary"),
    ADORATION("adoration"),
    ANGELUS("angelus"),
    DIVINE_MERCY("divineMercy"),
    CUSTOM("custom");

    val displayName: String
        get() = when (this) {
            MASS -> "Holy Mass"
            MORNING_PRAYER -> "Morning Prayer"
            EVENING_PRAYER -> "Evening Prayer"
            ROSARY -> "Rosary"
            ADORATION -> "Adoration"
            ANGELUS -> "Angelus"
            DIVINE_MERCY -> "Divine Mercy"
            CUSTOM -> "Custom"
        }

    val icon: ImageVector
        get() = when (this) {
            MASS -> Icons.Filled.AccountBalance
            MORNING_PRAYER -> Icons.Filled.WbSunny
            EVENING_PRAYER -> Icons.Filled.NightsStay
            ROSARY -> Icons.Filled.AutoAwesome
            ADORATION -> Icons.Filled.AutoAwesome
            ANGELUS -> Icons.Filled.Notifications
            DIVINE_MERCY -> Icons.Filled.Favorite
            CUSTOM -> Icons.Filled.Star
        }

    val defaultHour: Int
        get() = when (this) {
            MASS -> 9
            MORNING_PRAYER -> 7
            EVENING_PRAYER -> 20
            ROSARY -> 18
            ADORATION -> 17
            ANGELUS -> 12
            DIVINE_MERCY -> 15
            CUSTOM -> 9
        }

    val defaultMinute: Int get() = 0

    val defaultTitle: String
        get() = when (this) {
            MASS -> "Sunday Mass"
            MORNING_PRAYER -> "Morning Offering"
            EVENING_PRAYER -> "Evening Reflection"
            ROSARY -> "Daily Rosary"
            ADORATION -> "Adoration"
            ANGELUS -> "Angelus"
            DIVINE_MERCY -> "Divine Mercy Chaplet"
            CUSTOM -> ""
        }

    val notificationTitle: String
        get() = when (this) {
            MASS -> "Holy Mass"
            MORNING_PRAYER -> "Morning Prayer"
            EVENING_PRAYER -> "Evening Prayer"
            ROSARY -> "Rosary"
            ADORATION -> "Adoration"
            ANGELUS -> "Angelus"
            DIVINE_MERCY -> "Divine Mercy"
            CUSTOM -> "Prayer Routine"
        }

    val notificationMessage: String
        get() = when (this) {
            MASS -> "Time to prepare for Holy Mass"
            MORNING_PRAYER -> "Start your day with the Morning Offering"
            EVENING_PRAYER -> "End your day with reflection and prayer"
            ROSARY -> "Time to meditate with Mary through the mysteries"
            ADORATION -> "Time for Eucharistic Adoration"
            ANGELUS -> "The Angel of the Lord declared unto Mary"
            DIVINE_MERCY -> "Time to pray the Chaplet of Divine Mercy"
            CUSTOM -> "Time for your prayer routine"
        }

    companion object {
        fun fromRawValue(value: String): RoutineItemType =
            entries.find { it.rawValue == value } ?: CUSTOM
    }
}

// MARK: - Routine Suggestion

data class RoutineSuggestion(
    val type: RoutineItemType,
    val title: String,
    val subtitle: String,
    val defaultDays: Set<Int>, // 1=Sun..7=Sat (Calendar.SUNDAY=1)
    val defaultHour: Int,
    val defaultMinute: Int = 0
) {
    companion object {
        val sundayMass = RoutineSuggestion(
            type = RoutineItemType.MASS,
            title = "Sunday Mass",
            subtitle = "Weekly reminder for Holy Mass",
            defaultDays = setOf(1), // Sunday
            defaultHour = 9
        )

        val dailyRosary = RoutineSuggestion(
            type = RoutineItemType.ROSARY,
            title = "Daily Rosary",
            subtitle = "Meditate with Mary through the mysteries",
            defaultDays = setOf(1, 2, 3, 4, 5, 6, 7),
            defaultHour = 18
        )

        val morningOffering = RoutineSuggestion(
            type = RoutineItemType.MORNING_PRAYER,
            title = "Morning Offering",
            subtitle = "Start your day with the Morning Offering",
            defaultDays = setOf(1, 2, 3, 4, 5, 6, 7),
            defaultHour = 7
        )

        val eveningReflection = RoutineSuggestion(
            type = RoutineItemType.EVENING_PRAYER,
            title = "Evening Reflection",
            subtitle = "End your day with reflection and prayer",
            defaultDays = setOf(1, 2, 3, 4, 5, 6, 7),
            defaultHour = 21
        )

        val divineMercy = RoutineSuggestion(
            type = RoutineItemType.DIVINE_MERCY,
            title = "Divine Mercy Chaplet",
            subtitle = "Pray the Chaplet at the Hour of Mercy",
            defaultDays = setOf(1, 2, 3, 4, 5, 6, 7),
            defaultHour = 15
        )

        val adorationWeekly = RoutineSuggestion(
            type = RoutineItemType.ADORATION,
            title = "Weekly Adoration",
            subtitle = "Visit Jesus in the Blessed Sacrament",
            defaultDays = setOf(5), // Thursday
            defaultHour = 18
        )

        val angelusTraditional = RoutineSuggestion(
            type = RoutineItemType.ANGELUS,
            title = "Angelus",
            subtitle = "Traditional noon prayer",
            defaultDays = setOf(1, 2, 3, 4, 5, 6, 7),
            defaultHour = 12
        )

        val all = listOf(sundayMass, dailyRosary, morningOffering, eveningReflection, divineMercy, adorationWeekly, angelusTraditional)
    }
}

// MARK: - Lead Time Options

data class LeadTimeOption(val label: String, val minutes: Int)

val leadTimeOptions = listOf(
    LeadTimeOption("At time of event", 0),
    LeadTimeOption("5 minutes before", 5),
    LeadTimeOption("15 minutes before", 15),
    LeadTimeOption("30 minutes before", 30),
    LeadTimeOption("1 hour before", 60),
    LeadTimeOption("2 hours before", 120)
)

val firstFridayLeadTimeOptions = listOf(
    LeadTimeOption("At time of event", 0),
    LeadTimeOption("30 minutes before", 30),
    LeadTimeOption("1 hour before", 60),
    LeadTimeOption("2 hours before", 120),
    LeadTimeOption("1 day before", 1440)
)

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
