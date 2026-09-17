# Open-Meteo 多日预报（Daily Forecast）API 调研报告

> **关联 Issue**：[#13 调研 Open-Meteo 多日预报 API 能力与集成要点](https://github.com/Zoti321/app1/issues/13)  
> **调研日期**：2026-09-17  
> **数据来源**：仅使用 Open-Meteo 官方文档（open-meteo.com）与 2026-09-17 实测  
> **前置文档**：[open-meteo-api.md](./open-meteo-api.md)（MVP 当前天气 + 地理编码）

## 结论摘要

Open-Meteo **多日逐日预报与 MVP 当前天气共用同一 Forecast API 端点**（`https://api.open-meteo.com/v1/forecast`），无需新 Base URL 或 API Key。通过 `daily=` 参数请求日聚合变量，配合 `forecast_days=`（默认 7，最大 16）控制预报天数。响应中 `daily` 为**平行数组结构**（`time` + 各变量数组），日期格式为 `yyyy-MM-dd`（本地时区）。**免费额度、错误 JSON 形态、HTTPS 要求与 MVP 完全一致**；建议在 Android 端将 `current` 与 `daily` 合并为单次请求以节省配额。官方文档声明指定 `daily` 时必须提供 `timezone`；实测缺省时会回退 `GMT` 并导致日期边界偏移，**生产环境务必显式传入 `timezone=auto` 或 Geocoding 返回的 IANA 时区**。

---

## 1. 与 MVP Forecast API 的关系

| 维度 | MVP（当前天气） | 多日预报（Daily） |
|------|-----------------|-------------------|
| Base URL | `https://api.open-meteo.com/` | **相同** |
| Endpoint | `/v1/forecast` | **相同** |
| 必填参数 | `latitude`, `longitude` | **相同** |
| 数据选择参数 | `current=temperature_2m,...` | `daily=weather_code,temperature_2m_max,...` |
| 预报长度 | 不涉及 | `forecast_days`（默认 7，最大 16） |
| 时区 | 推荐 `timezone=auto` 或 IANA 名 | **强烈建议**（官方文档：指定 `daily` 时 `timezone` 为必填） |
| 响应根字段 | `current`, `current_units` | 额外出现 `daily`, `daily_units` |
| 可合并请求 | — | **可与 `current` 同次 GET**（实测成功） |

> **集成策略**：在现有 `ForecastApi` 接口上扩展 query 参数即可，无需新建 Retrofit Service 或第二 Base URL。一次请求同时返回「当前天气 + 7 日预报」，计为 **1 次 API 调用**（对免费 tier 限额友好）。

---

## 2. 北京 7 日预报完整请求示例

沿用 MVP 地理编码得到的坐标（39.9075, 116.39723），时区 `Asia/Shanghai`：

**请求 URL（实测 2026-09-17）：**

```
GET https://api.open-meteo.com/v1/forecast?latitude=39.9075&longitude=116.39723&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,wind_speed_10m_max&forecast_days=7&timezone=Asia%2FShanghai
```

**成功响应示例（格式化）：**

```json
{
  "latitude": 39.89455,
  "longitude": 116.35983,
  "generationtime_ms": 0.14829635620117188,
  "utc_offset_seconds": 28800,
  "timezone": "Asia/Shanghai",
  "timezone_abbreviation": "GMT+8",
  "elevation": 47.0,
  "daily_units": {
    "time": "iso8601",
    "weather_code": "wmo code",
    "temperature_2m_max": "°C",
    "temperature_2m_min": "°C",
    "precipitation_sum": "mm",
    "wind_speed_10m_max": "km/h"
  },
  "daily": {
    "time": [
      "2026-09-17", "2026-09-18", "2026-09-19", "2026-09-20",
      "2026-09-21", "2026-09-22", "2026-09-23"
    ],
    "weather_code": [3, 51, 51, 3, 3, 53, 3],
    "temperature_2m_max": [31.1, 29.5, 28.3, 30.8, 31.0, 27.6, 28.7],
    "temperature_2m_min": [20.5, 21.4, 20.0, 21.1, 20.8, 20.8, 20.0],
    "precipitation_sum": [0.00, 0.20, 0.20, 0.00, 0.00, 4.80, 0.00],
    "wind_speed_10m_max": [10.0, 9.7, 9.7, 8.5, 10.8, 7.4, 9.8]
  }
}
```

### 合并 current + daily 的单次请求（推荐）

```
GET https://api.open-meteo.com/v1/forecast?latitude=39.9075&longitude=116.39723&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum&forecast_days=7&timezone=Asia%2FShanghai
```

响应同时包含 `current` 与 `daily` 对象（实测 2026-09-17 正常）。

---

## 3. 请求参数

**官方文档**：<https://open-meteo.com/en/docs> → 「API Documentation」「Daily Parameter Definition」

### 3.1 通用参数（与 MVP 共用）

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `latitude` | Float | **是** | — | WGS84 纬度，-90 ~ 90 |
| `longitude` | Float | **是** | — | WGS84 经度 |
| `timezone` | String | 见注 | `GMT` | IANA 时区；`auto` 按坐标推断。官方：**指定 `daily` 时必填** |
| `temperature_unit` | String | 否 | `celsius` | 可选 `fahrenheit` |
| `wind_speed_unit` | String | 否 | `kmh` | 可选 `ms`、`mph`、`kn` |
| `precipitation_unit` | String | 否 | `mm` | 可选 `inch` |
| `timeformat` | String | 否 | `iso8601` | 可选 `unixtime`（daily 日期需再应用 `utc_offset_seconds`） |
| `apikey` | String | 否 | — | 仅商业订阅需要 |

> **时区实测差异**：省略 `timezone` 时 API 仍返回 200，但 `timezone` 为 `GMT`，`daily.time` 按 UTC 日界划分，与北京本地日期可能不一致。客户端应始终传入 Geocoding 结果的 `timezone` 或 `timezone=auto`。

### 3.2 多日预报专用参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `daily` | String[] | **是**（若要日数据） | — | 逗号分隔的日聚合变量列表 |
| `forecast_days` | Integer | 否 | **7** | 预报天数，范围 **0 ~ 16** |
| `past_days` | Integer | 否 | 0 | 附加返回过去 N 天（0–92），可与 `forecast_days` 组合 |
| `start_date` / `end_date` | String | 否 | — | ISO8601 日期（`yyyy-mm-dd`），替代 `forecast_days` 指定区间 |

### 3.3 预报天数支持

| 设置 | 行为 |
|------|------|
| 默认（省略 `forecast_days`） | 返回 **7 天** daily 数据 |
| `forecast_days=16` | 返回 **16 天**（实测数组长度 16） |
| `forecast_days=0` | 返回 **空数组** `{"time":[],"weather_code":[]}`（HTTP 200） |
| `forecast_days` > 16 | HTTP **400**，见 §6 错误场景 |

底层天气模型预报长度因模型而异（GFS 16 天、ICON 7.5 天等），API 通过多模型融合提供最长 16 天统一接口。

---

## 4. 推荐 `daily` 变量（UI 场景）

官方说明：daily 值为 hourly 数据的 **24 小时简单聚合**。

### 4.1 推荐用于天气 App 列表/卡片

| 变量 | 单位 | 说明 |
|------|------|------|
| `weather_code` | WMO code | **当日最严重**天气状况（与 MVP `current.weather_code` 码表相同） |
| `temperature_2m_max` | °C | 日最高温 |
| `temperature_2m_min` | °C | 日最低温 |
| `precipitation_sum` | mm | 日总降水量（雨 + 阵性降水 + 雪） |
| `precipitation_probability_max` | % | 日最大降水概率 |
| `wind_speed_10m_max` | km/h | 日最大风速 |
| `sunrise` / `sunset` | iso8601 | 日出日落（本地时区） |

### 4.2 完整可用 daily 变量（官方列表）

| 变量 | 单位 | 说明 |
|------|------|------|
| `temperature_2m_max` / `_mean` / `_min` | °C | 2 米气温 |
| `apparent_temperature_max` / `_mean` / `_min` | °C | 体感温度 |
| `precipitation_sum` | mm | 总降水 |
| `rain_sum` | mm | 雨量 |
| `showers_sum` | mm | 阵性降水 |
| `snowfall_sum` | cm | 降雪量 |
| `precipitation_hours` | hours | 有降水的小时数 |
| `precipitation_probability_max` / `_mean` / `_min` | % | 降水概率 |
| `weather_code` | WMO code | 最严重天气 |
| `sunrise` / `sunset` | iso8601 | 日出日落 |
| `sunshine_duration` | seconds | 日照时长（WMO 定义） |
| `daylight_duration` | seconds | 白昼时长 |
| `wind_speed_10m_max` / `wind_gusts_10m_max` | km/h | 最大风速/阵风 |
| `wind_direction_10m_dominant` | ° | 主导风向 |
| `shortwave_radiation_sum` | MJ/m² | 日太阳辐射总量 |
| `et0_fao_evapotranspiration` | mm | 参考蒸散量 |
| `uv_index_max` / `uv_index_clear_sky_max` | Index | UV 指数 |

---

## 5. 响应结构与关键 JSON 字段

### 5.1 根级字段

| JSON 字段 | 类型 | 说明 |
|-----------|------|------|
| `latitude`, `longitude` | Float | 网格中心坐标（可能与请求坐标偏差数公里） |
| `elevation` | Float | 海拔（米） |
| `timezone` | String | IANA 时区 |
| `timezone_abbreviation` | String | 时区缩写 |
| `utc_offset_seconds` | Int | 相对 UTC 偏移秒数 |
| `generationtime_ms` | Float | 服务端生成耗时 |
| `daily` | Object | 日预报数据（平行数组） |
| `daily_units` | Object | 各 daily 变量单位 |
| `current` | Object | 若同次请求含 `current=`，结构与 MVP 相同 |

### 5.2 `daily` 对象结构

每个请求的 daily 变量对应**等长数组**，索引 `i` 为同一天：

```json
"daily": {
  "time": ["2026-09-17", "2026-09-18", ...],
  "weather_code": [3, 51, ...],
  "temperature_2m_max": [31.1, 29.5, ...],
  "temperature_2m_min": [20.5, 21.4, ...]
}
```

| 字段 | 格式 | 注意 |
|------|------|------|
| `daily.time` | `yyyy-MM-dd` | 本地时区日期；**非**带时分秒的 ISO8601 |
| `daily.weather_code` | Int | 复用 MVP WMO 码表与中文映射 |
| `daily.temperature_2m_max/min` | Float | 可空值在 API 中或为 `null`（极端情况） |
| `daily.sunrise/sunset` | String | 完整 ISO8601 本地时间戳 |

### 5.3 客户端映射建议

将平行数组转为 `List<DailyForecast>`：

```kotlin
fun ForecastResponse.toDailyForecasts(): List<DailyForecast> {
    val d = daily ?: return emptyList()
    return d.time.indices.map { i ->
        DailyForecast(
            date = d.time[i],
            weatherCode = d.weatherCode[i],
            tempMax = d.temperature2mMax[i],
            tempMin = d.temperature2mMin[i],
            precipitationSum = d.precipitationSum?.getOrNull(i),
        )
    }
}
```

---

## 6. 常见 HTTP 错误场景

基于官方文档 + 2026-09-17 实测。错误体与 MVP **相同结构**：`{"error": true, "reason": "..."}`。

### Forecast API（Daily 相关）

| 场景 | HTTP 状态 | 响应体示例 | 客户端处理 |
|------|-----------|------------|------------|
| 无效 daily 变量名 | **400** | `{"error":true,"reason":"Invalid value: Cannot initialize ForecastVariableDaily from invalid String value tempeture_2m_max"}` | 检查 `daily` 拼写 |
| `forecast_days` 超出 0–16 | **400** | `{"error":true,"reason":"Forecast days is invalid. Allowed range 0 to 16. Given 16."}` | 限制 ≤ 16 |
| 纬度超出 -90~90 | **400** | 同 MVP | 校验坐标 |
| 无效路径 | **404** | `{"error":true,"reason":"Not Found"}` | 检查 endpoint |
| 网络超时 / 5xx | varies | — | 重试 + 错误 UI |

> **与 MVP 差异**：hourly/current 无效变量报 `Cannot initialize WeatherVariable...`；daily 无效变量报 `Cannot initialize ForecastVariableDaily...`，reason 前缀略有不同，但解析模型可复用 `OpenMeteoErrorResponse`。

> **注意**：`forecast_days=0` **不是错误**，返回空 daily 数组；UI 层应将此视为无预报数据。

---

## 7. 速率限制与使用条款

与 MVP **完全一致**，详见 [open-meteo-api.md §4](./open-meteo-api.md#4-速率限制与使用条款)。

| 维度 | 免费非商业 tier |
|------|-----------------|
| 每分钟 | 600 次 |
| 每小时 | 5,000 次 |
| 每天 | 10,000 次 |
| 每月 | 300,000 次 |

**多日预报不单独计费**：合并 `current` + `daily` 的单次 GET 仍计 **1 次**请求。学习项目符合非商业条款；须保留 Open-Meteo 归属（CC BY 4.0）。

**官方来源**：<https://open-meteo.com/en/terms>、<https://open-meteo.com/en/pricing>

---

## 8. Android / Kotlin 集成要点

### 8.1 扩展 Retrofit 接口

在现有 Forecast 请求上追加参数，无需新 Service：

```kotlin
@GET("v1/forecast")
suspend fun getForecast(
    @Query("latitude") latitude: Double,
    @Query("longitude") longitude: Double,
    @Query("current") current: String? = null,
    @Query("daily") daily: String? = null,
    @Query("forecast_days") forecastDays: Int = 7,
    @Query("timezone") timezone: String = "auto",
    @Query("temperature_unit") temperatureUnit: String = "celsius",
    @Query("wind_speed_unit") windSpeedUnit: String = "kmh",
): ForecastResponse
```

推荐常量：

```kotlin
const val DAILY_VARS = "weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,wind_speed_10m_max"
const val CURRENT_VARS = "temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m"
const val DEFAULT_FORECAST_DAYS = 7
```

### 8.2 扩展数据模型

在 MVP `ForecastResponse` 上追加 optional `daily` / `daily_units`：

```kotlin
@Serializable
data class ForecastResponse(
    val latitude: Double,
    val longitude: Double,
    @SerialName("generationtime_ms") val generationTimeMs: Double,
    @SerialName("utc_offset_seconds") val utcOffsetSeconds: Int,
    val timezone: String,
    @SerialName("timezone_abbreviation") val timezoneAbbreviation: String,
    val elevation: Double,
    @SerialName("current_units") val currentUnits: CurrentUnits? = null,
    val current: CurrentWeather? = null,
    @SerialName("daily_units") val dailyUnits: DailyUnits? = null,
    val daily: DailyWeather? = null,
)

@Serializable
data class DailyWeather(
    val time: List<String>,
    @SerialName("weather_code") val weatherCode: List<Int>,
    @SerialName("temperature_2m_max") val temperature2mMax: List<Double>,
    @SerialName("temperature_2m_min") val temperature2mMin: List<Double>,
    @SerialName("precipitation_sum") val precipitationSum: List<Double>? = null,
    @SerialName("wind_speed_10m_max") val windSpeed10mMax: List<Double>? = null,
    val sunrise: List<String>? = null,
    val sunset: List<String>? = null,
)

@Serializable
data class DailyUnits(
    val time: String? = null,
    @SerialName("weather_code") val weatherCode: String? = null,
    @SerialName("temperature_2m_max") val temperature2mMax: String? = null,
    @SerialName("temperature_2m_min") val temperature2mMin: String? = null,
    @SerialName("precipitation_sum") val precipitationSum: String? = null,
)
```

### 8.3 其他集成注意事项

| 项目 | 结论 |
|------|------|
| HTTPS / cleartext | 同 MVP，仅 HTTPS，无需 cleartext 配置 |
| API Key | 非商业学习项目**不需要** |
| 错误解析 | 复用 `OpenMeteoErrorResponse` |
| `weather_code` 中文 | 复用 MVP 客户端映射表（Open-Meteo 不提供中文 API） |
| 日期展示 | `daily.time` 为日期字符串，可直接解析为 `LocalDate` |
| 缓存策略 | 7 日预报可缓存 30–60 分钟（模型数小时更新一次） |
| 配额优化 | **合并** `current` + `daily` 为单次请求 |

---

## 9. 集成检查清单

- [x] Daily 预报与 MVP 共用 `/v1/forecast` 端点
- [x] `forecast_days` 默认 7、最大 16 已验证
- [x] 北京 7 日预报 URL 与响应已实测
- [x] `current` + `daily` 合并请求已实测
- [x] 错误 JSON 形态与 MVP 一致（`error` + `reason`）
- [x] 免费 tier 限额与 MVP 相同
- [ ] UI 传入正确 `timezone`（避免 GMT 日界偏移）
- [ ] 扩展 `ForecastResponse` 数据模型
- [ ] 复用 `weather_code` 中文映射
- [ ] 处理 `forecast_days=0` 或空数组边界情况
- [ ] 保留 Open-Meteo 归属链接（CC BY 4.0）

---

## 参考链接

| 主题 | URL |
|------|-----|
| Weather Forecast API（含 Daily） | <https://open-meteo.com/en/docs> |
| MVP 调研（Geocoding + Current） | [open-meteo-api.md](./open-meteo-api.md) |
| 使用条款 | <https://open-meteo.com/en/terms> |
| 定价与限额 | <https://open-meteo.com/en/pricing> |
