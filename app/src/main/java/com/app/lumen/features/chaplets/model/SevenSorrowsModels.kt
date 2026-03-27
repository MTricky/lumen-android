package com.app.lumen.features.chaplets.model

import kotlinx.serialization.Serializable

// --- JSON data models ---

@Serializable
data class SevenSorrowsPrayerData(
    val prayers: SevenSorrowsPrayers,
    val sorrows: List<SevenSorrowsSorrow>,
)

@Serializable
data class SevenSorrowsPrayers(
    val signOfTheCross: Prayer,
    val introductoryPrayer: Prayer,
    val actOfContrition: Prayer,
    val ourFather: Prayer,
    val hailMary: Prayer,
    val gloryBe: Prayer,
    val sorrowfulMotherPrayer: Prayer,
    val closingPrayer: Prayer,
    val finalInvocation: Prayer,
    val versicle: Prayer,
    val response: Prayer,
    val concludingPrayer: Prayer,
)

@Serializable
data class SevenSorrowsSorrow(
    val title: String,
    val name: String,
    val scripture: String,
    val meditation: String,
    val fruit: String,
)

// --- Prayer step progression ---

sealed class SevenSorrowsPrayerStep {
    data object Intro : SevenSorrowsPrayerStep()
    data object SignOfTheCross : SevenSorrowsPrayerStep()
    data object IntroductoryPrayer : SevenSorrowsPrayerStep()
    data object ActOfContrition : SevenSorrowsPrayerStep()
    // 7 Sorrows — each with meditation, Our Father, 7 Hail Marys, sorrowful mother prayer
    data class SorrowAnnouncement(val sorrow: Int) : SevenSorrowsPrayerStep()
    data class SorrowOurFather(val sorrow: Int) : SevenSorrowsPrayerStep()
    data class SorrowHailMary(val sorrow: Int, val count: Int) : SevenSorrowsPrayerStep()
    data class SorrowfulMotherPrayer(val sorrow: Int) : SevenSorrowsPrayerStep()
    // Closing prayers
    data object ClosingPrayer : SevenSorrowsPrayerStep()
    data class FinalInvocation(val count: Int) : SevenSorrowsPrayerStep()
    data object Versicle : SevenSorrowsPrayerStep()
    data object Response : SevenSorrowsPrayerStep()
    data object ConcludingPrayer : SevenSorrowsPrayerStep()
    data object ClosingSignOfTheCross : SevenSorrowsPrayerStep()

    fun getSorrow(): Int? = when (this) {
        is SorrowAnnouncement -> sorrow
        is SorrowOurFather -> sorrow
        is SorrowHailMary -> sorrow
        is SorrowfulMotherPrayer -> sorrow
        else -> null
    }

    fun getHailMaryCount(): Int? = (this as? SorrowHailMary)?.count

    fun getFinalInvocationCount(): Int? = (this as? FinalInvocation)?.count

    val isSorrowAnnouncement: Boolean
        get() = this is SorrowAnnouncement
}

data class SevenSorrowsProgress(
    val currentStep: SevenSorrowsPrayerStep,
    val totalSteps: Int,
    val currentStepIndex: Int,
) {
    val progress: Float
        get() = currentStepIndex.toFloat() / totalSteps.toFloat()
}
