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
    val isLoaded: Boolean = false,
    val directoryStack: List<String> = emptyList(),
    val directoryContents: List<DirectoryEntry> = emptyList(),
    val isBrowsingDirectory: Boolean = false
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

    fun openDirectory(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val entries = repository.listDirectory(path)
            _uiState.update {
                it.copy(
                    directoryStack = it.directoryStack + path,
                    directoryContents = entries,
                    isBrowsingDirectory = true
                )
            }
        }
    }

    fun navigateUp(): Boolean {
        val stack = _uiState.value.directoryStack
        if (stack.size <= 1) {
            _uiState.update { it.copy(directoryStack = emptyList(), directoryContents = emptyList(), isBrowsingDirectory = false) }
            return false
        }
        val parentPath = stack.dropLast(1).last()
        viewModelScope.launch(Dispatchers.IO) {
            val entries = repository.listDirectory(parentPath)
            _uiState.update {
                it.copy(
                    directoryStack = stack.dropLast(1),
                    directoryContents = entries,
                    isBrowsingDirectory = true
                )
            }
        }
        return true
    }

    fun exitDirectoryBrowsing() {
        _uiState.update { it.copy(directoryStack = emptyList(), directoryContents = emptyList(), isBrowsingDirectory = false) }
    }
}
