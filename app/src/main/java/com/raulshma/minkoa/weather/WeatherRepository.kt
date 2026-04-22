package com.raulshma.minkoa.weather

import android.content.Context
import android.location.LocationManager
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class CurrentWeather(
    val temperature: Double,
    val apparentTemperature: Double,
    val humidity: Int,
    val windSpeed: Double,
    val weatherCode: Int,
    val isDay: Boolean
)

data class HourlyForecast(
    val time: LocalDateTime,
    val temperature: Double,
    val weatherCode: Int
)

data class DailyForecast(
    val date: LocalDate,
    val weatherCode: Int,
    val tempMax: Double,
    val tempMin: Double
)

data class WeatherData(
    val current: CurrentWeather,
    val hourly: List<HourlyForecast>,
    val daily: List<DailyForecast>,
    val timezone: String
)

data class GeocodingResult(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String,
    val admin1: String? = null
)

class WeatherRepository(private val context: Context) {

    fun fetchWeather(lat: Double, lon: Double): WeatherData {
        val url = buildString {
            append("https://api.open-meteo.com/v1/forecast?")
            append("latitude=$lat&longitude=$lon")
            append("&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m,is_day")
            append("&hourly=temperature_2m,weather_code")
            append("&daily=weather_code,temperature_2m_max,temperature_2m_min")
            append("&timezone=auto&forecast_days=7")
        }

        val connection = java.net.URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000

            val response = connection.inputStream.bufferedReader().readText()
            return parseWeatherResponse(response)
        } finally {
            connection.disconnect()
        }
    }

    fun searchCity(query: String): List<GeocodingResult> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url =
            "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=5&language=en&format=json"

        val connection = java.net.URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000

            val response = connection.inputStream.bufferedReader().readText()
            return parseGeocodingResponse(response)
        } finally {
            connection.disconnect()
        }
    }

    fun getLastKnownLocation(): Pair<Double, Double>? {
        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = locationManager.getProviders(true)
        for (provider in providers) {
            try {
                val location = locationManager.getLastKnownLocation(provider) ?: continue
                return Pair(location.latitude, location.longitude)
            } catch (_: SecurityException) {
                continue
            }
        }
        return null
    }

    private val timeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

    private fun parseWeatherResponse(json: String): WeatherData {
        val root = JSONObject(json)

        val cur = root.getJSONObject("current")
        val current = CurrentWeather(
            temperature = cur.getDouble("temperature_2m"),
            apparentTemperature = cur.getDouble("apparent_temperature"),
            humidity = cur.getInt("relative_humidity_2m"),
            windSpeed = cur.getDouble("wind_speed_10m"),
            weatherCode = cur.getInt("weather_code"),
            isDay = cur.getInt("is_day") == 1
        )

        val hourlyObj = root.getJSONObject("hourly")
        val hTimes = hourlyObj.getJSONArray("time")
        val hTemps = hourlyObj.getJSONArray("temperature_2m")
        val hCodes = hourlyObj.getJSONArray("weather_code")
        val hourly = (0 until hTimes.length()).map { i ->
            HourlyForecast(
                time = LocalDateTime.parse(hTimes.getString(i), timeFmt),
                temperature = hTemps.getDouble(i),
                weatherCode = hCodes.getInt(i)
            )
        }

        val dailyObj = root.getJSONObject("daily")
        val dDates = dailyObj.getJSONArray("time")
        val dCodes = dailyObj.getJSONArray("weather_code")
        val dMax = dailyObj.getJSONArray("temperature_2m_max")
        val dMin = dailyObj.getJSONArray("temperature_2m_min")
        val daily = (0 until dDates.length()).map { i ->
            DailyForecast(
                date = LocalDate.parse(dDates.getString(i)),
                weatherCode = dCodes.getInt(i),
                tempMax = dMax.getDouble(i),
                tempMin = dMin.getDouble(i)
            )
        }

        return WeatherData(
            current = current,
            hourly = hourly,
            daily = daily,
            timezone = root.optString("timezone", "auto")
        )
    }

    private fun parseGeocodingResponse(json: String): List<GeocodingResult> {
        val root = JSONObject(json)
        val results = root.optJSONArray("results") ?: return emptyList()
        return (0 until results.length()).map { i ->
            val item = results.getJSONObject(i)
            GeocodingResult(
                name = item.getString("name"),
                latitude = item.getDouble("latitude"),
                longitude = item.getDouble("longitude"),
                country = item.optString("country", ""),
                admin1 = if (item.has("admin1")) item.getString("admin1") else null
            )
        }
    }
}

fun weatherCodeDescription(code: Int): String = when (code) {
    0 -> "Clear sky"
    1 -> "Mainly clear"
    2 -> "Partly cloudy"
    3 -> "Overcast"
    45 -> "Fog"
    48 -> "Rime fog"
    51 -> "Light drizzle"
    53 -> "Drizzle"
    55 -> "Dense drizzle"
    56 -> "Freezing drizzle"
    57 -> "Heavy freezing drizzle"
    61 -> "Slight rain"
    63 -> "Rain"
    65 -> "Heavy rain"
    66 -> "Freezing rain"
    67 -> "Heavy freezing rain"
    71 -> "Slight snow"
    73 -> "Snow"
    75 -> "Heavy snow"
    77 -> "Snow grains"
    80 -> "Rain showers"
    81 -> "Moderate showers"
    82 -> "Heavy showers"
    85 -> "Snow showers"
    86 -> "Heavy snow showers"
    95 -> "Thunderstorm"
    96 -> "Thunderstorm with hail"
    99 -> "Severe thunderstorm"
    else -> "Unknown"
}
