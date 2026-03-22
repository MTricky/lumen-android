package com.app.lumen.features.liturgy.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.app.lumen.features.audio.AudioPlayerManager
import com.app.lumen.features.audio.ReadingType
import com.app.lumen.features.liturgy.model.DailyLiturgy
import com.app.lumen.features.liturgy.model.DailyVerse
import com.app.lumen.features.liturgy.model.liturgicalColor
import com.app.lumen.features.liturgy.viewmodel.DaySelection
import com.app.lumen.features.liturgy.viewmodel.LiturgyViewModel
import com.app.lumen.ui.components.shimmerBrush
import com.app.lumen.ui.components.GlassButton
import com.app.lumen.ui.components.LiturgyLoadingSkeleton
import com.app.lumen.ui.components.ReadingCard
import com.app.lumen.ui.components.ReflectionCard
import com.app.lumen.ui.components.SaintCard
import com.app.lumen.ui.components.VerseCard
import com.app.lumen.ui.theme.*

private val HEADER_HEIGHT = 380.dp

@Composable
fun LiturgyScreen(
    bottomPadding: Dp = 0.dp,
    onOpenReadings: (DailyLiturgy, DailyVerse?, ReadingSection) -> Unit = { _, _, _ -> },
    viewModel: LiturgyViewModel = viewModel(),
) {
    val liturgy by viewModel.liturgy.collectAsStateWithLifecycle()
    val verse by viewModel.verse.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val daySelection by viewModel.daySelection.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val audioPlayer = remember { AudioPlayerManager.getInstance(context) }
    val currentReading by audioPlayer.currentReadingType.collectAsState()
    val isPlaying by audioPlayer.isPlaying.collectAsState()

    // Check for day change when resuming from background
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.checkForDayChange()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize().background(NearBlack)) {
        AnimatedContent(
            targetState = daySelection,
            transitionSpec = {
                fadeIn(tween(300)) togetherWith fadeOut(tween(300))
            },
            label = "day_switch"
        ) { currentDay ->
            if (isLoading && liturgy == null) {
                // Show shimmer skeleton while loading with no cached data
                Box(modifier = Modifier.fillMaxSize()) {
                    LiturgyLoadingSkeleton()

                    // Day picker on top of skeleton
                    DayPicker(
                        selected = currentDay,
                        onSelect = viewModel::selectDay,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = 8.dp)
                    )
                }
            } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // Header with image + overlay content
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(HEADER_HEIGHT)
                    ) {
                        // Background image
                        if (liturgy?.imageUrl != null) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(liturgy?.imageUrl)
                                    .crossfade(300)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                alignment = Alignment.Center,
                                loading = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(shimmerBrush())
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .drawWithContent {
                                        drawContent()
                                        // Top fade for status bar
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    NearBlack.copy(alpha = 0.4f),
                                                    Color.Transparent,
                                                ),
                                                startY = 0f,
                                                endY = size.height * 0.15f,
                                            )
                                        )
                                        // Bottom fade into content
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    NearBlack.copy(alpha = 0.5f),
                                                    NearBlack,
                                                ),
                                                startY = size.height * 0.45f,
                                                endY = size.height,
                                            )
                                        )
                                    }
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                MarianBlue.copy(alpha = 0.6f),
                                                NearBlack,
                                            )
                                        )
                                    )
                            )
                        }

                        // Day picker at top center
                        DayPicker(
                            selected = currentDay,
                            onSelect = viewModel::selectDay,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(top = 8.dp)
                        )

                        // Header text centered at bottom
                        if (liturgy != null) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(horizontal = 24.dp)
                                    .padding(bottom = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = viewModel.getDisplayDate(),
                                    fontSize = 15.sp,
                                    color = Color.White.copy(alpha = 0.85f),
                                )

                                Spacer(Modifier.height(6.dp))

                                Text(
                                    text = liturgy!!.celebration,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    lineHeight = 30.sp,
                                    textAlign = TextAlign.Center,
                                )

                                Spacer(Modifier.height(8.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(liturgicalColor(liturgy!!.liturgicalColor))
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = liturgy!!.season,
                                        fontSize = 14.sp,
                                        color = Color.White.copy(alpha = 0.7f),
                                    )
                                }
                            }
                        }
                    }
                }

                if (error != null && liturgy == null) {
                    item {
                        Text(
                            text = error!!,
                            color = Slate,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 60.dp, start = 20.dp, end = 20.dp)
                        )
                    }
                } else if (liturgy != null) {
                    val lit = liturgy!!

                    // Saint of the Day
                    if (lit.saintOfDay != null) {
                        item {
                            SaintCard(
                                name = lit.saintOfDay.name,
                                description = lit.saintOfDay.description,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                onClick = { onOpenReadings(lit, verse, ReadingSection.SAINT) },
                            )
                        }
                    }

                    // First Reading
                    item {
                        ReadingCard(
                            icon = Icons.Filled.LooksOne,
                            label = "First Reading",
                            reference = lit.readings.firstReading.reference,
                            previewText = lit.readings.firstReading.text,
                            audioUrl = lit.audioUrls?.firstReading,
                            isPlayingThis = isPlaying && currentReading == ReadingType.FIRST_READING,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            onClick = { onOpenReadings(lit, verse, ReadingSection.FIRST_READING) },
                            onPlayClick = {
                                val url = lit.audioUrls?.firstReading ?: return@ReadingCard
                                if (currentReading == ReadingType.FIRST_READING) {
                                    audioPlayer.togglePlayPause()
                                } else {
                                    audioPlayer.play(url, ReadingType.FIRST_READING, "First Reading")
                                }
                            },
                        )
                    }

                    // Psalm
                    item {
                        ReadingCard(
                            icon = Icons.Filled.MusicNote,
                            label = "Responsorial Psalm",
                            reference = lit.readings.psalm.reference,
                            previewText = lit.readings.psalm.response,
                            audioUrl = lit.audioUrls?.psalm,
                            isPlayingThis = isPlaying && currentReading == ReadingType.PSALM,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            onClick = { onOpenReadings(lit, verse, ReadingSection.PSALM) },
                            onPlayClick = {
                                val url = lit.audioUrls?.psalm ?: return@ReadingCard
                                if (currentReading == ReadingType.PSALM) {
                                    audioPlayer.togglePlayPause()
                                } else {
                                    audioPlayer.play(url, ReadingType.PSALM, "Responsorial Psalm")
                                }
                            },
                        )
                    }

                    // Second Reading
                    if (lit.readings.secondReading != null) {
                        item {
                            ReadingCard(
                                icon = Icons.Filled.LooksTwo,
                                label = "Second Reading",
                                reference = lit.readings.secondReading.reference,
                                previewText = lit.readings.secondReading.text,
                                audioUrl = lit.audioUrls?.secondReading,
                                isPlayingThis = isPlaying && currentReading == ReadingType.SECOND_READING,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                onClick = { onOpenReadings(lit, verse, ReadingSection.SECOND_READING) },
                                onPlayClick = {
                                    val url = lit.audioUrls?.secondReading ?: return@ReadingCard
                                    if (currentReading == ReadingType.SECOND_READING) {
                                        audioPlayer.togglePlayPause()
                                    } else {
                                        audioPlayer.play(url, ReadingType.SECOND_READING, "Second Reading")
                                    }
                                },
                            )
                        }
                    }

                    // Gospel (prominent)
                    item {
                        ReadingCard(
                            icon = Icons.Filled.AutoStories,
                            label = "Gospel",
                            reference = lit.readings.gospel.reference,
                            previewText = lit.readings.gospel.text,
                            prominent = true,
                            audioUrl = lit.audioUrls?.gospel,
                            isPlayingThis = isPlaying && currentReading == ReadingType.GOSPEL,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            onClick = { onOpenReadings(lit, verse, ReadingSection.GOSPEL) },
                            onPlayClick = {
                                val url = lit.audioUrls?.gospel ?: return@ReadingCard
                                if (currentReading == ReadingType.GOSPEL) {
                                    audioPlayer.togglePlayPause()
                                } else {
                                    audioPlayer.play(url, ReadingType.GOSPEL, "Gospel")
                                }
                            },
                        )
                    }

                    // Reflection (sermon)
                    if (lit.sermon != null) {
                        item {
                            ReflectionCard(
                                text = lit.sermon,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                onClick = { onOpenReadings(lit, verse, ReadingSection.REFLECTION) },
                            )
                        }
                    }

                    // Verse of the Day
                    if (verse != null) {
                        item {
                            Spacer(Modifier.height(4.dp))
                            VerseCard(
                                text = verse!!.text,
                                reference = verse!!.reference,
                                category = verse!!.category,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // View All Readings button
                    item {
                        Spacer(Modifier.height(16.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            GlassButton(
                                title = "View All Readings",
                                icon = Icons.AutoMirrored.Filled.MenuBook,
                                onClick = {
                                    val firstSection = if (lit.saintOfDay != null) ReadingSection.SAINT
                                        else ReadingSection.FIRST_READING
                                    onOpenReadings(lit, verse, firstSection)
                                },
                            )
                        }
                    }

                    // Bottom spacer for tab bar
                    item {
                        Spacer(Modifier.height(bottomPadding + 8.dp))
                    }
                }
            }
            } // end else (not loading)
        }
    }
}

@Composable
private fun DayPicker(
    selected: DaySelection,
    onSelect: (DaySelection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .width(190.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.25f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(3.dp),
    ) {
        DaySelection.entries.forEach { day ->
            val isSelected = day == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(17.dp))
                    .background(
                        if (isSelected) Color.White.copy(alpha = 0.2f)
                        else Color.Transparent
                    )
                    .clickable { onSelect(day) }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (day == DaySelection.TODAY) "Today" else "Tomorrow",
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = Color.White,
                )
            }
        }
    }
}
