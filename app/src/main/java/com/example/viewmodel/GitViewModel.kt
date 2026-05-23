package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.FileOperationResult
import com.example.model.GitAuthSettings
import com.example.model.GitRepoStatus
import com.example.repository.GitRepository
import com.example.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GitUiState(
    val repoStatus: GitRepoStatus = GitRepoStatus(isRepo = false),
    val isLoading: Boolean = false,
    val isActionLoading: Boolean = false,
    val actionMessage: String? = null,
    val authSettings: GitAuthSettings = GitAuthSettings(),
    val connectionTestResult: Boolean? = null,
    val isTestingConnection: Boolean = false
)

class GitViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)
    private val gitRepository = GitRepository(settingsRepository)

    private val _uiState = MutableStateFlow(GitUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            settingsRepository.gitAuthSettings.collect { auth ->
                _uiState.update { it.copy(authSettings = auth) }
            }
        }
    }

    fun loadGitStatus(path: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val status = gitRepository.getRepoStatus(path)
            _uiState.update { it.copy(repoStatus = status, isLoading = false) }
        }
    }

    fun initRepository(path: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            val result = gitRepository.initRepo(path)
            handleResult(result, path, "Repozitář úspěšně inicializován")
        }
    }

    fun addAll(path: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            val root = _uiState.value.repoStatus.rootPath ?: path
            val result = gitRepository.addAll(root)
            handleResult(result, path, "Všechny změny přidány do stage")
        }
    }

    fun addFile(repoPath: String, filePath: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            val result = gitRepository.addFile(repoPath, filePath)
            handleResult(result, repoPath, "Soubor přidán do stage")
        }
    }

    fun commit(path: String, message: String) {
        viewModelScope.launch {
            if (message.isBlank()) {
                _uiEvents.emit(UiEvent.ShowToast("Commit zpráva nesmí být prázdná"))
                return@launch
            }
            _uiState.update { it.copy(isActionLoading = true) }
            val root = _uiState.value.repoStatus.rootPath ?: path
            val result = gitRepository.commit(root, message)
            handleResult(result, path, "Commit úspěšný")
        }
    }

    fun push(path: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            val root = _uiState.value.repoStatus.rootPath ?: path
            val result = gitRepository.push(root)
            handleResult(result, path, "Push úspěšný")
        }
    }

    fun pull(path: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            val root = _uiState.value.repoStatus.rootPath ?: path
            val result = gitRepository.pull(root)
            handleResult(result, path, "Pull úspěšný")
        }
    }

    fun updateAuthSettings(settings: GitAuthSettings, token: String?) {
        viewModelScope.launch {
            settingsRepository.updateGitAuthSettings(settings, token)
            _uiEvents.emit(UiEvent.ShowToast("Git konfigurace uložena"))
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            val auth = _uiState.value.authSettings
            if (auth.providerUrl.isBlank()) {
                _uiEvents.emit(UiEvent.ShowToast("Chybí provider URL"))
                return@launch
            }
            
            _uiState.update { it.copy(isTestingConnection = true, connectionTestResult = null) }
            val success = gitRepository.testConnection(auth.providerUrl)
            _uiState.update { it.copy(isTestingConnection = false, connectionTestResult = success) }
            
            val msg = if (success) "Připojení k remote je úspěšné" else "Připojení k remote selhalo. Zkontrolujte token a připojení"
            _uiEvents.emit(UiEvent.ShowToast(msg))
        }
    }

    private suspend fun handleResult(result: FileOperationResult, path: String, successMsg: String) {
        _uiState.update { it.copy(isActionLoading = false) }
        when (result) {
            is FileOperationResult.Success -> {
                loadGitStatus(path)
                _uiEvents.emit(UiEvent.ShowToast(successMsg))
            }
            is FileOperationResult.Error -> {
                _uiEvents.emit(UiEvent.ShowToast(result.message))
            }
        }
    }
}
