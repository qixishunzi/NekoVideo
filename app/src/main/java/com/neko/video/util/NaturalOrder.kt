package com.neko.video.util

/**
 * Human-friendly "natural" string ordering.
 *
 * Runs of digits compare by numeric value instead of character by character, so course
 * folders such as `2.1.9`, `2.1.10`, `第2章`, `第10章` and `01.开营直播` sort the way people
 * expect. Everything else compares case-insensitively and falls back to code-point order.
 */
object NaturalOrder : Comparator<String> {
    override fun compare(a: String, b: String): Int = naturalCompare(a, b)
}

fun naturalCompare(a: String, b: String): Int {
    var i = 0
    var j = 0
    while (i < a.length && j < b.length) {
        val ca = a[i]
        val cb = b[j]
        if (ca.isDigit() && cb.isDigit()) {
            var endA = i
            while (endA < a.length && a[endA].isDigit()) endA++
            var endB = j
            while (endB < b.length && b[endB].isDigit()) endB++

            // Compare digit runs by value: strip leading zeros, then longer run = larger value.
            val digitsA = a.substring(i, endA).trimStart('0')
            val digitsB = b.substring(j, endB).trimStart('0')
            val byLength = digitsA.length.compareTo(digitsB.length)
            if (byLength != 0) return byLength
            val byValue = digitsA.compareTo(digitsB)
            if (byValue != 0) return byValue

            i = endA
            j = endB
            continue
        }

        val lowerA = ca.lowercaseChar()
        val lowerB = cb.lowercaseChar()
        if (lowerA != lowerB) return lowerA.compareTo(lowerB)
        i++
        j++
    }
    return (a.length - i).compareTo(b.length - j)
}
