package com.example.mynativeapp1.data

import com.example.mynativeapp1.data.remote.OpenMeteoApiFactory
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.HttpURLConnection

class WeatherRepositoryTest {

    private lateinit var geocodingServer: MockWebServer
    private lateinit var forecastServer: MockWebServer
    private lateinit var repository: WeatherRepository

    @Before
    fun setUp() {
        geocodingServer = MockWebServer()
        forecastServer = MockWebServer()
        geocodingServer.start()
        forecastServer.start()

        val api = OpenMeteoApiFactory.createOpenMeteoApi(
            geocodingBaseUrl = geocodingServer.url("/").toString(),
            forecastBaseUrl = forecastServer.url("/").toString(),
        )
        repository = WeatherRepository(api)
    }

    @After
    fun tearDown() {
        geocodingServer.shutdown()
        forecastServer.shutdown()
    }

    @Test
    fun getWeather_returnsWeatherInfo_whenForecastSucceeds() = runTest {
        forecastServer.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody(BEIJING_FORECAST_JSON),
        )

        val result = repository.getWeather(SavedLocation.DEFAULT_BEIJING, includeDailyForecast = false)

        assertTrue(result.isSuccess)
        val weather = result.getOrThrow()
        assertEquals("北京", weather.cityName)
        assertEquals(30.9, weather.temperatureCelsius, 0.01)
        assertTrue(weather.dailyForecasts.isEmpty())
    }

    @Test
    fun getWeather_returnsDailyForecasts_whenRequested() = runTest {
        forecastServer.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody(BEIJING_FORECAST_WITH_DAILY_JSON),
        )

        val result = repository.getWeather(SavedLocation.DEFAULT_BEIJING, includeDailyForecast = true)

        assertTrue(result.isSuccess)
        val weather = result.getOrThrow()
        assertEquals(2, weather.dailyForecasts.size)
        assertEquals("2026-09-17", weather.dailyForecasts.first().date)
        assertEquals("阴", weather.dailyForecasts.first().weatherDescription)
    }

    @Test
    fun searchLocations_returnsResults_whenGeocodeSucceeds() = runTest {
        geocodingServer.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody(BEIJING_GEOCODING_JSON),
        )

        val result = repository.searchLocations("北京")

        assertTrue(result.isSuccess)
        assertEquals("北京", result.getOrThrow().first().name)
    }

    @Test
    fun searchLocations_returnsCityNotFound_whenGeocodeHasNoResults() = runTest {
        geocodingServer.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody("""{"generationtime_ms":0.5}"""),
        )

        val result = repository.searchLocations("不存在城市")

        assertTrue(result.isFailure)
        assertEquals(WeatherError.CityNotFound, result.exceptionOrNull())
    }

    @Test
    fun getWeather_returnsNoNetwork_whenConnectionFails() = runTest {
        forecastServer.shutdown()

        val api = OpenMeteoApiFactory.createOpenMeteoApi(
            geocodingBaseUrl = geocodingServer.url("/").toString(),
            forecastBaseUrl = forecastServer.url("/").toString(),
        )
        val offlineRepository = WeatherRepository(api)

        val result = offlineRepository.getWeather(SavedLocation.DEFAULT_BEIJING)

        assertTrue(result.isFailure)
        assertEquals(WeatherError.NoNetwork, result.exceptionOrNull())
    }

    private companion object {
        const val BEIJING_GEOCODING_JSON = """
            {
              "results": [
                {
                  "id": 1816670,
                  "name": "北京",
                  "latitude": 39.9075,
                  "longitude": 116.39723,
                  "country": "中国",
                  "timezone": "Asia/Shanghai"
                }
              ],
              "generationtime_ms": 0.434
            }
        """

        const val BEIJING_FORECAST_JSON = """
            {
              "latitude": 39.89455,
              "longitude": 116.35983,
              "generationtime_ms": 0.11479854583740234,
              "utc_offset_seconds": 28800,
              "timezone": "Asia/Shanghai",
              "timezone_abbreviation": "GMT+8",
              "elevation": 47.0,
              "current": {
                "time": "2026-09-17T15:30",
                "interval": 900,
                "temperature_2m": 30.9,
                "relative_humidity_2m": 33,
                "weather_code": 3,
                "wind_speed_10m": 6.5
              }
            }
        """

        const val BEIJING_FORECAST_WITH_DAILY_JSON = """
            {
              "latitude": 39.89455,
              "longitude": 116.35983,
              "generationtime_ms": 0.11,
              "utc_offset_seconds": 28800,
              "timezone": "Asia/Shanghai",
              "timezone_abbreviation": "GMT+8",
              "elevation": 47.0,
              "current": {
                "time": "2026-09-17T15:30",
                "interval": 900,
                "temperature_2m": 30.9,
                "relative_humidity_2m": 33,
                "weather_code": 3,
                "wind_speed_10m": 6.5
              },
              "daily": {
                "time": ["2026-09-17", "2026-09-18"],
                "weather_code": [3, 51],
                "temperature_2m_max": [31.1, 29.5],
                "temperature_2m_min": [20.5, 21.4],
                "precipitation_sum": [0.0, 0.2]
              }
            }
        """
    }
}
