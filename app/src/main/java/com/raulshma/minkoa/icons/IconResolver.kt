package com.raulshma.minkoa.icons

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

@Immutable
data class ResolvedIcon(
    val key: String,
    val imageBitmap: ImageBitmap
)

data class IconPackInfo(
    val packageName: String,
    val label: String
)

class IconResolver(private val context: Context) {

    private val packageManager = context.packageManager
    private val cache = mutableMapOf<String, ResolvedIcon>()
    var activeIconPack: String? = null

    fun resolve(appKey: String): ResolvedIcon? {
        cache[appKey]?.let { return it }

        val parts = appKey.split("/")
        if (parts.size != 2) return null
        val packageName = parts[0]
        val activityName = parts[1]

        return try {
            val componentName = ComponentName(packageName, activityName)
            val icon = loadIconForComponent(componentName, packageName)
                ?: return null

            val bitmap = normalizeIcon(icon)
            val resolved = ResolvedIcon(
                key = appKey,
                imageBitmap = bitmap.asImageBitmap()
            )
            cache[appKey] = resolved
            resolved
        } catch (_: Exception) {
            null
        }
    }

    private fun loadIconForComponent(componentName: ComponentName, packageName: String): Drawable? {
        val iconPackPkg = activeIconPack
        if (iconPackPkg != null) {
            val packIcon = loadIconPackDrawable(iconPackPkg, componentName.flattenToString())
                ?: loadIconPackDrawable(iconPackPkg, packageName)
            if (packIcon != null) return packIcon
        }
        val activityInfo = packageManager.getActivityInfo(componentName, 0)
        return activityInfo.loadIcon(packageManager)
    }

    private fun loadIconPackDrawable(iconPackPkg: String, componentNameStr: String): Drawable? {
        return try {
            val resources = packageManager.getResourcesForApplication(iconPackPkg)
            val appFilterId = resources.getIdentifier("appfilter", "xml", iconPackPkg)
            if (appFilterId == 0) return null

            val xml = resources.getXml(appFilterId)
            while (xml.next() != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                if (xml.name == "item") {
                    val comp = xml.getAttributeValue(null, "component")
                    if (comp != null && comp.contains(componentNameStr)) {
                        val drawableName = xml.getAttributeValue(null, "drawable")
                        if (drawableName != null) {
                            val drawableId = resources.getIdentifier(drawableName, "drawable", iconPackPkg)
                            if (drawableId != 0) {
                                return packageManager.getDrawable(iconPackPkg, drawableId, null)
                            }
                        }
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    fun queryInstalledIconPacks(): List<IconPackInfo> {
        val intent = Intent("org.adw.launcher.THEMES")
        return packageManager.queryIntentActivities(intent, 0)
            .mapNotNull { resolveInfo ->
                val pkg = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
                val label = try {
                    resolveInfo.loadLabel(packageManager)?.toString() ?: pkg
                } catch (_: Exception) { pkg }
                IconPackInfo(pkg, label)
            }
            .sortedBy { it.label.lowercase() }
    }

    fun setIconPack(packageName: String?) {
        activeIconPack = packageName
        cache.clear()
    }

    fun preloadIcons(appKeys: List<String>) {
        appKeys.forEach { key ->
            if (!cache.containsKey(key)) {
                resolve(key)
            }
        }
    }

    fun clearCache() {
        cache.clear()
    }

    private fun normalizeIcon(drawable: Drawable): Bitmap {
        val size = 108
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        when (drawable) {
            is AdaptiveIconDrawable -> {
                val background = drawable.background
                val foreground = drawable.foreground
                if (background != null) {
                    background.setBounds(0, 0, size, size)
                    background.draw(canvas)
                }
                if (foreground != null) {
                    foreground.setBounds(0, 0, size, size)
                    foreground.draw(canvas)
                }
            }

            is BitmapDrawable -> {
                val src = drawable.bitmap
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                canvas.drawBitmap(src, null, android.graphics.Rect(0, 0, size, size), paint)
            }

            else -> {
                drawable.setBounds(0, 0, size, size)
                drawable.draw(canvas)
            }
        }

        return bitmap
    }
}
