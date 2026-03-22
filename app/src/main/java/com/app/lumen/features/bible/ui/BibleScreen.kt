package com.app.lumen.features.bible.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.lumen.R
import com.app.lumen.ui.theme.NearBlack
import com.app.lumen.ui.theme.Slate
import com.app.lumen.ui.theme.SoftGold

// ── Testament Selection ─────────────────────────────────────────────
enum class Testament {
    OLD, NEW
}

// ── Book Sections ───────────────────────────────────────────────────
enum class BibleBookSection(
    val displayName: String,
    val icon: ImageVector,
    val testament: Testament,
) {
    // Old Testament
    LAW("Law", Icons.Filled.Description, Testament.OLD),
    OT_HISTORY("History", Icons.AutoMirrored.Filled.MenuBook, Testament.OLD),
    POETRY("Poetry & Wisdom", Icons.Filled.MusicNote, Testament.OLD),
    MAJOR_PROPHETS("Major Prophets", Icons.Filled.RecordVoiceOver, Testament.OLD),
    MINOR_PROPHETS("Minor Prophets", Icons.Filled.RecordVoiceOver, Testament.OLD),

    // New Testament
    GOSPELS("Gospels", Icons.Filled.AutoStories, Testament.NEW),
    NT_HISTORY("History", Icons.AutoMirrored.Filled.DirectionsWalk, Testament.NEW),
    PAULINE_EPISTLES("Pauline Epistles", Icons.Filled.Email, Testament.NEW),
    GENERAL_EPISTLES("General Epistles", Icons.Filled.Email, Testament.NEW),
    PROPHECY("Prophecy", Icons.Filled.AutoAwesome, Testament.NEW),
}

// ── Bible Book ──────────────────────────────────────────────────────
data class BibleBook(
    val id: String,
    val abbreviation: String,
    val name: String,
    val section: BibleBookSection,
)

// ── Book Data ───────────────────────────────────────────────────────
private val allBooks = listOf(
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
) {
    var selectedTestament by remember { mutableStateOf(Testament.OLD) }

    val books = remember(selectedTestament) {
        allBooks.filter { it.section.testament == selectedTestament }
    }

    val sections = remember(selectedTestament) {
        BibleBookSection.entries.filter { it.testament == selectedTestament }
    }

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
                                onClick = { /* TODO: language picker */ },
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
                                onClick = { /* TODO: search */ },
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
                                    text = section.displayName,
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

                // Bottom spacer for tab bar
                item {
                    Spacer(Modifier.height(bottomPadding + 8.dp))
                }
            }
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
