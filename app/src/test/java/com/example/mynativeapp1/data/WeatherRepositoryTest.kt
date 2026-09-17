package com.example.mynativeapp1.data

import com.example.mynativeapp1.data.remote.NetworkModule
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

        val api = NetworkModule.createOpenMeteoApi(
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
    fun getWeather_returnsWeatherInfo_whenGeocodeAndForecastSucceed() = runTest {
        geocodingServer.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody(BEIJING_GEOCODING_JSON),
        )
        forecastServer.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody(BEIJING_FORECAST_JSON),
        )

        val result = repository.getWeather("北京")

        assertTrue(result.isSuccess)
        val weather = result.getOrThrow()
        assertEquals("北京", weather.cityName)
        assertEquals("中国", weather.country)
        assertEquals(30.9, weather.temperatureCelsius, 0.01)
        assertEquals(33, weather.humidityPercent)
        assertEquals("阴", weather.weatherDescription)
        assertEquals(3, weather.weatherCode)
        assertEquals(6.5, weather.windSpeedKmh!!, 0.01)
        assertEquals("2026-09-17T15:30", weather.observedAt)
    }

    @Test
    fun getWeather_returnsCityNotFound_whenGeocodeHasNoResults() = runTest {
        geocodingServer.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody("""{"generationtime_ms":0.5}"""),
        )

        val result = repository.getWeather("不存在城市")

        assertTrue(result.isFailure)
        assertEquals(WeatherError.CityNotFound, result.exceptionOrNull())
    }

    @Test
    fun getWeather_returnsApiFailure_whenGeocodeReturnsHttpError() = runTest {
        geocodingServer.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST)
                .setBody("""{"error":true,"reason":"Parameter count must be between 1 and 100."}"""),
        )

        val result = repository.getWeather("北京")

        assertTrue(result.isFailure)
        assertEquals(WeatherError.ApiFailure, result.exceptionOrNull())
    }

    @Test
    fun getWeather_returnsNoNetwork_whenConnectionFails() = runTest {
        geocodingServer.shutdown()

        val api = NetworkModule.createOpenMeteoApi(
            geocodingBaseUrl = geocodingServer.url("/").toString(),
            forecastBaseUrl = forecastServer.url("/").toString(),
        )
        val offlineRepository = WeatherRepository(api)

        val result = offlineRepository.getWeather("北京")

        assertTrue(result.isFailure)
        assertEquals(WeatherError.NoNetwork, result.exceptionOrNull())
    }

    @Test
    fun getWeather_returnsApiFailure_whenForecastResponseIsMissingCurrent() = runTest {
        geocodingServer.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody(BEIJING_GEOCODING_JSON),
        )
        forecastServer.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody(
                    """
                    {
                      "latitude": 39.89455,
                      "longitude": 116.35983,
                      "generationtime_ms": 0.1,
                      "utc_offset_seconds": 28800,
                      "timezone": "Asia/Shanghai",
                      "timezone_abbreviation": "GMT+8",
                      "elevation": 47.0
                    }
                    """.trimIndent(),
                ),
        )

        val result = repository.getWeather("北京")

        assertTrue(result.isFailure)
        assertEquals(WeatherError.ApiFailure, result.exceptionOrNull())
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
    }
}
