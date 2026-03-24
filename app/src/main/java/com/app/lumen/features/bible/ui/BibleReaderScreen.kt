package com.app.lumen.features.bible.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.app.lumen.R
import com.app.lumen.features.bible.model.BibleChapter
import com.app.lumen.features.bible.model.BibleChapterSummary
import com.app.lumen.features.bible.service.BibleBookInfo
import com.app.lumen.features.bible.viewmodel.BibleViewModel
import com.app.lumen.features.bible.viewmodel.LoadingState
import com.app.lumen.ui.theme.NearBlack
import com.app.lumen.ui.theme.Slate
import com.app.lumen.ui.theme.SoftGold
import kotlinx.coroutines.launch

private val GlassBg = Color(0xFF191927)
private val PanelBg = Color(0xFF23233D)
private val PanelBorder = Color.White.copy(alpha = 0.12f)

private const val PREFS_NAME = "bible_reader_prefs"
private const val KEY_FONT_SIZE = "font_size"
private const val KEY_LINE_SPACING = "line_spacing"
private const val KEY_JUSTIFY = "is_justified"
private const val KEY_LAST_CHAPTER = "last_chapter_"

private fun getReaderFontSize(context: Context): Float {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getFloat(KEY_FONT_SIZE, 20f)
}

private fun getReaderLineSpacing(context: Context): Float {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getFloat(KEY_LINE_SPACING, 8f)
}

private fun getReaderJustify(context: Context): Boolean {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_JUSTIFY, true)
}

private fun saveReaderSettings(context: Context, fontSize: Float, lineSpacing: Float, justify: Boolean) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        .putFloat(KEY_FONT_SIZE, fontSize)
        .putFloat(KEY_LINE_SPACING, lineSpacing)
        .putBoolean(KEY_JUSTIFY, justify)
        .apply()
}

private fun getLastChapterIndex(context: Context, bookId: String): Int {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getInt("$KEY_LAST_CHAPTER$bookId", 0)
}

private fun saveLastChapterIndex(context: Context, bookId: String, index: Int) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        .putInt("$KEY_LAST_CHAPTER$bookId", index)
        .apply()
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun BibleReaderScreen(
    book: BibleBook,
    bookInfo: BibleBookInfo,
    bibleViewModel: BibleViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val rawChapters by bibleViewModel.chapters.collectAsState()
    val chaptersLoadingState by bibleViewModel.chaptersLoadingState.collectAsState()

    // Filter out "intro" chapters from API.Bible
    val chapters = remember(rawChapters) {
        rawChapters.filter { it.number != "intro" }
    }

    var showChapterPicker by remember { mutableStateOf(false) }
    var showTextSettings by remember { mutableStateOf(false) }

    // Text settings state
    var readerFontSize by remember { mutableStateOf(getReaderFontSize(context)) }
    var readerLineSpacing by remember { mutableStateOf(getReaderLineSpacing(context)) }
    var readerJustify by remember { mutableStateOf(getReaderJustify(context)) }

    // Chapter cache for pager
    val chapterContentCache = remember { mutableStateMapOf<String, BibleChapter>() }

    // Load book data on entry
    LaunchedEffect(bookInfo.id) {
        bibleViewModel.selectBookForReader(bookInfo)
    }

    // Pager state — initialized once chapters are loaded
    val savedIndex = remember(bookInfo.id) { getLastChapterIndex(context, bookInfo.id) }
    val pagerState = rememberPagerState(
        initialPage = savedIndex.coerceIn(0, (chapters.size - 1).coerceAtLeast(0)),
    ) { chapters.size }

    // Jump to saved page when chapters first load
    LaunchedEffect(chapters.size) {
        if (chapters.isNotEmpty()) {
            val target = savedIndex.coerceIn(0, chapters.size - 1)
            if (pagerState.currentPage != target) {
                pagerState.scrollToPage(target)
            }
        }
    }

    // Save last chapter index when page changes
    LaunchedEffect(pagerState.currentPage) {
        if (chapters.isNotEmpty()) {
            saveLastChapterIndex(context, bookInfo.id, pagerState.currentPage)
        }
    }

    // Derive current chapter title from pager position
    val currentChapterRef = remember(pagerState.currentPage, chapters) {
        chapters.getOrNull(pagerState.currentPage)?.reference ?: ""
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NearBlack)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            // ── Toolbar ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 16.dp),
            ) {
                // Back button
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(GlassBg)
                        .border(0.5.dp, Color.White.copy(alpha = 0.18f), CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Book name (center)
                Text(
                    text = book.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SoftGold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 56.dp),
                )

                // Chapter picker button
                IconButton(
                    onClick = { showChapterPicker = !showChapterPicker },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(GlassBg)
                        .border(0.5.dp, Color.White.copy(alpha = 0.18f), CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.Filled.FormatListNumbered,
                        contentDescription = stringResource(R.string.cd_chapter_picker),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // ── Chapter title ───────────────────────────────────
            if (currentChapterRef.isNotEmpty()) {
                Text(
                    text = currentChapterRef,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SoftGold,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 8.dp),
                )
            }

            // ── Chapter content (swipeable pager) ───────────────
            when {
                chaptersLoadingState == LoadingState.LOADING ||
                        chaptersLoadingState == LoadingState.IDLE -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = SoftGold,
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                }

                chaptersLoadingState == LoadingState.ERROR -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.error_load_chapters),
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 15.sp,
                        )
                    }
                }

                chapters.isNotEmpty() -> {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f),
                        key = { chapters[it].id },
                        contentPadding = PaddingValues(0.dp),
                        pageSpacing = 0.dp,
                        beyondViewportPageCount = 1,
                    ) { pageIndex ->
                        val chapterId = chapters[pageIndex].id
                        val cachedChapter = chapterContentCache[chapterId]

                        // Load chapter content
                        LaunchedEffect(chapterId) {
                            if (cachedChapter == null) {
                                try {
                                    val bibleId = bibleViewModel.selectedBible.value?.id ?: return@LaunchedEffect
                                    val routingService = com.app.lumen.features.bible.service.BibleRoutingService.getInstance(context)
                                    val chapter = routingService.fetchChapter(bibleId, chapterId)
                                    chapterContentCache[chapterId] = chapter
                                } catch (_: Exception) {}
                            }
                        }

                        if (cachedChapter != null) {
                            val scrollState = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState)
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 120.dp),
                            ) {
                                BibleChapterText(
                                    html = cachedChapter.content,
                                    fontSize = readerFontSize,
                                    lineSpacing = readerLineSpacing,
                                    justify = readerJustify,
                                )

                                // Next / Previous chapter navigation
                                Spacer(Modifier.height(32.dp))
                                ChapterNavigationBar(
                                    pageIndex = pageIndex,
                                    chapters = chapters,
                                    onNavigate = { targetIndex ->
                                        scope.launch {
                                            pagerState.animateScrollToPage(targetIndex)
                                        }
                                    },
                                )
                            }
                        } else {
                            // Loading indicator for individual chapter
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    color = SoftGold,
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 2.dp,
                                )
                            }
                        }
                    }
                }

                else -> {
                    Spacer(Modifier.weight(1f))
                }
            }
        }

        // ── Text settings FAB ───────────────────────────────────
        IconButton(
            onClick = { showTextSettings = !showTextSettings },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = 16.dp)
                .size(48.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(GlassBg)
                .border(0.5.dp, Color.White.copy(alpha = 0.18f), CircleShape),
        ) {
            Text(
                text = "AA",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = SoftGold,
            )
        }

        // ── Scrim for dropdowns ─────────────────────────────────
        AnimatedVisibility(
            visible = showChapterPicker || showTextSettings,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(150)),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.2f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            showChapterPicker = false
                            showTextSettings = false
                        },
                    ),
            )
        }

        // ── Chapter picker dropdown ─────────────────────────────
        AnimatedVisibility(
            visible = showChapterPicker,
            enter = fadeIn(tween(150)) + scaleIn(
                initialScale = 0.4f,
                transformOrigin = TransformOrigin(0.9f, 0f),
                animationSpec = tween(150),
            ),
            exit = fadeOut(tween(100)) + scaleOut(
                targetScale = 0.4f,
                transformOrigin = TransformOrigin(0.9f, 0f),
                animationSpec = tween(100),
            ),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                ChapterPickerPanel(
                    chapters = chapters,
                    currentPageIndex = pagerState.currentPage,
                    onSelect = { index ->
                        showChapterPicker = false
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 60.dp, end = 20.dp),
                )
            }
        }

        // ── Text settings sheet ─────────────────────────────────
        if (showTextSettings) {
            TextSettingsSheet(
                fontSize = readerFontSize,
                lineSpacing = readerLineSpacing,
                isJustified = readerJustify,
                onFontSizeChange = { readerFontSize = it },
                onLineSpacingChange = { readerLineSpacing = it },
                onJustifyChange = { readerJustify = it },
                onDismiss = {
                    saveReaderSettings(context, readerFontSize, readerLineSpacing, readerJustify)
                    showTextSettings = false
                },
            )
        }
    }
}

// ── Chapter Picker Panel ────────────────────────────────────────────
@Composable
private fun ChapterPickerPanel(
    chapters: List<BibleChapterSummary>,
    currentPageIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(200.dp)
            .heightIn(max = 400.dp)
            .shadow(16.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(PanelBg)
            .border(0.5.dp, PanelBorder, RoundedCornerShape(14.dp)),
    ) {
        LazyColumn {
            items(chapters.size) { index ->
                val chapter = chapters[index]
                val isSelected = index == currentPageIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(index) }
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = SoftGold,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = chapter.reference,
                        fontSize = 15.sp,
                        color = if (isSelected) SoftGold else Color.White,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
                if (index < chapters.size - 1) {
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}

// ── Text Settings Bottom Sheet ──────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextSettingsSheet(
    fontSize: Float,
    lineSpacing: Float,
    isJustified: Boolean,
    onFontSizeChange: (Float) -> Unit,
    onLineSpacingChange: (Float) -> Unit,
    onJustifyChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PanelBg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.3f)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.cd_text_settings),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(R.string.done),
                        color = SoftGold,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Preview text
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.reader_preview_text),
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize + lineSpacing).sp,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = if (isJustified) TextAlign.Justify else TextAlign.Start,
                )
            }

            Spacer(Modifier.height(24.dp))

            // Font Size slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.reader_font_size),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.7f),
                )
                Text(
                    text = stringResource(R.string.reader_pt_format, fontSize.toInt()),
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.5f),
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "A", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
                Slider(
                    value = fontSize,
                    onValueChange = onFontSizeChange,
                    valueRange = 14f..28f,
                    steps = 6,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = SoftGold,
                        inactiveTrackColor = Color.White.copy(alpha = 0.15f),
                    ),
                )
                Text(text = "A", fontSize = 20.sp, color = Color.White.copy(alpha = 0.5f))
            }

            Spacer(Modifier.height(16.dp))

            // Line Spacing slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.reader_line_spacing),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.7f),
                )
                Text(
                    text = stringResource(R.string.reader_pt_format, lineSpacing.toInt()),
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.5f),
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Filled.DensitySmall,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp),
                )
                Slider(
                    value = lineSpacing,
                    onValueChange = onLineSpacingChange,
                    valueRange = 2f..16f,
                    steps = 6,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = SoftGold,
                        inactiveTrackColor = Color.White.copy(alpha = 0.15f),
                    ),
                )
                Icon(
                    imageVector = Icons.Filled.DensityMedium,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp),
                )
            }

            Spacer(Modifier.height(16.dp))

            // Justify toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.FormatAlignJustify,
                    contentDescription = null,
                    tint = SoftGold,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.reader_justify_text),
                    fontSize = 15.sp,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = isJustified,
                    onCheckedChange = onJustifyChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = SoftGold,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.7f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.15f),
                    ),
                )
            }
        }
    }
}

// ── Chapter Navigation Bar ──────────────────────────────────────────
@Composable
private fun ChapterNavigationBar(
    pageIndex: Int,
    chapters: List<BibleChapterSummary>,
    onNavigate: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Previous
        if (pageIndex > 0) {
            TextButton(onClick = { onNavigate(pageIndex - 1) }) {
                Icon(
                    imageVector = Icons.Filled.ChevronLeft,
                    contentDescription = null,
                    tint = SoftGold,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = chapters[pageIndex - 1].reference,
                    color = SoftGold,
                    fontSize = 14.sp,
                )
            }
        } else {
            Spacer(Modifier.width(1.dp))
        }

        // Next
        if (pageIndex < chapters.size - 1) {
            TextButton(onClick = { onNavigate(pageIndex + 1) }) {
                Text(
                    text = chapters[pageIndex + 1].reference,
                    color = SoftGold,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = SoftGold,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

// ── Native TextView-based Chapter Renderer ──────────────────────────

@Composable
private fun BibleChapterText(
    html: String,
    fontSize: Float,
    lineSpacing: Float,
    justify: Boolean,
) {
    val density = LocalContext.current.resources.displayMetrics.density
    val spannedText = remember(html, fontSize) { buildChapterSpannable(html, fontSize) }

    AndroidView(
        factory = { ctx ->
            android.widget.TextView(ctx).apply {
                setTextColor(android.graphics.Color.argb(217, 255, 255, 255)) // 0.85 alpha
                setLineSpacing(lineSpacing * density, 1f)
                includeFontPadding = false
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    justificationMode = if (justify) {
                        android.text.Layout.JUSTIFICATION_MODE_INTER_WORD
                    } else {
                        android.text.Layout.JUSTIFICATION_MODE_NONE
                    }
                }
            }
        },
        update = { tv ->
            tv.text = spannedText
            tv.textSize = fontSize
            tv.setLineSpacing(lineSpacing * density, 1f)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                tv.justificationMode = if (justify) {
                    android.text.Layout.JUSTIFICATION_MODE_INTER_WORD
                } else {
                    android.text.Layout.JUSTIFICATION_MODE_NONE
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun buildChapterSpannable(html: String, bodyFontSize: Float): android.text.SpannableString {
    val cleaned = cleanHtmlToPlainText(html)

    val versePattern = Regex("\\[(\\d+)]")
    val matches = versePattern.findAll(cleaned).toList()

    // Build plain text with verse numbers replacing markers
    val sb = StringBuilder()
    val verseRanges = mutableListOf<Pair<IntRange, String>>()
    var lastIndex = 0

    for (match in matches) {
        // Text before marker — trim trailing space
        if (match.range.first > lastIndex) {
            val text = cleaned.substring(lastIndex, match.range.first).trimEnd()
            if (text.isNotEmpty()) {
                sb.append(text)
            }
        }
        sb.append(" ")
        val verseStart = sb.length
        val verseNum = match.groupValues[1]
        sb.append(verseNum)
        val verseEnd = sb.length
        verseRanges.add(IntRange(verseStart, verseEnd - 1) to verseNum)
        // Thin space after verse number
        sb.append("\u2009")
        lastIndex = match.range.last + 1
    }

    // Remaining text
    if (lastIndex < cleaned.length) {
        val remaining = cleaned.substring(lastIndex).trimStart()
        if (remaining.isNotEmpty()) {
            sb.append(remaining)
        }
    }

    val spannable = android.text.SpannableString(sb.toString())
    val goldColor = android.graphics.Color.rgb(0xD4, 0xAF, 0x37) // SoftGold
    val verseSize = bodyFontSize * 0.65f

    for ((range, _) in verseRanges) {
        spannable.setSpan(
            android.text.style.SuperscriptSpan(),
            range.first, range.last + 1,
            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        spannable.setSpan(
            android.text.style.AbsoluteSizeSpan((verseSize).toInt(), true),
            range.first, range.last + 1,
            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        spannable.setSpan(
            android.text.style.ForegroundColorSpan(goldColor),
            range.first, range.last + 1,
            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        spannable.setSpan(
            android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
            range.first, range.last + 1,
            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
    }

    return spannable
}

private fun cleanHtmlToPlainText(html: String): String {
    // 1. Strip cross-reference notes and footnotes
    var cleaned = html
        .replace(Regex("<note[^>]*>.*?</note>", RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("<sup[^>]*class=\"[^\"]*f[^\"]*\"[^>]*>.*?</sup>", RegexOption.DOT_MATCHES_ALL), "")

    // 2. Extract verse numbers from various HTML formats and convert to [N] markers
    cleaned = cleaned.replace(
        Regex("<span[^>]*data-number=\"(\\d+)\"[^>]*class=\"[^\"]*v[^\"]*\"[^>]*>\\d+</span>"),
        " [$1] ",
    )
    cleaned = cleaned.replace(
        Regex("<span[^>]*class=\"[^\"]*v[^\"]*\"[^>]*>(\\d+)</span>"),
        " [$1] ",
    )
    cleaned = cleaned.replace(
        Regex("<sup[^>]*>(\\d+)</sup>"),
        " [$1] ",
    )
    cleaned = cleaned.replace(
        Regex("<span[^>]*data-number=\"(\\d+)\"[^>]*>\\s*\\d+\\s*</span>"),
        " [$1] ",
    )

    // 3. Handle paragraph breaks
    cleaned = cleaned
        .replace(Regex("<p[^>]*>"), "\n\n")
        .replace("</p>", "")
        .replace(Regex("<br[^>]*>"), "\n")

    // 4. Strip all remaining HTML tags
    cleaned = cleaned.replace(Regex("<[^>]+>"), "")

    // 5. Decode HTML entities
    cleaned = cleaned
        .replace("&nbsp;", " ")
        .replace("&#160;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")

    // 6. Clean up whitespace
    cleaned = cleaned
        .replace(Regex("[ \\t]+"), " ")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()

    // 7. Fallback: detect plain-text verse numbers if no markers created
    if (!cleaned.contains(Regex("\\[\\d+]"))) {
        cleaned = cleaned.replace(Regex("""(?<=[\.\!\?\u00BB\u201D\u201E]\s)(\d{1,3})(?=\s)"""), "[$1]")
        cleaned = cleaned.replace(Regex("""^(\d{1,3})\s"""), "[$1] ")
    }

    return cleaned
}
