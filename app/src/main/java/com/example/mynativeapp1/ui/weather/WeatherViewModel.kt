package com.example.mynativeapp1.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.mynativeapp1.data.FavoriteAddResult
import com.example.mynativeapp1.data.PreferencesStore
import com.example.mynativeapp1.data.FavoriteLocation
import com.example.mynativeapp1.data.SavedLocation
import com.example.mynativeapp1.data.WeatherDataSource
import com.example.mynativeapp1.data.location.DeviceLocationProvider
import com.example.mynativeapp1.data.location.LocationUnavailableException
import com.example.mynativeapp1.data.location.PlaceNameResolver
import com.example.mynativeapp1.data.toFavoriteLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val dataSource: WeatherDataSource,
    private val preferencesStore: PreferencesStore,
    private val locationProvider: DeviceLocationProvider,
    private val placeNameResolver: PlaceNameResolver,
) : ViewModel() {

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _currentLocation = MutableStateFlow(SavedLocation.DEFAULT_BEIJING)
    val currentLocation: StateFlow<SavedLocation> = _currentLocation.asStateFlow()

    private val _searchUiState = MutableStateFlow(SearchUiState())
    val searchUiState: StateFlow<SearchUiState> = _searchUiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _favorites = MutableStateFlow<List<FavoriteLocation>>(emptyList())
    val favorites: StateFlow<List<FavoriteLocation>> = _favorites.asStateFlow()

    val isCurrentLocationFavorite: StateFlow<Boolean> = combine(
        _currentLocation,
        _favorites,
    ) { location, favorites ->
        favorites.any { it.locationKey() == location.locationKey() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _events = MutableSharedFlow<WeatherUiEvent>()
    val events = _events.asSharedFlow()

    private val searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            preferencesStore.favoritesFlow().collect { favorites ->
                _favorites.value = favorites
            }
        }
        viewModelScope.launch {
            searchQuery
                .debounce(SEARCH_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collect { query -> performSearch(query) }
        }
        viewModelScope.launch {
            val saved = preferencesStore.getCurrentLocation() ?: SavedLocation.DEFAULT_BEIJING
            _currentLocation.value = saved
            loadWeatherForLocation(saved, showFullScreenLoading = true)
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchUiState.update { it.copy(query = query, inlineMessage = null, showRetry = false) }
        searchQuery.value = query
        if (query.length < MIN_SEARCH_LENGTH) {
            _searchUiState.update {
                it.copy(
                    results = emptyList(),
                    isSearching = false,
                    inlineMessage = if (query.isEmpty()) null else SearchErrorMessages.PLACEHOLDER,
                )
            }
        }
    }

    fun retrySearch() {
        performSearch(_searchUiState.value.query)
    }

    fun selectLocation(location: SavedLocation) {
        viewModelScope.launch {
            preferencesStore.saveCurrentLocation(location)
            _currentLocation.value = location
            loadWeatherForLocation(location, showFullScreenLoading = true)
        }
    }

    fun retry() {
        loadWeatherForLocation(_currentLocation.value, showFullScreenLoading = true)
    }

    fun refreshWeather() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            dataSource.getWeather(_currentLocation.value, includeDailyForecast = true).fold(
                onSuccess = { weather -> _uiState.value = WeatherUiState.Success(weather) },
                onFailure = { error ->
                    emitSnackbar(WeatherErrorMessages.messageFor(error))
                },
            )
            _isRefreshing.value = false
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val location = _currentLocation.value
            if (preferencesStore.isFavorite(location)) {
                preferencesStore.removeFavorite(location)
            } else {
                when (preferencesStore.addFavorite(location)) {
                    FavoriteAddResult.Added -> Unit
                    FavoriteAddResult.AlreadyExists -> Unit
                    FavoriteAddResult.LimitReached ->
                        emitSnackbar("最多收藏 10 个地点")
                }
            }
        }
    }

    fun removeFavorite(favorite: FavoriteLocation) {
        viewModelScope.launch {
            preferencesStore.removeFavorite(favorite.toSavedLocation())
        }
    }

    fun selectFavorite(favorite: FavoriteLocation) {
        selectLocation(favorite.toSavedLocation())
    }

    fun onMyLocationRequested(hasPermission: Boolean, requestPermission: () -> Unit) {
        if (!hasPermission) {
            requestPermission()
            return
        }
        locateUser()
    }

    fun onLocationPermissionResult(granted: Boolean, permanentlyDenied: Boolean) {
        viewModelScope.launch {
            when {
                granted -> locateUser()
                permanentlyDenied -> _events.emit(
                    WeatherUiEvent.Snackbar(
                        message = "需要定位权限才能获取附近天气",
                        actionLabel = "去设置",
                        action = WeatherUiEventAction.OpenAppSettings,
                    ),
                )
                else -> emitSnackbar("需要定位权限才能获取附近天气")
            }
        }
    }

    fun onSnackbarAction(action: WeatherUiEventAction?) {
        if (action == WeatherUiEventAction.OpenAppSettings) {
            viewModelScope.launch { _events.emit(WeatherUiEvent.OpenAppSettings) }
        }
    }

    private fun locateUser() {
        viewModelScope.launch {
            locationProvider.getCurrentCoordinates().fold(
                onSuccess = { coordinates ->
                    val name = withContext(Dispatchers.IO) {
                        placeNameResolver.resolveName(
                            coordinates.latitude,
                            coordinates.longitude,
                        )
                    } ?: "当前位置"
                    val location = SavedLocation(
                        name = name,
                        latitude = coordinates.latitude,
                        longitude = coordinates.longitude,
                        timezone = "auto",
                        country = null,
                    )
                    selectLocation(location)
                },
                onFailure = { error ->
                    val message = if (error is LocationUnavailableException) {
                        "无法获取当前位置，请稍后重试"
                    } else {
                        "无法获取当前位置，请稍后重试"
                    }
                    emitSnackbar(message)
                },
            )
        }
    }

    private fun performSearch(query: String) {
        if (query.length < MIN_SEARCH_LENGTH) return
        viewModelScope.launch {
            _searchUiState.update {
                it.copy(isSearching = true, inlineMessage = null, showRetry = false)
            }
            dataSource.searchLocations(query).fold(
                onSuccess = { results ->
                    _searchUiState.update {
                        it.copy(
                            results = results,
                            isSearching = false,
                            inlineMessage = null,
                            showRetry = false,
                        )
                    }
                },
                onFailure = { error ->
                    _searchUiState.update {
                        it.copy(
                            results = emptyList(),
                            isSearching = false,
                            inlineMessage = SearchErrorMessages.messageFor(error),
                            showRetry = error !is com.example.mynativeapp1.data.WeatherError.CityNotFound,
                        )
                    }
                },
            )
        }
    }

    private fun loadWeatherForLocation(
        location: SavedLocation,
        showFullScreenLoading: Boolean,
    ) {
        viewModelScope.launch {
            if (showFullScreenLoading) {
                _uiState.value = WeatherUiState.Loading
            }
            _uiState.value = dataSource.getWeather(location, includeDailyForecast = true).fold(
                onSuccess = { WeatherUiState.Success(it) },
                onFailure = { WeatherUiState.Error(WeatherErrorMessages.messageFor(it)) },
            )
        }
    }

    private suspend fun emitSnackbar(message: String) {
        _events.emit(WeatherUiEvent.Snackbar(message))
    }

    data class SearchUiState(
        val query: String = "",
        val results: List<SavedLocation> = emptyList(),
        val isSearching: Boolean = false,
        val inlineMessage: String? = null,
        val showRetry: Boolean = false,
    )

    private companion object {
        const val MIN_SEARCH_LENGTH = 2
        const val SEARCH_DEBOUNCE_MS = 400L
    }
}

sealed interface WeatherUiEvent {
    data class Snackbar(
        val message: String,
        val actionLabel: String? = null,
        val action: WeatherUiEventAction? = null,
    ) : WeatherUiEvent

    data object OpenAppSettings : WeatherUiEvent
}

enum class WeatherUiEventAction {
    OpenAppSettings,
}
