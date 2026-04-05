package com.app.lumen.features.calendar.model

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.app.lumen.R
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

    @get:StringRes
    val displayNameRes: Int
        get() = when (this) {
            MASS -> R.string.routine_type_mass
            MORNING_PRAYER -> R.string.routine_type_morning_prayer
            EVENING_PRAYER -> R.string.routine_type_evening_prayer
            ROSARY -> R.string.routine_type_rosary
            ADORATION -> R.string.routine_type_adoration
            ANGELUS -> R.string.routine_type_angelus
            DIVINE_MERCY -> R.string.routine_type_divine_mercy
            CUSTOM -> R.string.routine_type_custom
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

    @get:StringRes
    val defaultTitleRes: Int
        get() = when (this) {
            MASS -> R.string.routine_default_sunday_mass
            MORNING_PRAYER -> R.string.routine_default_morning_offering
            EVENING_PRAYER -> R.string.routine_default_evening_reflection
            ROSARY -> R.string.routine_default_daily_rosary
            ADORATION -> R.string.routine_default_weekly_adoration
            ANGELUS -> R.string.routine_type_angelus
            DIVINE_MERCY -> R.string.routine_default_divine_mercy_chaplet
            CUSTOM -> R.string.routine_type_custom
        }

    @get:StringRes
    val notificationTitleRes: Int
        get() = when (this) {
            MASS -> R.string.routine_type_mass
            MORNING_PRAYER -> R.string.routine_type_morning_prayer
            EVENING_PRAYER -> R.string.routine_type_evening_prayer
            ROSARY -> R.string.routine_type_rosary
            ADORATION -> R.string.routine_type_adoration
            ANGELUS -> R.string.routine_type_angelus
            DIVINE_MERCY -> R.string.routine_type_divine_mercy
            CUSTOM -> R.string.routine_type_prayer_routine
        }

    @get:StringRes
    val notificationMessageRes: Int
        get() = when (this) {
            MASS -> R.string.routine_notif_mass
            MORNING_PRAYER -> R.string.routine_notif_morning
            EVENING_PRAYER -> R.string.routine_notif_evening
            ROSARY -> R.string.routine_notif_rosary
            ADORATION -> R.string.routine_notif_adoration
            ANGELUS -> R.string.routine_notif_angelus
            DIVINE_MERCY -> R.string.routine_notif_divine_mercy
            CUSTOM -> R.string.routine_notif_custom
        }

    companion object {
        fun fromRawValue(value: String): RoutineItemType =
            entries.find { it.rawValue == value } ?: CUSTOM
    }
}

// MARK: - Routine Suggestion

data class RoutineSuggestion(
    val type: RoutineItemType,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val defaultDays: Set<Int>, // 1=Sun..7=Sat (Calendar.SUNDAY=1)
    val defaultHour: Int,
    val defaultMinute: Int = 0
) {
    companion object {
        val sundayMass = RoutineSuggestion(
            type = RoutineItemType.MASS,
            titleRes = R.string.routine_default_sunday_mass,
            subtitleRes = R.string.routine_suggestion_mass_subtitle,
            defaultDays = setOf(1), // Sunday
            defaultHour = 9
        )

        val dailyRosary = RoutineSuggestion(
            type = RoutineItemType.ROSARY,
            titleRes = R.string.routine_default_daily_rosary,
            subtitleRes = R.string.routine_suggestion_rosary_subtitle,
            defaultDays = setOf(1, 2, 3, 4, 5, 6, 7),
            defaultHour = 18
        )

        val morningOffering = RoutineSuggestion(
            type = RoutineItemType.MORNING_PRAYER,
            titleRes = R.string.routine_default_morning_offering,
            subtitleRes = R.string.routine_suggestion_morning_subtitle,
            defaultDays = setOf(1, 2, 3, 4, 5, 6, 7),
            defaultHour = 7
        )

        val eveningReflection = RoutineSuggestion(
            type = RoutineItemType.EVENING_PRAYER,
            titleRes = R.string.routine_default_evening_reflection,
            subtitleRes = R.string.routine_suggestion_evening_subtitle,
            defaultDays = setOf(1, 2, 3, 4, 5, 6, 7),
            defaultHour = 21
        )

        val divineMercy = RoutineSuggestion(
            type = RoutineItemType.DIVINE_MERCY,
            titleRes = R.string.routine_default_divine_mercy_chaplet,
            subtitleRes = R.string.routine_suggestion_divine_mercy_subtitle,
            defaultDays = setOf(1, 2, 3, 4, 5, 6, 7),
            defaultHour = 15
        )

        val adorationWeekly = RoutineSuggestion(
            type = RoutineItemType.ADORATION,
            titleRes = R.string.routine_default_weekly_adoration,
            subtitleRes = R.string.routine_suggestion_adoration_subtitle,
            defaultDays = setOf(5), // Thursday
            defaultHour = 18
        )

        val angelusTraditional = RoutineSuggestion(
            type = RoutineItemType.ANGELUS,
            titleRes = R.string.routine_type_angelus,
            subtitleRes = R.string.routine_suggestion_angelus_subtitle,
            defaultDays = setOf(1, 2, 3, 4, 5, 6, 7),
            defaultHour = 12
        )

        val all = listOf(sundayMass, dailyRosary, morningOffering, eveningReflection, divineMercy, adorationWeekly, angelusTraditional)
    }
}

// MARK: - Lead Time Options

data class LeadTimeOption(@StringRes val labelRes: Int, val minutes: Int)

val leadTimeOptions = listOf(
    LeadTimeOption(R.string.routine_lead_at_time, 0),
    LeadTimeOption(R.string.routine_lead_5min, 5),
    LeadTimeOption(R.string.routine_lead_15min, 15),
    LeadTimeOption(R.string.routine_lead_30min, 30),
    LeadTimeOption(R.string.routine_lead_1hour, 60),
    LeadTimeOption(R.string.routine_lead_2hours, 120)
)

val firstFridayLeadTimeOptions = listOf(
    LeadTimeOption(R.string.routine_lead_at_time, 0),
    LeadTimeOption(R.string.routine_lead_30min, 30),
    LeadTimeOption(R.string.routine_lead_1hour, 60),
    LeadTimeOption(R.string.routine_lead_2hours, 120),
    LeadTimeOption(R.string.routine_lead_1day, 1440)
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

    @get:StringRes
    val defaultTitleRes: Int
        get() = when (this) {
            MASS -> R.string.reminder_default_mass
            CONFESSION -> R.string.reminder_default_confession
            HOLY_DAY -> R.string.reminder_default_holy_day
            FIRST_FRIDAY -> R.string.reminder_default_first_friday
            FASTING -> R.string.reminder_default_fasting
            PRAYER -> R.string.reminder_default_prayer
            CUSTOM -> R.string.reminder_msg_default
        }

    @get:StringRes
    val defaultMessageRes: Int
        get() = when (this) {
            MASS -> R.string.reminder_msg_mass
            CONFESSION -> R.string.reminder_msg_confession
            HOLY_DAY -> R.string.reminder_msg_holy_day
            FIRST_FRIDAY -> R.string.reminder_msg_first_friday
            FASTING -> R.string.reminder_msg_fasting
            PRAYER -> R.string.reminder_msg_prayer
            CUSTOM -> R.string.reminder_msg_default
        }

    fun defaultTitle(context: android.content.Context, liturgicalDay: LiturgicalDay? = null): String = when (this) {
        MASS -> {
            val base = context.getString(defaultTitleRes)
            if (liturgicalDay != null) "$base - ${liturgicalDay.localizedCelebrationName()}" else base
        }
        HOLY_DAY -> {
            val base = context.getString(defaultTitleRes)
            if (liturgicalDay != null) "$base - ${liturgicalDay.localizedCelebrationName()}" else base
        }
        CUSTOM -> ""
        else -> context.getString(defaultTitleRes)
    }

    fun defaultMessage(context: android.content.Context): String =
        if (this == CUSTOM) "" else context.getString(defaultMessageRes)

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
        val formatter = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, java.util.Locale.getDefault())
        return formatter.format(triggerTime)
    }

    fun formattedDate(): String {
        val formatter = java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault())
        return formatter.format(date)
    }
}
