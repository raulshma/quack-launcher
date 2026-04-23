package com.raulshma.minkoa.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent

private const val DEFAULT_WIDGET_HOST_ID = 0xA11A

class LauncherWidgetHostController(
    context: Context,
    hostId: Int = DEFAULT_WIDGET_HOST_ID
) {
    private val appContext = context.applicationContext
    val appWidgetManager: AppWidgetManager =
        AppWidgetManager.getInstance(appContext)
    private val appWidgetHost = AppWidgetHost(appContext, hostId)

    fun startListening() {
        runCatching { appWidgetHost.startListening() }
    }

    fun isWidgetIdValid(appWidgetId: Int): Boolean {
        return runCatching { appWidgetManager.getAppWidgetInfo(appWidgetId) != null }.getOrDefault(false)
    }

    fun cleanupStaleWidgetIds(persistedIds: List<Int>): List<Int> {
        val validIds = appWidgetHost.appWidgetIds.toSet()
        val staleIds = persistedIds.filter { it !in validIds || !isWidgetIdValid(it) }
        staleIds.forEach { id ->
            runCatching { appWidgetHost.deleteAppWidgetId(id) }
        }
        return persistedIds.filter { it in validIds && isWidgetIdValid(it) }
    }

    fun stopListening() {
        runCatching { appWidgetHost.stopListening() }
    }

    fun allocateAppWidgetId(): Int = appWidgetHost.allocateAppWidgetId()

    fun deleteAppWidgetId(appWidgetId: Int) {
        appWidgetHost.deleteAppWidgetId(appWidgetId)
    }

    fun createHostView(
        appWidgetId: Int,
        providerInfo: AppWidgetProviderInfo
    ): AppWidgetHostView = appWidgetHost.createView(appContext, appWidgetId, providerInfo)

    fun installedProviders(): List<AppWidgetProviderInfo> =
        appWidgetManager.installedProviders

    fun bindAppWidgetIdIfAllowed(
        appWidgetId: Int,
        provider: ComponentName
    ): Boolean = appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, provider)

    fun getWidgetInfo(appWidgetId: Int): AppWidgetProviderInfo? =
        appWidgetManager.getAppWidgetInfo(appWidgetId)

    fun createBindIntent(
        appWidgetId: Int,
        provider: ComponentName
    ): Intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
    }
}
