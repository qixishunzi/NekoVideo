package com.neko.video

import com.neko.video.util.NaturalOrder
import org.junit.Assert.assertEquals
import org.junit.Test

class NaturalOrderTest {
    @Test
    fun ordersNumberedChaptersByValue() {
        val names = listOf(
            "2.1.10 信号量机制",
            "2.1.9 互斥锁",
            "2.1.2 进程互斥",
            "第10章",
            "第2章",
        )

        assertEquals(
            listOf("2.1.2 进程互斥", "2.1.9 互斥锁", "2.1.10 信号量机制", "第2章", "第10章"),
            names.sortedWith(NaturalOrder),
        )
    }

    @Test
    fun keepsCoursesWithZeroPaddedPrefixesInOrder() {
        val names = listOf("10.第十章", "02.第二章", "01.第一章", "00.开营直播")

        assertEquals(
            listOf("00.开营直播", "01.第一章", "02.第二章", "10.第十章"),
            names.sortedWith(NaturalOrder),
        )
    }

    @Test
    fun comparesCaseInsensitivelyAndFallsBackToLength() {
        assertEquals(0, NaturalOrder.compare("abc", "ABC"))
        assertEquals(-1, NaturalOrder.compare("ep1", "ep1-extra"))
        assertEquals(0, NaturalOrder.compare("01", "1"))
    }

    @Test
    fun reversedGivesDescendingOrder() {
        val names = listOf("a1", "a2", "a10")

        assertEquals(
            listOf("a10", "a2", "a1"),
            names.sortedWith(NaturalOrder.reversed()),
        )
    }
}
