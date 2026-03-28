package com.app.lumen.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class VerseWidgetWorker(
    private val appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "VerseWidgetWorker"
        private const val WORK_NAME = "verse_widget_update"

        fun enqueuePeriodicWork(context: Context) {
            val request = PeriodicWorkRequestBuilder<VerseWidgetWorker>(
                repeatInterval = 6,
                repeatIntervalTimeUnit = TimeUnit.HOURS,
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting widget data update")

        try {
            // Ensure Firebase is initialized
            if (FirebaseApp.getApps(appContext).isEmpty()) {
                FirebaseApp.initializeApp(appContext)
            }

            val firestore = FirebaseFirestore.getInstance()
            val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

            // Check if we already have today's data with a cached image
            if (VerseWidgetData.isForToday(appContext)) {
                val existing = VerseWidgetData.load(appContext)
                val hasImage = VerseWidgetData.loadBackgroundImage(appContext) != null
                if (existing != null && (existing.imageUrl == null || hasImage)) {
                    Log.d(TAG, "Already have today's data with image, skipping fetch")
                    VerseWidgetData.updateAllWidgets(appContext)
                    return Result.success()
                }
            }

            // Fetch image URL from dailyLiturgy
            var imageUrl: String? = null
            try {
                val liturgyDoc = firestore.collection("dailyLiturgy")
                    .document(dateString).get().await()
                imageUrl = liturgyDoc.data?.get("imageUrl") as? String
                Log.d(TAG, "Got imageUrl: $imageUrl")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get liturgy doc: ${e.message}")
            }

            // Fetch verse data from dailyVerse
            val verseDoc = firestore.collection("dailyVerse")
                .document(dateString).get().await()

            if (!verseDoc.exists()) {
                Log.e(TAG, "No verse document for $dateString")
                return Result.retry()
            }

            val data = verseDoc.data ?: return Result.retry()
            val verseData = parseVerseData(data, dateString, imageUrl)

            if (verseData != null) {
                VerseWidgetData.save(appContext, verseData)
                Log.d(TAG, "Verse data saved: ${verseData.reference}")

                // Download background image
                if (imageUrl != null) {
                    downloadImage(imageUrl)?.let { bitmap ->
                        VerseWidgetData.saveBackgroundImage(appContext, bitmap)
                        Log.d(TAG, "Background image saved")
                    }
                }

                VerseWidgetData.updateAllWidgets(appContext)
                return Result.success()
            }

            return Result.retry()
        } catch (e: Exception) {
            Log.e(TAG, "Worker failed: ${e.message}")
            return Result.retry()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseVerseData(
        data: Map<String, Any>,
        date: String,
        imageUrl: String?,
    ): VerseWidgetData? {
        val langCode = getLanguageCode()
        val category = data["category"] as? String ?: return null
        val versesData = data["verses"] as? Map<String, Any> ?: return null
        val verseData = versesData[langCode] as? Map<String, Any>
            ?: versesData["en"] as? Map<String, Any>
            ?: return null

        val text = verseData["text"] as? String ?: return null
        val mediumText = verseData["mediumText"] as? String ?: text.take(120) + "..."
        val shortText = verseData["shortText"] as? String ?: text.take(80) + "..."
        val reference = verseData["reference"] as? String ?: ""
        val shortReference = verseData["shortReference"] as? String ?: reference

        return VerseWidgetData(
            date = date,
            text = text,
            mediumText = mediumText,
            shortText = shortText,
            reference = reference,
            shortReference = shortReference,
            category = category,
            imageUrl = imageUrl,
        )
    }

    private fun getLanguageCode(): String {
        val lang = Locale.getDefault().language
        return when (lang) {
            "es", "pt", "fr", "it", "de", "pl" -> lang
            else -> "en"
        }
    }

    private fun downloadImage(urlString: String): Bitmap? {
        return try {
            val connection = URL(urlString).openConnection()
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.inputStream.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Image download failed: ${e.message}")
            null
        }
    }
}
