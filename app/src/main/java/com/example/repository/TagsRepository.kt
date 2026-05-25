package com.example.repository

import android.content.Context
import com.example.model.TagModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class TagsRepository(private val context: Context) {
    private val tagsFile = File(context.filesDir, "tags.json")
    private val _fileTags = MutableStateFlow<Map<String, List<TagModel>>>(emptyMap())
    val fileTags: StateFlow<Map<String, List<TagModel>>> = _fileTags.asStateFlow()

    private val _availableTags = MutableStateFlow<List<TagModel>>(emptyList())
    val availableTags: StateFlow<List<TagModel>> = _availableTags.asStateFlow()

    init {
        loadTags()
    }

    private fun loadTags() {
        if (!tagsFile.exists()) {
            // Default tags
            val defaultTags = listOf(
                TagModel(UUID.randomUUID().toString(), "Pracovní", 0xFF1976D2.toInt()),
                TagModel(UUID.randomUUID().toString(), "Důležité", 0xFFD32F2F.toInt()),
                TagModel(UUID.randomUUID().toString(), "Archiv", 0xFF757575.toInt()),
                TagModel(UUID.randomUUID().toString(), "Oblíbené", 0xFFFBC02D.toInt())
            )
            _availableTags.value = defaultTags
            saveTags()
            return
        }

        try {
            val content = tagsFile.readText()
            val root = JSONObject(content)
            
            // Parse available tags
            val availableArray = root.optJSONArray("availableTags") ?: JSONArray()
            val parsedAvailable = mutableListOf<TagModel>()
            for (i in 0 until availableArray.length()) {
                val obj = availableArray.getJSONObject(i)
                parsedAvailable.add(TagModel(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    colorArgb = obj.getInt("colorArgb")
                ))
            }
            _availableTags.value = parsedAvailable
            
            // Parse file tags
            val mappingObj = root.optJSONObject("fileTags") ?: JSONObject()
            val parsedMapping = mutableMapOf<String, List<TagModel>>()
            
            val keys = mappingObj.keys()
            while(keys.hasNext()) {
                val path = keys.next()
                val tagIdsArray = mappingObj.getJSONArray(path)
                val tagIds = mutableListOf<String>()
                for (i in 0 until tagIdsArray.length()) {
                    tagIds.add(tagIdsArray.getString(i))
                }
                
                val currentTags = tagIds.mapNotNull { id -> parsedAvailable.find { it.id == id } }
                parsedMapping[path] = currentTags
            }
            _fileTags.value = parsedMapping

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveTags() {
        try {
            val root = JSONObject()
            
            val availableArray = JSONArray()
            _availableTags.value.forEach { tag ->
                val tagObj = JSONObject()
                tagObj.put("id", tag.id)
                tagObj.put("name", tag.name)
                tagObj.put("colorArgb", tag.colorArgb)
                availableArray.put(tagObj)
            }
            root.put("availableTags", availableArray)
            
            val mappingObj = JSONObject()
            _fileTags.value.forEach { (path, tags) ->
                if (tags.isNotEmpty()) {
                    val idArray = JSONArray()
                    tags.forEach { idArray.put(it.id) }
                    mappingObj.put(path, idArray)
                }
            }
            root.put("fileTags", mappingObj)
            
            tagsFile.writeText(root.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun addAvailableTag(name: String, colorArgb: Int) = withContext(Dispatchers.IO) {
        val newTag = TagModel(UUID.randomUUID().toString(), name, colorArgb)
        _availableTags.value = _availableTags.value + newTag
        saveTags()
    }

    suspend fun updateAvailableTag(tag: TagModel) = withContext(Dispatchers.IO) {
        _availableTags.value = _availableTags.value.map { if (it.id == tag.id) tag else it }
        // Update mappings as well
        val newMap = _fileTags.value.mapValues { entry -> 
            entry.value.map { if (it.id == tag.id) tag else it }
        }
        _fileTags.value = newMap
        saveTags()
    }

    suspend fun deleteAvailableTag(tagId: String) = withContext(Dispatchers.IO) {
        _availableTags.value = _availableTags.value.filter { it.id != tagId }
        val newMap = _fileTags.value.mapValues { entry ->
            entry.value.filter { it.id != tagId }
        }.filterValues { it.isNotEmpty() }
        _fileTags.value = newMap
        saveTags()
    }

    suspend fun addTagToFile(path: String, tag: TagModel) = withContext(Dispatchers.IO) {
        val currentTags = _fileTags.value[path] ?: emptyList()
        if (currentTags.none { it.id == tag.id }) {
            val newMap = _fileTags.value.toMutableMap()
            newMap[path] = currentTags + tag
            _fileTags.value = newMap
            saveTags()
        }
    }

    suspend fun removeTagFromFile(path: String, tagId: String) = withContext(Dispatchers.IO) {
        val currentTags = _fileTags.value[path] ?: emptyList()
        val newTags = currentTags.filter { it.id != tagId }
        val newMap = _fileTags.value.toMutableMap()
        if (newTags.isEmpty()) {
            newMap.remove(path)
        } else {
            newMap[path] = newTags
        }
        _fileTags.value = newMap
        saveTags()
    }
}
