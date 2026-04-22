package com.raulshma.minkoa.weather

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WeatherUiState(
    val weatherData: WeatherData? = null,
    val locationName: String = "New York",
    val latitude: Double = 40.71427,
    val longitude: Double = -74.00597,
    val isLocationFromDevice: Boolean = false,
    val isLoading: Boolean = false,
    val isLoaded: Boolean = false,
    val error: String? = null,
    val citySearchResults: List<GeocodingResult> = emptyList(),
    val isSearching: Boolean = false
)

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WeatherRepository(application)

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    fun loadWeather(lat: Double? = null, lon: Double? = null) {
        if (_uiState.value.isLoaded && lat == null) return
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val latitude = lat ?: _uiState.value.latitude
                val longitude = lon ?: _uiState.value.longitude
                val data = repository.fetchWeather(latitude, longitude)
                _uiState.update {
                    it.copy(
                        weatherData = data,
                        latitude = latitude,
                        longitude = longitude,
                        isLoading = false,
                        isLoaded = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Unknown error")
                }
            }
        }
    }

    fun tryDeviceLocation() {
        viewModelScope.launch(Dispatchers.IO) {
            val location = repository.getLastKnownLocation()
            if (location != null) {
                _uiState.update {
                    it.copy(
                        latitude = location.first,
                        longitude = location.second,
                        isLocationFromDevice = true,
                        locationName = "My Location",
                        isLoaded = false
                    )
                }
                loadWeather()
            }
        }
    }

    fun searchCity(query: String) {
        if (query.isBlank()) {
            _uiState.update { it.copy(citySearchResults = emptyList()) }
            return
        }
        _uiState.update { it.copy(isSearching = true) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val results = repository.searchCity(query)
                _uiState.update { it.copy(citySearchResults = results, isSearching = false) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isSearching = false) }
            }
        }
    }

    fun selectCity(result: GeocodingResult) {
        val label = buildString {
            append(result.name)
            if (result.admin1 != null) append(", ${result.admin1}")
            append(", ${result.country}")
        }
        _uiState.update {
            it.copy(
                latitude = result.latitude,
                longitude = result.longitude,
                locationName = label,
                citySearchResults = emptyList(),
                isLoaded = false
            )
        }
        loadWeather()
    }

    fun refresh() {
        _uiState.update { it.copy(isLoaded = false) }
        loadWeather()
    }
}
