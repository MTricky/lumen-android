package com.app.lumen.features.calendar.service

import android.content.Context
import com.app.lumen.features.calendar.model.*
import kotlinx.serialization.json.Json
import java.util.Locale

/**
 * Loads and caches celebration data from bundled JSON files.
 */
class CelebrationDataService(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private var cachedData: CelebrationData? = null
    private var cachedLanguage: String? = null
    private val allLanguagesCache = mutableMapOf<String, CelebrationData>()

    companion object {
        val supportedLanguages = listOf("en", "pl", "pt", "es", "it", "fr", "de")
    }

    fun loadCelebrations(): CelebrationData {
        val currentLanguage = currentLanguageCode()

        cachedData?.let { cached ->
            if (cachedLanguage == currentLanguage) return cached
        }

        val data = loadCelebrationsForLanguage(currentLanguage)
        cachedData = data
        cachedLanguage = currentLanguage
        return data
    }

    private fun loadCelebrationsForLanguage(languageCode: String): CelebrationData {
        allLanguagesCache[languageCode]?.let { return it }

        val fileName = "celebrations/celebrations_$languageCode.json"
        val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
        val data = json.decodeFromString<CelebrationData>(jsonString)
        allLanguagesCache[languageCode] = data
        return data
    }

    private fun loadAllLanguages(): Map<String, CelebrationData> {
        for (lang in supportedLanguages) {
            if (!allLanguagesCache.containsKey(lang)) {
                loadCelebrationsForLanguage(lang)
            }
        }
        return allLanguagesCache
    }

    fun fixedCelebration(month: Int, day: Int, region: LiturgicalRegion): FixedCelebrationJSON? {
        val data = loadCelebrations()

        // Region-specific takes priority
        val regionCelebrations = data.fixed.celebrationsForRegion(region)
        regionCelebrations.firstOrNull { it.month == month && it.day == day }?.let { return it }

        // Fall back to universal
        return data.fixed.universal.firstOrNull { it.month == month && it.day == day }
    }

    fun allLanguageNames(key: String, month: Int, day: Int, region: LiturgicalRegion): Map<String, String> {
        val allData = loadAllLanguages()
        val names = mutableMapOf<String, String>()

        for ((langCode, data) in allData) {
            val regionCelebrations = data.fixed.celebrationsForRegion(region)

            // Region-specific takes priority
            val regionMatch = regionCelebrations.firstOrNull { it.key == key }
            if (regionMatch != null) {
                names[langCode] = regionMatch.name
                continue
            }

            // Fall back to universal
            val universalMatch = data.fixed.universal.firstOrNull { it.key == key }
            if (universalMatch != null) {
                names[langCode] = universalMatch.name
            }
        }

        return names
    }

    fun allLanguageNamesForMoveable(key: String): Map<String, String> {
        val allData = loadAllLanguages()
        val names = mutableMapOf<String, String>()

        for ((langCode, data) in allData) {
            data.moveable.firstOrNull { it.key == key }?.let {
                names[langCode] = it.name
            }
        }

        return names
    }

    fun formatSeasonWeekAllLanguages(
        season: LiturgicalSeason,
        week: Int,
        weekday: Int,
        isAshWeek: Boolean = false
    ): Map<String, String> {
        val allData = loadAllLanguages()
        val names = mutableMapOf<String, String>()

        for ((langCode, data) in allData) {
            val weekdayName = data.weekdays.name(weekday)
            val ordinal = ordinalStringForLanguage(week, langCode)

            val format = when (season) {
                LiturgicalSeason.ADVENT -> data.seasonWeekFormat.advent
                LiturgicalSeason.CHRISTMAS -> data.seasonWeekFormat.christmas
                LiturgicalSeason.LENT -> if (isAshWeek) data.seasonWeekFormat.lentAshWeek else data.seasonWeekFormat.lent
                LiturgicalSeason.EASTER -> data.seasonWeekFormat.easter
                LiturgicalSeason.ORDINARY_TIME -> data.seasonWeekFormat.ordinaryTime
            }

            names[langCode] = format
                .replace("{weekday}", weekdayName)
                .replace("{ordinal}", ordinal)
        }

        return names
    }

    private fun ordinalStringForLanguage(number: Int, language: String): String {
        return when (language) {
            "pl", "pt", "es", "it", "fr", "de" -> "$number"
            else -> {
                val suffix = when {
                    (number / 10) % 10 == 1 -> "th"
                    number % 10 == 1 -> "st"
                    number % 10 == 2 -> "nd"
                    number % 10 == 3 -> "rd"
                    else -> "th"
                }
                "$number$suffix"
            }
        }
    }

    private fun currentLanguageCode(): String {
        val lang = Locale.getDefault().language
        return when {
            lang.startsWith("pl") -> "pl"
            lang.startsWith("pt") -> "pt"
            lang.startsWith("es") -> "es"
            lang.startsWith("it") -> "it"
            lang.startsWith("fr") -> "fr"
            lang.startsWith("de") -> "de"
            else -> "en"
        }
    }
}
