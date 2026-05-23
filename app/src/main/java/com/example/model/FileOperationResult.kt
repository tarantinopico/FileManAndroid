package com.example.model

sealed interface FileOperationResult {
    data object Success : FileOperationResult
    data class Error(val message: String, val exception: Exception? = null) : FileOperationResult
}
