package com.example.model

data class FileModel(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val tags: List<TagModel> = emptyList()
)

data class TagModel(
    val id: String,
    val name: String,
    val colorArgb: Int
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
    val isRemovable: Boolean,
    val totalSpace: Long = 0L,
    val freeSpace: Long = 0L
)

enum class FavoriteIcon {
    FOLDER, STAR, PROJECT, DOWNLOAD, IMAGE, DOCUMENT, PIN
}

data class FavoriteModel(
    val path: String,
    val name: String,
    val isAvailable: Boolean = true,
    val icon: FavoriteIcon = FavoriteIcon.FOLDER,
    val isPinned: Boolean = false
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
    val language: SyntaxLanguage,
    val tagColorArgb: Int? = null
)

data class EditorSettings(
    val wordWrap: Boolean = false,
    val showLineNumbers: Boolean = true,
    val syntaxHighlightEnabled: Boolean = true,
    val activeLineHighlightEnabled: Boolean = true,
    val editorToolbarEnabled: Boolean = true,
    val autosaveEnabled: Boolean = false,
    val largeFileSafeModeEnabled: Boolean = true,
    val keyboardFriendlyBehavior: Boolean = true
)

enum class ServerType {
    FTP, SFTP
}

data class RemoteServerModel(
    val id: String,
    val type: ServerType,
    val name: String,
    val host: String,
    val port: Int,
    val username: String,
    val passwordOrKeyPath: String?,
    val remotePath: String = "/",
    val isAvailable: Boolean = true,
    val isConnected: Boolean = false
)

data class AppPreferences(
    val drawerEnabled: Boolean = true,
    val showFavorites: Boolean = true,
    val showPinned: Boolean = true,
    val showRecents: Boolean = true,
    val openLastLocationOnStartup: Boolean = false,
    val lastOpenedLocation: String? = null,
    val imageThumbnailsEnabled: Boolean = true,
    val showFileBadges: Boolean = true,
    val badgeColorEnabled: Boolean = true,
    val confirmDeletions: Boolean = true,
    val multiSelectEnabled: Boolean = true,
    val detailPanelsEnabled: Boolean = true,
    val showFreeSpace: Boolean = true,
    val compactListMode: Boolean = false
)

enum class GitFileStatusType {
    UNTRACKED, MODIFIED, ADDED, REMOVED, CONFLICTING, UNCOMMITTED
}

data class GitFileStatus(
    val path: String,
    val status: GitFileStatusType
)

data class GitRepoStatus(
    val isRepo: Boolean,
    val branchName: String? = null,
    val hasUncommittedChanges: Boolean = false,
    val fileStatuses: List<GitFileStatus> = emptyList(),
    val rootPath: String? = null,
    val remotes: List<String> = emptyList()
)

data class GitAuthSettings(
    val providerUrl: String = "https://github.com",
    val username: String = "",
    val tokenSet: Boolean = false,
    val authorName: String = "",
    val authorEmail: String = ""
)
