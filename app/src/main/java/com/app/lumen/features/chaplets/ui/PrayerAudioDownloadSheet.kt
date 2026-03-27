package com.app.lumen.features.chaplets.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.lumen.R
import com.app.lumen.features.rosary.service.RosaryAudioService
import com.app.lumen.ui.theme.NearBlack
import com.app.lumen.ui.theme.SoftGold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerAudioDownloadSheet(
    prayerName: String,
    chapletType: String,
    language: String,
    onDismiss: () -> Unit,
    onDownloaded: () -> Unit,
) {
    val context = LocalContext.current
    val audioService = remember { RosaryAudioService.getInstance(context) }
    val rosaryPrefs = remember { context.getSharedPreferences("rosary_prefs", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()

    val downloadProgress by audioService.chapletDownloadProgress.collectAsState()
    val isDownloading by audioService.isChapletDownloading.collectAsState()

    var isAudioEnabled by remember { mutableStateOf(false) }
    var downloadComplete by remember { mutableStateOf(audioService.isChapletAudioDownloaded(language, chapletType)) }

    ModalBottomSheet(
        onDismissRequest = {
            // Mark as shown only on dismiss (matching iOS)
            rosaryPrefs.edit()
                .putBoolean("chaplet_audio_offer_shown_${chapletType}_$language", true)
                .apply()
            onDismiss()
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.Transparent,
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 1.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color(0xFF2A2A2E).copy(alpha = 0.95f))
                .border(
                    width = 0.5.dp,
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                )
                .padding(horizontal = 24.dp)
                .padding(top = 32.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(SoftGold.copy(alpha = 0.3f), SoftGold.copy(alpha = 0.1f)),
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Headphones,
                    contentDescription = null,
                    tint = SoftGold,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(Modifier.height(16.dp))

            // Title
            Text(
                text = stringResource(R.string.prayer_audio_available),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(4.dp))

            // Subtitle (prayer name)
            Text(
                text = prayerName,
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(28.dp))

            // Audio toggle row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Headphones,
                    contentDescription = null,
                    tint = SoftGold,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.prayer_audio_enable),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                    )
                    Text(
                        text = if (downloadComplete) {
                            stringResource(R.string.prayer_audio_ready)
                        } else {
                            stringResource(R.string.prayer_audio_listen)
                        },
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.5f),
                    )
                }
                Switch(
                    checked = isAudioEnabled,
                    onCheckedChange = { enabled ->
                        isAudioEnabled = enabled
                        rosaryPrefs.edit().putBoolean("audio_enabled", enabled).apply()
                        if (enabled && !downloadComplete && !isDownloading) {
                            scope.launch(Dispatchers.IO) {
                                audioService.downloadChapletAudio(language, chapletType)
                                downloadComplete = audioService.isChapletAudioDownloaded(language, chapletType)
                            }
                        }
                    },
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

            // Download progress (visible only when downloading)
            if (isDownloading) {
                Spacer(Modifier.height(16.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.prayer_audio_downloading),
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.6f),
                        )
                        Text(
                            text = "${(downloadProgress * 100).toInt()}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = SoftGold,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.15f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(downloadProgress.toFloat().coerceIn(0f, 1f))
                                .clip(RoundedCornerShape(2.dp))
                                .background(SoftGold),
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Continue button
            Button(
                onClick = {
                    rosaryPrefs.edit()
                        .putBoolean("chaplet_audio_offer_shown_${chapletType}_$language", true)
                        .apply()
                    if (isAudioEnabled && downloadComplete) {
                        onDownloaded()
                    }
                    onDismiss()
                },
                enabled = !isDownloading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SoftGold,
                    contentColor = Color.White,
                    disabledContainerColor = SoftGold.copy(alpha = 0.5f),
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            ) {
                Text(
                    text = stringResource(R.string.prayer_audio_continue),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
            }

            Spacer(Modifier.height(12.dp))

            // Footer note
            Text(
                text = stringResource(R.string.prayer_audio_settings_note),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.navigationBarsPadding().height(8.dp))
        }
    }
}
