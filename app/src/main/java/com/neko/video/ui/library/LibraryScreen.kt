package com.neko.video.ui.library

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.neko.video.R
import com.neko.video.model.MediaFolder
import com.neko.video.model.MediaItemInfo
import com.neko.video.model.MediaSort
import com.neko.video.model.MediaSortField
import com.neko.video.ui.component.GlassFloatingTabRow
import com.neko.video.ui.components.EmptyState
import com.neko.video.ui.components.LoadingState
import com.neko.video.ui.components.MiuixPage
import com.neko.video.ui.components.PrimaryButton
import com.neko.video.ui.components.SecondaryButton
import com.neko.video.ui.theme.LocalMainBottomContentPadding
import com.neko.video.util.FormatUtils
import com.neko.video.util.SearchMode
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun LibraryScreen(
    onPlay: (MediaItemInfo) -> Unit,
    viewModel: LibraryViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val recent by viewModel.recent.collectAsStateWithLifecycle()
    var showSortSheet by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.setPermissionGranted(granted)
    }

    val pickVideoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            onPlay(viewModel.mediaFromPickedUri(uri, null))
        }
    }

    val pickFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            viewModel.importTree(uri)
        }
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_VIDEO,
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.setPermissionGranted(granted)
        if (!granted) {
            permissionLauncher.launch(Manifest.permission.READ_MEDIA_VIDEO)
        }
    }

    val tabs = listOf(
        stringResource(R.string.library_recent),
        stringResource(R.string.library_folders),
        stringResource(R.string.library_all_videos),
    )

    val layoutDirection = LocalLayoutDirection.current
    val surfaceColor = MiuixTheme.colorScheme.surface
    // Same pattern as the floating bottom bar: content is the blur source, pill floats on top.
    val tabGlassBackdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
    // Pill height + vertical gaps reserved so list content can scroll under the glass tabs.
    val floatingTabInset = 56.dp + 16.dp
    // Same idea for the floating search panel: field row + mode chips + gaps.
    val searchPanelInset = 132.dp

    MiuixPage(
        title = stringResource(R.string.library_title),
        actions = {
            IconButton(onClick = { pickVideoLauncher.launch(arrayOf("video/*")) }) {
                Icon(Icons.Outlined.VideoFile, contentDescription = stringResource(R.string.library_pick_video))
            }
            IconButton(onClick = { pickFolderLauncher.launch(null) }) {
                Icon(Icons.Outlined.FolderOpen, contentDescription = stringResource(R.string.library_pick_folder))
            }
            IconButton(onClick = { viewModel.refresh(force = true) }) {
                Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.library_refresh))
            }
            IconButton(
                onClick = {
                    if (state.search.active) viewModel.closeSearch() else viewModel.openSearch()
                },
            ) {
                Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.library_search))
            }
            IconButton(onClick = { showSortSheet = true }) {
                Icon(
                    Icons.AutoMirrored.Outlined.Sort,
                    contentDescription = stringResource(R.string.library_sort),
                )
            }
        },
    ) { padding, nestedScrollConnection ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = padding.calculateStartPadding(layoutDirection),
                    top = padding.calculateTopPadding(),
                    end = padding.calculateEndPadding(layoutDirection),
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(tabGlassBackdrop),
            ) {
                when {
                    state.isLoading -> LoadingState(stringResource(R.string.library_scanning))
                    state.error != null && state.videos.isEmpty() -> {
                        EmptyState(
                            title = stringResource(R.string.error_connection),
                            message = stringResource(R.string.error_generic, state.error.orEmpty()),
                            action = {
                                PrimaryButton(
                                    text = stringResource(R.string.action_retry),
                                    onClick = { viewModel.refresh(force = true) },
                                )
                            },
                        )
                    }
                    !state.hasPermission && state.videos.isEmpty() -> {
                        EmptyState(
                            title = stringResource(R.string.library_empty),
                            message = stringResource(R.string.library_empty_hint),
                            action = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    PrimaryButton(
                                        text = stringResource(R.string.library_grant_permission),
                                        onClick = {
                                            permissionLauncher.launch(Manifest.permission.READ_MEDIA_VIDEO)
                                        },
                                    )
                                    SecondaryButton(
                                        text = stringResource(R.string.library_pick_video),
                                        onClick = { pickVideoLauncher.launch(arrayOf("video/*")) },
                                    )
                                }
                            },
                        )
                    }
                    state.search.active -> {
                        SearchResults(
                            state = state,
                            onPlay = onPlay,
                            nestedScrollConnection = nestedScrollConnection,
                            topContentPadding = searchPanelInset,
                        )
                    }
                    else -> {
                        AnimatedContent(
                            targetState = state.tab to state.currentFolder?.id,
                            transitionSpec = {
                                val forward = targetState.first.ordinal >= initialState.first.ordinal
                                val enter = fadeIn() + slideInHorizontally { full ->
                                    if (forward) full / 5 else -full / 5
                                }
                                val exit = fadeOut() + slideOutHorizontally { full ->
                                    if (forward) -full / 5 else full / 5
                                }
                                enter togetherWith exit
                            },
                            label = "libraryTab",
                            modifier = Modifier.fillMaxSize(),
                        ) { (tab, _) ->
                            when (tab) {
                                LibraryTab.RECENT -> {
                                    if (recent.isEmpty()) {
                                        EmptyState(
                                            title = stringResource(R.string.history_empty),
                                            message = stringResource(R.string.history_empty_hint),
                                        )
                                    } else {
                                        VideoList(
                                            items = recent,
                                            onPlay = onPlay,
                                            nestedScrollConnection = nestedScrollConnection,
                                            topContentPadding = floatingTabInset,
                                        )
                                    }
                                }
                                LibraryTab.FOLDERS -> {
                                    val currentFolder = state.currentFolder
                                    if (currentFolder == null) {
                                        if (state.topFolders.isEmpty()) {
                                            EmptyState(
                                                title = stringResource(R.string.library_empty),
                                                message = stringResource(R.string.library_empty_hint),
                                            )
                                        } else {
                                            FolderGrid(
                                                folders = state.topFolders,
                                                onOpen = viewModel::openFolder,
                                                nestedScrollConnection = nestedScrollConnection,
                                                topContentPadding = floatingTabInset,
                                            )
                                        }
                                    } else {
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            FolderHeader(
                                                folder = currentFolder,
                                                onBack = viewModel::openParentFolder,
                                                topPadding = floatingTabInset,
                                            )
                                            FolderContents(
                                                folders = state.childFolders,
                                                videos = state.folderVideos,
                                                onOpenFolder = viewModel::openFolder,
                                                onPlay = onPlay,
                                                nestedScrollConnection = nestedScrollConnection,
                                            )
                                        }
                                    }
                                }
                                LibraryTab.ALL -> {
                                    if (state.videos.isEmpty()) {
                                        EmptyState(
                                            title = stringResource(R.string.library_empty),
                                            message = stringResource(R.string.library_empty_hint),
                                            action = {
                                                PrimaryButton(
                                                    text = stringResource(R.string.library_pick_video),
                                                    onClick = {
                                                        pickVideoLauncher.launch(arrayOf("video/*"))
                                                    },
                                                )
                                            },
                                        )
                                    } else {
                                        VideoList(
                                            items = state.videos,
                                            onPlay = onPlay,
                                            nestedScrollConnection = nestedScrollConnection,
                                            topContentPadding = floatingTabInset,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (state.search.active) {
                LibrarySearchPanel(
                    state = state.search,
                    hasFolder = state.currentFolder != null,
                    onQueryChange = viewModel::setSearchQuery,
                    onModeChange = viewModel::setSearchMode,
                    onScopeChange = viewModel::setSearchScope,
                    onClose = viewModel::closeSearch,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp),
                )
            } else {
                GlassFloatingTabRow(
                    tabs = tabs,
                    selectedTabIndex = state.tab.ordinal,
                    onTabSelected = { index ->
                        viewModel.selectTab(LibraryTab.entries[index])
                    },
                    backdrop = tabGlassBackdrop,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp),
                )
            }
        }
    }

    LibrarySortSheet(
        visible = showSortSheet,
        sort = state.sort,
        onDismiss = { showSortSheet = false },
        onSelect = viewModel::setSort,
    )
}

@Composable
private fun VideoList(
    items: List<MediaItemInfo>,
    onPlay: (MediaItemInfo) -> Unit,
    nestedScrollConnection: NestedScrollConnection,
    topContentPadding: Dp = 8.dp,
) {
    val bottomPadding = LocalMainBottomContentPadding.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .scrollEndHaptic()
            .overScrollVertical()
            .nestedScroll(nestedScrollConnection),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = topContentPadding,
            bottom = 8.dp + bottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        overscrollEffect = null,
    ) {
        items(items, key = { it.id }) { item ->
            Card(onClick = { onPlay(item) }) {
                VideoRow(item = item)
            }
        }
    }
}

@Composable
private fun FolderGrid(
    folders: List<MediaFolder>,
    onOpen: (MediaFolder) -> Unit,
    nestedScrollConnection: NestedScrollConnection,
    topContentPadding: Dp = 12.dp,
) {
    val bottomPadding = LocalMainBottomContentPadding.current
    LazyVerticalGrid(
        columns = GridCells.Adaptive(160.dp),
        modifier = Modifier
            .fillMaxSize()
            .scrollEndHaptic()
            .overScrollVertical()
            .nestedScroll(nestedScrollConnection),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = topContentPadding,
            bottom = 12.dp + bottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        overscrollEffect = null,
    ) {
        items(folders, key = { it.id }) { folder ->
            Card(onClick = { onOpen(folder) }) {
                Column(modifier = Modifier.padding(10.dp)) {
                    BoxThumbnail(folder.thumbnailUri)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = folder.name,
                        style = MiuixTheme.textStyles.headline1,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.library_videos_count, folder.totalVideoCount),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoRow(item: MediaItemInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BoxThumbnail(item.thumbnailUri, modifier = Modifier.width(120.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MiuixTheme.textStyles.headline1,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            val meta = buildList {
                if (item.duration > 0) add(FormatUtils.formatDuration(item.duration))
                item.displayResolution?.let { add(it) }
                if (item.size > 0) add(FormatUtils.formatSize(item.size))
                if (item.playbackPosition > 0) {
                    add(FormatUtils.formatDuration(item.playbackPosition))
                }
            }.joinToString(" · ")
            if (meta.isNotBlank()) {
                Text(
                    text = meta,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun BoxThumbnail(
    uri: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    if (uri == null) {
        Box(
            modifier = modifier
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Folder,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
        }
    } else {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(uri)
                .decoderFactory(VideoFrameDecoder.Factory())
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp)),
        )
    }
}


/** Header shown while browsing a folder: back arrow, folder name and recursive video count. */
@Composable
private fun FolderHeader(
    folder: MediaFolder,
    onBack: () -> Unit,
    topPadding: Dp,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding, start = 4.dp, end = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                MiuixIcons.Back,
                contentDescription = stringResource(R.string.library_folder_up),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folder.name,
                style = MiuixTheme.textStyles.headline1,
                color = MiuixTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.library_videos_count, folder.totalVideoCount),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

/**
 * Contents of one folder: sub-folders first, then the videos stored directly in it. Nested
 * folders stay nested instead of being flattened into the parent list.
 */
@Composable
private fun FolderContents(
    folders: List<MediaFolder>,
    videos: List<MediaItemInfo>,
    onOpenFolder: (MediaFolder) -> Unit,
    onPlay: (MediaItemInfo) -> Unit,
    nestedScrollConnection: NestedScrollConnection,
) {
    if (folders.isEmpty() && videos.isEmpty()) {
        EmptyState(
            title = stringResource(R.string.library_empty),
            message = stringResource(R.string.library_folder_empty_hint),
        )
        return
    }
    val bottomPadding = LocalMainBottomContentPadding.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .scrollEndHaptic()
            .overScrollVertical()
            .nestedScroll(nestedScrollConnection),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = 4.dp,
            bottom = 8.dp + bottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        overscrollEffect = null,
    ) {
        items(folders, key = { "folder_${it.id}" }) { folder ->
            Card(onClick = { onOpenFolder(folder) }) {
                FolderRow(folder = folder)
            }
        }
        items(videos, key = { it.id }) { item ->
            Card(onClick = { onPlay(item) }) {
                VideoRow(item = item)
            }
        }
    }
}

@Composable
private fun FolderRow(folder: MediaFolder) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.Folder,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folder.name,
                style = MiuixTheme.textStyles.headline1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.library_videos_count, folder.totalVideoCount),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

/** Sort picker. Same sheet style as the other Miuix list pickers in the app. */
@Composable
private fun LibrarySortSheet(
    visible: Boolean,
    sort: MediaSort,
    onDismiss: () -> Unit,
    onSelect: (MediaSort) -> Unit,
) {
    if (!visible) return
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.library_sort),
                    style = MiuixTheme.textStyles.title3,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
                MediaSort.OPTIONS.forEach { option ->
                    val selected = option == sort
                    Text(
                        text = stringResource(option.labelRes()),
                        color = if (selected) {
                            MiuixTheme.colorScheme.primary
                        } else {
                            MiuixTheme.colorScheme.onSurface
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(option)
                                onDismiss()
                            }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                    )
                }
            }
        }
    }
}

private fun MediaSort.labelRes(): Int = when (field) {
    MediaSortField.NAME -> if (ascending) {
        R.string.library_sort_name_asc
    } else {
        R.string.library_sort_name_desc
    }
    MediaSortField.DATE -> if (ascending) {
        R.string.library_sort_date_asc
    } else {
        R.string.library_sort_date_desc
    }
    MediaSortField.SIZE -> if (ascending) {
        R.string.library_sort_size_asc
    } else {
        R.string.library_sort_size_desc
    }
    MediaSortField.DURATION -> if (ascending) {
        R.string.library_sort_duration_asc
    } else {
        R.string.library_sort_duration_desc
    }
}

/**
 * Search box + mode/scope switches. Floats above the results the same way the glass tab row
 * floats above the normal lists, so it needs an opaque card to keep text readable.
 */
@Composable
private fun LibrarySearchPanel(
    state: SearchState,
    hasFolder: Boolean,
    onQueryChange: (String) -> Unit,
    onModeChange: (SearchMode) -> Unit,
    onScopeChange: (SearchScope) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    label = stringResource(R.string.library_search_hint),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.library_search_close),
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchChip(
                label = stringResource(R.string.library_search_mode_fuzzy),
                selected = state.mode == SearchMode.FUZZY,
                onClick = { onModeChange(SearchMode.FUZZY) },
            )
            SearchChip(
                label = stringResource(R.string.library_search_mode_exact),
                selected = state.mode == SearchMode.EXACT,
                onClick = { onModeChange(SearchMode.EXACT) },
            )
            Spacer(modifier = Modifier.weight(1f))
            if (hasFolder) {
                SearchChip(
                    label = stringResource(R.string.library_search_scope_all),
                    selected = state.scope == SearchScope.ALL,
                    onClick = { onScopeChange(SearchScope.ALL) },
                )
                SearchChip(
                    label = stringResource(R.string.library_search_scope_folder),
                    selected = state.scope == SearchScope.CURRENT_FOLDER,
                    onClick = { onScopeChange(SearchScope.CURRENT_FOLDER) },
                )
            }
        }
    }
}

@Composable
private fun SearchChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(onClick = onClick) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.body2,
            color = if (selected) {
                MiuixTheme.colorScheme.primary
            } else {
                MiuixTheme.colorScheme.onSurfaceVariantSummary
            },
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

/** Search results reuse [VideoList], so tapping a hit plays it with the results as playlist. */
@Composable
private fun SearchResults(
    state: LibraryUiState,
    onPlay: (MediaItemInfo) -> Unit,
    nestedScrollConnection: NestedScrollConnection,
    topContentPadding: Dp,
) {
    when {
        state.search.query.isBlank() -> EmptyState(
            title = stringResource(R.string.library_search_idle_title),
            message = stringResource(R.string.library_search_idle_hint),
            icon = Icons.Outlined.Search,
        )
        state.searchResults.isEmpty() -> EmptyState(
            title = stringResource(R.string.library_search_no_result),
            message = stringResource(R.string.library_search_no_result_hint),
            icon = Icons.Outlined.Search,
        )
        else -> VideoList(
            items = state.searchResults,
            onPlay = onPlay,
            nestedScrollConnection = nestedScrollConnection,
            topContentPadding = topContentPadding,
        )
    }
}
