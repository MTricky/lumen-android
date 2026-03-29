package com.app.lumen.features.calendar.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.app.lumen.R
import com.app.lumen.ui.theme.*
import kotlinx.serialization.Serializable

// MARK: - Liturgical Region

enum class LiturgicalRegion(val rawValue: String, val displayName: String, @StringRes val displayNameRes: Int) {
    UNIVERSAL("universal", "Universal", R.string.region_universal),
    USA("usa", "USA", R.string.region_usa),
    BRAZIL("brazil", "Brazil", R.string.region_brazil),
    POLAND("poland", "Poland", R.string.region_poland),
    PORTUGAL("portugal", "Portugal", R.string.region_portugal),
    IRELAND("ireland", "Ireland", R.string.region_ireland),
    PHILIPPINES("philippines", "Philippines", R.string.region_philippines),
    AUSTRALIA("australia", "Australia", R.string.region_australia),
    UK("uk", "United Kingdom", R.string.region_uk),
    CANADA("canada", "Canada", R.string.region_canada),
    SPAIN("spain", "Spain", R.string.region_spain),
    MEXICO("mexico", "Mexico", R.string.region_mexico),
    ARGENTINA("argentina", "Argentina", R.string.region_argentina),
    CHILE("chile", "Chile", R.string.region_chile),
    COLOMBIA("colombia", "Colombia", R.string.region_colombia),
    PERU("peru", "Peru", R.string.region_peru),
    FRANCE("france", "France", R.string.region_france),
    GERMANY("germany", "Germany", R.string.region_germany),
    AUSTRIA("austria", "Austria", R.string.region_austria),
    ITALY("italy", "Italy", R.string.region_italy);

    companion object {
        fun fromRawValue(value: String): LiturgicalRegion =
            entries.find { it.rawValue == value } ?: UNIVERSAL

        fun fromDisplayName(name: String): LiturgicalRegion =
            entries.find { it.displayName == name } ?: UNIVERSAL

        val sortedForDisplay: List<LiturgicalRegion>
            get() = listOf(UNIVERSAL) + entries.filter { it != UNIVERSAL }.sortedBy { it.displayName }
    }
}

// MARK: - Celebration Rank

enum class CelebrationRank(val value: Int) {
    SOLEMNITY(1),
    FEAST_OF_THE_LORD(2),
    FEAST(3),
    OBLIGATORY_MEMORIAL(4),
    OPTIONAL_MEMORIAL(5),
    WEEKDAY(6);

    companion object {
        fun fromValue(value: Int): CelebrationRank =
            entries.find { it.value == value } ?: WEEKDAY

        fun fromString(value: String): CelebrationRank = when (value) {
            "solemnity" -> SOLEMNITY
            "feastOfTheLord" -> FEAST_OF_THE_LORD
            "feast" -> FEAST
            "obligatoryMemorial" -> OBLIGATORY_MEMORIAL
            "optionalMemorial" -> OPTIONAL_MEMORIAL
            else -> WEEKDAY
        }
    }
}

// MARK: - Liturgical Season

enum class LiturgicalSeason(val rawValue: String) {
    ADVENT("Advent"),
    CHRISTMAS("Christmas"),
    LENT("Lent"),
    EASTER("Easter"),
    ORDINARY_TIME("Ordinary Time");

    companion object {
        fun fromRawValue(value: String): LiturgicalSeason =
            entries.find { it.rawValue == value } ?: ORDINARY_TIME
    }
}

// MARK: - Liturgical Color

enum class LiturgicalColor(val rawValue: String) {
    GREEN("green"),
    PURPLE("purple"),
    WHITE("white"),
    RED("red"),
    ROSE("rose");

    val color: Color
        get() = when (this) {
            GREEN -> LiturgicalGreen
            PURPLE -> LiturgicalPurple
            WHITE -> LiturgicalWhite
            RED -> LiturgicalRed
            ROSE -> LiturgicalRose
        }

    companion object {
        fun fromRawValue(value: String): LiturgicalColor = when (value) {
            "green" -> GREEN
            "purple" -> PURPLE
            "white" -> WHITE
            "red" -> RED
            "rose" -> ROSE
            else -> GREEN
        }
    }
}

// MARK: - Liturgical Day

data class LiturgicalDay(
    val id: String,
    val date: java.util.Date,
    val region: LiturgicalRegion,
    val season: LiturgicalSeason,
    val seasonWeek: Int,
    val color: LiturgicalColor,
    val celebrationKey: String,
    val celebrationNames: Map<String, String>,
    val rank: CelebrationRank,
    val isHolyDayOfObligation: Boolean = false,
    val isMoveable: Boolean = false,
    val readingsReference: String? = null
) {
    fun localizedCelebrationName(languageCode: String = "en"): String {
        return celebrationNames[languageCode]
            ?: celebrationNames["en"]
            ?: celebrationNames.values.firstOrNull()
            ?: celebrationKey
    }
}

// MARK: - Calendar Day Display

data class CalendarDayDisplay(
    val id: String,
    val date: java.util.Date,
    val dayNumber: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val liturgicalDay: LiturgicalDay?,
    val isPlaceholder: Boolean = false
) {
    val color: LiturgicalColor
        get() = liturgicalDay?.color ?: LiturgicalColor.GREEN

    val rank: CelebrationRank
        get() = liturgicalDay?.rank ?: CelebrationRank.WEEKDAY

    companion object {
        fun placeholder(): CalendarDayDisplay = CalendarDayDisplay(
            id = java.util.UUID.randomUUID().toString(),
            date = java.util.Date(),
            dayNumber = 0,
            isCurrentMonth = false,
            isToday = false,
            liturgicalDay = null,
            isPlaceholder = true
        )
    }
}

// MARK: - Month Data

data class MonthData(
    val id: String,
    val date: java.util.Date,
    val year: Int,
    val month: Int,
    val days: List<CalendarDayDisplay>
) {
    val monthYearTitle: String
        get() {
            val formatter = java.text.SimpleDateFormat("LLLL yyyy", java.util.Locale.getDefault())
            return formatter.format(date).replaceFirstChar { it.uppercase() }
        }

    val shortMonthName: String
        get() {
            val formatter = java.text.SimpleDateFormat("MMM", java.util.Locale.getDefault())
            return formatter.format(date).uppercase()
        }

    val firstDayIndex: Int
        get() = days.indexOfFirst { !it.isPlaceholder }.coerceAtLeast(0)

    val firstDayColumn: Int
        get() = firstDayIndex % 7
}

// MARK: - Season Info

data class SeasonInfo(
    val season: LiturgicalSeason,
    val week: Int,
    val color: LiturgicalColor,
    val weekdayName: String? = null
)

// MARK: - JSON Models for celebration data

@Serializable
data class CelebrationData(
    val metadata: CelebrationMetadata,
    val fixed: FixedCelebrationsByRegion,
    val moveable: List<MoveableCelebrationJSON>,
    val seasons: SeasonNames,
    val weekdays: WeekdayNames,
    val seasonWeekFormat: SeasonWeekFormats
)

@Serializable
data class CelebrationMetadata(
    val version: String,
    val language: String
)

@Serializable
data class FixedCelebrationsByRegion(
    val universal: List<FixedCelebrationJSON> = emptyList(),
    val poland: List<FixedCelebrationJSON> = emptyList(),
    val brazil: List<FixedCelebrationJSON> = emptyList(),
    val usa: List<FixedCelebrationJSON> = emptyList(),
    val portugal: List<FixedCelebrationJSON> = emptyList(),
    val ireland: List<FixedCelebrationJSON> = emptyList(),
    val philippines: List<FixedCelebrationJSON> = emptyList(),
    val australia: List<FixedCelebrationJSON> = emptyList(),
    val uk: List<FixedCelebrationJSON> = emptyList(),
    val canada: List<FixedCelebrationJSON> = emptyList(),
    val spain: List<FixedCelebrationJSON> = emptyList(),
    val mexico: List<FixedCelebrationJSON> = emptyList(),
    val argentina: List<FixedCelebrationJSON> = emptyList(),
    val chile: List<FixedCelebrationJSON> = emptyList(),
    val colombia: List<FixedCelebrationJSON> = emptyList(),
    val peru: List<FixedCelebrationJSON> = emptyList(),
    val france: List<FixedCelebrationJSON> = emptyList(),
    val germany: List<FixedCelebrationJSON> = emptyList(),
    val austria: List<FixedCelebrationJSON> = emptyList(),
    val italy: List<FixedCelebrationJSON> = emptyList()
) {
    fun celebrationsForRegion(region: LiturgicalRegion): List<FixedCelebrationJSON> = when (region) {
        LiturgicalRegion.UNIVERSAL -> emptyList()
        LiturgicalRegion.USA -> usa
        LiturgicalRegion.BRAZIL -> brazil
        LiturgicalRegion.POLAND -> poland
        LiturgicalRegion.PORTUGAL -> portugal
        LiturgicalRegion.IRELAND -> ireland
        LiturgicalRegion.PHILIPPINES -> philippines
        LiturgicalRegion.AUSTRALIA -> australia
        LiturgicalRegion.UK -> uk
        LiturgicalRegion.CANADA -> canada
        LiturgicalRegion.SPAIN -> spain
        LiturgicalRegion.MEXICO -> mexico
        LiturgicalRegion.ARGENTINA -> argentina
        LiturgicalRegion.CHILE -> chile
        LiturgicalRegion.COLOMBIA -> colombia
        LiturgicalRegion.PERU -> peru
        LiturgicalRegion.FRANCE -> france
        LiturgicalRegion.GERMANY -> germany
        LiturgicalRegion.AUSTRIA -> austria
        LiturgicalRegion.ITALY -> italy
    }
}

@Serializable
data class FixedCelebrationJSON(
    val month: Int,
    val day: Int,
    val key: String,
    val name: String,
    val rank: String,
    val color: String,
    val isHolyDay: Boolean? = null
) {
    val celebrationRank: CelebrationRank get() = CelebrationRank.fromString(rank)
    val liturgicalColor: LiturgicalColor get() = LiturgicalColor.fromRawValue(color)
}

@Serializable
data class MoveableCelebrationJSON(
    val key: String,
    val name: String,
    val daysFromEaster: Int = 0,
    val rank: String,
    val color: String,
    val isHolyDay: Boolean? = null,
    val transferToSunday: List<String>? = null,
    val specialCalculation: String? = null
) {
    val celebrationRank: CelebrationRank get() = CelebrationRank.fromString(rank)
    val liturgicalColor: LiturgicalColor get() = LiturgicalColor.fromRawValue(color)

    fun shouldTransferToSunday(region: LiturgicalRegion): Boolean =
        transferToSunday?.contains(region.rawValue) == true
}

@Serializable
data class SeasonNames(
    val advent: String,
    val christmas: String,
    val lent: String,
    val easter: String,
    val ordinaryTime: String
)

@Serializable
data class WeekdayNames(
    val sunday: String,
    val monday: String,
    val tuesday: String,
    val wednesday: String,
    val thursday: String,
    val friday: String,
    val saturday: String
) {
    fun name(weekday: Int): String = when (weekday) {
        java.util.Calendar.SUNDAY -> sunday
        java.util.Calendar.MONDAY -> monday
        java.util.Calendar.TUESDAY -> tuesday
        java.util.Calendar.WEDNESDAY -> wednesday
        java.util.Calendar.THURSDAY -> thursday
        java.util.Calendar.FRIDAY -> friday
        java.util.Calendar.SATURDAY -> saturday
        else -> ""
    }
}

@Serializable
data class SeasonWeekFormats(
    val advent: String,
    val christmas: String,
    val lent: String,
    val lentAshWeek: String,
    val easter: String,
    val ordinaryTime: String
)
