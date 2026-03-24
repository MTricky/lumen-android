package com.app.lumen.features.calendar.service

import android.content.Context
import com.app.lumen.features.calendar.model.Note
import com.app.lumen.features.calendar.model.Reminder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Calendar
import java.util.Date

class NotesStorageService(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val notesMutex = Mutex()
    private val remindersMutex = Mutex()

    private val notesFile: File
        get() = File(context.filesDir, "notes/notes.json").also { it.parentFile?.mkdirs() }

    private val remindersFile: File
        get() = File(context.filesDir, "reminders/reminders.json").also { it.parentFile?.mkdirs() }

    // In-memory caches
    private var notesCache: MutableList<Note>? = null
    private var remindersCache: MutableList<Reminder>? = null

    // MARK: - Notes CRUD

    suspend fun createNote(
        date: Date,
        type: com.app.lumen.features.calendar.model.NoteType,
        title: String,
        content: String? = null
    ): Note = notesMutex.withLock {
        val note = Note(
            dateLong = startOfDay(date).time,
            typeRaw = type.rawValue,
            title = title,
            content = content
        )
        val notes = loadNotesInternal().toMutableList()
        notes.add(note)
        saveNotesInternal(notes)
        note
    }

    suspend fun updateNote(id: String, title: String, content: String?): Boolean = notesMutex.withLock {
        val notes = loadNotesInternal().toMutableList()
        val index = notes.indexOfFirst { it.id == id }
        if (index == -1) return false
        notes[index] = notes[index].copy(
            title = title,
            content = content,
            modifiedAt = System.currentTimeMillis()
        )
        saveNotesInternal(notes)
        true
    }

    suspend fun deleteNote(id: String) = notesMutex.withLock {
        val notes = loadNotesInternal().toMutableList()
        notes.removeAll { it.id == id }
        saveNotesInternal(notes)
    }

    suspend fun notesForDate(date: Date): List<Note> = notesMutex.withLock {
        val start = startOfDay(date).time
        loadNotesInternal()
            .filter { it.dateLong == start }
            .sortedByDescending { it.createdAt }
    }

    suspend fun allNotes(): List<Note> = notesMutex.withLock {
        val endOfToday = endOfDay(Date()).time
        loadNotesInternal()
            .filter { it.dateLong <= endOfToday }
            .sortedWith(compareByDescending<Note> { it.dateLong }.thenByDescending { it.createdAt })
    }

    suspend fun searchNotes(query: String): List<Note> {
        if (query.isBlank()) return allNotes()
        val lowerQuery = query.lowercase()
        return allNotes().filter { note ->
            note.title.lowercase().contains(lowerQuery) ||
                    (note.content?.lowercase()?.contains(lowerQuery) == true)
        }
    }

    suspend fun hasNotes(date: Date): Boolean = notesForDate(date).isNotEmpty()

    suspend fun datesWithNotes(from: Date, to: Date): Set<Long> = notesMutex.withLock {
        val start = startOfDay(from).time
        val end = startOfDay(to).time
        loadNotesInternal()
            .filter { it.dateLong in start..end }
            .map { it.dateLong }
            .toSet()
    }

    // MARK: - Reminders CRUD

    suspend fun createReminder(
        date: Date,
        type: com.app.lumen.features.calendar.model.ReminderType,
        title: String,
        message: String? = null,
        triggerTime: Date,
        notes: String? = null
    ): Reminder = remindersMutex.withLock {
        val reminder = Reminder(
            dateLong = startOfDay(date).time,
            triggerTimeLong = triggerTime.time,
            typeRaw = type.rawValue,
            title = title,
            message = message,
            notes = notes
        )
        val reminders = loadRemindersInternal().toMutableList()
        reminders.add(reminder)
        saveRemindersInternal(reminders)
        reminder
    }

    suspend fun updateReminder(
        id: String,
        title: String,
        message: String?,
        triggerTime: Date,
        notes: String?
    ): Reminder? = remindersMutex.withLock {
        val reminders = loadRemindersInternal().toMutableList()
        val index = reminders.indexOfFirst { it.id == id }
        if (index == -1) return null
        val updated = reminders[index].copy(
            title = title,
            message = message,
            triggerTimeLong = triggerTime.time,
            notes = notes,
            modifiedAt = System.currentTimeMillis()
        )
        reminders[index] = updated
        saveRemindersInternal(reminders)
        updated
    }

    suspend fun deleteReminder(id: String): Reminder? = remindersMutex.withLock {
        val reminders = loadRemindersInternal().toMutableList()
        val reminder = reminders.find { it.id == id }
        reminders.removeAll { it.id == id }
        saveRemindersInternal(reminders)
        reminder
    }

    suspend fun remindersForDate(date: Date): List<Reminder> = remindersMutex.withLock {
        val start = startOfDay(date).time
        loadRemindersInternal()
            .filter { it.dateLong == start && it.isActive }
            .sortedBy { it.triggerTimeLong }
    }

    suspend fun upcomingReminders(): List<Reminder> = remindersMutex.withLock {
        val todayStart = startOfDay(Date()).time
        loadRemindersInternal()
            .filter { it.dateLong >= todayStart && it.isActive }
            .sortedWith(compareBy<Reminder> { it.dateLong }.thenBy { it.triggerTimeLong })
    }

    suspend fun reminderById(id: String): Reminder? = remindersMutex.withLock {
        loadRemindersInternal().find { it.id == id }
    }

    suspend fun hasReminders(date: Date): Boolean = remindersForDate(date).isNotEmpty()

    suspend fun datesWithReminders(from: Date, to: Date): Set<Long> = remindersMutex.withLock {
        val start = startOfDay(from).time
        val end = startOfDay(to).time
        loadRemindersInternal()
            .filter { it.dateLong in start..end && it.isActive }
            .map { it.dateLong }
            .toSet()
    }

    suspend fun cleanupPastReminders() = remindersMutex.withLock {
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val reminders = loadRemindersInternal().toMutableList()
        val before = reminders.size
        reminders.removeAll { it.dateLong < yesterday }
        if (reminders.size != before) {
            saveRemindersInternal(reminders)
        }
    }

    // MARK: - Internal IO

    private suspend fun loadNotesInternal(): List<Note> {
        notesCache?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                if (notesFile.exists()) {
                    val text = notesFile.readText()
                    if (text.isNotBlank()) {
                        json.decodeFromString<List<Note>>(text).also { notesCache = it.toMutableList() }
                    } else emptyList<Note>().also { notesCache = mutableListOf() }
                } else emptyList<Note>().also { notesCache = mutableListOf() }
            } catch (e: Exception) {
                emptyList<Note>().also { notesCache = mutableListOf() }
            }
        }
    }

    private suspend fun saveNotesInternal(notes: List<Note>) {
        notesCache = notes.toMutableList()
        withContext(Dispatchers.IO) {
            try {
                notesFile.parentFile?.mkdirs()
                notesFile.writeText(json.encodeToString(notes))
            } catch (e: Exception) {
                // Silently handle
            }
        }
    }

    private suspend fun loadRemindersInternal(): List<Reminder> {
        remindersCache?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                if (remindersFile.exists()) {
                    val text = remindersFile.readText()
                    if (text.isNotBlank()) {
                        json.decodeFromString<List<Reminder>>(text).also { remindersCache = it.toMutableList() }
                    } else emptyList<Reminder>().also { remindersCache = mutableListOf() }
                } else emptyList<Reminder>().also { remindersCache = mutableListOf() }
            } catch (e: Exception) {
                emptyList<Reminder>().also { remindersCache = mutableListOf() }
            }
        }
    }

    private suspend fun saveRemindersInternal(reminders: List<Reminder>) {
        remindersCache = reminders.toMutableList()
        withContext(Dispatchers.IO) {
            try {
                remindersFile.parentFile?.mkdirs()
                remindersFile.writeText(json.encodeToString(reminders))
            } catch (e: Exception) {
                // Silently handle
            }
        }
    }

    // MARK: - Date Helpers

    private fun startOfDay(date: Date): Date {
        val cal = Calendar.getInstance()
        cal.time = date
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    private fun endOfDay(date: Date): Date {
        val cal = Calendar.getInstance()
        cal.time = date
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.time
    }
}
