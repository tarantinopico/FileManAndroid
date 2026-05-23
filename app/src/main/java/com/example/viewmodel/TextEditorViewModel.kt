package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.FileOperationResult
import com.example.repository.FileRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TextEditorState(
    val path: String = "",
    val name: String = "",
    val content: String = "",
    val originalContent: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val errorMessage: String? = null
)

class TextEditorViewModel(application: Application) : AndroidViewModel(application) {
    private val fileRepository = FileRepository()

    private val _uiState = MutableStateFlow(TextEditorState())
    val uiState = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    fun loadFile(path: String, name: String) {
        _uiState.update { it.copy(path = path, name = name, isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            fileRepository.readFileContent(path).onSuccess { text ->
                _uiState.update {
                    it.copy(
                        content = text,
                        originalContent = text,
                        isLoading = false,
                        hasUnsavedChanges = false
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun onContentChanged(newContent: String) {
        _uiState.update {
            it.copy(
                content = newContent,
                hasUnsavedChanges = newContent != it.originalContent
            )
        }
    }

    fun saveFile(onSuccess: () -> Unit) {
        val state = _uiState.value
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = fileRepository.saveFileContent(state.path, state.content)
            _uiState.update { it.copy(isSaving = false) }
            when (result) {
                is FileOperationResult.Success -> {
                    _uiState.update { it.copy(originalContent = state.content, hasUnsavedChanges = false) }
                    _uiEvents.emit(UiEvent.ShowToast("Uloženo"))
                    onSuccess()
                }
                is FileOperationResult.Error -> {
                    _uiEvents.emit(UiEvent.ShowToast(result.message))
                }
            }
        }
    }
}
