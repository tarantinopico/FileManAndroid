package com.example.viewmodel

import android.app.Application
import android.os.Build
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.BreadcrumbModel
import com.example.model.FavoriteModel
import com.example.model.FileModel
import com.example.model.FileOperationResult
import com.example.model.StorageVolumeModel
import com.example.repository.FavoritesRepository
import com.example.repository.FavoritesRepositoryImpl
import com.example.repository.FileRepository
import com.example.repository.FileRepositoryImpl
import com.example.repository.StorageRepository
import com.example.repository.StorageRepositoryImpl
import com.example.ui.state.ClipboardOperation
import com.example.ui.state.ClipboardState
import com.example.ui.state.FileManagerState
import com.example.ui.state.UiEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FileManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FileRepository = FileRepositoryImpl()
    private val storageRepository: StorageRepository = StorageRepositoryImpl(application)
    private val favoritesRepository: FavoritesRepository = FavoritesRepositoryImpl(application)

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private val _uiState = MutableStateFlow(FileManagerState())
    val uiState: StateFlow<FileManagerState> = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()

    private val _storageVolumes = MutableStateFlow<List<StorageVolumeModel>>(emptyList())
    val storageVolumes: StateFlow<List<StorageVolumeModel>> = _storageVolumes.asStateFlow()

    private val _favorites = MutableStateFlow<List<FavoriteModel>>(emptyList())
    val favorites: StateFlow<List<FavoriteModel>> = _favorites.asStateFlow()

    init {
        checkPermission()
        loadDrawerData()
    }

    private fun loadDrawerData() {
        viewModelScope.launch {
            storageRepository.getStorageVolumes().collect { volumes ->
                _storageVolumes.value = volumes
                // If not accessed yet, start at primary storage
                if (_hasPermission.value && _uiState.value.currentPath.isEmpty() && volumes.isNotEmpty()) {
                    loadDirectory(volumes.first { it.isPrimary }.path)
                }
            }
        }
        viewModelScope.launch {
            favoritesRepository.favorites.collect { favs ->
                _favorites.value = favs
            }
        }
    }

    fun checkPermission() {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true 
        }
        _hasPermission.value = granted
        if (granted && _uiState.value.currentPath.isEmpty()) {
            val primary = _storageVolumes.value.firstOrNull { it.isPrimary }?.path
            if (primary != null) {
                loadDirectory(primary)
            } else {
                loadDirectory(repository.getRootPath())
            }
        }
    }

    fun loadDirectory(path: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val result = repository.listFiles(path)
            
            result.onSuccess { files ->
                val primaryRoot = _storageVolumes.value.firstOrNull { it.isPrimary }?.path ?: repository.getRootPath()
                val currentVolume = _storageVolumes.value.find { path.startsWith(it.path) }
                val rootPathForVolume = currentVolume?.path ?: primaryRoot
                
                val parentPath = if (path == rootPathForVolume) null else java.io.File(path).parent
                
                _uiState.update { 
                    it.copy(
                        currentPath = path,
                        parentPath = parentPath,
                        files = files,
                        breadcrumbs = buildBreadcrumbs(rootPathForVolume, path, currentVolume?.name ?: "Domů"),
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "Složka není přístupná: ${error.message}"
                    )
                }
            }
        }
    }
    
    fun navigateUp() {
        val currentContext = _uiState.value
        if (currentContext.parentPath != null) {
            loadDirectory(currentContext.parentPath)
        }
    }

    fun createFolder(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val currentPath = _uiState.value.currentPath
            val result = repository.createFolder(currentPath, name)
            handleOperationResult(result, currentPath, "Složka '$name' byla vytvořena.")
        }
    }

    fun deleteItem(file: FileModel) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val currentPath = _uiState.value.currentPath
            val result = repository.delete(file.path)
            
            // clear clipboard if we're deleting the copied/moved item
            val clipboard = _uiState.value.clipboard
            if (clipboard?.file?.path == file.path) {
                _uiState.update { it.copy(clipboard = null) }
            }
            
            handleOperationResult(result, currentPath, "Položka byla úspěšně smazána.")
        }
    }

    fun renameItem(file: FileModel, newName: String) {
        if (newName.isBlank() || newName == file.name) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val currentPath = _uiState.value.currentPath
            val result = repository.rename(file.path, newName)
            handleOperationResult(result, currentPath, "Položka byla úspěšně přejmenována.")
        }
    }

    fun setClipboard(file: FileModel, operation: ClipboardOperation) {
        _uiState.update { 
            it.copy(clipboard = ClipboardState(file, operation))
        }
        viewModelScope.launch {
            val verb = if (operation == ClipboardOperation.COPY) "kopírování" else "přesunu"
            _uiEvents.emit(UiEvent.ShowToast("Položka '${file.name}' připravena ke $verb."))
        }
    }
    
    fun cancelClipboard() {
        _uiState.update { it.copy(clipboard = null) }
    }

    fun pasteFromClipboard() {
        val clipboard = _uiState.value.clipboard ?: return
        val currentPath = _uiState.value.currentPath
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val result = when (clipboard.operation) {
                ClipboardOperation.COPY -> repository.copy(clipboard.file.path, currentPath)
                ClipboardOperation.MOVE -> repository.move(clipboard.file.path, currentPath)
            }
            
            if (result is FileOperationResult.Success) {
                if (clipboard.operation == ClipboardOperation.MOVE) {
                    _uiState.update { it.copy(clipboard = null) }
                }
            }
            
            val msgVerb = if (clipboard.operation == ClipboardOperation.COPY) "zkopírována" else "přesunuta"
            handleOperationResult(result, currentPath, "Položka úspěšně $msgVerb.")
        }
    }

    fun toggleFavorite(path: String, name: String) {
        viewModelScope.launch {
            val isFavorite = _favorites.value.any { it.path == path }
            if (isFavorite) {
                favoritesRepository.removeFavorite(path)
                _uiEvents.emit(UiEvent.ShowToast("Odebráno z oblíbených"))
            } else {
                favoritesRepository.addFavorite(path, name)
                _uiEvents.emit(UiEvent.ShowToast("Přidáno do oblíbených"))
            }
        }
    }

    private suspend fun handleOperationResult(result: FileOperationResult, refreshPath: String, successMsg: String) {
        when (result) {
            is FileOperationResult.Success -> {
                _uiEvents.emit(UiEvent.ShowToast(successMsg))
                loadDirectory(refreshPath)
            }
            is FileOperationResult.Error -> {
                _uiState.update { it.copy(isLoading = false) } 
                _uiEvents.emit(UiEvent.Error(result.message))
            }
        }
    }

    private fun buildBreadcrumbs(rootPath: String, currentPath: String, rootName: String): List<BreadcrumbModel> {
        val breadcrumbs = mutableListOf<BreadcrumbModel>()
        
        breadcrumbs.add(BreadcrumbModel(rootName, rootPath))
        
        if (currentPath == rootPath) {
            return breadcrumbs
        }
        
        var relativePath = currentPath.removePrefix(rootPath)
        if (relativePath.startsWith("/")) relativePath = relativePath.drop(1)
        
        val segments = relativePath.split("/")
        var buildPath = rootPath
        
        for (segment in segments) {
            if (segment.isNotEmpty()) {
                buildPath += "/$segment"
                breadcrumbs.add(BreadcrumbModel(segment, buildPath))
            }
        }
        
        return breadcrumbs
    }
}
