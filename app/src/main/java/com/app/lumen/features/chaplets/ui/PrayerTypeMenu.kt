package com.app.lumen.features.chaplets.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.lumen.R
import com.app.lumen.features.chaplets.model.PrayerType
import com.app.lumen.ui.theme.SoftGold

private val PanelBg = Color(0xFF2A2A2E).copy(alpha = 0.94f)
private val PanelBorder = SoftGold.copy(alpha = 0.20f)
private val PanelDivider = Color.White.copy(alpha = 0.08f)
private val ToolbarGlassBg = Color(0xFF121212).copy(alpha = 0.50f)

/**
 * Just the trigger button — place this in the header.
 */
@Composable
fun PrayerTypeMenuButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(ToolbarGlassBg)
            .border(0.5.dp, Color.White.copy(alpha = 0.25f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_rosary),
            contentDescription = stringResource(R.string.chaplet_prayer_type_menu),
            tint = SoftGold,
            modifier = Modifier.size(23.dp),
        )
    }
}

/**
 * Full-screen overlay panel for prayer type selection.
 * Matches AudioControlsPanel pattern exactly: scrim + scale-in panel.
 * Place this at the screen-level Box, on top of content.
 */
@Composable
fun PrayerTypePanel(
    isPresented: Boolean,
    onDismiss: () -> Unit,
    currentPrayerType: PrayerType,
    onPrayerTypeSelected: (PrayerType) -> Unit,
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
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 56.dp, end = 20.dp)
                        .width(260.dp)
                        .shadow(16.dp, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .background(PanelBg)
                        .border(0.5.dp, PanelBorder, RoundedCornerShape(14.dp)),
                ) {
                    PrayerTypeRow(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_rosary),
                                contentDescription = null,
                                tint = if (currentPrayerType == PrayerType.ROSARY) SoftGold else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        label = stringResource(R.string.prayer_type_rosary),
                        isSelected = currentPrayerType == PrayerType.ROSARY,
                        onClick = {
                            onDismiss()
                            onPrayerTypeSelected(PrayerType.ROSARY)
                        },
                    )

                    HorizontalDivider(thickness = 0.5.dp, color = PanelDivider)

                    PrayerTypeRow(
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = if (currentPrayerType == PrayerType.CHAPLETS) SoftGold else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        label = stringResource(R.string.prayer_type_chaplets),
                        isSelected = currentPrayerType == PrayerType.CHAPLETS,
                        onClick = {
                            onDismiss()
                            onPrayerTypeSelected(PrayerType.CHAPLETS)
                        },
                    )

                    HorizontalDivider(thickness = 0.5.dp, color = PanelDivider)

                    PrayerTypeRow(
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        label = stringResource(R.string.prayer_type_litanies),
                        isSelected = false,
                        enabled = false,
                        onClick = onDismiss,
                    )
                }
            }
        }
    }
}

@Composable
private fun PrayerTypeRow(
    icon: @Composable () -> Unit,
    label: String,
    isSelected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = when {
                !enabled -> Color.White.copy(alpha = 0.4f)
                isSelected -> SoftGold
                else -> Color.White
            },
        )
    }
}
