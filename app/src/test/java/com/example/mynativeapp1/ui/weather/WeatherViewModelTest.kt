package com.example.mynativeapp1.ui.weather

import com.example.mynativeapp1.data.WeatherDataSource
import com.example.mynativeapp1.data.WeatherError
import com.example.mynativeapp1.data.WeatherInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
    fun init_loadsBeijingWeather() = runTest {
        val dataSource = FakeWeatherDataSource(Result.success(SAMPLE_WEATHER))
        val viewModel = WeatherViewModel(dataSource)

        assertEquals(WeatherUiState.Success(SAMPLE_WEATHER), viewModel.uiState.value)
        assertEquals("北京", dataSource.lastCity)
    }

    @Test
    fun loadWeather_mapsNoNetworkError() = runTest {
        val dataSource = FakeWeatherDataSource(Result.failure(WeatherError.NoNetwork))
        val viewModel = WeatherViewModel(dataSource)

        val error = viewModel.uiState.value as WeatherUiState.Error
        assertEquals("无法连接网络，请检查后重试", error.message)
    }

    @Test
    fun loadWeather_mapsApiFailureError() = runTest {
        val dataSource = FakeWeatherDataSource(Result.failure(WeatherError.ApiFailure))
        val viewModel = WeatherViewModel(dataSource)

        val error = viewModel.uiState.value as WeatherUiState.Error
        assertEquals("获取天气失败，请稍后重试", error.message)
    }

    @Test
    fun loadWeather_mapsCityNotFoundError() = runTest {
        val dataSource = FakeWeatherDataSource(Result.failure(WeatherError.CityNotFound))
        val viewModel = WeatherViewModel(dataSource)

        val error = viewModel.uiState.value as WeatherUiState.Error
        assertEquals("未找到该城市天气信息", error.message)
    }

    @Test
    fun successState_persistsWithoutAdditionalFetch() = runTest {
        val dataSource = FakeWeatherDataSource(Result.success(SAMPLE_WEATHER))
        val viewModel = WeatherViewModel(dataSource)

        assertEquals(WeatherUiState.Success(SAMPLE_WEATHER), viewModel.uiState.value)
        assertEquals(1, dataSource.callCount)

        // 模拟屏幕旋转：同一 ViewModel 实例保留，不应再次请求
        assertEquals(WeatherUiState.Success(SAMPLE_WEATHER), viewModel.uiState.value)
        assertEquals(1, dataSource.callCount)
    }

    @Test
    fun retry_afterError_reloadsWeather() = runTest {
        val dataSource = FakeWeatherDataSource(
            results = mutableListOf(
                Result.failure(WeatherError.NoNetwork),
                Result.success(SAMPLE_WEATHER),
            ),
        )
        val viewModel = WeatherViewModel(dataSource)

        assertTrue(viewModel.uiState.value is WeatherUiState.Error)

        viewModel.retry()

        assertEquals(WeatherUiState.Success(SAMPLE_WEATHER), viewModel.uiState.value)
        assertEquals(2, dataSource.callCount)
    }

    private class FakeWeatherDataSource(
        private val results: MutableList<Result<WeatherInfo>>,
    ) : WeatherDataSource {
        constructor(result: Result<WeatherInfo>) : this(mutableListOf(result))

        var callCount = 0
            private set
        var lastCity: String? = null
            private set

        override suspend fun getWeather(city: String): Result<WeatherInfo> {
            callCount++
            lastCity = city
            return results.removeAt(0)
        }
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
