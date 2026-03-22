package com.app.lumen.ui.tabs

import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.lumen.features.audio.AudioPlayerManager
import com.app.lumen.features.audio.ReadingType
import com.app.lumen.features.liturgy.ui.LiturgyScreen
import com.app.lumen.features.liturgy.ui.ReadingSection
import com.app.lumen.features.liturgy.ui.ReadingsScreen
import com.app.lumen.features.liturgy.model.DailyLiturgy
import com.app.lumen.features.liturgy.model.DailyVerse
import com.app.lumen.ui.theme.NearBlack
import com.app.lumen.ui.theme.Slate
import com.app.lumen.ui.theme.SoftGold
import kotlinx.coroutines.delay
import kotlin.math.abs

enum class Tab(
    val label: String,
    val icon: ImageVector,
) {
    LITURGY("Liturgy", Icons.AutoMirrored.Filled.MenuBook),
    BIBLE("Bible", Icons.Filled.Book),
    PRAYERS("Prayers", Icons.Filled.GridView),
    CALENDAR("Calendar", Icons.Filled.CalendarMonth),
    SETTINGS("Settings", Icons.Filled.Settings),
}

// Styling
private val BarBg = Color(0xFF23233D).copy(alpha = 0.97f)
private val BarBorder = Color.White.copy(alpha = 0.10f)
private val SelectedBg = Color.White.copy(alpha = 0.10f)
private val SelectedBorder = Color.White.copy(alpha = 0.14f)
private val UnselectedTint = Color.White.copy(alpha = 0.85f)

// Scroll state
@Stable
class TabBarScrollState(private val thresholdPx: Float) : NestedScrollConnection {
    var isInline by mutableStateOf(false)
        private set
    private var accumulated = 0f
    private var lastDir = 0

    fun forceExpand() { isInline = false; accumulated = 0f }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val dy = available.y
        if (dy == 0f) return Offset.Zero
        val dir = if (dy < 0) 1 else -1
        if (dir != lastDir) { accumulated = 0f; lastDir = dir }
        accumulated += abs(dy)
        if (accumulated > thresholdPx) { isInline = dir == 1; accumulated = 0f }
        return Offset.Zero
    }
}

@Composable
fun rememberTabBarScrollState(threshold: Dp = 50.dp): TabBarScrollState {
    val px = with(LocalDensity.current) { threshold.toPx() }
    return remember { TabBarScrollState(px) }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainTabView() {
    var selectedTab by remember { mutableStateOf(Tab.LITURGY) }
    val scrollState = rememberTabBarScrollState()
    val context = LocalContext.current
    val audioPlayer = remember { AudioPlayerManager.getInstance(context) }
    val isPlaying by audioPlayer.isPlaying.collectAsState()
    val currentReading by audioPlayer.currentReadingType.collectAsState()
    val currentPosition by audioPlayer.currentPosition.collectAsState()

    LaunchedEffect(isPlaying) {
        while (isPlaying) { audioPlayer.updateProgress(); delay(500) }
    }

    val showAccessory = currentReading != null
    // Only allow inline mode on scrollable screens (Liturgy)
    val isInline = scrollState.isInline && selectedTab == Tab.LITURGY

    // Readings detail navigation state
    var showReadings by remember { mutableStateOf(false) }
    var readingsInitialSection by remember { mutableStateOf(ReadingSection.FIRST_READING) }
    var readingsLiturgy by remember { mutableStateOf<DailyLiturgy?>(null) }
    var readingsVerse by remember { mutableStateOf<DailyVerse?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NearBlack)
            .nestedScroll(scrollState)
    ) {
        when (selectedTab) {
            Tab.LITURGY -> LiturgyScreen(
                bottomPadding = if (showAccessory && !isInline) 140.dp else 100.dp,
                onOpenReadings = { liturgy, verse, section ->
                    readingsLiturgy = liturgy
                    readingsVerse = verse
                    readingsInitialSection = section
                    showReadings = true
                },
            )
            else -> PlaceholderScreen(tab = selectedTab)
        }

        // Tab bar
        SharedTransitionLayout(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp)
                .padding(bottom = 4.dp),
        ) {
            AnimatedContent(
                targetState = isInline,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                label = "tab_bar",
            ) { inline ->
                if (inline) {
                    // --- INLINE BAR ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Selected tab icon only (left) — tap to expand
                        Box(
                            modifier = Modifier
                                .sharedElement(
                                    rememberSharedContentState("tabGroup"),
                                    this@AnimatedContent,
                                )
                                .shadow(6.dp, RoundedCornerShape(50))
                                .clip(RoundedCornerShape(50))
                                .background(BarBg)
                                .border(1.dp, BarBorder, RoundedCornerShape(50))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { scrollState.forceExpand() },
                                )
                                .padding(14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = selectedTab.icon,
                                contentDescription = selectedTab.label,
                                tint = SoftGold,
                                modifier = Modifier.size(24.dp),
                            )
                        }

                        // Inline accessory (middle)
                        if (showAccessory && currentReading != null) {
                            Spacer(Modifier.width(6.dp))
                            InlineAccessory(
                                readingType = currentReading!!,
                                isPlaying = isPlaying,
                                currentPosition = currentPosition,
                                onPlayPause = { audioPlayer.togglePlayPause() },
                                onStop = { audioPlayer.stop() },
                                modifier = Modifier
                                    .weight(1f)
                                    .sharedElement(
                                        rememberSharedContentState("accessory"),
                                        this@AnimatedContent,
                                    )
                                    .shadow(6.dp, RoundedCornerShape(50))
                                    .clip(RoundedCornerShape(50))
                                    .background(BarBg)
                                    .border(1.dp, BarBorder, RoundedCornerShape(50)),
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }

                        Spacer(Modifier.width(6.dp))

                        // Settings circle
                        Box(
                            modifier = Modifier
                                .sharedElement(
                                    rememberSharedContentState("standalone"),
                                    this@AnimatedContent,
                                )
                                .size(50.dp)
                                .shadow(6.dp, CircleShape)
                                .clip(CircleShape)
                                .background(BarBg)
                                .border(1.dp, BarBorder, CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { selectedTab = Tab.SETTINGS },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Settings",
                                tint = animateColorAsState(
                                    targetValue = if (selectedTab == Tab.SETTINGS) SoftGold else UnselectedTint,
                                    animationSpec = tween(200),
                                    label = "settings_tint",
                                ).value,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                } else {
                    // --- EXPANDED BAR ---
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // Expanded accessory above
                        if (showAccessory && currentReading != null) {
                            ExpandedAccessory(
                                readingType = currentReading!!,
                                isPlaying = isPlaying,
                                currentPosition = currentPosition,
                                onPlayPause = { audioPlayer.togglePlayPause() },
                                onStop = { audioPlayer.stop() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                                    .sharedElement(
                                        rememberSharedContentState("accessory"),
                                        this@AnimatedContent,
                                    )
                                    .shadow(12.dp, RoundedCornerShape(50))
                                    .clip(RoundedCornerShape(50))
                                    .background(BarBg)
                                    .border(1.dp, BarBorder, RoundedCornerShape(50)),
                            )
                        }

                        // Tab row + settings (same height)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Main capsule with animated selection pill
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(IntrinsicSize.Min)
                                    .sharedElement(
                                        rememberSharedContentState("tabGroup"),
                                        this@AnimatedContent,
                                    )
                                    .shadow(12.dp, RoundedCornerShape(50))
                                    .clip(RoundedCornerShape(50))
                                    .background(BarBg)
                                    .border(1.dp, BarBorder, RoundedCornerShape(50))
                                    .padding(5.dp),
                            ) {
                                // Track tab positions
                                val tabPositions = remember { mutableStateMapOf<Tab, Pair<Float, Float>>() }
                                val density = LocalDensity.current

                                // Animated pill position
                                val selectedPos = tabPositions[selectedTab]
                                val pillOffsetX by animateDpAsState(
                                    targetValue = with(density) { (selectedPos?.first ?: 0f).toDp() },
                                    animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f),
                                    label = "pill_x",
                                )
                                val pillWidth by animateDpAsState(
                                    targetValue = with(density) { (selectedPos?.second ?: 0f).toDp() },
                                    animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f),
                                    label = "pill_w",
                                )

                                // Selection pill (behind tabs)
                                if (selectedPos != null && pillWidth > 0.dp) {
                                    val pillShape = RoundedCornerShape(50)
                                    Box(
                                        modifier = Modifier
                                            .offset(x = pillOffsetX)
                                            .width(pillWidth)
                                            .fillMaxHeight()
                                            .clip(pillShape)
                                            .background(SelectedBg)
                                            .border(0.5.dp, SelectedBorder, pillShape)
                                    )
                                }

                                // Tab items row (on top)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Tab.entries.filter { it != Tab.SETTINGS }.forEach { tab ->
                                        val isSelected = selectedTab == tab
                                        ExpandedTabItem(
                                            icon = tab.icon,
                                            label = tab.label,
                                            isSelected = isSelected,
                                            onClick = { selectedTab = tab },
                                            modifier = Modifier
                                                .weight(1f)
                                                .onGloballyPositioned { coords ->
                                                    tabPositions[tab] = Pair(
                                                        coords.positionInParent().x,
                                                        coords.size.width.toFloat(),
                                                    )
                                                },
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.width(8.dp))

                            // Settings circle — matches capsule height
                            Box(
                                modifier = Modifier
                                    .sharedElement(
                                        rememberSharedContentState("standalone"),
                                        this@AnimatedContent,
                                    )
                                    .fillMaxHeight()
                                    .aspectRatio(1f)
                                    .shadow(12.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(BarBg)
                                    .border(1.dp, BarBorder, CircleShape)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { selectedTab = Tab.SETTINGS },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = "Settings",
                                    tint = animateColorAsState(
                                    targetValue = if (selectedTab == Tab.SETTINGS) SoftGold else UnselectedTint,
                                    animationSpec = tween(200),
                                    label = "settings_tint",
                                ).value,
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Readings detail overlay
        androidx.compose.animation.AnimatedVisibility(
            visible = showReadings,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300),
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300),
            ),
        ) {
            readingsLiturgy?.let { liturgy ->
                ReadingsScreen(
                    liturgy = liturgy,
                    verse = readingsVerse,
                    initialSection = readingsInitialSection,
                    onBack = { showReadings = false },
                )
            }
        }
    }
}

@Composable
private fun ExpandedTabItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint by animateColorAsState(
        targetValue = if (isSelected) SoftGold else UnselectedTint,
        animationSpec = tween(200),
        label = "tab_tint",
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(start = 4.dp, end = 4.dp, top = 7.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = tint,
            lineHeight = 10.sp,
        )
    }
}

@Composable
private fun ExpandedAccessory(
    readingType: ReadingType,
    isPlaying: Boolean,
    currentPosition: Long,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = readingIcon(readingType),
            contentDescription = null,
            tint = SoftGold,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = readingType.displayName,
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
        IconButton(onClick = onPlayPause, modifier = Modifier.size(34.dp)) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = SoftGold,
                modifier = Modifier.size(24.dp),
            )
        }
        IconButton(onClick = onStop, modifier = Modifier.size(34.dp)) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = null,
                tint = Slate,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun InlineAccessory(
    readingType: ReadingType,
    isPlaying: Boolean,
    currentPosition: Long,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = readingIcon(readingType),
            contentDescription = null,
            tint = SoftGold,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = readingType.displayName,
            fontSize = 13.sp,
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
        )
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = onPlayPause, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = SoftGold,
                modifier = Modifier.size(22.dp),
            )
        }
        IconButton(onClick = onStop, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = null,
                tint = Slate,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun readingIcon(readingType: ReadingType): ImageVector = when (readingType) {
    ReadingType.FIRST_READING -> Icons.Filled.LooksOne
    ReadingType.PSALM -> Icons.Filled.MusicNote
    ReadingType.SECOND_READING -> Icons.Filled.LooksTwo
    ReadingType.GOSPEL -> Icons.Filled.AutoStories
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
private fun PlaceholderScreen(tab: Tab) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NearBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = tab.icon,
                contentDescription = null,
                tint = SoftGold.copy(alpha = 0.3f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = tab.label,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Coming soon",
                fontSize = 14.sp,
                color = Slate,
            )
        }
    }
}
