package com.app.lumen.features.rosary.service

import android.content.Context
import com.app.lumen.features.rosary.model.RosaryAudioConfig
import com.app.lumen.features.rosary.model.RosaryAudioFile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URL

/**
 * Downloads and manages rosary prayer audio files from Firebase.
 *
 * Files are stored in the app's internal files directory under RosaryAudio/{language}/
 * to survive cache eviction. A `_manifest.json` marks a completed download.
 */
class RosaryAudioService private constructor(private val context: Context) {

    private val firestore = FirebaseFirestore.getInstance()
    private val json = Json { ignoreUnknownKeys = true }
    private val baseDirectory = File(context.filesDir, "RosaryAudio")

    private var currentDownloadJob: Job? = null
    private val cachedConfigs = mutableMapOf<String, RosaryAudioConfig>()

    private val _downloadProgress = MutableStateFlow(0.0)
    val downloadProgress: StateFlow<Double> = _downloadProgress

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading

    // Chaplet download state (separate from rosary so they don't interfere)
    private var currentChapletDownloadJob: Job? = null
    private val cachedChapletConfigs = mutableMapOf<String, RosaryAudioConfig>()

    private val _chapletDownloadProgress = MutableStateFlow(0.0)
    val chapletDownloadProgress: StateFlow<Double> = _chapletDownloadProgress

    private val _isChapletDownloading = MutableStateFlow(false)
    val isChapletDownloading: StateFlow<Boolean> = _isChapletDownloading

    /** Max concurrent file downloads */
    private val maxConcurrentDownloads = 6

    // MARK: - Public API

    /** Fetch the audio config from Firestore for a given language. */
    suspend fun fetchAudioConfig(language: String): RosaryAudioConfig? {
        cachedConfigs[language]?.let { return it }

        return try {
            val document = firestore.collection("audio").document(language).get().await()
            if (!document.exists()) return null

            val data = document.data?.toMutableMap() ?: return null
            // Remove Firestore Timestamp fields (not JSON-serializable)
            data.remove("createdAt")
            data.remove("updatedAt")

            val jsonString = Json.encodeToString(
                kotlinx.serialization.json.JsonElement.serializer(),
                toJsonElement(data)
            )
            val config = json.decodeFromString<RosaryAudioConfig>(jsonString)
            cachedConfigs[language] = config
            config
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Check if audio has been fully downloaded for a language. */
    fun isAudioDownloaded(language: String): Boolean {
        val manifestFile = File(languageDirectory(language), "_manifest.json")
        return manifestFile.exists()
    }

    /** Download all audio files for a language with progress reporting. */
    suspend fun downloadAudio(language: String) {
        currentDownloadJob?.cancel()
        _isDownloading.value = true
        _downloadProgress.value = 0.0

        currentDownloadJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val config = fetchAudioConfig(language) ?: run {
                    _isDownloading.value = false
                    return@launch
                }

                val langDir = languageDirectory(language)
                langDir.mkdirs()

                downloadFilesInParallel(config.files, langDir, config.totalSize)

                writeManifest(language, config.version)

                _downloadProgress.value = 1.0
                _isDownloading.value = false
            } catch (e: CancellationException) {
                _isDownloading.value = false
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                _isDownloading.value = false
            }
        }

        currentDownloadJob?.join()
    }

    /** Cancel any in-progress download. */
    fun cancelDownload() {
        currentDownloadJob?.cancel()
        currentDownloadJob = null
        _isDownloading.value = false
    }

    /** Get the local file for a given storage path. */
    fun localFile(storagePath: String, language: String): File? {
        val langDir = languageDirectory(language)
        val file = File(langDir, localFileName(storagePath))
        return if (file.exists()) file else null
    }

    /** Delete downloaded audio for a language. */
    fun deleteAudio(language: String) {
        languageDirectory(language).deleteRecursively()
    }

    // MARK: - Chaplet Audio API

    /** Fetch the audio config from Firestore for a chaplet type and language. */
    suspend fun fetchChapletAudioConfig(language: String, chapletType: String): RosaryAudioConfig? {
        val cacheKey = "${chapletType}_$language"
        cachedChapletConfigs[cacheKey]?.let { return it }

        return try {
            val document = firestore.collection("chapletAudio")
                .document(chapletType)
                .collection("languages")
                .document(language)
                .get().await()
            if (!document.exists()) return null

            val data = document.data?.toMutableMap() ?: return null
            data.remove("createdAt")
            data.remove("updatedAt")

            val jsonString = Json.encodeToString(
                kotlinx.serialization.json.JsonElement.serializer(),
                toJsonElement(data)
            )
            val config = json.decodeFromString<RosaryAudioConfig>(jsonString)
            cachedChapletConfigs[cacheKey] = config
            config
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Check if chaplet audio has been fully downloaded. */
    fun isChapletAudioDownloaded(language: String, chapletType: String): Boolean {
        val manifestFile = File(chapletDirectory(language, chapletType), "_manifest.json")
        return manifestFile.exists()
    }

    /** Download all audio files for a chaplet with progress reporting. */
    suspend fun downloadChapletAudio(language: String, chapletType: String) {
        currentChapletDownloadJob?.cancel()
        _isChapletDownloading.value = true
        _chapletDownloadProgress.value = 0.0

        currentChapletDownloadJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val config = fetchChapletAudioConfig(language, chapletType) ?: run {
                    _isChapletDownloading.value = false
                    return@launch
                }

                val chapletDir = chapletDirectory(language, chapletType)
                chapletDir.mkdirs()

                downloadChapletFilesInParallel(config.files, chapletDir, config.totalSize)

                writeChapletManifest(language, chapletType, config.version)

                _chapletDownloadProgress.value = 1.0
                _isChapletDownloading.value = false
            } catch (e: CancellationException) {
                _isChapletDownloading.value = false
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                _isChapletDownloading.value = false
            }
        }

        currentChapletDownloadJob?.join()
    }

    /** Get the local file for a chaplet audio storage path. */
    fun chapletLocalFile(storagePath: String, language: String, chapletType: String): File? {
        // Try chaplet directory first
        val chapletDir = chapletDirectory(language, chapletType)
        val chapletFile = File(chapletDir, localFileName(storagePath))
        if (chapletFile.exists()) return chapletFile

        // Fall back to rosary directory for shared prayers
        val rosaryDir = languageDirectory(language)
        val rosaryFile = File(rosaryDir, localFileName(storagePath))
        return if (rosaryFile.exists()) rosaryFile else null
    }

    private fun chapletDirectory(language: String, chapletType: String) =
        File(baseDirectory, "chaplets/$chapletType/$language")

    private fun writeChapletManifest(language: String, chapletType: String, version: Int) {
        val manifest = """{"version":$version,"downloadedAt":"${System.currentTimeMillis()}"}"""
        File(chapletDirectory(language, chapletType), "_manifest.json").writeText(manifest)
    }

    private suspend fun downloadChapletFilesInParallel(
        files: List<RosaryAudioFile>,
        directory: File,
        totalSize: Int,
    ) = coroutineScope {
        val totalSizeDouble = totalSize.toDouble()
        val semaphore = Semaphore(maxConcurrentDownloads)
        var cumulativeBytes = 0.0

        val filesToDownload = mutableListOf<Pair<RosaryAudioFile, File>>()
        for (file in files) {
            val localFile = File(directory, localFileName(file.storagePath))
            if (localFile.exists()) {
                cumulativeBytes += file.size.toDouble()
                _chapletDownloadProgress.value = if (totalSizeDouble > 0) cumulativeBytes / totalSizeDouble else 0.0
            } else {
                filesToDownload.add(file to localFile)
            }
        }

        if (filesToDownload.isEmpty()) return@coroutineScope

        filesToDownload.map { it.second.parentFile }.toSet().forEach { it?.mkdirs() }

        val jobs = filesToDownload.map { (audioFile, localFile) ->
            async {
                semaphore.acquire()
                try {
                    if (!isActive) return@async
                    val data = URL(audioFile.downloadUrl).readBytes()
                    localFile.writeBytes(data)
                    synchronized(this@RosaryAudioService) {
                        cumulativeBytes += audioFile.size.toDouble()
                        _chapletDownloadProgress.value =
                            if (totalSizeDouble > 0) (cumulativeBytes / totalSizeDouble).coerceAtMost(1.0) else 0.0
                    }
                } finally {
                    semaphore.release()
                }
            }
        }

        jobs.awaitAll()
    }

    // MARK: - Parallel Download

    private suspend fun downloadFilesInParallel(
        files: List<RosaryAudioFile>,
        directory: File,
        totalSize: Int,
    ) = coroutineScope {
        val totalSizeDouble = totalSize.toDouble()
        val semaphore = Semaphore(maxConcurrentDownloads)
        var cumulativeBytes = 0.0

        // First pass: account for already-downloaded files
        val filesToDownload = mutableListOf<Pair<RosaryAudioFile, File>>()
        for (file in files) {
            val localFile = File(directory, localFileName(file.storagePath))
            if (localFile.exists()) {
                cumulativeBytes += file.size.toDouble()
                _downloadProgress.value = if (totalSizeDouble > 0) cumulativeBytes / totalSizeDouble else 0.0
            } else {
                filesToDownload.add(file to localFile)
            }
        }

        if (filesToDownload.isEmpty()) return@coroutineScope

        // Create all needed subdirectories upfront
        filesToDownload.map { it.second.parentFile }.toSet().forEach { it?.mkdirs() }

        // Download in parallel with semaphore limiting
        val jobs = filesToDownload.map { (audioFile, localFile) ->
            async {
                semaphore.acquire()
                try {
                    if (!isActive) return@async
                    val data = URL(audioFile.downloadUrl).readBytes()
                    localFile.writeBytes(data)
                    synchronized(this@RosaryAudioService) {
                        cumulativeBytes += audioFile.size.toDouble()
                        _downloadProgress.value =
                            if (totalSizeDouble > 0) (cumulativeBytes / totalSizeDouble).coerceAtMost(1.0) else 0.0
                    }
                } finally {
                    semaphore.release()
                }
            }
        }

        jobs.awaitAll()
    }

    // MARK: - Private

    private fun languageDirectory(language: String) = File(baseDirectory, language)

    /**
     * Convert a storage path like "audio/en/prayers/hail_mary_1.mp3"
     * to a local relative path "prayers/hail_mary_1.mp3"
     */
    private fun localFileName(storagePath: String): String {
        val components = storagePath.split("/")
        return if (components.size >= 3) {
            components.drop(2).joinToString("/")
        } else {
            storagePath
        }
    }

    private fun writeManifest(language: String, version: Int) {
        val manifest = """{"version":$version,"downloadedAt":"${System.currentTimeMillis()}"}"""
        File(languageDirectory(language), "_manifest.json").writeText(manifest)
    }

    // MARK: - Firestore data to JSON conversion

    private fun toJsonElement(value: Any?): kotlinx.serialization.json.JsonElement {
        return when (value) {
            null -> kotlinx.serialization.json.JsonNull
            is Boolean -> kotlinx.serialization.json.JsonPrimitive(value)
            is Number -> kotlinx.serialization.json.JsonPrimitive(value)
            is String -> kotlinx.serialization.json.JsonPrimitive(value)
            is Map<*, *> -> kotlinx.serialization.json.JsonObject(
                value.entries.associate { (k, v) -> k.toString() to toJsonElement(v) }
            )
            is List<*> -> kotlinx.serialization.json.JsonArray(value.map { toJsonElement(it) })
            else -> kotlinx.serialization.json.JsonPrimitive(value.toString())
        }
    }

    companion object {
        @Volatile
        private var instance: RosaryAudioService? = null

        fun getInstance(context: Context): RosaryAudioService {
            return instance ?: synchronized(this) {
                instance ?: RosaryAudioService(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
