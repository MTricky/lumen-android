package com.app.lumen.features.liturgy.model

import androidx.compose.ui.graphics.Color
import com.app.lumen.ui.theme.*

data class DailyLiturgy(
    val date: String,
    val celebration: String,
    val season: String,
    val liturgicalColor: String,
    val saintOfDay: SaintOfDay? = null,
    val readings: LiturgyReadings,
    val audioUrls: AudioUrls? = null,
    val sermon: String? = null,
    val imageUrl: String? = null,
)

data class AudioUrls(
    val firstReading: String? = null,
    val psalm: String? = null,
    val secondReading: String? = null,
    val gospel: String? = null,
)

data class LiturgyReadings(
    val firstReading: Reading,
    val psalm: Psalm,
    val secondReading: Reading? = null,
    val gospel: Reading,
)

data class Reading(
    val reference: String,
    val text: String,
)

data class Psalm(
    val reference: String,
    val response: String,
    val text: String,
)

data class SaintOfDay(
    val name: String,
    val description: String,
)

data class DailyVerse(
    val text: String,
    val reference: String,
    val category: String,
    val reflection: String? = null,
    val liturgicalConnectionType: String? = null,
    val liturgicalConnectionDescription: String? = null,
)

fun liturgicalColor(color: String): Color = when (color) {
    "green" -> LiturgicalGreen
    "purple" -> LiturgicalPurple
    "white" -> LiturgicalWhite
    "red" -> LiturgicalRed
    "rose" -> LiturgicalRose
    else -> LiturgicalGreen
}
