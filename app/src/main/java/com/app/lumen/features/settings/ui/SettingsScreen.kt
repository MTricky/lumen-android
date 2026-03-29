package com.app.lumen.features.settings.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.Formatter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.app.lumen.R
import com.app.lumen.features.rosary.service.RosaryAudioService
import com.app.lumen.features.rosary.ui.RosaryVisualMode
import java.util.Locale
import com.app.lumen.features.subscription.PaywallSheet
import com.app.lumen.features.subscription.SubscriptionManager
import com.app.lumen.ui.theme.NearBlack
import com.app.lumen.ui.theme.SoftGold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

// Card styling constants
private val CardBg = Color(0xFF1E1E32)
private val CardBorder = Color.White.copy(alpha = 0.10f)
private val SectionHeaderColor = SoftGold
private val FooterColor = Color.White.copy(alpha = 0.45f)
private val SecondaryTextColor = Color.White.copy(alpha = 0.5f)
private val DividerColor = Color.White.copy(alpha = 0.08f)

// Bible font size prefs (same keys as BibleReaderScreen)
private const val BIBLE_READER_PREFS = "bible_reader_prefs"
private const val KEY_FONT_SIZE = "font_size"

@Composable
fun SettingsScreen(
    bottomPadding: Dp = 100.dp,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showPaywall by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }

    // Bible font size state (synced with BibleReaderScreen prefs)
    val prefs = remember { context.getSharedPreferences(BIBLE_READER_PREFS, Context.MODE_PRIVATE) }
    var bibleFontSize by remember { mutableFloatStateOf(prefs.getFloat(KEY_FONT_SIZE, 20f)) }

    // Calendar region state
    val calendarPrefs = remember { context.getSharedPreferences("calendar_settings", Context.MODE_PRIVATE) }
    var selectedRegion by remember { mutableStateOf(calendarPrefs.getString("region", "Universal") ?: "Universal") }
    var showRegionPicker by remember { mutableStateOf(false) }

    // Rosary visual style state
    val rosaryPrefs = remember { context.getSharedPreferences("rosary_prefs", Context.MODE_PRIVATE) }
    var visualMode by remember { mutableStateOf(RosaryVisualMode.current(context)) }
    var showVisualStylePicker by remember { mutableStateOf(false) }
    var isAudioEnabled by remember { mutableStateOf(rosaryPrefs.getBoolean("audio_enabled", false)) }

    // Audio download state
    val audioService = remember { RosaryAudioService.getInstance(context) }
    val isDownloadingAudio by audioService.isDownloading.collectAsState()
    val audioDownloadProgress by audioService.downloadProgress.collectAsState()
    val audioLang = remember { prayerLanguageCode() }
    var isAudioDownloaded by remember { mutableStateOf(audioService.isAudioDownloaded(audioLang)) }

    // Bible cache size
    var cacheSizeBytes by remember { mutableLongStateOf(0L) }
    var isClearing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Calculate cache size on first composition
    LaunchedEffect(Unit) {
        cacheSizeBytes = withContext(Dispatchers.IO) { calculateBibleCacheSize(context) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NearBlack)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
            .statusBarsPadding()
            .padding(bottom = bottomPadding),
    ) {
        // Title
        Text(
            text = stringResource(R.string.settings_title),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
        )

        // ── Premium Section ──────────────────────────────────────
        val isPremium by SubscriptionManager.hasProAccess.collectAsState()
        val expiryDate by SubscriptionManager.expirationDate.collectAsState()

        SectionHeader(stringResource(R.string.settings_section_premium))
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                SoftGold.copy(alpha = 0.15f),
                                SoftGold.copy(alpha = 0.05f),
                            )
                        )
                    )
                    .clickable { showPaywall = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Icon circle
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(SoftGold, SoftGold.copy(alpha = 0.7f))
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isPremium) Icons.Filled.Verified else Icons.Filled.WorkspacePremium,
                        contentDescription = null,
                        tint = NearBlack,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isPremium) stringResource(R.string.settings_premium_lumen_pro) else stringResource(R.string.settings_premium_go_premium),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                    Text(
                        text = if (isPremium) {
                            expiryDate?.let { stringResource(R.string.settings_premium_renews, SubscriptionManager.formatExpirationDate(it)) }
                                ?: stringResource(R.string.settings_premium_active)
                        } else {
                            stringResource(R.string.settings_premium_unlock)
                        },
                        fontSize = 12.sp,
                        color = SecondaryTextColor,
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = SecondaryTextColor,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Calendar Section ──────────────────────────────────────
        SectionHeader(stringResource(R.string.settings_section_calendar))
        SettingsCard {
            SettingsRow(
                icon = Icons.Filled.Language,
                title = stringResource(R.string.settings_calendar_region),
                trailing = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showRegionPicker = true },
                    ) {
                        val regionLabelRes = REGION_ENTRIES.firstOrNull { it.first == selectedRegion }?.second
                        Text(
                            text = if (regionLabelRes != null) stringResource(regionLabelRes) else selectedRegion,
                            fontSize = 15.sp,
                            color = SoftGold,
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.UnfoldMore,
                            contentDescription = null,
                            tint = SoftGold,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
            )
        }
        FooterText(stringResource(R.string.settings_calendar_region_footer))

        Spacer(Modifier.height(24.dp))

        // ── Bible Section ──────────────────────────────────────
        SectionHeader(stringResource(R.string.settings_section_bible))
        SettingsCard {
            // Font Size
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "A",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoftGold,
                    )
                    Text(
                        text = "A",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoftGold,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.settings_bible_font_size),
                        fontSize = 15.sp,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${bibleFontSize.roundToInt()}",
                        fontSize = 15.sp,
                        color = SecondaryTextColor,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Slider(
                    value = bibleFontSize,
                    onValueChange = {
                        bibleFontSize = it
                        prefs.edit().putFloat(KEY_FONT_SIZE, it).apply()
                    },
                    valueRange = 14f..28f,
                    steps = 6,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = SoftGold,
                        inactiveTrackColor = Color.White.copy(alpha = 0.15f),
                    ),
                )
            }

            Divider()

            // Clear Bible Cache
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isClearing) {
                        scope.launch {
                            isClearing = true
                            withContext(Dispatchers.IO) { clearBibleCache(context) }
                            cacheSizeBytes = 0L
                            isClearing = false
                        }
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isClearing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = SoftGold,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        tint = SoftGold,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.settings_bible_clear_cache),
                    fontSize = 15.sp,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = Formatter.formatShortFileSize(context, cacheSizeBytes),
                    fontSize = 15.sp,
                    color = SoftGold,
                )
            }
        }
        FooterText(stringResource(R.string.settings_bible_cache_footer))

        Spacer(Modifier.height(24.dp))

        // ── Prayer Section ──────────────────────────────────────
        SectionHeader(stringResource(R.string.settings_section_prayer))
        SettingsCard {
            // Visual Style
            SettingsRow(
                icon = Icons.Filled.Palette,
                title = stringResource(R.string.settings_prayer_visual_style),
                trailing = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showVisualStylePicker = true },
                    ) {
                        Text(
                            text = stringResource(
                                when (visualMode) {
                                    RosaryVisualMode.SACRED_ART -> R.string.settings_prayer_visual_sacred_art
                                    RosaryVisualMode.SIMPLE -> R.string.settings_prayer_visual_simple
                                }
                            ),
                            fontSize = 15.sp,
                            color = SoftGold,
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.UnfoldMore,
                            contentDescription = null,
                            tint = SoftGold,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
            )

            Divider()

            // Audio Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Headphones,
                    contentDescription = null,
                    tint = SoftGold,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.rosary_audio_label),
                    fontSize = 15.sp,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = isAudioEnabled,
                    onCheckedChange = { enabled ->
                        isAudioEnabled = enabled
                        rosaryPrefs.edit().putBoolean("audio_enabled", enabled).apply()
                        if (enabled && !isAudioDownloaded) {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    audioService.downloadAudio(audioLang)
                                }
                                isAudioDownloaded = audioService.isAudioDownloaded(audioLang)
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
                )
            }

            // Download progress
            if (isDownloadingAudio) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.rosary_audio_downloading),
                            fontSize = 12.sp,
                            color = SecondaryTextColor,
                        )
                        Text(
                            text = "${(audioDownloadProgress * 100).roundToInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = SoftGold,
                        )
                    }
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
                                .fillMaxWidth(audioDownloadProgress.toFloat().coerceIn(0f, 1f))
                                .clip(RoundedCornerShape(2.dp))
                                .background(SoftGold),
                        )
                    }
                }
            }
        }
        FooterText(stringResource(R.string.settings_prayer_footer))

        Spacer(Modifier.height(24.dp))

        // ── Support Section ──────────────────────────────────────
        SectionHeader(stringResource(R.string.settings_section_support))
        SettingsCard {
            // Share the App
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showShareDialog = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = null,
                    tint = SoftGold,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.settings_support_share),
                    fontSize = 15.sp,
                    color = Color.White,
                )
            }

            Divider()

            // Contact Support
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:support@bettelf.com")
                        }
                        context.startActivity(intent)
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Email,
                    contentDescription = null,
                    tint = SoftGold,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.settings_support_contact),
                    fontSize = 15.sp,
                    color = Color.White,
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Legal Section ──────────────────────────────────────
        SectionHeader(stringResource(R.string.settings_section_legal))
        SettingsCard {
            // Privacy Policy
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maranatha.app/privacy"))
                        context.startActivity(intent)
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.PrivacyTip,
                    contentDescription = null,
                    tint = SoftGold,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.settings_legal_privacy),
                    fontSize = 15.sp,
                    color = Color.White,
                )
            }

            Divider()

            // Terms of Use
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/intl/en_us/about/play-terms/")
                        )
                        context.startActivity(intent)
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Description,
                    contentDescription = null,
                    tint = SoftGold,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.settings_legal_terms),
                    fontSize = 15.sp,
                    color = Color.White,
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }

    // ── Region Picker Dialog ──────────────────────────────────────
    if (showRegionPicker) {
        RegionPickerDialog(
            selectedRegion = selectedRegion,
            onRegionSelected = {
                selectedRegion = it
                calendarPrefs.edit().putString("region", it).apply()
                showRegionPicker = false
            },
            onDismiss = { showRegionPicker = false },
        )
    }

    // ── Paywall Sheet ────────────────────────────────────────────
    if (showPaywall) {
        PaywallSheet(onDismiss = { showPaywall = false })
    }

    // ── Share App Dialog ─────────────────────────────────────────
    if (showShareDialog) {
        ShareAppDialog(
            onPlatformSelected = { url ->
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_TEXT, url)
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, null))
                showShareDialog = false
            },
            onDismiss = { showShareDialog = false },
        )
    }

    // ── Visual Style Picker Dialog ────────────────────────────────
    if (showVisualStylePicker) {
        VisualStylePickerDialog(
            selectedMode = visualMode,
            onModeSelected = {
                visualMode = it
                rosaryPrefs.edit().putString("visual_style", it.key).apply()
                showVisualStylePicker = false
            },
            onDismiss = { showVisualStylePicker = false },
        )
    }
}

// ── Reusable components ──────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = SectionHeaderColor,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
        content = content,
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SoftGold,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(
        thickness = 0.5.dp,
        color = DividerColor,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Composable
private fun FooterText(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = FooterColor,
        lineHeight = 16.sp,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, end = 16.dp),
    )
}

// ── Dialogs ──────────────────────────────────────────────────────

private const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.bajpro.lumen"
private const val APP_STORE_URL = "https://apps.apple.com/us/app/lumen-bible-catholic-rosary/id6756075397"

@Composable
private fun ShareAppDialog(
    onPlatformSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    val animatedAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "dialogAlpha",
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.85f,
        animationSpec = tween(durationMillis = 250),
        label = "dialogScale",
    )

    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(animatedAlpha)
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { onDismiss() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .scale(animatedScale)
                .padding(horizontal = 32.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CardBg)
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                .padding(24.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { /* consume clicks so they don't dismiss */ },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.settings_share_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.settings_share_subtitle),
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )

            Spacer(modifier = Modifier.height(24.dp))

            ShareDialogButton(
                title = stringResource(R.string.settings_share_android),
                iconRes = R.drawable.ic_android,
                iconTint = Color(0xFF3DDC84),
            ) {
                onPlatformSelected(PLAY_STORE_URL)
            }

            Spacer(modifier = Modifier.height(10.dp))

            ShareDialogButton(
                title = stringResource(R.string.settings_share_ios),
                iconRes = R.drawable.ic_apple,
                iconTint = Color.Black,
                iconSize = 24.dp,
            ) {
                onPlatformSelected(APP_STORE_URL)
            }

            Spacer(modifier = Modifier.height(20.dp))

            ShareDialogButton(
                title = stringResource(R.string.cancel),
            ) {
                onDismiss()
            }
        }
    }
}

@Composable
private fun ShareDialogButton(
    title: String,
    iconRes: Int? = null,
    iconTint: Color = Color.White.copy(alpha = 0.8f),
    iconSize: Dp = 20.dp,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(iconSize),
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                fontSize = 17.sp,
                color = Color.White.copy(alpha = 0.8f),
            )
        }
    }
}

private val REGION_ENTRIES = listOf(
    "Universal" to R.string.region_universal,
    "Argentina" to R.string.region_argentina,
    "Australia" to R.string.region_australia,
    "Austria" to R.string.region_austria,
    "Brazil" to R.string.region_brazil,
    "Canada" to R.string.region_canada,
    "Chile" to R.string.region_chile,
    "Colombia" to R.string.region_colombia,
    "France" to R.string.region_france,
    "Germany" to R.string.region_germany,
    "Ireland" to R.string.region_ireland,
    "Italy" to R.string.region_italy,
    "Mexico" to R.string.region_mexico,
    "Peru" to R.string.region_peru,
    "Philippines" to R.string.region_philippines,
    "Poland" to R.string.region_poland,
    "Portugal" to R.string.region_portugal,
    "Spain" to R.string.region_spain,
    "UK" to R.string.region_uk,
    "USA" to R.string.region_usa,
)

@Composable
private fun RegionPickerDialog(
    selectedRegion: String,
    onRegionSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.settings_calendar_region), color = Color.White)
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                REGION_ENTRIES.forEach { (key, labelRes) ->
                    val isSelected = key == selectedRegion
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onRegionSelected(key) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(labelRes),
                            fontSize = 15.sp,
                            color = if (isSelected) SoftGold else Color.White,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f),
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = SoftGold,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = SoftGold)
            }
        },
        containerColor = CardBg,
        titleContentColor = Color.White,
    )
}

// ── Bible cache helpers ──────────────────────────────────────────

private fun calculateBibleCacheSize(context: Context): Long {
    var total = 0L
    val apiCacheDir = File(context.cacheDir, "BibleAPICache")
    val firebaseCacheDir = File(context.cacheDir, "FirebaseBibles")
    apiCacheDir.listFiles()?.forEach { total += it.length() }
    firebaseCacheDir.listFiles()?.forEach { total += it.length() }
    return total
}

private fun clearBibleCache(context: Context) {
    val apiCacheDir = File(context.cacheDir, "BibleAPICache")
    val firebaseCacheDir = File(context.cacheDir, "FirebaseBibles")
    apiCacheDir.deleteRecursively()
    firebaseCacheDir.deleteRecursively()
    apiCacheDir.mkdirs()
    firebaseCacheDir.mkdirs()
}

private fun prayerLanguageCode(): String {
    val lang = Locale.getDefault().language
    return when {
        lang.startsWith("pl") -> "pl"
        lang.startsWith("fr") -> "fr"
        lang.startsWith("es") -> "es"
        lang.startsWith("pt") -> "pt"
        lang.startsWith("it") -> "it"
        else -> "en"
    }
}

@Composable
private fun VisualStylePickerDialog(
    selectedMode: RosaryVisualMode,
    onModeSelected: (RosaryVisualMode) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.settings_prayer_visual_style), color = Color.White)
        },
        text = {
            Column {
                RosaryVisualMode.entries.forEach { mode ->
                    val label = stringResource(
                        when (mode) {
                            RosaryVisualMode.SACRED_ART -> R.string.settings_prayer_visual_sacred_art
                            RosaryVisualMode.SIMPLE -> R.string.settings_prayer_visual_simple
                        }
                    )
                    val isSelected = mode == selectedMode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onModeSelected(mode) }
                            .padding(vertical = 14.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = label,
                            fontSize = 15.sp,
                            color = if (isSelected) SoftGold else Color.White,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f),
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = SoftGold,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = SoftGold)
            }
        },
        containerColor = CardBg,
        titleContentColor = Color.White,
    )
}
