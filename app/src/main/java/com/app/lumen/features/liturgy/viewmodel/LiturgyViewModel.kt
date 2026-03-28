package com.app.lumen.features.liturgy.viewmodel

import android.app.Application
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.lumen.features.liturgy.model.*
import com.app.lumen.features.liturgy.model.AudioUrls
import com.app.lumen.services.CacheService
import com.app.lumen.widget.VerseWidgetData
import com.app.lumen.widget.VerseWidgetWorker
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

enum class DaySelection { TODAY, TOMORROW }

class LiturgyViewModel(application: Application) : AndroidViewModel(application) {
    private val firestore = FirebaseFirestore.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val cacheService = CacheService(application)

    private val _daySelection = MutableStateFlow(DaySelection.TODAY)
    val daySelection: StateFlow<DaySelection> = _daySelection

    private val _liturgy = MutableStateFlow<DailyLiturgy?>(null)
    val liturgy: StateFlow<DailyLiturgy?> = _liturgy

    private val _verse = MutableStateFlow<DailyVerse?>(null)
    val verse: StateFlow<DailyVerse?> = _verse

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // In-memory cache
    private val liturgyCache = mutableMapOf<String, DailyLiturgy>()
    private val verseCache = mutableMapOf<String, DailyVerse>()

    // Track the last loaded date to detect day changes when resuming
    private var lastLoadedDate: String = ""

    init {
        // Check disk cache synchronously so cached data shows on first frame
        val todayDate = dateFormat.format(Calendar.getInstance().time)
        lastLoadedDate = todayDate
        val diskLiturgy = cacheService.getCachedLiturgy(todayDate)
        val diskVerse = cacheService.getCachedVerse(todayDate)
        if (diskLiturgy != null) {
            _liturgy.value = diskLiturgy
            _verse.value = diskVerse
            liturgyCache[todayDate] = diskLiturgy
            if (diskVerse != null) verseCache[todayDate] = diskVerse
            // No loading needed — cached data is ready
            // Still refresh from network in background
            viewModelScope.launch { fetchFromNetwork(todayDate, showLoading = false) }
        } else {
            // No cache — show loading skeleton immediately
            _isLoading.value = true
            loadData()
        }
        prefetchTomorrow()
        // Enqueue periodic widget updates
        VerseWidgetWorker.enqueuePeriodicWork(application)
    }

    /**
     * Call when app resumes from background to detect if the day changed.
     */
    fun checkForDayChange() {
        val currentDate = dateFormat.format(Calendar.getInstance().time)
        if (currentDate != lastLoadedDate) {
            lastLoadedDate = currentDate
            // Day changed — clear stale memory cache
            liturgyCache.clear()
            verseCache.clear()
            // Reset to today
            _daySelection.value = DaySelection.TODAY
            // Try disk cache first
            val diskLiturgy = cacheService.getCachedLiturgy(currentDate)
            val diskVerse = cacheService.getCachedVerse(currentDate)
            if (diskLiturgy != null) {
                _liturgy.value = diskLiturgy
                _verse.value = diskVerse
                liturgyCache[currentDate] = diskLiturgy
                if (diskVerse != null) verseCache[currentDate] = diskVerse
                _isLoading.value = false
                _error.value = null
                viewModelScope.launch { fetchFromNetwork(currentDate, showLoading = false) }
            } else {
                _liturgy.value = null
                _verse.value = null
                _isLoading.value = true
                loadData()
            }
            prefetchTomorrow()
        }
    }

    fun selectDay(day: DaySelection) {
        if (_daySelection.value == day) return
        _daySelection.value = day
        val dateString = getDateString()

        // Show cached data immediately if available (memory first, then disk)
        val cachedLiturgy = liturgyCache[dateString] ?: cacheService.getCachedLiturgy(dateString)
        val cachedVerse = verseCache[dateString] ?: cacheService.getCachedVerse(dateString)
        if (cachedLiturgy != null) {
            _liturgy.value = cachedLiturgy
            _verse.value = cachedVerse
            _error.value = null
            // Still refresh in background
            refreshData(showLoading = false)
        } else {
            // Clear old data so stale image doesn't flash
            _liturgy.value = null
            _verse.value = null
            loadData()
        }
    }

    private fun getDateString(): String {
        val cal = Calendar.getInstance()
        if (_daySelection.value == DaySelection.TOMORROW) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return dateFormat.format(cal.time)
    }

    fun getDisplayDate(): String {
        val cal = Calendar.getInstance()
        if (_daySelection.value == DaySelection.TOMORROW) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        val displayFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        return displayFormat.format(cal.time)
    }

    private fun loadData() {
        refreshData(showLoading = true)
    }

    private fun prefetchTomorrow() {
        viewModelScope.launch {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
            val tomorrowDate = dateFormat.format(cal.time)
            // Skip if already in memory cache
            if (liturgyCache.containsKey(tomorrowDate)) return@launch
            // Check disk cache
            val diskCached = cacheService.getCachedLiturgy(tomorrowDate)
            if (diskCached != null) {
                liturgyCache[tomorrowDate] = diskCached
                return@launch
            }
            // Fetch from network
            try {
                val mainDoc = firestore.collection("dailyLiturgy")
                    .document(tomorrowDate).get().await()
                val imageUrl = mainDoc.data?.get("imageUrl") as? String
                val contentDoc = firestore.collection("dailyLiturgy")
                    .document(tomorrowDate).collection("content")
                    .document("en").get().await()
                if (contentDoc.exists()) {
                    val parsed = parseLiturgy(contentDoc.data!!, imageUrl)
                    liturgyCache[tomorrowDate] = parsed
                    cacheService.cacheLiturgy(parsed, tomorrowDate)
                }
            } catch (_: Exception) { }
        }
    }

    private fun refreshData(showLoading: Boolean) {
        viewModelScope.launch {
            val dateString = getDateString()

            // Try disk cache first if we have nothing in memory
            if (showLoading && _liturgy.value == null) {
                val diskLiturgy = cacheService.getCachedLiturgy(dateString)
                val diskVerse = cacheService.getCachedVerse(dateString)
                if (diskLiturgy != null) {
                    _liturgy.value = diskLiturgy
                    _verse.value = diskVerse
                    liturgyCache[dateString] = diskLiturgy
                    if (diskVerse != null) verseCache[dateString] = diskVerse
                    // Still fetch from network in background
                    showLoading.let { }  // don't show loading since we have cached data
                    _isLoading.value = false
                    fetchFromNetwork(dateString, showLoading = false)
                    return@launch
                }
            }

            if (showLoading) _isLoading.value = true
            _error.value = null
            fetchFromNetwork(dateString, showLoading)
        }
    }

    private suspend fun fetchFromNetwork(dateString: String, showLoading: Boolean) {
        var liturgyImageUrl: String? = null

        try {
            val mainDoc = firestore.collection("dailyLiturgy")
                .document(dateString)
                .get()
                .await()

            val imageUrl = mainDoc.data?.get("imageUrl") as? String
            liturgyImageUrl = imageUrl

            val contentDoc = firestore.collection("dailyLiturgy")
                .document(dateString)
                .collection("content")
                .document("en")
                .get()
                .await()

            if (contentDoc.exists()) {
                val data = contentDoc.data ?: throw Exception("Empty document")
                val parsed = parseLiturgy(data, imageUrl)
                liturgyCache[dateString] = parsed
                cacheService.cacheLiturgy(parsed, dateString)
                // Only update UI if still on the same day
                if (getDateString() == dateString) {
                    _liturgy.value = parsed
                }
            } else {
                if (getDateString() == dateString && _liturgy.value == null) {
                    _error.value = "No liturgy available for this day"
                }
            }
        } catch (e: Exception) {
            if (getDateString() == dateString && (_liturgy.value == null || showLoading)) {
                _error.value = "Failed to load liturgy: ${e.message}"
            }
        }

        try {
            val verseDoc = firestore.collection("dailyVerse")
                .document(dateString)
                .get()
                .await()

            if (verseDoc.exists()) {
                val data = verseDoc.data ?: run {
                    if (getDateString() == dateString) _isLoading.value = false
                    return
                }
                val parsed = parseVerse(data)
                if (parsed != null) {
                    verseCache[dateString] = parsed
                    cacheService.cacheVerse(parsed, dateString)
                }
                if (getDateString() == dateString) {
                    _verse.value = parsed
                }
                // Update widget data (only for today's verse)
                val todayDate = dateFormat.format(Calendar.getInstance().time)
                if (dateString == todayDate) {
                    updateWidgetData(data, dateString, liturgyImageUrl)
                }
            }
        } catch (_: Exception) { }

        if (getDateString() == dateString) {
            _isLoading.value = false
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseLiturgy(data: Map<String, Any>, imageUrl: String?): DailyLiturgy {
        val readings = data["readings"] as? Map<String, Any> ?: emptyMap()
        val firstReading = readings["firstReading"] as? Map<String, Any> ?: emptyMap()
        val psalm = readings["psalm"] as? Map<String, Any> ?: emptyMap()
        val secondReading = readings["secondReading"] as? Map<String, Any>
        val gospel = readings["gospel"] as? Map<String, Any> ?: emptyMap()
        val saint = data["saintOfDay"] as? Map<String, Any>
        val audioUrlsMap = data["audioUrls"] as? Map<String, Any>

        return DailyLiturgy(
            date = data["date"] as? String ?: "",
            celebration = data["celebration"] as? String ?: "",
            season = data["season"] as? String ?: "",
            liturgicalColor = data["liturgicalColor"] as? String ?: "green",
            saintOfDay = saint?.let {
                SaintOfDay(
                    name = it["name"] as? String ?: "",
                    description = it["description"] as? String ?: "",
                )
            },
            readings = LiturgyReadings(
                firstReading = Reading(
                    reference = firstReading["reference"] as? String ?: "",
                    text = firstReading["text"] as? String ?: "",
                ),
                psalm = Psalm(
                    reference = psalm["reference"] as? String ?: "",
                    response = psalm["response"] as? String ?: "",
                    text = psalm["text"] as? String ?: "",
                ),
                secondReading = secondReading?.let {
                    Reading(
                        reference = it["reference"] as? String ?: "",
                        text = it["text"] as? String ?: "",
                    )
                },
                gospel = Reading(
                    reference = gospel["reference"] as? String ?: "",
                    text = gospel["text"] as? String ?: "",
                ),
            ),
            audioUrls = audioUrlsMap?.let {
                AudioUrls(
                    firstReading = it["first_reading"] as? String,
                    psalm = it["psalm"] as? String,
                    secondReading = it["second_reading"] as? String,
                    gospel = it["gospel"] as? String,
                )
            },
            sermon = data["sermon"] as? String,
            imageUrl = imageUrl,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun updateWidgetData(verseData: Map<String, Any>, dateString: String, imageUrl: String?) {
        val app = getApplication<Application>()
        val lang = getLanguageCode()
        val category = verseData["category"] as? String ?: return
        val versesData = verseData["verses"] as? Map<String, Any> ?: return
        val localizedVerse = versesData[lang] as? Map<String, Any>
            ?: versesData["en"] as? Map<String, Any>
            ?: return

        val text = localizedVerse["text"] as? String ?: return
        val mediumText = localizedVerse["mediumText"] as? String ?: text.take(120) + "..."
        val shortText = localizedVerse["shortText"] as? String ?: text.take(80) + "..."
        val reference = localizedVerse["reference"] as? String ?: ""
        val shortReference = localizedVerse["shortReference"] as? String ?: reference

        val widgetData = VerseWidgetData(
            date = dateString,
            text = text,
            mediumText = mediumText,
            shortText = shortText,
            reference = reference,
            shortReference = shortReference,
            category = category,
            imageUrl = imageUrl,
        )
        VerseWidgetData.save(app, widgetData)

        // Download image and update widgets
        viewModelScope.launch {
            if (imageUrl != null) {
                withContext(Dispatchers.IO) {
                    try {
                        val connection = URL(imageUrl).openConnection()
                        connection.connectTimeout = 10_000
                        connection.readTimeout = 10_000
                        connection.inputStream.use { stream ->
                            val bitmap = BitmapFactory.decodeStream(stream)
                            if (bitmap != null) {
                                VerseWidgetData.saveBackgroundImage(app, bitmap)
                            }
                        }
                    } catch (_: Exception) { }
                }
            }
            VerseWidgetData.updateAllWidgets(app)
        }
    }

    private fun getLanguageCode(): String {
        val lang = Locale.getDefault().language
        return when (lang) {
            "es" -> "es"
            "pt" -> "pt"
            "fr" -> "fr"
            "it" -> "it"
            "de" -> "de"
            "pl" -> "pl"
            else -> "en"
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseVerse(data: Map<String, Any>): DailyVerse? {
        val lang = getLanguageCode()
        val verses = data["verses"] as? Map<String, Any> ?: return null
        val localizedVerse = verses[lang] as? Map<String, Any>
            ?: verses["en"] as? Map<String, Any>
            ?: return null
        val reflections = data["reflection"] as? Map<String, Any>
        val connection = data["liturgicalConnection"] as? Map<String, Any>

        // Resolve localized liturgical connection description
        val connectionDesc = if (connection != null) {
            val localized = connection["descriptionLocalized"] as? Map<String, String>
            localized?.get(lang)
                ?: connection["description"] as? String
        } else null

        return DailyVerse(
            text = localizedVerse["text"] as? String ?: return null,
            reference = localizedVerse["reference"] as? String ?: "",
            category = data["category"] as? String ?: "",
            reflection = reflections?.get(lang) as? String
                ?: reflections?.get("en") as? String,
            liturgicalConnectionType = connection?.get("type") as? String,
            liturgicalConnectionDescription = connectionDesc,
        )
    }
}
