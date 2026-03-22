package com.app.lumen.features.bible.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.lumen.features.bible.model.*
import com.app.lumen.features.bible.service.BibleBookInfo
import com.app.lumen.features.bible.service.BibleRoutingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class LoadingState {
    IDLE, LOADING, LOADED, ERROR
}

class BibleViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val PREFS_NAME = "bible_settings"
        private const val KEY_SELECTED_BIBLE_ID = "selected_bible_id"
        private const val KEY_SELECTED_LANGUAGE = "selected_language"

        // Preferred Bible IDs per language (Firebase first, then API.Bible fallback)
        private val PREFERRED_BIBLES = mapOf(
            "eng" to listOf("WEBC", "72f4e6dc683324df-03"),
            "spa" to listOf("LPD", "48acedcf8595c754-01"),
            "pol" to listOf("BT5", "1c9761e0230da6e0-01"),
            "por" to listOf("MS", "941380703fcb500c-01"),
            "fra" to listOf("AELF", "a93a92589195411f-01"),
            "ita" to listOf("CEI2008", "41f25b97f468e10b-01"),
            "deu" to listOf("EU"),
        )
    }

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val routingService = BibleRoutingService.getInstance(application)

    // ── State ───────────────────────────────────────────────────────

    private val _bibleVersions = MutableStateFlow<List<BibleVersion>>(emptyList())
    val bibleVersions: StateFlow<List<BibleVersion>> = _bibleVersions.asStateFlow()

    private val _selectedBible = MutableStateFlow<BibleVersion?>(null)
    val selectedBible: StateFlow<BibleVersion?> = _selectedBible.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(loadSavedLanguage())
    val selectedLanguage: StateFlow<BibleLanguageOption> = _selectedLanguage.asStateFlow()

    private val _loadingState = MutableStateFlow(LoadingState.IDLE)
    val loadingState: StateFlow<LoadingState> = _loadingState.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _books = MutableStateFlow<List<BibleBookInfo>>(emptyList())
    val books: StateFlow<List<BibleBookInfo>> = _books.asStateFlow()

    private val _booksLoadingState = MutableStateFlow(LoadingState.IDLE)
    val booksLoadingState: StateFlow<LoadingState> = _booksLoadingState.asStateFlow()

    init {
        loadBibleVersions()
    }

    // ── Public Methods ──────────────────────────────────────────────

    fun loadBibleVersions() {
        viewModelScope.launch {
            _loadingState.value = LoadingState.LOADING
            _error.value = null

            try {
                val versions = routingService.fetchBibles(_selectedLanguage.value.code)

                // Deduplicate by dblId (keep Catholic priority)
                val deduped = deduplicateVersions(versions)

                _bibleVersions.value = deduped
                _loadingState.value = LoadingState.LOADED

                // Restore or auto-select best Bible
                val savedId = prefs.getString(KEY_SELECTED_BIBLE_ID, null)
                if (savedId != null) {
                    val saved = deduped.find { it.id == savedId }
                        ?: routingService.resolveSelectedBible(savedId)
                    if (saved != null) {
                        _selectedBible.value = saved
                        loadBooks()
                        return@launch
                    }
                }
                selectBestBible(deduped)
            } catch (e: Exception) {
                _loadingState.value = LoadingState.ERROR
                _error.value = e.message ?: "Failed to load Bible versions"
            }
        }
    }

    fun selectBible(bible: BibleVersion) {
        if (bible.id == _selectedBible.value?.id) return
        _selectedBible.value = bible
        prefs.edit().putString(KEY_SELECTED_BIBLE_ID, bible.id).apply()
        _books.value = emptyList()
        loadBooks()
    }

    fun loadBooks() {
        val bibleId = _selectedBible.value?.id ?: return
        if (_booksLoadingState.value == LoadingState.LOADING) return

        viewModelScope.launch {
            _booksLoadingState.value = LoadingState.LOADING
            try {
                val fetchedBooks = routingService.fetchBooks(bibleId)
                _books.value = fetchedBooks
                _booksLoadingState.value = LoadingState.LOADED
            } catch (e: Exception) {
                _booksLoadingState.value = LoadingState.ERROR
            }
        }
    }

    fun changeLanguage(language: BibleLanguageOption) {
        if (language == _selectedLanguage.value) return
        _selectedLanguage.value = language
        prefs.edit().putString(KEY_SELECTED_LANGUAGE, language.code).apply()
        // Only reload the versions list for the picker — keep current Bible & books
        _bibleVersions.value = emptyList()
        loadBibleVersionsOnly()
    }

    private fun loadBibleVersionsOnly() {
        viewModelScope.launch {
            _loadingState.value = LoadingState.LOADING
            _error.value = null
            try {
                val versions = routingService.fetchBibles(_selectedLanguage.value.code)
                _bibleVersions.value = deduplicateVersions(versions)
                _loadingState.value = LoadingState.LOADED
            } catch (e: Exception) {
                _loadingState.value = LoadingState.ERROR
                _error.value = e.message ?: "Failed to load Bible versions"
            }
        }
    }

    // ── Private Helpers ─────────────────────────────────────────────

    private fun loadSavedLanguage(): BibleLanguageOption {
        val code = prefs.getString(KEY_SELECTED_LANGUAGE, null)
        return if (code != null) {
            BibleLanguageOption.fromCode(code)
        } else {
            BibleLanguageOption.fromSystemLocale()
        }
    }

    private fun selectBestBible(versions: List<BibleVersion>) {
        if (versions.isEmpty()) return

        val lang = _selectedLanguage.value.code
        val preferred = PREFERRED_BIBLES[lang] ?: emptyList()

        // Try preferred IDs in order
        for (id in preferred) {
            val match = versions.find { it.id == id || it.displayAbbreviation == id }
            if (match != null) {
                selectBible(match)
                return
            }
        }

        // Fallback: first Catholic Bible, then first in list
        val catholic = versions.find { it.tradition == BibleTradition.CATHOLIC }
        selectBible(catholic ?: versions.first())
    }

    private fun deduplicateVersions(versions: List<BibleVersion>): List<BibleVersion> {
        val seen = mutableMapOf<String, BibleVersion>()
        val result = mutableListOf<BibleVersion>()

        for (v in versions) {
            val key = v.dblId
            if (key == null) {
                result.add(v)
                continue
            }
            val existing = seen[key]
            if (existing == null) {
                seen[key] = v
                result.add(v)
            } else if (v.tradition.sortOrder < existing.tradition.sortOrder) {
                // Replace with higher-priority tradition
                result.remove(existing)
                result.add(v)
                seen[key] = v
            }
        }

        return result
    }
}
