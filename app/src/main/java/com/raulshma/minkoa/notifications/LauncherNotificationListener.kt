package com.raulshma.minkoa.notifications

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LauncherNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        refreshCounts()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
        _notificationCounts.value = emptyMap()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        refreshCounts()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        refreshCounts()
    }

    private fun refreshCounts() {
        val counts = mutableMapOf<String, Int>()
        activeNotifications?.forEach { sbn ->
            if (sbn.isOngoing) return@forEach
            val pkg = sbn.packageName
            counts[pkg] = (counts[pkg] ?: 0) + 1
        }
        _notificationCounts.value = counts
    }

    companion object {
        private var instance: LauncherNotificationListener? = null

        private val _notificationCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
        val notificationCounts: StateFlow<Map<String, Int>> = _notificationCounts.asStateFlow()

        fun isListenerConnected(): Boolean = instance != null
    }
}
