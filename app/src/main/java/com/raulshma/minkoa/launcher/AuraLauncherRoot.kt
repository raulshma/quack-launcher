package com.raulshma.minkoa.launcher

import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.raulshma.minkoa.data.LayoutRepository
import com.raulshma.minkoa.data.SavedLayout
import com.raulshma.minkoa.data.SlotContent
import com.raulshma.minkoa.files.FilesScreen
import com.raulshma.minkoa.gallery.GalleryScreen
import com.raulshma.minkoa.weather.WeatherScreen
import com.raulshma.minkoa.widget.LauncherWidgetHostController
import com.raulshma.minkoa.widget.WidgetPickerSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private const val PAGER_ANCHOR_PAGE = Int.MAX_VALUE / 2
private const val WORKSPACE_COLUMNS = 4
private const val WORKSPACE_ROWS = 5
private const val WORKSPACE_SLOTS = WORKSPACE_COLUMNS * WORKSPACE_ROWS
private const val DOCK_APP_SLOTS = 5
private const val SWIPE_UP_THRESHOLD = -120f

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())

private enum class SlotArea {
    Workspace,
    Dock
}

private enum class ScreenSide {
    Left,
    Right;

    companion object {
        fun fromName(name: String): ScreenSide = entries.firstOrNull { side ->
            side.name == name
        } ?: Left
    }
}

private enum class ExtensionScreenType(
    val id: String,
    val title: String,
    val summary: String
) {
    Gallery(
        id = "gallery",
        title = "Gallery",
        summary = "Recent photos in a focused, fast visual grid."
    ),
    Files(
        id = "files",
        title = "Files",
        summary = "Quick access to documents, downloads, and recent files."
    ),
    Weather(
        id = "weather",
        title = "Weather",
        summary = "Immersive at-a-glance temperature and forecast surface."
    );

    companion object {
        fun fromId(id: String): ExtensionScreenType? =
            entries.firstOrNull { type -> type.id == id }
    }
}

private data class SlotSelection(
    val area: SlotArea,
    val index: Int
)

private data class DragInProgress(
    val source: SlotSelection,
    val label: String,
    val pointerInRoot: Offset
)

@Immutable
data class LauncherApp(
    val label: String,
    val packageName: String,
    val activityName: String
)

data class LauncherUiState(
    val installedApps: List<LauncherApp> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

class LauncherViewModel(application: android.app.Application) :
    AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    init {
        refreshApps()
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state -> state.copy(searchQuery = query) }
    }

    fun refreshApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = queryLaunchableApps(getApplication())
            _uiState.update { state ->
                state.copy(
                    installedApps = apps,
                    isLoading = false
                )
            }
        }
    }

    private fun queryLaunchableApps(application: android.app.Application): List<LauncherApp> {
        val packageManager = application.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        return packageManager
            .queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
            .asSequence()
            .mapNotNull { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                if (activityInfo.packageName == application.packageName) {
                    return@mapNotNull null
                }

                val label = resolveInfo.loadLabel(packageManager)?.toString()
                    ?.takeIf { it.isNotBlank() }
                    ?: activityInfo.name.substringAfterLast('.')

                LauncherApp(
                    label = label,
                    packageName = activityInfo.packageName,
                    activityName = activityInfo.name
                )
            }
            .distinctBy { app -> appKey(app) }
            .sortedBy { app -> app.label.lowercase(Locale.getDefault()) }
            .toList()
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AuraLauncherRoot(
    viewModel: LauncherViewModel = viewModel(),
    widgetHostController: LauncherWidgetHostController,
    requestWidgetBind: (
        appWidgetId: Int,
        provider: ComponentName,
        onResult: (SlotContent.Widget?) -> Unit
    ) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val pagerState = rememberPagerState(
        initialPage = PAGER_ANCHOR_PAGE,
        pageCount = { Int.MAX_VALUE }
    )
    val pagerScope = rememberCoroutineScope()
    val appDrawerState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val layoutRepository = remember { LayoutRepository(context) }
    val savedLayout = remember { layoutRepository.loadLayout() }

    var isAppDrawerOpen by rememberSaveable { mutableStateOf(false) }
    var isEditMode by rememberSaveable { mutableStateOf(false) }
    var isScreenEditorOpen by rememberSaveable { mutableStateOf(false) }
    var isWidgetPickerOpen by rememberSaveable { mutableStateOf(false) }
    var pendingScreenSide by rememberSaveable { mutableStateOf(ScreenSide.Left.name) }

    var workspaceSlots by remember {
        mutableStateOf(
            savedLayout?.workspaceSlots ?: List(WORKSPACE_SLOTS) { null }
        )
    }
    var dockSlots by remember {
        mutableStateOf(
            savedLayout?.dockSlots ?: List(DOCK_APP_SLOTS) { null }
        )
    }
    var layoutInitialized by remember { mutableStateOf(savedLayout != null) }

    var selectedSlot by remember { mutableStateOf<SlotSelection?>(null) }
    val workspaceBounds = remember { mutableStateMapOf<Int, Rect>() }
    val dockBounds = remember { mutableStateMapOf<Int, Rect>() }
    var dragInProgress by remember { mutableStateOf<DragInProgress?>(null) }
    var dragHoverTarget by remember { mutableStateOf<SlotSelection?>(null) }

    var leftScreenIds by rememberSaveable {
        mutableStateOf(savedLayout?.leftScreenIds ?: emptyList())
    }
    var rightScreenIds by rememberSaveable {
        mutableStateOf(savedLayout?.rightScreenIds ?: emptyList())
    }

    val leftScreens = remember(leftScreenIds) {
        leftScreenIds.mapNotNull(ExtensionScreenType::fromId)
    }
    val rightScreens = remember(rightScreenIds) {
        rightScreenIds.mapNotNull(ExtensionScreenType::fromId)
    }

    val availableWidgets = remember { widgetHostController.installedProviders() }

    LaunchedEffect(uiState.installedApps, layoutInitialized) {
        if (!layoutInitialized && uiState.installedApps.isNotEmpty()) {
            val installedKeys = uiState.installedApps.map(::appKey)
            workspaceSlots = List(WORKSPACE_SLOTS) { index ->
                installedKeys.getOrNull(index)?.let { SlotContent.App(it) }
            }
            dockSlots = List(DOCK_APP_SLOTS) { index ->
                installedKeys.getOrNull(index)?.let { SlotContent.App(it) }
            }
            layoutInitialized = true
        }
    }

    val appsByKey = remember(uiState.installedApps) {
        uiState.installedApps.associateBy(::appKey)
    }

    val isAnchorPage by remember {
        derivedStateOf { pagerState.currentPage == PAGER_ANCHOR_PAGE }
    }

    val dockAlpha by animateFloatAsState(
        targetValue = if (isAnchorPage) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "dock-fade"
    )

    val filteredApps by remember(uiState.installedApps, uiState.searchQuery) {
        derivedStateOf {
            val trimmedQuery = uiState.searchQuery.trim()
            if (trimmedQuery.isBlank()) {
                uiState.installedApps
            } else {
                uiState.installedApps.filter { app ->
                    app.label.contains(trimmedQuery, ignoreCase = true) ||
                        app.packageName.contains(trimmedQuery, ignoreCase = true)
                }
            }
        }
    }

    fun persistLayout() {
        layoutRepository.saveLayout(
            workspaceSlots = workspaceSlots,
            dockSlots = dockSlots,
            leftScreenIds = leftScreenIds,
            rightScreenIds = rightScreenIds
        )
    }

    fun assignSlot(selection: SlotSelection, content: SlotContent?) {
        when (selection.area) {
            SlotArea.Workspace -> {
                workspaceSlots = workspaceSlots.toMutableList().apply {
                    this[selection.index] = content
                }
            }

            SlotArea.Dock -> {
                dockSlots = dockSlots.toMutableList().apply {
                    this[selection.index] = content
                }
            }
        }
        persistLayout()
    }

    fun contentAtSlot(selection: SlotSelection): SlotContent? {
        return when (selection.area) {
            SlotArea.Workspace -> workspaceSlots.getOrNull(selection.index)
            SlotArea.Dock -> dockSlots.getOrNull(selection.index)
        }
    }

    fun swapSlots(from: SlotSelection, to: SlotSelection) {
        val fromValue = contentAtSlot(from)
        val toValue = contentAtSlot(to)
        assignSlot(from, toValue)
        assignSlot(to, fromValue)
    }

    fun findDropTarget(pointerInRoot: Offset): SlotSelection? {
        workspaceBounds.entries.firstOrNull { (_, rect) ->
            rect.contains(pointerInRoot)
        }?.let { (index, _) ->
            return SlotSelection(SlotArea.Workspace, index)
        }

        dockBounds.entries.firstOrNull { (_, rect) ->
            rect.contains(pointerInRoot)
        }?.let { (index, _) ->
            return SlotSelection(SlotArea.Dock, index)
        }

        return null
    }

    fun beginDrag(source: SlotSelection, content: SlotContent, pointerInRoot: Offset) {
        if (!isEditMode) return

        val label = when (content) {
            is SlotContent.App -> appsByKey[content.key]?.label ?: "App"
            is SlotContent.Widget -> "Widget"
        }
        dragInProgress = DragInProgress(
            source = source,
            label = label,
            pointerInRoot = pointerInRoot
        )
        dragHoverTarget = source
        selectedSlot = null
    }

    fun updateDrag(pointerInRoot: Offset) {
        val current = dragInProgress ?: return
        dragInProgress = current.copy(pointerInRoot = pointerInRoot)
        dragHoverTarget = findDropTarget(pointerInRoot)
    }

    fun finishDrag() {
        val drag = dragInProgress
        val target = dragHoverTarget

        if (drag != null && target != null && target != drag.source) {
            val sourceValue = contentAtSlot(drag.source)
            val targetValue = contentAtSlot(target)
            assignSlot(target, sourceValue)
            assignSlot(drag.source, targetValue)
        }

        dragInProgress = null
        dragHoverTarget = null
    }

    fun cancelDrag() {
        dragInProgress = null
        dragHoverTarget = null
    }

    fun handleSlotTap(area: SlotArea, index: Int) {
        if (dragInProgress != null) return

        val tapped = SlotSelection(area, index)
        val tappedContent = contentAtSlot(tapped)

        if (!isEditMode) {
            when (tappedContent) {
                is SlotContent.App -> {
                    appsByKey[tappedContent.key]?.let { app ->
                        launchApp(context, app)
                    }
                }

                is SlotContent.Widget -> { /* no-op on tap for widgets */ }
                null -> { /* empty slot */ }
            }
            return
        }

        val currentSelection = selectedSlot
        when {
            currentSelection == null -> {
                if (tappedContent != null) {
                    selectedSlot = tapped
                }
            }

            currentSelection == tapped -> {
                selectedSlot = null
            }

            else -> {
                swapSlots(currentSelection, tapped)
                selectedSlot = null
            }
        }
    }

    fun handleWidgetSelected(providerInfo: AppWidgetProviderInfo) {
        val appWidgetId = widgetHostController.allocateAppWidgetId()
        val provider = providerInfo.provider
        requestWidgetBind(appWidgetId, provider) { widgetSlot ->
            if (widgetSlot != null) {
                val target = selectedSlot
                if (target != null) {
                    assignSlot(target, widgetSlot)
                    selectedSlot = null
                } else {
                    val firstEmpty = workspaceSlots.indexOfFirst { it == null }
                    if (firstEmpty >= 0) {
                        workspaceSlots = workspaceSlots.toMutableList().apply {
                            this[firstEmpty] = widgetSlot
                        }
                        persistLayout()
                    } else {
                        widgetHostController.deleteAppWidgetId(appWidgetId)
                    }
                }
            }
            isWidgetPickerOpen = false
        }
    }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when {
                    page == PAGER_ANCHOR_PAGE -> {
                        AnchorHomeScreen(
                            workspaceSlots = workspaceSlots,
                            appsByKey = appsByKey,
                            widgetHostController = widgetHostController,
                            isEditMode = isEditMode,
                            selectedSlot = selectedSlot,
                            dragHoverTarget = dragHoverTarget,
                            dragSource = dragInProgress?.source,
                            onOpenDrawer = { isAppDrawerOpen = true },
                            onEnterEditMode = {
                                isEditMode = true
                                selectedSlot = null
                            },
                            onWorkspaceSlotTapped = { index ->
                                handleSlotTap(SlotArea.Workspace, index)
                            },
                            onWorkspaceSlotBoundsChanged = { index, bounds ->
                                workspaceBounds[index] = bounds
                            },
                            onWorkspaceDragStart = { index, pointerInRoot ->
                                val slot = SlotSelection(SlotArea.Workspace, index)
                                val content = contentAtSlot(slot)
                                if (content != null) {
                                    beginDrag(slot, content, pointerInRoot)
                                }
                            },
                            onWorkspaceDragMove = ::updateDrag,
                            onWorkspaceDragEnd = ::finishDrag,
                            onWorkspaceDragCancel = ::cancelDrag
                        )
                    }

                    page < PAGER_ANCHOR_PAGE -> {
                        val offset = PAGER_ANCHOR_PAGE - page
                        val screenType = leftScreens.getOrNull(offset - 1)
                        if (screenType == null) {
                            UnassignedExtensionPage(
                                direction = "Left",
                                index = offset,
                                isEditMode = isEditMode,
                                onAddScreen = {
                                    pendingScreenSide = ScreenSide.Left.name
                                    isScreenEditorOpen = true
                                },
                                onGoHome = {
                                    pagerScope.launch {
                                        pagerState.animateScrollToPage(PAGER_ANCHOR_PAGE)
                                    }
                                }
                            )
                        } else {
                            when (screenType) {
                                ExtensionScreenType.Gallery -> GalleryScreen()
                                ExtensionScreenType.Weather -> WeatherScreen()
                                ExtensionScreenType.Files -> FilesScreen()
                            }
                        }
                    }

                    else -> {
                        val offset = page - PAGER_ANCHOR_PAGE
                        val screenType = rightScreens.getOrNull(offset - 1)
                        if (screenType == null) {
                            UnassignedExtensionPage(
                                direction = "Right",
                                index = offset,
                                isEditMode = isEditMode,
                                onAddScreen = {
                                    pendingScreenSide = ScreenSide.Right.name
                                    isScreenEditorOpen = true
                                },
                                onGoHome = {
                                    pagerScope.launch {
                                        pagerState.animateScrollToPage(PAGER_ANCHOR_PAGE)
                                    }
                                }
                            )
                        } else {
                            when (screenType) {
                                ExtensionScreenType.Gallery -> GalleryScreen()
                                ExtensionScreenType.Weather -> WeatherScreen()
                                ExtensionScreenType.Files -> FilesScreen()
                            }
                        }
                    }
                }
            }

            GlobalDock(
                slotContents = dockSlots,
                appsByKey = appsByKey,
                widgetHostController = widgetHostController,
                isEditMode = isEditMode,
                selectedSlot = selectedSlot,
                dragHoverTarget = dragHoverTarget,
                dragSource = dragInProgress?.source,
                onDockSlotTapped = { index ->
                    handleSlotTap(SlotArea.Dock, index)
                },
                onDockSlotBoundsChanged = { index, bounds ->
                    dockBounds[index] = bounds
                },
                onDockDragStart = { index, pointerInRoot ->
                    val slot = SlotSelection(SlotArea.Dock, index)
                    val content = contentAtSlot(slot)
                    if (content != null) {
                        beginDrag(slot, content, pointerInRoot)
                    }
                },
                onDockDragMove = ::updateDrag,
                onDockDragEnd = ::finishDrag,
                onDockDragCancel = ::cancelDrag,
                onOpenDrawer = { isAppDrawerOpen = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .safeDrawingPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .alpha(dockAlpha)
            )

            if (isEditMode && isAnchorPage) {
                dragInProgress?.let { drag ->
                    DraggedAppPreview(
                        label = drag.label,
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = (drag.pointerInRoot.x - 26f).roundToInt(),
                                    y = (drag.pointerInRoot.y - 26f).roundToInt()
                                )
                            }
                    )
                }
                EditModeOverlay(
                    leftCount = leftScreens.size,
                    rightCount = rightScreens.size,
                    onAddLeft = {
                        pendingScreenSide = ScreenSide.Left.name
                        isScreenEditorOpen = true
                    },
                    onAddRight = {
                        pendingScreenSide = ScreenSide.Right.name
                        isScreenEditorOpen = true
                    },
                    onWidgets = {
                        isWidgetPickerOpen = true
                    },
                    onDone = {
                        isEditMode = false
                        selectedSlot = null
                    },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .safeDrawingPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }

    if (isAppDrawerOpen) {
        ModalBottomSheet(
            onDismissRequest = { isAppDrawerOpen = false },
            sheetState = appDrawerState
        ) {
            AppDrawerSheet(
                isLoading = uiState.isLoading,
                apps = filteredApps,
                searchQuery = uiState.searchQuery,
                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                onAppClicked = { app ->
                    val selected = selectedSlot
                    if (isEditMode) {
                        if (selected != null) {
                            assignSlot(selected, SlotContent.App(appKey(app)))
                            selectedSlot = null
                        } else {
                            val firstEmpty = workspaceSlots.indexOfFirst { it == null }
                            if (firstEmpty >= 0) {
                                workspaceSlots = workspaceSlots.toMutableList().apply {
                                    this[firstEmpty] = SlotContent.App(appKey(app))
                                }
                                persistLayout()
                            } else {
                                launchApp(context, app)
                            }
                        }
                    } else {
                        launchApp(context, app)
                    }
                    isAppDrawerOpen = false
                }
            )
        }
    }

    if (isScreenEditorOpen) {
        ScreenEditorSheet(
            initialSide = ScreenSide.fromName(pendingScreenSide),
            onDismissRequest = { isScreenEditorOpen = false },
            onConfirm = { side, type ->
                when (side) {
                    ScreenSide.Left -> leftScreenIds = leftScreenIds + type.id
                    ScreenSide.Right -> rightScreenIds = rightScreenIds + type.id
                }
                persistLayout()
                isScreenEditorOpen = false
            }
        )
    }

    if (isWidgetPickerOpen) {
        WidgetPickerSheet(
            widgets = availableWidgets,
            onSelectWidget = { providerInfo ->
                handleWidgetSelected(providerInfo)
            },
            onDismissRequest = { isWidgetPickerOpen = false }
        )
    }
}

@Composable
private fun AnchorHomeScreen(
    workspaceSlots: List<SlotContent?>,
    appsByKey: Map<String, LauncherApp>,
    widgetHostController: LauncherWidgetHostController,
    isEditMode: Boolean,
    selectedSlot: SlotSelection?,
    dragHoverTarget: SlotSelection?,
    dragSource: SlotSelection?,
    onOpenDrawer: () -> Unit,
    onEnterEditMode: () -> Unit,
    onWorkspaceSlotTapped: (Int) -> Unit,
    onWorkspaceSlotBoundsChanged: (Int, Rect) -> Unit,
    onWorkspaceDragStart: (Int, Offset) -> Unit,
    onWorkspaceDragMove: (Offset) -> Unit,
    onWorkspaceDragEnd: () -> Unit,
    onWorkspaceDragCancel: () -> Unit
) {
    var cumulativeDrag by remember { mutableStateOf(0f) }
    val now by produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(30_000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        cumulativeDrag += dragAmount
                    },
                    onDragEnd = {
                        if (cumulativeDrag < SWIPE_UP_THRESHOLD) {
                            onOpenDrawer()
                        }
                        cumulativeDrag = 0f
                    },
                    onDragCancel = {
                        cumulativeDrag = 0f
                    }
                )
            }
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = now.format(timeFormatter),
            style = MaterialTheme.typography.displayLarge
        )
        Text(
            text = now.format(dateFormatter),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (isEditMode) {
                "Edit mode: tap an icon, then tap another slot to swap."
            } else {
                "Swipe up for app drawer. Long-press an empty slot to edit."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FilledTonalButton(onClick = onOpenDrawer) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowUp,
                contentDescription = null
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = if (isEditMode) {
                    "Open Drawer (assign app)"
                } else {
                    "Open App Drawer"
                }
            )
        }

        WorkspaceCard(
            workspaceSlots = workspaceSlots,
            appsByKey = appsByKey,
            widgetHostController = widgetHostController,
            isEditMode = isEditMode,
            selectedSlot = selectedSlot,
            dragHoverTarget = dragHoverTarget,
            dragSource = dragSource,
            onWorkspaceSlotTapped = onWorkspaceSlotTapped,
            onLongPressEmptySlot = onEnterEditMode,
            onWorkspaceSlotBoundsChanged = onWorkspaceSlotBoundsChanged,
            onWorkspaceDragStart = onWorkspaceDragStart,
            onWorkspaceDragMove = onWorkspaceDragMove,
            onWorkspaceDragEnd = onWorkspaceDragEnd,
            onWorkspaceDragCancel = onWorkspaceDragCancel
        )
    }
}

@Composable
private fun WorkspaceCard(
    workspaceSlots: List<SlotContent?>,
    appsByKey: Map<String, LauncherApp>,
    widgetHostController: LauncherWidgetHostController,
    isEditMode: Boolean,
    selectedSlot: SlotSelection?,
    dragHoverTarget: SlotSelection?,
    dragSource: SlotSelection?,
    onWorkspaceSlotTapped: (Int) -> Unit,
    onLongPressEmptySlot: () -> Unit,
    onWorkspaceSlotBoundsChanged: (Int, Rect) -> Unit,
    onWorkspaceDragStart: (Int, Offset) -> Unit,
    onWorkspaceDragMove: (Offset) -> Unit,
    onWorkspaceDragEnd: () -> Unit,
    onWorkspaceDragCancel: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(36.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Home Workspace \u00b7 4 \u00d7 5",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(WORKSPACE_COLUMNS),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                userScrollEnabled = false
            ) {
                items(WORKSPACE_SLOTS) { index ->
                    val content = workspaceSlots.getOrNull(index)
                    WorkspaceSlot(
                        content = content,
                        app = when (content) {
                            is SlotContent.App -> appsByKey[content.key]
                            else -> null
                        },
                        widgetHostController = widgetHostController,
                        isEditMode = isEditMode,
                        isSelected = selectedSlot == SlotSelection(
                            area = SlotArea.Workspace,
                            index = index
                        ),
                        isDropTarget = dragHoverTarget == SlotSelection(
                            area = SlotArea.Workspace,
                            index = index
                        ),
                        isDragSource = dragSource == SlotSelection(
                            area = SlotArea.Workspace,
                            index = index
                        ),
                        onTap = { onWorkspaceSlotTapped(index) },
                        onLongPressEmpty = {
                            if (content == null) {
                                onLongPressEmptySlot()
                            }
                        },
                        onBoundsChanged = { bounds ->
                            onWorkspaceSlotBoundsChanged(index, bounds)
                        },
                        onDragStart = { pointerInRoot ->
                            onWorkspaceDragStart(index, pointerInRoot)
                        },
                        onDragMove = onWorkspaceDragMove,
                        onDragEnd = onWorkspaceDragEnd,
                        onDragCancel = onWorkspaceDragCancel
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkspaceSlot(
    content: SlotContent?,
    app: LauncherApp?,
    widgetHostController: LauncherWidgetHostController,
    isEditMode: Boolean,
    isSelected: Boolean,
    isDropTarget: Boolean,
    isDragSource: Boolean,
    onTap: () -> Unit,
    onLongPressEmpty: () -> Unit,
    onBoundsChanged: (Rect) -> Unit,
    onDragStart: (Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit
) {
    var slotBounds by remember { mutableStateOf<Rect?>(null) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            modifier = Modifier
                .size(52.dp)
                .onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInRoot()
                    slotBounds = bounds
                    onBoundsChanged(bounds)
                }
                .pointerInput(content, isEditMode) {
                    detectTapGestures(
                        onTap = { onTap() },
                        onLongPress = {
                            if (content == null) {
                                onLongPressEmpty()
                            }
                        }
                    )
                }
                .pointerInput(content, isEditMode) {
                    if (!isEditMode || content == null) {
                        return@pointerInput
                    }

                    var currentPointerInRoot = Offset.Zero
                    detectDragGesturesAfterLongPress(
                        onDragStart = { localOffset ->
                            val origin = slotBounds?.topLeft ?: Offset.Zero
                            currentPointerInRoot = origin + localOffset
                            onDragStart(currentPointerInRoot)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            currentPointerInRoot += dragAmount
                            onDragMove(currentPointerInRoot)
                        },
                        onDragEnd = {
                            onDragEnd()
                        },
                        onDragCancel = {
                            onDragCancel()
                        }
                    )
                },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
            color = when {
                isDragSource -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                isDropTarget -> MaterialTheme.colorScheme.tertiaryContainer
                isSelected -> MaterialTheme.colorScheme.tertiaryContainer
                content == null -> MaterialTheme.colorScheme.surfaceVariant
                content is SlotContent.Widget -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.secondaryContainer
            }
        ) {
            Box(contentAlignment = Alignment.Center) {
                when (content) {
                    null -> {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    is SlotContent.App -> {
                        if (app != null) {
                            Text(
                                text = app.label.firstOrNull()?.uppercaseChar()
                                    ?.toString() ?: "?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                text = "?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    is SlotContent.Widget -> {
                        val providerInfo = remember(content.appWidgetId) {
                            widgetHostController.getWidgetInfo(content.appWidgetId)
                        }
                        if (providerInfo != null) {
                            AndroidView(
                                factory = { ctx ->
                                    FrameLayout(ctx).apply {
                                        layoutParams = FrameLayout.LayoutParams(
                                            FrameLayout.LayoutParams.MATCH_PARENT,
                                            FrameLayout.LayoutParams.MATCH_PARENT
                                        )
                                        addView(
                                            widgetHostController.createHostView(
                                                content.appWidgetId,
                                                providerInfo
                                            ).apply {
                                                layoutParams = FrameLayout.LayoutParams(
                                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                                    FrameLayout.LayoutParams.MATCH_PARENT
                                                )
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = "W",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = when {
                content is SlotContent.App && app != null -> app.label
                content is SlotContent.Widget -> "Widget"
                isEditMode -> "Empty"
                else -> ""
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EditModeOverlay(
    leftCount: Int,
    rightCount: Int,
    onAddLeft: () -> Unit,
    onAddRight: () -> Unit,
    onWidgets: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Edit Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Screens \u00b7 Left: $leftCount  |  Right: $rightCount",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(onClick = onWidgets) {
                    Icon(
                        imageVector = Icons.Rounded.ViewAgenda,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Widgets")
                }
                FilledTonalButton(onClick = onAddLeft) {
                    Text("Add Left")
                }
                FilledTonalButton(onClick = onAddRight) {
                    Text("Add Right")
                }
                OutlinedButton(onClick = onDone) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
private fun ExtensionScreenPage(
    type: ExtensionScreenType,
    direction: String,
    index: Int,
    onGoHome: () -> Unit
) {
    val icon = when (type) {
        ExtensionScreenType.Gallery -> Icons.Rounded.PhotoLibrary
        ExtensionScreenType.Files -> Icons.Rounded.Folder
        ExtensionScreenType.Weather -> Icons.Rounded.WbSunny
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(40.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "$direction page $index",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${type.title} Screen",
                        style = MaterialTheme.typography.displayMedium
                    )
                }
                Text(
                    text = type.summary,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FilledTonalButton(onClick = onGoHome) {
                    Icon(
                        imageVector = Icons.Rounded.Home,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Return to Home")
                }
            }
        }
    }
}

@Composable
private fun UnassignedExtensionPage(
    direction: String,
    index: Int,
    isEditMode: Boolean,
    onAddScreen: () -> Unit,
    onGoHome: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(40.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "$direction page $index",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Unassigned App Screen",
                    style = MaterialTheme.typography.displayMedium
                )
                Text(
                    text = if (isEditMode) {
                        "This page is ready. Add Gallery, Files, or Weather from the editor."
                    } else {
                        "Long-press an empty slot on Home to enter Edit Mode and assign this page."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isEditMode) {
                    FilledTonalButton(onClick = onAddScreen) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Assign Screen")
                    }
                }
                OutlinedButton(onClick = onGoHome) {
                    Icon(
                        imageVector = Icons.Rounded.Home,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Back Home")
                }
            }
        }
    }
}

@Composable
private fun GlobalDock(
    slotContents: List<SlotContent?>,
    appsByKey: Map<String, LauncherApp>,
    widgetHostController: LauncherWidgetHostController,
    isEditMode: Boolean,
    selectedSlot: SlotSelection?,
    dragHoverTarget: SlotSelection?,
    dragSource: SlotSelection?,
    onDockSlotTapped: (Int) -> Unit,
    onDockSlotBoundsChanged: (Int, Rect) -> Unit,
    onDockDragStart: (Int, Offset) -> Unit,
    onDockDragMove: (Offset) -> Unit,
    onDockDragEnd: () -> Unit,
    onDockDragCancel: () -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(100.dp),
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(DOCK_APP_SLOTS) { index ->
                val content = slotContents.getOrNull(index)
                val app = when (content) {
                    is SlotContent.App -> appsByKey[content.key]
                    else -> null
                }
                DockSlot(
                    content = content,
                    app = app,
                    widgetHostController = widgetHostController,
                    isEditMode = isEditMode,
                    isSelected = selectedSlot == SlotSelection(
                        area = SlotArea.Dock,
                        index = index
                    ),
                    isDropTarget = dragHoverTarget == SlotSelection(
                        area = SlotArea.Dock,
                        index = index
                    ),
                    isDragSource = dragSource == SlotSelection(
                        area = SlotArea.Dock,
                        index = index
                    ),
                    onTap = { onDockSlotTapped(index) },
                    onBoundsChanged = { bounds ->
                        onDockSlotBoundsChanged(index, bounds)
                    },
                    onDragStart = { pointerInRoot ->
                        onDockDragStart(index, pointerInRoot)
                    },
                    onDragMove = onDockDragMove,
                    onDragEnd = onDockDragEnd,
                    onDragCancel = onDockDragCancel
                )
            }

            IconButton(onClick = onOpenDrawer) {
                Icon(
                    imageVector = Icons.Rounded.Apps,
                    contentDescription = "Open app drawer"
                )
            }
        }
    }
}

@Composable
private fun DockSlot(
    content: SlotContent?,
    app: LauncherApp?,
    widgetHostController: LauncherWidgetHostController,
    isEditMode: Boolean,
    isSelected: Boolean,
    isDropTarget: Boolean,
    isDragSource: Boolean,
    onTap: () -> Unit,
    onBoundsChanged: (Rect) -> Unit,
    onDragStart: (Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit
) {
    var slotBounds by remember { mutableStateOf<Rect?>(null) }

    Column(
        modifier = Modifier.widthIn(min = 48.dp, max = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            modifier = Modifier
                .size(40.dp)
                .onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInRoot()
                    slotBounds = bounds
                    onBoundsChanged(bounds)
                }
                .pointerInput(content, isEditMode) {
                    detectTapGestures(onTap = { onTap() })
                }
                .pointerInput(content, isEditMode) {
                    if (!isEditMode || content == null) {
                        return@pointerInput
                    }

                    var currentPointerInRoot = Offset.Zero
                    detectDragGesturesAfterLongPress(
                        onDragStart = { localOffset ->
                            val origin = slotBounds?.topLeft ?: Offset.Zero
                            currentPointerInRoot = origin + localOffset
                            onDragStart(currentPointerInRoot)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            currentPointerInRoot += dragAmount
                            onDragMove(currentPointerInRoot)
                        },
                        onDragEnd = {
                            onDragEnd()
                        },
                        onDragCancel = {
                            onDragCancel()
                        }
                    )
                },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            color = when {
                isDragSource -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                isDropTarget -> MaterialTheme.colorScheme.tertiaryContainer
                isSelected -> MaterialTheme.colorScheme.tertiaryContainer
                content == null -> MaterialTheme.colorScheme.surfaceVariant
                content is SlotContent.Widget -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.primaryContainer
            }
        ) {
            Box(contentAlignment = Alignment.Center) {
                when (content) {
                    null -> {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    is SlotContent.App -> {
                        if (app != null) {
                            Text(
                                text = app.label.firstOrNull()?.uppercaseChar()
                                    ?.toString() ?: "?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                text = "?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    is SlotContent.Widget -> {
                        Text(
                            text = "W",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Text(
            text = when {
                content is SlotContent.App && app != null -> app.label
                content is SlotContent.Widget -> "Widget"
                isEditMode -> "Empty"
                else -> ""
            },
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DraggedAppPreview(
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.size(52.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        tonalElevation = 12.dp,
        shadowElevation = 12.dp,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.94f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AppDrawerSheet(
    isLoading: Boolean,
    apps: List<LauncherApp>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onAppClicked: (LauncherApp) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "App Drawer",
            style = MaterialTheme.typography.displayMedium
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null
                )
            },
            placeholder = {
                Text("Search by name or package")
            }
        )

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            apps.isEmpty() -> {
                Text(
                    text = "No apps matched your search.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = apps,
                        key = { app -> appKey(app) }
                    ) { app ->
                        Surface(
                            onClick = { onAppClicked(app) },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(
                                20.dp
                            ),
                            tonalElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(36.dp),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(
                                        12.dp
                                    ),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = app.label.firstOrNull()
                                                ?.uppercaseChar()?.toString() ?: "?",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = app.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = app.packageName,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenEditorSheet(
    initialSide: ScreenSide,
    onDismissRequest: () -> Unit,
    onConfirm: (ScreenSide, ExtensionScreenType) -> Unit
) {
    var selectedSide by remember(initialSide) { mutableStateOf(initialSide) }
    var selectedType by remember { mutableStateOf(ExtensionScreenType.Gallery) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Assign App Screen",
                style = MaterialTheme.typography.displayMedium
            )
            Text(
                text = "Choose the side and which environment screen to append.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedSide == ScreenSide.Left) {
                    FilledTonalButton(onClick = { selectedSide = ScreenSide.Left }) {
                        Text("Left")
                    }
                } else {
                    OutlinedButton(onClick = { selectedSide = ScreenSide.Left }) {
                        Text("Left")
                    }
                }

                if (selectedSide == ScreenSide.Right) {
                    FilledTonalButton(onClick = { selectedSide = ScreenSide.Right }) {
                        Text("Right")
                    }
                } else {
                    OutlinedButton(onClick = { selectedSide = ScreenSide.Right }) {
                        Text("Right")
                    }
                }
            }

            ExtensionScreenType.entries.forEach { type ->
                val isSelected = selectedType == type
                val icon = when (type) {
                    ExtensionScreenType.Gallery -> Icons.Rounded.PhotoLibrary
                    ExtensionScreenType.Files -> Icons.Rounded.Folder
                    ExtensionScreenType.Weather -> Icons.Rounded.WbSunny
                }
                Surface(
                    onClick = { selectedType = type },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    tonalElevation = if (isSelected) 4.dp else 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.15f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = type.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = type.summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            FilledTonalButton(
                onClick = { onConfirm(selectedSide, selectedType) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add ${selectedType.title} Screen")
            }
        }
    }
}

private fun appKey(app: LauncherApp): String =
    "${app.packageName}/${app.activityName}"

private fun launchApp(context: Context, app: LauncherApp) {
    val launchIntent = Intent(Intent.ACTION_MAIN).apply {
        component = ComponentName(app.packageName, app.activityName)
        addCategory(Intent.CATEGORY_LAUNCHER)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    runCatching {
        context.startActivity(launchIntent)
    }.onFailure {
        Toast.makeText(
            context,
            "Unable to open ${app.label}",
            Toast.LENGTH_SHORT
        ).show()
    }
}
