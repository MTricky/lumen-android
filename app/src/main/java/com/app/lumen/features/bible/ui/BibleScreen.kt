package com.app.lumen.features.bible.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.StringRes
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.lumen.R
import com.app.lumen.features.bible.service.BibleBookInfo
import com.app.lumen.features.bible.viewmodel.BibleViewModel
import com.app.lumen.features.bible.viewmodel.LoadingState
import com.app.lumen.ui.theme.NearBlack
import com.app.lumen.ui.theme.Slate
import com.app.lumen.ui.theme.SoftGold

// ── Testament Selection ─────────────────────────────────────────────
enum class Testament {
    OLD, NEW
}

// ── Book Sections ───────────────────────────────────────────────────
enum class BibleBookSection(
    @StringRes val displayNameRes: Int,
    val icon: ImageVector,
    val testament: Testament,
) {
    // Old Testament
    LAW(R.string.section_law, Icons.Filled.Description, Testament.OLD),
    OT_HISTORY(R.string.section_ot_history, Icons.AutoMirrored.Filled.MenuBook, Testament.OLD),
    POETRY(R.string.section_poetry, Icons.Filled.MusicNote, Testament.OLD),
    MAJOR_PROPHETS(R.string.section_major_prophets, Icons.Filled.RecordVoiceOver, Testament.OLD),
    MINOR_PROPHETS(R.string.section_minor_prophets, Icons.Filled.RecordVoiceOver, Testament.OLD),

    // New Testament
    GOSPELS(R.string.section_gospels, Icons.Filled.AutoStories, Testament.NEW),
    NT_HISTORY(R.string.section_nt_history, Icons.AutoMirrored.Filled.DirectionsWalk, Testament.NEW),
    PAULINE_EPISTLES(R.string.section_pauline_epistles, Icons.Filled.Email, Testament.NEW),
    GENERAL_EPISTLES(R.string.section_general_epistles, Icons.Filled.Email, Testament.NEW),
    PROPHECY(R.string.section_prophecy, Icons.Filled.AutoAwesome, Testament.NEW),
}

// ── Bible Book ──────────────────────────────────────────────────────
data class BibleBook(
    val id: String,
    val abbreviation: String,
    val name: String,
    val section: BibleBookSection,
)

// ── Section mapping from book ID ────────────────────────────────────
private val bookSectionMap: Map<String, BibleBookSection> by lazy {
    defaultBooks.associate { it.id to it.section }
}

private fun sectionForBookId(id: String): BibleBookSection? = bookSectionMap[id]

private fun toBibleBook(info: BibleBookInfo): BibleBook? {
    val section = sectionForBookId(info.id) ?: return null
    return BibleBook(
        id = info.id,
        abbreviation = info.abbreviation.ifEmpty { info.id },
        name = info.name,
        section = section,
    )
}

// ── Default Book Data (fallback + section mapping source) ───────────
private val defaultBooks = listOf(
    // Law
    BibleBook("GEN", "GEN", "Genesis", BibleBookSection.LAW),
    BibleBook("EXO", "EXO", "Exodus", BibleBookSection.LAW),
    BibleBook("LEV", "LEV", "Leviticus", BibleBookSection.LAW),
    BibleBook("NUM", "NUM", "Numbers", BibleBookSection.LAW),
    BibleBook("DEU", "DEU", "Deuteronomy", BibleBookSection.LAW),

    // OT History
    BibleBook("JOS", "JOS", "Joshua", BibleBookSection.OT_HISTORY),
    BibleBook("JDG", "JDG", "Judges", BibleBookSection.OT_HISTORY),
    BibleBook("RUT", "RUT", "Ruth", BibleBookSection.OT_HISTORY),
    BibleBook("1SA", "1SA", "1 Samuel", BibleBookSection.OT_HISTORY),
    BibleBook("2SA", "2SA", "2 Samuel", BibleBookSection.OT_HISTORY),
    BibleBook("1KI", "1KI", "1 Kings", BibleBookSection.OT_HISTORY),
    BibleBook("2KI", "2KI", "2 Kings", BibleBookSection.OT_HISTORY),
    BibleBook("1CH", "1CH", "1 Chronicles", BibleBookSection.OT_HISTORY),
    BibleBook("2CH", "2CH", "2 Chronicles", BibleBookSection.OT_HISTORY),
    BibleBook("EZR", "EZR", "Ezra", BibleBookSection.OT_HISTORY),
    BibleBook("NEH", "NEH", "Nehemiah", BibleBookSection.OT_HISTORY),
    BibleBook("TOB", "TOB", "Tobit", BibleBookSection.OT_HISTORY),
    BibleBook("JDT", "JDT", "Judith", BibleBookSection.OT_HISTORY),
    BibleBook("EST", "EST", "Esther", BibleBookSection.OT_HISTORY),
    BibleBook("1MA", "1MA", "1 Maccabees", BibleBookSection.OT_HISTORY),
    BibleBook("2MA", "2MA", "2 Maccabees", BibleBookSection.OT_HISTORY),

    // Poetry & Wisdom
    BibleBook("JOB", "JOB", "Job", BibleBookSection.POETRY),
    BibleBook("PSA", "PSA", "Psalms", BibleBookSection.POETRY),
    BibleBook("PRO", "PRO", "Proverbs", BibleBookSection.POETRY),
    BibleBook("ECC", "ECC", "Ecclesiastes", BibleBookSection.POETRY),
    BibleBook("SNG", "SNG", "Song of Songs", BibleBookSection.POETRY),
    BibleBook("WIS", "WIS", "Wisdom", BibleBookSection.POETRY),
    BibleBook("SIR", "SIR", "Sirach", BibleBookSection.POETRY),

    // Major Prophets
    BibleBook("ISA", "ISA", "Isaiah", BibleBookSection.MAJOR_PROPHETS),
    BibleBook("JER", "JER", "Jeremiah", BibleBookSection.MAJOR_PROPHETS),
    BibleBook("LAM", "LAM", "Lamentations", BibleBookSection.MAJOR_PROPHETS),
    BibleBook("BAR", "BAR", "Baruch", BibleBookSection.MAJOR_PROPHETS),
    BibleBook("EZK", "EZK", "Ezekiel", BibleBookSection.MAJOR_PROPHETS),
    BibleBook("DAN", "DAN", "Daniel", BibleBookSection.MAJOR_PROPHETS),

    // Minor Prophets
    BibleBook("HOS", "HOS", "Hosea", BibleBookSection.MINOR_PROPHETS),
    BibleBook("JOL", "JOL", "Joel", BibleBookSection.MINOR_PROPHETS),
    BibleBook("AMO", "AMO", "Amos", BibleBookSection.MINOR_PROPHETS),
    BibleBook("OBA", "OBA", "Obadiah", BibleBookSection.MINOR_PROPHETS),
    BibleBook("JON", "JON", "Jonah", BibleBookSection.MINOR_PROPHETS),
    BibleBook("MIC", "MIC", "Micah", BibleBookSection.MINOR_PROPHETS),
    BibleBook("NAM", "NAM", "Nahum", BibleBookSection.MINOR_PROPHETS),
    BibleBook("HAB", "HAB", "Habakkuk", BibleBookSection.MINOR_PROPHETS),
    BibleBook("ZEP", "ZEP", "Zephaniah", BibleBookSection.MINOR_PROPHETS),
    BibleBook("HAG", "HAG", "Haggai", BibleBookSection.MINOR_PROPHETS),
    BibleBook("ZEC", "ZEC", "Zechariah", BibleBookSection.MINOR_PROPHETS),
    BibleBook("MAL", "MAL", "Malachi", BibleBookSection.MINOR_PROPHETS),

    // Gospels
    BibleBook("MAT", "MAT", "Matthew", BibleBookSection.GOSPELS),
    BibleBook("MRK", "MRK", "Mark", BibleBookSection.GOSPELS),
    BibleBook("LUK", "LUK", "Luke", BibleBookSection.GOSPELS),
    BibleBook("JHN", "JHN", "John", BibleBookSection.GOSPELS),

    // NT History
    BibleBook("ACT", "ACT", "Acts", BibleBookSection.NT_HISTORY),

    // Pauline Epistles
    BibleBook("ROM", "ROM", "Romans", BibleBookSection.PAULINE_EPISTLES),
    BibleBook("1CO", "1CO", "1 Corinthians", BibleBookSection.PAULINE_EPISTLES),
    BibleBook("2CO", "2CO", "2 Corinthians", BibleBookSection.PAULINE_EPISTLES),
    BibleBook("GAL", "GAL", "Galatians", BibleBookSection.PAULINE_EPISTLES),
    BibleBook("EPH", "EPH", "Ephesians", BibleBookSection.PAULINE_EPISTLES),
    BibleBook("PHP", "PHP", "Philippians", BibleBookSection.PAULINE_EPISTLES),
    BibleBook("COL", "COL", "Colossians", BibleBookSection.PAULINE_EPISTLES),
    BibleBook("1TH", "1TH", "1 Thessalonians", BibleBookSection.PAULINE_EPISTLES),
    BibleBook("2TH", "2TH", "2 Thessalonians", BibleBookSection.PAULINE_EPISTLES),
    BibleBook("1TI", "1TI", "1 Timothy", BibleBookSection.PAULINE_EPISTLES),
    BibleBook("2TI", "2TI", "2 Timothy", BibleBookSection.PAULINE_EPISTLES),
    BibleBook("TIT", "TIT", "Titus", BibleBookSection.PAULINE_EPISTLES),
    BibleBook("PHM", "PHM", "Philemon", BibleBookSection.PAULINE_EPISTLES),

    // General Epistles
    BibleBook("HEB", "HEB", "Hebrews", BibleBookSection.GENERAL_EPISTLES),
    BibleBook("JAS", "JAS", "James", BibleBookSection.GENERAL_EPISTLES),
    BibleBook("1PE", "1PE", "1 Peter", BibleBookSection.GENERAL_EPISTLES),
    BibleBook("2PE", "2PE", "2 Peter", BibleBookSection.GENERAL_EPISTLES),
    BibleBook("1JN", "1JN", "1 John", BibleBookSection.GENERAL_EPISTLES),
    BibleBook("2JN", "2JN", "2 John", BibleBookSection.GENERAL_EPISTLES),
    BibleBook("3JN", "3JN", "3 John", BibleBookSection.GENERAL_EPISTLES),
    BibleBook("JUD", "JUD", "Jude", BibleBookSection.GENERAL_EPISTLES),

    // Prophecy
    BibleBook("REV", "REV", "Revelation", BibleBookSection.PROPHECY),
)

private val HEADER_HEIGHT = 380.dp
private val ToolbarGlassBg = Color.White.copy(alpha = 0.15f)
private val BookCardBg = Color(0xFF22222E)
private val BookCardBorder = Color.White.copy(alpha = 0.10f)

// ── Main Screen ─────────────────────────────────────────────────────
@Composable
fun BibleScreen(
    bottomPadding: Dp = 0.dp,
    onBookSelected: (BibleBook) -> Unit = {},
    bibleViewModel: BibleViewModel = viewModel(),
) {
    var selectedTestament by remember { mutableStateOf(Testament.OLD) }
    var showTranslationPicker by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }

    val apiBooks by bibleViewModel.books.collectAsState()
    val booksLoadingState by bibleViewModel.booksLoadingState.collectAsState()

    // Convert API books to BibleBook with section mapping, fall back to defaults
    val allBooks = remember(apiBooks) {
        if (apiBooks.isEmpty()) {
            defaultBooks
        } else {
            apiBooks.mapNotNull { toBibleBook(it) }.ifEmpty { defaultBooks }
        }
    }

    val books = remember(allBooks, selectedTestament) {
        allBooks.filter { it.section.testament == selectedTestament }
    }

    // Search: filter across all books by name, abbreviation, and id
    val searchResults = remember(allBooks, searchText) {
        if (searchText.isBlank()) emptyList()
        else {
            val query = searchText.lowercase()
            allBooks.filter { book ->
                book.name.lowercase().contains(query) ||
                        book.abbreviation.lowercase().contains(query) ||
                        book.id.lowercase().contains(query)
            }
        }
    }

    val sections = remember(selectedTestament) {
        BibleBookSection.entries.filter { it.testament == selectedTestament }
    }

    val isLoadingBooks = booksLoadingState == LoadingState.LOADING ||
            booksLoadingState == LoadingState.IDLE

    val bookCount = books.size
    val testamentTitle = if (selectedTestament == Testament.OLD)
        stringResource(R.string.bible_old_testament)
    else
        stringResource(R.string.bible_new_testament)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NearBlack),
    ) {
        AnimatedContent(
            targetState = selectedTestament,
            transitionSpec = {
                fadeIn(tween(200, delayMillis = 50)) togetherWith fadeOut(tween(150))
            },
            label = "testament_switch",
        ) { currentTestament ->
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // Header
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(HEADER_HEIGHT),
                    ) {
                        // Background image
                        Image(
                            painter = painterResource(
                                if (currentTestament == Testament.OLD) R.drawable.bible_old_testament
                                else R.drawable.bible_new_testament,
                            ),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .drawWithContent {
                                    drawContent()
                                    // Top fade for status bar
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                NearBlack.copy(alpha = 0.4f),
                                                Color.Transparent,
                                            ),
                                            startY = 0f,
                                            endY = size.height * 0.15f,
                                        ),
                                    )
                                    // Bottom fade into content
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            colorStops = arrayOf(
                                                0.0f to Color.Transparent,
                                                0.3f to Color.Transparent,
                                                0.5f to NearBlack.copy(alpha = 0.3f),
                                                0.65f to NearBlack.copy(alpha = 0.6f),
                                                0.78f to NearBlack.copy(alpha = 0.85f),
                                                0.88f to NearBlack.copy(alpha = 0.95f),
                                                1.0f to NearBlack,
                                            ),
                                        ),
                                    )
                                },
                        )

                        // Toolbar: language button + picker + search button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Language button
                            val languageBtnBg = if (currentTestament == Testament.OLD)
                                Color.White.copy(alpha = 0.22f)
                            else
                                ToolbarGlassBg
                            IconButton(
                                onClick = { showTranslationPicker = true },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(languageBtnBg)
                                    .border(0.5.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Language,
                                    contentDescription = stringResource(R.string.cd_language),
                                    tint = SoftGold,
                                    modifier = Modifier.size(20.dp),
                                )
                            }

                            Spacer(Modifier.weight(1f))

                            // Old / New picker
                            TestamentPicker(
                                selected = currentTestament,
                                onSelect = { selectedTestament = it },
                            )

                            Spacer(Modifier.weight(1f))

                            // Search button
                            IconButton(
                                onClick = { isSearching = true },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(ToolbarGlassBg)
                                    .border(0.5.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = stringResource(R.string.cd_search),
                                    tint = SoftGold,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }

                        // Header text centered at bottom
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 24.dp)
                                .padding(bottom = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = testamentTitle,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                lineHeight = 30.sp,
                                textAlign = TextAlign.Center,
                            )

                            Spacer(Modifier.height(6.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = SoftGold,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.bible_book_count, bookCount),
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                )
                            }
                        }
                    }
                }

                if (isLoadingBooks) {
                    // Shimmer placeholder grid (12 cards in 2 columns)
                    items(6) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            ShimmerBookCard(modifier = Modifier.weight(1f))
                            ShimmerBookCard(modifier = Modifier.weight(1f))
                        }
                    }
                } else {
                    // Sections with books
                    var sectionIndex = 0
                    sections.forEach { section ->
                        val sectionBooks = books.filter { it.section == section }
                        if (sectionBooks.isNotEmpty()) {
                            val topPad = if (sectionIndex == 0) 0.dp else 20.dp
                            sectionIndex++
                            // Section header
                            item(key = "header_${section.name}") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp)
                                        .padding(top = topPad, bottom = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = section.icon,
                                        contentDescription = null,
                                        tint = SoftGold,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(section.displayNameRes),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Slate,
                                    )
                                }
                            }

                            // Books in 2-column grid
                            val rows = sectionBooks.chunked(2)
                            items(rows, key = { row -> row.first().id }) { row ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .padding(bottom = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    row.forEach { book ->
                                        BookCard(
                                            book = book,
                                            onClick = { onBookSelected(book) },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                    // Fill empty space if odd number
                                    if (row.size == 1) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom spacer for tab bar
                item {
                    Spacer(Modifier.height(bottomPadding + 8.dp))
                }
            }
        }

        // Translation picker sheet
        if (showTranslationPicker) {
            TranslationPickerSheet(
                viewModel = bibleViewModel,
                onDismiss = { showTranslationPicker = false },
            )
        }

        // Search overlay
        AnimatedVisibility(
            visible = isSearching,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(150)),
        ) {
            BibleSearchOverlay(
                searchText = searchText,
                onSearchTextChange = { searchText = it },
                searchResults = searchResults,
                onBookSelected = { book ->
                    searchText = ""
                    isSearching = false
                    onBookSelected(book)
                },
                onClose = {
                    searchText = ""
                    isSearching = false
                },
            )
        }
    }
}

// ── Search Overlay ──────────────────────────────────────────────────
@Composable
private fun BibleSearchOverlay(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    searchResults: List<BibleBook>,
    onBookSelected: (BibleBook) -> Unit,
    onClose: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NearBlack)
            .statusBarsPadding(),
    ) {
        // Search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (searchText.isEmpty()) {
                        Text(
                            text = stringResource(R.string.bible_search_books),
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.35f),
                        )
                    }
                    BasicTextField(
                        value = searchText,
                        onValueChange = onSearchTextChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            color = Color.White,
                        ),
                        cursorBrush = SolidColor(SoftGold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                    )
                }

                if (searchText.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchTextChange("") },
                        modifier = Modifier.size(20.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            // Close button
            IconButton(
                onClick = {
                    keyboardController?.hide()
                    onClose()
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f)),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.done),
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // Results
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (searchText.isEmpty()) {
                // Hint
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.bible_search_hint),
                            fontSize = 15.sp,
                            color = Color.White.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else if (searchResults.isEmpty()) {
                // No results
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SearchOff,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.bible_no_results, searchText),
                            fontSize = 15.sp,
                            color = Color.White.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                items(searchResults, key = { it.id }) { book ->
                    SearchResultRow(
                        book = book,
                        onClick = { onBookSelected(book) },
                    )
                }
            }

            // Bottom spacer for nav bar
            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}

// ── Search Result Row ───────────────────────────────────────────────
@Composable
private fun SearchResultRow(
    book: BibleBook,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BookCardBg)
            .border(1.dp, BookCardBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Section icon circle
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(SoftGold.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = book.section.icon,
                contentDescription = null,
                tint = SoftGold,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(Modifier.width(14.dp))

        // Book name + section info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = book.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(book.section.displayNameRes),
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.5f),
                )
                Text(
                    text = " · ",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.3f),
                )
                Text(
                    text = if (book.section.testament == Testament.OLD)
                        stringResource(R.string.bible_old_testament)
                    else
                        stringResource(R.string.bible_new_testament),
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Abbreviation + chevron
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = book.abbreviation,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = SoftGold,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

// ── Testament Picker (Old / New) ────────────────────────────────────
@Composable
private fun TestamentPicker(
    selected: Testament,
    onSelect: (Testament) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .width(190.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.25f),
                shape = RoundedCornerShape(20.dp),
            )
            .padding(3.dp),
    ) {
        Testament.entries.forEach { testament ->
            val isSelected = testament == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(17.dp))
                    .background(
                        if (isSelected) Color.White.copy(alpha = 0.2f)
                        else Color.Transparent,
                    )
                    .clickable { onSelect(testament) }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(
                        if (testament == Testament.OLD) R.string.bible_old
                        else R.string.bible_new,
                    ),
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = Color.White,
                )
            }
        }
    }
}

// ── Book Card ───────────────────────────────────────────────────────
@Composable
private fun BookCard(
    book: BibleBook,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(BookCardBg)
            .border(1.dp, BookCardBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = book.abbreviation,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = SoftGold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = book.name,
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.7f),
        )
    }
}

// ── Shimmer Book Card ──────────────────────────────────────────────
@Composable
private fun ShimmerBookCard(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val offsetX by transition.animateFloat(
        initialValue = -200f,
        targetValue = 200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_offset",
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color.Transparent,
            Color.White.copy(alpha = 0.1f),
            Color.Transparent,
        ),
        start = Offset(offsetX, 0f),
        end = Offset(offsetX + 200f, 0f),
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .clipToBounds()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Abbreviation placeholder
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .background(shimmerBrush),
        )
        Spacer(Modifier.height(6.dp))
        // Name placeholder
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .background(shimmerBrush),
        )
    }
}
