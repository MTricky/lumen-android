package com.app.lumen.features.settings.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    // Bible font size state (synced with BibleReaderScreen prefs)
    val prefs = remember { context.getSharedPreferences(BIBLE_READER_PREFS, Context.MODE_PRIVATE) }
    var bibleFontSize by remember { mutableFloatStateOf(prefs.getFloat(KEY_FONT_SIZE, 20f)) }

    // Calendar region state
    val calendarPrefs = remember { context.getSharedPreferences("calendar_settings", Context.MODE_PRIVATE) }
    var selectedRegion by remember { mutableStateOf(calendarPrefs.getString("region", "Poland") ?: "Poland") }
    var showRegionPicker by remember { mutableStateOf(false) }

    // Rosary visual style state
    val rosaryPrefs = remember { context.getSharedPreferences("rosary_prefs", Context.MODE_PRIVATE) }
    var visualStyle by remember { mutableStateOf(rosaryPrefs.getString("visual_style", "Sacred Art") ?: "Sacred Art") }
    var showVisualStylePicker by remember { mutableStateOf(false) }
    var isAudioEnabled by remember { mutableStateOf(rosaryPrefs.getBoolean("audio_enabled", false)) }

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
            text = "Settings",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
        )

        // ── Premium Section ──────────────────────────────────────
        SectionHeader("Premium")
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
                    .clickable { /* TODO: open paywall */ }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Gold circle with checkmark
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
                        imageVector = Icons.Filled.Verified,
                        contentDescription = null,
                        tint = NearBlack,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Lumen Pro",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                    Text(
                        text = "Renews 20 Mar 2027",
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
        SectionHeader("Calendar")
        SettingsCard {
            SettingsRow(
                icon = Icons.Filled.Language,
                title = "Calendar Region",
                trailing = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showRegionPicker = true },
                    ) {
                        Text(
                            text = selectedRegion,
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
        FooterText("Determines which regional feast days and holy days of obligation are shown.")

        Spacer(Modifier.height(24.dp))

        // ── Bible Section ──────────────────────────────────────
        SectionHeader("Bible")
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
                        text = "Font Size",
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
                    text = "Clear Bible Cache",
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
        FooterText("Cached chapters are automatically removed after 30 days.")

        Spacer(Modifier.height(24.dp))

        // ── Prayer Section ──────────────────────────────────────
        SectionHeader("Prayer")
        SettingsCard {
            // Visual Style
            SettingsRow(
                icon = Icons.Filled.Palette,
                title = "Visual Style",
                trailing = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showVisualStylePicker = true },
                    ) {
                        Text(
                            text = visualStyle,
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
                    text = "Audio",
                    fontSize = 15.sp,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = isAudioEnabled,
                    onCheckedChange = {
                        isAudioEnabled = it
                        rosaryPrefs.edit().putBoolean("audio_enabled", it).apply()
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
        }
        FooterText("Sacred Art shows paintings for each mystery. Simple uses a dark background. Audio plays narrated prayers.")

        Spacer(Modifier.height(24.dp))

        // ── Support Section ──────────────────────────────────────
        SectionHeader("Support")
        SettingsCard {
            // Share the App
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "https://play.google.com/store/apps/details?id=com.app.lumen"
                            )
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, null))
                    }
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
                    text = "Share the App",
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
                    text = "Contact Support",
                    fontSize = 15.sp,
                    color = Color.White,
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Legal Section ──────────────────────────────────────
        SectionHeader("Legal")
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
                    text = "Privacy Policy",
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
                    text = "Terms of Use",
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

    // ── Visual Style Picker Dialog ────────────────────────────────
    if (showVisualStylePicker) {
        VisualStylePickerDialog(
            selectedStyle = visualStyle,
            onStyleSelected = {
                visualStyle = it
                rosaryPrefs.edit().putString("visual_style", it).apply()
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

private val REGIONS = listOf(
    "Universal",
    "Argentina", "Australia", "Austria", "Brazil", "Canada", "Chile",
    "Colombia", "France", "Germany", "Ireland", "Italy", "Mexico",
    "Peru", "Philippines", "Poland", "Portugal", "Spain", "UK", "USA",
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
            Text("Calendar Region", color = Color.White)
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                REGIONS.forEach { region ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onRegionSelected(region) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = region,
                            fontSize = 15.sp,
                            color = if (region == selectedRegion) SoftGold else Color.White,
                            fontWeight = if (region == selectedRegion) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f),
                        )
                        if (region == selectedRegion) {
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
                Text("Cancel", color = SoftGold)
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

private val VISUAL_STYLES = listOf("Sacred Art", "Simple")

@Composable
private fun VisualStylePickerDialog(
    selectedStyle: String,
    onStyleSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Visual Style", color = Color.White)
        },
        text = {
            Column {
                VISUAL_STYLES.forEach { style ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onStyleSelected(style) }
                            .padding(vertical = 14.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = style,
                            fontSize = 15.sp,
                            color = if (style == selectedStyle) SoftGold else Color.White,
                            fontWeight = if (style == selectedStyle) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f),
                        )
                        if (style == selectedStyle) {
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
                Text("Cancel", color = SoftGold)
            }
        },
        containerColor = CardBg,
        titleContentColor = Color.White,
    )
}
