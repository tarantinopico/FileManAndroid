package com.example.repository

import com.example.model.FileOperationResult
import com.example.model.GitAuthSettings
import com.example.model.GitFileStatus
import com.example.model.GitFileStatusType
import com.example.model.GitRepoStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.lib.RepositoryCache
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.util.FS
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class GitRepository(private val settingsRepository: SettingsRepository) {

    private suspend fun getCredentialsProvider(): UsernamePasswordCredentialsProvider? {
        val auth = settingsRepository.gitAuthSettings.first()
        if (!auth.tokenSet || auth.username.isEmpty()) return null
        
        // Use token from secure storage. Usually we pass token as password, username can be token or real username.
        // For github HTTPS, username does not matter much when token is used, but we provide both.
        val token = settingsRepository.getGitToken() ?: return null
        return UsernamePasswordCredentialsProvider(auth.username, token)
    }

    suspend fun getRepoStatus(path: String): GitRepoStatus = withContext(Dispatchers.IO) {
        try {
            val directory = File(path)
            val builder = FileRepositoryBuilder()
            val repo = builder.findGitDir(directory).build()
            
            val git = Git(repo)
            val status = git.status().call()
            val branch = repo.branch
            val remotes = repo.config.getSubsections("remote").toList()
            val rootPath = repo.directory?.parentFile?.absolutePath

            val fileStatuses = mutableListOf<GitFileStatus>()
            
            status.untracked.forEach { fileStatuses.add(GitFileStatus(it, GitFileStatusType.UNTRACKED)) }
            status.modified.forEach { fileStatuses.add(GitFileStatus(it, GitFileStatusType.MODIFIED)) }
            status.added.forEach { fileStatuses.add(GitFileStatus(it, GitFileStatusType.ADDED)) }
            status.removed.forEach { fileStatuses.add(GitFileStatus(it, GitFileStatusType.REMOVED)) }
            status.missing.forEach { fileStatuses.add(GitFileStatus(it, GitFileStatusType.REMOVED)) }
            status.conflicting.forEach { fileStatuses.add(GitFileStatus(it, GitFileStatusType.CONFLICTING)) }
            
            git.close()
            repo.close()

            GitRepoStatus(
                isRepo = true,
                branchName = branch,
                hasUncommittedChanges = status.hasUncommittedChanges(),
                fileStatuses = fileStatuses,
                rootPath = rootPath,
                remotes = remotes
            )
        } catch (e: Exception) {
            GitRepoStatus(isRepo = false)
        }
    }

    suspend fun initRepo(path: String): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            val directory = File(path)
            if (!directory.exists() || !directory.isDirectory) {
                return@withContext FileOperationResult.Error("Neplatná složka pro inicializaci gitu")
            }
            val git = Git.init().setDirectory(directory).call()
            git.close()
            FileOperationResult.Success
        } catch (e: Exception) {
            FileOperationResult.Error(e.message ?: "Chyba při inicializaci gitu")
        }
    }

    suspend fun addAll(path: String): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            val repo = FileRepositoryBuilder().findGitDir(File(path)).build()
            val git = Git(repo)
            git.add().addFilepattern(".").call()
            git.close()
            repo.close()
            FileOperationResult.Success
        } catch (e: Exception) {
            FileOperationResult.Error(e.message ?: "Chyba při add")
        }
    }

    suspend fun addFile(repoPath: String, filePath: String): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            val repo = FileRepositoryBuilder().findGitDir(File(repoPath)).build()
            val git = Git(repo)
            val rootPath = repo.directory?.parentFile?.absolutePath ?: repoPath
            // JGit add expects relative path to repository root
            val relativePath = File(filePath).relativeTo(File(rootPath)).path.replace("\\", "/")
            git.add().addFilepattern(relativePath).call()
            git.close()
            repo.close()
            FileOperationResult.Success
        } catch (e: Exception) {
            FileOperationResult.Error(e.message ?: "Chyba při přidávání souboru do stage")
        }
    }

    suspend fun commit(path: String, message: String): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            val auth = settingsRepository.gitAuthSettings.first()
            val repo = FileRepositoryBuilder().findGitDir(File(path)).build()
            val git = Git(repo)
            val commitCommand = git.commit().setMessage(message)
            
            if (auth.authorName.isNotBlank() && auth.authorEmail.isNotBlank()) {
                commitCommand.setAuthor(auth.authorName, auth.authorEmail)
                commitCommand.setCommitter(auth.authorName, auth.authorEmail)
            }
            
            commitCommand.call()
            git.close()
            repo.close()
            FileOperationResult.Success
        } catch (e: Exception) {
            FileOperationResult.Error(e.message ?: "Chyba při commitu")
        }
    }

    suspend fun push(path: String): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            val credentials = getCredentialsProvider() ?: return@withContext FileOperationResult.Error("Nenastaven autentizační token. Prosím nastavte jej v Nastavení.")
            
            val repo = FileRepositoryBuilder().findGitDir(File(path)).build()
            val git = Git(repo)
            val remotes = repo.config.getSubsections("remote").toList()
            if (remotes.isEmpty()) {
                git.close()
                repo.close()
                return@withContext FileOperationResult.Error("Žádný vzdálený repozitář není nastaven (origin not found). Založte nebo připojte vzdálený repozitář.")
            }
            
            val branch = repo.branch ?: "master"
            git.push().setRemote("origin").add(branch).setCredentialsProvider(credentials).call()
            git.close()
            repo.close()
            FileOperationResult.Success
        } catch (e: Exception) {
            FileOperationResult.Error(e.message ?: "Chyba při push")
        }
    }

    suspend fun pull(path: String): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            val credentials = getCredentialsProvider() ?: return@withContext FileOperationResult.Error("Nenastaven autentizační token. Prosím nastavte jej v Nastavení.")
            
            val repo = FileRepositoryBuilder().findGitDir(File(path)).build()
            val remotes = repo.config.getSubsections("remote").toList()
            if (remotes.isEmpty()) {
                repo.close()
                return@withContext FileOperationResult.Error("Žádný vzdálený repozitář není nastaven (origin not found).")
            }
            val git = Git(repo)
            val result = git.pull().setCredentialsProvider(credentials).setRemote("origin").call()
            git.close()
            repo.close()
            if (result.isSuccessful) {
                FileOperationResult.Success
            } else {
                FileOperationResult.Error("Pull selhal: Konflikty nebo lokální změny")
            }
        } catch (e: Exception) {
            FileOperationResult.Error(e.message ?: "Chyba při pull")
        }
    }

    suspend fun testConnection(remoteUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val credentials = getCredentialsProvider() ?: return@withContext false
            // Perform a quick remote ls-remote to test connection and auth
            Git.lsRemoteRepository()
                .setRemote(remoteUrl)
                .setCredentialsProvider(credentials)
                .call()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun setRemoteUrl(path: String, remoteUrl: String): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            val repo = FileRepositoryBuilder().findGitDir(File(path)).build()
            val git = Git(repo)
            
            val config = repo.config
            config.setString("remote", "origin", "url", remoteUrl)
            config.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*")
            config.save()
            
            git.close()
            repo.close()
            FileOperationResult.Success
        } catch (e: Exception) {
            FileOperationResult.Error(e.message ?: "Chyba při nastavování remote URL")
        }
    }
    
    suspend fun createGithubRepo(name: String, isPrivate: Boolean, description: String = ""): Result<String> = withContext(Dispatchers.IO) {
        try {
            val token = settingsRepository.getGitToken() ?: return@withContext Result.failure(Exception("Not configured"))
            val url = URL("https://api.github.com/user/repos")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            
            val json = org.json.JSONObject()
            json.put("name", name)
            json.put("private", isPrivate)
            json.put("description", description)
            
            connection.outputStream.write(json.toString().toByteArray(Charsets.UTF_8))
            
            val responseCode = connection.responseCode
            if (responseCode == 201) {
                val response = connection.inputStream.bufferedReader().readText()
                val responseObj = org.json.JSONObject(response)
                val cloneUrl = responseObj.getString("clone_url")
                Result.success(cloneUrl)
            } else {
                val error = connection.errorStream?.bufferedReader()?.readText() ?: ""
                Result.failure(Exception("Github API error: $responseCode $error"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Network error"))
        }
    }
}
