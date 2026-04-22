package com.raulshma.minkoa.data

import android.content.ComponentName

sealed interface SlotContent {

    data class App(val key: String) : SlotContent

    data class Widget(
        val appWidgetId: Int,
        val providerPkg: String,
        val providerCls: String
    ) : SlotContent

    val providerComponent: ComponentName?
        get() = when (this) {
            is App -> null
            is Widget -> ComponentName(providerPkg, providerCls)
        }

    fun serialize(): String = when (this) {
        is App -> "app:$key"
        is Widget -> "widget:$appWidgetId:$providerPkg:$providerCls"
    }

    companion object {
        fun deserialize(value: String): SlotContent? {
            return when {
                value.startsWith("app:") -> App(value.removePrefix("app:"))
                value.startsWith("widget:") -> {
                    val parts = value.removePrefix("widget:").split(":", limit = 3)
                    if (parts.size == 3) {
                        Widget(
                            appWidgetId = parts[0].toIntOrNull() ?: return null,
                            providerPkg = parts[1],
                            providerCls = parts[2]
                        )
                    } else null
                }
                else -> null
            }
        }
    }
}
