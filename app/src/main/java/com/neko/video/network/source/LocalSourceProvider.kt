package com.neko.video.network.source

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.DocumentsContract
import com.neko.video.model.MediaFolder
import com.neko.video.model.MediaItemInfo
import com.neko.video.model.SourceType
import com.neko.video.model.SubtitleCandidate
import com.neko.video.util.FormatUtils
import com.neko.video.util.commonAncestorDirectory
import com.neko.video.util.directoryNameOf
import com.neko.video.util.isStorageVolumeRoot
import com.neko.video.util.isUnderDirectory
import com.neko.video.util.parentDirectoryOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LocalSourceProvider(
    private val context: Context,
) : MediaSourceProvider {
    override val sourceType: SourceType = SourceType.LOCAL

    override suspend fun list(path: String): List<MediaItemInfo> = loadAllVideos()

    suspend fun loadAllVideos(): List<MediaItemInfo> = withContext(Dispatchers.IO) {
        val result = mutableListOf<MediaItemInfo>()
        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.BUCKET_ID,
        )
        val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
        context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val modifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(collection, id)
                val path = cursor.getString(dataCol)
                val name = cursor.getString(nameCol) ?: "video_$id"
                result += MediaItemInfo(
                    id = "local_$id",
                    name = name,
                    uri = contentUri.toString(),
                    path = path,
                    sourceType = SourceType.LOCAL,
                    size = cursor.getLong(sizeCol),
                    modifiedTime = cursor.getLong(modifiedCol) * 1000L,
                    mimeType = cursor.getString(mimeCol),
                    duration = cursor.getLong(durationCol),
                    width = cursor.getInt(widthCol),
                    height = cursor.getInt(heightCol),
                    thumbnailUri = contentUri.toString(),
                    subtitleCandidates = findSiblingSubtitles(path, name),
                )
            }
        }
        result
    }

    /**
     * Builds the browsable folder tree for [videos].
     *
     * Every folder that directly holds videos becomes a node, together with the ancestors needed
     * to reach it, so nested chapters are entered from their course folder instead of being
     * flattened into the same list. The shared root of the tree is shown as one top-level entry;
     * storage volume roots (`/`, `/storage/emulated/0`, …) never are.
     */
    suspend fun loadFolders(videos: List<MediaItemInfo>): List<MediaFolder> =
        withContext(Dispatchers.Default) {
            val videosByDir = LinkedHashMap<String, MutableList<MediaItemInfo>>()
            videos.forEach { video ->
                val dir = video.path?.let(::parentDirectoryOf)?.takeIf { it.isNotBlank() }
                    ?: return@forEach
                videosByDir.getOrPut(dir) { mutableListOf() }.add(video)
            }
            if (videosByDir.isEmpty()) return@withContext emptyList()

            val root = commonAncestorDirectory(videosByDir.keys)
            val rootIsBrowsable = root.isNotBlank() && !isStorageVolumeRoot(root)

            // Node set: every video folder plus the ancestors between it and the root.
            val nodeDirs = LinkedHashSet<String>()
            videosByDir.keys.forEach { dir ->
                var current: String? = dir
                while (current != null && current.isNotBlank()) {
                    // A volume root stops the walk but never becomes a browsable entry.
                    if (current == root && !rootIsBrowsable) break
                    nodeDirs += current
                    if (current == root) break
                    current = parentDirectoryOf(current)
                }
            }

            // Collapse a single-child top chain (e.g. "Download/课程" when only one course exists),
            // so the first screen starts at a branch point instead of a pointless wrapper folder.
            var collapsed = true
            while (collapsed) {
                collapsed = false
                val tops = nodeDirs.filter { dir ->
                    parentDirectoryOf(dir)?.let { it in nodeDirs } != true
                }
                if (tops.size == 1) {
                    val only = tops.first()
                    val childCount = nodeDirs.count { parentDirectoryOf(it) == only }
                    val hasDirectVideos = videosByDir[only].orEmpty().isNotEmpty()
                    if (childCount == 1 && !hasDirectVideos) {
                        nodeDirs.remove(only)
                        collapsed = true
                    }
                }
            }

            nodeDirs.map { dir ->
                val direct = videosByDir[dir].orEmpty()
                val recursive = videos.filter { item ->
                    item.path?.let { isUnderDirectory(it, dir) } == true
                }
                MediaFolder(
                    id = dir,
                    name = directoryNameOf(dir),
                    path = dir,
                    videoCount = direct.size,
                    thumbnailUri = recursive.firstOrNull()?.thumbnailUri,
                    parentPath = parentDirectoryOf(dir)?.takeIf { it in nodeDirs },
                    totalVideoCount = recursive.size,
                    latestModifiedTime = recursive.maxOfOrNull { it.modifiedTime } ?: 0L,
                    totalSize = recursive.sumOf { it.size },
                    totalDuration = recursive.sumOf { it.duration },
                )
            }
        }

    suspend fun loadFromTreeUri(treeUri: Uri): List<MediaItemInfo> = withContext(Dispatchers.IO) {
        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val found = mutableListOf<MediaItemInfo>()
        walkTreeDocuments(
            treeUri = treeUri,
            parentDocId = rootDocId,
            parentPath = documentDisplayPath(treeUri, rootDocId),
            out = found,
        )
        found.sortedBy { it.name.lowercase() }
    }

    private fun walkTreeDocuments(
        treeUri: Uri,
        parentDocId: String,
        parentPath: String?,
        out: MutableList<MediaItemInfo>,
    ) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
        context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sizeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
            val modifiedCol =
                cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            while (cursor.moveToNext()) {
                val docId = cursor.getString(idCol)
                val name = cursor.getString(nameCol) ?: continue
                val mime = cursor.getString(mimeCol)
                val childPath = if (parentPath.isNullOrBlank()) name else "$parentPath/$name"
                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    walkTreeDocuments(treeUri, docId, childPath, out)
                    continue
                }
                if (!FormatUtils.isVideoFile(name, mime)) continue
                val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                out += MediaItemInfo(
                    id = "saf_${uri.hashCode()}",
                    name = name,
                    uri = uri.toString(),
                    // Synthetic display path for folder grouping (not a real filesystem path).
                    path = childPath,
                    sourceType = SourceType.LOCAL,
                    size = cursor.getLong(sizeCol),
                    modifiedTime = cursor.getLong(modifiedCol),
                    mimeType = mime,
                    thumbnailUri = uri.toString(),
                )
            }
        }
    }

    private fun documentDisplayPath(treeUri: Uri, docId: String): String? {
        // Prefer a stable folder label for grouping; SAF paths are not filesystem paths.
        val last = docId.substringAfterLast(':').substringAfterLast('/')
        return last.takeIf { it.isNotBlank() }
            ?: runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
    }

    private fun findSiblingSubtitles(path: String?, videoName: String): List<SubtitleCandidate> {
        if (path.isNullOrBlank()) return emptyList()
        val videoFile = File(path)
        val parent = videoFile.parentFile ?: return emptyList()
        val base = videoFile.nameWithoutExtension
        return parent.listFiles()
            ?.filter { FormatUtils.isSubtitleFile(it.name) && it.name.startsWith(base, ignoreCase = true) }
            ?.map {
                SubtitleCandidate(
                    name = it.name,
                    uri = Uri.fromFile(it).toString(),
                )
            }
            .orEmpty()
    }
}
