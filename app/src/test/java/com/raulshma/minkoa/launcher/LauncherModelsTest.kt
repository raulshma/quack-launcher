package com.raulshma.minkoa.launcher

import org.junit.Assert.*
import org.junit.Test

class LauncherUiStateTest {

    @Test
    fun defaultStateIsEmpty() {
        val state = LauncherUiState()
        assertTrue(state.installedApps.isEmpty())
        assertEquals("", state.searchQuery)
        assertTrue(state.isLoading)
        assertTrue(state.icons.isEmpty())
        assertTrue(state.hiddenApps.isEmpty())
        assertTrue(state.customLabels.isEmpty())
    }

    @Test
    fun stateCopyPreservesValues() {
        val state = LauncherUiState(
            installedApps = listOf(LauncherApp("Chrome", "com.chrome", "com.chrome.Main", AppCategory.Productivity)),
            searchQuery = "chr",
            isLoading = false,
            hiddenApps = setOf("com.spam/app"),
            customLabels = mapOf("com.chrome/main" to "Browser")
        )
        val copied = state.copy(searchQuery = "fire")
        assertEquals("fire", copied.searchQuery)
        assertEquals(state.installedApps, copied.installedApps)
        assertEquals(state.hiddenApps, copied.hiddenApps)
        assertEquals(state.customLabels, copied.customLabels)
    }
}

class LauncherAppTest {

    @Test
    fun appProperties() {
        val app = LauncherApp("Chrome", "com.chrome", "com.chrome.Main", AppCategory.Productivity)
        assertEquals("Chrome", app.label)
        assertEquals("com.chrome", app.packageName)
        assertEquals("com.chrome.Main", app.activityName)
        assertEquals(AppCategory.Productivity, app.category)
    }

    @Test
    fun appDefaultCategoryIsOther() {
        val app = LauncherApp("Test", "com.test", "com.test.Main")
        assertEquals(AppCategory.Other, app.category)
    }
}
