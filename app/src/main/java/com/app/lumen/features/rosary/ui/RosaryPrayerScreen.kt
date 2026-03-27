package com.app.lumen.features.rosary.ui

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.app.lumen.features.rosary.model.*
import com.app.lumen.features.rosary.service.RosaryAudioPlayer
import com.app.lumen.features.rosary.service.RosaryAudioService
import com.app.lumen.features.rosary.viewmodel.RosaryViewModel
import com.app.lumen.ui.theme.NearBlack
import com.app.lumen.ui.theme.SoftGold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

// Sacred Art mode: subtle warm glass with gold border
private val GlassBg = Color(0xFF2A2A2E).copy(alpha = 0.86f)
private val GlassBorder = SoftGold.copy(alpha = 0.20f)
// Simple mode: cool dark glass that blends with NearBlack
private val SimpleGlassBg = Color(0xFF1E1E32).copy(alpha = 0.85f)
private val SimpleGlassBorder = Color.White.copy(alpha = 0.10f)
private val ButtonGlassBg = Color.White.copy(alpha = 0.12f)
private val ButtonGlassBorder = Color.White.copy(alpha = 0.20f)

private const val CROSSFADE_MS = 1000

@Composable
fun RosaryPrayerScreen(
    viewModel: RosaryViewModel,
    onBack: () -> Unit,
    onComplete: () -> Unit,
) {
    val currentStepIndex by viewModel.currentStepIndex.collectAsState()

    var contentVisible by remember { mutableStateOf(true) }
    var isTransitioning by remember { mutableStateOf(false) }
    // Track direction for slide: 1 = forward, -1 = backward
    var transitionDirection by remember { mutableIntStateOf(1) }
    // Track whether coming from intro (fade-only, no slide)
    var transitionFromIntro by remember { mutableStateOf(false) }
    // Track whether transition involves a mystery (for softer timing)
    var transitionInvolvesMystery by remember { mutableStateOf(false) }

    val step = viewModel.currentStep
    val prayer = viewModel.currentPrayer
    val mystery = viewModel.currentMystery
    val progress = viewModel.progress
    val mysteryType by viewModel.selectedMysteryType.collectAsState()

    val isIntro = step is RosaryPrayerStep.Intro
    val isMystery = step?.isMysteryAnnouncement == true

    val context = LocalContext.current
    val visualMode = remember { RosaryVisualMode.current(context) }
    val isSimple = visualMode == RosaryVisualMode.SIMPLE

    // Audio state — reactive
    val audioPlayer = remember { RosaryAudioPlayer.getInstance(context) }
    val audioService = remember { RosaryAudioService.getInstance(context) }
    val isAudioDownloaded = remember { audioService.isAudioDownloaded("en") }
    val audioIsPlaying by audioPlayer.isPlaying.collectAsState()
    val audioIsAutoAdvancing by audioPlayer.isAutoAdvancing.collectAsState()

    val rosaryPrefs = remember { context.getSharedPreferences("rosary_prefs", Context.MODE_PRIVATE) }
    var isAudioEnabled by remember { mutableStateOf(rosaryPrefs.getBoolean("audio_enabled", false)) }
    var audioSpeed by remember { mutableFloatStateOf(rosaryPrefs.getFloat("audio_speed", 1.0f)) }
    var isAutoAdvanceEnabled by remember { mutableStateOf(rosaryPrefs.getBoolean("auto_advance", false)) }

    // Audio controls panel state
    var showAudioControls by remember { mutableStateOf(false) }
    // Track if audio was toggled on while panel was open (defer playback until dismiss)
    var audioWasReEnabled by remember { mutableStateOf(false) }

    // Load audio config on appear
    LaunchedEffect(Unit) {
        if (isAudioDownloaded) {
            withContext(Dispatchers.IO) {
                val config = audioService.fetchAudioConfig("en")
                if (config != null) {
                    withContext(Dispatchers.Main) {
                        audioPlayer.configure(config, "en")
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

    val backgroundRes: Int? = if (step != null && mysteryType != null) {
        RosaryBackgroundManager.background(step, mysteryType, visualMode)
    } else if (!isSimple) {
        R.drawable.mystery_joyful
    } else {
        null
    }

    // When the step changes and contentVisible is false, wait one frame then show.
    // This guarantees the new content composes with visible=false before we animate in.
    LaunchedEffect(currentStepIndex) {
        if (!contentVisible && !viewModel.isComplete.value) {
            // Wait one frame so new content composes with visible=false first
            delay(1)
            contentVisible = true
        }
    }

    val scope = rememberCoroutineScope()
    val view = LocalView.current

    fun triggerAudioForCurrentStep() {
        if (isAudioEnabled && isAudioDownloaded) {
            viewModel.currentStep?.let { currentStep ->
                audioPlayer.playAudio(currentStep, mysteryType) {
                    // auto-advance callback — will be called from animateTransition
                }
            }
        }
    }

    fun animateTransition(direction: Int) {
        if (isTransitioning) return
        if (direction == -1 && currentStepIndex == 0) return
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)

        // Stop current audio before transitioning
        audioPlayer.stopCurrentPlayback()

        val nextIsMystery = if (direction == 1) viewModel.peekNextStep()?.isMysteryAnnouncement == true else false
        transitionFromIntro = isIntro
        val isSlowTransition = isIntro || isMystery || nextIsMystery
        transitionInvolvesMystery = isSlowTransition
        transitionDirection = direction
        isTransitioning = true
        contentVisible = false

        val fadeOutMs = if (isSlowTransition) 350L else 150L

        scope.launch {
            delay(fadeOutMs)
            if (direction == 1) viewModel.advanceToNextStep() else viewModel.goToPreviousStep()

            // If rosary is complete, wait for background to crossfade then signal completion
            if (viewModel.isComplete.value) {
                delay(400)
                onComplete()
                return@launch
            }

            // contentVisible stays false here — the LaunchedEffect(currentStepIndex)
            // will set it to true after the new content has composed in the next frame.

            val fadeInMs = if (isSlowTransition) 450L else 200L
            delay(fadeInMs)
            isTransitioning = false

            // Trigger audio for the new step
            if (isAudioEnabled && isAudioDownloaded) {
                viewModel.currentStep?.let { newStep ->
                    audioPlayer.playAudio(newStep, mysteryType) {
                        animateTransition(1) // auto-advance
                    }
                }
            }
        }
    }

    val onTapAdvance: () -> Unit = { animateTransition(1) }
    val onTapBack: () -> Unit = { animateTransition(-1) }

    // Build enter/exit animations based on current transition state
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
        // Background: sacred art image with crossfade, or plain dark
        if (backgroundRes != null) {
            Crossfade(
                targetState = backgroundRes,
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
        if (isIntro) {
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(tween(450, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(350, easing = FastOutSlowInEasing)),
            ) {
                IntroContent(mysteryType = mysteryType, isSimple = isSimple, onTap = onTapAdvance)
            }
        } else if (isMystery) {
            MysteryAnnouncementScreen(
                mystery = mystery,
                decade = step?.getDecade() ?: 1,
                isSimple = isSimple,
                contentVisible = contentVisible,
                enterAnim = enterAnim,
                exitAnim = exitAnim,
                onTap = onTapAdvance,
            )
        } else {
            PrayerScreen(
                prayer = prayer,
                progress = progress,
                isSimple = isSimple,
                contentVisible = contentVisible,
                enterAnim = enterAnim,
                exitAnim = exitAnim,
                onTap = onTapAdvance,
            )
        }

        // Top bar — always show back button + audio button (including on intro)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassCircleButton(onClick = onBack, size = 40) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.rosary_close),
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            if (!isIntro) {
                Text(
                    text = mysteryType?.let { stringResource(it.labelRes) } ?: "",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            if (isAudioDownloaded) {
                GlassCircleButton(onClick = { showAudioControls = true }, size = 40) {
                    AudioBarsView(
                        isAnimating = isAudioEnabled && (audioIsPlaying || audioIsAutoAdvancing),
                        color = if (isAudioEnabled) Color.White else Color.White.copy(alpha = 0.4f),
                    )
                }
            } else {
                Spacer(Modifier.size(40.dp))
            }
        }

        // Bottom chevrons
        if (!isIntro) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp, start = 24.dp, end = 24.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                GlassCircleButton(onClick = onTapBack, size = 46, enabled = currentStepIndex > 0) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.rosary_previous),
                        tint = if (currentStepIndex > 0) Color.White else Color.White.copy(alpha = 0.3f),
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
                // Start playing only after menu is dismissed, if audio was toggled on
                if (audioWasReEnabled && isAudioEnabled && !audioIsPlaying) {
                    viewModel.currentStep?.let { currentStep ->
                        audioPlayer.playAudio(currentStep, mysteryType) {
                            animateTransition(1)
                        }
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
                    // Track that audio was toggled on — playback deferred until dismiss
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

// --- Standard Prayer ---

@Composable
private fun PrayerScreen(
    prayer: Prayer?,
    progress: RosaryProgress?,
    isSimple: Boolean,
    contentVisible: Boolean,
    enterAnim: EnterTransition,
    exitAnim: ExitTransition,
    onTap: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .tapToAdvance(onTap)
            .statusBarsPadding()
            .padding(top = 68.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Progress panel — scrolls with content
        RosaryProgressPanel(
            progress = progress,
            isSimple = isSimple,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(Modifier.height(32.dp))

        // Prayer card — fades + slides in/out
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

// --- Mystery Announcement ---

@Composable
private fun MysteryAnnouncementScreen(
    mystery: Mystery?,
    decade: Int,
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
        // Gradient overlay only needed for Sacred Art mode
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
            if (mystery != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 100.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val ordinalSuffix = when (decade) {
                        1 -> stringResource(R.string.rosary_ordinal_1)
                        2 -> stringResource(R.string.rosary_ordinal_2)
                        3 -> stringResource(R.string.rosary_ordinal_3)
                        else -> stringResource(R.string.rosary_ordinal_other)
                    }
                    Text(
                        text = stringResource(R.string.rosary_mystery_number, decade, ordinalSuffix),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoftGold,
                        letterSpacing = 2.sp,
                    )
                    Text(
                        text = mystery.name,
                        fontSize = 28.sp,
                        lineHeight = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = mystery.meditation,
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(text = "\uD83C\uDF3F", fontSize = 14.sp)
                        Text(
                            text = stringResource(R.string.rosary_fruit, mystery.fruit),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.8f),
                        )
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
}

// --- Intro ---

@Composable
private fun IntroContent(
    mysteryType: MysteryType?,
    isSimple: Boolean,
    onTap: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .tapToAdvance(onTap),
    ) {
        // Gradient overlay only for Sacred Art
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
                text = stringResource(R.string.rosary_the_holy_rosary),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = SoftGold,
                letterSpacing = 1.5.sp,
            )
            if (mysteryType != null) {
                Text(
                    text = stringResource(mysteryType.labelRes),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
            }
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
private fun GlassCircleButton(
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
