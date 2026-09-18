package com.example.mynativeapp1.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "weather_app")

class AppPreferencesStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val json: Json,
) : PreferencesStore {
    override suspend fun getCurrentLocation(): SavedLocation? {
        val raw = context.dataStore.data.first()[CURRENT_LOCATION_KEY] ?: return null
        return runCatching { json.decodeFromString<SavedLocation>(raw) }.getOrNull()
    }

    override suspend fun saveCurrentLocation(location: SavedLocation) {
        context.dataStore.edit { prefs ->
            prefs[CURRENT_LOCATION_KEY] = json.encodeToString(location)
        }
    }

    override fun favoritesFlow(): Flow<List<FavoriteLocation>> =
        context.dataStore.data.map { prefs ->
            val raw = prefs[FAVORITE_LOCATIONS_KEY] ?: return@map emptyList()
            runCatching {
                json.decodeFromString<List<FavoriteLocation>>(raw)
            }.getOrDefault(emptyList())
                .sortedByDescending { it.addedAt }
        }

    override suspend fun getFavorites(): List<FavoriteLocation> = favoritesFlow().first()

    override suspend fun addFavorite(location: SavedLocation): FavoriteAddResult {
        val favorites = getFavorites().toMutableList()
        if (favorites.any { it.locationKey() == location.locationKey() }) {
            return FavoriteAddResult.AlreadyExists
        }
        if (favorites.size >= MAX_FAVORITES) {
            return FavoriteAddResult.LimitReached
        }
        favorites.add(location.toFavoriteLocation())
        saveFavorites(favorites)
        return FavoriteAddResult.Added
    }

    override suspend fun removeFavorite(location: SavedLocation) {
        val favorites = getFavorites()
            .filterNot { it.locationKey() == location.locationKey() }
        saveFavorites(favorites)
    }

    override suspend fun isFavorite(location: SavedLocation): Boolean =
        getFavorites().any { it.locationKey() == location.locationKey() }

    private suspend fun saveFavorites(favorites: List<FavoriteLocation>) {
        context.dataStore.edit { prefs ->
            prefs[FAVORITE_LOCATIONS_KEY] = json.encodeToString(favorites)
        }
    }

    private companion object {
        val CURRENT_LOCATION_KEY = stringPreferencesKey("current_location")
        val FAVORITE_LOCATIONS_KEY = stringPreferencesKey("favorite_locations")
        const val MAX_FAVORITES = 10
    }
}
