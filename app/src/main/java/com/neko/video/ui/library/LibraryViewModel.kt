package com.neko.video.ui.library

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neko.video.asNekoVideoApp
import com.neko.video.model.MediaFolder
import com.neko.video.model.MediaItemInfo
import com.neko.video.model.MediaSort
import com.neko.video.model.MediaSortField
import com.neko.video.util.MediaSearch
import com.neko.video.util.NaturalOrder
import com.neko.video.util.SearchMode
import com.neko.video.util.isUnderDirectory
import com.neko.video.util.parentDirectoryOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LibraryTab { RECENT, FOLDERS, ALL }

/** Which videos the search box looks at. */
enum class SearchScope {
    /** Every scanned video, wherever it lives. */
    ALL,

    /** Only videos inside the folder currently being browsed. */
    CURRENT_FOLDER,
}

/** State of the library search. Matching itself lives in `util/MediaSearch`. */
data class SearchState(
    val active: Boolean = false,
    val query: String = "",
    val mode: SearchMode = SearchMode.FUZZY,
    val scope: SearchScope = SearchScope.ALL,
)

data class LibraryUiState(
    val tab: LibraryTab = LibraryTab.ALL,
    val isLoading: Boolean = false,
    val videos: List<MediaItemInfo> = emptyList(),
    /** Top-level folder entries of the library tree, already sorted. */
    val topFolders: List<MediaFolder> = emptyList(),
    /** Folder currently being browsed; null while at the top level. */
    val currentFolder: MediaFolder? = null,
    /** Sub-folders of [currentFolder], already sorted. */
    val childFolders: List<MediaFolder> = emptyList(),
    /** Videos stored directly in [currentFolder], already sorted. */
    val folderVideos: List<MediaItemInfo> = emptyList(),
    val sort: MediaSort = MediaSort.DEFAULT,
    /** Search box state; while [SearchState.active] the screen shows [searchResults]. */
    val search: SearchState = SearchState(),
    /** Videos matching the current query, already sorted like every other list. */
    val searchResults: List<MediaItemInfo> = emptyList(),
    val error: String? = null,
    val hasPermission: Boolean = false,
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application.asNekoVideoApp()
    private val repository = app.container.mediaRepository
    private val settings = app.container.settings

    private var scanJob: Job? = null
    private var importJob: Job? = null

    /** Unfiltered scan results; [uiState] holds the sorted, folder-scoped view of them. */
    private var allVideos: List<MediaItemInfo> = LocalMediaCache.videos.orEmpty()
    private var allFolders: List<MediaFolder> = LocalMediaCache.folders.orEmpty()
    private var currentFolderPath: String? = null
    private var sort: MediaSort = MediaSort.DEFAULT
    private var search: SearchState = SearchState()

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    val recent = repository.recentHistoryFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    init {
        rebuild()
        // Sort choice is a persisted preference, so follow it like any other setting.
        viewModelScope.launch {
            settings.settingsFlow.collect { state ->
                if (state.librarySort != sort) {
                    sort = state.librarySort
                    rebuild()
                }
            }
        }
    }

    fun setPermissionGranted(granted: Boolean) {
        _uiState.update { it.copy(hasPermission = granted) }
        if (granted) {
            // Re-entering the tab must not rescan; only load once per process (or after clear).
            ensureLoaded()
        }
    }

    fun selectTab(tab: LibraryTab) {
        if (tab != LibraryTab.FOLDERS) currentFolderPath = null
        _uiState.update { it.copy(tab = tab) }
        rebuild()
    }

    /** Load from memory cache if present; otherwise scan once. */
    fun ensureLoaded() {
        val cachedVideos = LocalMediaCache.videos
        val cachedFolders = LocalMediaCache.folders
        if (cachedVideos != null && cachedFolders != null) {
            allVideos = cachedVideos
            allFolders = cachedFolders
            _uiState.update { it.copy(isLoading = false, error = null) }
            rebuild()
            return
        }
        refresh(force = true)
    }

    fun refresh(force: Boolean = true) {
        if (!force && LocalMediaCache.isReady) {
            ensureLoaded()
            return
        }
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { repository.loadLocalVideos() }
                .onSuccess { videos ->
                    val folders = repository.loadLocalFolders(videos)
                    LocalMediaCache.update(videos, folders)
                    allVideos = videos
                    allFolders = folders
                    _uiState.update { it.copy(isLoading = false) }
                    rebuild()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message)
                    }
                }
        }
    }

    /** Enter a folder: its sub-folders and its own videos replace the flat list. */
    fun openFolder(folder: MediaFolder) {
        currentFolderPath = folder.path
        _uiState.update { it.copy(tab = LibraryTab.FOLDERS) }
        rebuild()
    }

    /** Go one level up; from a top-level folder this returns to the folder grid. */
    fun openParentFolder() {
        val current = currentFolderPath ?: return
        val parent = allFolders.firstOrNull { it.path == current }?.parentPath
        currentFolderPath = parent?.takeIf { candidate -> allFolders.any { it.path == candidate } }
        rebuild()
    }

    fun setSort(next: MediaSort) {
        if (next == sort) return
        sort = next
        rebuild()
        viewModelScope.launch {
            settings.update { it.copy(librarySort = next) }
        }
    }

    /** Enter search mode; inside a folder the default scope is that folder. */
    fun openSearch() {
        search = SearchState(
            active = true,
            scope = if (currentFolderPath != null) SearchScope.CURRENT_FOLDER else SearchScope.ALL,
        )
        rebuild()
    }

    /** Leave search mode and restore the previous view (folder position and sort stay). */
    fun closeSearch() {
        if (!search.active) return
        search = SearchState()
        rebuild()
    }

    fun setSearchQuery(query: String) {
        if (search.query == query) return
        search = search.copy(query = query)
        rebuild()
    }

    fun setSearchMode(mode: SearchMode) {
        if (search.mode == mode) return
        search = search.copy(mode = mode)
        rebuild()
    }

    fun setSearchScope(scope: SearchScope) {
        if (search.scope == scope) return
        search = search.copy(scope = scope)
        rebuild()
    }

    fun importTree(uri: Uri) {
        importJob?.cancel()
        importJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { repository.loadFromTreeUri(uri) }
                .onSuccess { imported ->
                    val merged = (allVideos + imported).distinctBy { it.uri }
                    val folders = repository.loadLocalFolders(merged)
                    LocalMediaCache.update(merged, folders)
                    allVideos = merged
                    allFolders = folders
                    currentFolderPath = null
                    _uiState.update {
                        it.copy(isLoading = false, tab = LibraryTab.ALL)
                    }
                    rebuild()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun mediaFromPickedUri(uri: Uri, name: String?): MediaItemInfo {
        return MediaItemInfo(
            id = "picked_${uri.hashCode()}",
            name = name ?: uri.lastPathSegment ?: "video",
            uri = uri.toString(),
            sourceType = com.neko.video.model.SourceType.LOCAL,
            thumbnailUri = uri.toString(),
        )
    }

    /**
     * Recomputes the visible lists from the raw scan plus the current folder and sort choice.
     * Sorting lives here (not in the composables) so every list stays consistent.
     */
    private fun rebuild(error: String? = null) {
        if (currentFolderPath != null &&
            allFolders.isNotEmpty() &&
            allFolders.none { it.path == currentFolderPath }
        ) {
            // The folder disappeared after a rescan.
            currentFolderPath = null
        }
        val path = currentFolderPath
        val folder = path?.let { current -> allFolders.firstOrNull { it.path == current } }
        val searchState = search

        _uiState.update {
            it.copy(
                videos = sortedVideos(allVideos),
                topFolders = sortedFolders(allFolders.filter { item -> item.parentPath == null }),
                currentFolder = folder,
                childFolders = if (path == null) {
                    emptyList()
                } else {
                    sortedFolders(allFolders.filter { item -> item.parentPath == path })
                },
                folderVideos = if (path == null) {
                    emptyList()
                } else {
                    sortedVideos(
                        allVideos.filter { item -> parentDirectoryOf(item.path.orEmpty()) == path },
                    )
                },
                sort = sort,
                search = searchState,
                searchResults = if (searchState.active) {
                    computeSearchResults(searchState, path)
                } else {
                    emptyList()
                },
                error = error,
            )
        }
    }

    /**
     * In-memory filter over the already scanned library: no Room / MediaStore round trip, and the
     * result keeps the user's current sort because it goes through [sortedVideos].
     */
    private fun computeSearchResults(state: SearchState, path: String?): List<MediaItemInfo> {
        if (state.query.isBlank()) return emptyList()
        val pool = when (state.scope) {
            SearchScope.ALL -> allVideos
            SearchScope.CURRENT_FOLDER -> path?.let { dir ->
                allVideos.filter { item -> item.path?.let { isUnderDirectory(it, dir) } == true }
            } ?: allVideos
        }
        return sortedVideos(
            pool.filter { item -> MediaSearch.matches(item, state.query, state.mode) },
        )
    }

    private fun sortedVideos(items: List<MediaItemInfo>): List<MediaItemInfo> {
        val primary: Comparator<MediaItemInfo> = when (sort.field) {
            MediaSortField.NAME -> Comparator { a, b -> NaturalOrder.compare(a.name, b.name) }
            MediaSortField.DATE -> Comparator { a, b -> a.modifiedTime.compareTo(b.modifiedTime) }
            MediaSortField.SIZE -> Comparator { a, b -> a.size.compareTo(b.size) }
            MediaSortField.DURATION -> Comparator { a, b -> a.duration.compareTo(b.duration) }
        }
        return applyOrder(primary.thenBy(NaturalOrder) { it.name }, items)
    }

    private fun sortedFolders(items: List<MediaFolder>): List<MediaFolder> {
        val primary: Comparator<MediaFolder> = when (sort.field) {
            MediaSortField.NAME -> Comparator { a, b -> NaturalOrder.compare(a.name, b.name) }
            MediaSortField.DATE -> Comparator { a, b ->
                a.latestModifiedTime.compareTo(b.latestModifiedTime)
            }
            MediaSortField.SIZE -> Comparator { a, b -> a.totalSize.compareTo(b.totalSize) }
            MediaSortField.DURATION -> Comparator { a, b ->
                a.totalDuration.compareTo(b.totalDuration)
            }
        }
        return applyOrder(primary.thenBy(NaturalOrder) { it.name }, items)
    }

    private fun <T> applyOrder(comparator: Comparator<T>, items: List<T>): List<T> =
        if (sort.ascending) items.sortedWith(comparator) else items.sortedWith(comparator.reversed())
}
