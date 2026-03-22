package com.app.lumen.features.bible.ui

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.app.lumen.R
import com.app.lumen.features.bible.model.*
import com.app.lumen.features.bible.viewmodel.BibleViewModel
import com.app.lumen.features.bible.viewmodel.LoadingState
import com.app.lumen.ui.theme.Slate
import com.app.lumen.ui.theme.SoftGold

private val SheetBg = Color(0xFF121220)
private val DividerColor = Color.White.copy(alpha = 0.08f)
private val PanelBg = Color(0xFF23233D)

private val placeholderBible = BibleVersion(
    id = "_placeholder",
    name = "",
    language = BibleLanguage(id = "", name = ""),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationPickerSheet(
    viewModel: BibleViewModel,
    onDismiss: () -> Unit,
) {
    val versions by viewModel.bibleVersions.collectAsState()
    val selectedBible by viewModel.selectedBible.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val loadingState by viewModel.loadingState.collectAsState()
    val error by viewModel.error.collectAsState()

    var showLanguageMenu by remember { mutableStateOf(false) }

    val displayBibles = remember(versions, selectedLanguage) {
        pickCatholicAndProtestant(versions, selectedLanguage)
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SheetBg,
        contentColor = Color.White,
        scrimColor = Color.Transparent,
        dragHandle = null,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Toolbar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 12.dp),
            ) {
                // Done button
                Text(
                    text = stringResource(R.string.done),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SoftGold,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(
                            0.5.dp,
                            Color.White.copy(alpha = 0.15f),
                            RoundedCornerShape(20.dp),
                        )
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )

                // Title
                Text(
                    text = stringResource(R.string.translation),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center),
                )

                // Language picker button
                Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(
                                0.5.dp,
                                Color.White.copy(alpha = 0.15f),
                                RoundedCornerShape(12.dp),
                            )
                            .clickable { showLanguageMenu = !showLanguageMenu }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Language,
                            contentDescription = stringResource(R.string.cd_language),
                            tint = SoftGold,
                            modifier = Modifier.size(18.dp),
                        )
                        Icon(
                            imageVector = Icons.Filled.UnfoldMore,
                            contentDescription = null,
                            tint = SoftGold,
                            modifier = Modifier.size(14.dp),
                        )
                    }

                    // Glass language popup (opens upward)
                    if (showLanguageMenu) {
                        val density = LocalDensity.current
                        Popup(
                            alignment = Alignment.BottomEnd,
                            offset = IntOffset(0, with(density) { (-44).dp.roundToPx() }),
                            onDismissRequest = { showLanguageMenu = false },
                            properties = PopupProperties(focusable = true),
                        ) {
                            LanguageGlassPanel(
                                selectedLanguage = selectedLanguage,
                                onSelect = { lang ->
                                    viewModel.changeLanguage(lang)
                                    showLanguageMenu = false
                                },
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = DividerColor)

            // Content — fixed height with 2 placeholder rows to prevent layout jumps
            Box(modifier = Modifier.fillMaxWidth()) {
                // Always show list (or placeholders) to keep height stable
                TranslationList(
                    bibles = displayBibles.ifEmpty {
                        listOf(
                            DisplayBible(placeholderBible, R.string.catholic_bible),
                            DisplayBible(placeholderBible, R.string.protestant_bible),
                        )
                    },
                    selectedBible = selectedBible,
                    onSelect = { bible ->
                        if (loadingState == LoadingState.LOADED) {
                            viewModel.selectBible(bible)
                            onDismiss()
                        }
                    },
                    dimmed = loadingState != LoadingState.LOADED,
                )

                // Overlay spinner while loading
                if (loadingState == LoadingState.LOADING || loadingState == LoadingState.IDLE) {
                    Box(
                        modifier = Modifier
                            .matchParentSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = SoftGold, strokeWidth = 2.dp)
                    }
                }

                // Error overlay
                if (loadingState == LoadingState.ERROR) {
                    Box(
                        modifier = Modifier
                            .matchParentSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = error
                                    ?: stringResource(R.string.error_load_translations),
                                color = Slate,
                                fontSize = 14.sp,
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.retry),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SoftGold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .clickable { viewModel.loadBibleVersions() }
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Pick Catholic (Firebase) + Protestant only ──────────────────────

private data class DisplayBible(
    val bible: BibleVersion,
    @androidx.annotation.StringRes val labelRes: Int,
)

/** Best Protestant Bible ID per language (from API.Bible data). */
private val PROTESTANT_BIBLE_IDS = mapOf(
    "eng" to "de4e12af7f28f599-01",  // KJV (survives dedup over -02 variant)
    "pol" to "1c9761e0230da6e0-01",  // UBG (Updated Gdansk Bible)
    "spa" to "592420522e16049f-01",  // Reina Valera 1909
    "por" to "d63894c8d9a7a503-01",  // Biblia Livre Para Todos
    "fra" to "a93a92589195411f-01",  // Bible J.N. Darby
    "ita" to "41f25b97f468e10b-01",  // Diodati Bible 1885
    "deu" to "926aa5efbc5e04e2-01",  // German Luther Bible 1912
)

/** Firebase Bible IDs are short (e.g. "WEBC", "BT5"), API.Bible IDs are long UUIDs. */
private fun isFirebaseBible(id: String): Boolean = id.length <= 10

private fun pickCatholicAndProtestant(
    versions: List<BibleVersion>,
    language: BibleLanguageOption,
): List<DisplayBible> {
    if (versions.isEmpty()) return emptyList()

    val result = mutableListOf<DisplayBible>()

    // 1. Catholic Bible = Firebase Bible (always first)
    val catholic = versions
        .filter { isFirebaseBible(it.id) }
        .firstOrNull()
        ?: versions.find { it.tradition == BibleTradition.CATHOLIC }

    if (catholic != null) {
        result.add(DisplayBible(catholic, R.string.catholic_bible))
    }

    // 2. Protestant Bible = specific best pick per language
    val protestantId = PROTESTANT_BIBLE_IDS[language.code]
    val protestant = if (protestantId != null) {
        versions.find { it.id == protestantId }
    } else {
        // Fallback: find any Protestant Bible
        versions.find { it.tradition == BibleTradition.PROTESTANT && it.id != catholic?.id }
    }

    if (protestant != null) {
        result.add(DisplayBible(protestant, R.string.protestant_bible))
    }

    return result
}

// ── Translation List ────────────────────────────────────────────────

@Composable
private fun TranslationList(
    bibles: List<DisplayBible>,
    selectedBible: BibleVersion?,
    onSelect: (BibleVersion) -> Unit,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
) {
    val alpha = if (dimmed) 0.3f else 1f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
    ) {
        bibles.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !dimmed) { onSelect(item.bible) }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(item.labelRes),
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = alpha),
                    modifier = Modifier.weight(1f),
                )
                if (!dimmed && selectedBible?.id == item.bible.id) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = SoftGold,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            if (index < bibles.size - 1) {
                HorizontalDivider(
                    color = DividerColor,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}

// ── Language Glass Panel (rendered via Popup) ────────────────────────

@Composable
private fun LanguageGlassPanel(
    selectedLanguage: BibleLanguageOption,
    onSelect: (BibleLanguageOption) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(200.dp)
            .shadow(16.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(PanelBg)
            .border(
                0.5.dp,
                Color.White.copy(alpha = 0.12f),
                RoundedCornerShape(14.dp),
            ),
    ) {
        BibleLanguageOption.entries.forEachIndexed { index, lang ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(lang) }
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (selectedLanguage == lang) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = SoftGold,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    text = stringResource(lang.displayNameRes),
                    fontSize = 15.sp,
                    color = if (selectedLanguage == lang) SoftGold else Color.White,
                    fontWeight = if (selectedLanguage == lang) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
            if (index < BibleLanguageOption.entries.size - 1) {
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}
