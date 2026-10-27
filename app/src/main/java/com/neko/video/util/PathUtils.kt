package com.neko.video.util

/**
 * Path helpers shared by the local library scanner and the folder browser.
 *
 * Paths come from two places: real filesystem paths from `MediaStore` (`/storage/emulated/0/…`)
 * and synthetic display paths from the SAF folder import (`Download/课程/第一章`). Both use `/`
 * as separator, so the same helpers work for each of them.
 */

/** True when [path] looks like an absolute filesystem path (leading `/`). */
fun isAbsolutePath(path: String): Boolean = path.startsWith("/")

/** Parent directory of [path], or `null` when it has no parent segment left. */
fun parentDirectoryOf(path: String): String? {
    val normalised = path.trimEnd('/')
    if (normalised.isEmpty()) return null
    val index = normalised.lastIndexOf('/')
    return when {
        index < 0 -> null                 // "课程/第一章" -> top segment has no parent
        index == 0 -> "/"                 // "/storage" -> "/"
        else -> normalised.substring(0, index)
    }
}

/** Last segment of [path], used as the folder label. */
fun directoryNameOf(path: String): String {
    val normalised = path.trimEnd('/')
    return normalised.substringAfterLast('/').ifBlank { normalised }
}

/** True when [path] is [dir] itself or lives below it. */
fun isUnderDirectory(path: String, dir: String): Boolean {
    val parent = dir.trimEnd('/')
    return path == parent || path.startsWith("$parent/")
}

/**
 * Storage volume roots that must never show up as browsable folders: they are plumbing, not
 * something the user picked (`/`, `/storage/emulated/0`, `/sdcard`, …).
 */
fun isStorageVolumeRoot(path: String): Boolean {
    val normalised = path.trimEnd('/')
    if (normalised.isEmpty() || normalised == "/") return true
    if (normalised == "/storage" || normalised == "/storage/emulated" || normalised == "/sdcard") {
        return true
    }
    return VOLUME_ROOT_REGEX.matches(normalised)
}

/** Deepest directory that contains every entry of [dirs]; empty when they only share a prefix-less root. */
fun commonAncestorDirectory(dirs: Collection<String>): String {
    if (dirs.isEmpty()) return ""
    var shared = dirs.first().split('/')
    dirs.drop(1).forEach { dir ->
        val segments = dir.split('/')
        var matched = 0
        while (matched < shared.size && matched < segments.size && shared[matched] == segments[matched]) {
            matched++
        }
        shared = shared.subList(0, matched)
    }
    return shared.joinToString("/")
}

private val VOLUME_ROOT_REGEX = Regex("^/storage/[^/]+/[0-9]+$")
