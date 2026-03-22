package com.app.lumen.features.bible.service

import android.content.Context
import com.app.lumen.features.bible.model.BibleChapter
import com.app.lumen.features.bible.model.BibleChapterSummary
import com.app.lumen.features.bible.model.BibleVersion

/**
 * Routes Bible data requests to either Firebase or the remote API.Bible.
 * Firebase-hosted Bibles are served by [BibleFirebaseService].
 * All other Bibles go through [BibleAPIService].
 */
class BibleRoutingService(private val context: Context) {

    companion object {
        @Volatile
        private var instance: BibleRoutingService? = null

        fun getInstance(context: Context): BibleRoutingService {
            return instance ?: synchronized(this) {
                instance ?: BibleRoutingService(context.applicationContext).also { instance = it }
            }
        }
    }

    private val apiService by lazy { BibleAPIService.getInstance(context) }
    private val firebaseService by lazy { BibleFirebaseService.getInstance(context) }

    /**
     * Fetch Bible versions from both backends, Firebase Bibles inserted first.
     */
    suspend fun fetchBibles(language: String?): List<BibleVersion> {
        // 1. Fetch from API (graceful failure)
        val apiVersions = try {
            apiService.fetchBibles(language)
        } catch (_: Exception) {
            emptyList()
        }

        // 2. Fetch from Firebase
        val firebaseVersions = try {
            firebaseService.fetchBibles(language)
        } catch (_: Exception) {
            emptyList()
        }

        // 3. Merge: Firebase first, then API (no duplicates)
        val result = mutableListOf<BibleVersion>()
        result.addAll(firebaseVersions)
        val existingIds = firebaseVersions.map { it.id }.toSet()
        apiVersions.filter { it.id !in existingIds }.let { result.addAll(it) }

        return result
    }

    /**
     * Fetch books for a Bible. Firebase Bibles use cached config, API Bibles hit the API.
     */
    suspend fun fetchBooks(bibleId: String): List<BibleBookInfo> {
        // Try Firebase first (instant, from config)
        val firebaseBooks = firebaseService.fetchBooks(bibleId)
        if (!firebaseBooks.isNullOrEmpty()) return firebaseBooks

        // Fall back to API.Bible
        return apiService.fetchBooks(bibleId)
    }

    /**
     * Fetch chapter summaries for a book. Firebase first, then API.Bible.
     */
    suspend fun fetchChapters(bibleId: String, bookId: String): List<BibleChapterSummary> {
        val firebaseChapters = firebaseService.fetchChapters(bibleId, bookId)
        if (!firebaseChapters.isNullOrEmpty()) return firebaseChapters
        return apiService.fetchChapters(bibleId, bookId)
    }

    /**
     * Fetch a single chapter's content. Firebase first, then API.Bible.
     */
    suspend fun fetchChapter(bibleId: String, chapterId: String): BibleChapter {
        val firebaseChapter = firebaseService.fetchChapter(bibleId, chapterId)
        if (firebaseChapter != null) return firebaseChapter
        return apiService.fetchChapter(bibleId, chapterId)
    }

    /**
     * Resolve a previously-selected Firebase Bible even when it's not in the main list.
     */
    fun resolveSelectedBible(bibleId: String): BibleVersion? {
        return firebaseService.cachedBibleVersion(bibleId)
    }
}
