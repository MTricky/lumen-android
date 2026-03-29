package com.app.lumen.widget

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.Color
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.app.lumen.R
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class VerseWidgetData(
    val date: String,
    val text: String,
    val mediumText: String,
    val shortText: String,
    val reference: String,
    val shortReference: String,
    val category: String,
    val imageUrl: String? = null,
    val lastUpdated: Long = System.currentTimeMillis(),
) {
    companion object {
        private const val PREFS_NAME = "verse_widget_prefs"
        private const val KEY_VERSE_DATA = "verse_widget_data"
        private const val KEY_YESTERDAY_VERSE_DATA = "verse_widget_yesterday_data"
        private const val KEY_IS_PREMIUM = "is_premium_user"
        private const val IMAGE_FILENAME = "widget_background.jpg"
        private const val IMAGE_BLURRED_FILENAME = "widget_background_blurred.jpg"
        private const val YESTERDAY_IMAGE_FILENAME = "widget_background_yesterday.jpg"

        private val json = Json { ignoreUnknownKeys = true }

        private fun prefs(context: Context): SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        fun save(context: Context, data: VerseWidgetData) {
            prefs(context).edit()
                .putString(KEY_VERSE_DATA, json.encodeToString(data))
                .apply()
        }

        fun load(context: Context): VerseWidgetData? {
            val jsonStr = prefs(context).getString(KEY_VERSE_DATA, null) ?: return null
            return try {
                json.decodeFromString<VerseWidgetData>(jsonStr)
            } catch (_: Exception) {
                null
            }
        }

        fun saveYesterday(context: Context, data: VerseWidgetData) {
            prefs(context).edit()
                .putString(KEY_YESTERDAY_VERSE_DATA, json.encodeToString(data))
                .apply()
        }

        fun loadYesterday(context: Context): VerseWidgetData? {
            val jsonStr = prefs(context).getString(KEY_YESTERDAY_VERSE_DATA, null) ?: return null
            return try {
                json.decodeFromString<VerseWidgetData>(jsonStr)
            } catch (_: Exception) {
                null
            }
        }

        fun saveYesterdayBackgroundImage(context: Context, bitmap: Bitmap) {
            try {
                val file = File(context.cacheDir, YESTERDAY_IMAGE_FILENAME)
                file.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
            } catch (_: Exception) { }
        }

        fun loadYesterdayBackgroundImage(context: Context): Bitmap? {
            return try {
                val file = File(context.cacheDir, YESTERDAY_IMAGE_FILENAME)
                if (!file.exists()) return null
                BitmapFactory.decodeFile(file.absolutePath)
            } catch (_: Exception) {
                null
            }
        }

        fun isForToday(context: Context): Boolean {
            val data = load(context) ?: return false
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            return data.date == today
        }

        fun savePremiumStatus(context: Context, isPremium: Boolean) {
            prefs(context).edit()
                .putBoolean(KEY_IS_PREMIUM, isPremium)
                .apply()
        }

        fun loadPremiumStatus(context: Context): Boolean =
            prefs(context).getBoolean(KEY_IS_PREMIUM, false)

        fun saveBackgroundImage(context: Context, bitmap: Bitmap) {
            try {
                // Save normal version
                val file = File(context.cacheDir, IMAGE_FILENAME)
                file.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }

                // Save blurred semi-transparent version for locked overlay
                val blurred = blurBitmap(bitmap)
                val semiTransparent = applyAlpha(blurred, 178) // ~70% opacity
                blurred.recycle()
                val blurredFile = File(context.cacheDir, IMAGE_BLURRED_FILENAME)
                blurredFile.outputStream().use { out ->
                    semiTransparent.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                semiTransparent.recycle()
            } catch (_: Exception) { }
        }

        fun loadBackgroundImage(context: Context): Bitmap? {
            return try {
                val file = File(context.cacheDir, IMAGE_FILENAME)
                if (!file.exists()) return null
                BitmapFactory.decodeFile(file.absolutePath)
            } catch (_: Exception) {
                null
            }
        }

        fun loadBlurredBackgroundImage(context: Context): Bitmap? {
            return try {
                val file = File(context.cacheDir, IMAGE_BLURRED_FILENAME)
                if (!file.exists()) return null
                BitmapFactory.decodeFile(file.absolutePath)
            } catch (_: Exception) {
                null
            }
        }

        private fun blurBitmap(source: Bitmap): Bitmap {
            // Downscale for performance, apply stack blur, then upscale
            val scale = 0.25f
            val scaledW = (source.width * scale).toInt().coerceAtLeast(1)
            val scaledH = (source.height * scale).toInt().coerceAtLeast(1)
            val small = Bitmap.createScaledBitmap(source, scaledW, scaledH, true)

            val blurredSmall = stackBlur(small, 20)
            if (blurredSmall !== small) small.recycle()

            val result = Bitmap.createScaledBitmap(
                blurredSmall, source.width, source.height, true,
            )
            blurredSmall.recycle()
            return result
        }

        private fun applyAlpha(source: Bitmap, alpha: Int): Bitmap {
            val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint().apply {
                this.alpha = alpha
            }
            canvas.drawBitmap(source, 0f, 0f, paint)
            return output
        }

        private fun stackBlur(source: Bitmap, radius: Int): Bitmap {
            if (radius < 1) return source

            val bitmap = source.copy(Bitmap.Config.ARGB_8888, true)
            val w = bitmap.width
            val h = bitmap.height
            val pix = IntArray(w * h)
            bitmap.getPixels(pix, 0, w, 0, 0, w, h)

            val wm = w - 1
            val hm = h - 1
            val wh = w * h
            val div = radius + radius + 1

            val r = IntArray(wh)
            val g = IntArray(wh)
            val b = IntArray(wh)

            val vmin = IntArray(maxOf(w, h))
            var divsum = (div + 1) shr 1
            divsum *= divsum
            val dv = IntArray(256 * divsum)
            for (i in dv.indices) dv[i] = i / divsum

            val stack = Array(div) { IntArray(3) }
            val r1 = radius + 1

            // Horizontal pass
            var yi = 0
            for (y in 0 until h) {
                var rsum = 0; var gsum = 0; var bsum = 0
                var rinsum = 0; var ginsum = 0; var binsum = 0
                var routsum = 0; var goutsum = 0; var boutsum = 0
                for (i in -radius..radius) {
                    val p = pix[yi + minOf(wm, maxOf(i, 0))]
                    val sir = stack[i + radius]
                    sir[0] = (p shr 16) and 0xff
                    sir[1] = (p shr 8) and 0xff
                    sir[2] = p and 0xff
                    val rbs = r1 - kotlin.math.abs(i)
                    rsum += sir[0] * rbs; gsum += sir[1] * rbs; bsum += sir[2] * rbs
                    if (i > 0) { rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2] }
                    else { routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2] }
                }
                var stackpointer = radius
                for (x in 0 until w) {
                    r[yi] = dv[rsum]; g[yi] = dv[gsum]; b[yi] = dv[bsum]
                    rsum -= routsum; gsum -= goutsum; bsum -= boutsum
                    val stackstart = stackpointer - radius + div
                    var sir = stack[stackstart % div]
                    routsum -= sir[0]; goutsum -= sir[1]; boutsum -= sir[2]
                    if (y == 0) vmin[x] = minOf(x + radius + 1, wm)
                    val p = pix[yi + vmin[x]]
                    sir[0] = (p shr 16) and 0xff; sir[1] = (p shr 8) and 0xff; sir[2] = p and 0xff
                    rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]
                    rsum += rinsum; gsum += ginsum; bsum += binsum
                    stackpointer = (stackpointer + 1) % div
                    sir = stack[stackpointer % div]
                    routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]
                    rinsum -= sir[0]; ginsum -= sir[1]; binsum -= sir[2]
                    yi++
                }
            }

            // Vertical pass
            for (x in 0 until w) {
                var rsum = 0; var gsum = 0; var bsum = 0
                var rinsum = 0; var ginsum = 0; var binsum = 0
                var routsum = 0; var goutsum = 0; var boutsum = 0
                var yp = -radius * w
                for (i in -radius..radius) {
                    yi = maxOf(0, yp) + x
                    val sir = stack[i + radius]
                    sir[0] = r[yi]; sir[1] = g[yi]; sir[2] = b[yi]
                    val rbs = r1 - kotlin.math.abs(i)
                    rsum += r[yi] * rbs; gsum += g[yi] * rbs; bsum += b[yi] * rbs
                    if (i > 0) { rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2] }
                    else { routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2] }
                    if (i < hm) yp += w
                }
                yi = x
                var stackpointer = radius
                for (y in 0 until h) {
                    pix[yi] = (0xff shl 24) or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]
                    rsum -= routsum; gsum -= goutsum; bsum -= boutsum
                    val stackstart = stackpointer - radius + div
                    var sir = stack[stackstart % div]
                    routsum -= sir[0]; goutsum -= sir[1]; boutsum -= sir[2]
                    if (x == 0) vmin[y] = minOf(y + r1, hm) * w
                    val p = x + vmin[y]
                    sir[0] = r[p]; sir[1] = g[p]; sir[2] = b[p]
                    rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]
                    rsum += rinsum; gsum += ginsum; bsum += binsum
                    stackpointer = (stackpointer + 1) % div
                    sir = stack[stackpointer % div]
                    routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]
                    rinsum -= sir[0]; ginsum -= sir[1]; binsum -= sir[2]
                    yi += w
                }
            }

            bitmap.setPixels(pix, 0, w, 0, 0, w, h)
            return bitmap
        }

        /**
         * Blur a bitmap (e.g. pre-rendered widget content) using stack blur.
         * @param radius pixel radius for the blur effect
         */
        fun blurContentBitmap(bitmap: Bitmap, radius: Int): Bitmap {
            return stackBlur(bitmap, radius)
        }

        suspend fun updateAllWidgets(context: Context) {
            try {
                val manager = GlanceAppWidgetManager(context)

                val mediumIds = manager.getGlanceIds(VerseMediumWidget::class.java)
                mediumIds.forEach { id ->
                    VerseMediumWidget().update(context, id)
                }

                val largeIds = manager.getGlanceIds(VerseLargeWidget::class.java)
                largeIds.forEach { id ->
                    VerseLargeWidget().update(context, id)
                }
            } catch (e: Exception) {
                android.util.Log.e("VerseWidget", "Failed to update widgets", e)
            }
        }

        val placeholder = VerseWidgetData(
            date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
            text = "For God so loved the world, that he gave his only begotten Son, that whosoever believeth in him should not perish, but have everlasting life.",
            mediumText = "For God so loved the world, that he gave his only begotten Son, that whosoever believeth in him should not perish...",
            shortText = "For God so loved the world, that he gave his only begotten Son...",
            reference = "John 3:16",
            shortReference = "Jn 3:16",
            category = "love",
        )
    }
}

// Category helpers
fun categoryColor(category: String): Color = when (category) {
    "faith" -> Color(0xFF6A1B9A)
    "love" -> Color(0xFFC62828)
    "hope" -> Color(0xFFF9A825)
    "strength" -> Color(0xFF2E7D32)
    "peace" -> Color(0xFF0277BD)
    "guidance" -> Color(0xFF4527A0)
    "healing" -> Color(0xFF00838F)
    "family" -> Color(0xFFD84315)
    "gratitude" -> Color(0xFF558B2F)
    "forgiveness" -> Color(0xFF6D4C41)
    else -> Color(0xFF6A1B9A)
}

fun categoryColorInt(category: String): Int = when (category) {
    "faith" -> 0xFF6A1B9A.toInt()
    "love" -> 0xFFC62828.toInt()
    "hope" -> 0xFFF9A825.toInt()
    "strength" -> 0xFF2E7D32.toInt()
    "peace" -> 0xFF0277BD.toInt()
    "guidance" -> 0xFF4527A0.toInt()
    "healing" -> 0xFF00838F.toInt()
    "family" -> 0xFFD84315.toInt()
    "gratitude" -> 0xFF558B2F.toInt()
    "forgiveness" -> 0xFF6D4C41.toInt()
    else -> 0xFF6A1B9A.toInt()
}

fun categoryIconRes(category: String): Int = when (category) {
    "faith" -> R.drawable.ic_cat_faith
    "love" -> R.drawable.ic_cat_love
    "hope" -> R.drawable.ic_cat_hope
    "strength" -> R.drawable.ic_cat_strength
    "peace" -> R.drawable.ic_cat_peace
    "guidance" -> R.drawable.ic_cat_guidance
    "healing" -> R.drawable.ic_cat_healing
    "family" -> R.drawable.ic_cat_family
    "gratitude" -> R.drawable.ic_cat_gratitude
    "forgiveness" -> R.drawable.ic_cat_forgiveness
    else -> R.drawable.ic_cat_faith
}

fun categorySkeletonBadgeRes(category: String): Int = when (category) {
    "faith" -> R.drawable.widget_skeleton_badge_faith
    "love" -> R.drawable.widget_skeleton_badge_love
    "hope" -> R.drawable.widget_skeleton_badge_hope
    "strength" -> R.drawable.widget_skeleton_badge_strength
    "peace" -> R.drawable.widget_skeleton_badge_peace
    "guidance" -> R.drawable.widget_skeleton_badge_guidance
    "healing" -> R.drawable.widget_skeleton_badge_healing
    "family" -> R.drawable.widget_skeleton_badge_family
    "gratitude" -> R.drawable.widget_skeleton_badge_gratitude
    "forgiveness" -> R.drawable.widget_skeleton_badge_forgiveness
    else -> R.drawable.widget_skeleton_badge_faith
}

fun categoryNameRes(category: String): Int = when (category) {
    "faith" -> R.string.verse_category_faith
    "love" -> R.string.verse_category_love
    "hope" -> R.string.verse_category_hope
    "strength" -> R.string.verse_category_strength
    "peace" -> R.string.verse_category_peace
    "guidance" -> R.string.verse_category_guidance
    "healing" -> R.string.verse_category_healing
    "family" -> R.string.verse_category_family
    "gratitude" -> R.string.verse_category_gratitude
    "forgiveness" -> R.string.verse_category_forgiveness
    else -> R.string.verse_category_faith
}

fun categoryName(context: Context, category: String): String =
    context.getString(categoryNameRes(category))
