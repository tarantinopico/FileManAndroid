package com.example.repository

import android.os.Environment
import com.example.model.FileModel
import com.example.model.FileOperationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class FileRepositoryImpl : FileRepository {

    override fun getRootPath(): String = Environment.getExternalStorageDirectory().absolutePath

    override suspend fun listFiles(path: String): Result<List<FileModel>> = withContext(Dispatchers.IO) {
        try {
            val dir = File(path)
            if (!dir.exists() || !dir.isDirectory) {
                return@withContext Result.failure(IllegalArgumentException("Cesta neexistuje nebo není složka."))
            }
            if (!dir.canRead()) {
                return@withContext Result.failure(SecurityException("Odepřen přístup k čtení složky."))
            }
            val files = dir.listFiles()
            if (files == null) {
                return@withContext Result.failure(IOException("Nelze načíst obsah složky."))
            }

            val models = files.map { file ->
                FileModel(
                    id = file.absolutePath,
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = file.isDirectory,
                    size = if (file.isDirectory) 0 else file.length(),
                    lastModified = file.lastModified()
                )
            }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))

            Result.success(models)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createFolder(parentPath: String, name: String): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            val parentFile = File(parentPath)
            if (!parentFile.exists() || !parentFile.canWrite()) {
                return@withContext FileOperationResult.Error("Nadřazená složka není přístupná pro zápis.")
            }
            val newFolder = File(parentFile, name)
            if (newFolder.exists()) {
                return@withContext FileOperationResult.Error("Složka s tímto názvem již existuje.")
            }
            if (newFolder.mkdir()) {
                FileOperationResult.Success
            } else {
                FileOperationResult.Error("Složku se nepodařilo vytvořit.")
            }
        } catch (e: Exception) {
            FileOperationResult.Error("Chyba při vytváření složky: ${e.message}", e)
        }
    }

    override suspend fun delete(path: String): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            if (isProtected(path)) {
                 return@withContext FileOperationResult.Error("Nelze odstranit kořenovou složku.")
            }
            val file = File(path)
            if (!file.exists()) {
                 return@withContext FileOperationResult.Error("Soubor nebo složka neexistuje.")
            }
            if (deleteRecursivelySafe(file)) {
                 FileOperationResult.Success
            } else {
                 FileOperationResult.Error("Některé položky se nepodařilo smazat.")
            }
        } catch (e: Exception) {
            FileOperationResult.Error("Chyba při mazání: ${e.message}", e)
        }
    }

    override suspend fun rename(path: String, newName: String): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            if (isProtected(path)) {
                 return@withContext FileOperationResult.Error("Nelze přejmenovat kořenovou složku.")
            }
            val file = File(path)
            if (!file.exists()) {
                 return@withContext FileOperationResult.Error("Soubor nebo složka neexistuje.")
            }
            val newFile = File(file.parentFile, newName)
            if (newFile.exists()) {
                 return@withContext FileOperationResult.Error("Položka s tímto názvem již existuje.")
            }
            if (file.renameTo(newFile)) {
                 FileOperationResult.Success
            } else {
                 FileOperationResult.Error("Nepodařilo se přejmenovat položku.")
            }
        } catch (e: Exception) {
             FileOperationResult.Error("Chyba při přejmenování: ${e.message}", e)
        }
    }

    override suspend fun copy(sourcePath: String, destinationParentPath: String): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(sourcePath)
            val destParentFile = File(destinationParentPath)
            
            if (!sourceFile.exists()) return@withContext FileOperationResult.Error("Zdrojový soubor neexistuje.")
            if (!destParentFile.exists() || !destParentFile.canWrite()) return@withContext FileOperationResult.Error("Cílová složka není přístupná pro zápis.")
            if (destinationParentPath.startsWith(sourcePath)) return@withContext FileOperationResult.Error("Nelze kopírovat složku do sebe samé.")

            val targetFile = getUniqueFile(destParentFile, sourceFile.name)
            
            copyRecursivelySafe(sourceFile, targetFile)
            FileOperationResult.Success
        } catch (e: IOException) {
            val message = if (e.message?.contains("ENOSPC") == true) "Nedostatek místa na disku." else "Chyba IO při kopírování: ${e.message}"
            FileOperationResult.Error(message, e)
        } catch (e: Exception) {
            FileOperationResult.Error("Neznámá chyba při kopírování: ${e.message}", e)
        }
    }

    override suspend fun move(sourcePath: String, destinationParentPath: String): FileOperationResult = withContext(Dispatchers.IO) {
        try {
             if (isProtected(sourcePath)) {
                 return@withContext FileOperationResult.Error("Nelze přesunout systémově chráněnou složku.")
             }
             val sourceFile = File(sourcePath)
             val destParentFile = File(destinationParentPath)
            
             if (!sourceFile.exists()) return@withContext FileOperationResult.Error("Zdrojový soubor neexistuje.")
             if (!destParentFile.exists() || !destParentFile.canWrite()) return@withContext FileOperationResult.Error("Cílová složka není přístupná pro zápis.")
             if (destinationParentPath.startsWith(sourcePath)) return@withContext FileOperationResult.Error("Nelze přesunout složku do sebe samé.")

             val targetFile = getUniqueFile(destParentFile, sourceFile.name)

             // Fast standard rename
             if (sourceFile.renameTo(targetFile)) {
                 return@withContext FileOperationResult.Success
             }
             
             // Fallback copy & delete (e.g. across mount points)
             copyRecursivelySafe(sourceFile, targetFile)
             deleteRecursivelySafe(sourceFile)
             FileOperationResult.Success
        } catch (e: IOException) {
             val message = if (e.message?.contains("ENOSPC") == true) "Nedostatek místa na disku." else "Chyba IO při přesouvání: ${e.message}"
             FileOperationResult.Error(message, e)
        } catch (e: Exception) {
             FileOperationResult.Error("Neznámá chyba při přesouvání: ${e.message}", e)
        }
    }

    private fun isProtected(path: String): Boolean {
        val storageRoot = Environment.getExternalStorageDirectory().absolutePath
        return path == storageRoot || path == "/" || !path.startsWith(storageRoot)
    }

    private fun getUniqueFile(parentDir: File, originalName: String): File {
        var destFile = File(parentDir, originalName)
        if (!destFile.exists()) return destFile

        val nameWithoutExt = destFile.nameWithoutExtension
        val ext = destFile.extension.let { if (it.isNotEmpty()) ".$it" else "" }
        var counter = 1

        while (destFile.exists()) {
            destFile = File(parentDir, "$nameWithoutExt($counter)$ext")
            counter++
        }
        return destFile
    }

    private fun deleteRecursivelySafe(file: File): Boolean {
        var success = true
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                success = success && deleteRecursivelySafe(child)
            }
        }
        return success && file.delete()
    }

    @Throws(IOException::class)
    private fun copyRecursivelySafe(source: File, target: File) {
        if (source.isDirectory) {
            if (!target.mkdirs() && !target.exists()) {
                 throw IOException("Nepodařilo se vytvořit cílovou složku.")
            }
            source.listFiles()?.forEach { child ->
                copyRecursivelySafe(child, File(target, child.name))
            }
        } else {
            FileInputStream(source).use { ins ->
                FileOutputStream(target).use { outs ->
                    val buffer = ByteArray(8192)
                    var length: Int
                    while (ins.read(buffer).also { length = it } > 0) {
                        outs.write(buffer, 0, length)
                    }
                }
            }
        }
    }
}
