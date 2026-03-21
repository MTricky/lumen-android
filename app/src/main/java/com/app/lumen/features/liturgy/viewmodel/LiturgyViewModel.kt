package com.app.lumen.features.liturgy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.lumen.features.liturgy.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

enum class DaySelection { TODAY, TOMORROW }

class LiturgyViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val _daySelection = MutableStateFlow(DaySelection.TODAY)
    val daySelection: StateFlow<DaySelection> = _daySelection

    private val _liturgy = MutableStateFlow<DailyLiturgy?>(null)
    val liturgy: StateFlow<DailyLiturgy?> = _liturgy

    private val _verse = MutableStateFlow<DailyVerse?>(null)
    val verse: StateFlow<DailyVerse?> = _verse

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // In-memory cache
    private val liturgyCache = mutableMapOf<String, DailyLiturgy>()
    private val verseCache = mutableMapOf<String, DailyVerse>()

    init {
        loadData()
    }

    fun selectDay(day: DaySelection) {
        if (_daySelection.value == day) return
        _daySelection.value = day
        val dateString = getDateString()

        // Show cached data immediately if available
        val cachedLiturgy = liturgyCache[dateString]
        val cachedVerse = verseCache[dateString]
        if (cachedLiturgy != null) {
            _liturgy.value = cachedLiturgy
            _verse.value = cachedVerse
            _error.value = null
            // Still refresh in background
            refreshData(showLoading = false)
        } else {
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

    private fun refreshData(showLoading: Boolean) {
        viewModelScope.launch {
            if (showLoading) _isLoading.value = true
            _error.value = null
            val dateString = getDateString()

            try {
                val mainDoc = firestore.collection("dailyLiturgy")
                    .document(dateString)
                    .get()
                    .await()

                val imageUrl = mainDoc.data?.get("imageUrl") as? String

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
                    _liturgy.value = parsed
                } else {
                    _error.value = "No liturgy available for this day"
                }
            } catch (e: Exception) {
                if (_liturgy.value == null || showLoading) {
                    _error.value = "Failed to load liturgy: ${e.message}"
                }
            }

            try {
                val verseDoc = firestore.collection("dailyVerse")
                    .document(dateString)
                    .get()
                    .await()

                if (verseDoc.exists()) {
                    val data = verseDoc.data ?: return@launch
                    val parsed = parseVerse(data)
                    if (parsed != null) {
                        verseCache[dateString] = parsed
                    }
                    _verse.value = parsed
                }
            } catch (_: Exception) { }

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
            sermon = data["sermon"] as? String,
            imageUrl = imageUrl,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseVerse(data: Map<String, Any>): DailyVerse? {
        val verses = data["verses"] as? Map<String, Any> ?: return null
        val en = verses["en"] as? Map<String, Any> ?: return null
        val reflections = data["reflection"] as? Map<String, Any>

        return DailyVerse(
            text = en["text"] as? String ?: return null,
            reference = en["reference"] as? String ?: "",
            category = data["category"] as? String ?: "",
            reflection = reflections?.get("en") as? String,
        )
    }
}
