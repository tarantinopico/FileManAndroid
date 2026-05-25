package com.example.repository

import com.example.model.FileModel
import com.example.model.FileOperationResult
import com.example.model.ServerType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

class VirtualFileSystem(
    private val localRepo: FileRepository,
    private val sftpRepo: SftpRepository,
    private val ftpRepo: FtpRepository,
    private val serversRepo: RemoteServersRepository
) {

    private fun getServerForPath(path: String): com.example.model.RemoteServerModel? {
        if (path.startsWith("sftp://")) {
            val id = path.substringAfter("sftp://").substringBefore("/")
            return serversRepo.getServer(id)
        }
        if (path.startsWith("ftp://")) {
            val id = path.substringAfter("ftp://").substringBefore("/")
            return serversRepo.getServer(id)
        }
        return null
    }

    suspend fun listFiles(path: String): Result<List<FileModel>> {
        val server = getServerForPath(path)
        return if (server != null) {
            when (server.type) {
                ServerType.SFTP -> sftpRepo.listFiles(server, path)
                ServerType.FTP -> ftpRepo.listFiles(server, path)
            }
        } else {
            localRepo.listFiles(path)
        }
    }

    suspend fun deleteFile(path: String, isDirectory: Boolean): FileOperationResult {
        val server = getServerForPath(path)
        return if (server != null) {
            when (server.type) {
                ServerType.SFTP -> sftpRepo.deleteFile(server, path, isDirectory)
                ServerType.FTP -> ftpRepo.deleteFile(server, path, isDirectory)
            }
        } else {
            localRepo.deleteFile(path)
        }
    }

    suspend fun createFolder(parentPath: String, name: String): FileOperationResult {
        val server = getServerForPath(parentPath)
        return if (server != null) {
            when (server.type) {
                ServerType.SFTP -> sftpRepo.createFolder(server, parentPath, name)
                ServerType.FTP -> ftpRepo.createFolder(server, parentPath, name)
            }
        } else {
            localRepo.createFolder(parentPath, name)
        }
    }

    suspend fun renameFile(path: String, newName: String): FileOperationResult {
        val server = getServerForPath(path)
        return if (server != null) {
            when (server.type) {
                ServerType.SFTP -> sftpRepo.renameFile(server, path, newName)
                ServerType.FTP -> ftpRepo.renameFile(server, path, newName)
            }
        } else {
            localRepo.renameFile(path, newName)
        }
    }

    // Creating files is typically only local for now, but we can return error or unsupported for remote
    suspend fun createFile(parentPath: String, name: String, content: String? = null): FileOperationResult {
        val server = getServerForPath(parentPath)
        if (server != null) return FileOperationResult.Error("Vytváření textových souborů je podporováno pouze lokálně")
        return localRepo.createFile(parentPath, name, content)
    }

    // copyFile handles local-to-local for now. Later I can add stream copying.
    suspend fun copyFile(sourceModel: FileModel, destPath: String): FileOperationResult {
        val srcServer = getServerForPath(sourceModel.path)
        val dstServer = getServerForPath(destPath)
        
        if (srcServer == null && dstServer == null) {
            return localRepo.copyFile(sourceModel.path, destPath)
        }
        
        // Complex stream copy will go here if needed, but for now we error out if not implemented
        return FileOperationResult.Error("Zkopírovat na vzdálený server není plně implementováno")
    }

    suspend fun moveFile(sourceModel: FileModel, destPath: String): FileOperationResult {
        val srcServer = getServerForPath(sourceModel.path)
        val dstServer = getServerForPath(destPath)
        
        if (srcServer == null && dstServer == null) {
            return localRepo.moveFile(sourceModel.path, destPath)
        }
        
        return FileOperationResult.Error("Přesun na vzdálený server není plně implementován")
    }

    // Helper to get parent path properly
    fun getParentPath(path: String): String? {
        val server = getServerForPath(path)
        if (server != null) {
            val prefix = if (server.type == ServerType.SFTP) "sftp://${server.id}" else "ftp://${server.id}"
            val actualPath = path.substringAfter(prefix)
            if (actualPath.isEmpty() || actualPath == "/" || actualPath == server.remotePath) return null
            val parent = actualPath.substringBeforeLast("/")
            return if (parent.isEmpty()) prefix else "$prefix$parent"
        } else {
            return java.io.File(path).parent
        }
    }
}
