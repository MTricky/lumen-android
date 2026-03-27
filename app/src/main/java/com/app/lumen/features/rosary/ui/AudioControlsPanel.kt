package com.app.lumen.features.rosary.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.lumen.R
import com.app.lumen.ui.theme.SoftGold

private val PanelDivider = Color.White.copy(alpha = 0.08f)
private val DisabledColor = Color.White.copy(alpha = 0.25f)

/**
 * Glass dropdown panel for audio controls, matching iOS AudioControlsPanel.
 * Shows: Audio toggle, Playback Speed slider, Auto-advance toggle.
 * No scrim — tapping outside dismisses via a transparent full-screen touch catcher.
 */
@Composable
fun AudioControlsPanel(
    isPresented: Boolean,
    onDismiss: () -> Unit,
    isAudioEnabled: Boolean,
    onAudioEnabledChange: (Boolean) -> Unit,
    audioSpeed: Float,
    onAudioSpeedChange: (Float) -> Unit,
    isAutoAdvanceEnabled: Boolean,
    onAutoAdvanceChange: (Boolean) -> Unit,
    isSimple: Boolean = false,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Subtle scrim — fades independently with softer timing
        AnimatedVisibility(
            visible = isPresented,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )
        }

        // Panel — scales in from top-trailing
        AnimatedVisibility(
            visible = isPresented,
            enter = fadeIn(tween(200)) + scaleIn(
                initialScale = 0.4f,
                transformOrigin = TransformOrigin(0.9f, 0f),
                animationSpec = tween(200),
            ),
            exit = fadeOut(tween(100)) + scaleOut(
                targetScale = 0.4f,
                transformOrigin = TransformOrigin(0.9f, 0f),
                animationSpec = tween(100),
            ),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val panelBg = if (isSimple) Color(0xFF1E1E32).copy(alpha = 0.93f)
                    else Color(0xFF2A2A2E).copy(alpha = 0.94f)
                val panelBorder = if (isSimple) Color.White.copy(alpha = 0.10f)
                    else SoftGold.copy(alpha = 0.20f)

                Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 56.dp, end = 16.dp)
                    .width(260.dp)
                    .shadow(16.dp, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .background(panelBg)
                    .border(0.5.dp, panelBorder, RoundedCornerShape(14.dp)),
            ) {
                // Audio toggle row
                AudioToggleRow(
                    isEnabled = isAudioEnabled,
                    onToggle = onAudioEnabledChange,
                )

                HorizontalDivider(thickness = 0.5.dp, color = PanelDivider)

                // Speed slider row
                SpeedRow(
                    speed = audioSpeed,
                    onSpeedChange = onAudioSpeedChange,
                    enabled = isAudioEnabled,
                )

                HorizontalDivider(thickness = 0.5.dp, color = PanelDivider)

                // Auto-advance toggle row
                AutoAdvanceRow(
                    isEnabled = isAutoAdvanceEnabled,
                    onToggle = onAutoAdvanceChange,
                    audioEnabled = isAudioEnabled,
                )
            }
            }
        }
    }
}

@Composable
private fun AudioToggleRow(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
            contentDescription = null,
            tint = SoftGold,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.rosary_audio_label),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SoftGold,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.White.copy(alpha = 0.15f),
                uncheckedBorderColor = Color.Transparent,
            ),
            modifier = Modifier.height(24.dp),
        )
    }
}

@Composable
private fun SpeedRow(
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    enabled: Boolean,
) {
    val tintColor = if (enabled) SoftGold else DisabledColor
    val textColor = if (enabled) Color.White else DisabledColor

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Speed,
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.rosary_audio_playback_speed),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = String.format("%.1f×", speed),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = tintColor,
            )
        }
        Spacer(Modifier.height(8.dp))
        Slider(
            value = speed,
            onValueChange = onSpeedChange,
            valueRange = 1.0f..2.0f,
            steps = 9,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = SoftGold,
                activeTickColor = SoftGold,
                inactiveTrackColor = Color.White.copy(alpha = 0.15f),
                inactiveTickColor = Color.White.copy(alpha = 0.3f),
                disabledThumbColor = DisabledColor,
                disabledActiveTrackColor = DisabledColor,
                disabledActiveTickColor = DisabledColor,
                disabledInactiveTrackColor = Color.White.copy(alpha = 0.08f),
                disabledInactiveTickColor = Color.White.copy(alpha = 0.1f),
            ),
        )
    }
}

@Composable
private fun AutoAdvanceRow(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    audioEnabled: Boolean,
) {
    val tintColor = if (audioEnabled) SoftGold else DisabledColor
    val textColor = if (audioEnabled) Color.White else DisabledColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.SkipNext,
            contentDescription = null,
            tint = tintColor,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.rosary_audio_auto_advance),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            enabled = audioEnabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SoftGold,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.White.copy(alpha = 0.15f),
                uncheckedBorderColor = Color.Transparent,
                disabledCheckedThumbColor = DisabledColor,
                disabledCheckedTrackColor = DisabledColor,
                disabledUncheckedThumbColor = DisabledColor,
                disabledUncheckedTrackColor = Color.White.copy(alpha = 0.08f),
            ),
            modifier = Modifier.height(24.dp),
        )
    }
}
