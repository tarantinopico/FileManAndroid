package com.example.repository

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import com.example.model.FileModel
import com.example.model.FileOperationResult
import com.example.model.StorageVolumeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileRepository {
    fun getRootPath(): String = Environment.getExternalStorageDirectory().absolutePath

    suspend fun listFiles(path: String): Result<List<FileModel>> = withContext(Dispatchers.IO) {
        try {
            val directory = File(path)
            if (!directory.exists() || !directory.isDirectory) {
                return@withContext Result.failure(Exception("Path is not a valid directory"))
            }

            val files = directory.listFiles() ?: return@withContext Result.success(emptyList())

            val formattedFiles = files.map { file ->
                FileModel(
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = file.isDirectory,
                    size = file.length(),
                    lastModified = file.lastModified()
                )
            }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))

            Result.success(formattedFiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isValidFileName(name: String): Boolean {
        if (name.isBlank()) return false
        val invalidChars = arrayOf("<", ">", ":", "\"", "/", "\\", "|", "?", "*")
        return invalidChars.none { name.contains(it) }
    }

    suspend fun createFile(parentPath: String, name: String, content: String? = null): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            if (!isValidFileName(name)) return@withContext FileOperationResult.Error("Neplatný název")
            val newFile = File(parentPath, name)
            if (newFile.exists()) return@withContext FileOperationResult.Error("Soubor již existuje")
            
            if (newFile.createNewFile()) {
                if (content != null) {
                    newFile.writeText(content)
                }
                FileOperationResult.Success
            } else {
                FileOperationResult.Error("Soubor nelze vytvořit")
            }
        } catch (e: Exception) {
            FileOperationResult.Error(e.message ?: "Chyba")
        }
    }

    suspend fun readFileContent(path: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists() || !file.isFile) {
                return@withContext Result.failure(Exception("Neplatný soubor"))
            }
            if (!file.canRead()) {
                return@withContext Result.failure(Exception("Nemáte oprávnění ke čtení tohoto souboru"))
            }
            if (file.length() > 2 * 1024 * 1024) { // 2MB limit for text editors to prevent severe OOM/lag
                return@withContext Result.failure(Exception("Soubor je příliš velký pro integrovaný editor (max 2MB)"))
            }
            Result.success(file.readText())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveFileContent(path: String, content: String): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists() || !file.isFile) {
                return@withContext FileOperationResult.Error("Neplatný soubor")
            }
            file.writeText(content)
            FileOperationResult.Success
        } catch (e: Exception) {
            FileOperationResult.Error(e.message ?: "Chyba při ukládání")
        }
    }

    suspend fun createFolder(parentPath: String, name: String): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            if (!isValidFileName(name)) return@withContext FileOperationResult.Error("Neplatný název")
            val newFolder = File(parentPath, name)
            if (newFolder.exists()) return@withContext FileOperationResult.Error("Složka již existuje")
            if (newFolder.mkdirs()) FileOperationResult.Success
            else FileOperationResult.Error("Složku nelze vytvořit")
        } catch (e: Exception) {
            FileOperationResult.Error(e.message ?: "Chyba")
        }
    }

    suspend fun deleteFile(path: String): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists()) return@withContext FileOperationResult.Error("Soubor neexistuje")
            if (file.deleteRecursively()) FileOperationResult.Success
            else FileOperationResult.Error("Nelze smazat")
        } catch (e: Exception) {
            FileOperationResult.Error(e.message ?: "Chyba")
        }
    }

    suspend fun renameFile(path: String, newName: String): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            if (!isValidFileName(newName)) return@withContext FileOperationResult.Error("Neplatný název")
            val file = File(path)
            if (!file.exists()) return@withContext FileOperationResult.Error("Soubor neexistuje")
            val newFile = File(file.parent, newName)
            if (newFile.exists()) return@withContext FileOperationResult.Error("Název již existuje")
            if (file.renameTo(newFile)) FileOperationResult.Success
            else FileOperationResult.Error("Nelze přejmenovat")
        } catch (e: Exception) {
            FileOperationResult.Error(e.message ?: "Chyba")
        }
    }

    suspend fun copyFile(sourcePath: String, destPath: String): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            val source = File(sourcePath)
            val destDir = File(destPath)
            var dest = File(destDir, source.name)
            
            var counter = 1
            while (dest.exists()) {
                val nameWithoutExtension = source.nameWithoutExtension
                val extension = source.extension
                val newName = if (extension.isNotEmpty()) "$nameWithoutExtension ($counter).$extension" else "$nameWithoutExtension ($counter)"
                dest = File(destDir, newName)
                counter++
            }
            
            source.copyRecursively(dest)
            FileOperationResult.Success
        } catch (e: Exception) {
            FileOperationResult.Error(e.message ?: "Chyba")
        }
    }

    suspend fun moveFile(sourcePath: String, destPath: String): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            val source = File(sourcePath)
            val destDir = File(destPath)
            var dest = File(destDir, source.name)
            
            if (source.parentFile?.absolutePath == destDir.absolutePath) {
                return@withContext FileOperationResult.Error("Cílová složka je stejná jako zdrojová")
            }
            
            var counter = 1
            while (dest.exists()) {
                val nameWithoutExtension = source.nameWithoutExtension
                val extension = source.extension
                val newName = if (extension.isNotEmpty()) "$nameWithoutExtension ($counter).$extension" else "$nameWithoutExtension ($counter)"
                dest = File(destDir, newName)
                counter++
            }
            
            if (source.renameTo(dest)) {
                FileOperationResult.Success
            } else {
                source.copyRecursively(dest)
                source.deleteRecursively()
                FileOperationResult.Success
            }
        } catch (e: Exception) {
            FileOperationResult.Error(e.message ?: "Chyba")
        }
    }
}

class StorageRepository(private val context: Context) {
    fun getStorageVolumes(): List<StorageVolumeModel> {
        val volumes = mutableListOf<StorageVolumeModel>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            storageManager.storageVolumes.forEach { volume ->
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
        return volumes
    }
}
