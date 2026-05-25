package com.example.repository

import com.example.model.FileModel
import com.example.model.FileOperationResult
import com.example.model.RemoteServerModel
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FtpRepository {
    private val clients = mutableMapOf<String, FTPClient>()

    @Synchronized
    private fun getClient(server: RemoteServerModel): FTPClient {
        val key = server.id
        var client = clients[key]
        if (client != null && client.isConnected && client.sendNoOp()) {
            return client
        }
        
        client = FTPClient()
        client.connect(server.host, server.port)
        val reply = client.replyCode
        if (!FTPReply.isPositiveCompletion(reply)) {
            client.disconnect()
            throw Exception("FTP server refused connection.")
        }
        
        if (!client.login(server.username, server.passwordOrKeyPath ?: "")) {
            client.disconnect()
            throw Exception("FTP login failed.")
        }
        
        client.enterLocalPassiveMode()
        client.setFileType(FTP.BINARY_FILE_TYPE)
        
        clients[key] = client
        return client
    }

    suspend fun testConnection(server: RemoteServerModel): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val client = getClient(server)
            client.printWorkingDirectory()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listFiles(server: RemoteServerModel, path: String): Result<List<FileModel>> = withContext(Dispatchers.IO) {
        try {
            val client = getClient(server)
            val files = mutableListOf<FileModel>()
            
            val remotePath = if (path.substringAfter("ftp://${server.id}", "").isEmpty()) server.remotePath else path.substringAfter("ftp://${server.id}")
            val actualPath = if (remotePath.isEmpty()) "/" else remotePath

            val ftpFiles = client.listFiles(actualPath)
            ftpFiles?.forEach { file ->
                if (file.name == "." || file.name == "..") return@forEach
                
                val filePath = if (actualPath.endsWith("/")) actualPath + file.name else actualPath + "/" + file.name
                
                files.add(
                    FileModel(
                        name = file.name,
                        path = "ftp://${server.id}$filePath",
                        isDirectory = file.isDirectory,
                        size = file.size,
                        lastModified = file.timestamp?.timeInMillis ?: 0L
                    )
                )
            }
            Result.success(files.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFile(server: RemoteServerModel, path: String, isDirectory: Boolean): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            val client = getClient(server)
            val actualPath = path.substringAfter("ftp://${server.id}")
            val success = if (isDirectory) {
                client.removeDirectory(actualPath)
            } else {
                client.deleteFile(actualPath)
            }
            if (success) FileOperationResult.Success else FileOperationResult.Error("Nelze smazat položku na FTP")
        } catch (e: Exception) {
            FileOperationResult.Error(e.message ?: "Chyba při mazání na FTP")
        }
    }

    suspend fun createFolder(server: RemoteServerModel, parentPath: String, name: String): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            val client = getClient(server)
            val actualParent = parentPath.substringAfter("ftp://${server.id}")
            val newPath = if (actualParent.endsWith("/")) actualParent + name else "$actualParent/$name"
            if (client.makeDirectory(newPath)) FileOperationResult.Success
            else FileOperationResult.Error("Složku nelze vytvořit na FTP")
        } catch (e: Exception) {
            FileOperationResult.Error(e.message ?: "Chyba při vytváření složky na FTP")
        }
    }

    suspend fun renameFile(server: RemoteServerModel, path: String, newName: String): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            val client = getClient(server)
            val actualPath = path.substringAfter("ftp://${server.id}")
            val parentPath = actualPath.substringBeforeLast("/")
            val newPath = if (parentPath.isEmpty()) "/$newName" else "$parentPath/$newName"
            if (client.rename(actualPath, newPath)) FileOperationResult.Success
            else FileOperationResult.Error("Přemenování selhalo na FTP")
        } catch (e: Exception) {
            FileOperationResult.Error(e.message ?: "Chyba při přejmenování na FTP")
        }
    }

    fun disconnect(serverId: String) {
        try {
            clients[serverId]?.disconnect()
            clients.remove(serverId)
        } catch (_: Exception) {}
    }
}
