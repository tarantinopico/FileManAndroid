package com.example.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.EditorSettings
import com.example.model.FavoriteModel
import com.example.model.SyntaxLanguage
import com.example.model.SyntaxMapping
import com.example.model.ThemeMode
import com.example.model.UiDensity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val THEME_KEY = stringPreferencesKey("theme_mode")
    private val DENSITY_KEY = stringPreferencesKey("ui_density")
    private val FAVORITES_KEY = stringSetPreferencesKey("favorite_paths") // old
    private val FAVORITE_ITEMS_KEY = stringSetPreferencesKey("favorite_items_v2")
    
    private val EDITOR_WORD_WRAP = booleanPreferencesKey("editor_word_wrap")
    private val EDITOR_LINE_NUMBERS = booleanPreferencesKey("editor_line_numbers")
    private val EDITOR_HIGHLIGHT = booleanPreferencesKey("editor_highlight")
    private val SYNTAX_MAPPINGS = stringPreferencesKey("syntax_mappings")
    
    // File Settings
    private val FILE_SHOW_HIDDEN = booleanPreferencesKey("file_show_hidden")
    private val FILE_SHOW_EXTENSIONS = booleanPreferencesKey("file_show_extensions")
    private val FILE_SORT_OPTION = stringPreferencesKey("file_sort_option")

    val themePreference: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[THEME_KEY]) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }
    
    val densityPreference: Flow<UiDensity> = context.dataStore.data.map { prefs ->
        when (prefs[DENSITY_KEY]) {
            UiDensity.COMPACT.name -> UiDensity.COMPACT
            UiDensity.LARGE.name -> UiDensity.LARGE
            UiDensity.EXTRA_LARGE.name -> UiDensity.EXTRA_LARGE
            else -> UiDensity.NORMAL
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[THEME_KEY] = mode.name
        }
    }
    
    suspend fun setUiDensity(density: UiDensity) {
        context.dataStore.edit { prefs ->
            prefs[DENSITY_KEY] = density.name
        }
    }
    
    val favorites: Flow<List<FavoriteModel>> = context.dataStore.data.map { prefs ->
        val oldPaths = prefs[FAVORITES_KEY] ?: emptySet()
        val items = prefs[FAVORITE_ITEMS_KEY] ?: emptySet()
        
        val parsedItems = items.mapNotNull { 
            val parts = it.split("|", limit = 3)
            if (parts.size == 3) {
                val file = File(parts[0])
                FavoriteModel(
                    path = parts[0],
                    name = parts[1],
                    isAvailable = file.exists(),
                    icon = try { com.example.model.FavoriteIcon.valueOf(parts[2]) } catch (e: Exception) { com.example.model.FavoriteIcon.FOLDER }
                )
            } else null
        }
        
        // Migrate old if not present in new
        val oldParsedItems = MapOldToNew(oldPaths, parsedItems)
        
        (parsedItems + oldParsedItems).sortedBy { it.name.lowercase() }
    }
    
    private fun MapOldToNew(oldPaths: Set<String>, newItems: List<FavoriteModel>): List<FavoriteModel> {
        val currentPaths = newItems.map { it.path }.toSet()
        return oldPaths.filter { it !in currentPaths }.map { path ->
            val file = File(path)
            FavoriteModel(
                path = path,
                name = file.name.ifEmpty { path },
                isAvailable = file.exists(),
                icon = com.example.model.FavoriteIcon.STAR
            )
        }
    }

    suspend fun saveFavorite(favorite: FavoriteModel) {
        context.dataStore.edit { prefs ->
            val current = prefs[FAVORITE_ITEMS_KEY] ?: emptySet()
            // remove if path already exists to overwrite
            val filtered = current.filter { !it.startsWith("${favorite.path}|") }.toMutableSet()
            filtered.add("${favorite.path}|${favorite.name}|${favorite.icon.name}")
            prefs[FAVORITE_ITEMS_KEY] = filtered
        }
    }

    suspend fun removeFavorite(path: String) {
        context.dataStore.edit { prefs ->
            // remove from old key if it exists
            val oldPaths = prefs[FAVORITES_KEY] ?: emptySet()
            if (oldPaths.contains(path)) {
                prefs[FAVORITES_KEY] = oldPaths - path
            }
            
            val current = prefs[FAVORITE_ITEMS_KEY] ?: emptySet()
            prefs[FAVORITE_ITEMS_KEY] = current.filter { !it.startsWith("$path|") }.toSet()
        }
    }

    val editorSettings: Flow<EditorSettings> = context.dataStore.data.map { prefs ->
        EditorSettings(
            wordWrap = prefs[EDITOR_WORD_WRAP] ?: false,
            showLineNumbers = prefs[EDITOR_LINE_NUMBERS] ?: true,
            syntaxHighlightEnabled = prefs[EDITOR_HIGHLIGHT] ?: true
        )
    }

    suspend fun updateEditorSettings(settings: EditorSettings) {
        context.dataStore.edit { prefs ->
            prefs[EDITOR_WORD_WRAP] = settings.wordWrap
            prefs[EDITOR_LINE_NUMBERS] = settings.showLineNumbers
            prefs[EDITOR_HIGHLIGHT] = settings.syntaxHighlightEnabled
        }
    }

    val defaultMappings = listOf(
        SyntaxMapping("kt", SyntaxLanguage.KOTLIN), SyntaxMapping("java", SyntaxLanguage.KOTLIN),
        SyntaxMapping("cpp", SyntaxLanguage.KOTLIN), SyntaxMapping("c", SyntaxLanguage.KOTLIN),
        SyntaxMapping("cs", SyntaxLanguage.KOTLIN), SyntaxMapping("ts", SyntaxLanguage.KOTLIN),
        SyntaxMapping("js", SyntaxLanguage.KOTLIN),
        SyntaxMapping("py", SyntaxLanguage.PYTHON), SyntaxMapping("sh", SyntaxLanguage.PYTHON), SyntaxMapping("bash", SyntaxLanguage.PYTHON),
        SyntaxMapping("xml", SyntaxLanguage.XML), SyntaxMapping("html", SyntaxLanguage.XML), SyntaxMapping("htm", SyntaxLanguage.XML), SyntaxMapping("svg", SyntaxLanguage.XML),
        SyntaxMapping("json", SyntaxLanguage.JSON),
        SyntaxMapping("yml", SyntaxLanguage.YAML), SyntaxMapping("yaml", SyntaxLanguage.YAML),
        SyntaxMapping("gradle", SyntaxLanguage.GRADLE),
        SyntaxMapping("css", SyntaxLanguage.CSS),
        SyntaxMapping("md", SyntaxLanguage.MARKDOWN)
    )

    val syntaxMappings: Flow<List<SyntaxMapping>> = context.dataStore.data.map { prefs ->
        val raw = prefs[SYNTAX_MAPPINGS]
        if (raw == null) {
            defaultMappings
        } else {
            raw.split(";").filter { it.isNotBlank() }.mapNotNull {
                val parts = it.split("=")
                if (parts.size == 2) {
                    try {
                        SyntaxMapping(parts[0], SyntaxLanguage.valueOf(parts[1]))
                    } catch (e: Exception) { null }
                } else null
            }.sortedBy { it.extension }
        }
    }

    val fileSettings: Flow<com.example.model.FileSettings> = context.dataStore.data.map { prefs ->
        val sortOptionStr = prefs[FILE_SORT_OPTION]
        val sortOption = try {
            if (sortOptionStr != null) com.example.model.SortOption.valueOf(sortOptionStr)
            else com.example.model.SortOption.NAME_ASC
        } catch (e: Exception) {
            com.example.model.SortOption.NAME_ASC
        }
        
        com.example.model.FileSettings(
            showHiddenFiles = prefs[FILE_SHOW_HIDDEN] ?: false,
            showFileExtensions = prefs[FILE_SHOW_EXTENSIONS] ?: true,
            sortOption = sortOption
        )
    }

    suspend fun updateFileSettings(settings: com.example.model.FileSettings) {
        context.dataStore.edit { prefs ->
            prefs[FILE_SHOW_HIDDEN] = settings.showHiddenFiles
            prefs[FILE_SHOW_EXTENSIONS] = settings.showFileExtensions
            prefs[FILE_SORT_OPTION] = settings.sortOption.name
        }
    }

    suspend fun addSyntaxMapping(mapping: SyntaxMapping) {
        context.dataStore.edit { prefs ->
            val currentRaw = prefs[SYNTAX_MAPPINGS]
            val currentList = if (currentRaw != null) {
                currentRaw.split(";").filter { it.isNotBlank() }.associate {
                    val p = it.split("=")
                    p[0] to p[1]
                }.toMutableMap()
            } else {
                defaultMappings.associate { it.extension to it.language.name }.toMutableMap()
            }
            currentList[mapping.extension] = mapping.language.name
            prefs[SYNTAX_MAPPINGS] = currentList.entries.joinToString(";") { "${it.key}=${it.value}" }
        }
    }

    suspend fun removeSyntaxMapping(extension: String) {
        context.dataStore.edit { prefs ->
            val currentRaw = prefs[SYNTAX_MAPPINGS]
            val currentList = if (currentRaw != null) {
                currentRaw.split(";").filter { it.isNotBlank() }.associate {
                    val p = it.split("=")
                    p[0] to p[1]
                }.toMutableMap()
            } else {
                defaultMappings.associate { it.extension to it.language.name }.toMutableMap()
            }
            currentList.remove(extension)
            prefs[SYNTAX_MAPPINGS] = currentList.entries.joinToString(";") { "${it.key}=${it.value}" }
        }
    }
    
    suspend fun resetSyntaxMappings() {
        context.dataStore.edit { prefs ->
            prefs.remove(SYNTAX_MAPPINGS)
        }
    }
}
