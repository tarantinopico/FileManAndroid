package com.example.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.FavoriteModel
import com.example.model.ThemeMode
import com.example.model.UiDensity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val THEME_KEY = stringPreferencesKey("theme_mode")
    private val DENSITY_KEY = stringPreferencesKey("ui_density")
    private val FAVORITES_KEY = stringSetPreferencesKey("favorite_paths")

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
        val paths = prefs[FAVORITES_KEY] ?: emptySet()
        paths.map { path ->
            val file = File(path)
            FavoriteModel(
                path = path,
                name = file.name.ifEmpty { path },
                isAvailable = file.exists()
            )
        }.sortedBy { it.name.lowercase() }
    }

    suspend fun addFavorite(path: String, name: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[FAVORITES_KEY] ?: emptySet()
            prefs[FAVORITES_KEY] = current + path
        }
    }

    suspend fun removeFavorite(path: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[FAVORITES_KEY] ?: emptySet()
            prefs[FAVORITES_KEY] = current - path
        }
    }
}
