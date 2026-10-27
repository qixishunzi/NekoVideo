package com.neko.video

import com.neko.video.util.MediaSearch
import com.neko.video.util.SearchMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaSearchTest {
    private val courseVideo = "[01]--1V1 咨询.mp4"

    @Test
    fun normalizeFoldsCaseFullWidthAndPunctuation() {
        assertEquals("011v1咨询mp4", MediaSearch.normalize(courseVideo))
        assertEquals("chapter01abc", MediaSearch.normalize("ＣＨＡＰＴＥＲ（01） ＡＢＣ"))
        assertEquals("第1章操作系统mkv", MediaSearch.normalize("第 1 章 - 操作系统 .mkv"))
    }

    @Test
    fun exactMatchesSeparatorsAndCase() {
        assertTrue(MediaSearch.matches(courseVideo, null, "01 1v1", SearchMode.EXACT))
        assertTrue(MediaSearch.matches(courseVideo, null, "011V1", SearchMode.EXACT))
        assertTrue(MediaSearch.matches(courseVideo, null, "咨询", SearchMode.EXACT))
    }

    @Test
    fun exactRequiresEveryWordToAppear() {
        assertFalse(MediaSearch.matches(courseVideo, null, "01 1v2", SearchMode.EXACT))
        assertFalse(MediaSearch.matches("操作系统 第1章.mp4", null, "操作系统 第2章", SearchMode.EXACT))
    }

    @Test
    fun fuzzyAllowsSkippedCharactersExactDoesNot() {
        // 1v咨询 skips the "1" between "1v" and "咨询".
        assertTrue(MediaSearch.matches(courseVideo, null, "1v咨询", SearchMode.FUZZY))
        assertFalse(MediaSearch.matches(courseVideo, null, "1v咨询", SearchMode.EXACT))
    }

    @Test
    fun fuzzyFallsBackToSubstringForShortWords() {
        // "0n" is short enough that subsequence matching would match almost anything.
        assertFalse(MediaSearch.matches("操作系统 第1章.mp4", null, "0n", SearchMode.FUZZY))
        assertFalse(MediaSearch.matches(courseVideo, null, "0n", SearchMode.FUZZY))
    }

    @Test
    fun fuzzyIsCaseAndWidthInsensitive() {
        assertTrue(MediaSearch.matches("ＡＢＣ 考研英语.mp4", null, "abc", SearchMode.FUZZY))
        assertTrue(MediaSearch.matches("ＡＢＣ 考研英语.mp4", null, "考研", SearchMode.FUZZY))
    }

    @Test
    fun matchesChineseFragmentInsideName() {
        val name = "第3章 操作系统 进程管理.mp4"
        assertTrue(MediaSearch.matches(name, null, "操作系统", SearchMode.EXACT))
        assertTrue(MediaSearch.matches(name, null, "操作系统", SearchMode.FUZZY))
    }

    @Test
    fun fallsBackToDirectoryPathWhenNameMisses() {
        val path = "/storage/emulated/0/课程/操作系统/第一章/ch1.mp4"
        assertTrue(MediaSearch.matches("ch1.mp4", path, "操作系统", SearchMode.EXACT))
        assertFalse(MediaSearch.matches("ch1.mp4", "/storage/emulated/0/课程/ch1.mp4", "操作系统", SearchMode.FUZZY))
    }

    @Test
    fun blankQueryMatchesEverything() {
        assertTrue(MediaSearch.matches(courseVideo, null, "", SearchMode.EXACT))
        assertTrue(MediaSearch.matches(courseVideo, null, "   ", SearchMode.FUZZY))
        assertTrue(MediaSearch.matches(courseVideo, null, "[ ] () -_", SearchMode.EXACT))
    }

    @Test
    fun overlongQueryDoesNotCrash() {
        assertFalse(MediaSearch.matches(courseVideo, null, "a".repeat(10_000), SearchMode.FUZZY))
        assertFalse(MediaSearch.matches(courseVideo, null, "咨询".repeat(2_000), SearchMode.EXACT))
    }

    @Test
    fun handlesNullNameAndPath() {
        assertFalse(MediaSearch.matches(null, null, "abc", SearchMode.FUZZY))
        assertTrue(MediaSearch.matches(null, null, "", SearchMode.FUZZY))
    }
}
