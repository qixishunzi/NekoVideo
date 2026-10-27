package com.neko.video

import com.neko.video.util.commonAncestorDirectory
import com.neko.video.util.directoryNameOf
import com.neko.video.util.isStorageVolumeRoot
import com.neko.video.util.isUnderDirectory
import com.neko.video.util.parentDirectoryOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PathUtilsTest {
    @Test
    fun walksUpFilesystemPaths() {
        assertEquals(
            "/storage/emulated/0/Download/课程",
            parentDirectoryOf("/storage/emulated/0/Download/课程/01.第一章"),
        )
        assertEquals("/storage/emulated", parentDirectoryOf("/storage/emulated/0"))
        assertEquals("/", parentDirectoryOf("/storage"))
        assertNull(parentDirectoryOf("/"))
    }

    @Test
    fun walksUpSafDisplayPaths() {
        assertEquals("Download/课程", parentDirectoryOf("Download/课程/01.第一章"))
        assertEquals("Download", parentDirectoryOf("Download/课程"))
        assertNull(parentDirectoryOf("Download"))
    }

    @Test
    fun resolvesFolderLabelsAndMembership() {
        assertEquals("01.第一章", directoryNameOf("/storage/emulated/0/Download/课程/01.第一章"))
        assertTrue(isUnderDirectory("/a/b/c.mp4", "/a/b"))
        assertTrue(isUnderDirectory("/a/b", "/a/b"))
        assertFalse(isUnderDirectory("/a/bc/d.mp4", "/a/b"))
    }

    @Test
    fun detectsVolumeRoots() {
        assertTrue(isStorageVolumeRoot("/"))
        assertTrue(isStorageVolumeRoot("/storage/emulated/0"))
        assertTrue(isStorageVolumeRoot("/sdcard/"))
        assertFalse(isStorageVolumeRoot("/storage/emulated/0/Download"))
        assertFalse(isStorageVolumeRoot("Download"))
    }

    @Test
    fun findsDeepestSharedAncestor() {
        val dirs = listOf(
            "/storage/emulated/0/Download/课程/01.第一章",
            "/storage/emulated/0/Download/课程/02.第二章",
            "/storage/emulated/0/Download/课程/03.第三章",
        )

        assertEquals(
            "/storage/emulated/0/Download/课程",
            commonAncestorDirectory(dirs),
        )
    }

    @Test
    fun sharedAncestorOfUnrelatedBranchesIsEmpty() {
        val dirs = listOf("Download/课程", "Movies/旅行")

        assertEquals("", commonAncestorDirectory(dirs))
    }
}
