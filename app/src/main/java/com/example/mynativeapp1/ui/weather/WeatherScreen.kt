package com.example.mynativeapp1.ui.weather

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mynativeapp1.data.DailyForecast
import com.example.mynativeapp1.data.FavoriteLocation
import com.example.mynativeapp1.data.SavedLocation
import com.example.mynativeapp1.data.WeatherInfo
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle()
    val searchUiState by viewModel.searchUiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isCurrentLocationFavorite.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showSearchSheet by remember { mutableStateOf(false) }
    var showFavoritesSheet by remember { mutableStateOf(false) }
    var hasRequestedLocationPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions.values.any { it }
        val activity = context as? androidx.activity.ComponentActivity
        val shouldShowRationale = activity?.let {
            androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                it,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
            )
        } ?: false
        val permanentlyDenied = !granted &&
            hasRequestedLocationPermission &&
            !shouldShowRationale
        hasRequestedLocationPermission = true
        viewModel.onLocationPermissionResult(
            granted = granted,
            permanentlyDenied = permanentlyDenied,
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is WeatherUiEvent.Snackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = event.actionLabel,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onSnackbarAction(event.action)
                    }
                }
                WeatherUiEvent.OpenAppSettings -> {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        ),
                    )
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = currentLocation.name) },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.onMyLocationRequested(
                                hasPermission = hasLocationPermission(context),
                                requestPermission = {
                                    hasRequestedLocationPermission = true
                                    permissionLauncher.launch(locationPermissions())
                                },
                            )
                        },
                        modifier = Modifier.semantics { contentDescription = "使用当前位置" },
                    ) {
                        Icon(Icons.Outlined.MyLocation, contentDescription = null)
                    }
                    IconButton(
                        onClick = { showFavoritesSheet = true },
                        modifier = Modifier.semantics { contentDescription = "收藏地点列表" },
                    ) {
                        Icon(Icons.Outlined.Bookmark, contentDescription = null)
                    }
                    IconButton(
                        onClick = viewModel::toggleFavorite,
                        modifier = Modifier.semantics {
                            contentDescription = if (isFavorite) {
                                "取消收藏当前地点"
                            } else {
                                "收藏当前地点"
                            }
                        },
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = null,
                            tint = if (isFavorite) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                    IconButton(
                        onClick = { showSearchSheet = true },
                        modifier = Modifier.semantics { contentDescription = "搜索地点" },
                    ) {
                        Icon(Icons.Outlined.Search, contentDescription = null)
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            when (val state = uiState) {
                WeatherUiState.Loading -> LoadingContent()
                is WeatherUiState.Success -> PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = viewModel::refreshWeather,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    SuccessContent(weather = state.weather)
                }
                is WeatherUiState.Error -> ErrorContent(
                    message = state.message,
                    onRetry = viewModel::retry,
                )
            }
        }
    }

    if (showSearchSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSearchSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            SearchSheetContent(
                searchUiState = searchUiState,
                onQueryChange = viewModel::onSearchQueryChange,
                onRetrySearch = viewModel::retrySearch,
                onSelectLocation = { location ->
                    showSearchSheet = false
                    viewModel.selectLocation(location)
                },
            )
        }
    }

    if (showFavoritesSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFavoritesSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            FavoritesSheetContent(
                favorites = favorites,
                onSelectFavorite = { favorite ->
                    showFavoritesSheet = false
                    viewModel.selectFavorite(favorite)
                },
                onRemoveFavorite = viewModel::removeFavorite,
            )
        }
    }
}

@Composable
private fun SearchSheetContent(
    searchUiState: WeatherViewModel.SearchUiState,
    onQueryChange: (String) -> Unit,
    onRetrySearch: () -> Unit,
    onSelectLocation: (SavedLocation) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "搜索地点",
            style = MaterialTheme.typography.titleMedium,
        )
        OutlinedTextField(
            value = searchUiState.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("搜索城市或地区") },
            singleLine = true,
        )
        when {
            searchUiState.isSearching -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            }
            searchUiState.inlineMessage != null && searchUiState.results.isEmpty() -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = searchUiState.inlineMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    if (searchUiState.showRetry) {
                        TextButton(onClick = onRetrySearch) {
                            Text("重试")
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(searchUiState.results, key = { it.locationKey() }) { location ->
                        TextButton(
                            onClick = { onSelectLocation(location) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = location.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                if (location.subtitle().isNotBlank()) {
                                    Text(
                                        text = location.subtitle(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoritesSheetContent(
    favorites: List<FavoriteLocation>,
    onSelectFavorite: (FavoriteLocation) -> Unit,
    onRemoveFavorite: (FavoriteLocation) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = "收藏地点",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        if (favorites.isEmpty()) {
            Text(
                text = "暂无收藏地点",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp),
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(favorites, key = { it.locationKey() }) { favorite ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = { onSelectFavorite(favorite) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = favorite.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                if (favorite.subtitle().isNotBlank()) {
                                    Text(
                                        text = favorite.subtitle(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = { onRemoveFavorite(favorite) },
                            modifier = Modifier.semantics { contentDescription = "删除收藏" },
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .semantics {
                contentDescription = "正在加载天气数据"
                liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Text(
            text = "加载中…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SuccessContent(
    weather: WeatherInfo,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = WeatherIconMapper.iconFor(weather.weatherCode),
            contentDescription = weather.weatherDescription,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Text(
            text = "${weather.temperatureCelsius.roundToInt()}°",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics {
                contentDescription = "当前温度 ${weather.temperatureCelsius.roundToInt()} 度"
            },
        )

        Text(
            text = weather.weatherDescription,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        WeatherMetricsCard(weather = weather)

        if (weather.dailyForecasts.isNotEmpty()) {
            DailyForecastSection(forecasts = weather.dailyForecasts)
        }

        Text(
            text = WeatherAttribution.TEXT,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics {
                contentDescription = WeatherAttribution.CONTENT_DESCRIPTION
            },
        )
    }
}

@Composable
private fun DailyForecastSection(
    forecasts: List<DailyForecast>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "未来7天",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
        ) {
            itemsIndexed(forecasts, key = { _, item -> item.date }) { index, forecast ->
                DailyForecastCard(forecast = forecast, index = index)
            }
        }
    }
}

@Composable
private fun DailyForecastCard(
    forecast: DailyForecast,
    index: Int,
    modifier: Modifier = Modifier,
) {
    val maxTemp = forecast.temperatureMaxCelsius.roundToInt()
    val minTemp = forecast.temperatureMinCelsius.roundToInt()
    Surface(
        modifier = modifier
            .width(72.dp)
            .semantics {
                contentDescription = ForecastDayLabel.contentDescription(
                    date = forecast.date,
                    index = index,
                    maxTemp = maxTemp,
                    minTemp = minTemp,
                    description = forecast.weatherDescription,
                )
            },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = ForecastDayLabel.labelFor(forecast.date, index),
                style = MaterialTheme.typography.labelMedium,
            )
            Icon(
                imageVector = WeatherIconMapper.iconFor(forecast.weatherCode),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "$maxTemp° / $minTemp°",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (forecast.precipitationSumMm > 0) {
                Text(
                    text = "${formatPrecipitation(forecast.precipitationSumMm)}mm",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun WeatherMetricsCard(
    weather: WeatherInfo,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            MetricItem(
                icon = Icons.Outlined.WaterDrop,
                label = "湿度",
                value = weather.humidityPercent?.let { "$it%" } ?: "—",
            )
            MetricItem(
                icon = Icons.Outlined.Air,
                label = "风速",
                value = weather.windSpeedKmh?.let { "${formatWindSpeed(it)} km/h" } ?: "—",
            )
        }
    }
}

@Composable
private fun MetricItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "$label $value"
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .semantics {
                liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.CloudOff,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { contentDescription = message },
        )
        FilledTonalButton(
            onClick = onRetry,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .semantics { contentDescription = "重试加载天气" },
        ) {
            Text(text = "重试")
        }
    }
}

private fun formatWindSpeed(windSpeedKmh: Double): String =
    if (windSpeedKmh % 1.0 == 0.0) windSpeedKmh.toInt().toString() else windSpeedKmh.toString()

private fun formatPrecipitation(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

private fun locationPermissions(): Array<String> = arrayOf(
    android.Manifest.permission.ACCESS_COARSE_LOCATION,
    android.Manifest.permission.ACCESS_FINE_LOCATION,
)

private fun hasLocationPermission(context: android.content.Context): Boolean {
    val fine = androidx.core.content.ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    val coarse = androidx.core.content.ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    return fine || coarse
}
