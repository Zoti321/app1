package com.example.mynativeapp1.data

import kotlinx.coroutines.flow.Flow

interface PreferencesStore {
    suspend fun getCurrentLocation(): SavedLocation?
    suspend fun saveCurrentLocation(location: SavedLocation)
    fun favoritesFlow(): Flow<List<FavoriteLocation>>
    suspend fun getFavorites(): List<FavoriteLocation>
    suspend fun addFavorite(location: SavedLocation): FavoriteAddResult
    suspend fun removeFavorite(location: SavedLocation)
    suspend fun isFavorite(location: SavedLocation): Boolean
}

enum class FavoriteAddResult {
    Added,
    AlreadyExists,
    LimitReached,
}
