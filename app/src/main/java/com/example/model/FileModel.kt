package com.example.model

data class FileModel(
    val id: String,
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long
)
