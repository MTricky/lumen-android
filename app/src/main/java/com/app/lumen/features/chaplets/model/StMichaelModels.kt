package com.app.lumen.features.chaplets.model

import kotlinx.serialization.Serializable

// --- JSON data models ---

@Serializable
data class StMichaelPrayerData(
    val prayers: StMichaelPrayers,
    val salutations: List<StMichaelSalutation>,
    val archangelPrayers: List<StMichaelArchangelPrayer>,
)

@Serializable
data class StMichaelPrayers(
    val signOfTheCross: Prayer,
    val openingPrayer: Prayer,
    val ourFather: Prayer,
    val hailMary: Prayer,
    val gloryBe: Prayer,
    val closingPrayer: Prayer,
    val finalPrayer: Prayer,
)

@Serializable
data class StMichaelSalutation(
    val title: String,
    val choir: String,
    val salutation: String,
)

@Serializable
data class StMichaelArchangelPrayer(
    val title: String,
    val instruction: String,
)

// --- Prayer step progression ---

sealed class StMichaelPrayerStep {
    data object Intro : StMichaelPrayerStep()
    data object SignOfTheCross : StMichaelPrayerStep()
    data object OpeningPrayer : StMichaelPrayerStep()
    // 9 Salutations — each followed by Our Father + 3 Hail Marys + Glory Be
    data class SalutationAnnouncement(val salutation: Int) : StMichaelPrayerStep()
    data class SalutationOurFather(val salutation: Int) : StMichaelPrayerStep()
    data class SalutationHailMary(val salutation: Int, val count: Int) : StMichaelPrayerStep()
    data class SalutationGloryBe(val salutation: Int) : StMichaelPrayerStep()
    // 4 Archangel prayers — each Our Father
    data class ArchangelAnnouncement(val archangel: Int) : StMichaelPrayerStep()
    data class ArchangelOurFather(val archangel: Int) : StMichaelPrayerStep()
    data object ClosingPrayer : StMichaelPrayerStep()
    data object FinalPrayer : StMichaelPrayerStep()
    data object ClosingSignOfTheCross : StMichaelPrayerStep()

    fun getSalutation(): Int? = when (this) {
        is SalutationAnnouncement -> salutation
        is SalutationOurFather -> salutation
        is SalutationHailMary -> salutation
        is SalutationGloryBe -> salutation
        else -> null
    }

    fun getArchangel(): Int? = when (this) {
        is ArchangelAnnouncement -> archangel
        is ArchangelOurFather -> archangel
        else -> null
    }

    fun getHailMaryCount(): Int? = (this as? SalutationHailMary)?.count

    val isSalutationAnnouncement: Boolean
        get() = this is SalutationAnnouncement

    val isArchangelAnnouncement: Boolean
        get() = this is ArchangelAnnouncement
}

data class StMichaelProgress(
    val currentStep: StMichaelPrayerStep,
    val totalSteps: Int,
    val currentStepIndex: Int,
) {
    val progress: Float
        get() = currentStepIndex.toFloat() / totalSteps.toFloat()
}
