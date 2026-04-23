package com.raulshma.minkoa.preferences

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

class AppPreferencesRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("quack_launcher_app_prefs", Context.MODE_PRIVATE)

    fun getHiddenApps(): Set<String> {
        return prefs.getStringSet(KEY_HIDDEN_APPS, emptySet()) ?: emptySet()
    }

    fun setHiddenApps(keys: Set<String>) {
        prefs.edit().putStringSet(KEY_HIDDEN_APPS, keys).apply()
    }

    fun hideApp(key: String) {
        val current = getHiddenApps().toMutableSet()
        current.add(key)
        setHiddenApps(current)
    }

    fun showApp(key: String) {
        val current = getHiddenApps().toMutableSet()
        current.remove(key)
        setHiddenApps(current)
    }

    fun isHidden(key: String): Boolean = getHiddenApps().contains(key)

    fun getCustomLabels(): Map<String, String> {
        val raw = prefs.getString(KEY_CUSTOM_LABELS, null) ?: return emptyMap()
        return try {
            val json = JSONObject(raw)
            val map = mutableMapOf<String, String>()
            json.keys().forEach { key -> map[key] = json.getString(key) }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun setCustomLabel(appKey: String, label: String) {
        val current = getCustomLabels().toMutableMap()
        current[appKey] = label
        saveCustomLabels(current)
    }

    fun removeCustomLabel(appKey: String) {
        val current = getCustomLabels().toMutableMap()
        current.remove(appKey)
        saveCustomLabels(current)
    }

    fun getCustomLabel(appKey: String): String? = getCustomLabels()[appKey]

    private fun saveCustomLabels(labels: Map<String, String>) {
        val json = JSONObject()
        labels.forEach { (k, v) -> json.put(k, v) }
        prefs.edit().putString(KEY_CUSTOM_LABELS, json.toString()).apply()
    }

    fun getIconBackground(): IconBackground {
        return try {
            IconBackground.valueOf(prefs.getString(KEY_ICON_BACKGROUND, IconBackground.Default.name)!!)
        } catch (_: Exception) {
            IconBackground.Default
        }
    }

    fun setIconBackground(background: IconBackground) {
        prefs.edit().putString(KEY_ICON_BACKGROUND, background.name).apply()
    }

    fun getIconShape(): IconShape {
        return try {
            IconShape.valueOf(prefs.getString(KEY_ICON_SHAPE, IconShape.Rounded.name)!!)
        } catch (_: Exception) {
            IconShape.Rounded
        }
    }

    fun setIconShape(shape: IconShape) {
        prefs.edit().putString(KEY_ICON_SHAPE, shape.name).apply()
    }

    fun getWorkspaceColumns(): Int {
        return prefs.getInt(KEY_WORKSPACE_COLUMNS, 4).coerceIn(3, 6)
    }

    fun setWorkspaceColumns(value: Int) {
        prefs.edit().putInt(KEY_WORKSPACE_COLUMNS, value.coerceIn(3, 6)).apply()
    }

    fun getWorkspaceRows(): Int {
        return prefs.getInt(KEY_WORKSPACE_ROWS, 5).coerceIn(3, 7)
    }

    fun setWorkspaceRows(value: Int) {
        prefs.edit().putInt(KEY_WORKSPACE_ROWS, value.coerceIn(3, 7)).apply()
    }

    fun getDockSlots(): Int {
        return prefs.getInt(KEY_DOCK_SLOTS, 5).coerceIn(3, 7)
    }

    fun setDockSlots(value: Int) {
        prefs.edit().putInt(KEY_DOCK_SLOTS, value.coerceIn(3, 7)).apply()
    }

    companion object {
        private const val KEY_HIDDEN_APPS = "hidden_apps"
        private const val KEY_CUSTOM_LABELS = "custom_labels"
        private const val KEY_ICON_BACKGROUND = "icon_background"
        private const val KEY_ICON_SHAPE = "icon_shape"
        private const val KEY_WORKSPACE_COLUMNS = "workspace_columns"
        private const val KEY_WORKSPACE_ROWS = "workspace_rows"
        private const val KEY_DOCK_SLOTS = "dock_slots"
    }
}
