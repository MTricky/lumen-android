package com.app.lumen.features.onboarding.ui.steps

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flare
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.lumen.R
import com.app.lumen.features.onboarding.OnboardingStep
import com.app.lumen.features.onboarding.OnboardingViewModel
import com.app.lumen.features.onboarding.RosaryVisualMode
import com.app.lumen.features.onboarding.ui.components.OnboardingBottomSpacer
import com.app.lumen.features.onboarding.ui.components.OnboardingGlassCard
import com.app.lumen.features.onboarding.ui.components.OnboardingGlassProminentButton
import com.app.lumen.features.onboarding.ui.components.OnboardingPageContainer
import com.app.lumen.features.onboarding.ui.components.OnboardingTopSpacer
import com.app.lumen.features.rosary.service.RosaryAudioService
import com.app.lumen.ui.HapticManager
import com.app.lumen.ui.theme.SoftGold

private val CardBg = Color(0xFF1A1A29)
private val CardBorder = Color.White.copy(alpha = 0.10f)

@Composable
fun RosaryStep(viewModel: OnboardingViewModel) {
    val view = LocalView.current
    OnboardingPageContainer(
        backgroundRes = OnboardingStep.ROSARY.backgroundRes,
        button = {
            OnboardingGlassProminentButton(title = stringResource(R.string.onboarding_continue)) {
                HapticManager.selection(view)
                viewModel.goToNextStep()
            }
        }
    ) {
        OnboardingTopSpacer()

        // Title Card
        OnboardingGlassCard {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.Flare,
                    contentDescription = null,
                    tint = SoftGold,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.onboarding_rosary_title),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.onboarding_rosary_subtitle),
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Visual Mode Picker
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            VisualModeOption(
                mode = RosaryVisualMode.SACRED_ART,
                icon = Icons.Filled.Image,
                isSelected = viewModel.selectedVisualMode == RosaryVisualMode.SACRED_ART,
                modifier = Modifier.weight(1f)
            ) { HapticManager.selection(view); viewModel.selectVisualMode(RosaryVisualMode.SACRED_ART) }

            VisualModeOption(
                mode = RosaryVisualMode.SIMPLE,
                icon = Icons.Filled.RemoveRedEye,
                isSelected = viewModel.selectedVisualMode == RosaryVisualMode.SIMPLE,
                modifier = Modifier.weight(1f)
            ) { HapticManager.selection(view); viewModel.selectVisualMode(RosaryVisualMode.SIMPLE) }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.onboarding_rosary_description),
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 3.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Audio toggle card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CardBg)
                .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                .padding(14.dp)
                .animateContentSize()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Headphones,
                    contentDescription = null,
                    tint = SoftGold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.onboarding_rosary_enable_audio),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = when {
                            !viewModel.isRosaryAudioEnabled -> stringResource(R.string.onboarding_rosary_audio_guided)
                            viewModel.isAudioDownloaded -> stringResource(R.string.onboarding_rosary_audio_ready)
                            viewModel.isAudioDownloading -> stringResource(R.string.onboarding_rosary_audio_downloading, (viewModel.audioDownloadProgress * 100).toInt())
                            else -> stringResource(R.string.onboarding_rosary_audio_will_download)
                        },
                        fontSize = 11.sp,
                        color = when {
                            !viewModel.isRosaryAudioEnabled -> Color.White.copy(alpha = 0.5f)
                            viewModel.isAudioDownloaded -> Color.Green.copy(alpha = 0.8f)
                            viewModel.isAudioDownloading -> SoftGold
                            else -> Color.White.copy(alpha = 0.5f)
                        }
                    )
                }
                Switch(
                    checked = viewModel.isRosaryAudioEnabled,
                    onCheckedChange = { HapticManager.selection(view); viewModel.toggleRosaryAudio() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = SoftGold,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.7f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.15f)
                    )
                )
            }

            // Download progress bar
            if (viewModel.isRosaryAudioEnabled && viewModel.isAudioDownloading) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = viewModel.audioDownloadProgress.toFloat().coerceIn(0f, 1f))
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(SoftGold)
                    )
                }
            }
        }

        // Audio preview player (shows when downloaded)
        if (viewModel.isRosaryAudioEnabled && viewModel.isAudioDownloaded && !viewModel.isAudioDownloading) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AudioPreviewCard(viewModel)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        OnboardingBottomSpacer()
    }
}

@Composable
private fun AudioPreviewCard(viewModel: OnboardingViewModel) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    Row(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
            .clickable {
                if (isPlaying) {
                    mediaPlayer?.pause()
                    isPlaying = false
                } else {
                    if (mediaPlayer == null) {
                        val audioService = RosaryAudioService.getInstance(context)
                        val language = viewModel.audioLanguageCode
                        val file = audioService.localFile("audio/$language/prayers/hail_mary_1.mp3", language)
                            ?: audioService.localFile("audio/en/prayers/hail_mary_1.mp3", "en")
                        if (file != null) {
                            mediaPlayer = MediaPlayer().apply {
                                setDataSource(file.absolutePath)
                                prepare()
                                setOnCompletionListener {
                                    isPlaying = false
                                }
                                start()
                            }
                            isPlaying = true
                        }
                    } else {
                        mediaPlayer?.start()
                        isPlaying = true
                    }
                }
            }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = SoftGold,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.onboarding_rosary_audio_preview),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}

@Composable
private fun VisualModeOption(
    mode: RosaryVisualMode,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .then(
                if (isSelected)
                    Modifier.border(1.5.dp, SoftGold, RoundedCornerShape(14.dp))
                else
                    Modifier.border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            )
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) SoftGold else Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = mode.displayName,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f)
        )
    }
}
