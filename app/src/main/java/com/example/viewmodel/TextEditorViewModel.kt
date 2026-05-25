package com.example.viewmodel

import android.app.Application
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.FileOperationResult
import com.example.repository.FileRepository
import com.example.model.EditorSettings
import com.example.model.SyntaxLanguage
import com.example.model.SyntaxMapping
import com.example.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TextEditorState(
    val path: String = "",
    val name: String = "",
    val extension: String = "",
    val textFieldValue: TextFieldValue = TextFieldValue(""),
    val originalContent: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val errorMessage: String? = null,
    
    // Editor features
    val showFindReplace: Boolean = false,
    val findQuery: String = "",
    val replaceQuery: String = "",
    val findResults: List<Int> = emptyList(),
    val currentFindIndex: Int = -1,
    val showGoToLineDialog: Boolean = false,
    
    val canUndo: Boolean = false,
    val canRedo: Boolean = false
)

class TextEditorViewModel(application: Application) : AndroidViewModel(application) {
    private val fileRepository = FileRepository()
    private val settingsRepository = SettingsRepository(application)

    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()
    
    val editorSettings = settingsRepository.editorSettings
    val syntaxMappings = settingsRepository.syntaxMappings
    
    private val _tabs = MutableStateFlow<List<TextEditorState>>(emptyList())
    val tabs = _tabs.asStateFlow()

    private val _activeTabIndex = MutableStateFlow(-1)
    val activeTabIndex = _activeTabIndex.asStateFlow()

    val uiState = kotlinx.coroutines.flow.combine(_tabs, _activeTabIndex) { tabsList, index ->
        if (index in tabsList.indices) tabsList[index] else TextEditorState()
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), TextEditorState())
    
    private val undoStacks = mutableMapOf<String, MutableList<TextFieldValue>>()
    private val redoStacks = mutableMapOf<String, MutableList<TextFieldValue>>()
    private val lastUndoSaveTimes = mutableMapOf<String, Long>()

    // Optional: restore tabs from preferences on init
    init {
        viewModelScope.launch {
            val prefs = settingsRepository.appPreferences.first()
            val openPaths = prefs.openEditorTabs
            if (openPaths.isNotEmpty()) {
                val initialTabs = openPaths.map { path ->
                    TextEditorState(path = path, name = path.substringAfterLast('/'), extension = path.substringAfterLast('.', ""))
                }
                _tabs.value = initialTabs
                val activeTarget = prefs.activeEditorTab ?: openPaths.first()
                val targetIndex = initialTabs.indexOfFirst { it.path == activeTarget }.takeIf { it >= 0 } ?: 0
                _activeTabIndex.value = targetIndex
                
                // Trigger load for the active one at least
                if (targetIndex in initialTabs.indices) {
                    val activePath = initialTabs[targetIndex].path
                    val activeName = initialTabs[targetIndex].name
                    loadFileInternal(activePath, activeName, targetIndex)
                }
            }
        }
    }

    private suspend fun saveTabsState() {
        val currentTabs = _tabs.value.map { it.path }
        val active = _tabs.value.getOrNull(_activeTabIndex.value)?.path
        val prefs = settingsRepository.appPreferences.first()
        settingsRepository.updateAppPreferences(
            prefs.copy(openEditorTabs = currentTabs, activeEditorTab = active)
        )
    }

    fun openFileTab(path: String, name: String) {
        val currentTabs = _tabs.value
        val existingIndex = currentTabs.indexOfFirst { it.path == path }
        if (existingIndex >= 0) {
            _activeTabIndex.value = existingIndex
            if (currentTabs[existingIndex].originalContent.isEmpty() && !currentTabs[existingIndex].isLoading) {
                loadFileInternal(path, name, existingIndex)
            }
        } else {
            val newTab = TextEditorState(path = path, name = name, extension = name.substringAfterLast('.', ""))
            val newTabs = currentTabs + newTab
            _tabs.value = newTabs
            val newIndex = newTabs.size - 1
            _activeTabIndex.value = newIndex
            loadFileInternal(path, name, newIndex)
        }
        viewModelScope.launch { saveTabsState() }
    }

    fun switchToTab(index: Int) {
        if (index in _tabs.value.indices) {
            _activeTabIndex.value = index
            val tab = _tabs.value[index]
            if (tab.originalContent.isEmpty() && !tab.isLoading) {
                loadFileInternal(tab.path, tab.name, index)
            }
            viewModelScope.launch { saveTabsState() }
        }
    }

    fun closeTab(index: Int) {
        val currentTabs = _tabs.value.toMutableList()
        if (index in currentTabs.indices) {
            val removingPath = currentTabs[index].path
            currentTabs.removeAt(index)
            undoStacks.remove(removingPath)
            redoStacks.remove(removingPath)
            lastUndoSaveTimes.remove(removingPath)

            _tabs.value = currentTabs
            if (currentTabs.isEmpty()) {
                _activeTabIndex.value = -1
            } else if (_activeTabIndex.value >= index) {
                _activeTabIndex.value = maxOf(0, _activeTabIndex.value - 1)
            }
            viewModelScope.launch { saveTabsState() }
        }
    }

    fun closeAllTabs() {
        _tabs.value = emptyList()
        _activeTabIndex.value = -1
        undoStacks.clear()
        redoStacks.clear()
        lastUndoSaveTimes.clear()
        viewModelScope.launch { saveTabsState() }
    }
    
    fun closeOtherTabs(index: Int) {
        val currentTabs = _tabs.value
        if (index in currentTabs.indices) {
            val keepTab = currentTabs[index]
            _tabs.value = listOf(keepTab)
            _activeTabIndex.value = 0
            
            // Clean memory
            undoStacks.keys.retainAll(setOf(keepTab.path))
            redoStacks.keys.retainAll(setOf(keepTab.path))
            lastUndoSaveTimes.keys.retainAll(setOf(keepTab.path))
            viewModelScope.launch { saveTabsState() }
        }
    }

    private fun updateActiveTab(update: (TextEditorState) -> TextEditorState) {
        val idx = _activeTabIndex.value
        if (idx in _tabs.value.indices) {
            val currentTabs = _tabs.value.toMutableList()
            currentTabs[idx] = update(currentTabs[idx])
            _tabs.value = currentTabs
        }
    }

    fun loadFile(path: String, name: String) {
        openFileTab(path, name)
    }

    private fun loadFileInternal(path: String, name: String, targetIndex: Int) {
        val tabsList = _tabs.value.toMutableList()
        if (targetIndex !in tabsList.indices) return
        tabsList[targetIndex] = tabsList[targetIndex].copy(isLoading = true, errorMessage = null)
        _tabs.value = tabsList

        viewModelScope.launch {
            val safeMode = settingsRepository.editorSettings.first().largeFileSafeModeEnabled
            fileRepository.readFileContent(path, safeMode).onSuccess { text ->
                val tfv = TextFieldValue(text)
                val currentTabs = _tabs.value.toMutableList()
                val idx = currentTabs.indexOfFirst { it.path == path }
                if (idx >= 0) {
                    currentTabs[idx] = currentTabs[idx].copy(
                        textFieldValue = tfv,
                        originalContent = text,
                        isLoading = false,
                        hasUnsavedChanges = false,
                        canUndo = false,
                        canRedo = false
                    )
                    _tabs.value = currentTabs
                    undoStacks[path] = mutableListOf(tfv)
                    redoStacks[path] = mutableListOf()
                }
            }.onFailure { e ->
                val currentTabs = _tabs.value.toMutableList()
                val idx = currentTabs.indexOfFirst { it.path == path }
                if (idx >= 0) {
                    currentTabs[idx] = currentTabs[idx].copy(isLoading = false, errorMessage = e.message)
                    _tabs.value = currentTabs
                }
            }
        }
    }

    fun onContentChanged(newValue: TextFieldValue) {
        val state = uiState.value
        val path = state.path
        if (path.isEmpty()) return
        
        val current = state.textFieldValue
        if (current.text != newValue.text) {
            val now = System.currentTimeMillis()
            val lastTime = lastUndoSaveTimes[path] ?: 0L
            val stack = undoStacks.getOrPut(path) { mutableListOf() }
            
            if (now - lastTime > 1000) {
                if (stack.lastOrNull()?.text != current.text) {
                    stack.add(current)
                    if (stack.size > 50) stack.removeAt(0)
                }
                redoStacks[path]?.clear()
                lastUndoSaveTimes[path] = now
            }
        }
        
        updateActiveTab { s ->
            val hasChanges = newValue.text != s.originalContent
            s.copy(
                textFieldValue = newValue,
                hasUnsavedChanges = hasChanges,
                canUndo = (undoStacks[path]?.isNotEmpty() == true),
                canRedo = (redoStacks[path]?.isNotEmpty() == true)
            )
        }
        
        if (state.showFindReplace && current.text != newValue.text) {
            performSearch(newValue.text, state.findQuery)
        }
    }

    fun undo() {
        val state = uiState.value
        val path = state.path
        val stack = undoStacks[path] ?: return
        if (stack.isNotEmpty()) {
            val redoList = redoStacks.getOrPut(path) { mutableListOf() }
            redoList.add(state.textFieldValue)
            val previous = stack.removeLast()
            
            updateActiveTab { s ->
                s.copy(
                    textFieldValue = previous,
                    hasUnsavedChanges = previous.text != s.originalContent,
                    canUndo = stack.isNotEmpty(),
                    canRedo = redoList.isNotEmpty()
                )
            }
            if (state.showFindReplace) {
                performSearch(previous.text, state.findQuery)
            }
        }
    }

    fun redo() {
        val state = uiState.value
        val path = state.path
        val redoList = redoStacks[path] ?: return
        if (redoList.isNotEmpty()) {
            val stack = undoStacks.getOrPut(path) { mutableListOf() }
            stack.add(state.textFieldValue)
            val next = redoList.removeLast()
            
            updateActiveTab { s ->
                s.copy(
                    textFieldValue = next,
                    hasUnsavedChanges = next.text != s.originalContent,
                    canUndo = stack.isNotEmpty(),
                    canRedo = redoList.isNotEmpty()
                )
            }
            if (state.showFindReplace) {
                performSearch(next.text, state.findQuery)
            }
        }
    }

    fun toggleFindReplace() {
        updateActiveTab { it.copy(showFindReplace = !it.showFindReplace) }
        val state = uiState.value
        if (!state.showFindReplace) {
            updateActiveTab { it.copy(findResults = emptyList(), currentFindIndex = -1, findQuery = "", replaceQuery = "") }
        }
    }

    fun onFindQueryChanged(query: String) {
        updateActiveTab { it.copy(findQuery = query) }
        performSearch(uiState.value.textFieldValue.text, query)
    }

    fun onReplaceQueryChanged(query: String) {
        updateActiveTab { it.copy(replaceQuery = query) }
    }

    private fun performSearch(text: String, query: String) {
        if (query.isEmpty()) {
            updateActiveTab { it.copy(findResults = emptyList(), currentFindIndex = -1) }
            return
        }
        
        val results = mutableListOf<Int>()
        var index = text.indexOf(query, ignoreCase = true)
        while (index >= 0) {
            results.add(index)
            index = text.indexOf(query, index + query.length, ignoreCase = true)
        }
        
        val currentIndex = if (results.isNotEmpty()) 0 else -1
        updateActiveTab { it.copy(findResults = results, currentFindIndex = currentIndex) }
        selectCurrentSearchResult()
    }

    fun findNext() {
        val state = uiState.value
        if (state.findResults.isNotEmpty()) {
            val nextIndex = (state.currentFindIndex + 1) % state.findResults.size
            updateActiveTab { it.copy(currentFindIndex = nextIndex) }
            selectCurrentSearchResult()
        }
    }

    fun findPrevious() {
        val state = uiState.value
        if (state.findResults.isNotEmpty()) {
            val prevIndex = if (state.currentFindIndex - 1 < 0) state.findResults.size - 1 else state.currentFindIndex - 1
            updateActiveTab { it.copy(currentFindIndex = prevIndex) }
            selectCurrentSearchResult()
        }
    }

    private fun selectCurrentSearchResult() {
        val state = uiState.value
        if (state.currentFindIndex in state.findResults.indices) {
            val matchIndex = state.findResults[state.currentFindIndex]
            val newValue = state.textFieldValue.copy(
                selection = TextRange(matchIndex, matchIndex + state.findQuery.length)
            )
            updateActiveTab { it.copy(textFieldValue = newValue) }
        }
    }

    fun replaceCurrent() {
        val state = uiState.value
        if (state.currentFindIndex in state.findResults.indices) {
            val matchIndex = state.findResults[state.currentFindIndex]
            val text = state.textFieldValue.text
            val queryLen = state.findQuery.length
            
            val newText = text.substring(0, matchIndex) + state.replaceQuery + text.substring(matchIndex + queryLen)
            
            captureUndoState()
            val newValue = TextFieldValue(newText, TextRange(matchIndex + state.replaceQuery.length))
            updateActiveTab { it.copy(textFieldValue = newValue, hasUnsavedChanges = newText != it.originalContent) }
            performSearch(newText, state.findQuery)
        }
    }

    fun replaceAll() {
        val state = uiState.value
        if (state.findQuery.isNotEmpty() && state.findResults.isNotEmpty()) {
            captureUndoState()
            val newText = state.textFieldValue.text.replace(state.findQuery, state.replaceQuery, ignoreCase = true)
            val newValue = TextFieldValue(newText, TextRange(0))
            updateActiveTab { it.copy(textFieldValue = newValue, hasUnsavedChanges = newText != it.originalContent) }
            performSearch(newText, state.findQuery)
        }
    }

    private fun captureUndoState() {
        val path = uiState.value.path
        if (path.isEmpty()) return
        undoStacks.getOrPut(path) { mutableListOf() }.add(uiState.value.textFieldValue)
        redoStacks[path]?.clear()
        updateActiveTab { it.copy(canUndo = true, canRedo = false) }
    }

    fun setGoToLineDialogVisible(visible: Boolean) {
        updateActiveTab { it.copy(showGoToLineDialog = visible) }
    }

    fun goToLine(line: Int) {
        val text = uiState.value.textFieldValue.text
        val lines = text.split("\n")
        if (line in 1..lines.size) {
            var offset = 0
            for (i in 0 until line - 1) {
                offset += lines[i].length + 1
            }
            val newValue = uiState.value.textFieldValue.copy(selection = TextRange(offset))
            updateActiveTab { it.copy(textFieldValue = newValue, showGoToLineDialog = false) }
        } else {
            viewModelScope.launch { _uiEvents.emit(UiEvent.ShowToast("Neplatný řádek")) }
        }
    }

    fun saveFile() {
        val state = uiState.value
        if (state.hasUnsavedChanges && state.path.isNotEmpty()) {
            updateActiveTab { it.copy(isSaving = true) }
            viewModelScope.launch {
                val result = fileRepository.saveFileContent(state.path, state.textFieldValue.text)
                updateActiveTab { it.copy(isSaving = false) }
                when (result) {
                    is FileOperationResult.Success -> {
                        updateActiveTab { it.copy(originalContent = state.textFieldValue.text, hasUnsavedChanges = false) }
                        _uiEvents.emit(UiEvent.ShowToast("Uloženo"))
                    }
                    is FileOperationResult.Error -> {
                        _uiEvents.emit(UiEvent.ShowToast(result.message))
                    }
                }
            }
        }
    }
}
