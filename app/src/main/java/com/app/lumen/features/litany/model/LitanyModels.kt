package com.app.lumen.features.litany.model

import com.app.lumen.features.chaplets.model.Prayer
import kotlinx.serialization.Serializable

// --- JSON data models ---

@Serializable
data class LitanyPrayerData(
    val id: String,
    val title: String,
    val subtitle: String,
    val prayers: LitanyPrayers,
    val sections: List<LitanySection>,
)

@Serializable
data class LitanyPrayers(
    val signOfTheCross: Prayer,
    val closingPrayer: Prayer,
)

@Serializable
data class LitanySection(
    val title: String,
    val type: String,
    val response: String? = null,
    val invocations: List<LitanyInvocation>,
)

@Serializable
data class LitanyInvocation(
    val text: String,
    val response: String? = null,
)

// --- Section type ---

enum class LitanySectionType {
    KYRIE,
    PETITION,
    LAMB_OF_GOD,
    VERSICLE;

    val hasSharedResponse: Boolean
        get() = this == PETITION

    companion object {
        fun from(type: String): LitanySectionType = when (type) {
            "kyrie" -> KYRIE
            "petition" -> PETITION
            "lambOfGod" -> LAMB_OF_GOD
            "versicle" -> VERSICLE
            else -> PETITION
        }
    }
}

// --- Prayer step progression ---

sealed class LitanyPrayerStep {
    data object Intro : LitanyPrayerStep()
    data object SignOfTheCross : LitanyPrayerStep()
    data class Invocation(val sectionIndex: Int, val invocationIndex: Int) : LitanyPrayerStep()
    data object ClosingPrayer : LitanyPrayerStep()
    data object ClosingSignOfTheCross : LitanyPrayerStep()

    val isInvocation: Boolean
        get() = this is Invocation
}

// --- Progress ---

data class LitanyProgress(
    val currentStep: LitanyPrayerStep,
    val totalSteps: Int,
    val currentStepIndex: Int,
    val sectionTitle: String?,
    val sectionInvocationIndex: Int?,
    val sectionInvocationCount: Int?,
    val sectionType: LitanySectionType?,
    val sectionResponse: String?,
) {
    val progress: Float
        get() = if (totalSteps > 0) currentStepIndex.toFloat() / totalSteps.toFloat() else 0f

    val remaining: Int
        get() = maxOf(0, totalSteps - currentStepIndex - 1)
}
