package com.app.lumen.services

import android.content.Context
import com.app.lumen.features.liturgy.model.DailyLiturgy
import com.app.lumen.features.liturgy.model.DailyVerse
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Serializable
data class CachedLiturgy(
    val liturgy: SerializableLiturgy,
    val cachedAt: Long,
)

@Serializable
data class CachedVerse(
    val verse: SerializableVerse,
    val cachedAt: Long,
)

// Serializable mirrors of the model classes
@Serializable
data class SerializableLiturgy(
    val date: String,
    val celebration: String,
    val season: String,
    val liturgicalColor: String,
    val saintName: String? = null,
    val saintDescription: String? = null,
    val firstReadingRef: String,
    val firstReadingText: String,
    val psalmRef: String,
    val psalmResponse: String,
    val psalmText: String,
    val secondReadingRef: String? = null,
    val secondReadingText: String? = null,
    val gospelRef: String,
    val gospelText: String,
    val audioFirstReading: String? = null,
    val audioPsalm: String? = null,
    val audioSecondReading: String? = null,
    val audioGospel: String? = null,
    val sermon: String? = null,
    val imageUrl: String? = null,
)

@Serializable
data class SerializableVerse(
    val text: String,
    val reference: String,
    val category: String,
    val reflection: String? = null,
)

class CacheService(private val context: Context) {
    private val cacheDir = File(context.cacheDir, "LiturgyCache").apply { mkdirs() }
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val prefs = context.getSharedPreferences("cache_prefs", Context.MODE_PRIVATE)

    init {
        // Clear cache if language changed since last run
        val currentLang = Locale.getDefault().language
        val cachedLang = prefs.getString("cached_language", null)
        if (cachedLang != null && cachedLang != currentLang) {
            clearAllCache()
        }
        prefs.edit().putString("cached_language", currentLang).apply()

        cleanupOldCache()
        cleanupOldImageCache()
    }

    fun clearAllCache() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    fun cacheLiturgy(liturgy: DailyLiturgy, dateString: String) {
        try {
            val serializable = liturgy.toSerializable()
            val cached = CachedLiturgy(serializable, System.currentTimeMillis())
            val file = File(cacheDir, "liturgy_$dateString.json")
            file.writeText(json.encodeToString(cached))
        } catch (_: Exception) { }
        cleanupOldCache()
    }

    fun getCachedLiturgy(dateString: String): DailyLiturgy? {
        return try {
            val file = File(cacheDir, "liturgy_$dateString.json")
            if (!file.exists()) return null
            val cached = json.decodeFromString<CachedLiturgy>(file.readText())
            cached.liturgy.toDailyLiturgy()
        } catch (_: Exception) { null }
    }

    fun cacheVerse(verse: DailyVerse, dateString: String) {
        try {
            val serializable = verse.toSerializable()
            val cached = CachedVerse(serializable, System.currentTimeMillis())
            val file = File(cacheDir, "verse_$dateString.json")
            file.writeText(json.encodeToString(cached))
        } catch (_: Exception) { }
    }

    fun getCachedVerse(dateString: String): DailyVerse? {
        return try {
            val file = File(cacheDir, "verse_$dateString.json")
            if (!file.exists()) return null
            val cached = json.decodeFromString<CachedVerse>(file.readText())
            cached.verse.toDailyVerse()
        } catch (_: Exception) { null }
    }

    private fun cleanupOldCache() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = Calendar.getInstance()
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val validDates = setOf(dateFormat.format(today.time), dateFormat.format(tomorrow.time))

        cacheDir.listFiles()?.forEach { file ->
            val datePart = file.nameWithoutExtension
                .removePrefix("liturgy_")
                .removePrefix("verse_")
            if (datePart !in validDates) {
                file.delete()
            }
        }
    }

    /**
     * Clear the Coil image disk cache when stale liturgy entries are removed.
     * This keeps image cache in sync with data cache — old day images are purged.
     */
    private fun cleanupOldImageCache() {
        try {
            val imageCacheDir = File(context.cacheDir, "image_cache")
            if (!imageCacheDir.exists()) return
            // If there are no valid liturgy cache files, clear all images
            // Otherwise Coil manages eviction by size; we just clear on full day transitions
            val hasAnyCachedLiturgy = cacheDir.listFiles()?.any {
                it.name.startsWith("liturgy_")
            } == true
            if (!hasAnyCachedLiturgy) {
                imageCacheDir.deleteRecursively()
                imageCacheDir.mkdirs()
            }
        } catch (_: Exception) { }
    }
}

// Extension functions for conversion
private fun DailyLiturgy.toSerializable() = SerializableLiturgy(
    date = date,
    celebration = celebration,
    season = season,
    liturgicalColor = liturgicalColor,
    saintName = saintOfDay?.name,
    saintDescription = saintOfDay?.description,
    firstReadingRef = readings.firstReading.reference,
    firstReadingText = readings.firstReading.text,
    psalmRef = readings.psalm.reference,
    psalmResponse = readings.psalm.response,
    psalmText = readings.psalm.text,
    secondReadingRef = readings.secondReading?.reference,
    secondReadingText = readings.secondReading?.text,
    gospelRef = readings.gospel.reference,
    gospelText = readings.gospel.text,
    audioFirstReading = audioUrls?.firstReading,
    audioPsalm = audioUrls?.psalm,
    audioSecondReading = audioUrls?.secondReading,
    audioGospel = audioUrls?.gospel,
    sermon = sermon,
    imageUrl = imageUrl,
)

private fun SerializableLiturgy.toDailyLiturgy() = DailyLiturgy(
    date = date,
    celebration = celebration,
    season = season,
    liturgicalColor = liturgicalColor,
    saintOfDay = if (saintName != null) com.app.lumen.features.liturgy.model.SaintOfDay(
        name = saintName,
        description = saintDescription ?: "",
    ) else null,
    readings = com.app.lumen.features.liturgy.model.LiturgyReadings(
        firstReading = com.app.lumen.features.liturgy.model.Reading(firstReadingRef, firstReadingText),
        psalm = com.app.lumen.features.liturgy.model.Psalm(psalmRef, psalmResponse, psalmText),
        secondReading = if (secondReadingRef != null) com.app.lumen.features.liturgy.model.Reading(
            secondReadingRef, secondReadingText ?: ""
        ) else null,
        gospel = com.app.lumen.features.liturgy.model.Reading(gospelRef, gospelText),
    ),
    audioUrls = if (audioFirstReading != null || audioPsalm != null || audioSecondReading != null || audioGospel != null)
        com.app.lumen.features.liturgy.model.AudioUrls(audioFirstReading, audioPsalm, audioSecondReading, audioGospel)
    else null,
    sermon = sermon,
    imageUrl = imageUrl,
)

private fun DailyVerse.toSerializable() = SerializableVerse(
    text = text,
    reference = reference,
    category = category,
    reflection = reflection,
)

private fun SerializableVerse.toDailyVerse() = DailyVerse(
    text = text,
    reference = reference,
    category = category,
    reflection = reflection,
)
