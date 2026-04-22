package com.raulshma.minkoa.files

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FilesUiState(
    val recentFiles: List<FileItem> = emptyList(),
    val categories: List<CategorySummary> = emptyList(),
    val selectedCategory: FileCategory? = null,
    val isLoading: Boolean = false,
    val isLoaded: Boolean = false
)

class FilesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FilesRepository(application)

    private val _uiState = MutableStateFlow(FilesUiState())
    val uiState: StateFlow<FilesUiState> = _uiState.asStateFlow()

    fun loadFiles() {
        if (_uiState.value.isLoaded) return
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch(Dispatchers.IO) {
            val files = repository.queryRecentFiles()
            val categories = repository.computeCategorySummaries(files)
            _uiState.update {
                it.copy(
                    recentFiles = files,
                    categories = categories,
                    isLoading = false,
                    isLoaded = true
                )
            }
        }
    }

    fun selectCategory(category: FileCategory?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }
}
