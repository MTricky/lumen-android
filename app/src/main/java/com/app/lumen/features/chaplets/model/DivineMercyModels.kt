package com.app.lumen.features.chaplets.model

import kotlinx.serialization.Serializable

// --- JSON data models ---

@Serializable
data class DivineMercyPrayerData(
    val prayers: DivineMercyPrayers,
    val decades: List<DivineMercyDecade>,
)

@Serializable
data class DivineMercyPrayers(
    val signOfTheCross: Prayer,
    val openingPrayer: Prayer,
    val ourFather: Prayer,
    val hailMary: Prayer,
    val apostlesCreed: Prayer,
    val eternalFather: Prayer,
    val forTheSake: Prayer,
    val holyGod: Prayer,
    val closingPrayer: Prayer,
)

@Serializable
data class DivineMercyDecade(
    val title: String,
    val meditation: String,
)

// --- Prayer step progression ---

sealed class DivineMercyPrayerStep {
    data object Intro : DivineMercyPrayerStep()
    data object SignOfTheCross : DivineMercyPrayerStep()
    data object OpeningPrayer : DivineMercyPrayerStep()
    data object OurFather : DivineMercyPrayerStep()
    data object HailMary : DivineMercyPrayerStep()
    data object ApostlesCreed : DivineMercyPrayerStep()
    data class DecadeAnnouncement(val decade: Int) : DivineMercyPrayerStep()
    data class EternalFather(val decade: Int) : DivineMercyPrayerStep()
    data class ForTheSake(val decade: Int, val count: Int) : DivineMercyPrayerStep()
    data class HolyGod(val count: Int) : DivineMercyPrayerStep()
    data object ClosingPrayer : DivineMercyPrayerStep()
    data object ClosingSignOfTheCross : DivineMercyPrayerStep()

    fun getDecade(): Int? = when (this) {
        is DecadeAnnouncement -> decade
        is EternalFather -> decade
        is ForTheSake -> decade
        else -> null
    }

    fun getForTheSakeCount(): Int? = (this as? ForTheSake)?.count

    fun getHolyGodCount(): Int? = (this as? HolyGod)?.count

    val isDecadeAnnouncement: Boolean
        get() = this is DecadeAnnouncement
}

data class DivineMercyProgress(
    val currentStep: DivineMercyPrayerStep,
    val totalSteps: Int,
    val currentStepIndex: Int,
) {
    val progress: Float
        get() = currentStepIndex.toFloat() / totalSteps.toFloat()
}
