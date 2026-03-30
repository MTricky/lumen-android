package com.app.lumen.features.liturgy.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.app.lumen.features.subscription.PaywallSheet
import com.app.lumen.features.subscription.SubscriptionManager
import com.app.lumen.features.survey.ui.SurveySheet
import com.app.lumen.features.survey.viewmodel.SurveyViewModel
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
import androidx.compose.ui.res.stringResource
import com.app.lumen.R
import com.app.lumen.features.calendar.model.LiturgicalSeason
import com.app.lumen.services.AnalyticsEvent
import com.app.lumen.services.AnalyticsManager
import com.app.lumen.ui.theme.*

private val HEADER_HEIGHT = 380.dp

@Composable
fun LiturgyScreen(
    bottomPadding: Dp = 0.dp,
    onOpenReadings: (DailyLiturgy, DailyVerse?, ReadingSection) -> Unit = { _, _, _ -> },
    onVerseClick: (DailyVerse) -> Unit = {},
    viewModel: LiturgyViewModel = viewModel(),
) {
    val liturgy by viewModel.liturgy.collectAsStateWithLifecycle()
    val verse by viewModel.verse.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val daySelection by viewModel.daySelection.collectAsStateWithLifecycle()
    val isPremium by SubscriptionManager.hasProAccess.collectAsState()
    var showPaywall by remember { mutableStateOf(false) }
    var showSurveyAlert by remember { mutableStateOf(false) }
    var showSurvey by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var showSurveyButton by remember { mutableStateOf(SurveyViewModel.shouldShowSurvey(context)) }
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

                        // Survey button (top-right) with pulsing glow
                        if (showSurveyButton) {
                            val infiniteTransition = rememberInfiniteTransition(label = "survey_pulse")
                            val pulseAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.15f,
                                targetValue = 0.35f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1200, easing = androidx.compose.animation.core.EaseInOut),
                                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
                                ),
                                label = "survey_pulse_alpha",
                            )
                            val pulseScale by infiniteTransition.animateFloat(
                                initialValue = 1f,
                                targetValue = 1.08f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1200, easing = androidx.compose.animation.core.EaseInOut),
                                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
                                ),
                                label = "survey_pulse_scale",
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .statusBarsPadding()
                                    .padding(top = 8.dp, end = 12.dp)
                                    .size(44.dp)
                                    .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = pulseAlpha))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                                    .clickable { showSurveyAlert = true },
                                contentAlignment = Alignment.Center,
                            ) {
                                @Suppress("DEPRECATION")
                                Icon(
                                    imageVector = Icons.Filled.Assignment,
                                    contentDescription = "Survey",
                                    tint = SoftGold,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }

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
                                    val seasonRes = LiturgicalSeason.fromRawValue(liturgy!!.season).displayNameRes
                                    Text(
                                        text = stringResource(seasonRes),
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
                        val firstReadingLabel = stringResource(R.string.section_first_reading_title)
                        ReadingCard(
                            icon = Icons.Filled.LooksOne,
                            label = firstReadingLabel,
                            reference = lit.readings.firstReading.reference,
                            previewText = lit.readings.firstReading.text,
                            audioUrl = lit.audioUrls?.firstReading,
                            isPlayingThis = isPlaying && currentReading == ReadingType.FIRST_READING,
                            isPremium = isPremium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            onClick = { onOpenReadings(lit, verse, ReadingSection.FIRST_READING) },
                            onPlayClick = {
                                if (!isPremium) { showPaywall = true; return@ReadingCard }
                                val url = lit.audioUrls?.firstReading ?: return@ReadingCard
                                if (currentReading == ReadingType.FIRST_READING) {
                                    audioPlayer.togglePlayPause()
                                } else {
                                    audioPlayer.play(url, ReadingType.FIRST_READING, firstReadingLabel)
                                }
                            },
                        )
                    }

                    // Psalm
                    item {
                        val psalmLabel = stringResource(R.string.section_psalm_title)
                        ReadingCard(
                            icon = Icons.Filled.MusicNote,
                            label = psalmLabel,
                            reference = lit.readings.psalm.reference,
                            previewText = lit.readings.psalm.response,
                            audioUrl = lit.audioUrls?.psalm,
                            isPlayingThis = isPlaying && currentReading == ReadingType.PSALM,
                            isPremium = isPremium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            onClick = { onOpenReadings(lit, verse, ReadingSection.PSALM) },
                            onPlayClick = {
                                if (!isPremium) { showPaywall = true; return@ReadingCard }
                                val url = lit.audioUrls?.psalm ?: return@ReadingCard
                                if (currentReading == ReadingType.PSALM) {
                                    audioPlayer.togglePlayPause()
                                } else {
                                    audioPlayer.play(url, ReadingType.PSALM, psalmLabel)
                                }
                            },
                        )
                    }

                    // Second Reading
                    if (lit.readings.secondReading != null) {
                        item {
                            val secondReadingLabel = stringResource(R.string.section_second_reading_title)
                            ReadingCard(
                                icon = Icons.Filled.LooksTwo,
                                label = secondReadingLabel,
                                reference = lit.readings.secondReading.reference,
                                previewText = lit.readings.secondReading.text,
                                audioUrl = lit.audioUrls?.secondReading,
                                isPlayingThis = isPlaying && currentReading == ReadingType.SECOND_READING,
                                isPremium = isPremium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                onClick = { onOpenReadings(lit, verse, ReadingSection.SECOND_READING) },
                                onPlayClick = {
                                    if (!isPremium) { showPaywall = true; return@ReadingCard }
                                    val url = lit.audioUrls?.secondReading ?: return@ReadingCard
                                    if (currentReading == ReadingType.SECOND_READING) {
                                        audioPlayer.togglePlayPause()
                                    } else {
                                        audioPlayer.play(url, ReadingType.SECOND_READING, secondReadingLabel)
                                    }
                                },
                            )
                        }
                    }

                    // Gospel (prominent)
                    item {
                        val gospelLabel = stringResource(R.string.section_gospel_title)
                        ReadingCard(
                            icon = Icons.Filled.AutoStories,
                            label = gospelLabel,
                            reference = lit.readings.gospel.reference,
                            previewText = lit.readings.gospel.text,
                            prominent = true,
                            audioUrl = lit.audioUrls?.gospel,
                            isPlayingThis = isPlaying && currentReading == ReadingType.GOSPEL,
                            isPremium = isPremium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            onClick = { onOpenReadings(lit, verse, ReadingSection.GOSPEL) },
                            onPlayClick = {
                                if (!isPremium) { showPaywall = true; return@ReadingCard }
                                val url = lit.audioUrls?.gospel ?: return@ReadingCard
                                if (currentReading == ReadingType.GOSPEL) {
                                    audioPlayer.togglePlayPause()
                                } else {
                                    audioPlayer.play(url, ReadingType.GOSPEL, gospelLabel)
                                }
                            },
                        )
                    }

                    // Reflection (sermon)
                    if (lit.sermon != null) {
                        item {
                            ReflectionCard(
                                text = lit.sermon,
                                isPremium = isPremium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                onClick = {
                                    if (!isPremium) { showPaywall = true; return@ReflectionCard }
                                    onOpenReadings(lit, verse, ReadingSection.REFLECTION)
                                },
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
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                onClick = { onVerseClick(verse!!) },
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
                                title = stringResource(R.string.view_all_readings),
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

        if (showPaywall) {
            PaywallSheet(onDismiss = { showPaywall = false })
        }

        if (showSurvey) {
            SurveySheet(onDismiss = {
                showSurvey = false
                showSurveyButton = false
            })
        }

        SurveyAlertDialog(
            isPresented = showSurveyAlert,
            onContinue = {
                showSurveyAlert = false
                AnalyticsManager.trackEvent(AnalyticsEvent.SURVEY_ALERT_INTERACTED, mapOf("dismissed" to false))
                showSurveyButton = false
                showSurvey = true
            },
            onDismiss = {
                showSurveyAlert = false
                AnalyticsManager.trackEvent(AnalyticsEvent.SURVEY_ALERT_INTERACTED, mapOf("dismissed" to true))
                SurveyViewModel.dismissSurvey(context)
                showSurveyButton = false
            },
        )
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
                    text = stringResource(if (day == DaySelection.TODAY) R.string.today else R.string.tomorrow),
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = Color.White,
                )
            }
        }
    }
}

// Glass survey alert matching the CompletionReviewDialog pattern
@Composable
private fun SurveyAlertDialog(
    isPresented: Boolean,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = isPresented,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(200)),
    ) {
        val cardScale = remember { androidx.compose.animation.core.Animatable(0.85f) }
        LaunchedEffect(Unit) {
            cardScale.animateTo(
                targetValue = 1f,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = 0.75f,
                    stiffness = 400f,
                ),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                ) { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .graphicsLayer {
                        scaleX = cardScale.value
                        scaleY = cardScale.value
                    }
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1A1A29))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                    ) { /* consume taps */ }
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.survey_alert_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = stringResource(R.string.survey_alert_message),
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Start,
                    lineHeight = 20.sp,
                )

                Spacer(Modifier.height(24.dp))

                // Continue
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                        .clickable { onContinue() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.survey_alert_continue),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Not Now
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                        .clickable { onDismiss() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.survey_alert_not_now),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                    )
                }
            }
        }
    }
}
