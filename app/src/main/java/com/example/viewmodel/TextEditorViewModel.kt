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
import kotlinx.coroutines.flow.update
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

    private val _uiState = MutableStateFlow(TextEditorState())
    val uiState = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()
    
    val editorSettings = settingsRepository.editorSettings
    val syntaxMappings = settingsRepository.syntaxMappings
    
    private val undoStack = mutableListOf<TextFieldValue>()
    private val redoStack = mutableListOf<TextFieldValue>()
    private var lastUndoSaveTime = 0L

    fun loadFile(path: String, name: String) {
        val extension = name.substringAfterLast('.', "")
        _uiState.update { it.copy(path = path, name = name, extension = extension, isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            fileRepository.readFileContent(path).onSuccess { text ->
                val tfv = TextFieldValue(text)
                _uiState.update {
                    it.copy(
                        textFieldValue = tfv,
                        originalContent = text,
                        isLoading = false,
                        hasUnsavedChanges = false,
                        canUndo = false,
                        canRedo = false
                    )
                }
                undoStack.clear()
                redoStack.clear()
                undoStack.add(tfv)
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun onContentChanged(newValue: TextFieldValue) {
        val current = _uiState.value.textFieldValue
        if (current.text != newValue.text) {
            val now = System.currentTimeMillis()
            if (now - lastUndoSaveTime > 1000) {
                if (undoStack.lastOrNull()?.text != current.text) {
                    undoStack.add(current)
                    if (undoStack.size > 50) undoStack.removeAt(0)
                }
                redoStack.clear()
                lastUndoSaveTime = now
            }
        }
        
        _uiState.update { state ->
            val hasChanges = newValue.text != state.originalContent
            state.copy(
                textFieldValue = newValue,
                hasUnsavedChanges = hasChanges,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
        
        if (_uiState.value.showFindReplace && current.text != newValue.text) {
            performSearch(newValue.text, _uiState.value.findQuery)
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val current = _uiState.value.textFieldValue
            redoStack.add(current)
            val previous = undoStack.removeLast()
            _uiState.update { 
                it.copy(
                    textFieldValue = previous,
                    hasUnsavedChanges = previous.text != it.originalContent,
                    canUndo = undoStack.isNotEmpty(),
                    canRedo = redoStack.isNotEmpty()
                )
            }
            if (_uiState.value.showFindReplace) {
                performSearch(previous.text, _uiState.value.findQuery)
            }
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val current = _uiState.value.textFieldValue
            undoStack.add(current)
            val next = redoStack.removeLast()
            _uiState.update { 
                it.copy(
                    textFieldValue = next,
                    hasUnsavedChanges = next.text != it.originalContent,
                    canUndo = undoStack.isNotEmpty(),
                    canRedo = redoStack.isNotEmpty()
                )
            }
            if (_uiState.value.showFindReplace) {
                performSearch(next.text, _uiState.value.findQuery)
            }
        }
    }

    fun toggleFindReplace() {
        _uiState.update { it.copy(showFindReplace = !it.showFindReplace) }
        if (!_uiState.value.showFindReplace) {
            _uiState.update { it.copy(findResults = emptyList(), currentFindIndex = -1, findQuery = "", replaceQuery = "") }
        }
    }

    fun onFindQueryChanged(query: String) {
        _uiState.update { it.copy(findQuery = query) }
        performSearch(_uiState.value.textFieldValue.text, query)
    }

    fun onReplaceQueryChanged(query: String) {
        _uiState.update { it.copy(replaceQuery = query) }
    }

    private fun performSearch(text: String, query: String) {
        if (query.isEmpty()) {
            _uiState.update { it.copy(findResults = emptyList(), currentFindIndex = -1) }
            return
        }
        
        val results = mutableListOf<Int>()
        var index = text.indexOf(query, ignoreCase = true)
        while (index >= 0) {
            results.add(index)
            index = text.indexOf(query, index + query.length, ignoreCase = true)
        }
        
        _uiState.update { 
            val currentIndex = if (results.isNotEmpty()) 0 else -1
            it.copy(findResults = results, currentFindIndex = currentIndex)
        }
        selectCurrentSearchResult()
    }

    fun findNext() {
        val state = _uiState.value
        if (state.findResults.isNotEmpty()) {
            val nextIndex = (state.currentFindIndex + 1) % state.findResults.size
            _uiState.update { it.copy(currentFindIndex = nextIndex) }
            selectCurrentSearchResult()
        }
    }

    fun findPrevious() {
        val state = _uiState.value
        if (state.findResults.isNotEmpty()) {
            val prevIndex = if (state.currentFindIndex - 1 < 0) state.findResults.size - 1 else state.currentFindIndex - 1
            _uiState.update { it.copy(currentFindIndex = prevIndex) }
            selectCurrentSearchResult()
        }
    }

    private fun selectCurrentSearchResult() {
        val state = _uiState.value
        if (state.currentFindIndex in state.findResults.indices) {
            val matchIndex = state.findResults[state.currentFindIndex]
            val newValue = state.textFieldValue.copy(
                selection = TextRange(matchIndex, matchIndex + state.findQuery.length)
            )
            _uiState.update { it.copy(textFieldValue = newValue) }
        }
    }

    fun replaceCurrent() {
        val state = _uiState.value
        if (state.currentFindIndex in state.findResults.indices) {
            val matchIndex = state.findResults[state.currentFindIndex]
            val text = state.textFieldValue.text
            val queryLen = state.findQuery.length
            
            val newText = text.substring(0, matchIndex) + state.replaceQuery + text.substring(matchIndex + queryLen)
            
            captureUndoState()
            val newValue = TextFieldValue(newText, TextRange(matchIndex + state.replaceQuery.length))
            _uiState.update { it.copy(textFieldValue = newValue, hasUnsavedChanges = newText != it.originalContent) }
            performSearch(newText, state.findQuery)
        }
    }

    fun replaceAll() {
        val state = _uiState.value
        if (state.findQuery.isNotEmpty() && state.findResults.isNotEmpty()) {
            captureUndoState()
            val newText = state.textFieldValue.text.replace(state.findQuery, state.replaceQuery, ignoreCase = true)
            val newValue = TextFieldValue(newText, TextRange(0))
            _uiState.update { it.copy(textFieldValue = newValue, hasUnsavedChanges = newText != it.originalContent) }
            performSearch(newText, state.findQuery)
        }
    }

    private fun captureUndoState() {
        undoStack.add(_uiState.value.textFieldValue)
        redoStack.clear()
        _uiState.update { it.copy(canUndo = true, canRedo = false) }
    }

    fun setGoToLineDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showGoToLineDialog = visible) }
    }

    fun goToLine(line: Int) {
        val text = _uiState.value.textFieldValue.text
        val lines = text.split("\n")
        if (line in 1..lines.size) {
            var offset = 0
            for (i in 0 until line - 1) {
                offset += lines[i].length + 1
            }
            val newValue = _uiState.value.textFieldValue.copy(selection = TextRange(offset))
            _uiState.update { it.copy(textFieldValue = newValue, showGoToLineDialog = false) }
        } else {
            viewModelScope.launch { _uiEvents.emit(UiEvent.ShowToast("Neplatný řádek")) }
        }
    }

    fun saveFile() {
        val state = _uiState.value
        if (state.hasUnsavedChanges) {
            _uiState.update { it.copy(isSaving = true) }
            viewModelScope.launch {
                val result = fileRepository.saveFileContent(state.path, state.textFieldValue.text)
                _uiState.update { it.copy(isSaving = false) }
                when (result) {
                    is FileOperationResult.Success -> {
                        _uiState.update { it.copy(originalContent = state.textFieldValue.text, hasUnsavedChanges = false) }
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
