package com.example.model

data class FileModel(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long
)

data class BreadcrumbModel(
    val name: String,
    val path: String
)

sealed class FileOperationResult {
    object Success : FileOperationResult()
    data class Error(val message: String) : FileOperationResult()
}

data class StorageVolumeModel(
    val id: String,
    val name: String,
    val path: String,
    val isPrimary: Boolean,
    val isRemovable: Boolean
)

data class FavoriteModel(
    val path: String,
    val name: String,
    val isAvailable: Boolean = true
)

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class UiDensity {
    COMPACT, NORMAL, LARGE, EXTRA_LARGE
}

enum class SyntaxLanguage {
    KOTLIN, PYTHON, XML, JSON, YAML, GRADLE, CSS, MARKDOWN, PLAIN
}

data class SyntaxMapping(
    val extension: String,
    val language: SyntaxLanguage
)

data class EditorSettings(
    val wordWrap: Boolean = false,
    val showLineNumbers: Boolean = true,
    val syntaxHighlightEnabled: Boolean = true
)
