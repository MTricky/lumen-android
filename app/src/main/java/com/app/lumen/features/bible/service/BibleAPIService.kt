package com.app.lumen.features.bible.service

import android.content.Context
import com.app.lumen.features.bible.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

/**
 * REST client for API.Bible (https://rest.api.bible/v1).
 * Includes rate limiting (3000 req/day) and disk caching.
 */
class BibleAPIService(private val context: Context) {

    companion object {
        private const val BASE_URL = "https://rest.api.bible/v1"
        private const val DEFAULT_API_KEY = "AB2TcOfV_GiefxlulGQb6"
        private const val DAILY_LIMIT = 3000
        private const val PREFS_NAME = "bible_api_prefs"
        private const val KEY_DAILY_COUNT = "daily_request_count"
        private const val KEY_REQUEST_DATE = "request_date"

        @Volatile
        private var instance: BibleAPIService? = null

        fun getInstance(context: Context): BibleAPIService {
            return instance ?: synchronized(this) {
                instance ?: BibleAPIService(context.applicationContext).also { instance = it }
            }
        }
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val prefs by lazy { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    private val cacheDir by lazy {
        File(context.cacheDir, "BibleAPICache").also { it.mkdirs() }
    }

    // Memory cache for versions (avoids disk reads on repeated calls)
    private val versionsMemoryCache = mutableMapOf<String, List<BibleVersion>>()

    private var apiKey: String = DEFAULT_API_KEY

    fun setApiKey(key: String) {
        if (key.isNotEmpty()) apiKey = key
    }

    // ── Rate Limiting ───────────────────────────────────────────────

    private fun getDailyCount(): Int {
        val savedDay = prefs.getInt(KEY_REQUEST_DATE, -1)
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        if (savedDay != today) {
            prefs.edit().putInt(KEY_DAILY_COUNT, 0).putInt(KEY_REQUEST_DATE, today).apply()
            return 0
        }
        return prefs.getInt(KEY_DAILY_COUNT, 0)
    }

    private fun incrementCount() {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val current = getDailyCount()
        prefs.edit().putInt(KEY_DAILY_COUNT, current + 1).putInt(KEY_REQUEST_DATE, today).apply()
    }

    private fun checkRateLimit() {
        if (getDailyCount() >= DAILY_LIMIT) {
            throw BibleAPIException("Daily API limit reached. Please try again tomorrow.")
        }
    }

    // ── API Methods ─────────────────────────────────────────────────

    suspend fun fetchBibles(language: String?): List<BibleVersion> = withContext(Dispatchers.IO) {
        val cacheKey = "versions_${language ?: "all"}"

        // 1. Memory cache (instant)
        versionsMemoryCache[cacheKey]?.let { return@withContext it }

        // 2. Disk cache (valid for 30 days — versions rarely change)
        val cached = readCache(cacheKey, 30L * 24 * 60 * 60 * 1000)
        if (cached != null) {
            return@withContext try {
                val versions = json.decodeFromString<List<BibleVersion>>(cached)
                versionsMemoryCache[cacheKey] = versions
                versions
            } catch (_: Exception) {
                fetchBiblesFromNetwork(language)
            }
        }

        // 3. Network
        fetchBiblesFromNetwork(language)
    }

    private suspend fun fetchBiblesFromNetwork(language: String?): List<BibleVersion> =
        withContext(Dispatchers.IO) {
            checkRateLimit()
            val url = if (language != null) {
                "$BASE_URL/bibles?language=$language"
            } else {
                "$BASE_URL/bibles"
            }

            val responseText = makeRequest(url)
            incrementCount()

            // Parse the API response: { "data": [...] }
            val versions = try {
                val wrapper = json.decodeFromString<BibleAPIListResponse>(responseText)
                wrapper.data.map { it.toBibleVersion() }
            } catch (e: Exception) {
                throw BibleAPIException("Failed to parse Bible versions: ${e.message}")
            }

            // Cache to memory + disk
            val cacheKey = "versions_${language ?: "all"}"
            versionsMemoryCache[cacheKey] = versions
            writeCache(cacheKey, json.encodeToString(kotlinx.serialization.builtins.ListSerializer(BibleVersion.serializer()), versions))

            versions
        }

    suspend fun fetchBooks(bibleId: String): List<BibleBookInfo> = withContext(Dispatchers.IO) {
        val cacheKey = "books_$bibleId"
        val cached = readCache(cacheKey, 30L * 24 * 60 * 60 * 1000)
        if (cached != null) {
            return@withContext try {
                json.decodeFromString<List<BibleBookInfo>>(cached)
            } catch (_: Exception) {
                fetchBooksFromNetwork(bibleId)
            }
        }
        fetchBooksFromNetwork(bibleId)
    }

    private suspend fun fetchBooksFromNetwork(bibleId: String): List<BibleBookInfo> =
        withContext(Dispatchers.IO) {
            checkRateLimit()
            val responseText = makeRequest("$BASE_URL/bibles/$bibleId/books")
            incrementCount()
            val wrapper = json.decodeFromString<BibleAPIBookListResponse>(responseText)
            val books = wrapper.data
            writeCache("books_$bibleId", json.encodeToString(kotlinx.serialization.builtins.ListSerializer(BibleBookInfo.serializer()), books))
            books
        }

    suspend fun fetchChapters(bibleId: String, bookId: String): List<BibleChapterSummary> =
        withContext(Dispatchers.IO) {
            val cacheKey = "chapters_${bibleId}_$bookId"
            val cached = readCache(cacheKey, 30L * 24 * 60 * 60 * 1000)
            if (cached != null) {
                return@withContext try {
                    json.decodeFromString<List<BibleChapterSummary>>(cached)
                } catch (_: Exception) {
                    fetchChaptersFromNetwork(bibleId, bookId)
                }
            }
            fetchChaptersFromNetwork(bibleId, bookId)
        }

    private suspend fun fetchChaptersFromNetwork(
        bibleId: String,
        bookId: String,
    ): List<BibleChapterSummary> = withContext(Dispatchers.IO) {
        checkRateLimit()
        val responseText = makeRequest("$BASE_URL/bibles/$bibleId/books/$bookId/chapters")
        incrementCount()
        val wrapper = json.decodeFromString<BibleAPIChapterListResponse>(responseText)
        val chapters = wrapper.data.map {
            BibleChapterSummary(id = it.id, number = it.number, reference = it.reference)
        }
        writeCache(
            "chapters_${bibleId}_$bookId",
            json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(BibleChapterSummary.serializer()),
                chapters,
            ),
        )
        chapters
    }

    suspend fun fetchChapter(bibleId: String, chapterId: String): BibleChapter =
        withContext(Dispatchers.IO) {
            val cacheKey = "chapter_${bibleId}_$chapterId"
            val cached = readCache(cacheKey, 30L * 24 * 60 * 60 * 1000)
            if (cached != null) {
                return@withContext try {
                    json.decodeFromString<BibleChapter>(cached)
                } catch (_: Exception) {
                    fetchChapterFromNetwork(bibleId, chapterId)
                }
            }
            fetchChapterFromNetwork(bibleId, chapterId)
        }

    private suspend fun fetchChapterFromNetwork(
        bibleId: String,
        chapterId: String,
    ): BibleChapter = withContext(Dispatchers.IO) {
        checkRateLimit()
        val url = "$BASE_URL/bibles/$bibleId/chapters/$chapterId" +
                "?content-type=html&include-verse-numbers=true"
        val responseText = makeRequest(url)
        incrementCount()
        val wrapper = json.decodeFromString<BibleAPIChapterResponse>(responseText)
        val chapter = wrapper.data.toBibleChapter()
        writeCache(
            "chapter_${bibleId}_$chapterId",
            json.encodeToString(BibleChapter.serializer(), chapter),
        )
        chapter
    }

    // ── HTTP ────────────────────────────────────────────────────────

    private fun makeRequest(urlString: String): String {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("api-key", apiKey)
        conn.setRequestProperty("Accept", "application/json")
        conn.connectTimeout = 30_000
        conn.readTimeout = 30_000

        val code = conn.responseCode
        if (code != 200) {
            val errorBody = try {
                conn.errorStream?.bufferedReader()?.readText() ?: ""
            } catch (_: Exception) { "" }
            throw BibleAPIException("API error $code: $errorBody")
        }

        return conn.inputStream.bufferedReader().readText()
    }

    // ── Disk Cache ──────────────────────────────────────────────────

    private fun readCache(key: String, maxAge: Long): String? {
        val file = File(cacheDir, "$key.json")
        if (!file.exists()) return null
        if (System.currentTimeMillis() - file.lastModified() > maxAge) {
            file.delete()
            return null
        }
        return try { file.readText() } catch (_: Exception) { null }
    }

    private fun writeCache(key: String, data: String) {
        try {
            File(cacheDir, "$key.json").writeText(data)
        } catch (_: Exception) { /* ignore cache write failures */ }
    }
}

class BibleAPIException(message: String) : Exception(message)

// ── API response models (match API.Bible JSON shape) ────────────────
@kotlinx.serialization.Serializable
data class APIBibleEntry(
    val id: String,
    val dblId: String? = null,
    val abbreviation: String? = null,
    val abbreviationLocal: String? = null,
    val name: String,
    val nameLocal: String? = null,
    val description: String? = null,
    val descriptionLocal: String? = null,
    val language: BibleLanguage,
) {
    fun toBibleVersion(): BibleVersion = BibleVersion(
        id = id, dblId = dblId, abbreviation = abbreviation,
        abbreviationLocal = abbreviationLocal, name = name,
        nameLocal = nameLocal, description = description,
        descriptionLocal = descriptionLocal, language = language,
    )
}

@kotlinx.serialization.Serializable
data class BibleAPIListResponse(val data: List<APIBibleEntry>)

@kotlinx.serialization.Serializable
data class BibleBookInfo(
    val id: String,
    val bibleId: String = "",
    val abbreviation: String = "",
    val name: String = "",
    val nameLong: String = "",
)

@kotlinx.serialization.Serializable
data class BibleAPIBookListResponse(val data: List<BibleBookInfo>)

@kotlinx.serialization.Serializable
data class APIChapterSummaryEntry(
    val id: String,
    val number: String,
    val reference: String = "",
)

@kotlinx.serialization.Serializable
data class BibleAPIChapterListResponse(val data: List<APIChapterSummaryEntry>)

@kotlinx.serialization.Serializable
data class APIChapterData(
    val id: String,
    val number: String,
    val content: String = "",
    val verseCount: Int = 0,
    val next: com.app.lumen.features.bible.model.ChapterNav? = null,
    val previous: com.app.lumen.features.bible.model.ChapterNav? = null,
) {
    fun toBibleChapter(): BibleChapter = BibleChapter(
        id = id, number = number, content = content,
        verseCount = verseCount, next = next, previous = previous,
    )
}

@kotlinx.serialization.Serializable
data class BibleAPIChapterResponse(val data: APIChapterData)
