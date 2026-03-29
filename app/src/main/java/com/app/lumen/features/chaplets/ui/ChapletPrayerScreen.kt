package com.app.lumen.features.chaplets.ui

import android.content.Context
import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.lumen.R
import com.app.lumen.features.chaplets.model.Prayer
import com.app.lumen.features.chaplets.service.ChapletAudioPlayer
import com.app.lumen.features.rosary.service.RosaryAudioService
import com.app.lumen.features.rosary.ui.AudioBarsView
import com.app.lumen.features.rosary.ui.AudioControlsPanel
import com.app.lumen.features.rosary.ui.RosaryVisualMode
import com.app.lumen.ui.theme.NearBlack
import com.app.lumen.ui.theme.SoftGold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

// Sacred Art mode
private val GlassBg = Color(0xFF2A2A2E).copy(alpha = 0.86f)
private val GlassBorder = SoftGold.copy(alpha = 0.20f)
// Simple mode
private val SimpleGlassBg = Color(0xFF1E1E32).copy(alpha = 0.85f)
private val SimpleGlassBorder = Color.White.copy(alpha = 0.10f)
private val ButtonGlassBg = Color.White.copy(alpha = 0.12f)
private val ButtonGlassBorder = Color.White.copy(alpha = 0.20f)

private const val CROSSFADE_MS = 1000

private fun prayerLanguageCode(): String {
    val lang = Locale.getDefault().language
    return when {
        lang.startsWith("pl") -> "pl"
        lang.startsWith("fr") -> "fr"
        lang.startsWith("es") -> "es"
        lang.startsWith("pt") -> "pt"
        lang.startsWith("it") -> "it"
        lang.startsWith("de") -> "de"
        else -> "en"
    }
}

/**
 * Data class to hold what the chaplet prayer screen needs to display.
 * Each chaplet ViewModel maps its state into this common representation.
 */
data class ChapletPrayerState(
    val currentStepIndex: Int,
    val isIntro: Boolean,
    val isAnnouncement: Boolean,
    val prayer: Prayer?,
    val backgroundRes: Int?,
    val titleLabel: String,
    val progressContent: @Composable () -> Unit,
    // Audio
    val chapletType: String = "",
    val chapletDisplayName: String = "",
    // Announcement-specific content
    val announcementTitle: String? = null,
    val announcementSubtitle: String? = null,
    val announcementBody: String? = null,
    val announcementFruit: String? = null,
    val announcementScripture: String? = null,
    val announcementNumber: Int? = null,
    val announcementNumberLabel: String? = null,
)

@Composable
fun ChapletPrayerScreen(
    state: ChapletPrayerState,
    onAdvance: () -> Unit,
    onGoBack: () -> Unit,
    onNavigateBack: () -> Unit,
    onComplete: () -> Unit,
    isComplete: Boolean,
    peekNextIsAnnouncement: Boolean = false,
    playAudioForStep: ((onFinished: () -> Unit) -> Unit)? = null,
) {
    var contentVisible by remember { mutableStateOf(true) }
    var isTransitioning by remember { mutableStateOf(false) }
    var transitionDirection by remember { mutableIntStateOf(1) }
    var transitionFromIntro by remember { mutableStateOf(false) }
    var transitionInvolvesMystery by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val visualMode = remember { RosaryVisualMode.current(context) }
    val isSimple = visualMode == RosaryVisualMode.SIMPLE

    // Audio state
    val audioPlayer = remember { ChapletAudioPlayer.getInstance(context) }
    val audioService = remember { RosaryAudioService.getInstance(context) }
    val rosaryPrefs = remember { context.getSharedPreferences("rosary_prefs", Context.MODE_PRIVATE) }

    val chapletType = state.chapletType

    val audioLang = remember { prayerLanguageCode() }

    // Audio requires BOTH rosary AND chaplet downloaded (like iOS)
    var isChapletAudioDownloaded by remember { mutableStateOf(
        if (chapletType.isNotEmpty()) audioService.isChapletAudioDownloaded(audioLang, chapletType) else false
    ) }
    var isRosaryAudioDownloaded by remember { mutableStateOf(audioService.isAudioDownloaded(audioLang)) }
    val isAudioFullyDownloaded = isChapletAudioDownloaded && isRosaryAudioDownloaded
    // Show audio button if either is downloaded or chaplet config exists remotely
    var hasChapletAudioAvailable by remember { mutableStateOf(false) }
    val showAudioButton = isAudioFullyDownloaded || (hasChapletAudioAvailable && isRosaryAudioDownloaded)

    val audioIsPlaying by audioPlayer.isPlaying.collectAsState()
    val audioIsAutoAdvancing by audioPlayer.isAutoAdvancing.collectAsState()

    var isAudioEnabled by remember { mutableStateOf(rosaryPrefs.getBoolean("audio_enabled", false)) }
    var audioSpeed by remember { mutableFloatStateOf(rosaryPrefs.getFloat("audio_speed", 1.0f)) }
    var isAutoAdvanceEnabled by remember { mutableStateOf(rosaryPrefs.getBoolean("auto_advance", false)) }

    var showAudioControls by remember { mutableStateOf(false) }
    var audioWasReEnabled by remember { mutableStateOf(false) }

    // Show download sheet if needed — with slight delay
    var showDownloadSheet by remember { mutableStateOf(false) }

    fun recheckAudioState() {
        isRosaryAudioDownloaded = audioService.isAudioDownloaded(audioLang)
        isChapletAudioDownloaded = if (chapletType.isNotEmpty()) {
            audioService.isChapletAudioDownloaded(audioLang, chapletType)
        } else false
    }

    LaunchedEffect(chapletType) {
        if (chapletType.isEmpty()) return@LaunchedEffect

        // Check remote availability
        withContext(Dispatchers.IO) {
            val chapletConfig = audioService.fetchChapletAudioConfig(audioLang, chapletType)
            if (chapletConfig != null) {
                hasChapletAudioAvailable = true
            }
        }

        // Show sheet with slight delay if chaplet not downloaded (only if rosary audio is already downloaded)
        val sheetShown = rosaryPrefs.getBoolean("chaplet_audio_offer_shown_${chapletType}_${audioLang}", false)
        if (!sheetShown && !isChapletAudioDownloaded && isRosaryAudioDownloaded) {
            delay(400)
            showDownloadSheet = true
        }
    }

    // Load audio config only when BOTH are downloaded
    LaunchedEffect(isAudioFullyDownloaded) {
        if (chapletType.isNotEmpty() && isAudioFullyDownloaded) {
            withContext(Dispatchers.IO) {
                val chapletConfig = audioService.fetchChapletAudioConfig(audioLang, chapletType)
                val rosaryConfig = audioService.fetchAudioConfig(audioLang)
                if (chapletConfig != null) {
                    withContext(Dispatchers.Main) {
                        audioPlayer.configure(chapletConfig, rosaryConfig, audioLang, chapletType)
                    }
                }
            }
        }
    }

    // Teardown audio on dispose
    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.teardown()
        }
    }

    // When step changes and contentVisible is false, show after one frame
    LaunchedEffect(state.currentStepIndex) {
        if (!contentVisible && !isComplete) {
            delay(1)
            contentVisible = true
        }
    }

    val scope = rememberCoroutineScope()
    val view = LocalView.current

    fun animateTransition(direction: Int) {
        if (isTransitioning) return
        if (direction == -1 && state.currentStepIndex == 0) return
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)

        // Stop current audio before transitioning
        audioPlayer.stopCurrentPlayback()

        transitionFromIntro = state.isIntro
        val isSlowTransition = state.isIntro || state.isAnnouncement || (direction == 1 && peekNextIsAnnouncement)
        transitionInvolvesMystery = isSlowTransition
        transitionDirection = direction
        isTransitioning = true
        contentVisible = false

        val fadeOutMs = if (isSlowTransition) 350L else 150L

        scope.launch {
            delay(fadeOutMs)
            if (direction == 1) onAdvance() else onGoBack()

            if (isComplete) {
                delay(400)
                onComplete()
                return@launch
            }

            val fadeInMs = if (isSlowTransition) 450L else 200L
            delay(fadeInMs)
            isTransitioning = false

            // Trigger audio for the new step (only if BOTH downloaded)
            if (isAudioEnabled && isAudioFullyDownloaded) {
                playAudioForStep?.invoke {
                    animateTransition(1) // auto-advance
                }
            }
        }
    }

    val onTapAdvance: () -> Unit = { animateTransition(1) }
    val onTapBack: () -> Unit = { animateTransition(-1) }

    // Build enter/exit animations
    val slideOffset = if (transitionInvolvesMystery) 40 else 60
    val fadeInMs = if (transitionInvolvesMystery) 450 else 200
    val fadeOutMs = if (transitionInvolvesMystery) 350 else 150

    val enterAnim = if (transitionFromIntro) {
        fadeIn(tween(fadeInMs, easing = FastOutSlowInEasing))
    } else {
        fadeIn(tween(fadeInMs, easing = FastOutSlowInEasing)) +
            slideIn(tween(fadeInMs, easing = FastOutSlowInEasing)) {
                IntOffset(0, if (transitionDirection == 1) slideOffset else -slideOffset)
            }
    }

    val exitAnim = fadeOut(tween(fadeOutMs, easing = FastOutSlowInEasing)) +
        slideOut(tween(fadeOutMs, easing = FastOutSlowInEasing)) {
            IntOffset(0, if (transitionDirection == 1) -slideOffset else slideOffset)
        }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NearBlack),
    ) {
        // Background
        if (state.backgroundRes != null) {
            Crossfade(
                targetState = state.backgroundRes,
                animationSpec = tween(CROSSFADE_MS),
                label = "bg_crossfade",
            ) { bgRes ->
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(bgRes),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.25f)),
                    )
                }
            }
        }

        // Content
        if (state.isIntro) {
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(tween(450, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(350, easing = FastOutSlowInEasing)),
            ) {
                IntroContent(
                    titleLabel = state.titleLabel,
                    isSimple = isSimple,
                    onTap = onTapAdvance,
                )
            }
        } else if (state.isAnnouncement) {
            AnnouncementScreen(
                title = state.announcementTitle,
                subtitle = state.announcementSubtitle,
                body = state.announcementBody,
                fruit = state.announcementFruit,
                scripture = state.announcementScripture,
                number = state.announcementNumber,
                numberLabel = state.announcementNumberLabel,
                isSimple = isSimple,
                contentVisible = contentVisible,
                enterAnim = enterAnim,
                exitAnim = exitAnim,
                onTap = onTapAdvance,
            )
        } else {
            PrayerScreen(
                prayer = state.prayer,
                progressContent = state.progressContent,
                isSimple = isSimple,
                contentVisible = contentVisible,
                enterAnim = enterAnim,
                exitAnim = exitAnim,
                onTap = onTapAdvance,
            )
        }

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassCircleButton(onClick = onNavigateBack, size = 40) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.rosary_close),
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            if (!state.isIntro) {
                Text(
                    text = state.titleLabel,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            if (showAudioButton) {
                GlassCircleButton(
                    onClick = {
                        if (isAudioFullyDownloaded) {
                            // Both downloaded — show controls
                            showAudioControls = true
                        } else {
                            // Not fully downloaded — re-show download sheet
                            showDownloadSheet = true
                        }
                    },
                    size = 40,
                ) {
                    AudioBarsView(
                        isAnimating = isAudioEnabled && isAudioFullyDownloaded && (audioIsPlaying || audioIsAutoAdvancing),
                        color = if (isAudioEnabled && isAudioFullyDownloaded) Color.White else Color.White.copy(alpha = 0.4f),
                    )
                }
            } else {
                Spacer(Modifier.size(40.dp))
            }
        }

        // Bottom chevrons
        if (!state.isIntro) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp, start = 24.dp, end = 24.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                GlassCircleButton(onClick = onTapBack, size = 46, enabled = state.currentStepIndex > 0) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.rosary_previous),
                        tint = if (state.currentStepIndex > 0) Color.White else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(24.dp),
                    )
                }
                GlassCircleButton(onClick = onTapAdvance, size = 46) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.rosary_next),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        // Audio controls panel overlay
        AudioControlsPanel(
            isPresented = showAudioControls,
            isSimple = isSimple,
            onDismiss = {
                showAudioControls = false
                if (audioWasReEnabled && isAudioEnabled && isAudioFullyDownloaded && !audioIsPlaying) {
                    playAudioForStep?.invoke {
                        animateTransition(1)
                    }
                }
                audioWasReEnabled = false
            },
            isAudioEnabled = isAudioEnabled,
            onAudioEnabledChange = { enabled ->
                isAudioEnabled = enabled
                rosaryPrefs.edit().putBoolean("audio_enabled", enabled).apply()
                if (!enabled) {
                    audioPlayer.stopAll()
                    audioWasReEnabled = false
                } else {
                    audioWasReEnabled = true
                }
            },
            audioSpeed = audioSpeed,
            onAudioSpeedChange = { speed ->
                audioSpeed = speed
                rosaryPrefs.edit().putFloat("audio_speed", speed).apply()
                audioPlayer.updatePlaybackSpeed()
            },
            isAutoAdvanceEnabled = isAutoAdvanceEnabled,
            onAutoAdvanceChange = { enabled ->
                isAutoAdvanceEnabled = enabled
                rosaryPrefs.edit().putBoolean("auto_advance", enabled).apply()
            },
        )
    }

    // Download sheet
    if (showDownloadSheet && chapletType.isNotEmpty()) {
        PrayerAudioDownloadSheet(
            prayerName = state.chapletDisplayName,
            chapletType = chapletType,
            language = audioLang,
            onDismiss = {
                showDownloadSheet = false
                // Re-check audio state after sheet dismissal (like iOS)
                recheckAudioState()
            },
            onDownloaded = {
                // Re-check triggers LaunchedEffect to load config
                recheckAudioState()
            },
        )
    }
}

// --- Standard Prayer ---

@Composable
private fun PrayerScreen(
    prayer: Prayer?,
    progressContent: @Composable () -> Unit,
    isSimple: Boolean,
    contentVisible: Boolean,
    enterAnim: EnterTransition,
    exitAnim: ExitTransition,
    onTap: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val canScroll = scrollState.canScrollForward || scrollState.canScrollBackward
    Column(
        modifier = Modifier
            .fillMaxSize()
            .tapToAdvance(onTap)
            .statusBarsPadding()
            .padding(top = 68.dp)
            .then(
                if (canScroll) Modifier
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        val fadeHeight = 32.dp.toPx()
                        if (scrollState.canScrollBackward) {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black),
                                    startY = 0f,
                                    endY = fadeHeight,
                                ),
                                blendMode = BlendMode.DstIn,
                            )
                        }
                        if (scrollState.canScrollForward) {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.Black, Color.Transparent),
                                    startY = size.height - fadeHeight,
                                    endY = size.height,
                                ),
                                blendMode = BlendMode.DstIn,
                            )
                        }
                    }
                else Modifier
            )
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Progress panel
        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
            progressContent()
        }

        Spacer(Modifier.height(32.dp))

        AnimatedVisibility(
            visible = contentVisible,
            enter = enterAnim,
            exit = exitAnim,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (prayer != null) {
                    PrayerTextContent(prayer = prayer, isSimple = isSimple)
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.rosary_tap_to_continue),
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f),
                )
            }
        }

        Spacer(Modifier.height(100.dp))
    }
}

// --- Announcement Screen ---

@Composable
private fun AnnouncementScreen(
    title: String?,
    subtitle: String?,
    body: String?,
    fruit: String?,
    scripture: String?,
    number: Int?,
    numberLabel: String?,
    isSimple: Boolean,
    contentVisible: Boolean,
    enterAnim: EnterTransition,
    exitAnim: ExitTransition,
    onTap: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .tapToAdvance(onTap),
    ) {
        if (!isSimple) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.55f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Black.copy(alpha = 0f),
                                0.2f to Color.Black.copy(alpha = 0.25f),
                                0.5f to Color.Black.copy(alpha = 0.5f),
                                1.0f to Color.Black.copy(alpha = 0.7f),
                            )
                        )
                    ),
            )
        }

        AnimatedVisibility(
            visible = contentVisible,
            enter = enterAnim,
            exit = exitAnim,
            modifier = Modifier.align(if (isSimple) Alignment.Center else Alignment.BottomCenter),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 100.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (numberLabel != null) {
                    Text(
                        text = numberLabel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoftGold,
                        letterSpacing = 2.sp,
                    )
                }
                if (title != null) {
                    Text(
                        text = title,
                        fontSize = 28.sp,
                        lineHeight = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SoftGold,
                        textAlign = TextAlign.Center,
                    )
                }
                if (scripture != null) {
                    Text(
                        text = scripture,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                    )
                }
                if (body != null) {
                    Text(
                        text = body,
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
                if (fruit != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(text = "\uD83C\uDF3F", fontSize = 14.sp)
                        Text(
                            text = stringResource(R.string.rosary_fruit, fruit),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.rosary_tap_to_continue),
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

// --- Intro ---

@Composable
private fun IntroContent(
    titleLabel: String,
    isSimple: Boolean,
    onTap: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .tapToAdvance(onTap),
    ) {
        if (!isSimple) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Black.copy(alpha = 0f),
                                0.3f to Color.Black.copy(alpha = 0.3f),
                                0.6f to Color.Black.copy(alpha = 0.6f),
                                1.0f to Color.Black.copy(alpha = 0.85f),
                            )
                        )
                    ),
            )
        }

        Column(
            modifier = Modifier
                .align(if (isSimple) Alignment.Center else Alignment.BottomCenter)
                .padding(horizontal = 32.dp)
                .then(if (!isSimple) Modifier.padding(bottom = 80.dp) else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = titleLabel,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.rosary_tap_to_begin),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.6f),
            )
        }
    }
}

// --- Shared ---

@Composable
internal fun GlassCircleButton(
    onClick: () -> Unit,
    size: Int,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(ButtonGlassBg)
            .border(0.5.dp, ButtonGlassBorder, CircleShape)
            .then(
                if (enabled) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ) else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) { content() }
}

private fun Modifier.tapToAdvance(onTap: () -> Unit) = this.clickable(
    interactionSource = MutableInteractionSource(),
    indication = null,
    onClick = onTap,
)

@Composable
private fun PrayerTextContent(prayer: Prayer, isSimple: Boolean = false) {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .glassBackground(cornerRadius = 20f, isSimple = isSimple)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = prayer.title.uppercase(Locale.getDefault()),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = SoftGold,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            text = prayer.text,
            fontSize = 19.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp,
        )
    }
}

private fun Modifier.glassBackground(cornerRadius: Float = 20f, isSimple: Boolean = false): Modifier {
    val shape = RoundedCornerShape(cornerRadius.dp)
    val bg = if (isSimple) SimpleGlassBg else GlassBg
    val border = if (isSimple) SimpleGlassBorder else GlassBorder
    return this
        .clip(shape)
        .background(bg)
        .border(0.5.dp, border, shape)
}
