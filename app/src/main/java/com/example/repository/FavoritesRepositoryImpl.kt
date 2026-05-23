package com.example.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.FavoriteModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

private val Context.dataStore by preferencesDataStore(name = "favorites")

class FavoritesRepositoryImpl(private val context: Context) : FavoritesRepository {

    private val FAVORITES_KEY = stringSetPreferencesKey("favorite_paths")

    override val favorites: Flow<List<FavoriteModel>> = context.dataStore.data
        .map { preferences ->
            val paths = preferences[FAVORITES_KEY] ?: emptySet()
            paths.map { path ->
                val file = File(path)
                FavoriteModel(
                    path = path,
                    name = file.name.ifEmpty { path },
                    isAvailable = file.exists()
                )
            }.sortedBy { it.name.lowercase() }
        }

    override suspend fun addFavorite(path: String, name: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[FAVORITES_KEY] ?: emptySet()
            preferences[FAVORITES_KEY] = current + path
        }
    }

    override suspend fun removeFavorite(path: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[FAVORITES_KEY] ?: emptySet()
            preferences[FAVORITES_KEY] = current - path
        }
    }
}
