package com.example.repository

import com.example.model.FavoriteModel
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    val favorites: Flow<List<FavoriteModel>>
    suspend fun addFavorite(path: String, name: String)
    suspend fun removeFavorite(path: String)
}
