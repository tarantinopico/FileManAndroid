package com.example.repository

import com.example.model.FileModel
import com.example.model.FileOperationResult
import com.example.model.RemoteServerModel
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.Properties

class SftpRepository {
    
    private val sessions = mutableMapOf<String, Session>()
    private val channels = mutableMapOf<String, ChannelSftp>()

    @Synchronized
    private fun getChannel(server: RemoteServerModel): ChannelSftp {
        val key = server.id
        if (channels[key]?.isConnected == true) {
            return channels[key]!!
        }
        
        val jsch = JSch()
        if (server.passwordOrKeyPath != null && server.passwordOrKeyPath.startsWith("/")) {
            jsch.addIdentity(server.passwordOrKeyPath)
        }
        
        val session = jsch.getSession(server.username, server.host, server.port)
        if (server.passwordOrKeyPath != null && !server.passwordOrKeyPath.startsWith("/")) {
            session.setPassword(server.passwordOrKeyPath)
        }
        
        val config = Properties()
        config.put("StrictHostKeyChecking", "no")
        session.setConfig(config)
        session.connect(15000)
        
        val channel = session.openChannel("sftp") as ChannelSftp
        channel.connect(15000)
        
        sessions[key] = session
        channels[key] = channel
        
        return channel
    }

    suspend fun testConnection(server: RemoteServerModel): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val channel = getChannel(server)
            channel.pwd()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listFiles(server: RemoteServerModel, path: String): Result<List<FileModel>> = withContext(Dispatchers.IO) {
        try {
            val channel = getChannel(server)
            val files = mutableListOf<FileModel>()
            
            val remotePath = if (path.substringAfter("sftp://${server.id}", "").isEmpty()) server.remotePath else path.substringAfter("sftp://${server.id}")
            val actualPath = if (remotePath.isEmpty()) "/" else remotePath

            val list = channel.ls(actualPath)
            for (item in list) {
                val entry = item as ChannelSftp.LsEntry
                if (entry.filename == "." || entry.filename == "..") continue
                
                val isDir = entry.attrs.isDir
                val filePath = if (actualPath.endsWith("/")) actualPath + entry.filename else actualPath + "/" + entry.filename
                
                files.add(
                    FileModel(
                        name = entry.filename,
                        path = "sftp://${server.id}$filePath",
                        isDirectory = isDir,
                        size = entry.attrs.size,
                        lastModified = entry.attrs.mTime * 1000L
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
            val channel = getChannel(server)
            val actualPath = path.substringAfter("sftp://${server.id}")
            if (isDirectory) {
                channel.rmdir(actualPath)
            } else {
                channel.rm(actualPath)
            }
            FileOperationResult.Success
        } catch (e: Exception) {
            FileOperationResult.Error(e.message ?: "Chyba při mazání na SFTP")
        }
    }
    
    suspend fun createFolder(server: RemoteServerModel, parentPath: String, name: String): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            val channel = getChannel(server)
            val actualParent = parentPath.substringAfter("sftp://${server.id}")
            val newPath = if (actualParent.endsWith("/")) actualParent + name else "$actualParent/$name"
            channel.mkdir(newPath)
            FileOperationResult.Success
        } catch (e: Exception) {
            FileOperationResult.Error(e.message ?: "Chyba při vytváření složky na SFTP")
        }
    }

    suspend fun renameFile(server: RemoteServerModel, path: String, newName: String): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            val channel = getChannel(server)
            val actualPath = path.substringAfter("sftp://${server.id}")
            val parentPath = actualPath.substringBeforeLast("/")
            val newPath = if (parentPath.isEmpty()) "/$newName" else "$parentPath/$newName"
            channel.rename(actualPath, newPath)
            FileOperationResult.Success
        } catch (e: Exception) {
            FileOperationResult.Error(e.message ?: "Chyba při přejmenování na SFTP")
        }
    }

    fun disconnect(serverId: String) {
        try {
            channels[serverId]?.disconnect()
            sessions[serverId]?.disconnect()
            channels.remove(serverId)
            sessions.remove(serverId)
        } catch (_: Exception) {}
    }
}
