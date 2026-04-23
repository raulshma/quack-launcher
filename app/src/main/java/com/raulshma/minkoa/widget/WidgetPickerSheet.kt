package com.raulshma.minkoa.widget

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class WidgetAppGroup(
    val packageName: String,
    val appLabel: String,
    val appIcon: Drawable?,
    val widgets: List<AppWidgetProviderInfo>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetPickerSheet(
    widgets: List<AppWidgetProviderInfo>,
    workspaceColumns: Int,
    workspaceRows: Int,
    onSelectWidget: (AppWidgetProviderInfo) -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val packageManager = context.packageManager

    val grouped = remember(widgets) {
        widgets
            .groupBy { it.provider.packageName }
            .map { (pkg, widgetList) ->
                val appLabel = try {
                    packageManager.getApplicationLabel(
                        packageManager.getApplicationInfo(pkg, PackageManager.GET_META_DATA)
                    ).toString()
                } catch (_: Exception) {
                    pkg
                }
                val appIcon = runCatching { packageManager.getApplicationIcon(pkg) }.getOrNull()
                WidgetAppGroup(pkg, appLabel, appIcon, widgetList)
            }
            .sortedBy { it.appLabel.lowercase() }
    }

    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Widgets",
                style = MaterialTheme.typography.displayMedium
            )
            Text(
                text = "Select a widget to place on your home screen.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (widgets.isEmpty()) {
                Text(
                    text = "No widgets available on this device.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    grouped.forEach { group ->
                        item(key = "header_${group.packageName}") {
                            AppGroupHeader(
                                appLabel = group.appLabel,
                                appIcon = group.appIcon,
                                widgetCount = group.widgets.size
                            )
                        }

                        items(
                            items = group.widgets,
                            key = { info ->
                                "${info.provider.packageName}/${info.provider.className}"
                            }
                        ) { widgetInfo ->
                            WidgetPickerItem(
                                widgetInfo = widgetInfo,
                                workspaceColumns = workspaceColumns,
                                workspaceRows = workspaceRows,
                                packageManager = packageManager,
                                onClick = { onSelectWidget(widgetInfo) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppGroupHeader(
    appLabel: String,
    appIcon: Drawable?,
    widgetCount: Int
) {
    val iconBitmap = remember(appIcon) {
        appIcon?.let { drawable ->
            runCatching {
                drawable.toBitmap(width = 96, height = 96).asImageBitmap()
            }.getOrNull()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = appLabel.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Text(
            text = appLabel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = widgetCount.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun WidgetPickerItem(
    widgetInfo: AppWidgetProviderInfo,
    workspaceColumns: Int,
    workspaceRows: Int,
    packageManager: PackageManager,
    onClick: () -> Unit
) {
    val label = remember(widgetInfo.provider) {
        try {
            widgetInfo.loadLabel(packageManager)?.toString()
        } catch (_: Exception) {
            null
        } ?: widgetInfo.provider.shortClassName
    }

    val spanX = remember(widgetInfo.provider, workspaceColumns) {
        (widgetInfo.minWidth.coerceAtLeast(70) / 70)
            .coerceAtLeast(1)
            .coerceAtMost(workspaceColumns)
    }
    val spanY = remember(widgetInfo.provider, workspaceRows) {
        (widgetInfo.minHeight.coerceAtLeast(70) / 70)
            .coerceAtLeast(1)
            .coerceAtMost(workspaceRows)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp, max = 200.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                WidgetPreview(
                    widgetInfo = widgetInfo,
                    spanX = spanX,
                    spanY = spanY
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                val sizeText = buildString {
                    append(spanX)
                    append(" \u00d7 ")
                    append(spanY)
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = sizeText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetPreview(
    widgetInfo: AppWidgetProviderInfo,
    spanX: Int,
    spanY: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val previewBitmap by produceState<ImageBitmap?>(null, widgetInfo.provider) {
        value = generateWidgetPreviewBitmap(context, widgetInfo, spanX, spanY)
    }

    if (previewBitmap != null) {
        Image(
            bitmap = previewBitmap!!,
            contentDescription = null,
            modifier = modifier.fillMaxWidth(),
            contentScale = ContentScale.Fit
        )
    } else {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private suspend fun generateWidgetPreviewBitmap(
    context: android.content.Context,
    widgetInfo: AppWidgetProviderInfo,
    spanX: Int,
    spanY: Int
): ImageBitmap? = withContext(Dispatchers.IO) {
    val density = context.resources.displayMetrics.densityDpi

    // 1. Try static preview image
    val staticPreview = runCatching {
        widgetInfo.loadPreviewImage(context, density)
    }.getOrNull()

    if (staticPreview != null) {
        val intrinsicW = staticPreview.intrinsicWidth.takeIf { it > 0 } ?: 200
        val intrinsicH = staticPreview.intrinsicHeight.takeIf { it > 0 } ?: 200
        val maxDim = 600
        val scale = if (maxOf(intrinsicW, intrinsicH) > maxDim) {
            maxDim.toFloat() / maxOf(intrinsicW, intrinsicH)
        } else 1f
        val w = (intrinsicW * scale).toInt().coerceAtLeast(1)
        val h = (intrinsicH * scale).toInt().coerceAtLeast(1)
        return@withContext runCatching {
            staticPreview.toBitmap(w, h).asImageBitmap()
        }.getOrNull()
    }

    // 2. Try previewLayout (Android 12+) – clone info and swap initialLayout
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val previewLayout = widgetInfo.previewLayout
        if (previewLayout != 0) {
            val cloned = widgetInfo.clone() as AppWidgetProviderInfo
            cloned.initialLayout = previewLayout
            val rendered = renderPreviewHostView(context, cloned, null)
            if (rendered != null) {
                return@withContext rendered
            }
        }
    }

    // 3. Try generated preview via hidden API (Android 12+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val generated = loadGeneratedPreview(context, widgetInfo)
        if (generated != null) {
            return@withContext generated
        }
    }

    // 4. Fallback: draw a preview placeholder like Launcher3 does
    generatePlaceholderPreview(context, widgetInfo, spanX, spanY)
}

private suspend fun loadGeneratedPreview(
    context: android.content.Context,
    widgetInfo: AppWidgetProviderInfo
): ImageBitmap? = withContext(Dispatchers.Main.immediate) {
    try {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        // Use reflection to access hidden getAppWidgetPreview API
        val remoteViews = try {
            val method = AppWidgetManager::class.java.getDeclaredMethod(
                "getAppWidgetPreview",
                android.content.ComponentName::class.java,
                Int::class.javaPrimitiveType
            )
            method.invoke(appWidgetManager, widgetInfo.provider, 0) as? android.widget.RemoteViews
        } catch (_: Exception) {
            // Try alternative method signature
            try {
                val method = AppWidgetManager::class.java.getDeclaredMethod(
                    "getAppWidgetPreview",
                    android.content.ComponentName::class.java
                )
                method.invoke(appWidgetManager, widgetInfo.provider) as? android.widget.RemoteViews
            } catch (_: Exception) {
                null
            }
        }

        if (remoteViews == null) {
            return@withContext null
        }

        renderPreviewHostView(context, widgetInfo, remoteViews)
    } catch (_: Exception) {
        null
    }
}

private suspend fun renderPreviewHostView(
    context: android.content.Context,
    providerInfo: AppWidgetProviderInfo,
    remoteViews: android.widget.RemoteViews?
): ImageBitmap? = withContext(Dispatchers.Main.immediate) {
    try {
        val providerContext = runCatching {
            context.createPackageContext(
                providerInfo.provider.packageName,
                android.content.Context.CONTEXT_IGNORE_SECURITY
            )
        }.getOrDefault(context)
        val hostView = AppWidgetHostView(providerContext)
        hostView.setAppWidget(-1, providerInfo)

        val width = providerInfo.minWidth.takeIf { it > 0 } ?: 200
        val height = providerInfo.minHeight.takeIf { it > 0 } ?: 200
        val maxDim = 600
        val scale = if (maxOf(width, height) > maxDim) {
            maxDim.toFloat() / maxOf(width, height)
        } else 1f
        val scaledW = (width * scale).toInt().coerceAtLeast(1)
        val scaledH = (height * scale).toInt().coerceAtLeast(1)

        // Add to a temporary FrameLayout for measuring
        val container = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(scaledW, scaledH)
        }

        if (remoteViews != null) {
            hostView.updateAppWidget(remoteViews)
        }
        container.addView(hostView, FrameLayout.LayoutParams(scaledW, scaledH))

        // Measure and layout
        container.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(scaledW, android.view.View.MeasureSpec.EXACTLY),
            android.view.View.MeasureSpec.makeMeasureSpec(scaledH, android.view.View.MeasureSpec.EXACTLY)
        )
        container.layout(0, 0, scaledW, scaledH)

        // Give the view a chance to apply async updates
        hostView.invalidate()

        // Draw to bitmap
        val bitmap = Bitmap.createBitmap(scaledW, scaledH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        container.draw(canvas)

        bitmap.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}

private fun generatePlaceholderPreview(
    context: android.content.Context,
    widgetInfo: AppWidgetProviderInfo,
    spanX: Int,
    spanY: Int
): ImageBitmap? {
    val density = context.resources.displayMetrics.density
    val cellSizeDp = 70
    val previewWidth = (spanX * cellSizeDp * density).toInt().coerceAtLeast(1)
    val previewHeight = (spanY * cellSizeDp * density).toInt().coerceAtLeast(1)

    // Scale down if too large
    val maxDim = 600
    val scale = if (maxOf(previewWidth, previewHeight) > maxDim) {
        maxDim.toFloat() / maxOf(previewWidth, previewHeight)
    } else 1f
    val w = (previewWidth * scale).toInt().coerceAtLeast(1)
    val h = (previewHeight * scale).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Draw background
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    val cornerRadius = 16f * scale
    canvas.drawRoundRect(0f, 0f, w.toFloat(), h.toFloat(), cornerRadius, cornerRadius, bgPaint)

    // Draw grid lines
    val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.LTGRAY
        strokeWidth = 1f * scale
        style = Paint.Style.STROKE
    }

    // Vertical lines
    val cellW = w.toFloat() / spanX
    for (i in 1 until spanX) {
        val x = i * cellW
        canvas.drawLine(x, 0f, x, h.toFloat(), gridPaint)
    }

    // Horizontal lines
    val cellH = h.toFloat() / spanY
    for (i in 1 until spanY) {
        val y = i * cellH
        canvas.drawLine(0f, y, w.toFloat(), y, gridPaint)
    }

    // Draw app icon in center
    try {
        val appIcon = context.packageManager.getApplicationIcon(widgetInfo.provider.packageName)
        val iconSize = (48 * density * scale).toInt().coerceAtLeast(24)
        val iconBitmap = appIcon.toBitmap(iconSize, iconSize)
        val left = (w - iconSize) / 2f
        val top = (h - iconSize) / 2f
        canvas.drawBitmap(iconBitmap, left, top, null)
    } catch (_: Exception) {
        // If no icon, draw a letter
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.DKGRAY
            textSize = 32f * scale
            textAlign = Paint.Align.CENTER
        }
        val label = widgetInfo.provider.shortClassName.firstOrNull()?.toString() ?: "?"
        canvas.drawText(label, w / 2f, h / 2f + textPaint.textSize / 3f, textPaint)
    }

    return bitmap.asImageBitmap()
}
