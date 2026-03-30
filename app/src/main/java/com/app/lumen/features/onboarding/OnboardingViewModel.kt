package com.app.lumen.features.onboarding

import android.app.Application
import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.lumen.R
import com.app.lumen.features.bible.model.BibleLanguageOption
import com.app.lumen.features.bible.model.BibleTradition
import com.app.lumen.features.bible.model.BibleVersion
import com.app.lumen.features.bible.service.BibleRoutingService
import com.app.lumen.features.calendar.model.LiturgicalRegion
import com.app.lumen.features.survey.viewmodel.SurveyViewModel
import com.app.lumen.features.calendar.model.RoutineItemType
import com.app.lumen.features.calendar.data.RoutineDataStore
import com.app.lumen.features.calendar.data.WeeklyRoutineEntity
import com.app.lumen.features.calendar.data.FirstFridayRoutineEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import com.app.lumen.features.calendar.service.LumenNotificationManager
import com.app.lumen.features.liturgy.viewmodel.LiturgyViewModel
import com.app.lumen.features.rosary.service.RosaryAudioService
import com.app.lumen.services.AnalyticsEvent
import com.app.lumen.services.AnalyticsManager
import com.app.lumen.widget.VerseWidgetData
import com.app.lumen.widget.VerseWidgetWorker
import android.graphics.BitmapFactory
import coil.imageLoader
import coil.request.ImageRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.URL
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class OnboardingStep(val index: Int, val backgroundRes: Int) {
    WELCOME(0, R.drawable.onboarding_1),
    FEATURES(1, R.drawable.onboarding_2),
    REGION(2, R.drawable.onboarding_3),
    NOTIFICATIONS(3, R.drawable.onboarding_4),
    BIBLE(4, R.drawable.onboarding_5),
    ROSARY(5, R.drawable.onboarding_rosary),
    ROUTINE_INTRO(6, R.drawable.onboarding_6),
    ROUTINE_SETUP(7, R.drawable.onboarding_6);

    companion object {
        fun fromIndex(index: Int): OnboardingStep = entries.first { it.index == index }
    }
}

enum class OnboardingPhase {
    STEPS,
    LOADING,
    WIDGETS,
    COMPLETION
}

data class OnboardingRoutineSelection(
    val id: String = UUID.randomUUID().toString(),
    var title: String,
    val type: RoutineItemType,
    var selectedDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),
    var selectedHour: Int = 9,
    var selectedMinute: Int = 0,
    var isNotificationEnabled: Boolean = true,
    var notificationLeadTimeMinutes: Int = 0,
    val isCustom: Boolean = false
)

data class RoutineSuggestion(
    val id: String,
    @androidx.annotation.StringRes val titleRes: Int,
    val type: RoutineItemType,
    val defaultDays: List<Int>,
    val defaultHour: Int,
    val defaultMinute: Int
) {
    companion object {
        // Matches iOS order exactly: Mass, Rosary, Morning Prayer, Evening Prayer, Divine Mercy, Adoration
        val all = listOf(
            RoutineSuggestion("mass", R.string.routine_type_mass, RoutineItemType.MASS, listOf(1), 9, 0),
            RoutineSuggestion("rosary", R.string.routine_type_rosary, RoutineItemType.ROSARY, (1..7).toList(), 18, 0),
            RoutineSuggestion("morning", R.string.routine_default_morning_offering, RoutineItemType.MORNING_PRAYER, (1..7).toList(), 7, 0),
            RoutineSuggestion("evening", R.string.routine_type_evening_prayer, RoutineItemType.EVENING_PRAYER, (1..7).toList(), 21, 0),
            RoutineSuggestion("divineMercy", R.string.routine_type_divine_mercy, RoutineItemType.DIVINE_MERCY, (1..7).toList(), 15, 0),
            RoutineSuggestion("adoration", R.string.routine_type_adoration, RoutineItemType.ADORATION, listOf(5), 18, 0),
        )
        val angelus = RoutineSuggestion("angelus", R.string.routine_type_angelus, RoutineItemType.ANGELUS, (1..7).toList(), 12, 0)
    }
}

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    // Navigation
    var currentStep by mutableStateOf(OnboardingStep.WELCOME)
        private set
    var currentPhase by mutableStateOf(OnboardingPhase.STEPS)
        private set

    // Region
    var selectedRegion by mutableStateOf(detectRegion())
        private set

    // Notifications
    var notificationsAuthorized by mutableStateOf(false)
    var isRequestingNotifications by mutableStateOf(false)
        private set

    // Bible
    var selectedBible by mutableStateOf<BibleVersion?>(null)
        private set
    var availableBibles by mutableStateOf<List<BibleVersion>>(emptyList())
        private set
    var isBibleLoading by mutableStateOf(false)
        private set
    var selectedBibleLanguage by mutableStateOf(detectBibleLanguage())
        private set

    // Rosary
    var selectedVisualMode by mutableStateOf(RosaryVisualMode.SACRED_ART)
        private set
    var isRosaryAudioEnabled by mutableStateOf(true)
        private set
    var audioDownloadProgress by mutableStateOf(0.0)
        private set
    var isAudioDownloading by mutableStateOf(false)
        private set
    var isAudioDownloaded by mutableStateOf(false)
        private set

    // Routines
    val selectedRoutines = mutableStateListOf<OnboardingRoutineSelection>()
    var routineBeingEdited by mutableStateOf<OnboardingRoutineSelection?>(null)
        private set
    var isFirstFridaySelected by mutableStateOf(false)
        private set
    var isFirstFridayEditSheetShown by mutableStateOf(false)
        private set
    var firstFridayInitialCount by mutableIntStateOf(0)

    // Loading
    var loadingCurrentStep by mutableIntStateOf(0)
        private set
    var isLoadingComplete by mutableStateOf(false)
        private set
    var loadingButtonProgress by mutableFloatStateOf(0f)
        private set

    // Completion
    var isCompleting by mutableStateOf(false)
        private set

    private val context: Context get() = getApplication()

    private val audioService = RosaryAudioService.getInstance(application)

    init {
        loadBibleVersions()
        // Check if audio already downloaded and start download if enabled
        isAudioDownloaded = audioService.isAudioDownloaded(audioLanguageCode)
        if (isRosaryAudioEnabled && !isAudioDownloaded) {
            viewModelScope.launch { try { audioService.downloadAudio(audioLanguageCode) } catch (_: Exception) { } }
        }
        // Collect download progress
        viewModelScope.launch {
            audioService.downloadProgress.collectLatest { progress ->
                audioDownloadProgress = progress
            }
        }
        viewModelScope.launch {
            audioService.isDownloading.collectLatest { downloading ->
                isAudioDownloading = downloading
                if (!downloading && audioDownloadProgress >= 1.0) {
                    isAudioDownloaded = true
                }
            }
        }
    }

    val audioLanguageCode: String
        get() = when (selectedBibleLanguage) {
            BibleLanguageOption.ENGLISH -> "en"
            BibleLanguageOption.SPANISH -> "es"
            BibleLanguageOption.POLISH -> "pl"
            BibleLanguageOption.PORTUGUESE -> "pt"
            BibleLanguageOption.FRENCH -> "fr"
            BibleLanguageOption.ITALIAN -> "it"
            BibleLanguageOption.GERMAN -> "de"
        }

    // MARK: - Navigation

    fun goToNextStep() {
        when (currentStep) {
            OnboardingStep.WELCOME -> currentStep = OnboardingStep.FEATURES
            OnboardingStep.FEATURES -> currentStep = OnboardingStep.REGION
            OnboardingStep.REGION -> currentStep = OnboardingStep.NOTIFICATIONS
            OnboardingStep.NOTIFICATIONS -> currentStep = OnboardingStep.BIBLE
            OnboardingStep.BIBLE -> currentStep = OnboardingStep.ROSARY
            OnboardingStep.ROSARY -> {
                saveRosaryPreferences()
                // Track rosary onboarding completed (matching iOS)
                val props = mutableMapOf<String, Any>(
                    "visual_mode" to selectedVisualMode.name.lowercase()
                )
                if (isRosaryAudioEnabled || isAudioDownloaded) {
                    props["audio_enabled"] = if (isRosaryAudioEnabled) "true" else "false"
                }
                AnalyticsManager.trackEvent(AnalyticsEvent.ROSARY_ONBOARDING_COMPLETED, props)
                currentStep = OnboardingStep.ROUTINE_INTRO
            }
            OnboardingStep.ROUTINE_INTRO -> currentStep = OnboardingStep.ROUTINE_SETUP
            OnboardingStep.ROUTINE_SETUP -> currentPhase = OnboardingPhase.LOADING
        }
    }

    fun goToPreviousStep() {
        when (currentStep) {
            OnboardingStep.WELCOME -> {}
            OnboardingStep.FEATURES -> currentStep = OnboardingStep.WELCOME
            OnboardingStep.REGION -> currentStep = OnboardingStep.FEATURES
            OnboardingStep.NOTIFICATIONS -> currentStep = OnboardingStep.REGION
            OnboardingStep.BIBLE -> currentStep = OnboardingStep.NOTIFICATIONS
            OnboardingStep.ROSARY -> currentStep = OnboardingStep.BIBLE
            OnboardingStep.ROUTINE_INTRO -> currentStep = OnboardingStep.ROSARY
            OnboardingStep.ROUTINE_SETUP -> currentStep = OnboardingStep.ROUTINE_INTRO
        }
    }

    fun goToWidgetsPhase() {
        currentPhase = OnboardingPhase.WIDGETS
    }

    fun goToCompletionPhase() {
        currentPhase = OnboardingPhase.COMPLETION
    }

    // MARK: - Region

    fun setRegion(region: LiturgicalRegion) {
        selectedRegion = region
        val prefs = context.getSharedPreferences("calendar_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("region", region.displayName).apply()
    }

    // MARK: - Notifications

    fun onNotificationPermissionResult(granted: Boolean) {
        notificationsAuthorized = granted
        isRequestingNotifications = false
        goToNextStep()
    }

    fun requestNotifications() {
        isRequestingNotifications = true
    }

    // MARK: - Bible

    private fun loadBibleVersions() {
        viewModelScope.launch {
            isBibleLoading = true
            try {
                val service = BibleRoutingService(context)
                var versions = service.fetchBibles(selectedBibleLanguage.code)

                if (versions.isEmpty() && selectedBibleLanguage != BibleLanguageOption.ENGLISH) {
                    selectedBibleLanguage = BibleLanguageOption.ENGLISH
                    versions = service.fetchBibles(BibleLanguageOption.ENGLISH.code)
                }

                availableBibles = deduplicateBibles(versions)

                if (selectedBible == null) {
                    selectedBible = selectBestBible(availableBibles)
                    selectedBible?.let {
                        val prefs = context.getSharedPreferences("bible_settings", Context.MODE_PRIVATE)
                        prefs.edit().putString("selected_bible_id", it.id).apply()
                    }
                }
            } catch (e: Exception) {
                // Silent fail
            }
            isBibleLoading = false
        }
    }

    fun selectBible(bible: BibleVersion) {
        selectedBible = bible
        val prefs = context.getSharedPreferences("bible_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("selected_bible_id", bible.id).apply()
    }

    fun changeBibleLanguage(language: BibleLanguageOption) {
        if (language == selectedBibleLanguage) return
        selectedBibleLanguage = language
        availableBibles = emptyList()
        selectedBible = null
        val prefs = context.getSharedPreferences("bible_settings", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("selected_language", language.code)
            .remove("selected_bible_id")
            .apply()
        loadBibleVersions()
    }

    private fun deduplicateBibles(versions: List<BibleVersion>): List<BibleVersion> {
        return versions.groupBy { it.dblId ?: it.id }.map { (_, group) ->
            if (group.size == 1) group[0]
            else group.firstOrNull { it.tradition == BibleTradition.CATHOLIC }
                ?: group.firstOrNull { it.tradition == BibleTradition.ECUMENICAL }
                ?: group.firstOrNull { it.tradition == BibleTradition.ORTHODOX }
                ?: group.firstOrNull { it.tradition == BibleTradition.PROTESTANT }
                ?: group[0]
        }
    }

    private fun selectBestBible(versions: List<BibleVersion>): BibleVersion? {
        val preferredIds = when (selectedBibleLanguage) {
            BibleLanguageOption.ENGLISH -> listOf("WEBC", "72f4e6dc683324df-03")
            BibleLanguageOption.SPANISH -> listOf("LPD", "48acedcf8595c754-01")
            BibleLanguageOption.POLISH -> listOf("BT5", "1c9761e0230da6e0-01")
            BibleLanguageOption.PORTUGUESE -> listOf("MS", "941380703fcb500c-01")
            BibleLanguageOption.FRENCH -> listOf("AELF", "a93a92589195411f-01")
            BibleLanguageOption.ITALIAN -> listOf("CEI2008", "41f25b97f468e10b-01")
            BibleLanguageOption.GERMAN -> listOf("EU")
        }

        for (id in preferredIds) {
            versions.firstOrNull { it.id == id }?.let { return it }
        }

        return versions.firstOrNull()
    }

    // MARK: - Rosary

    fun selectVisualMode(mode: RosaryVisualMode) {
        selectedVisualMode = mode
    }

    fun toggleRosaryAudio() {
        isRosaryAudioEnabled = !isRosaryAudioEnabled
        if (isRosaryAudioEnabled) {
            startAudioDownload()
        } else {
            cancelAudioDownload()
        }
    }

    private fun startAudioDownload() {
        if (isAudioDownloaded) return
        viewModelScope.launch {
            try {
                audioService.downloadAudio(audioLanguageCode)
            } catch (_: Exception) { }
        }
    }

    private fun cancelAudioDownload() {
        audioService.cancelDownload()
    }

    private fun saveRosaryPreferences() {
        val prefs = context.getSharedPreferences("rosary_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("visual_style", selectedVisualMode.name)
            .putBoolean("audio_enabled", isRosaryAudioEnabled)
            .apply()
    }

    // MARK: - Routines

    fun toggleRoutineSelection(suggestion: RoutineSuggestion) {
        val existing = selectedRoutines.indexOfFirst { it.type == suggestion.type && !it.isCustom }
        if (existing >= 0) {
            selectedRoutines.removeAt(existing)
        } else {
            selectedRoutines.add(
                OnboardingRoutineSelection(
                    title = getApplication<Application>().getString(suggestion.titleRes),
                    type = suggestion.type,
                    selectedDays = suggestion.defaultDays.toSet(),
                    selectedHour = suggestion.defaultHour,
                    selectedMinute = suggestion.defaultMinute
                )
            )
        }
    }

    fun isRoutineSelected(suggestion: RoutineSuggestion): Boolean {
        return selectedRoutines.any { it.type == suggestion.type && !it.isCustom }
    }

    fun toggleFirstFridaySelection() {
        isFirstFridaySelected = !isFirstFridaySelected
        if (isFirstFridaySelected) {
            isFirstFridayEditSheetShown = true
        } else {
            firstFridayInitialCount = 0
        }
    }

    fun showFirstFridayEditSheet() {
        isFirstFridayEditSheetShown = true
    }

    fun dismissFirstFridayEditSheet() {
        isFirstFridayEditSheetShown = false
    }

    fun addCustomRoutine(title: String) {
        val selection = OnboardingRoutineSelection(
            title = title,
            type = RoutineItemType.CUSTOM,
            isCustom = true
        )
        selectedRoutines.add(selection)
        routineBeingEdited = selection
    }

    fun removeRoutine(selection: OnboardingRoutineSelection) {
        selectedRoutines.removeAll { it.id == selection.id }
    }

    fun editRoutine(selection: OnboardingRoutineSelection) {
        routineBeingEdited = selection
    }

    fun updateRoutine(updated: OnboardingRoutineSelection) {
        val index = selectedRoutines.indexOfFirst { it.id == updated.id }
        if (index >= 0) {
            selectedRoutines[index] = updated
        }
        routineBeingEdited = null
    }

    fun cancelRoutineEdit() {
        routineBeingEdited = null
    }

    val hasSelectedRoutines: Boolean
        get() = selectedRoutines.isNotEmpty() || isFirstFridaySelected

    val totalRoutineCount: Int
        get() = selectedRoutines.size + if (isFirstFridaySelected) 1 else 0

    // MARK: - Loading

    fun startLoadingSequence() {
        // Prefetch liturgy content in background so it's ready when onboarding finishes
        viewModelScope.launch {
            try {
                LiturgyViewModel(getApplication())
            } catch (_: Exception) { }
        }

        // Prefetch widget verse data + background image from Firebase
        VerseWidgetWorker.enqueuePeriodicWork(context)

        // Preload today's liturgy header image into Coil's singleton cache
        viewModelScope.launch {
            try {
                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val firestore = FirebaseFirestore.getInstance()
                val liturgyDoc = firestore.collection("dailyLiturgy")
                    .document(todayDate).get().await()
                val imageUrl = liturgyDoc.data?.get("imageUrl") as? String
                if (imageUrl != null) {
                    val request = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .build()
                    context.imageLoader.execute(request)
                }
            } catch (_: Exception) { }
        }

        // Fetch yesterday's verse + background image for the large widget preview
        viewModelScope.launch {
            try {
                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                val yesterdayDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
                val firestore = FirebaseFirestore.getInstance()

                // Fetch yesterday's image URL from dailyLiturgy
                var yesterdayImageUrl: String? = null
                try {
                    val liturgyDoc = firestore.collection("dailyLiturgy")
                        .document(yesterdayDate).get().await()
                    yesterdayImageUrl = liturgyDoc.data?.get("imageUrl") as? String
                } catch (_: Exception) { }

                // Fetch yesterday's verse
                val verseDoc = firestore.collection("dailyVerse")
                    .document(yesterdayDate).get().await()
                if (verseDoc.exists()) {
                    val data = verseDoc.data ?: return@launch
                    val lang = Locale.getDefault().language.let {
                        if (it in listOf("es", "pt", "fr", "it", "de", "pl")) it else "en"
                    }
                    val category = data["category"] as? String ?: return@launch
                    @Suppress("UNCHECKED_CAST")
                    val versesData = data["verses"] as? Map<String, Any> ?: return@launch
                    @Suppress("UNCHECKED_CAST")
                    val verseData = versesData[lang] as? Map<String, Any>
                        ?: versesData["en"] as? Map<String, Any> ?: return@launch
                    val text = verseData["text"] as? String ?: return@launch
                    val mediumText = verseData["mediumText"] as? String ?: text.take(120) + "..."
                    val shortText = verseData["shortText"] as? String ?: text.take(80) + "..."
                    val reference = verseData["reference"] as? String ?: ""
                    val shortReference = verseData["shortReference"] as? String ?: reference
                    VerseWidgetData.saveYesterday(context, VerseWidgetData(
                        date = yesterdayDate,
                        text = text,
                        mediumText = mediumText,
                        shortText = shortText,
                        reference = reference,
                        shortReference = shortReference,
                        category = category,
                        imageUrl = yesterdayImageUrl,
                    ))

                    // Download yesterday's background image
                    if (yesterdayImageUrl != null) {
                        withContext(Dispatchers.IO) {
                            try {
                                val connection = URL(yesterdayImageUrl).openConnection()
                                connection.connectTimeout = 10_000
                                connection.readTimeout = 10_000
                                connection.inputStream.use { stream ->
                                    val bitmap = BitmapFactory.decodeStream(stream)
                                    if (bitmap != null) {
                                        VerseWidgetData.saveYesterdayBackgroundImage(context, bitmap)
                                    }
                                }
                            } catch (_: Exception) { }
                        }
                    }
                }
            } catch (_: Exception) { }
        }

        viewModelScope.launch {
            val stepDuration = 3000L
            val totalSteps = 4
            val totalDuration = stepDuration * totalSteps // 12000ms
            val updateInterval = 50L
            val totalTicks = totalDuration / updateInterval
            var tick = 0L

            for (step in 0 until totalSteps) {
                loadingCurrentStep = step
                val stepTicks = stepDuration / updateInterval
                for (t in 1..stepTicks) {
                    delay(updateInterval)
                    tick++
                    loadingButtonProgress = tick.toFloat() / totalTicks.toFloat()
                }
            }

            // Ensure progress is exactly 1.0 before marking complete
            loadingButtonProgress = 1f
            isLoadingComplete = true
        }
    }

    // MARK: - Completion

    fun completeOnboarding() {
        isCompleting = true
        viewModelScope.launch {
            // Create routines
            val routineStore = RoutineDataStore.getInstance(context)
            for (selection in selectedRoutines) {
                val entity = WeeklyRoutineEntity(
                    id = UUID.randomUUID().toString(),
                    title = selection.title,
                    typeRaw = selection.type.name,
                    selectedDaysJson = Json.encodeToString(selection.selectedDays.sorted()),
                    hour = selection.selectedHour,
                    minute = selection.selectedMinute,
                    isNotificationEnabled = selection.isNotificationEnabled && notificationsAuthorized,
                    notificationIdentifiersJson = "[]",
                    isActive = true,
                    isLoggingEnabled = true,
                    notificationLeadTimeMinutes = selection.notificationLeadTimeMinutes,
                    sortOrder = 0,
                    createdAt = System.currentTimeMillis()
                )
                routineStore.insertRoutine(entity)

                // Schedule notifications if enabled
                if (entity.isNotificationEnabled) {
                    scheduleRoutineNotifications(entity)
                }
            }

            // Create First Friday routine if selected
            if (isFirstFridaySelected) {
                val ffEntity = FirstFridayRoutineEntity(
                    id = UUID.randomUUID().toString(),
                    isActive = true,
                    isNotificationEnabled = notificationsAuthorized,
                    notificationHour = 9,
                    notificationMinute = 0,
                    notificationLeadTimeMinutes = 0,
                    notificationIdentifiersJson = "[]",
                    initialConsecutiveCount = firstFridayInitialCount,
                    sortOrder = 0,
                    createdAt = System.currentTimeMillis()
                )
                routineStore.insertFirstFridayRoutine(ffEntity)
            }

            // Track routine_created_onboarding (matching iOS)
            if (selectedRoutines.isNotEmpty()) {
                val routineTypes = selectedRoutines.joinToString(",") { it.type.rawValue }
                AnalyticsManager.trackEvent(
                    AnalyticsEvent.ROUTINE_CREATED_ONBOARDING,
                    mapOf(
                        "routine_types" to routineTypes,
                        "count" to selectedRoutines.size
                    )
                )
            }

            // Track finished onboarding (matching iOS)
            AnalyticsManager.trackEvent(AnalyticsEvent.FINISHED_ONBOARDING)

            // Mark onboarding complete
            OnboardingManager.shared.completeOnboarding()

            // Set survey eligibility date (3 days from now)
            SurveyViewModel.setOnboardingCompletedDateIfNeeded(getApplication())
            isCompleting = false
        }
    }

    private fun scheduleRoutineNotifications(entity: WeeklyRoutineEntity) {
        try {
            val notificationManager = LumenNotificationManager(context)
            val days = entity.selectedDaysJson
                .removeSurrounding("[", "]")
                .split(",")
                .mapNotNull { it.trim().toIntOrNull() }

            for (day in days) {
                val identifier = "${entity.id}_day_$day"
                notificationManager.scheduleWeeklyNotification(
                    identifier = identifier,
                    title = entity.title,
                    body = context.getString(R.string.routine_notification_body, entity.title),
                    weekday = day,
                    hour = entity.hour,
                    minute = entity.minute,
                    leadTimeMinutes = entity.notificationLeadTimeMinutes
                )
            }
        } catch (_: Exception) {
            // Silent fail - notifications are best effort
        }
    }

    // MARK: - Helpers

    companion object {
        private fun detectRegion(): LiturgicalRegion {
            val country = Locale.getDefault().country.uppercase()
            // Primary: match by country code
            return when (country) {
                "US" -> LiturgicalRegion.USA
                "BR" -> LiturgicalRegion.BRAZIL
                "PL" -> LiturgicalRegion.POLAND
                "PT" -> LiturgicalRegion.PORTUGAL
                "IE" -> LiturgicalRegion.IRELAND
                "PH" -> LiturgicalRegion.PHILIPPINES
                "AU" -> LiturgicalRegion.AUSTRALIA
                "GB" -> LiturgicalRegion.UK
                "CA" -> LiturgicalRegion.CANADA
                "ES" -> LiturgicalRegion.SPAIN
                "MX" -> LiturgicalRegion.MEXICO
                "AR" -> LiturgicalRegion.ARGENTINA
                "CL" -> LiturgicalRegion.CHILE
                "CO" -> LiturgicalRegion.COLOMBIA
                "PE" -> LiturgicalRegion.PERU
                "FR" -> LiturgicalRegion.FRANCE
                "DE" -> LiturgicalRegion.GERMANY
                "AT" -> LiturgicalRegion.AUSTRIA
                "IT" -> LiturgicalRegion.ITALY
                else -> {
                    // Fallback: match by language (matching iOS)
                    val lang = Locale.getDefault().language
                    when {
                        lang.startsWith("pl") -> LiturgicalRegion.POLAND
                        lang.startsWith("pt") -> LiturgicalRegion.BRAZIL
                        lang.startsWith("es") -> LiturgicalRegion.SPAIN
                        lang.startsWith("fr") -> LiturgicalRegion.FRANCE
                        lang.startsWith("de") -> LiturgicalRegion.GERMANY
                        lang.startsWith("it") -> LiturgicalRegion.ITALY
                        else -> LiturgicalRegion.UNIVERSAL
                    }
                }
            }
        }

        private fun detectBibleLanguage(): BibleLanguageOption {
            val lang = Locale.getDefault().language
            return when {
                lang.startsWith("pl") -> BibleLanguageOption.POLISH
                lang.startsWith("pt") -> BibleLanguageOption.PORTUGUESE
                lang.startsWith("es") -> BibleLanguageOption.SPANISH
                lang.startsWith("it") -> BibleLanguageOption.ITALIAN
                lang.startsWith("fr") -> BibleLanguageOption.FRENCH
                lang.startsWith("de") -> BibleLanguageOption.GERMAN
                else -> BibleLanguageOption.ENGLISH
            }
        }
    }
}

enum class RosaryVisualMode(@StringRes val displayNameRes: Int) {
    SACRED_ART(R.string.settings_prayer_visual_sacred_art),
    SIMPLE(R.string.settings_prayer_visual_simple)
}
