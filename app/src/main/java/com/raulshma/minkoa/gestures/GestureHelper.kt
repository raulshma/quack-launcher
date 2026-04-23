package com.raulshma.minkoa.gestures

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

object GestureHelper {

    fun expandNotificationShade(context: Context) {
        try {
            val statusBarManager = context.getSystemService(Context.STATUS_BAR_SERVICE)
            val method = statusBarManager?.javaClass?.getMethod("expandNotificationsPanel")
            method?.invoke(statusBarManager)
        } catch (_: Exception) {
            try {
                @Suppress("DEPRECATION")
                val intent = Intent("android.intent.action.ACTION_NOTIFICATION_PANEL_OPEN")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (_: Exception) {
            }
        }
    }

    fun isDeviceAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isAdminActive(getAdminComponent(context))
    }

    fun requestDeviceAdmin(context: Context) {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, getAdminComponent(context))
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Allow Quack Launcher to lock the screen on double-tap.")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun lockScreen(context: Context): Boolean {
        if (!isDeviceAdminActive(context)) return false
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        runCatching { dpm.lockNow() }
        return true
    }

    private fun getAdminComponent(context: Context): ComponentName {
        return ComponentName(context, LauncherDeviceAdminReceiver::class.java)
    }
}
