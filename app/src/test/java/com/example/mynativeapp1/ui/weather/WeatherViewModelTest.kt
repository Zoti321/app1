package com.example.mynativeapp1.ui.weather

import com.example.mynativeapp1.data.FavoriteAddResult
import com.example.mynativeapp1.data.FavoriteLocation
import com.example.mynativeapp1.data.PreferencesStore
import com.example.mynativeapp1.data.SavedLocation
import com.example.mynativeapp1.data.WeatherDataSource
import com.example.mynativeapp1.data.WeatherError
import com.example.mynativeapp1.data.WeatherInfo
import com.example.mynativeapp1.data.location.Coordinates
import com.example.mynativeapp1.data.location.DeviceLocationProvider
import com.example.mynativeapp1.data.location.PlaceNameResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_loadsDefaultBeijingWeather() = runTest {
        val viewModel = createViewModel(FakeWeatherDataSource(Result.success(SAMPLE_WEATHER)))

        assertEquals(WeatherUiState.Success(SAMPLE_WEATHER), viewModel.uiState.value)
        assertEquals(SavedLocation.DEFAULT_BEIJING, viewModel.currentLocation.value)
    }

    @Test
    fun loadWeather_mapsNoNetworkError() = runTest {
        val viewModel = createViewModel(FakeWeatherDataSource(Result.failure(WeatherError.NoNetwork)))

        val error = viewModel.uiState.value as WeatherUiState.Error
        assertEquals("无法连接网络，请检查后重试", error.message)
    }

    @Test
    fun retry_afterError_reloadsWeather() = runTest {
        val dataSource = FakeWeatherDataSource(
            mutableListOf(
                Result.failure(WeatherError.NoNetwork),
                Result.success(SAMPLE_WEATHER),
            ),
        )
        val viewModel = createViewModel(dataSource)

        assertTrue(viewModel.uiState.value is WeatherUiState.Error)
        viewModel.retry()
        assertEquals(WeatherUiState.Success(SAMPLE_WEATHER), viewModel.uiState.value)
    }

    @Test
    fun selectLocation_updatesCurrentLocationAndLoadsWeather() = runTest {
        val dataSource = FakeWeatherDataSource(
            mutableListOf(
                Result.success(SAMPLE_WEATHER),
                Result.success(SAMPLE_WEATHER.copy(cityName = "上海")),
            ),
        )
        val preferences = FakePreferencesStore()
        val shanghai = SavedLocation(
            name = "上海",
            latitude = 31.23,
            longitude = 121.47,
            timezone = "Asia/Shanghai",
            country = "中国",
        )
        val viewModel = createViewModel(dataSource, preferences)

        viewModel.selectLocation(shanghai)

        assertEquals(shanghai, viewModel.currentLocation.value)
        assertEquals(shanghai, preferences.savedLocation)
        assertEquals(2, dataSource.callCount)
    }

    @Test
    fun search_debounceReturnsResults() = runTest {
        val dataSource = FakeWeatherDataSource(
            weatherResults = mutableListOf(Result.success(SAMPLE_WEATHER)),
            searchResults = mutableListOf(
                Result.success(
                    listOf(
                        SavedLocation(
                            name = "上海",
                            latitude = 31.23,
                            longitude = 121.47,
                            timezone = "Asia/Shanghai",
                            country = "中国",
                        ),
                    ),
                ),
            ),
        )
        val viewModel = createViewModel(dataSource)

        viewModel.onSearchQueryChange("上海")
        advanceTimeBy(500)

        assertEquals(1, viewModel.searchUiState.value.results.size)
        assertEquals("上海", viewModel.searchUiState.value.results.first().name)
    }

    private fun createViewModel(
        dataSource: FakeWeatherDataSource,
        preferences: FakePreferencesStore = FakePreferencesStore(),
    ): WeatherViewModel = WeatherViewModel(
        dataSource = dataSource,
        preferencesStore = preferences,
        locationProvider = FakeLocationProvider,
        placeNameResolver = FakePlaceNameResolver,
    )

    private class FakeWeatherDataSource(
        private val weatherResults: MutableList<Result<WeatherInfo>>,
        private val searchResults: MutableList<Result<List<SavedLocation>>> = mutableListOf(
            Result.success(emptyList()),
        ),
    ) : WeatherDataSource {
        constructor(result: Result<WeatherInfo>) : this(mutableListOf(result))

        var callCount = 0
            private set

        override suspend fun getWeather(
            location: SavedLocation,
            includeDailyForecast: Boolean,
        ): Result<WeatherInfo> {
            callCount++
            return weatherResults.removeAt(0.coerceAtMost(weatherResults.lastIndex))
        }

        override suspend fun searchLocations(query: String): Result<List<SavedLocation>> =
            searchResults.removeAt(0.coerceAtMost(searchResults.lastIndex))
    }

    private class FakePreferencesStore : PreferencesStore {
        var savedLocation: SavedLocation? = null

        override suspend fun getCurrentLocation(): SavedLocation? = savedLocation

        override suspend fun saveCurrentLocation(location: SavedLocation) {
            savedLocation = location
        }

        override fun favoritesFlow(): Flow<List<FavoriteLocation>> = flowOf(emptyList())

        override suspend fun getFavorites(): List<FavoriteLocation> = emptyList()

        override suspend fun addFavorite(location: SavedLocation): FavoriteAddResult =
            FavoriteAddResult.Added

        override suspend fun removeFavorite(location: SavedLocation) = Unit

        override suspend fun isFavorite(location: SavedLocation): Boolean = false
    }

    private object FakeLocationProvider : DeviceLocationProvider {
        override suspend fun getCurrentCoordinates(): Result<Coordinates> =
            Result.success(Coordinates(39.9, 116.4))
    }

    private object FakePlaceNameResolver : PlaceNameResolver {
        override suspend fun resolveName(latitude: Double, longitude: Double): String? = "当前位置"
    }

    private companion object {
        val SAMPLE_WEATHER = WeatherInfo(
            cityName = "北京",
            country = "中国",
            temperatureCelsius = 30.9,
            humidityPercent = 33,
            weatherDescription = "阴",
            weatherCode = 3,
            windSpeedKmh = 6.5,
            observedAt = "2026-09-17T15:30",
        )
    }
}
