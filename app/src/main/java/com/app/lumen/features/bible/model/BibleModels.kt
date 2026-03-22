package com.app.lumen.features.bible.model

import androidx.annotation.StringRes
import com.app.lumen.R
import kotlinx.serialization.Serializable
import java.util.Locale

// ── Bible Version ───────────────────────────────────────────────────
@Serializable
data class BibleVersion(
    val id: String,
    val dblId: String? = null,
    val abbreviation: String? = null,
    val abbreviationLocal: String? = null,
    val name: String,
    val nameLocal: String? = null,
    val description: String? = null,
    val descriptionLocal: String? = null,
    val language: BibleLanguage,
) {
    val displayName: String get() = nameLocal ?: name
    val displayAbbreviation: String get() = abbreviationLocal ?: abbreviation ?: id
    val tradition: BibleTradition get() = BibleTradition.from(name, descriptionLocal ?: description)
}

// ── Bible Language ──────────────────────────────────────────────────
@Serializable
data class BibleLanguage(
    val id: String,
    val name: String,
    val nameLocal: String? = null,
    val script: String? = null,
    val scriptDirection: String? = null,
) {
    val isRTL: Boolean get() = scriptDirection?.uppercase() == "RTL"
}

// ── Bible Tradition (for grouping) ──────────────────────────────────
enum class BibleTradition(val displayName: String, val sortOrder: Int) {
    CATHOLIC("Catholic", 0),
    ECUMENICAL("Ecumenical", 1),
    PROTESTANT("Protestant", 2),
    ORTHODOX("Orthodox", 3),
    MESSIANIC("Messianic", 4),
    JEWISH("Jewish", 5),
    OTHER("Other", 6);

    val infoMessage: String
        get() = when (this) {
            CATHOLIC -> "Catholic Bibles include 73 books: the 66 books of the Protestant canon plus 7 deuterocanonical books (Tobit, Judith, Wisdom, Sirach, Baruch, 1 Maccabees, 2 Maccabees) and additions to Esther and Daniel."
            ORTHODOX -> "Orthodox Bibles include all Catholic books plus additional texts such as 3-4 Maccabees, 1-2 Esdras, Prayer of Manasseh, and Psalm 151."
            PROTESTANT -> "Protestant Bibles contain 66 books: 39 in the Old Testament and 27 in the New Testament. They do not include the deuterocanonical books."
            ECUMENICAL -> "Ecumenical Bibles are designed to be suitable for Christians of all traditions. They typically include books from both Protestant and Catholic/Orthodox canons."
            MESSIANIC -> "Messianic Bibles are translations used by Messianic Jews who believe in Jesus (Yeshua) as the Messiah."
            JEWISH -> "Jewish texts include the Hebrew Bible (Tanakh) and related writings. The Tanakh contains 24 books and does not include the New Testament."
            OTHER -> ""
        }

    companion object {
        fun from(name: String, description: String?): BibleTradition {
            val nameLower = name.lowercase()
            val descLower = description?.lowercase() ?: ""

            // Check description for explicit tradition markers
            if (descLower.contains("catholic")) return CATHOLIC
            if (descLower.contains("orthodox") && !descLower.contains("jewish")) return ORTHODOX
            if (descLower.contains("protestant")) return PROTESTANT
            if (descLower.contains("ecumenical") || descLower.contains("interconfessional")) return ECUMENICAL
            if (descLower.contains("messianic")) return MESSIANIC
            if (descLower.contains("jewish")) return JEWISH

            // Check name for known Bibles
            if (nameLower.contains("berean") || nameLower.contains("geneva") ||
                nameLower.contains("american standard") || nameLower.contains("asv") ||
                nameLower.contains("king james") || nameLower.contains("kjv") ||
                nameLower.contains("cambridge paragraph") || nameLower.contains("free bible") ||
                nameLower.contains("unlocked literal")
            ) return PROTESTANT

            if (nameLower.contains("septuagint") || nameLower.contains("brenton")) return ORTHODOX
            if (nameLower.contains("orthodox jewish") || nameLower.contains("targum")) return JEWISH
            if (nameLower.contains("apocrypha") || nameLower.contains("asvbt")) return ECUMENICAL

            return OTHER
        }
    }
}

// ── Bible Language Option ───────────────────────────────────────────
enum class BibleLanguageOption(val code: String, @StringRes val displayNameRes: Int) {
    ENGLISH("eng", R.string.lang_english),
    SPANISH("spa", R.string.lang_spanish),
    PORTUGUESE("por", R.string.lang_portuguese),
    FRENCH("fra", R.string.lang_french),
    ITALIAN("ita", R.string.lang_italian),
    GERMAN("deu", R.string.lang_german),
    POLISH("pol", R.string.lang_polish);

    companion object {
        fun fromSystemLocale(): BibleLanguageOption {
            val lang = Locale.getDefault().language
            return when (lang) {
                "es" -> SPANISH
                "pt" -> PORTUGUESE
                "fr" -> FRENCH
                "it" -> ITALIAN
                "de" -> GERMAN
                "pl" -> POLISH
                else -> ENGLISH
            }
        }

        fun fromCode(code: String): BibleLanguageOption {
            return entries.find { it.code == code } ?: ENGLISH
        }
    }
}

// ── Chapter Models (for future reader use) ──────────────────────────
@Serializable
data class BibleChapterSummary(
    val id: String,
    val number: String,
    val reference: String,
)

@Serializable
data class BibleChapter(
    val id: String,
    val number: String,
    val content: String,
    val verseCount: Int = 0,
    val next: ChapterNav? = null,
    val previous: ChapterNav? = null,
)

@Serializable
data class ChapterNav(
    val id: String,
    val number: String,
)

// ── API Response Wrappers ───────────────────────────────────────────
@Serializable
data class BibleAPIResponse<T>(val data: T)

// ── Firebase Config Model ───────────────────────────────────────────
@Serializable
data class FirebaseBibleConfig(
    val id: String,
    val dblId: String? = null,
    val abbreviation: String? = null,
    val abbreviationLocal: String? = null,
    val name: String,
    val nameLocal: String? = null,
    val description: String? = null,
    val descriptionLocal: String? = null,
    val language: BibleLanguage,
    val books: List<FirebaseBookEntry> = emptyList(),
    val totalBooks: Int = 0,
    val totalChapters: Int = 0,
    val totalVerses: Int = 0,
    val version: Int = 1,
) {
    fun toBibleVersion(): BibleVersion = BibleVersion(
        id = id, dblId = dblId, abbreviation = abbreviation,
        abbreviationLocal = abbreviationLocal, name = name,
        nameLocal = nameLocal, description = description,
        descriptionLocal = descriptionLocal, language = language,
    )

    fun downloadUrl(bookId: String): String? =
        books.find { it.id == bookId }?.downloadUrl
}

@Serializable
data class FirebaseBookEntry(
    val id: String,
    val bibleId: String,
    val abbreviation: String,
    val name: String,
    val nameLong: String,
    val chapters: Int = 0,
    val verses: Int = 0,
    val storagePath: String = "",
    val downloadUrl: String = "",
)

@Serializable
data class FirebaseBookData(
    val chapterSummaries: List<BibleChapterSummary> = emptyList(),
    val chapters: Map<String, BibleChapter> = emptyMap(),
)
