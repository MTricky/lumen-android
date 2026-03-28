package com.app.lumen.features.onboarding.ui.phases

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.lumen.R
import com.app.lumen.features.onboarding.OnboardingViewModel
import com.app.lumen.ui.HapticManager
import com.app.lumen.features.onboarding.ui.components.OnboardingGlassProminentButton
import com.app.lumen.ui.theme.NearBlack
import com.app.lumen.ui.theme.SoftGold
import com.app.lumen.widget.VerseWidgetData
import com.app.lumen.widget.categoryColor
import kotlinx.coroutines.delay

private data class WidgetVerseDisplay(
    val text: String,
    val mediumText: String,
    val reference: String,
    val shortReference: String,
    val category: String
)

private val fallbackTodayVerse = WidgetVerseDisplay(
    text = "For God so loved the world, that he gave his only begotten Son, that whosoever believeth in him should not perish, but have everlasting life.",
    mediumText = "For God so loved the world, that he gave his only begotten Son, that whosoever believeth in him should not perish...",
    reference = "John 3:16",
    shortReference = "Jn 3:16",
    category = "love"
)

private val fallbackYesterdayVerse = WidgetVerseDisplay(
    text = "The Lord is my shepherd; I shall not want. He maketh me to lie down in green pastures: he leadeth me beside the still waters. He restoreth my soul.",
    mediumText = "The Lord is my shepherd; I shall not want. He maketh me to lie down in green pastures...",
    reference = "Psalm 23:1-3",
    shortReference = "Ps 23:1-3",
    category = "peace"
)

@Composable
fun WidgetsPhase(viewModel: OnboardingViewModel, onContinue: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    var largeVerse by remember { mutableStateOf(fallbackYesterdayVerse) }
    var mediumVerse by remember { mutableStateOf(fallbackTodayVerse) }
    var todayBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var yesterdayBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Try to load real verse data + background images (may take a moment for worker to finish)
    LaunchedEffect(Unit) {
        // Retry a few times since the widget worker may still be fetching
        repeat(5) {
            // Today's verse for medium widget
            val data = VerseWidgetData.load(context)
            if (data != null) {
                mediumVerse = WidgetVerseDisplay(
                    text = data.text,
                    mediumText = data.mediumText,
                    reference = data.reference,
                    shortReference = data.shortReference,
                    category = data.category
                )
            }
            // Yesterday's verse for large widget
            val yesterdayData = VerseWidgetData.loadYesterday(context)
            if (yesterdayData != null) {
                largeVerse = WidgetVerseDisplay(
                    text = yesterdayData.text,
                    mediumText = yesterdayData.mediumText,
                    reference = yesterdayData.reference,
                    shortReference = yesterdayData.shortReference,
                    category = yesterdayData.category
                )
            }
            // Load separate background images for each widget
            val todayBg = VerseWidgetData.loadBackgroundImage(context)
            if (todayBg != null) todayBitmap = todayBg
            val yesterdayBg = VerseWidgetData.loadYesterdayBackgroundImage(context)
            if (yesterdayBg != null) yesterdayBitmap = yesterdayBg
            // Exit early if both images loaded
            if (todayBitmap != null && yesterdayBitmap != null) return@LaunchedEffect
            delay(2000)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Blurred background
        Image(
            painter = painterResource(id = R.drawable.onboarding_olive_garden),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(30.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NearBlack.copy(alpha = 0.4f))
        )

        // Title + subtitle take ~130dp, button ~60dp, padding ~80dp = ~270dp fixed
        // Remaining space is split between widgets
        androidx.compose.foundation.layout.BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 40.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 24.dp)
        ) {
            val totalHeight = maxHeight
            // Reserve space for title (~130dp) + button (~70dp) + spacers (~40dp)
            val widgetSpace = totalHeight - 240.dp
            val largeHeight = (widgetSpace * 0.63f).coerceIn(200.dp, 300.dp)
            val mediumHeight = (widgetSpace * 0.32f).coerceIn(110.dp, 150.dp)

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title section
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Widgets,
                        contentDescription = null,
                        tint = SoftGold,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.onboarding_widgets_title),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.onboarding_widgets_subtitle),
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Widget previews with dynamic heights
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    WidgetLargePreview(
                        verse = largeVerse,
                        backgroundBitmap = yesterdayBitmap,
                        height = largeHeight
                    )
                    WidgetMediumPreview(
                        verse = mediumVerse,
                        backgroundBitmap = todayBitmap,
                        height = mediumHeight
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                OnboardingGlassProminentButton(title = stringResource(R.string.onboarding_continue)) {
                    HapticManager.selection(view)
                    onContinue()
                }
            }
        }
    }
}

// ---- Exact 1:1 replicas of premium widget content using regular Compose ----

@Composable
private fun WidgetLargePreview(
    verse: WidgetVerseDisplay,
    backgroundBitmap: Bitmap? = null,
    height: androidx.compose.ui.unit.Dp = 300.dp
) {
    val catColor = categoryColor(verse.category)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(24.dp))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
    ) {
        // Background image (prefer Firebase cached image, fallback to drawable)
        if (backgroundBitmap != null) {
            Image(
                bitmap = backgroundBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.onboarding_olive_garden),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        // Gradient overlay (matching iOS: 0.2 top -> 0.4 bottom)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.4f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Top: category badge + quote icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category badge (capsule, matching iOS sizes)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(catColor.copy(alpha = 0.9f))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = categoryIconRes(verse.category)),
                            contentDescription = null,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = categoryName(verse.category),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Quote icon
                Image(
                    painter = painterResource(id = R.drawable.ic_widget_quote),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    alpha = 0.4f
                )
            }

            // Center: verse text
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 2.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = verse.text,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 6,
                    lineHeight = 28.sp
                )
            }

            // Bottom: reference badge (capsule)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = verse.reference,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetMediumPreview(
    verse: WidgetVerseDisplay,
    backgroundBitmap: Bitmap? = null,
    height: androidx.compose.ui.unit.Dp = 150.dp
) {
    val catColor = categoryColor(verse.category)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(22.dp))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
    ) {
        // Background image (same image as large widget, matching iOS)
        if (backgroundBitmap != null) {
            Image(
                bitmap = backgroundBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.onboarding_olive_garden),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        // Gradient overlay (matching iOS: 0.2 top -> 0.4 bottom)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.4f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Top: category badge + quote icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category badge (capsule, matching iOS medium sizes)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(catColor.copy(alpha = 0.9f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = categoryIconRes(verse.category)),
                            contentDescription = null,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = categoryName(verse.category),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Image(
                    painter = painterResource(id = R.drawable.ic_widget_quote),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    alpha = 0.4f
                )
            }

            // Center: verse text
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 2.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = verse.mediumText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 3,
                    lineHeight = 21.sp
                )
            }
        }
    }
}

// Helpers matching widget code
private fun categoryIconRes(category: String): Int = when (category) {
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

private fun categoryName(category: String): String = when (category) {
    "faith" -> "Faith"
    "love" -> "Love"
    "hope" -> "Hope"
    "strength" -> "Strength"
    "peace" -> "Peace"
    "guidance" -> "Guidance"
    "healing" -> "Healing"
    "family" -> "Family"
    "gratitude" -> "Gratitude"
    "forgiveness" -> "Forgiveness"
    else -> category.replaceFirstChar { it.uppercase() }
}
