package com.raulshma.minkoa.weather

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val hourFmt = DateTimeFormatter.ofPattern("h a")
private val dayFmt = DateTimeFormatter.ofPattern("EEE")

@Composable
fun WeatherScreen(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val viewModel: WeatherViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(isActive) {
        if (isActive && !uiState.isLoaded && !uiState.isLoading) {
            viewModel.tryDeviceLocation()
            viewModel.loadWeather()
        }
    }

    val weatherCode = uiState.weatherData?.current?.weatherCode ?: 0
    val isDay = uiState.weatherData?.current?.isDay ?: true
    val gradientColors = weatherGradient(weatherCode, isDay)
    val onColor = weatherOnColor(weatherCode, isDay)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(gradientColors)
            )
            .statusBarsPadding()
            .safeDrawingPadding()
    ) {
        when {
            uiState.isLoading -> {
                LoadingState(onColor)
            }

            uiState.error != null -> {
                ErrorState(
                    error = uiState.error ?: "Unknown error",
                    onColor = onColor,
                    onRetry = { viewModel.refresh() }
                )
            }

            uiState.weatherData != null -> {
                WeatherContent(
                    uiState = uiState,
                    onColor = onColor,
                    onRefresh = { viewModel.refresh() },
                    onSearchCity = { viewModel.searchCity(it) },
                    onSelectCity = { viewModel.selectCity(it) },
                    onUseLocation = {
                        viewModel.tryDeviceLocation()
                    }
                )
            }
        }
    }
}

@Composable
private fun WeatherContent(
    uiState: WeatherUiState,
    onColor: Color,
    onRefresh: () -> Unit,
    onSearchCity: (String) -> Unit,
    onSelectCity: (GeocodingResult) -> Unit,
    onUseLocation: () -> Unit
) {
    val data = uiState.weatherData!!
    val current = data.current
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = onColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = uiState.locationName,
                    style = MaterialTheme.typography.titleMedium,
                    color = onColor,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
            Row {
                IconButton(onClick = { showSearch = !showSearch }) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search city",
                        tint = onColor
                    )
                }
                IconButton(onClick = onUseLocation) {
                    Icon(
                        imageVector = Icons.Rounded.MyLocation,
                        contentDescription = "Use my location",
                        tint = onColor
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Refresh",
                        tint = onColor
                    )
                }
            }
        }

        if (showSearch) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    onSearchCity(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                placeholder = { Text("Search city...") },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null)
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp)
            )

            if (uiState.citySearchResults.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    uiState.citySearchResults.forEach { result ->
                        Surface(
                            onClick = {
                                onSelectCity(result)
                                showSearch = false
                                searchQuery = ""
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocationOn,
                                    contentDescription = null,
                                    tint = onColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = result.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = onColor,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = buildString {
                                            val parts =
                                                listOfNotNull(result.admin1, result.country)
                                            append(parts.joinToString(", "))
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = onColor.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Icon(
            imageVector = weatherCodeToIcon(current.weatherCode, current.isDay),
            contentDescription = null,
            tint = onColor,
            modifier = Modifier.size(72.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${current.temperature.roundToInt()}\u00b0",
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = MaterialTheme.typography.displayLarge.fontSize * 2
            ),
            color = onColor,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = weatherCodeDescription(current.weatherCode),
            style = MaterialTheme.typography.headlineMedium,
            color = onColor,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Feels like ${current.apparentTemperature.roundToInt()}\u00b0",
                style = MaterialTheme.typography.bodyLarge,
                color = onColor.copy(alpha = 0.8f)
            )
            if (data.daily.isNotEmpty()) {
                Text(
                    text = "H: ${data.daily[0].tempMax.roundToInt()}\u00b0  L: ${data.daily[0].tempMin.roundToInt()}\u00b0",
                    style = MaterialTheme.typography.bodyLarge,
                    color = onColor.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White.copy(alpha = 0.12f)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Hourly Forecast",
                    style = MaterialTheme.typography.titleSmall,
                    color = onColor.copy(alpha = 0.7f),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))

                val now = LocalDateTime.now()
                val futureHourly =
                    data.hourly.filter { it.time.isAfter(now) }.take(24)

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(
                        items = futureHourly,
                        key = { it.time.toString() }
                    ) { hour ->
                        HourlyItem(
                            hour = hour,
                            onColor = onColor
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White.copy(alpha = 0.12f)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "7-Day Forecast",
                    style = MaterialTheme.typography.titleSmall,
                    color = onColor.copy(alpha = 0.7f),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                data.daily.forEachIndexed { index, day ->
                    DailyRow(
                        day = day,
                        isToday = index == 0,
                        onColor = onColor
                    )
                    if (index < data.daily.lastIndex) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            WeatherStat(
                label = "Wind",
                value = "${current.windSpeed.roundToInt()} km/h",
                icon = Icons.Rounded.Air,
                onColor = onColor
            )
            WeatherStat(
                label = "Humidity",
                value = "${current.humidity}%",
                icon = Icons.Rounded.WaterDrop,
                onColor = onColor
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun HourlyItem(
    hour: HourlyForecast,
    onColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(60.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = hour.time.format(hourFmt),
            style = MaterialTheme.typography.labelSmall,
            color = onColor.copy(alpha = 0.7f)
        )
        Icon(
            imageVector = weatherCodeToIcon(hour.weatherCode, true),
            contentDescription = null,
            tint = onColor,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = "${hour.temperature.roundToInt()}\u00b0",
            style = MaterialTheme.typography.bodyMedium,
            color = onColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DailyRow(
    day: DailyForecast,
    isToday: Boolean,
    onColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (isToday) "Today" else day.date.format(dayFmt),
            style = MaterialTheme.typography.bodyLarge,
            color = onColor,
            fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.width(56.dp)
        )
        Icon(
            imageVector = weatherCodeToIcon(day.weatherCode, true),
            contentDescription = null,
            tint = onColor,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = weatherCodeDescription(day.weatherCode),
            style = MaterialTheme.typography.bodyMedium,
            color = onColor.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        Text(
            text = "${day.tempMax.roundToInt()}\u00b0 / ${day.tempMin.roundToInt()}\u00b0",
            style = MaterialTheme.typography.bodyMedium,
            color = onColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun WeatherStat(
    label: String,
    value: String,
    icon: ImageVector,
    onColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = onColor,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = onColor,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = onColor.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun LoadingState(onColor: Color) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = onColor)
    }
}

@Composable
private fun ErrorState(
    error: String,
    onColor: Color,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.WbCloudy,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = onColor
            )
            Text(
                text = "Unable to load weather",
                style = MaterialTheme.typography.headlineMedium,
                color = onColor,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyLarge,
                color = onColor.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            FilledTonalButton(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Retry")
            }
        }
    }
}

fun weatherCodeToIcon(code: Int, isDay: Boolean): ImageVector = when {
    code == 0 && isDay -> Icons.Rounded.WbSunny
    code == 0 -> Icons.Rounded.WbCloudy
    code in 1..3 -> Icons.Rounded.Cloud
    code in 45..48 -> Icons.Rounded.Air
    code in 51..67 -> Icons.Rounded.WaterDrop
    code in 71..77 -> Icons.Rounded.AcUnit
    code in 80..82 -> Icons.Rounded.WaterDrop
    code in 85..86 -> Icons.Rounded.AcUnit
    code >= 95 -> Icons.Rounded.Thunderstorm
    else -> Icons.Rounded.Cloud
}

fun weatherGradient(code: Int, isDay: Boolean): List<Color> = when {
    code == 0 && isDay -> listOf(
        Color(0xFF42A5F5),
        Color(0xFF64B5F6),
        Color(0xFF90CAF9)
    )

    code == 0 -> listOf(
        Color(0xFF0D47A1),
        Color(0xFF1565C0),
        Color(0xFF1E88E5)
    )

    code in 1..3 && isDay -> listOf(
        Color(0xFF607D8B),
        Color(0xFF78909C),
        Color(0xFF90A4AE)
    )

    code in 1..3 -> listOf(
        Color(0xFF263238),
        Color(0xFF37474F),
        Color(0xFF455A64)
    )

    code in 45..48 -> listOf(
        Color(0xFF78909C),
        Color(0xFF90A4AE),
        Color(0xFFB0BEC5)
    )

    code in 51..67 -> listOf(
        Color(0xFF37474F),
        Color(0xFF455A64),
        Color(0xFF607D8B)
    )

    code in 71..77 -> listOf(
        Color(0xFF78909C),
        Color(0xFF90A4AE),
        Color(0xFFB0BEC5)
    )

    code in 80..86 -> listOf(
        Color(0xFF455A64),
        Color(0xFF546E7A),
        Color(0xFF78909C)
    )

    code >= 95 -> listOf(
        Color(0xFF1A237E),
        Color(0xFF311B92),
        Color(0xFF4A148C)
    )

    else -> listOf(
        Color(0xFF607D8B),
        Color(0xFF78909C),
        Color(0xFF90A4AE)
    )
}

fun weatherOnColor(code: Int, isDay: Boolean): Color = Color.White
