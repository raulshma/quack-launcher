package com.raulshma.minkoa.launcher

import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Process
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.graphicsLayer
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
import com.raulshma.minkoa.gestures.GestureHelper
import com.raulshma.minkoa.icons.IconPackInfo
import com.raulshma.minkoa.icons.IconResolver
import com.raulshma.minkoa.icons.ResolvedIcon
import com.raulshma.minkoa.notifications.LauncherNotificationListener
import com.raulshma.minkoa.preferences.AppPreferencesRepository
import com.raulshma.minkoa.ui.theme.LocalMotionTokens
import com.raulshma.minkoa.ui.theme.LocalShapeTokens
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
private const val SWIPE_DOWN_THRESHOLD = 120f

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())

private enum class SlotArea { Workspace, Dock }

private enum class ScreenSide {
    Left, Right;
    companion object {
        fun fromName(name: String): ScreenSide = entries.firstOrNull { it.name == name } ?: Left
    }
}

private enum class ExtensionScreenType(
    val id: String,
    val title: String,
    val summary: String
) {
    Gallery("gallery", "Gallery", "Recent photos in a focused, fast visual grid."),
    Files("files", "Files", "Quick access to documents, downloads, and recent files."),
    Weather("weather", "Weather", "Immersive at-a-glance temperature and forecast surface.");

    companion object {
        fun fromId(id: String): ExtensionScreenType? = entries.firstOrNull { it.id == id }
    }
}

private data class SlotSelection(val area: SlotArea, val index: Int)

private data class DragInProgress(
    val source: SlotSelection,
    val label: String,
    val pointerInRoot: Offset
)

@Immutable
data class LauncherApp(
    val label: String,
    val packageName: String,
    val activityName: String,
    val category: AppCategory = AppCategory.Other
)

enum class AppCategory(val label: String) {
    Social("Social"),
    Productivity("Productivity"),
    Games("Games"),
    Entertainment("Entertainment"),
    Communication("Communication"),
    Media("Media"),
    Tools("Tools"),
    Shopping("Shopping"),
    Finance("Finance"),
    Health("Health"),
    Travel("Travel"),
    Education("Education"),
    Other("Other");

    companion object {
        fun fromAppInfo(applicationInfo: ApplicationInfo?): AppCategory {
            if (applicationInfo == null) return Other
            return when (applicationInfo.category) {
                ApplicationInfo.CATEGORY_SOCIAL -> Social
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> Productivity
                ApplicationInfo.CATEGORY_GAME -> Games
                7 -> Entertainment
                3 -> Communication
                ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_VIDEO, ApplicationInfo.CATEGORY_IMAGE -> Media
                ApplicationInfo.CATEGORY_MAPS, 9 -> Travel
                5 -> Shopping
                8 -> Finance
                10 -> Health
                6 -> Education
                else -> Other
            }
        }
    }
}

data class LauncherUiState(
    val installedApps: List<LauncherApp> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val icons: Map<String, ResolvedIcon> = emptyMap(),
    val hiddenApps: Set<String> = emptySet(),
    val customLabels: Map<String, String> = emptyMap()
)

class LauncherViewModel(application: android.app.Application) :
    AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()
    private val iconResolver = IconResolver(application)
    val activeIconPack: String? get() = iconResolver.activeIconPack
    private val appPrefs = AppPreferencesRepository(application)

    init { refreshApps() }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun refreshApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = queryLaunchableApps(getApplication())
            val keys = apps.map(::appKey)
            iconResolver.preloadIcons(keys)
            val hiddenApps = appPrefs.getHiddenApps()
            val customLabels = appPrefs.getCustomLabels()
            _uiState.update { state ->
                state.copy(
                    installedApps = apps,
                    isLoading = false,
                    hiddenApps = hiddenApps,
                    customLabels = customLabels,
                    icons = buildMap {
                        keys.forEach { key ->
                            iconResolver.resolve(key)?.let { put(key, it) }
                        }
                    }
                )
            }
        }
    }

    fun hideApp(key: String) {
        appPrefs.hideApp(key)
        _uiState.update { it.copy(hiddenApps = appPrefs.getHiddenApps()) }
    }

    fun showApp(key: String) {
        appPrefs.showApp(key)
        _uiState.update { it.copy(hiddenApps = appPrefs.getHiddenApps()) }
    }

    fun renameApp(key: String, newLabel: String) {
        if (newLabel.isBlank()) appPrefs.removeCustomLabel(key)
        else appPrefs.setCustomLabel(key, newLabel.trim())
        _uiState.update { it.copy(customLabels = appPrefs.getCustomLabels()) }
    }

    fun getDisplayLabel(app: LauncherApp): String {
        val key = appKey(app)
        return _uiState.value.customLabels[key] ?: app.label
    }

    fun getInstalledIconPacks(): List<IconPackInfo> = iconResolver.queryInstalledIconPacks()

    fun setIconPack(packageName: String?) {
        iconResolver.setIconPack(packageName)
        refreshApps()
    }

    private fun queryLaunchableApps(application: android.app.Application): List<LauncherApp> {
        val appsFromLauncherService = runCatching {
            val launcherApps = application.getSystemService(LauncherApps::class.java)
            launcherApps
                ?.getActivityList(null, Process.myUserHandle())
                ?.asSequence()
                ?.mapNotNull { activityInfo ->
                    val component = activityInfo.componentName
                    if (component.packageName == application.packageName) return@mapNotNull null
                    val label = activityInfo.label?.toString()
                        ?.takeIf { it.isNotBlank() }
                        ?: component.className.substringAfterLast('.')
                    LauncherApp(
                        label = label,
                        packageName = component.packageName,
                        activityName = component.className,
                        category = AppCategory.fromAppInfo(activityInfo.applicationInfo)
                    )
                }
                ?.toList()
                .orEmpty()
        }.getOrDefault(emptyList())

        val apps = if (appsFromLauncherService.isNotEmpty()) {
            appsFromLauncherService
        } else {
            queryLaunchableAppsFromPackageManager(application)
        }

        return apps
            .distinctBy(::appKey)
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
    }

    private fun queryLaunchableAppsFromPackageManager(application: android.app.Application): List<LauncherApp> {
        val packageManager = application.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return packageManager
            .queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
            .asSequence()
            .mapNotNull { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                if (activityInfo.packageName == application.packageName) return@mapNotNull null
                val label = resolveInfo.loadLabel(packageManager)?.toString()
                    ?.takeIf { it.isNotBlank() }
                    ?: activityInfo.name.substringAfterLast('.')
                val category = runCatching {
                    AppCategory.fromAppInfo(
                        packageManager.getApplicationInfo(activityInfo.packageName, 0)
                    )
                }.getOrDefault(AppCategory.Other)
                LauncherApp(label, activityInfo.packageName, activityInfo.name, category)
            }
            .toList()
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun QuackLauncherRoot(
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
    val pagerState = rememberPagerState(initialPage = PAGER_ANCHOR_PAGE, pageCount = { Int.MAX_VALUE })
    val pagerScope = rememberCoroutineScope()
    val appDrawerState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val layoutRepository = remember { LayoutRepository(context) }
    val savedLayout = remember { layoutRepository.loadLayout() }
    val notificationCounts by LauncherNotificationListener.notificationCounts.collectAsStateWithLifecycle()

    var isAppDrawerOpen by rememberSaveable { mutableStateOf(false) }
    var isEditMode by rememberSaveable { mutableStateOf(false) }
    var isScreenEditorOpen by rememberSaveable { mutableStateOf(false) }
    var isWidgetPickerOpen by rememberSaveable { mutableStateOf(false) }
    var pendingScreenSide by rememberSaveable { mutableStateOf(ScreenSide.Left.name) }
    var workspaceSlots by remember { mutableStateOf(validateWidgetSlots(savedLayout?.workspaceSlots ?: List(WORKSPACE_SLOTS) { null }, widgetHostController)) }
    var dockSlots by remember { mutableStateOf(validateWidgetSlots(savedLayout?.dockSlots ?: List(DOCK_APP_SLOTS) { null }, widgetHostController)) }
    var layoutInitialized by remember { mutableStateOf(savedLayout != null) }
    var selectedSlot by remember { mutableStateOf<SlotSelection?>(null) }
    val workspaceBounds = remember { mutableStateMapOf<Int, Rect>() }
    val dockBounds = remember { mutableStateMapOf<Int, Rect>() }
    var dragInProgress by remember { mutableStateOf<DragInProgress?>(null) }
    var dragHoverTarget by remember { mutableStateOf<SlotSelection?>(null) }
    var leftScreenIds by rememberSaveable { mutableStateOf(savedLayout?.leftScreenIds ?: emptyList()) }
    var rightScreenIds by rememberSaveable { mutableStateOf(savedLayout?.rightScreenIds ?: emptyList()) }
    val leftScreens = remember(leftScreenIds) { leftScreenIds.mapNotNull(ExtensionScreenType::fromId) }
    val rightScreens = remember(rightScreenIds) { rightScreenIds.mapNotNull(ExtensionScreenType::fromId) }
    val availableWidgets = remember { widgetHostController.installedProviders() }

    val appsByKey = remember(uiState.installedApps) { uiState.installedApps.associateBy(::appKey) }
    val iconsByKey = remember(uiState.icons) { uiState.icons }

    LaunchedEffect(uiState.installedApps, layoutInitialized) {
        if (!layoutInitialized && uiState.installedApps.isNotEmpty()) {
            val installedKeys = uiState.installedApps.map(::appKey)
            workspaceSlots = List(WORKSPACE_SLOTS) { index -> installedKeys.getOrNull(index)?.let { SlotContent.App(it) } }
            dockSlots = List(DOCK_APP_SLOTS) { index -> installedKeys.getOrNull(index)?.let { SlotContent.App(it) } }
            layoutInitialized = true
        }
    }

    val isAnchorPage by remember { derivedStateOf { pagerState.currentPage == PAGER_ANCHOR_PAGE } }
    val dockAlpha by animateFloatAsState(
        targetValue = if (isAnchorPage) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = LocalMotionTokens.current.springStiffness
        ),
        label = "dock-fade"
    )

    val filteredApps by remember(
        uiState.installedApps,
        uiState.searchQuery,
        uiState.hiddenApps,
        uiState.customLabels
    ) {
        derivedStateOf {
            val q = uiState.searchQuery.trim()
            val visible = uiState.installedApps.filter { app ->
                appKey(app) !in uiState.hiddenApps
            }
            if (q.isBlank()) visible
            else visible.filter { app ->
                val displayLabel = uiState.customLabels[appKey(app)] ?: app.label
                displayLabel.contains(q, ignoreCase = true) ||
                    app.label.contains(q, ignoreCase = true) ||
                    app.packageName.contains(q, ignoreCase = true)
            }
        }
    }

    fun persistLayout() {
        layoutRepository.saveLayout(workspaceSlots, dockSlots, leftScreenIds, rightScreenIds)
    }

    fun assignSlot(selection: SlotSelection, content: SlotContent?) {
        when (selection.area) {
            SlotArea.Workspace -> workspaceSlots = workspaceSlots.toMutableList().apply { this[selection.index] = content }
            SlotArea.Dock -> dockSlots = dockSlots.toMutableList().apply { this[selection.index] = content }
        }
        persistLayout()
    }

    fun contentAtSlot(selection: SlotSelection): SlotContent? = when (selection.area) {
        SlotArea.Workspace -> workspaceSlots.getOrNull(selection.index)
        SlotArea.Dock -> dockSlots.getOrNull(selection.index)
    }

    fun swapSlots(from: SlotSelection, to: SlotSelection) {
        val fromValue = contentAtSlot(from)
        val toValue = contentAtSlot(to)
        assignSlot(from, toValue)
        assignSlot(to, fromValue)
    }

    fun findDropTarget(pointerInRoot: Offset): SlotSelection? {
        workspaceBounds.entries.firstOrNull { (_, rect) -> rect.contains(pointerInRoot) }?.let { return SlotSelection(SlotArea.Workspace, it.key) }
        dockBounds.entries.firstOrNull { (_, rect) -> rect.contains(pointerInRoot) }?.let { return SlotSelection(SlotArea.Dock, it.key) }
        return null
    }

    fun beginDrag(source: SlotSelection, content: SlotContent, pointerInRoot: Offset) {
        if (!isEditMode) return
        val label = when (content) {
            is SlotContent.App -> appsByKey[content.key]?.label ?: "App"
            is SlotContent.Widget -> "Widget"
        }
        dragInProgress = DragInProgress(source, label, pointerInRoot)
        dragHoverTarget = source
        selectedSlot = null
    }

    fun updateDrag(pointerInRoot: Offset) {
        dragInProgress?.let { dragInProgress = it.copy(pointerInRoot = pointerInRoot) }
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

    fun cancelDrag() { dragInProgress = null; dragHoverTarget = null }

    fun handleSlotTap(area: SlotArea, index: Int) {
        if (dragInProgress != null) return
        val tapped = SlotSelection(area, index)
        val tappedContent = contentAtSlot(tapped)
        if (!isEditMode) {
            if (tappedContent is SlotContent.App) appsByKey[tappedContent.key]?.let { launchApp(context, it) }
            return
        }
        when {
            selectedSlot == null -> { if (tappedContent != null) selectedSlot = tapped }
            selectedSlot == tapped -> { selectedSlot = null }
            else -> { swapSlots(selectedSlot!!, tapped); selectedSlot = null }
        }
    }

    fun handleWidgetSelected(providerInfo: AppWidgetProviderInfo) {
        val appWidgetId = widgetHostController.allocateAppWidgetId()
        val provider = providerInfo.provider
        requestWidgetBind(appWidgetId, provider) { widgetSlot ->
            if (widgetSlot != null) {
                val target = selectedSlot
                if (target != null) { assignSlot(target, widgetSlot); selectedSlot = null }
                else {
                    val firstEmpty = workspaceSlots.indexOfFirst { it == null }
                    if (firstEmpty >= 0) { workspaceSlots = workspaceSlots.toMutableList().apply { this[firstEmpty] = widgetSlot }; persistLayout() }
                    else widgetHostController.deleteAppWidgetId(appWidgetId)
                }
            }
            isWidgetPickerOpen = false
        }
    }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                when {
                    page == PAGER_ANCHOR_PAGE -> AnchorHomeScreen(
                        workspaceSlots, appsByKey, iconsByKey, notificationCounts, widgetHostController,
                        isEditMode, selectedSlot, dragHoverTarget, dragInProgress?.source,
                        uiState.customLabels,
                        onOpenDrawer = { isAppDrawerOpen = true },
                        onEnterEditMode = { isEditMode = true; selectedSlot = null },
                        onWorkspaceSlotTapped = { handleSlotTap(SlotArea.Workspace, it) },
                        onWorkspaceSlotBoundsChanged = { i, b -> workspaceBounds[i] = b },
                        onWorkspaceDragStart = { i, p -> contentAtSlot(SlotSelection(SlotArea.Workspace, i))?.let { beginDrag(SlotSelection(SlotArea.Workspace, i), it, p) } },
                        onWorkspaceDragMove = ::updateDrag, onWorkspaceDragEnd = ::finishDrag, onWorkspaceDragCancel = ::cancelDrag
                    )
                    page < PAGER_ANCHOR_PAGE -> {
                        val offset = PAGER_ANCHOR_PAGE - page
                        val screenType = leftScreens.getOrNull(offset - 1)
                        if (screenType == null) UnassignedExtensionPage("Left", offset, isEditMode, { pendingScreenSide = ScreenSide.Left.name; isScreenEditorOpen = true }) { pagerScope.launch { pagerState.animateScrollToPage(PAGER_ANCHOR_PAGE) } }
                        else when (screenType) {
                            ExtensionScreenType.Gallery -> GalleryScreen(isActive = page == pagerState.currentPage)
                            ExtensionScreenType.Weather -> WeatherScreen(isActive = page == pagerState.currentPage)
                            ExtensionScreenType.Files -> FilesScreen(isActive = page == pagerState.currentPage)
                        }
                    }
                    else -> {
                        val offset = page - PAGER_ANCHOR_PAGE
                        val screenType = rightScreens.getOrNull(offset - 1)
                        if (screenType == null) UnassignedExtensionPage("Right", offset, isEditMode, { pendingScreenSide = ScreenSide.Right.name; isScreenEditorOpen = true }) { pagerScope.launch { pagerState.animateScrollToPage(PAGER_ANCHOR_PAGE) } }
                        else when (screenType) {
                            ExtensionScreenType.Gallery -> GalleryScreen(isActive = page == pagerState.currentPage)
                            ExtensionScreenType.Weather -> WeatherScreen(isActive = page == pagerState.currentPage)
                            ExtensionScreenType.Files -> FilesScreen(isActive = page == pagerState.currentPage)
                        }
                    }
                }
            }

            GlobalDock(
                dockSlots, appsByKey, iconsByKey, notificationCounts, widgetHostController,
                isEditMode, selectedSlot, dragHoverTarget, dragInProgress?.source,
                uiState.customLabels,
                onDockSlotTapped = { handleSlotTap(SlotArea.Dock, it) },
                onDockSlotBoundsChanged = { i, b -> dockBounds[i] = b },
                onDockDragStart = { i, p -> contentAtSlot(SlotSelection(SlotArea.Dock, i))?.let { beginDrag(SlotSelection(SlotArea.Dock, i), it, p) } },
                onDockDragMove = ::updateDrag, onDockDragEnd = ::finishDrag, onDockDragCancel = ::cancelDrag,
                onOpenDrawer = { isAppDrawerOpen = true },
                modifier = Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(horizontal = 16.dp, vertical = 12.dp).alpha(dockAlpha)
            )

            if (isEditMode && isAnchorPage) {
                dragInProgress?.let { drag ->
                    DraggedAppPreview(label = drag.label, modifier = Modifier.offset { IntOffset((drag.pointerInRoot.x - 26f).roundToInt(), (drag.pointerInRoot.y - 26f).roundToInt()) })
                }
                val iconPacks = remember { viewModel.getInstalledIconPacks() }
                EditModeOverlay(
                    leftScreens.size, rightScreens.size, iconPacks, viewModel.activeIconPack,
                    onAddLeft = { pendingScreenSide = ScreenSide.Left.name; isScreenEditorOpen = true },
                    onAddRight = { pendingScreenSide = ScreenSide.Right.name; isScreenEditorOpen = true },
                    onWidgets = { isWidgetPickerOpen = true },
                    onDone = { isEditMode = false; selectedSlot = null },
                    onIconPackSelected = { viewModel.setIconPack(it) },
                    modifier = Modifier.align(Alignment.TopCenter).safeDrawingPadding().padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }

    if (isAppDrawerOpen) {
        ModalBottomSheet(onDismissRequest = { isAppDrawerOpen = false }, sheetState = appDrawerState) {
            AppDrawerSheet(
                isLoading = uiState.isLoading,
                apps = filteredApps,
                icons = iconsByKey,
                notificationCounts = notificationCounts,
                searchQuery = uiState.searchQuery,
                customLabels = uiState.customLabels,
                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                onAppClicked = { app ->
                    val selected = selectedSlot
                    if (isEditMode) {
                        if (selected != null) { assignSlot(selected, SlotContent.App(appKey(app))); selectedSlot = null }
                        else {
                            val firstEmpty = workspaceSlots.indexOfFirst { it == null }
                            if (firstEmpty >= 0) { workspaceSlots = workspaceSlots.toMutableList().apply { this[firstEmpty] = SlotContent.App(appKey(app)) }; persistLayout() }
                            else launchApp(context, app)
                        }
                    } else launchApp(context, app)
                    isAppDrawerOpen = false
                },
                onHideApp = { key -> viewModel.hideApp(key) },
                onRenameApp = { key, label -> viewModel.renameApp(key, label) }
            )
        }
    }

    if (isScreenEditorOpen) {
        ScreenEditorSheet(ScreenSide.fromName(pendingScreenSide), { isScreenEditorOpen = false }) { side, type ->
            when (side) { ScreenSide.Left -> leftScreenIds = leftScreenIds + type.id; ScreenSide.Right -> rightScreenIds = rightScreenIds + type.id }
            persistLayout()
            isScreenEditorOpen = false
        }
    }

    if (isWidgetPickerOpen) {
        WidgetPickerSheet(availableWidgets, { handleWidgetSelected(it) }) { isWidgetPickerOpen = false }
    }
}

// ── Anchor Home Screen ──

@Composable
private fun AnchorHomeScreen(
    workspaceSlots: List<SlotContent?>,
    appsByKey: Map<String, LauncherApp>,
    iconsByKey: Map<String, ResolvedIcon>,
    notificationCounts: Map<String, Int>,
    widgetHostController: LauncherWidgetHostController,
    isEditMode: Boolean,
    selectedSlot: SlotSelection?,
    dragHoverTarget: SlotSelection?,
    dragSource: SlotSelection?,
    customLabels: Map<String, String>,
    onOpenDrawer: () -> Unit,
    onEnterEditMode: () -> Unit,
    onWorkspaceSlotTapped: (Int) -> Unit,
    onWorkspaceSlotBoundsChanged: (Int, Rect) -> Unit,
    onWorkspaceDragStart: (Int, Offset) -> Unit,
    onWorkspaceDragMove: (Offset) -> Unit,
    onWorkspaceDragEnd: () -> Unit,
    onWorkspaceDragCancel: () -> Unit
) {
    val context = LocalContext.current
    var cumulativeDrag by remember { mutableStateOf(0f) }
    val now by produceState(initialValue = LocalDateTime.now()) { while (true) { value = LocalDateTime.now(); delay(30_000) } }

    Column(
        modifier = Modifier.fillMaxSize().pointerInput(Unit) {
            detectTapGestures(
                onDoubleTap = {
                    if (!GestureHelper.lockScreen(context)) {
                        GestureHelper.requestDeviceAdmin(context)
                    }
                }
            )
        }.pointerInput(Unit) {
            detectVerticalDragGestures(
                onVerticalDrag = { _, dragAmount -> cumulativeDrag += dragAmount },
                onDragEnd = {
                    when {
                        cumulativeDrag < SWIPE_UP_THRESHOLD -> onOpenDrawer()
                        cumulativeDrag > SWIPE_DOWN_THRESHOLD -> GestureHelper.expandNotificationShade(context)
                    }
                    cumulativeDrag = 0f
                },
                onDragCancel = { cumulativeDrag = 0f }
            )
        }.padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = now.format(timeFormatter), style = MaterialTheme.typography.displayLarge)
        Text(text = now.format(dateFormatter), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = if (isEditMode) "Edit mode: tap an icon, then tap another slot to swap." else "Swipe up for app drawer. Long-press an empty slot to edit.",
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FilledTonalButton(onClick = onOpenDrawer) {
            Icon(imageVector = Icons.Rounded.KeyboardArrowUp, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text(text = if (isEditMode) "Open Drawer (assign app)" else "Open App Drawer")
        }
        WorkspaceCard(
            workspaceSlots, appsByKey, iconsByKey, notificationCounts, widgetHostController,
            isEditMode, selectedSlot, dragHoverTarget, dragSource, customLabels,
            onWorkspaceSlotTapped, onEnterEditMode, onWorkspaceSlotBoundsChanged,
            onWorkspaceDragStart, onWorkspaceDragMove, onWorkspaceDragEnd, onWorkspaceDragCancel
        )
    }
}

// ── Workspace Card ──

@Composable
private fun WorkspaceCard(
    workspaceSlots: List<SlotContent?>,
    appsByKey: Map<String, LauncherApp>,
    iconsByKey: Map<String, ResolvedIcon>,
    notificationCounts: Map<String, Int>,
    widgetHostController: LauncherWidgetHostController,
    isEditMode: Boolean, selectedSlot: SlotSelection?, dragHoverTarget: SlotSelection?, dragSource: SlotSelection?,
    customLabels: Map<String, String>,
    onWorkspaceSlotTapped: (Int) -> Unit, onLongPressEmptySlot: () -> Unit,
    onWorkspaceSlotBoundsChanged: (Int, Rect) -> Unit,
    onWorkspaceDragStart: (Int, Offset) -> Unit, onWorkspaceDragMove: (Offset) -> Unit,
    onWorkspaceDragEnd: () -> Unit, onWorkspaceDragCancel: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(LocalShapeTokens.current.cardCornerRadius)) {
        Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Home Workspace \u00b7 4 \u00d7 5", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            LazyVerticalGrid(
                columns = GridCells.Fixed(WORKSPACE_COLUMNS),
                modifier = Modifier.fillMaxWidth().height(300.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                userScrollEnabled = false
            ) {
                items(WORKSPACE_SLOTS) { index ->
                    val content = workspaceSlots.getOrNull(index)
                    val ak = if (content is SlotContent.App) content.key else null
                    WorkspaceSlot(
                        content = content,
                        app = if (content is SlotContent.App) appsByKey[content.key] else null,
                        displayLabel = if (content is SlotContent.App) customLabels[content.key] ?: appsByKey[content.key]?.label else null,
                        icon = ak?.let { iconsByKey[it] },
                        hasNotification = ak?.let { val pkg = it.substringBefore("/"); (notificationCounts[pkg] ?: 0) > 0 } ?: false,
                        widgetHostController = widgetHostController,
                        isEditMode = isEditMode,
                        isSelected = selectedSlot == SlotSelection(SlotArea.Workspace, index),
                        isDropTarget = dragHoverTarget == SlotSelection(SlotArea.Workspace, index),
                        isDragSource = dragSource == SlotSelection(SlotArea.Workspace, index),
                        onTap = { onWorkspaceSlotTapped(index) },
                        onLongPressEmpty = { if (content == null) onLongPressEmptySlot() },
                        onBoundsChanged = { onWorkspaceSlotBoundsChanged(index, it) },
                        onDragStart = { onWorkspaceDragStart(index, it) },
                        onDragMove = onWorkspaceDragMove, onDragEnd = onWorkspaceDragEnd, onDragCancel = onWorkspaceDragCancel
                    )
                }
            }
        }
    }
}

// ── Workspace Slot ──

@Composable
private fun WorkspaceSlot(
    content: SlotContent?, app: LauncherApp?, displayLabel: String?, icon: ResolvedIcon?, hasNotification: Boolean,
    widgetHostController: LauncherWidgetHostController,
    isEditMode: Boolean, isSelected: Boolean, isDropTarget: Boolean, isDragSource: Boolean,
    onTap: () -> Unit, onLongPressEmpty: () -> Unit, onBoundsChanged: (Rect) -> Unit,
    onDragStart: (Offset) -> Unit, onDragMove: (Offset) -> Unit, onDragEnd: () -> Unit, onDragCancel: () -> Unit
) {
    var slotBounds by remember { mutableStateOf<Rect?>(null) }
    val motionTokens = LocalMotionTokens.current
    val shapeTokens = LocalShapeTokens.current
    val scale by animateFloatAsState(
        targetValue = when { isDragSource -> 0.85f; isDropTarget -> 1.08f; isSelected -> 1.05f; else -> 1f },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = motionTokens.springStiffness),
        label = "slot-scale"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box {
            Surface(
                modifier = Modifier.size(52.dp).graphicsLayer { scaleX = scale; scaleY = scale }
                    .onGloballyPositioned { slotBounds = it.boundsInRoot(); onBoundsChanged(it.boundsInRoot()) }
                    .pointerInput(content, isEditMode) { detectTapGestures(onTap = { onTap() }, onLongPress = { if (content == null) onLongPressEmpty() }) }
                    .pointerInput(content, isEditMode) {
                        if (!isEditMode || content == null) return@pointerInput
                        var currentPointer = Offset.Zero
                        detectDragGesturesAfterLongPress(
                            onDragStart = { localOffset -> currentPointer = (slotBounds?.topLeft ?: Offset.Zero) + localOffset; onDragStart(currentPointer) },
                            onDrag = { change, dragAmount -> change.consume(); currentPointer += dragAmount; onDragMove(currentPointer) },
                            onDragEnd = { onDragEnd() }, onDragCancel = { onDragCancel() }
                        )
                    },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(shapeTokens.iconCornerRadius),
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
                        null -> Icon(imageVector = Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        is SlotContent.App -> {
                            if (icon != null) Image(bitmap = icon.imageBitmap, contentDescription = app?.label, modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Fit)
                            else Text(text = (app?.label?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        is SlotContent.Widget -> {
                            val providerInfo = remember(content.appWidgetId) { widgetHostController.getWidgetInfo(content.appWidgetId) }
                            if (providerInfo != null) {
                                AndroidView(factory = { ctx ->
                                    FrameLayout(ctx).apply {
                                        layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                                        addView(widgetHostController.createHostView(content.appWidgetId, providerInfo).apply { layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT) })
                                    }
                                }, modifier = Modifier.fillMaxSize())
                            } else Text("W", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            if (hasNotification && !isEditMode) NotificationDot(modifier = Modifier.align(Alignment.TopEnd))
        }
        Text(
            text = when { content is SlotContent.App && app != null -> displayLabel ?: app.label; content is SlotContent.Widget -> "Widget"; isEditMode -> "Empty"; else -> "" },
            maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center
        )
    }
}

// ── Edit Mode Overlay ──

@Composable
private fun EditModeOverlay(
    leftCount: Int, rightCount: Int,
    iconPacks: List<IconPackInfo>,
    activeIconPack: String?,
    onAddLeft: () -> Unit, onAddRight: () -> Unit, onWidgets: () -> Unit, onDone: () -> Unit,
    onIconPackSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showIconPacks by remember { mutableStateOf(false) }

    Surface(modifier = modifier, shape = androidx.compose.foundation.shape.RoundedCornerShape(LocalShapeTokens.current.buttonCornerRadius), tonalElevation = 8.dp, shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f)) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Edit Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Screens \u00b7 Left: $leftCount  |  Right: $rightCount", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(onClick = onWidgets) { Icon(Icons.Rounded.ViewAgenda, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text("Widgets") }
                FilledTonalButton(onClick = onAddLeft) { Text("Add Left") }
                FilledTonalButton(onClick = onAddRight) { Text("Add Right") }
                OutlinedButton(onClick = onDone) { Text("Done") }
            }
            if (iconPacks.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Icons:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(
                        onClick = { showIconPacks = !showIconPacks },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = activeIconPack?.let { pkg -> iconPacks.find { it.packageName == pkg }?.label ?: "System" } ?: "System",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                if (showIconPacks) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Surface(
                            onClick = { onIconPackSelected(null); showIconPacks = false },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            color = if (activeIconPack == null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text("System Default", modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
                        }
                        iconPacks.forEach { pack ->
                            Surface(
                                onClick = { onIconPackSelected(pack.packageName); showIconPacks = false },
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                                color = if (activeIconPack == pack.packageName) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(pack.label, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Unassigned Extension Page ──

@Composable
private fun UnassignedExtensionPage(
    direction: String, index: Int, isEditMode: Boolean,
    onAddScreen: () -> Unit, onGoHome: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        ElevatedCard(shape = androidx.compose.foundation.shape.RoundedCornerShape(LocalShapeTokens.current.dialogCornerRadius)) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("$direction page $index", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text("Unassigned App Screen", style = MaterialTheme.typography.displayMedium)
                Text(
                    text = if (isEditMode) "This page is ready. Add Gallery, Files, or Weather from the editor." else "Long-press an empty slot on Home to enter Edit Mode and assign this page.",
                    style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isEditMode) FilledTonalButton(onClick = onAddScreen) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.size(8.dp)); Text("Assign Screen") }
                OutlinedButton(onClick = onGoHome) { Icon(Icons.Rounded.Home, null); Spacer(Modifier.size(8.dp)); Text("Back Home") }
            }
        }
    }
}

// ── Global Dock ──

@Composable
private fun GlobalDock(
    slotContents: List<SlotContent?>, appsByKey: Map<String, LauncherApp>,
    iconsByKey: Map<String, ResolvedIcon>, notificationCounts: Map<String, Int>,
    widgetHostController: LauncherWidgetHostController,
    isEditMode: Boolean, selectedSlot: SlotSelection?, dragHoverTarget: SlotSelection?, dragSource: SlotSelection?,
    customLabels: Map<String, String>,
    onDockSlotTapped: (Int) -> Unit, onDockSlotBoundsChanged: (Int, Rect) -> Unit,
    onDockDragStart: (Int, Offset) -> Unit, onDockDragMove: (Offset) -> Unit,
    onDockDragEnd: () -> Unit, onDockDragCancel: () -> Unit, onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier, shape = androidx.compose.foundation.shape.RoundedCornerShape(LocalShapeTokens.current.dockCornerRadius), tonalElevation = 8.dp, shadowElevation = 10.dp, color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f)) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(DOCK_APP_SLOTS) { index ->
                val content = slotContents.getOrNull(index)
                val ak = if (content is SlotContent.App) content.key else null
                DockSlot(
                    content = content, app = if (content is SlotContent.App) appsByKey[content.key] else null,
                    displayLabel = if (content is SlotContent.App) customLabels[content.key] ?: appsByKey[content.key]?.label else null,
                    icon = ak?.let { iconsByKey[it] },
                    hasNotification = ak?.let { val pkg = it.substringBefore("/"); (notificationCounts[pkg] ?: 0) > 0 } ?: false,
                    widgetHostController = widgetHostController,
                    isEditMode = isEditMode,
                    isSelected = selectedSlot == SlotSelection(SlotArea.Dock, index),
                    isDropTarget = dragHoverTarget == SlotSelection(SlotArea.Dock, index),
                    isDragSource = dragSource == SlotSelection(SlotArea.Dock, index),
                    onTap = { onDockSlotTapped(index) },
                    onBoundsChanged = { onDockSlotBoundsChanged(index, it) },
                    onDragStart = { onDockDragStart(index, it) },
                    onDragMove = onDockDragMove, onDragEnd = onDockDragEnd, onDragCancel = onDockDragCancel
                )
            }
            IconButton(onClick = onOpenDrawer) { Icon(Icons.Rounded.Apps, "Open app drawer") }
        }
    }
}

// ── Dock Slot ──

@Composable
private fun DockSlot(
    content: SlotContent?, app: LauncherApp?, displayLabel: String?, icon: ResolvedIcon?, hasNotification: Boolean,
    widgetHostController: LauncherWidgetHostController,
    isEditMode: Boolean, isSelected: Boolean, isDropTarget: Boolean, isDragSource: Boolean,
    onTap: () -> Unit, onBoundsChanged: (Rect) -> Unit,
    onDragStart: (Offset) -> Unit, onDragMove: (Offset) -> Unit, onDragEnd: () -> Unit, onDragCancel: () -> Unit
) {
    var slotBounds by remember { mutableStateOf<Rect?>(null) }
    val motionTokens = LocalMotionTokens.current
    val scale by animateFloatAsState(
        targetValue = when { isDragSource -> 0.8f; isDropTarget -> 1.12f; isSelected -> 1.06f; else -> 1f },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = motionTokens.springStiffness),
        label = "dock-slot-scale"
    )

    Column(modifier = Modifier.widthIn(min = 48.dp, max = 64.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box {
            Surface(
                modifier = Modifier.size(40.dp).graphicsLayer { scaleX = scale; scaleY = scale }
                    .onGloballyPositioned { slotBounds = it.boundsInRoot(); onBoundsChanged(it.boundsInRoot()) }
                    .pointerInput(content, isEditMode) { detectTapGestures(onTap = { onTap() }) }
                    .pointerInput(content, isEditMode) {
                        if (!isEditMode || content == null) return@pointerInput
                        var currentPointer = Offset.Zero
                        detectDragGesturesAfterLongPress(
                            onDragStart = { localOffset -> currentPointer = (slotBounds?.topLeft ?: Offset.Zero) + localOffset; onDragStart(currentPointer) },
                            onDrag = { change, dragAmount -> change.consume(); currentPointer += dragAmount; onDragMove(currentPointer) },
                            onDragEnd = { onDragEnd() }, onDragCancel = { onDragCancel() }
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
                        null -> Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        is SlotContent.App -> {
                            if (icon != null) Image(bitmap = icon.imageBitmap, contentDescription = app?.label, modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Fit)
                            else Text(text = (app?.label?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        is SlotContent.Widget -> Text("W", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (hasNotification && !isEditMode) NotificationDot(modifier = Modifier.align(Alignment.TopEnd))
        }
        Text(
            text = when { content is SlotContent.App && app != null -> displayLabel ?: app.label; content is SlotContent.Widget -> "Widget"; isEditMode -> "Empty"; else -> "" },
            style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
        )
    }
}

// ── Dragged App Preview ──

@Composable
private fun DraggedAppPreview(label: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.size(52.dp), shape = androidx.compose.foundation.shape.RoundedCornerShape(LocalShapeTokens.current.iconCornerRadius), tonalElevation = 12.dp, shadowElevation = 12.dp, color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.94f)) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = label.firstOrNull()?.uppercaseChar()?.toString() ?: "?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── App Drawer Sheet ──

@Composable
private fun AppDrawerSheet(
    isLoading: Boolean, apps: List<LauncherApp>, icons: Map<String, ResolvedIcon>,
    notificationCounts: Map<String, Int>, searchQuery: String,
    customLabels: Map<String, String>,
    onSearchQueryChanged: (String) -> Unit, onAppClicked: (LauncherApp) -> Unit,
    onHideApp: (String) -> Unit, onRenameApp: (String, String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<AppCategory?>(null) }
    var renameDialogApp by remember { mutableStateOf<LauncherApp?>(null) }
    var renameText by remember { mutableStateOf("") }

    val categorizedApps by remember(apps, selectedCategory) {
        derivedStateOf {
            when (selectedCategory) {
                null -> apps
                else -> apps.filter { it.category == selectedCategory }
            }
        }
    }

    val availableCategories by remember(apps) {
        derivedStateOf {
            apps.groupBy { it.category }
                .filter { it.key != AppCategory.Other && it.value.size >= 2 }
                .keys
                .sortedBy { it.ordinal }
        }
    }

    Column(modifier = Modifier.fillMaxWidth().imePadding().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("App Drawer", style = MaterialTheme.typography.displayMedium)
        OutlinedTextField(value = searchQuery, onValueChange = onSearchQueryChanged, modifier = Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Icon(Icons.Rounded.Search, null) }, placeholder = { Text("Search by name or package") })

        if (availableCategories.isNotEmpty()) {
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                item {
                    Surface(
                        onClick = { selectedCategory = null },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                        color = if (selectedCategory == null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text("All", modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
                    }
                }
                items(availableCategories.size) { index ->
                    val cat = availableCategories[index]
                    Surface(
                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                        color = if (selectedCategory == cat) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(cat.label, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        when {
            isLoading -> Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            categorizedApps.isEmpty() -> Text("No apps matched your search.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> LazyColumn(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items = categorizedApps, key = { a -> appKey(a) }) { app ->
                    val displayLabel = customLabels[appKey(app)] ?: app.label
                    Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(LocalShapeTokens.current.listItemCornerRadius), tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(modifier = Modifier.size(36.dp).clickable { onAppClicked(app) }, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                                Box(contentAlignment = Alignment.Center) {
                                    val drawerIcon = icons[appKey(app)]
                                    if (drawerIcon != null) Image(bitmap = drawerIcon.imageBitmap, contentDescription = displayLabel, modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Fit)
                                    else Text(text = displayLabel.firstOrNull()?.uppercaseChar()?.toString() ?: "?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    if ((notificationCounts[app.packageName] ?: 0) > 0) NotificationDot(modifier = Modifier.align(Alignment.TopEnd))
                                }
                            }
                            Column(modifier = Modifier.weight(1f).clickable { onAppClicked(app) }, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(text = displayLabel, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(text = app.packageName, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row {
                                IconButton(onClick = { renameDialogApp = app; renameText = customLabels[appKey(app)] ?: app.label }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Rounded.Edit, "Rename", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { onHideApp(appKey(app)) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Rounded.VisibilityOff, "Hide", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    renameDialogApp?.let { app ->
        AlertDialog(
            onDismissRequest = { renameDialogApp = null },
            title = { Text("Rename app") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    label = { Text("Label") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRenameApp(appKey(app), renameText)
                    renameDialogApp = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renameDialogApp = null }) { Text("Cancel") }
            }
        )
    }
}

// ── Screen Editor Sheet ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenEditorSheet(
    initialSide: ScreenSide, onDismissRequest: () -> Unit,
    onConfirm: (ScreenSide, ExtensionScreenType) -> Unit
) {
    var selectedSide by remember(initialSide) { mutableStateOf(initialSide) }
    var selectedType by remember { mutableStateOf(ExtensionScreenType.Gallery) }
    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.fillMaxWidth().imePadding().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Assign App Screen", style = MaterialTheme.typography.displayMedium)
            Text("Choose the side and which environment screen to append.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (selectedSide == ScreenSide.Left) FilledTonalButton(onClick = { selectedSide = ScreenSide.Left }) { Text("Left") } else OutlinedButton(onClick = { selectedSide = ScreenSide.Left }) { Text("Left") }
                if (selectedSide == ScreenSide.Right) FilledTonalButton(onClick = { selectedSide = ScreenSide.Right }) { Text("Right") } else OutlinedButton(onClick = { selectedSide = ScreenSide.Right }) { Text("Right") }
            }
            ExtensionScreenType.entries.forEach { type ->
                val isSelected = selectedType == type
                val icon = when (type) { ExtensionScreenType.Gallery -> Icons.Rounded.PhotoLibrary; ExtensionScreenType.Files -> Icons.Rounded.Folder; ExtensionScreenType.Weather -> Icons.Rounded.WbSunny }
                Surface(onClick = { selectedType = type }, shape = androidx.compose.foundation.shape.RoundedCornerShape(LocalShapeTokens.current.chipCornerRadius), color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant, tonalElevation = if (isSelected) 4.dp else 0.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp), color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp)) }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(type.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(type.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isSelected) Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
            }
            FilledTonalButton(onClick = { onConfirm(selectedSide, selectedType) }, modifier = Modifier.fillMaxWidth()) { Text("Add ${selectedType.title} Screen") }
        }
    }
}

// ── Notification Dot ──

@Composable
private fun NotificationDot(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.size(10.dp).offset(x = 2.dp, y = (-2).dp), shape = androidx.compose.foundation.shape.CircleShape, color = MaterialTheme.colorScheme.error) {}
}

// ── Utilities ──

private fun appKey(app: LauncherApp): String = "${app.packageName}/${app.activityName}"

private fun launchApp(context: Context, app: LauncherApp) {
    val targetComponent = ComponentName(app.packageName, app.activityName)
    val launchedByLauncherApps = runCatching {
        val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return@runCatching false
        launcherApps.startMainActivity(targetComponent, Process.myUserHandle(), null, null)
        true
    }.getOrDefault(false)

    if (launchedByLauncherApps) return

    val launchIntent = Intent(Intent.ACTION_MAIN).apply {
        component = targetComponent
        addCategory(Intent.CATEGORY_LAUNCHER)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    runCatching { context.startActivity(launchIntent) }.onFailure { Toast.makeText(context, "Unable to open ${app.label}", Toast.LENGTH_SHORT).show() }
}

private fun validateWidgetSlots(
    slots: List<SlotContent?>,
    widgetHostController: LauncherWidgetHostController
): List<SlotContent?> {
    return slots.map { slot ->
        if (slot is SlotContent.Widget && !widgetHostController.isWidgetIdValid(slot.appWidgetId)) null
        else slot
    }
}
