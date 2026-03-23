package com.app.lumen.features.rosary.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.app.lumen.R
import kotlinx.serialization.Serializable
import java.util.Calendar

enum class MysteryType(
    @StringRes val labelRes: Int,
    @StringRes val daysRes: Int,
    @DrawableRes val imageRes: Int,
    val daysOfWeek: Set<Int>,
) {
    JOYFUL(
        labelRes = R.string.rosary_joyful,
        daysRes = R.string.rosary_days_joyful,
        imageRes = R.drawable.mystery_joyful,
        daysOfWeek = setOf(Calendar.MONDAY, Calendar.SATURDAY),
    ),
    SORROWFUL(
        labelRes = R.string.rosary_sorrowful,
        daysRes = R.string.rosary_days_sorrowful,
        imageRes = R.drawable.mystery_sorrowful,
        daysOfWeek = setOf(Calendar.TUESDAY, Calendar.FRIDAY),
    ),
    GLORIOUS(
        labelRes = R.string.rosary_glorious,
        daysRes = R.string.rosary_days_glorious,
        imageRes = R.drawable.mystery_glorious,
        daysOfWeek = setOf(Calendar.WEDNESDAY, Calendar.SUNDAY),
    ),
    LUMINOUS(
        labelRes = R.string.rosary_luminous,
        daysRes = R.string.rosary_days_luminous,
        imageRes = R.drawable.mystery_luminous,
        daysOfWeek = setOf(Calendar.THURSDAY),
    );

    val isTodaysMystery: Boolean
        get() = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) in daysOfWeek

    companion object {
        fun forToday(): MysteryType {
            val weekday = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            return entries.firstOrNull { weekday in it.daysOfWeek } ?: JOYFUL
        }
    }
}

// --- Prayer JSON data models ---

@Serializable
data class Mystery(
    val name: String,
    val meditation: String,
    val fruit: String,
)

@Serializable
data class Prayer(
    val title: String,
    val text: String,
)

@Serializable
data class Prayers(
    val signOfTheCross: Prayer,
    val apostlesCreed: Prayer,
    val ourFather: Prayer,
    val hailMary: Prayer,
    val gloryBe: Prayer,
    val fatimaPrayer: Prayer,
    val hailHolyQueen: Prayer,
    val finalPrayer: Prayer,
    val introHailMaryFaith: Prayer,
    val introHailMaryHope: Prayer,
    val introHailMaryCharity: Prayer,
)

@Serializable
data class Mysteries(
    val joyful: List<Mystery>,
    val sorrowful: List<Mystery>,
    val glorious: List<Mystery>,
    val luminous: List<Mystery>,
) {
    fun forType(type: MysteryType): List<Mystery> = when (type) {
        MysteryType.JOYFUL -> joyful
        MysteryType.SORROWFUL -> sorrowful
        MysteryType.GLORIOUS -> glorious
        MysteryType.LUMINOUS -> luminous
    }
}

@Serializable
data class PrayerData(
    val prayers: Prayers,
    val mysteries: Mysteries,
)

// --- Prayer step progression ---

enum class IntroVirtue(val index: Int) {
    FAITH(1), HOPE(2), CHARITY(3);
}

sealed class RosaryPrayerStep {
    data object Intro : RosaryPrayerStep()
    data object SignOfTheCross : RosaryPrayerStep()
    data object ApostlesCreed : RosaryPrayerStep()
    data object IntroOurFather : RosaryPrayerStep()
    data class IntroHailMary(val virtue: IntroVirtue) : RosaryPrayerStep()
    data object IntroGloryBe : RosaryPrayerStep()
    data class MysteryAnnouncement(val decade: Int) : RosaryPrayerStep()
    data class DecadeOurFather(val decade: Int) : RosaryPrayerStep()
    data class DecadeHailMary(val decade: Int, val count: Int) : RosaryPrayerStep()
    data class DecadeGloryBe(val decade: Int) : RosaryPrayerStep()
    data class DecadeFatimaPrayer(val decade: Int) : RosaryPrayerStep()
    data object HailHolyQueen : RosaryPrayerStep()
    data object FinalPrayer : RosaryPrayerStep()
    data object ClosingSignOfTheCross : RosaryPrayerStep()

    fun getDecade(): Int? = when (this) {
        is MysteryAnnouncement -> decade
        is DecadeOurFather -> decade
        is DecadeHailMary -> decade
        is DecadeGloryBe -> decade
        is DecadeFatimaPrayer -> decade
        else -> null
    }

    fun getHailMaryCount(): Int? = (this as? DecadeHailMary)?.count

    val isMysteryAnnouncement: Boolean
        get() = this is MysteryAnnouncement
}

data class RosaryProgress(
    val currentStep: RosaryPrayerStep,
    val totalSteps: Int,
    val currentStepIndex: Int,
) {
    val progress: Float
        get() = currentStepIndex.toFloat() / totalSteps.toFloat()
}
