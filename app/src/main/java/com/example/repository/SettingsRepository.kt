package com.example.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppPreferences
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
    private val EDITOR_ACTIVE_LINE = booleanPreferencesKey("editor_active_line")
    private val EDITOR_TOOLBAR = booleanPreferencesKey("editor_toolbar")
    private val EDITOR_AUTOSAVE = booleanPreferencesKey("editor_autosave")
    private val EDITOR_LARGE_FILE_SAFE = booleanPreferencesKey("editor_large_file_safe")
    private val EDITOR_KEYBOARD_FRIENDLY = booleanPreferencesKey("editor_keyboard_friendly")
    private val SYNTAX_MAPPINGS = stringPreferencesKey("syntax_mappings")
    
    // File Settings
    private val FILE_SHOW_HIDDEN = booleanPreferencesKey("file_show_hidden")
    private val FILE_SHOW_EXTENSIONS = booleanPreferencesKey("file_show_extensions")
    private val FILE_SORT_OPTION = stringPreferencesKey("file_sort_option")

    // App Preferences
    private val APP_DRAWER_ENABLED = booleanPreferencesKey("app_drawer_enabled")
    private val APP_SHOW_FAVORITES = booleanPreferencesKey("app_show_favorites")
    private val APP_SHOW_PINNED = booleanPreferencesKey("app_show_pinned")
    private val APP_SHOW_RECENTS = booleanPreferencesKey("app_show_recents")
    private val APP_START_LAST_LOC = booleanPreferencesKey("app_start_last_loc")
    private val APP_LAST_LOC = stringPreferencesKey("app_last_loc")
    private val APP_THUMBNAILS = booleanPreferencesKey("app_thumbnails")
    private val APP_BADGES = booleanPreferencesKey("app_badges")
    private val APP_BADGE_COLORS = booleanPreferencesKey("app_badge_colors")
    private val APP_CONFIRM_DEL = booleanPreferencesKey("app_confirm_del")
    private val APP_MULTI_SELECT = booleanPreferencesKey("app_multi_select")
    private val APP_DETAIL_PANELS = booleanPreferencesKey("app_detail_panels")
    private val APP_SHOW_FREE_SPACE = booleanPreferencesKey("app_show_free_space")
    private val APP_COMPACT_LIST = booleanPreferencesKey("app_compact_list")

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
            val parts = it.split("|", limit = 4)
            if (parts.size >= 3) {
                val file = File(parts[0])
                FavoriteModel(
                    path = parts[0],
                    name = parts[1],
                    isAvailable = file.exists(),
                    icon = try { com.example.model.FavoriteIcon.valueOf(parts[2]) } catch (e: Exception) { com.example.model.FavoriteIcon.FOLDER },
                    isPinned = try { parts.getOrNull(3)?.toBoolean() ?: false } catch (e: Exception) { false }
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
                icon = com.example.model.FavoriteIcon.STAR,
                isPinned = false
            )
        }
    }

    suspend fun saveFavorite(favorite: FavoriteModel) {
        context.dataStore.edit { prefs ->
            val current = prefs[FAVORITE_ITEMS_KEY] ?: emptySet()
            // remove if path already exists to overwrite
            val filtered = current.filter { !it.startsWith("${favorite.path}|") }.toMutableSet()
            filtered.add("${favorite.path}|${favorite.name}|${favorite.icon.name}|${favorite.isPinned}")
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
            syntaxHighlightEnabled = prefs[EDITOR_HIGHLIGHT] ?: true,
            activeLineHighlightEnabled = prefs[EDITOR_ACTIVE_LINE] ?: true,
            editorToolbarEnabled = prefs[EDITOR_TOOLBAR] ?: true,
            autosaveEnabled = prefs[EDITOR_AUTOSAVE] ?: false,
            largeFileSafeModeEnabled = prefs[EDITOR_LARGE_FILE_SAFE] ?: true,
            keyboardFriendlyBehavior = prefs[EDITOR_KEYBOARD_FRIENDLY] ?: true
        )
    }

    suspend fun updateEditorSettings(settings: EditorSettings) {
        context.dataStore.edit { prefs ->
            prefs[EDITOR_WORD_WRAP] = settings.wordWrap
            prefs[EDITOR_LINE_NUMBERS] = settings.showLineNumbers
            prefs[EDITOR_HIGHLIGHT] = settings.syntaxHighlightEnabled
            prefs[EDITOR_ACTIVE_LINE] = settings.activeLineHighlightEnabled
            prefs[EDITOR_TOOLBAR] = settings.editorToolbarEnabled
            prefs[EDITOR_AUTOSAVE] = settings.autosaveEnabled
            prefs[EDITOR_LARGE_FILE_SAFE] = settings.largeFileSafeModeEnabled
            prefs[EDITOR_KEYBOARD_FRIENDLY] = settings.keyboardFriendlyBehavior
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
                if (parts.size >= 2) {
                    try {
                        val colorInfo = if (parts.size > 2) parts[2].toIntOrNull() else null
                        SyntaxMapping(parts[0], SyntaxLanguage.valueOf(parts[1]), colorInfo)
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

    val appPreferences: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        AppPreferences(
            drawerEnabled = prefs[APP_DRAWER_ENABLED] ?: true,
            showFavorites = prefs[APP_SHOW_FAVORITES] ?: true,
            showPinned = prefs[APP_SHOW_PINNED] ?: true,
            showRecents = prefs[APP_SHOW_RECENTS] ?: true,
            openLastLocationOnStartup = prefs[APP_START_LAST_LOC] ?: false,
            lastOpenedLocation = prefs[APP_LAST_LOC],
            imageThumbnailsEnabled = prefs[APP_THUMBNAILS] ?: true,
            showFileBadges = prefs[APP_BADGES] ?: true,
            badgeColorEnabled = prefs[APP_BADGE_COLORS] ?: true,
            confirmDeletions = prefs[APP_CONFIRM_DEL] ?: true,
            multiSelectEnabled = prefs[APP_MULTI_SELECT] ?: true,
            detailPanelsEnabled = prefs[APP_DETAIL_PANELS] ?: true,
            showFreeSpace = prefs[APP_SHOW_FREE_SPACE] ?: true,
            compactListMode = prefs[APP_COMPACT_LIST] ?: false
        )
    }

    suspend fun updateAppPreferences(preferences: AppPreferences) {
        context.dataStore.edit { prefs ->
            prefs[APP_DRAWER_ENABLED] = preferences.drawerEnabled
            prefs[APP_SHOW_FAVORITES] = preferences.showFavorites
            prefs[APP_SHOW_PINNED] = preferences.showPinned
            prefs[APP_SHOW_RECENTS] = preferences.showRecents
            prefs[APP_START_LAST_LOC] = preferences.openLastLocationOnStartup
            preferences.lastOpenedLocation?.let { prefs[APP_LAST_LOC] = it } ?: prefs.remove(APP_LAST_LOC)
            prefs[APP_THUMBNAILS] = preferences.imageThumbnailsEnabled
            prefs[APP_BADGES] = preferences.showFileBadges
            prefs[APP_BADGE_COLORS] = preferences.badgeColorEnabled
            prefs[APP_CONFIRM_DEL] = preferences.confirmDeletions
            prefs[APP_MULTI_SELECT] = preferences.multiSelectEnabled
            prefs[APP_DETAIL_PANELS] = preferences.detailPanelsEnabled
            prefs[APP_SHOW_FREE_SPACE] = preferences.showFreeSpace
            prefs[APP_COMPACT_LIST] = preferences.compactListMode
        }
    }

    suspend fun setLastOpenedLocation(path: String) {
        context.dataStore.edit { prefs ->
            prefs[APP_LAST_LOC] = path
        }
    }

    suspend fun addSyntaxMapping(mapping: SyntaxMapping) {
        context.dataStore.edit { prefs ->
            val currentRaw = prefs[SYNTAX_MAPPINGS]
            val currentList = if (currentRaw != null) {
                currentRaw.split(";").filter { it.isNotBlank() }.associate {
                    val p = it.split("=")
                    // extension to language, optionally color
                    p[0] to (if (p.size > 2) "${p[1]}=${p[2]}" else p[1])
                }.toMutableMap()
            } else {
                defaultMappings.associate { it.extension to it.language.name }.toMutableMap()
            }
            currentList[mapping.extension] = mapping.language.name + (mapping.tagColorArgb?.let { "=$it" } ?: "")
            prefs[SYNTAX_MAPPINGS] = currentList.entries.joinToString(";") { "${it.key}=${it.value}" }
        }
    }

    suspend fun removeSyntaxMapping(extension: String) {
        context.dataStore.edit { prefs ->
            val currentRaw = prefs[SYNTAX_MAPPINGS]
            val currentList = if (currentRaw != null) {
                currentRaw.split(";").filter { it.isNotBlank() }.associate {
                    val p = it.split("=")
                    p[0] to (if (p.size > 2) "${p[1]}=${p[2]}" else p[1])
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
