package com.neko.video.util

import com.neko.video.model.MediaItemInfo

/** How a query is matched against a video name. */
enum class SearchMode {
    /**
     * Every query word must appear in order but not necessarily next to each other
     * (fzf style): `1v咨询` still finds `[01]--1V1 咨询.mp4`.
     */
    FUZZY,

    /** Every query word must appear as one contiguous run of characters. */
    EXACT,
}

/**
 * Name matching for the library search box.
 *
 * Both modes compare [normalize]d text, so case, full-width characters and the punctuation
 * that video files are littered with (`[01]`, `_`, `-`, spaces) never block a hit.
 * Pure Kotlin on purpose: the matching rules are unit tested without an Android runtime.
 */
object MediaSearch {
    /**
     * Subsequence matching only kicks in from this length on; shorter words fall back to plain
     * substring matching, otherwise a query like `ab` would match nearly the whole library.
     */
    private const val FUZZY_MIN_LENGTH = 3

    private val WHITESPACE = Regex("\\s+")

    /** Separator between name and path so one query word never spans both. */
    private const val FIELD_SEPARATOR = '\u0000'

    fun matches(item: MediaItemInfo, rawQuery: String, mode: SearchMode): Boolean =
        matches(item.name, item.path, rawQuery, mode)

    fun matches(name: String?, path: String?, rawQuery: String, mode: SearchMode): Boolean {
        val tokens = tokens(rawQuery)
        if (tokens.isEmpty()) return true
        val haystack = buildString {
            append(normalize(name.orEmpty()))
            append(FIELD_SEPARATOR)
            append(normalize(path.orEmpty()))
        }
        return tokens.all { token -> matchesToken(haystack, token, mode) }
    }

    /**
     * Drops everything that is not a letter or a digit: case folding, full-width → half-width,
     * and stripping `[ ] ( ) - _ .` plus spacing all happen here.
     *
     * `[01]--1V1 咨询.mp4` becomes `011v1咨询mp4`, so the query `01 1v1` hits it.
     */
    fun normalize(text: String): String = buildString {
        for (raw in text) {
            val ch = toHalfWidth(raw).lowercaseChar()
            if (ch.isLetterOrDigit()) append(ch)
        }
    }

    private fun matchesToken(haystack: String, token: String, mode: SearchMode): Boolean = when {
        mode == SearchMode.EXACT -> haystack.contains(token)
        token.length < FUZZY_MIN_LENGTH -> haystack.contains(token)
        else -> isSubsequence(haystack, token)
    }

    private fun tokens(rawQuery: String): List<String> = rawQuery
        .trim()
        .split(WHITESPACE)
        .map(::normalize)
        .filter { it.isNotEmpty() }

    private fun isSubsequence(haystack: String, needle: String): Boolean {
        var matched = 0
        if (needle.isEmpty()) return true
        for (ch in haystack) {
            if (ch == needle[matched]) {
                matched++
                if (matched == needle.length) return true
            }
        }
        return false
    }

    /** Full-width ASCII (`Ａ`, `０`, `（`) and the ideographic space become their half-width forms. */
    private fun toHalfWidth(ch: Char): Char = when {
        ch.code == IDEOGRAPHIC_SPACE -> ' '
        ch.code in FULL_WIDTH_START..FULL_WIDTH_END -> (ch.code - FULL_WIDTH_OFFSET).toChar()
        else -> ch
    }

    private const val IDEOGRAPHIC_SPACE = 0x3000
    private const val FULL_WIDTH_START = 0xFF01
    private const val FULL_WIDTH_END = 0xFF5E
    private const val FULL_WIDTH_OFFSET = 0xFEE0
}
