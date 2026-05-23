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

enum class FavoriteIcon {
    FOLDER, STAR, PROJECT, DOWNLOAD, IMAGE, DOCUMENT
}

data class FavoriteModel(
    val path: String,
    val name: String,
    val isAvailable: Boolean = true,
    val icon: FavoriteIcon = FavoriteIcon.FOLDER
)

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class UiDensity {
    COMPACT, NORMAL, LARGE, EXTRA_LARGE
}

enum class SortOption {
    NAME_ASC, NAME_DESC,
    SIZE_ASC, SIZE_DESC,
    DATE_ASC, DATE_DESC,
    TYPE_ASC, TYPE_DESC
}

data class FileSettings(
    val showHiddenFiles: Boolean = false,
    val showFileExtensions: Boolean = true,
    val sortOption: SortOption = SortOption.NAME_ASC
)

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
