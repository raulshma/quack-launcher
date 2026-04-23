package com.raulshma.minkoa.gallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GalleryUiState(
    val images: List<GalleryImage> = emptyList(),
    val albums: List<GalleryAlbum> = emptyList(),
    val selectedAlbum: String? = null,
    val isLoading: Boolean = false,
    val isLoaded: Boolean = false
)

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GalleryRepository(application)

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    private var observationActive = false

    fun loadImages() {
        if (observationActive) return
        observationActive = true
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            repository.observeImages().collect { images ->
                val albums = repository.computeAlbums(images)
                _uiState.update {
                    it.copy(
                        images = images,
                        albums = albums,
                        isLoading = false,
                        isLoaded = true
                    )
                }
            }
        }
    }

    fun selectAlbum(bucketId: String?) {
        _uiState.update { it.copy(selectedAlbum = bucketId) }
    }
}
