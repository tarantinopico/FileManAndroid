package com.example.viewmodel

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.BreadcrumbModel
import com.example.model.FileModel
import com.example.model.FileOperationResult
import com.example.model.ThemeMode
import com.example.repository.FileRepository
import com.example.repository.SettingsRepository
import com.example.repository.StorageRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ClipboardOperation { COPY, MOVE }
data class ClipboardState(val file: FileModel, val operation: ClipboardOperation)

data class FileManagerState(
    val currentPath: String = "",
    val parentPath: String? = null,
    val files: List<FileModel> = emptyList(),
    val breadcrumbs: List<BreadcrumbModel> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedFiles: Set<String> = emptySet(),
    val searchQuery: String = ""
)

sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
}

class FileManagerViewModel(application: Application) : AndroidViewModel(application) {
    private val fileRepository = FileRepository()
    private val storageRepository = StorageRepository(application)
    private val settingsRepository = SettingsRepository(application)

    private val _uiState = MutableStateFlow(FileManagerState())
    val uiState = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission = _hasPermission.asStateFlow()

    private val _clipboard = MutableStateFlow<ClipboardState?>(null)
    val clipboard = _clipboard.asStateFlow()

    val storageVolumes = MutableStateFlow(storageRepository.getStorageVolumes())
    val themePreference = settingsRepository.themePreference
    val densityPreference = settingsRepository.densityPreference
    val favorites = settingsRepository.favorites

    init {
        checkPermission()
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }
    
    fun setUiDensity(density: com.example.model.UiDensity) {
        viewModelScope.launch {
            settingsRepository.setUiDensity(density)
        }
    }

    fun toggleFavorite(path: String, name: String, isFavorite: Boolean) {
        viewModelScope.launch {
            if (isFavorite) {
                settingsRepository.removeFavorite(path)
                _uiEvents.emit(UiEvent.ShowToast("Odebráno z oblíbených"))
            } else {
                settingsRepository.addFavorite(path, name)
                _uiEvents.emit(UiEvent.ShowToast("Přidáno do oblíbených"))
            }
        }
    }

    fun checkPermission() {
        val granted = Environment.isExternalStorageManager()
        _hasPermission.value = granted
        if (granted && _uiState.value.currentPath.isEmpty()) {
            val primary = storageVolumes.value.find { it.isPrimary }?.path ?: fileRepository.getRootPath()
            loadDirectory(primary)
        }
    }

    fun loadDirectory(path: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            fileRepository.listFiles(path).onSuccess { files ->
                val primaryRoot = storageVolumes.value.find { it.isPrimary }?.path ?: fileRepository.getRootPath()
                val currentVolume = storageVolumes.value.find { path.startsWith(it.path) }
                val rootPathForVolume = currentVolume?.path ?: primaryRoot
                
                val parentPath = if (path == rootPathForVolume) null else java.io.File(path).parent
                _uiState.update {
                    it.copy(
                        currentPath = path,
                        parentPath = parentPath,
                        files = files,
                        breadcrumbs = buildBreadcrumbs(rootPathForVolume, path, currentVolume?.name ?: "Domů"),
                        isLoading = false
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun navigateUp() {
        _uiState.value.parentPath?.let { loadDirectory(it) }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            val result = fileRepository.createFolder(_uiState.value.currentPath, name)
            handleOperationResult(result, "Složka vytvořena")
        }
    }

    fun createFile(name: String, content: String? = null) {
        viewModelScope.launch {
            val result = fileRepository.createFile(_uiState.value.currentPath, name, content)
            handleOperationResult(result, "Soubor vytvořen")
        }
    }

    fun deleteItem(file: FileModel) {
        viewModelScope.launch {
            val result = fileRepository.deleteFile(file.path)
            handleOperationResult(result, "Smazáno")
        }
    }

    fun renameItem(file: FileModel, newName: String) {
        viewModelScope.launch {
            val result = fileRepository.renameFile(file.path, newName)
            handleOperationResult(result, "Přejmenováno")
        }
    }

    fun setClipboard(file: FileModel, operation: ClipboardOperation) {
        _clipboard.value = ClipboardState(file, operation)
    }

    fun cancelClipboard() {
        _clipboard.value = null
    }

    fun pasteFromClipboard() {
        val clip = _clipboard.value ?: return
        val dest = _uiState.value.currentPath
        viewModelScope.launch {
            val result = if (clip.operation == ClipboardOperation.COPY) {
                fileRepository.copyFile(clip.file.path, dest)
            } else {
                fileRepository.moveFile(clip.file.path, dest)
            }
            _clipboard.value = null
            handleOperationResult(result, "Dokončeno")
        }
    }

    private suspend fun handleOperationResult(result: FileOperationResult, successMsg: String) {
        when (result) {
            is FileOperationResult.Success -> {
                loadDirectory(_uiState.value.currentPath)
                _uiEvents.emit(UiEvent.ShowToast(successMsg))
            }
            is FileOperationResult.Error -> {
                _uiEvents.emit(UiEvent.ShowToast(result.message))
            }
        }
    }

    private fun buildBreadcrumbs(rootPath: String, currentPath: String, rootName: String): List<BreadcrumbModel> {
        val breadcrumbs = mutableListOf(BreadcrumbModel(rootName, rootPath))
        if (currentPath == rootPath) return breadcrumbs

        val relative = currentPath.substringAfter(rootPath).removePrefix("/")
        if (relative.isNotEmpty()) {
            val parts = relative.split("/")
            var pathBuilder = rootPath
            for (part in parts) {
                pathBuilder += "/$part"
                breadcrumbs.add(BreadcrumbModel(part, pathBuilder))
            }
        }
        return breadcrumbs
    }

    fun toggleSelection(path: String) {
        _uiState.update { state ->
            val set = state.selectedFiles.toMutableSet()
            if (set.contains(path)) set.remove(path) else set.add(path)
            state.copy(selectedFiles = set)
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            state.copy(selectedFiles = state.files.map { it.path }.toSet())
        }
    }

    fun clearSelection() {
        _uiState.update { state ->
            state.copy(selectedFiles = emptySet())
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun deleteSelected() {
        val selected = _uiState.value.selectedFiles.toList()
        if (selected.isEmpty()) return
        clearSelection()
        viewModelScope.launch {
            var errors = 0
            for (path in selected) {
                if (fileRepository.deleteFile(path) is FileOperationResult.Error) errors++
            }
            if (errors == 0) handleOperationResult(FileOperationResult.Success, "Smazáno")
            else handleOperationResult(FileOperationResult.Error("Některé soubory se nepodařilo smazat"), "")
        }
    }

    fun zipSelected(zipName: String) {
        val selected = _uiState.value.selectedFiles.toList()
        if (selected.isEmpty()) return
        val currentPath = _uiState.value.currentPath
        clearSelection()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = fileRepository.zipFiles(selected, currentPath, zipName)
            _uiState.update { it.copy(isLoading = false) }
            handleOperationResult(result, "Zabaleno")
        }
    }

    fun unzipFile(file: FileModel) {
        val currentPath = _uiState.value.currentPath
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = fileRepository.unzipFile(file.path, currentPath)
            _uiState.update { it.copy(isLoading = false) }
            handleOperationResult(result, "Rozbaleno")
        }
    }
    
    fun encryptSelected(password: String) {
        val selected = _uiState.value.selectedFiles.toList()
        if (selected.isEmpty()) return
        val currentPath = _uiState.value.currentPath
        clearSelection()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = fileRepository.encryptFiles(selected, currentPath, password)
            _uiState.update { it.copy(isLoading = false) }
            handleOperationResult(result, "Zašifrováno")
        }
    }
    
    fun decryptSelected(file: FileModel, password: String) {
        val currentPath = _uiState.value.currentPath
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = fileRepository.decryptFile(file.path, currentPath, password)
            _uiState.update { it.copy(isLoading = false) }
            handleOperationResult(result, "Dešifrováno")
        }
    }
}
