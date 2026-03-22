package com.app.lumen.features.liturgy.ui

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.lumen.features.audio.AudioPlayerManager
import com.app.lumen.features.audio.ReadingType
import com.app.lumen.features.liturgy.model.DailyLiturgy
import com.app.lumen.features.liturgy.model.DailyVerse
import com.app.lumen.ui.theme.NearBlack
import com.app.lumen.ui.theme.Slate
import com.app.lumen.ui.theme.SoftGold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ReadingSection(
    val title: String,
    val shortTitle: String,
    val icon: ImageVector,
) {
    SAINT("Saint of the Day", "Saint", Icons.Filled.Person),
    FIRST_READING("First Reading", "1st", Icons.Filled.LooksOne),
    PSALM("Responsorial Psalm", "Psalm", Icons.Filled.MusicNote),
    SECOND_READING("Second Reading", "2nd", Icons.Filled.LooksTwo),
    GOSPEL("Gospel", "Gospel", Icons.Filled.AutoStories),
    REFLECTION("Reflection", "Reflect", Icons.Filled.FormatListBulleted),
}

enum class ReadingFontSize(val label: String, val bodySize: Int, val lineHeight: Int) {
    SMALL("Small", 15, 24),
    MEDIUM("Medium", 17, 28),
    LARGE("Large", 20, 32),
    EXTRA_LARGE("Extra Large", 24, 38),
}

private fun getSavedFontSize(context: Context): ReadingFontSize {
    val prefs = context.getSharedPreferences("reading_prefs", Context.MODE_PRIVATE)
    val name = prefs.getString("font_size", ReadingFontSize.MEDIUM.name) ?: ReadingFontSize.MEDIUM.name
    return try { ReadingFontSize.valueOf(name) } catch (_: Exception) { ReadingFontSize.MEDIUM }
}

private fun saveFontSize(context: Context, size: ReadingFontSize) {
    context.getSharedPreferences("reading_prefs", Context.MODE_PRIVATE)
        .edit().putString("font_size", size.name).apply()
}

private val ChipBg = Color.White.copy(alpha = 0.12f)
private val ChipBorder = Color.White.copy(alpha = 0.15f)
private val ChipSelectedBg = Color.White.copy(alpha = 0.22f)
private val ChipSelectedBorder = Color.White.copy(alpha = 0.25f)
private val DividerColor = Color.White.copy(alpha = 0.15f)
private val ReferenceBlue = Color(0xFF5BA8D9)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ReadingsScreen(
    liturgy: DailyLiturgy,
    verse: DailyVerse?,
    initialSection: ReadingSection,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val audioPlayer = remember { AudioPlayerManager.getInstance(context) }
    val isPlaying by audioPlayer.isPlaying.collectAsState()
    val currentReading by audioPlayer.currentReadingType.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(isPlaying) {
        while (isPlaying) { audioPlayer.updateProgress(); delay(500) }
    }

    // Font size
    var fontSize by remember { mutableStateOf(getSavedFontSize(context)) }
    var showFontMenu by remember { mutableStateOf(false) }

    // Build available sections based on data
    val sections = remember(liturgy) {
        buildList {
            if (liturgy.saintOfDay != null) add(ReadingSection.SAINT)
            add(ReadingSection.FIRST_READING)
            add(ReadingSection.PSALM)
            if (liturgy.readings.secondReading != null) add(ReadingSection.SECOND_READING)
            add(ReadingSection.GOSPEL)
            if (liturgy.sermon != null) add(ReadingSection.REFLECTION)
        }
    }

    val initialPage = sections.indexOf(initialSection).coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage) { sections.size }
    val chipListState = rememberLazyListState()

    // Sync chip scroll when pager changes
    LaunchedEffect(pagerState.currentPage) {
        chipListState.animateScrollToItem(
            index = pagerState.currentPage,
            scrollOffset = -200,
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NearBlack)
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                )
            }
            Text(
                text = liturgy.celebration,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 56.dp),
            )

            // Font size button
            IconButton(
                onClick = { showFontMenu = !showFontMenu },
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Icon(
                    imageVector = Icons.Filled.FormatSize,
                    contentDescription = "Font size",
                    tint = Color.White,
                )
            }
        }

        // Chip selector
        LazyRow(
            state = chipListState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(sections) { index, section ->
                val isSelected = pagerState.currentPage == index
                SectionChip(
                    section = section,
                    isSelected = isSelected,
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(index) }
                    },
                )
            }
        }

        // Pager content + audio player overlay
        Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val section = sections[page]
            val bottomPad = if (currentReading != null) 80.dp else 40.dp
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp, bottom = bottomPad)
                    .navigationBarsPadding(),
            ) {
                when (section) {
                    ReadingSection.SAINT -> SaintContent(liturgy, fontSize)
                    ReadingSection.FIRST_READING -> ReadingContent(
                        icon = ReadingSection.FIRST_READING.icon,
                        title = "First Reading",
                        reference = liturgy.readings.firstReading.reference,
                        text = liturgy.readings.firstReading.text,
                        audioUrl = liturgy.audioUrls?.firstReading,
                        readingType = ReadingType.FIRST_READING,
                        isPlaying = isPlaying && currentReading == ReadingType.FIRST_READING,
                        audioPlayer = audioPlayer,
                        fontSize = fontSize,
                    )
                    ReadingSection.PSALM -> PsalmContent(
                        liturgy = liturgy,
                        isPlaying = isPlaying && currentReading == ReadingType.PSALM,
                        audioPlayer = audioPlayer,
                        fontSize = fontSize,
                    )
                    ReadingSection.SECOND_READING -> ReadingContent(
                        icon = ReadingSection.SECOND_READING.icon,
                        title = "Second Reading",
                        reference = liturgy.readings.secondReading?.reference ?: "",
                        text = liturgy.readings.secondReading?.text ?: "",
                        audioUrl = liturgy.audioUrls?.secondReading,
                        readingType = ReadingType.SECOND_READING,
                        isPlaying = isPlaying && currentReading == ReadingType.SECOND_READING,
                        audioPlayer = audioPlayer,
                        fontSize = fontSize,
                    )
                    ReadingSection.GOSPEL -> ReadingContent(
                        icon = ReadingSection.GOSPEL.icon,
                        title = "Gospel",
                        reference = liturgy.readings.gospel.reference,
                        text = liturgy.readings.gospel.text,
                        prominent = true,
                        audioUrl = liturgy.audioUrls?.gospel,
                        readingType = ReadingType.GOSPEL,
                        isPlaying = isPlaying && currentReading == ReadingType.GOSPEL,
                        audioPlayer = audioPlayer,
                        fontSize = fontSize,
                    )
                    ReadingSection.REFLECTION -> ReflectionContent(liturgy, fontSize)
                }
            }
        }

            // Audio player bar (no tabs, just the player)
            if (currentReading != null) {
                val currentPosition by audioPlayer.currentPosition.collectAsState()
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF23233D).copy(alpha = 0.97f))
                        .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(50))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = when (currentReading) {
                            ReadingType.FIRST_READING -> Icons.Filled.LooksOne
                            ReadingType.PSALM -> Icons.Filled.MusicNote
                            ReadingType.SECOND_READING -> Icons.Filled.LooksTwo
                            ReadingType.GOSPEL -> Icons.Filled.AutoStories
                            else -> Icons.Filled.PlayArrow
                        },
                        contentDescription = null,
                        tint = SoftGold,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = currentReading!!.displayName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = formatTime(currentPosition),
                        fontSize = 12.sp,
                        color = Slate,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = { audioPlayer.togglePlayPause() },
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = SoftGold,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    IconButton(
                        onClick = { audioPlayer.stop() },
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                            tint = Slate,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        } // end Box (pager + player)
    } // end Column

    // Font size panel overlay (on top of everything)
    androidx.compose.animation.AnimatedVisibility(
        visible = showFontMenu,
        enter = androidx.compose.animation.fadeIn(tween(150)) +
                androidx.compose.animation.scaleIn(
                    initialScale = 0.85f,
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.9f, 0f),
                    animationSpec = tween(150),
                ),
        exit = androidx.compose.animation.fadeOut(tween(100)) +
                androidx.compose.animation.scaleOut(
                    targetScale = 0.85f,
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.9f, 0f),
                    animationSpec = tween(100),
                ),
        modifier = Modifier.fillMaxSize(),
    ) {
        // Invisible dismiss layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { showFontMenu = false },
                ),
        ) {
            // Glass panel anchored below the toolbar button
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 52.dp, end = 8.dp)
                    .width(200.dp)
                    .shadow(16.dp, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF23233D))
                    .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
            ) {
                ReadingFontSize.entries.forEachIndexed { index, size ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                fontSize = size
                                saveFontSize(context, size)
                                showFontMenu = false
                            }
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "A",
                            fontSize = (13 + size.ordinal * 3).sp,
                            color = if (fontSize == size) SoftGold else Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.width(28.dp),
                        )
                        Text(
                            text = size.label,
                            fontSize = 15.sp,
                            color = if (fontSize == size) SoftGold else Color.White,
                            fontWeight = if (fontSize == size) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                    if (index < ReadingFontSize.entries.size - 1) {
                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.08f),
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }
        }
    }
    } // end outer Box
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
private fun SectionChip(
    section: ReadingSection,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) ChipSelectedBg else ChipBg,
        animationSpec = tween(200),
        label = "chip_bg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) ChipSelectedBorder else ChipBorder,
        animationSpec = tween(200),
        label = "chip_border",
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
        animationSpec = tween(200),
        label = "chip_text",
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .border(0.5.dp, borderColor, RoundedCornerShape(50))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = section.icon,
            contentDescription = null,
            tint = if (isSelected && section == ReadingSection.GOSPEL) SoftGold else textColor,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = section.shortTitle,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected && section == ReadingSection.GOSPEL) SoftGold else textColor,
        )
    }
}

@Composable
private fun SaintContent(liturgy: DailyLiturgy, fontSize: ReadingFontSize) {
    val saint = liturgy.saintOfDay ?: return

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = null,
            tint = SoftGold,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Saint of the Day",
            fontSize = 15.sp,
            color = Slate,
        )
    }

    Spacer(Modifier.height(12.dp))

    Text(
        text = saint.name,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
    )

    Spacer(Modifier.height(16.dp))
    HorizontalDivider(color = DividerColor)
    Spacer(Modifier.height(16.dp))

    Text(
        text = saint.description,
        fontSize = fontSize.bodySize.sp,
        color = Color.White.copy(alpha = 0.85f),
        lineHeight = fontSize.lineHeight.sp,
    )
}

@Composable
private fun ReadingContent(
    icon: ImageVector,
    title: String,
    reference: String,
    text: String,
    prominent: Boolean = false,
    audioUrl: String?,
    readingType: ReadingType,
    isPlaying: Boolean,
    audioPlayer: AudioPlayerManager,
    fontSize: ReadingFontSize = ReadingFontSize.MEDIUM,
) {
    // Header
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (prominent) SoftGold else Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            color = Slate,
        )
        Spacer(Modifier.weight(1f))
        if (audioUrl != null) {
            ListenButton(
                isPlaying = isPlaying,
                prominent = prominent,
                onClick = {
                    if (isPlaying) {
                        audioPlayer.togglePlayPause()
                    } else {
                        audioPlayer.play(audioUrl, readingType, title)
                    }
                },
            )
        }
    }

    Spacer(Modifier.height(12.dp))

    Text(
        text = reference,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (prominent) SoftGold else ReferenceBlue,
    )

    Spacer(Modifier.height(12.dp))
    HorizontalDivider(color = DividerColor)
    Spacer(Modifier.height(16.dp))

    Text(
        text = text,
        fontSize = fontSize.bodySize.sp,
        color = Color.White.copy(alpha = 0.85f),
        lineHeight = fontSize.lineHeight.sp,
    )
}

@Composable
private fun PsalmContent(
    liturgy: DailyLiturgy,
    isPlaying: Boolean,
    audioPlayer: AudioPlayerManager,
    fontSize: ReadingFontSize = ReadingFontSize.MEDIUM,
) {
    val psalm = liturgy.readings.psalm

    // Header
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Responsorial Psalm",
            fontSize = 15.sp,
            color = Slate,
        )
        Spacer(Modifier.weight(1f))
        if (liturgy.audioUrls?.psalm != null) {
            ListenButton(
                isPlaying = isPlaying,
                prominent = false,
                onClick = {
                    if (isPlaying) {
                        audioPlayer.togglePlayPause()
                    } else {
                        audioPlayer.play(liturgy.audioUrls.psalm!!, ReadingType.PSALM, "Psalm")
                    }
                },
            )
        }
    }

    Spacer(Modifier.height(12.dp))

    Text(
        text = psalm.reference,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = ReferenceBlue,
    )

    Spacer(Modifier.height(12.dp))
    HorizontalDivider(color = DividerColor)
    Spacer(Modifier.height(16.dp))

    // Response highlight box
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Response",
            fontSize = 12.sp,
            color = Slate,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = psalm.response,
            fontSize = 17.sp,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
        )
    }

    Spacer(Modifier.height(16.dp))

    Text(
        text = psalm.text,
        fontSize = fontSize.bodySize.sp,
        color = Color.White.copy(alpha = 0.85f),
        lineHeight = fontSize.lineHeight.sp,
    )
}

@Composable
private fun ReflectionContent(liturgy: DailyLiturgy, fontSize: ReadingFontSize = ReadingFontSize.MEDIUM) {
    val sermon = liturgy.sermon ?: return

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.FormatListBulleted,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Reflection",
            fontSize = 15.sp,
            color = Slate,
        )
    }

    Spacer(Modifier.height(16.dp))
    HorizontalDivider(color = DividerColor)
    Spacer(Modifier.height(16.dp))

    Text(
        text = sermon,
        fontSize = fontSize.bodySize.sp,
        color = Color.White.copy(alpha = 0.85f),
        lineHeight = fontSize.lineHeight.sp,
    )
}

@Composable
private fun ListenButton(
    isPlaying: Boolean,
    prominent: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (prominent) SoftGold else Color.White.copy(alpha = 0.7f)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.08f))
            .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (isPlaying) "Pause" else "Listen",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = tint,
        )
    }
}
