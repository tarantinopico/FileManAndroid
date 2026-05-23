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

    suspend fun readFileContent(path: String, safeMode: Boolean = true): Result<String> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists() || !file.isFile) {
                return@withContext Result.failure(Exception("Neplatný soubor nebo soubor neexistuje"))
            }
            if (!file.canRead()) {
                return@withContext Result.failure(Exception("Nemáte oprávnění ke čtení tohoto souboru"))
            }
            val limit = if (safeMode) 2 * 1024 * 1024L else 10 * 1024 * 1024L
            if (file.length() > limit) {
                return@withContext Result.failure(Exception("Soubor je příliš velký (max ${if (safeMode) "2MB v bezpečném režimu" else "10MB"})"))
            }
            
            if (safeMode && file.length() > 0) {
                val input = file.inputStream()
                val bytes = ByteArray(minOf(1024, file.length().toInt()))
                val read = input.read(bytes)
                input.close()
                if (read > 0) {
                    var nullBytes = 0
                    for (i in 0 until read) {
                        if (bytes[i] == 0.toByte()) nullBytes++
                    }
                    if (nullBytes > 0) {
                        return@withContext Result.failure(Exception("Soubor obsahuje binární data a nelze jej otevřít jako text (Safe mode)"))
                    }
                }
            }

            val text = file.readText(Charsets.UTF_8)
            Result.success(text)
        } catch (e: java.nio.charset.MalformedInputException) {
            Result.failure(Exception("Soubor má neplatné kódování textu"))
        } catch (e: OutOfMemoryError) {
            Result.failure(Exception("Nedostatek paměti pro načtení souboru. Extrémně velký obsah!"))
        } catch (e: Exception) {
            Result.failure(Exception("Nelze přečíst soubor: ${e.message}"))
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

    suspend fun zipFiles(sourcePaths: List<String>, destZipDir: String, zipName: String): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            val validName = if (zipName.endsWith(".zip", ignoreCase = true)) zipName else "$zipName.zip"
            if (!isValidFileName(validName)) return@withContext FileOperationResult.Error("Neplatný název archivu")
            
            var dest = File(destZipDir, validName)
            var counter = 1
            while (dest.exists()) {
                val nameWithoutExtension = validName.substringBeforeLast(".")
                dest = File(destZipDir, "$nameWithoutExtension ($counter).zip")
                counter++
            }
            
            java.util.zip.ZipOutputStream(java.io.FileOutputStream(dest)).use { zout ->
                for (path in sourcePaths) {
                    val fileToZip = File(path)
                    if (fileToZip.exists()) {
                        zipFileOrFolder(fileToZip, fileToZip.name, zout)
                    }
                }
            }
            FileOperationResult.Success
        } catch (e: Exception) {
            FileOperationResult.Error(e.message ?: "Chyba při kompresi")
        }
    }

    private fun zipFileOrFolder(fileToZip: File, currentName: String, zout: java.util.zip.ZipOutputStream) {
        if (fileToZip.isHidden) return
        if (fileToZip.isDirectory) {
            val fileName = if (currentName.endsWith("/")) currentName else "$currentName/"
            zout.putNextEntry(java.util.zip.ZipEntry(fileName))
            zout.closeEntry()
            fileToZip.listFiles()?.forEach { child ->
                zipFileOrFolder(child, fileName + child.name, zout)
            }
            return
        }
        val fis = java.io.FileInputStream(fileToZip)
        val zipEntry = java.util.zip.ZipEntry(currentName)
        zout.putNextEntry(zipEntry)
        val bytes = ByteArray(4096)
        var length: Int
        while (fis.read(bytes).also { length = it } >= 0) {
            zout.write(bytes, 0, length)
        }
        fis.close()
    }

    suspend fun unzipFile(zipPath: String, destDir: String): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            val zipFile = File(zipPath)
            val destFolder = File(destDir, zipFile.nameWithoutExtension)
            if (!destFolder.exists()) destFolder.mkdirs()

            java.util.zip.ZipInputStream(java.io.FileInputStream(zipFile)).use { zis ->
                var zipEntry: java.util.zip.ZipEntry? = zis.nextEntry
                while (zipEntry != null) {
                    val newFile = File(destFolder, zipEntry.name)
                    
                    // Prevent path traversal
                    if (!newFile.canonicalPath.startsWith(destFolder.canonicalPath + File.separator)) {
                        return@withContext FileOperationResult.Error("Zip attempt path traversal: ${zipEntry.name}")
                    }
                    
                    if (zipEntry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        java.io.FileOutputStream(newFile).use { fos ->
                            val buffer = ByteArray(4096)
                            var len: Int
                            while (zis.read(buffer).also { len = it } > 0) {
                                fos.write(buffer, 0, len)
                            }
                        }
                    }
                    zipEntry = zis.nextEntry
                }
                zis.closeEntry()
            }
            FileOperationResult.Success
        } catch (e: Exception) {
            FileOperationResult.Error(e.message ?: "Chyba při extrakci")
        }
    }

    suspend fun encryptFiles(sourcePaths: List<String>, destDir: String, password: String): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            for (path in sourcePaths) {
                val source = File(path)
                if (!source.isFile) continue // Skip directories for now, or require them to be zipped first
                val dest = File(destDir, "${source.name}.enc")
                
                val salt = java.security.SecureRandom().generateSeed(16)
                val iv = java.security.SecureRandom().generateSeed(16)
                
                val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                val spec = javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt, 65536, 256)
                val tmp = factory.generateSecret(spec)
                val secret = javax.crypto.spec.SecretKeySpec(tmp.encoded, "AES")
                
                val cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secret, javax.crypto.spec.IvParameterSpec(iv))
                
                java.io.FileOutputStream(dest).use { fos ->
                    fos.write(salt)
                    fos.write(iv)
                    javax.crypto.CipherOutputStream(fos, cipher).use { cos ->
                        java.io.FileInputStream(source).use { fis ->
                            val buffer = ByteArray(4096)
                            var len: Int
                            while (fis.read(buffer).also { len = it } > 0) {
                                cos.write(buffer, 0, len)
                            }
                        }
                    }
                }
            }
            FileOperationResult.Success
        } catch (e: Exception) {
            FileOperationResult.Error(e.message ?: "Chyba při šifrování")
        }
    }

    suspend fun decryptFile(sourcePath: String, destDir: String, password: String): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            val source = File(sourcePath)
            var destName = source.name
            if (destName.endsWith(".enc")) destName = destName.removeSuffix(".enc")
            else destName = "$destName.dec"
            
            var dest = File(destDir, destName)
            var counter = 1
            while (dest.exists()) {
                val nameWithoutExtension = destName.substringBeforeLast(".")
                val ext = if (destName.contains(".")) ".${destName.substringAfterLast(".")}" else ""
                dest = File(destDir, "$nameWithoutExtension ($counter)$ext")
                counter++
            }

            java.io.FileInputStream(source).use { fis ->
                val salt = ByteArray(16)
                val iv = ByteArray(16)
                if (fis.read(salt) != 16 || fis.read(iv) != 16) {
                    return@withContext FileOperationResult.Error("Neplatný šifrovaný soubor")
                }

                val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                val spec = javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt, 65536, 256)
                val tmp = factory.generateSecret(spec)
                val secret = javax.crypto.spec.SecretKeySpec(tmp.encoded, "AES")

                val cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(javax.crypto.Cipher.DECRYPT_MODE, secret, javax.crypto.spec.IvParameterSpec(iv))

                javax.crypto.CipherInputStream(fis, cipher).use { cis ->
                    java.io.FileOutputStream(dest).use { fos ->
                        val buffer = ByteArray(4096)
                        var len: Int
                        try {
                            while (cis.read(buffer).also { len = it } > 0) {
                                fos.write(buffer, 0, len)
                            }
                        } catch (e: javax.crypto.BadPaddingException) {
                            dest.delete()
                            return@withContext FileOperationResult.Error("Nesprávné heslo")
                        } catch (e: java.io.IOException) {
                            dest.delete()
                            return@withContext FileOperationResult.Error("Nesprávné heslo nebo poškozená data")
                        }
                    }
                }
            }
            FileOperationResult.Success
        } catch (e: Exception) {
            FileOperationResult.Error(e.message ?: "Chyba při dešifrování")
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
                            isRemovable = volume.isRemovable,
                            totalSpace = volume.directory?.totalSpace ?: 0L,
                            freeSpace = volume.directory?.freeSpace ?: 0L
                        )
                    )
                }
            }
        } else {
            val defaultDir = Environment.getExternalStorageDirectory()
            volumes.add(
                StorageVolumeModel(
                    id = "primary",
                    name = "Interní úložiště",
                    path = defaultDir.absolutePath,
                    isPrimary = true,
                    isRemovable = false,
                    totalSpace = defaultDir.totalSpace,
                    freeSpace = defaultDir.freeSpace
                )
            )
        }
        return volumes
    }
}
