package com.example.model

data class StorageVolumeModel(
    val id: String, // mostly path or UUID
    val name: String,
    val path: String,
    val isPrimary: Boolean,
    val isRemovable: Boolean
)
