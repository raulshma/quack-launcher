package com.raulshma.minkoa.widget

import android.appwidget.AppWidgetProviderInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetPickerSheet(
    widgets: List<AppWidgetProviderInfo>,
    onSelectWidget: (AppWidgetProviderInfo) -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val packageManager = context.packageManager

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
                    items(
                        items = widgets,
                        key = { info ->
                            "${info.provider.packageName}/${info.provider.className}"
                        }
                    ) { widgetInfo ->
                        val label = try {
                            widgetInfo.loadLabel(packageManager)
                        } catch (_: Exception) {
                            widgetInfo.provider.shortClassName
                        }

                        Surface(
                            onClick = { onSelectWidget(widgetInfo) },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(
                                20.dp
                            ),
                            tonalElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(44.dp),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(
                                        14.dp
                                    ),
                                    color = MaterialTheme.colorScheme.tertiaryContainer
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = label.firstOrNull()
                                                ?.uppercaseChar()?.toString() ?: "?",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val sizeText = buildString {
                                        append(widgetInfo.minWidth)
                                        append(" \u00d7 ")
                                        append(widgetInfo.minHeight)
                                        append(" dp")
                                    }
                                    Text(
                                        text = sizeText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
