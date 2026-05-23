package com.example.repository

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import com.example.model.StorageVolumeModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

class StorageRepositoryImpl(private val context: Context) : StorageRepository {

    override fun getStorageVolumes(): Flow<List<StorageVolumeModel>> = flow {
        val volumes = mutableListOf<StorageVolumeModel>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            val storageVolumes = storageManager.storageVolumes
            
            storageVolumes.forEach { volume ->
                val path = volume.directory?.absolutePath
                if (path != null) {
                    volumes.add(
                        StorageVolumeModel(
                            id = volume.uuid ?: "primary",
                            name = if (volume.isPrimary) "Interní úložiště" else (volume.getDescription(context) ?: "Neznámé úložiště"),
                            path = path,
                            isPrimary = volume.isPrimary,
                            isRemovable = volume.isRemovable
                        )
                    )
                }
            }
        } else {
            // Fallback for completeness, though target is 11+
            volumes.add(
                StorageVolumeModel(
                    id = "primary",
                    name = "Interní úložiště",
                    path = Environment.getExternalStorageDirectory().absolutePath,
                    isPrimary = true,
                    isRemovable = false
                )
            )
        }
        
        emit(volumes)
    }

    override fun checkPathAvailable(path: String): Boolean {
        return File(path).exists()
    }
}
