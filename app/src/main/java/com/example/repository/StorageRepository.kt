package com.example.repository

import com.example.model.StorageVolumeModel
import kotlinx.coroutines.flow.Flow

interface StorageRepository {
    fun getStorageVolumes(): Flow<List<StorageVolumeModel>>
    fun checkPathAvailable(path: String): Boolean
}
