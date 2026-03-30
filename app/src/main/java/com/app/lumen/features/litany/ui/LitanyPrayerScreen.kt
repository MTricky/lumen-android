package com.app.lumen.features.litany.ui

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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.lumen.R
import com.app.lumen.features.chaplets.model.Prayer
import com.app.lumen.features.chaplets.service.ChapletAudioPlayer
import com.app.lumen.features.chaplets.ui.GlassCircleButton
import com.app.lumen.features.chaplets.ui.PrayerAudioDownloadSheet
import com.app.lumen.features.litany.model.LitanyInvocation
import com.app.lumen.features.litany.model.LitanyPrayerStep
import com.app.lumen.features.litany.model.LitanyType
import com.app.lumen.features.litany.viewmodel.LitanyViewModel
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

// Sacred Art mode
private val GlassBg = Color(0xFF2A2A2E).copy(alpha = 0.86f)
private val GlassBorder = SoftGold.copy(alpha = 0.20f)
// Simple mode
private val SimpleGlassBg = Color(0xFF1E1E32).copy(alpha = 0.85f)
private val SimpleGlassBorder = Color.White.copy(alpha = 0.10f)

@Composable
fun LitanyPrayerScreen(
    viewModel: LitanyViewModel,
    litanyType: LitanyType,
    onBack: () -> Unit,
    onComplete: () -> Unit,
) {
    val currentStepIndex by viewModel.currentStepIndex.collectAsState()
    val isComplete by viewModel.isComplete.collectAsState()

    val step = viewModel.currentStep
    val prayer = viewModel.currentPrayer
    val invocation = viewModel.currentInvocation
    val responseText = viewModel.currentResponseText
    val progress = viewModel.progress
    val prayerData = viewModel.prayerData.collectAsState().value

    val context = LocalContext.current
    val visualMode = remember { RosaryVisualMode.current(context) }
    val isSimple = visualMode == RosaryVisualMode.SIMPLE

    val audioPlayer = remember { ChapletAudioPlayer.getInstance(context) }
    val audioService = remember { RosaryAudioService.getInstance(context) }
    val rosaryPrefs = remember { context.getSharedPreferences("rosary_prefs", Context.MODE_PRIVATE) }

    val chapletType = litanyType.audioChapletType
    val audioLang = remember { LitanyType.prayerLanguageCode() }

    // Audio state
    var isChapletAudioDownloaded by remember { mutableStateOf(audioService.isChapletAudioDownloaded(audioLang, chapletType)) }
    var isRosaryAudioDownloaded by remember { mutableStateOf(audioService.isAudioDownloaded(audioLang)) }
    val isAudioFullyDownloaded = isChapletAudioDownloaded && isRosaryAudioDownloaded
    var hasLitanyAudioAvailable by remember { mutableStateOf(false) }
    val showAudioButton = isAudioFullyDownloaded || (hasLitanyAudioAvailable && isRosaryAudioDownloaded)

    val audioIsPlaying by audioPlayer.isPlaying.collectAsState()
    val audioIsAutoAdvancing by audioPlayer.isAutoAdvancing.collectAsState()

    var isAudioEnabled by remember { mutableStateOf(rosaryPrefs.getBoolean("audio_enabled", false)) }
    var audioSpeed by remember { mutableFloatStateOf(rosaryPrefs.getFloat("audio_speed", 1.0f)) }
    var isAutoAdvanceEnabled by remember { mutableStateOf(rosaryPrefs.getBoolean("auto_advance", false)) }
    var responseDelay by remember { mutableFloatStateOf(rosaryPrefs.getFloat("litany_response_delay", 2.5f)) }

    var showAudioControls by remember { mutableStateOf(false) }
    var audioWasReEnabled by remember { mutableStateOf(false) }
    var showDownloadSheet by remember { mutableStateOf(false) }

    var contentVisible by remember { mutableStateOf(true) }
    var isTransitioning by remember { mutableStateOf(false) }
    var transitionDirection by remember { mutableIntStateOf(1) }
    var transitionFromIntro by remember { mutableStateOf(false) }
    var transitionIsSlowType by remember { mutableStateOf(false) }

    val isIntro = step is LitanyPrayerStep.Intro

    fun recheckAudioState() {
        isRosaryAudioDownloaded = audioService.isAudioDownloaded(audioLang)
        isChapletAudioDownloaded = audioService.isChapletAudioDownloaded(audioLang, chapletType)
    }

    fun playAudioForCurrentStep() {
        if (!isAudioEnabled || !isAudioFullyDownloaded) return
        val currentStep = viewModel.currentStep ?: return
        audioPlayer.playLitanyAudio(currentStep) {
            // auto-advance callback — will be set below
        }
    }

    LaunchedEffect(chapletType) {
        withContext(Dispatchers.IO) {
            val config = audioService.fetchChapletAudioConfig(audioLang, chapletType)
            if (config != null) {
                hasLitanyAudioAvailable = true
            }
        }

        val sheetShown = rosaryPrefs.getBoolean("chaplet_audio_offer_shown_${chapletType}_${audioLang}", false)
        if (!sheetShown && !isChapletAudioDownloaded && isRosaryAudioDownloaded) {
            delay(400)
            showDownloadSheet = true
        }
    }

    LaunchedEffect(isAudioFullyDownloaded) {
        if (isAudioFullyDownloaded) {
            withContext(Dispatchers.IO) {
                val config = audioService.fetchChapletAudioConfig(audioLang, chapletType)
                val rosaryConfig = audioService.fetchAudioConfig(audioLang)
                if (config != null) {
                    withContext(Dispatchers.Main) {
                        audioPlayer.configure(config, rosaryConfig, audioLang, chapletType)
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { audioPlayer.teardown() }
    }

    LaunchedEffect(currentStepIndex) {
        if (!contentVisible && !viewModel.isComplete.value) {
            delay(1)
            contentVisible = true
        }
    }

    val scope = rememberCoroutineScope()
    val view = LocalView.current

    fun animateTransition(direction: Int) {
        if (isTransitioning) return
        if (direction == -1 && currentStepIndex == 0) return
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        audioPlayer.stopCurrentPlayback()

        transitionFromIntro = isIntro
        val isSlowTransition = isIntro
        transitionIsSlowType = isSlowTransition
        transitionDirection = direction
        isTransitioning = true
        contentVisible = false

        val fadeOutMs = if (isSlowTransition) 350L else 150L

        scope.launch {
            delay(fadeOutMs)
            if (direction == 1) viewModel.advanceToNextStep() else viewModel.goToPreviousStep()

            if (viewModel.isComplete.value) {
                delay(400)
                onComplete()
                return@launch
            }

            val fadeInMs = if (isSlowTransition) 450L else 200L
            delay(fadeInMs)
            contentVisible = true
            isTransitioning = false

            if (isAudioEnabled && isAudioFullyDownloaded) {
                val currentStep = viewModel.currentStep ?: return@launch
                audioPlayer.playLitanyAudio(currentStep) {
                    animateTransition(1) // auto-advance
                }
            }
        }
    }

    val onTapAdvance: () -> Unit = { animateTransition(1) }
    val onTapBack: () -> Unit = { animateTransition(-1) }

    val slideOffset = if (transitionIsSlowType) 40 else 60
    val fadeInMs = if (transitionIsSlowType) 450 else 200
    val fadeOutMs = if (transitionIsSlowType) 350 else 150

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

    val bg = if (isSimple) SimpleGlassBg else GlassBg
    val border = if (isSimple) SimpleGlassBorder else GlassBorder

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NearBlack),
    ) {
        // Background
        if (!isSimple) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(litanyType.backgroundImageRes),
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

        // Content
        if (isIntro) {
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(tween(450, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(350, easing = FastOutSlowInEasing)),
            ) {
                IntroContent(
                    title = prayerData?.title ?: stringResource(litanyType.titleRes),
                    subtitle = prayerData?.subtitle ?: stringResource(litanyType.subtitleRes),
                    isSimple = isSimple,
                    onTap = onTapAdvance,
                )
            }
        } else {
            // Prayer or Invocation content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onTapAdvance,
                    )
                    .statusBarsPadding()
                    .padding(top = 68.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Progress panel — stays visible during transitions
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    LitanyProgressPanel(progress = progress, isSimple = isSimple)
                }

                Spacer(Modifier.height(32.dp))

                // Only the prayer content animates
                AnimatedVisibility(
                    visible = contentVisible,
                    enter = enterAnim,
                    exit = exitAnim,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (prayer != null) {
                            PrayerContentCard(prayer = prayer, bg = bg, border = border)
                        } else if (invocation != null) {
                            InvocationContentCard(
                                invocation = invocation,
                                responseText = responseText,
                                sectionTitle = viewModel.currentSectionTitle,
                                invocationIndex = viewModel.currentInvocationIndexInSection,
                                invocationCount = viewModel.currentSectionInvocationCount,
                                bg = bg,
                                border = border,
                            )
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

        // Top bar
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
                    text = stringResource(R.string.litany_navigation_title),
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
                            showAudioControls = true
                        } else {
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
                if (audioWasReEnabled && isAudioEnabled && isAudioFullyDownloaded && !audioIsPlaying) {
                    val currentStep = viewModel.currentStep ?: return@AudioControlsPanel
                    audioPlayer.playLitanyAudio(currentStep) {
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
            responseDelay = responseDelay,
            onResponseDelayChange = { delay ->
                responseDelay = delay
                rosaryPrefs.edit().putFloat("litany_response_delay", delay).apply()
            },
        )
    }

    // Download sheet
    if (showDownloadSheet) {
        PrayerAudioDownloadSheet(
            prayerName = prayerData?.title ?: stringResource(litanyType.titleRes),
            chapletType = chapletType,
            language = audioLang,
            onDismiss = {
                showDownloadSheet = false
                recheckAudioState()
            },
            onDownloaded = {
                recheckAudioState()
            },
        )
    }
}

// --- Intro ---

@Composable
private fun IntroContent(
    title: String,
    subtitle: String,
    isSimple: Boolean,
    onTap: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap,
            ),
    ) {
        // Bottom gradient scrim for sacred art mode
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
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(if (isSimple) Alignment.Center else Alignment.BottomCenter)
                .padding(horizontal = 32.dp)
                .then(if (!isSimple) Modifier.navigationBarsPadding().padding(bottom = 80.dp) else Modifier),
        ) {
            Text(
                text = subtitle,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                fontStyle = FontStyle.Italic,
                color = SoftGold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 40.sp,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.litany_tap_to_begin),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.6f),
            )
        }
    }
}

// --- Prayer Card (Sign of Cross / Closing Prayer) ---

@Composable
private fun PrayerContentCard(
    prayer: Prayer,
    bg: Color,
    border: Color,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(0.5.dp, border, RoundedCornerShape(20.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = prayer.title.uppercase(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = SoftGold,
            textAlign = TextAlign.Center,
            letterSpacing = 1.sp,
        )
        Text(
            text = prayer.text,
            fontSize = 20.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            lineHeight = 30.sp,
        )
    }
}

// --- Invocation Card (V. / ℟ pattern) ---

@Composable
private fun InvocationContentCard(
    invocation: LitanyInvocation,
    responseText: String?,
    sectionTitle: String?,
    invocationIndex: Int?,
    invocationCount: Int?,
    bg: Color,
    border: Color,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(0.5.dp, border, RoundedCornerShape(20.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Section counter: "Mother • 3 of 14"
        if (sectionTitle != null && invocationIndex != null && invocationCount != null) {
            Text(
                text = "$sectionTitle • ${invocationIndex + 1} of $invocationCount",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SoftGold.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                letterSpacing = 1.5.sp,
            )
        }

        // Invocation text
        Text(
            text = invocation.text,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            textAlign = TextAlign.Center,
            lineHeight = 32.sp,
        )

        // Response (℟)
        if (responseText != null) {
            // Gold separator line
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(1.dp)
                    .background(SoftGold.copy(alpha = 0.3f)),
            )

            Text(
                text = "℟ $responseText",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                fontStyle = FontStyle.Italic,
                color = SoftGold,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp,
            )
        }
    }
}
