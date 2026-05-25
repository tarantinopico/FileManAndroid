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
import com.example.repository.TagsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ClipboardOperation { COPY, MOVE }
data class ClipboardState(val files: List<FileModel>, val operation: ClipboardOperation)

data class FileManagerState(
    val currentPath: String = "",
    val parentPath: String? = null,
    val files: List<FileModel> = emptyList(),
    val breadcrumbs: List<BreadcrumbModel> = emptyList(),
    val isLoading: Boolean = false,
    val loadingMessage: String? = null,
    val errorMessage: String? = null,
    val selectedFiles: Set<String> = emptySet(),
    val searchQuery: String = "",
    val searchResults: List<FileModel>? = null,
    val historyBackStack: List<String> = emptyList(),
    val historyForwardStack: List<String> = emptyList()
)

sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
}

class FileManagerViewModel(application: Application) : AndroidViewModel(application) {
    private val localFileRepository = FileRepository()
    private val sftpRepository = com.example.repository.SftpRepository()
    private val ftpRepository = com.example.repository.FtpRepository()
    val remoteServersRepository = com.example.repository.RemoteServersRepository(application)
    
    private val virtualFileSystem = com.example.repository.VirtualFileSystem(
        localFileRepository, sftpRepository, ftpRepository, remoteServersRepository
    )
    
    private val storageRepository = StorageRepository(application)
    private val settingsRepository = SettingsRepository(application)
    private val tagsRepository = TagsRepository(application)

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
    val editorSettings = settingsRepository.editorSettings
    val syntaxMappings = settingsRepository.syntaxMappings
    val fileSettings = settingsRepository.fileSettings.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.Eagerly,
        initialValue = com.example.model.FileSettings()
    )
    val favorites = settingsRepository.favorites
    val gitAuthSettings = settingsRepository.gitAuthSettings
    val appPreferences = settingsRepository.appPreferences.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.Eagerly,
        initialValue = com.example.model.AppPreferences()
    )
    
    val availableTags = tagsRepository.availableTags
    val fileTags = tagsRepository.fileTags

    private var _currentFilesRaw = emptyList<FileModel>()

    init {
        checkPermission()
        
        viewModelScope.launch {
            fileSettings.collect {
                if (_uiState.value.currentPath.isNotEmpty()) {
                    loadDirectory(_uiState.value.currentPath, addToHistory = false)
                }
            }
        }
        
        viewModelScope.launch {
            appPreferences.collect { prefs ->
                if (_hasPermission.value && _uiState.value.currentPath.isEmpty()) {
                    if (prefs.openLastLocationOnStartup && !prefs.lastOpenedLocation.isNullOrEmpty()) {
                        loadDirectory(prefs.lastOpenedLocation, addToHistory = true)
                    } else {
                        val primary = storageVolumes.value.find { it.isPrimary }?.path ?: localFileRepository.getRootPath()
                        loadDirectory(primary, addToHistory = true)
                    }
                }
            }
        }

        viewModelScope.launch {
            tagsRepository.fileTags.collect { tagsMap ->
                val currentFiles = _currentFilesRaw
                if (currentFiles.isNotEmpty()) {
                    _uiState.update { state ->
                        state.copy(files = applyFileSettings(currentFiles).map { it.copy(tags = tagsMap[it.path] ?: emptyList()) })
                    }
                }
            }
        }
    }

    fun updateAppPreferences(prefs: com.example.model.AppPreferences) {
        viewModelScope.launch {
            settingsRepository.updateAppPreferences(prefs)
        }
    }
    
    fun updateGitAuthSettings(settings: com.example.model.GitAuthSettings, token: String? = null) {
        viewModelScope.launch {
            settingsRepository.updateGitAuthSettings(settings, token)
        }
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

    fun updateEditorSettings(settings: com.example.model.EditorSettings) {
        viewModelScope.launch {
            settingsRepository.updateEditorSettings(settings)
        }
    }

    fun updateFileSettings(settings: com.example.model.FileSettings) {
        viewModelScope.launch {
            settingsRepository.updateFileSettings(settings)
            
            // Reload if we have a current path
            val currentPath = _uiState.value.currentPath
            if (currentPath.isNotEmpty()) {
                loadDirectory(currentPath)
            }
        }
    }

    fun addSyntaxMapping(extension: String, language: com.example.model.SyntaxLanguage, tagColorArgb: Int? = null) {
        viewModelScope.launch {
            settingsRepository.addSyntaxMapping(com.example.model.SyntaxMapping(extension.lowercase().removePrefix("."), language, tagColorArgb))
        }
    }

    fun removeSyntaxMapping(extension: String) {
        viewModelScope.launch {
            settingsRepository.removeSyntaxMapping(extension.lowercase().removePrefix("."))
        }
    }

    fun resetSyntaxMappings() {
        viewModelScope.launch {
            settingsRepository.resetSyntaxMappings()
        }
    }

    fun saveFavorite(favorite: com.example.model.FavoriteModel) {
        viewModelScope.launch {
            settingsRepository.saveFavorite(favorite)
            _uiEvents.emit(UiEvent.ShowToast("Uloženo do oblíbených"))
        }
    }

    fun removeFavorite(path: String) {
        viewModelScope.launch {
            settingsRepository.removeFavorite(path)
            _uiEvents.emit(UiEvent.ShowToast("Odebráno z oblíbených"))
        }
    }

    fun checkPermission() {
        val granted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            androidx.core.content.ContextCompat.checkSelfPermission(
                getApplication(), 
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        _hasPermission.value = granted
        // Initialization logic is now partially handled by the appPreferences collector
    }

    fun loadDirectory(path: String, addToHistory: Boolean = true) {
        val oldPath = _uiState.value.currentPath
        val backStack = _uiState.value.historyBackStack.toMutableList()
        val forwardStack = _uiState.value.historyForwardStack.toMutableList()
        
        if (addToHistory && oldPath.isNotEmpty() && oldPath != path) {
            backStack.add(oldPath)
            forwardStack.clear()
        }
        
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            virtualFileSystem.listFiles(path).onSuccess { files ->
                _currentFilesRaw = files
                val primaryRoot = storageVolumes.value.find { it.isPrimary }?.path ?: localFileRepository.getRootPath()
                val currentVolume = storageVolumes.value.find { path.startsWith(it.path) }
                val rootPathForVolume = currentVolume?.path ?: primaryRoot
                
                val filteredAndSorted = applyFileSettings(files)
                val tagsMap = tagsRepository.fileTags.value
                val finalizedFiles = filteredAndSorted.map { it.copy(tags = tagsMap[it.path] ?: emptyList()) }
                
                // save last opened location
                settingsRepository.setLastOpenedLocation(path)
                
                val parentPath = if (path == rootPathForVolume) null else virtualFileSystem.getParentPath(path)
                _uiState.update {
                    it.copy(
                        currentPath = path,
                        parentPath = parentPath,
                        files = finalizedFiles,
                        breadcrumbs = buildBreadcrumbs(rootPathForVolume, path, currentVolume?.name ?: "Domů"),
                        isLoading = false,
                        historyBackStack = backStack,
                        historyForwardStack = forwardStack,
                        searchQuery = "", // Clear search on navigation
                        searchResults = null
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }
    
    private fun applyFileSettings(files: List<FileModel>): List<FileModel> {
        var result = files
        val settings = fileSettings.value
        
        if (!settings.showHiddenFiles) {
            result = result.filter { !it.name.startsWith(".") }
        }
        
        val comparator = when (settings.sortOption) {
            com.example.model.SortOption.NAME_ASC -> compareBy<FileModel> { !it.isDirectory }.thenBy { it.name.lowercase() }
            com.example.model.SortOption.NAME_DESC -> compareBy<FileModel> { !it.isDirectory }.thenByDescending { it.name.lowercase() }
            com.example.model.SortOption.SIZE_ASC -> compareBy<FileModel> { !it.isDirectory }.thenBy { it.size }
            com.example.model.SortOption.SIZE_DESC -> compareBy<FileModel> { !it.isDirectory }.thenByDescending { it.size }
            com.example.model.SortOption.DATE_ASC -> compareBy<FileModel> { !it.isDirectory }.thenBy { it.lastModified }
            com.example.model.SortOption.DATE_DESC -> compareBy<FileModel> { !it.isDirectory }.thenByDescending { it.lastModified }
            com.example.model.SortOption.TYPE_ASC -> compareBy<FileModel> { !it.isDirectory }.thenBy { it.name.substringAfterLast('.', "").lowercase() }.thenBy { it.name.lowercase() }
            com.example.model.SortOption.TYPE_DESC -> compareBy<FileModel> { !it.isDirectory }.thenByDescending { it.name.substringAfterLast('.', "").lowercase() }.thenByDescending { it.name.lowercase() }
        }
        return result.sortedWith(comparator)
    }

    fun navigateUp() {
        _uiState.value.parentPath?.let { loadDirectory(it) }
    }
    
    fun navigateBack() {
        val stack = _uiState.value.historyBackStack
        if (stack.isNotEmpty()) {
            val prev = stack.last()
            val newBackStack = stack.dropLast(1)
            val newForwardStack = listOf(_uiState.value.currentPath) + _uiState.value.historyForwardStack
            
            _uiState.update { it.copy(historyBackStack = newBackStack, historyForwardStack = newForwardStack) }
            loadDirectory(prev, addToHistory = false)
        } else {
            navigateUp()
        }
    }
    
    fun navigateForward() {
        val stack = _uiState.value.historyForwardStack
        if (stack.isNotEmpty()) {
            val next = stack.first()
            val newForwardStack = stack.drop(1)
            val newBackStack = _uiState.value.historyBackStack + _uiState.value.currentPath
            
            _uiState.update { it.copy(historyBackStack = newBackStack, historyForwardStack = newForwardStack) }
            loadDirectory(next, addToHistory = false)
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            val result = virtualFileSystem.createFolder(_uiState.value.currentPath, name)
            handleOperationResult(result, "Složka vytvořena")
        }
    }

    fun createFile(name: String, content: String? = null) {
        viewModelScope.launch {
            val result = virtualFileSystem.createFile(_uiState.value.currentPath, name, content)
            handleOperationResult(result, "Soubor vytvořen")
        }
    }

    fun deleteItem(file: FileModel) {
        viewModelScope.launch {
            val result = virtualFileSystem.deleteFile(file.path, file.isDirectory)
            handleOperationResult(result, "Smazáno")
        }
    }

    fun renameItem(file: FileModel, newName: String) {
        viewModelScope.launch {
            val result = virtualFileSystem.renameFile(file.path, newName)
            handleOperationResult(result, "Přejmenováno")
        }
    }

    fun bulkRename(baseName: String) {
        val selected = _uiState.value.selectedFiles.toList()
        if (selected.isEmpty()) return
        clearSelection()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            var errors = 0
            
            // Sort by name originally so that numbering makes some sense
            val filesToRename = selected.mapNotNull { path -> _uiState.value.files.find { it.path == path } }.sortedBy { it.name }
            
            for ((index, file) in filesToRename.withIndex()) {
                val ext = if (file.isDirectory) "" else ".${file.name.substringAfterLast('.', "")}".takeIf { it != "." } ?: ""
                val newName = if (filesToRename.size > 1) "${baseName}_${index + 1}$ext" else "$baseName$ext"
                val result = virtualFileSystem.renameFile(file.path, newName)
                if (result is FileOperationResult.Error) errors++
            }
            
            _uiState.update { it.copy(isLoading = false) }
            if (errors == 0) {
                handleOperationResult(FileOperationResult.Success, "Přejmenováno")
            } else {
                handleOperationResult(FileOperationResult.Error("Některé položky se nepodařilo přejmenovat"), "")
            }
        }
    }

    fun setClipboard(files: List<FileModel>, operation: ClipboardOperation) {
        _clipboard.value = ClipboardState(files, operation)
    }

    fun cancelClipboard() {
        _clipboard.value = null
    }

    fun pasteFromClipboard() {
        val clip = _clipboard.value ?: return
        val dest = _uiState.value.currentPath
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Kopírování...") }
            var errors = 0
            for (file in clip.files) {
                val result = if (clip.operation == ClipboardOperation.COPY) {
                    virtualFileSystem.copyFile(file, dest)
                } else {
                    virtualFileSystem.moveFile(file, dest)
                }
                if (result is FileOperationResult.Error) errors++
            }
            _uiState.update { it.copy(isLoading = false, loadingMessage = null) }
            _clipboard.value = null
            if (errors == 0) {
                handleOperationResult(FileOperationResult.Success, "Dokončeno")
            } else {
                handleOperationResult(FileOperationResult.Error("Operace se nezdařila u $errors položek"), "")
            }
        }
    }

    fun addTagToFile(path: String, tag: com.example.model.TagModel) {
        viewModelScope.launch {
            tagsRepository.addTagToFile(path, tag)
        }
    }

    fun removeTagFromFile(path: String, tagId: String) {
        viewModelScope.launch {
            tagsRepository.removeTagFromFile(path, tagId)
        }
    }

    fun addAvailableTag(name: String, colorArgb: Int) {
        viewModelScope.launch {
            tagsRepository.addAvailableTag(name, colorArgb)
        }
    }

    fun updateAvailableTag(tag: com.example.model.TagModel) {
        viewModelScope.launch {
            tagsRepository.updateAvailableTag(tag)
        }
    }

    fun deleteAvailableTag(tagId: String) {
        viewModelScope.launch {
            tagsRepository.deleteAvailableTag(tagId)
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
        _uiState.update { state -> 
            val newResults = if (query.isNotBlank()) {
                state.files.filter { it.name.contains(query, ignoreCase = true) }
            } else null
            state.copy(searchQuery = query, searchResults = newResults) 
        }
    }

    fun deleteSelected() {
        val selected = _uiState.value.selectedFiles.toList()
        if (selected.isEmpty()) return
        val currentFiles = _uiState.value.files
        clearSelection()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Mazání...") }
            var errors = 0
            for (path in selected) {
                val file = currentFiles.find { it.path == path }
                if (file != null) {
                    if (virtualFileSystem.deleteFile(path, file.isDirectory) is FileOperationResult.Error) errors++
                } else {
                    errors++
                }
            }
            _uiState.update { it.copy(isLoading = false, loadingMessage = null) }
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
            if (currentPath.startsWith("sftp://") || currentPath.startsWith("ftp://")) {
                handleOperationResult(FileOperationResult.Error("Zabalení je povolené jen pro lokální soubory"), "")
                return@launch
            }
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Vytváření ZIP archivu...") }
            val result = localFileRepository.zipFiles(selected, currentPath, zipName)
            _uiState.update { it.copy(isLoading = false, loadingMessage = null) }
            handleOperationResult(result, "Zabaleno")
        }
    }

    fun unzipFile(file: FileModel) {
        val currentPath = _uiState.value.currentPath
        viewModelScope.launch {
            if (currentPath.startsWith("sftp://") || currentPath.startsWith("ftp://")) {
                handleOperationResult(FileOperationResult.Error("Rozbalení je povolené jen pro lokální soubory"), "")
                return@launch
            }
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Rozbalování ${file.name}...") }
            val result = localFileRepository.unzipFile(file.path, currentPath)
            _uiState.update { it.copy(isLoading = false, loadingMessage = null) }
            handleOperationResult(result, "Rozbaleno")
        }
    }
    
    fun encryptSelected(password: String) {
        val selected = _uiState.value.selectedFiles.toList()
        if (selected.isEmpty()) return
        val currentPath = _uiState.value.currentPath
        clearSelection()
        viewModelScope.launch {
            if (currentPath.startsWith("sftp://") || currentPath.startsWith("ftp://")) {
                handleOperationResult(FileOperationResult.Error("Šifrování je povolené jen pro lokální soubory"), "")
                return@launch
            }
            _uiState.update { it.copy(isLoading = true) }
            val result = localFileRepository.encryptFiles(selected, currentPath, password)
            _uiState.update { it.copy(isLoading = false) }
            handleOperationResult(result, "Zašifrováno")
        }
    }
    
    fun decryptSelected(file: FileModel, password: String) {
        val currentPath = _uiState.value.currentPath
        viewModelScope.launch {
            if (currentPath.startsWith("sftp://") || currentPath.startsWith("ftp://")) {
                handleOperationResult(FileOperationResult.Error("Dešifrování je povolené jen pro lokální soubory"), "")
                return@launch
            }
            _uiState.update { it.copy(isLoading = true) }
            val result = localFileRepository.decryptFile(file.path, currentPath, password)
            _uiState.update { it.copy(isLoading = false) }
            handleOperationResult(result, "Dešifrováno")
        }
    }
}
