package com.example.ui.state

import com.example.model.BreadcrumbModel
import com.example.model.FileModel

data class FileManagerState(
    val currentPath: String = "",
    val parentPath: String? = null,
    val files: List<FileModel> = emptyList(),
    val breadcrumbs: List<BreadcrumbModel> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val clipboard: ClipboardState? = null
)
