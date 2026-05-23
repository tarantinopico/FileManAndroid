package com.example.ui.state

import com.example.model.FileModel

data class ClipboardState(
    val file: FileModel,
    val operation: ClipboardOperation
)

enum class ClipboardOperation {
    COPY, MOVE
}
