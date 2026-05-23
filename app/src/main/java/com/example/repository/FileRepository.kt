package com.example.repository

import com.example.model.FileModel
import com.example.model.FileOperationResult

interface FileRepository {
    fun getRootPath(): String
    suspend fun listFiles(path: String): Result<List<FileModel>>
    suspend fun createFolder(parentPath: String, name: String): FileOperationResult
    suspend fun delete(path: String): FileOperationResult
    suspend fun rename(path: String, newName: String): FileOperationResult
    suspend fun copy(sourcePath: String, destinationParentPath: String): FileOperationResult
    suspend fun move(sourcePath: String, destinationParentPath: String): FileOperationResult
}
