package com.app.lumen.features.bible.service

import android.content.Context
import com.app.lumen.features.bible.model.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Fetches Bible data from Firebase (Firestore config + Storage per-book JSONs).
 * Firestore `bibles` collection holds config documents.
 * Firebase Storage holds per-book JSON files at `bibles/{bibleId}/{bookId}.json`.
 */
class BibleFirebaseService(private val context: Context) {

    companion object {
        private const val CONFIG_CACHE_VALIDITY_MS = 24 * 60 * 60 * 1000L // 24 hours

        @Volatile
        private var instance: BibleFirebaseService? = null

        fun getInstance(context: Context): BibleFirebaseService {
            return instance ?: synchronized(this) {
                instance ?: BibleFirebaseService(context.applicationContext).also { instance = it }
            }
        }
    }

    private val firestore = FirebaseFirestore.getInstance()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val cacheDir by lazy {
        File(context.cacheDir, "FirebaseBibles").also { it.mkdirs() }
    }

    // In-memory config cache
    private val configs = mutableMapOf<String, FirebaseBibleConfig>()
    val knownBibleIds = mutableSetOf<String>()

    fun handles(bibleId: String): Boolean = knownBibleIds.contains(bibleId)

    suspend fun fetchBibles(language: String?): List<BibleVersion> = withContext(Dispatchers.IO) {
        val lang = language ?: "all"

        // Try disk cache first
        val indexFile = File(cacheDir, "index_$lang.json")
        if (indexFile.exists() &&
            System.currentTimeMillis() - indexFile.lastModified() < CONFIG_CACHE_VALIDITY_MS
        ) {
            val cached = loadCachedVersions(indexFile)
            if (cached.isNotEmpty()) return@withContext cached
        }

        // Query Firestore
        val query = if (language != null) {
            firestore.collection("bibles").whereEqualTo("language.id", language)
        } else {
            firestore.collection("bibles")
        }

        val snapshot = try {
            query.get().await()
        } catch (e: Exception) {
            return@withContext emptyList()
        }

        val versions = mutableListOf<BibleVersion>()
        val bibleIds = mutableListOf<String>()

        for (doc in snapshot.documents) {
            val config = parseConfig(doc) ?: continue
            configs[config.id] = config
            knownBibleIds.add(config.id)
            saveConfigToDisk(config)
            versions.add(config.toBibleVersion())
            bibleIds.add(config.id)
        }

        // Save index
        try {
            indexFile.writeText(json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(kotlinx.serialization.serializer<String>()),
                bibleIds,
            ))
        } catch (_: Exception) {}

        versions
    }

    fun fetchBooks(bibleId: String): List<BibleBookInfo>? {
        val config = configs[bibleId] ?: loadConfigFromDisk(bibleId) ?: return null
        return config.books.map { entry ->
            BibleBookInfo(
                id = entry.id,
                bibleId = entry.bibleId,
                abbreviation = entry.abbreviation,
                name = entry.name,
                nameLong = entry.nameLong,
            )
        }
    }

    fun cachedBibleVersion(bibleId: String): BibleVersion? {
        val config = configs[bibleId] ?: loadConfigFromDisk(bibleId)
        return config?.toBibleVersion()
    }

    // ── Parsing ─────────────────────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    private fun parseConfig(doc: com.google.firebase.firestore.DocumentSnapshot): FirebaseBibleConfig? {
        val data = doc.data ?: return null
        return try {
            val langMap = data["language"] as? Map<String, Any?> ?: return null
            val language = BibleLanguage(
                id = langMap["id"] as? String ?: return null,
                name = langMap["name"] as? String ?: "",
                nameLocal = langMap["nameLocal"] as? String,
                script = langMap["script"] as? String,
                scriptDirection = langMap["scriptDirection"] as? String,
            )

            val booksList = (data["books"] as? List<Map<String, Any?>>)?.map { bookMap ->
                FirebaseBookEntry(
                    id = bookMap["id"] as? String ?: "",
                    bibleId = bookMap["bibleId"] as? String ?: "",
                    abbreviation = bookMap["abbreviation"] as? String ?: "",
                    name = bookMap["name"] as? String ?: "",
                    nameLong = bookMap["nameLong"] as? String ?: "",
                    chapters = (bookMap["chapters"] as? Number)?.toInt() ?: 0,
                    verses = (bookMap["verses"] as? Number)?.toInt() ?: 0,
                    storagePath = bookMap["storagePath"] as? String ?: "",
                    downloadUrl = bookMap["downloadUrl"] as? String ?: "",
                )
            } ?: emptyList()

            FirebaseBibleConfig(
                id = doc.id,
                dblId = data["dblId"] as? String,
                abbreviation = data["abbreviation"] as? String,
                abbreviationLocal = data["abbreviationLocal"] as? String,
                name = data["name"] as? String ?: doc.id,
                nameLocal = data["nameLocal"] as? String,
                description = data["description"] as? String,
                descriptionLocal = data["descriptionLocal"] as? String,
                language = language,
                books = booksList,
                totalBooks = (data["totalBooks"] as? Number)?.toInt() ?: booksList.size,
                totalChapters = (data["totalChapters"] as? Number)?.toInt() ?: 0,
                totalVerses = (data["totalVerses"] as? Number)?.toInt() ?: 0,
                version = (data["version"] as? Number)?.toInt() ?: 1,
            )
        } catch (_: Exception) {
            null
        }
    }

    // ── Disk Cache ──────────────────────────────────────────────────

    private fun saveConfigToDisk(config: FirebaseBibleConfig) {
        try {
            val file = File(cacheDir, "config_${config.id}.json")
            file.writeText(json.encodeToString(FirebaseBibleConfig.serializer(), config))
        } catch (_: Exception) {}
    }

    private fun loadConfigFromDisk(bibleId: String): FirebaseBibleConfig? {
        val file = File(cacheDir, "config_$bibleId.json")
        if (!file.exists()) return null
        return try {
            val config = json.decodeFromString(FirebaseBibleConfig.serializer(), file.readText())
            configs[bibleId] = config
            knownBibleIds.add(bibleId)
            config
        } catch (_: Exception) { null }
    }

    private fun loadCachedVersions(indexFile: File): List<BibleVersion> {
        return try {
            val ids = json.decodeFromString(
                kotlinx.serialization.builtins.ListSerializer(kotlinx.serialization.serializer<String>()),
                indexFile.readText(),
            )
            ids.mapNotNull { bibleId ->
                val config = configs[bibleId] ?: loadConfigFromDisk(bibleId)
                config?.toBibleVersion()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
