package com.raulshma.minkoa.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class LayoutRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("quack_launcher_layout", Context.MODE_PRIVATE)

    fun saveLayout(
        workspaceSlots: List<SlotContent?>,
        dockSlots: List<SlotContent?>,
        leftScreenIds: List<String>,
        rightScreenIds: List<String>
    ) {
        val json = JSONObject().apply {
            put("v", 1)
            put("workspace", serializeSlots(workspaceSlots))
            put("dock", serializeSlots(dockSlots))
            put("leftScreens", JSONArray(leftScreenIds))
            put("rightScreens", JSONArray(rightScreenIds))
        }
        prefs.edit().putString(KEY_LAYOUT, json.toString()).apply()
    }

    fun loadLayout(): SavedLayout? {
        val raw = prefs.getString(KEY_LAYOUT, null) ?: return null
        return try {
            val json = JSONObject(raw)
            SavedLayout(
                workspaceSlots = deserializeSlots(
                    json.getJSONArray("workspace"),
                    WORKSPACE_SLOTS
                ),
                dockSlots = deserializeSlots(json.getJSONArray("dock"), DOCK_SLOTS),
                leftScreenIds = deserializeStrings(json.optJSONArray("leftScreens")),
                rightScreenIds = deserializeStrings(json.optJSONArray("rightScreens"))
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun serializeSlots(slots: List<SlotContent?>): JSONArray =
        JSONArray().apply {
            for (slot in slots) put(slot?.serialize())
        }

    private fun deserializeSlots(array: JSONArray, expectedSize: Int): List<SlotContent?> {
        val list = mutableListOf<SlotContent?>()
        for (i in 0 until array.length()) {
            val raw = array.optString(i, null)
            list.add(raw?.let { SlotContent.deserialize(it) })
        }
        while (list.size < expectedSize) list.add(null)
        return list.take(expectedSize)
    }

    private fun deserializeStrings(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { array.optString(it, null) }
    }

    companion object {
        private const val KEY_LAYOUT = "layout_json"
        private const val WORKSPACE_SLOTS = 20
        private const val DOCK_SLOTS = 5
    }
}

data class SavedLayout(
    val workspaceSlots: List<SlotContent?>,
    val dockSlots: List<SlotContent?>,
    val leftScreenIds: List<String>,
    val rightScreenIds: List<String>
)
