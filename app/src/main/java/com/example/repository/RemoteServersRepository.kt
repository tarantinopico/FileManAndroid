package com.example.repository

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.model.RemoteServerModel
import com.example.model.ServerType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class RemoteServersRepository(private val context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "remote_servers_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _servers = MutableStateFlow<List<RemoteServerModel>>(emptyList())
    val servers: StateFlow<List<RemoteServerModel>> = _servers.asStateFlow()

    init {
        loadServers()
    }

    private fun loadServers() {
        val serversJson = sharedPreferences.getString("servers", "[]") ?: "[]"
        try {
            val jsonArray = JSONArray(serversJson)
            val loadedServers = mutableListOf<RemoteServerModel>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                loadedServers.add(
                    RemoteServerModel(
                        id = obj.getString("id"),
                        type = ServerType.valueOf(obj.getString("type")),
                        name = obj.getString("name"),
                        host = obj.getString("host"),
                        port = obj.getInt("port"),
                        username = obj.getString("username"),
                        passwordOrKeyPath = if (obj.has("passwordOrKeyPath")) obj.getString("passwordOrKeyPath") else null,
                        remotePath = obj.getString("remotePath")
                    )
                )
            }
            _servers.value = loadedServers
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveServers() {
        val jsonArray = JSONArray()
        _servers.value.forEach { server ->
            val obj = JSONObject()
            obj.put("id", server.id)
            obj.put("type", server.type.name)
            obj.put("name", server.name)
            obj.put("host", server.host)
            obj.put("port", server.port)
            obj.put("username", server.username)
            if (server.passwordOrKeyPath != null) {
                obj.put("passwordOrKeyPath", server.passwordOrKeyPath)
            }
            obj.put("remotePath", server.remotePath)
            jsonArray.put(obj)
        }
        sharedPreferences.edit().putString("servers", jsonArray.toString()).apply()
    }

    fun addServer(server: RemoteServerModel) {
        val newServer = if (server.id.isEmpty()) server.copy(id = UUID.randomUUID().toString()) else server
        _servers.value = _servers.value + newServer
        saveServers()
    }

    fun updateServer(server: RemoteServerModel) {
        _servers.value = _servers.value.map { if (it.id == server.id) server else it }
        saveServers()
    }

    fun removeServer(serverId: String) {
        _servers.value = _servers.value.filter { it.id != serverId }
        saveServers()
    }

    fun getServer(serverId: String): RemoteServerModel? {
        return _servers.value.find { it.id == serverId }
    }
}
