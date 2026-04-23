package com.raulshma.minkoa.widget

import android.appwidget.AppWidgetProviderInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
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
                        packageManager.getApplicationInfo(pkg, PackageManager.GET_META_DATA
                        )
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
                    .heightIn(min = 64.dp, max = 160.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                WidgetPreview(widgetInfo = widgetInfo)
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val preview by produceState<ImageBitmap?>(null, widgetInfo.provider) {
        value = withContext(Dispatchers.IO) {
            val density = context.resources.displayMetrics.densityDpi
            val drawable = runCatching {
                widgetInfo.loadPreviewImage(context, density)
            }.getOrNull() ?: runCatching {
                widgetInfo.loadIcon(context, density)
            }.getOrNull()

            drawable?.let {
                val intrinsicW = it.intrinsicWidth.takeIf { w -> w > 0 } ?: 200
                val intrinsicH = it.intrinsicHeight.takeIf { h -> h > 0 } ?: 200
                val maxDim = 512
                val scale = if (maxOf(intrinsicW, intrinsicH) > maxDim) {
                    maxDim.toFloat() / maxOf(intrinsicW, intrinsicH)
                } else 1f
                val w = (intrinsicW * scale).toInt().coerceAtLeast(1)
                val h = (intrinsicH * scale).toInt().coerceAtLeast(1)
                runCatching { it.toBitmap(w, h).asImageBitmap() }.getOrNull()
            }
        }
    }

    if (preview != null) {
        Image(
            bitmap = preview!!,
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
