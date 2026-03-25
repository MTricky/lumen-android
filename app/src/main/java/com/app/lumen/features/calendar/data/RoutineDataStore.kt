package com.app.lumen.features.calendar.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * JSON-based persistence for routine data.
 * Follows the same pattern as NotesStorageService.
 */
class RoutineDataStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val routinesMutex = Mutex()
    private val completionsMutex = Mutex()
    private val ffRoutineMutex = Mutex()
    private val ffCompletionsMutex = Mutex()

    private val routinesFile: File
        get() = File(context.filesDir, "routines/weekly_routines.json").also { it.parentFile?.mkdirs() }
    private val completionsFile: File
        get() = File(context.filesDir, "routines/completions.json").also { it.parentFile?.mkdirs() }
    private val ffRoutineFile: File
        get() = File(context.filesDir, "routines/first_friday_routine.json").also { it.parentFile?.mkdirs() }
    private val ffCompletionsFile: File
        get() = File(context.filesDir, "routines/first_friday_completions.json").also { it.parentFile?.mkdirs() }

    // In-memory caches
    private var routinesCache: MutableList<WeeklyRoutineEntity>? = null
    private var completionsCache: MutableList<RoutineCompletionEntity>? = null
    private var ffRoutineCache: FirstFridayRoutineEntity? = null
    private var ffRoutineLoaded = false
    private var ffCompletionsCache: MutableList<FirstFridayCompletionEntity>? = null

    // --- Weekly Routines ---

    suspend fun insertRoutine(routine: WeeklyRoutineEntity) = routinesMutex.withLock {
        val list = loadRoutinesInternal().toMutableList()
        list.removeAll { it.id == routine.id }
        list.add(routine)
        saveRoutinesInternal(list)
    }

    suspend fun updateRoutine(routine: WeeklyRoutineEntity) = routinesMutex.withLock {
        val list = loadRoutinesInternal().toMutableList()
        val idx = list.indexOfFirst { it.id == routine.id }
        if (idx >= 0) list[idx] = routine
        saveRoutinesInternal(list)
    }

    suspend fun deleteRoutine(routine: WeeklyRoutineEntity) = routinesMutex.withLock {
        val list = loadRoutinesInternal().toMutableList()
        list.removeAll { it.id == routine.id }
        saveRoutinesInternal(list)
        // Also delete completions
        completionsMutex.withLock {
            val comps = loadCompletionsInternal().toMutableList()
            comps.removeAll { it.routineId == routine.id }
            saveCompletionsInternal(comps)
        }
    }

    suspend fun allRoutines(): List<WeeklyRoutineEntity> = routinesMutex.withLock {
        loadRoutinesInternal().sortedBy { it.sortOrder }
    }

    suspend fun activeRoutines(): List<WeeklyRoutineEntity> = routinesMutex.withLock {
        loadRoutinesInternal().filter { it.isActive }.sortedBy { it.sortOrder }
    }

    suspend fun pausedRoutines(): List<WeeklyRoutineEntity> = routinesMutex.withLock {
        loadRoutinesInternal().filter { !it.isActive }.sortedBy { it.sortOrder }
    }

    suspend fun routineById(id: String): WeeklyRoutineEntity? = routinesMutex.withLock {
        loadRoutinesInternal().find { it.id == id }
    }

    suspend fun maxSortOrder(): Int? = routinesMutex.withLock {
        loadRoutinesInternal().maxOfOrNull { it.sortOrder }
    }

    // --- Routine Completions ---

    suspend fun insertCompletion(completion: RoutineCompletionEntity) = completionsMutex.withLock {
        val list = loadCompletionsInternal().toMutableList()
        list.removeAll { it.routineId == completion.routineId && it.dateLong == completion.dateLong }
        list.add(completion)
        saveCompletionsInternal(list)
    }

    suspend fun deleteCompletion(routineId: String, dateLong: Long) = completionsMutex.withLock {
        val list = loadCompletionsInternal().toMutableList()
        list.removeAll { it.routineId == routineId && it.dateLong == dateLong }
        saveCompletionsInternal(list)
    }

    suspend fun completionsForRoutine(routineId: String): List<RoutineCompletionEntity> = completionsMutex.withLock {
        loadCompletionsInternal().filter { it.routineId == routineId }.sortedByDescending { it.dateLong }
    }

    suspend fun completionForDate(routineId: String, dateLong: Long): RoutineCompletionEntity? = completionsMutex.withLock {
        loadCompletionsInternal().find { it.routineId == routineId && it.dateLong == dateLong }
    }

    suspend fun deleteOldCompletions(beforeTimestamp: Long) = completionsMutex.withLock {
        val list = loadCompletionsInternal().toMutableList()
        val before = list.size
        list.removeAll { it.completedAt < beforeTimestamp }
        if (list.size != before) saveCompletionsInternal(list)
    }

    // --- First Friday Routine ---

    suspend fun insertFirstFridayRoutine(routine: FirstFridayRoutineEntity) = ffRoutineMutex.withLock {
        ffRoutineCache = routine
        ffRoutineLoaded = true
        withContext(Dispatchers.IO) {
            try {
                ffRoutineFile.parentFile?.mkdirs()
                ffRoutineFile.writeText(json.encodeToString(routine))
            } catch (_: Exception) {}
        }
    }

    suspend fun updateFirstFridayRoutine(routine: FirstFridayRoutineEntity) =
        insertFirstFridayRoutine(routine)

    suspend fun deleteFirstFridayRoutine(routine: FirstFridayRoutineEntity) = ffRoutineMutex.withLock {
        ffRoutineCache = null
        ffRoutineLoaded = true
        withContext(Dispatchers.IO) {
            try { ffRoutineFile.delete() } catch (_: Exception) {}
        }
        // Also delete completions
        ffCompletionsMutex.withLock {
            val list = loadFFCompletionsInternal().toMutableList()
            list.removeAll { it.routineId == routine.id }
            saveFFCompletionsInternal(list)
        }
    }

    suspend fun getFirstFridayRoutine(): FirstFridayRoutineEntity? = ffRoutineMutex.withLock {
        loadFFRoutineInternal()
    }

    // --- First Friday Completions ---

    suspend fun insertFirstFridayCompletion(completion: FirstFridayCompletionEntity) = ffCompletionsMutex.withLock {
        val list = loadFFCompletionsInternal().toMutableList()
        list.removeAll { it.routineId == completion.routineId && it.dateLong == completion.dateLong }
        list.add(completion)
        saveFFCompletionsInternal(list)
    }

    suspend fun deleteFirstFridayCompletion(routineId: String, dateLong: Long) = ffCompletionsMutex.withLock {
        val list = loadFFCompletionsInternal().toMutableList()
        list.removeAll { it.routineId == routineId && it.dateLong == dateLong }
        saveFFCompletionsInternal(list)
    }

    suspend fun firstFridayCompletions(routineId: String): List<FirstFridayCompletionEntity> = ffCompletionsMutex.withLock {
        loadFFCompletionsInternal().filter { it.routineId == routineId }.sortedBy { it.dateLong }
    }

    suspend fun firstFridayCompletionForDate(routineId: String, dateLong: Long): FirstFridayCompletionEntity? = ffCompletionsMutex.withLock {
        loadFFCompletionsInternal().find { it.routineId == routineId && it.dateLong == dateLong }
    }

    // --- Internal IO ---

    private suspend fun loadRoutinesInternal(): List<WeeklyRoutineEntity> {
        routinesCache?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                if (routinesFile.exists()) {
                    val text = routinesFile.readText()
                    if (text.isNotBlank()) {
                        json.decodeFromString<List<WeeklyRoutineEntity>>(text)
                            .also { routinesCache = it.toMutableList() }
                    } else emptyList<WeeklyRoutineEntity>().also { routinesCache = mutableListOf() }
                } else emptyList<WeeklyRoutineEntity>().also { routinesCache = mutableListOf() }
            } catch (_: Exception) {
                emptyList<WeeklyRoutineEntity>().also { routinesCache = mutableListOf() }
            }
        }
    }

    private suspend fun saveRoutinesInternal(list: List<WeeklyRoutineEntity>) {
        routinesCache = list.toMutableList()
        withContext(Dispatchers.IO) {
            try {
                routinesFile.parentFile?.mkdirs()
                routinesFile.writeText(json.encodeToString(list))
            } catch (_: Exception) {}
        }
    }

    private suspend fun loadCompletionsInternal(): List<RoutineCompletionEntity> {
        completionsCache?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                if (completionsFile.exists()) {
                    val text = completionsFile.readText()
                    if (text.isNotBlank()) {
                        json.decodeFromString<List<RoutineCompletionEntity>>(text)
                            .also { completionsCache = it.toMutableList() }
                    } else emptyList<RoutineCompletionEntity>().also { completionsCache = mutableListOf() }
                } else emptyList<RoutineCompletionEntity>().also { completionsCache = mutableListOf() }
            } catch (_: Exception) {
                emptyList<RoutineCompletionEntity>().also { completionsCache = mutableListOf() }
            }
        }
    }

    private suspend fun saveCompletionsInternal(list: List<RoutineCompletionEntity>) {
        completionsCache = list.toMutableList()
        withContext(Dispatchers.IO) {
            try {
                completionsFile.parentFile?.mkdirs()
                completionsFile.writeText(json.encodeToString(list))
            } catch (_: Exception) {}
        }
    }

    private suspend fun loadFFRoutineInternal(): FirstFridayRoutineEntity? {
        if (ffRoutineLoaded) return ffRoutineCache
        return withContext(Dispatchers.IO) {
            try {
                if (ffRoutineFile.exists()) {
                    val text = ffRoutineFile.readText()
                    if (text.isNotBlank()) {
                        json.decodeFromString<FirstFridayRoutineEntity>(text)
                            .also { ffRoutineCache = it; ffRoutineLoaded = true }
                    } else {
                        ffRoutineLoaded = true
                        null
                    }
                } else {
                    ffRoutineLoaded = true
                    null
                }
            } catch (_: Exception) {
                ffRoutineLoaded = true
                null
            }
        }
    }

    private suspend fun loadFFCompletionsInternal(): List<FirstFridayCompletionEntity> {
        ffCompletionsCache?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                if (ffCompletionsFile.exists()) {
                    val text = ffCompletionsFile.readText()
                    if (text.isNotBlank()) {
                        json.decodeFromString<List<FirstFridayCompletionEntity>>(text)
                            .also { ffCompletionsCache = it.toMutableList() }
                    } else emptyList<FirstFridayCompletionEntity>().also { ffCompletionsCache = mutableListOf() }
                } else emptyList<FirstFridayCompletionEntity>().also { ffCompletionsCache = mutableListOf() }
            } catch (_: Exception) {
                emptyList<FirstFridayCompletionEntity>().also { ffCompletionsCache = mutableListOf() }
            }
        }
    }

    private suspend fun saveFFCompletionsInternal(list: List<FirstFridayCompletionEntity>) {
        ffCompletionsCache = list.toMutableList()
        withContext(Dispatchers.IO) {
            try {
                ffCompletionsFile.parentFile?.mkdirs()
                ffCompletionsFile.writeText(json.encodeToString(list))
            } catch (_: Exception) {}
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: RoutineDataStore? = null

        fun getInstance(context: Context): RoutineDataStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RoutineDataStore(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
