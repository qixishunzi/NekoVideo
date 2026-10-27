package com.neko.video.model

import android.net.Uri

enum class SourceType {
    LOCAL,
    HTTP,
    WEBDAV,
    SMB,
    SFTP,
    FTP,
}

data class MediaItemInfo(
    val id: String,
    val name: String,
    val uri: String,
    val path: String? = null,
    val sourceType: SourceType,
    val size: Long = 0L,
    val modifiedTime: Long = 0L,
    val mimeType: String? = null,
    val duration: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val thumbnailUri: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val subtitleCandidates: List<SubtitleCandidate> = emptyList(),
    val isDirectory: Boolean = false,
    val serverId: String? = null,
    val playbackPosition: Long = 0L,
    val lastPlayedAt: Long = 0L,
) {
    fun toUri(): Uri = Uri.parse(uri)

    val displayResolution: String?
        get() = if (width > 0 && height > 0) "${width}x$height" else null
}

data class SubtitleCandidate(
    val name: String,
    val uri: String,
    val language: String? = null,
    val mimeType: String? = null,
)

data class MediaFolder(
    val id: String,
    val name: String,
    val path: String,
    /** Videos stored directly in this folder. */
    val videoCount: Int,
    val thumbnailUri: String? = null,
    /** Parent folder path; `null` when this folder is a top-level entry of the library. */
    val parentPath: String? = null,
    /** Videos in this folder and in every folder below it. */
    val totalVideoCount: Int = videoCount,
    val latestModifiedTime: Long = 0L,
    val totalSize: Long = 0L,
    val totalDuration: Long = 0L,
)

/** Sort keys offered by the library sort sheet. */
enum class MediaSortField {
    NAME,
    DATE,
    SIZE,
    DURATION,
}

/**
 * Library sort choice. [MediaSortField.SIZE] / [MediaSortField.DURATION] map to the aggregated
 * totals for folders, so one selection drives both the folder grid and the video lists.
 */
data class MediaSort(
    val field: MediaSortField = MediaSortField.NAME,
    val ascending: Boolean = true,
) {
    companion object {
        val DEFAULT = MediaSort()

        /** Order of the options shown in the UI. */
        val OPTIONS: List<MediaSort> = listOf(
            MediaSort(MediaSortField.NAME, ascending = true),
            MediaSort(MediaSortField.NAME, ascending = false),
            MediaSort(MediaSortField.DATE, ascending = false),
            MediaSort(MediaSortField.DATE, ascending = true),
            MediaSort(MediaSortField.SIZE, ascending = false),
            MediaSort(MediaSortField.SIZE, ascending = true),
            MediaSort(MediaSortField.DURATION, ascending = false),
            MediaSort(MediaSortField.DURATION, ascending = true),
        )
    }
}

data class PlaybackRequest(
    val media: MediaItemInfo,
    val playlist: List<MediaItemInfo> = listOf(media),
    val startIndex: Int = 0,
    val startPositionMs: Long? = null,
)

enum class AspectRatioMode {
    FIT,
    FILL,
    CROP,
    STRETCH,
    RATIO_16_9,
    RATIO_4_3,
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class UserAgentPreset {
    APP_DEFAULT,
    ANDROID,
    CHROME_ANDROID,
    CUSTOM,
}

data class AudioTrackInfo(
    val groupIndex: Int,
    val trackIndex: Int,
    val name: String,
    val language: String?,
    val codecs: String?,
    val channelCount: Int?,
    val selected: Boolean,
)

data class SubtitleTrackInfo(
    val groupIndex: Int,
    val trackIndex: Int,
    val name: String,
    val language: String?,
    val selected: Boolean,
)
