# Open-Meteo API 调研报告

> **关联 Issue**：[#2 调研 Open-Meteo API 能力与集成要点](https://github.com/Zoti321/app1/issues/2)  
> **调研日期**：2026-09-17  
> **数据来源**：仅使用 Open-Meteo 官方文档（open-meteo.com）与 WMO 官方文档（codes.wmo.int、library.wmo.int）

## 结论摘要

Open-Meteo **满足 Android MVP 需求**：地理编码 + 当前天气均可通过 HTTPS GET 获取，无需 API Key，JSON 结构清晰。学习项目属于**非商业用途**，免费额度（600 次/分钟、10,000 次/天）远超 MVP 用量。Android 端应**仅使用 HTTPS**，无需配置 cleartext traffic。`weather_code` 的英文含义以 Open-Meteo 官方文档为准；**WMO 与 Open-Meteo 均未提供中文描述 API**，需在客户端维护本地映射表。

---

## 1. 北京天气完整请求流程

### Step 1：地理编码（城市名 → 经纬度）

**请求 URL（实测 2026-09-17）：**

```
GET https://geocoding-api.open-meteo.com/v1/search?name=Beijing&count=1&language=zh
```

等效中文搜索：

```
GET https://geocoding-api.open-meteo.com/v1/search?name=北京&count=1&language=zh
```

**成功响应示例：**

```json
{
  "results": [
    {
      "id": 1816670,
      "name": "北京",
      "latitude": 39.9075,
      "longitude": 116.39723,
      "elevation": 49.0,
      "feature_code": "PPLC",
      "country_code": "CN",
      "admin1_id": 2038349,
      "admin2_id": 11876380,
      "timezone": "Asia/Shanghai",
      "population": 18960744,
      "country_id": 1814991,
      "country": "中国",
      "admin1": "北京市",
      "admin2": "北京市"
    }
  ],
  "generationtime_ms": 0.43404102
}
```

### Step 2：当前天气（经纬度 → 实时数据）

使用 Step 1 返回的 `latitude` / `longitude`：

```
GET https://api.open-meteo.com/v1/forecast?latitude=39.9075&longitude=116.39723&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m&timezone=Asia%2FShanghai
```

**成功响应示例：**

```json
{
  "latitude": 39.89455,
  "longitude": 116.35983,
  "generationtime_ms": 0.11479854583740234,
  "utc_offset_seconds": 28800,
  "timezone": "Asia/Shanghai",
  "timezone_abbreviation": "GMT+8",
  "elevation": 47.0,
  "current_units": {
    "time": "iso8601",
    "interval": "seconds",
    "temperature_2m": "°C",
    "relative_humidity_2m": "%",
    "weather_code": "wmo code",
    "wind_speed_10m": "km/h"
  },
  "current": {
    "time": "2026-09-17T15:30",
    "interval": 900,
    "temperature_2m": 30.9,
    "relative_humidity_2m": 33,
    "weather_code": 3,
    "wind_speed_10m": 6.5
  }
}
```

> **说明**：响应中的 `latitude`/`longitude` 为网格中心坐标，可能与请求坐标相差数公里（官方文档说明）。`current.interval` 为 900 秒（15 分钟），表示聚合指标的回溯窗口。

---

## 2. Geocoding API 请求/响应格式

**官方文档**：<https://open-meteo.com/en/docs/geocoding-api>

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `name` | String | **是** | — | 城市名或邮编；可用 `城市,国家` 缩小范围，如 `Paris,France` |
| `count` | Integer | 否 | 10 | 返回结果数，最大 100 |
| `language` | String | 否 | `en` | 小写语言代码；有翻译则返回本地化名称 |
| `format` | String | 否 | `json` | 可选 `protobuf` |
| `countryCode` | String | 否 | — | ISO-3166-1 alpha-2 国家代码过滤 |
| `apikey` | String | 否 | — | 仅商业订阅需要 |

### 搜索匹配规则

- **1 个字符**：无结果
- **2 个字符**：精确匹配
- **3 个及以上**：前缀匹配（不区分大小写与变音符号）
- 国家/行政区限定符须**精确匹配**（不支持前缀）

### 响应字段

| JSON 字段 | 类型 | MVP 用途 |
|-----------|------|----------|
| `results` | Array | 匹配列表；无匹配时**整段缺失** |
| `results[].name` | String | 显示城市名 |
| `results[].latitude` | Float | 传给 Forecast API |
| `results[].longitude` | Float | 传给 Forecast API |
| `results[].country` | String | 显示国家 |
| `results[].timezone` | String | 可选，传给 Forecast `timezone` |
| `results[].country_code` | String | 可选 |
| `results[].admin1` | String | 可选，省/州 |
| `generationtime_ms` | Float | 服务端性能指标，可忽略 |

---

## 3. Forecast API 请求/响应格式（MVP：当前天气）

**官方文档**：<https://open-meteo.com/en/docs>

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `latitude` | Float | **是** | — | WGS84 纬度，范围 -90 ~ 90 |
| `longitude` | Float | **是** | — | WGS84 经度 |
| `current` | String[] | 否 | — | 当前天气变量列表，逗号分隔 |
| `timezone` | String | 否 | `GMT` | IANA 时区名；`auto` 自动推断 |
| `temperature_unit` | String | 否 | `celsius` | 可选 `fahrenheit` |
| `wind_speed_unit` | String | 否 | `kmh` | 可选 `ms`、`mph`、`kn` |

### MVP 推荐 `current` 变量

| 变量 | 单位 | 说明 |
|------|------|------|
| `temperature_2m` | °C | 2 米气温 |
| `relative_humidity_2m` | % | 相对湿度 |
| `weather_code` | WMO code | 天气状况代码 |
| `wind_speed_10m` | km/h | 10 米风速 |

### 响应结构

| JSON 字段 | 类型 | 说明 |
|-----------|------|------|
| `latitude`, `longitude` | Float | 实际使用的网格中心坐标 |
| `timezone` | String | 时区标识 |
| `timezone_abbreviation` | String | 时区缩写 |
| `utc_offset_seconds` | Int | 相对 UTC 偏移秒数 |
| `elevation` | Float | 海拔（米） |
| `current` | Object | 当前天气数值 |
| `current.time` | String | ISO8601 本地时间 |
| `current.interval` | Int | 聚合窗口（秒），通常 900 |
| `current_units` | Object | 各字段单位 |

> `current` 基于 15 分钟模型数据；文档说明 hourly 中可用变量均可在 `current` 中请求。

---

## 4. 速率限制与使用条款

**官方来源**：<https://open-meteo.com/en/terms>、<https://open-meteo.com/en/pricing>

### 免费非商业 tier 限制

| 维度 | 限额 |
|------|------|
| 每分钟 | 600 次 |
| 每小时 | 5,000 次 |
| 每天 | 10,000 次 |
| 每月 | 300,000 次 |

### 非商业用途定义（符合学习项目）

- 私人或非盈利网站/应用，**无订阅、无广告**
- 个人家庭自动化
- 公立机构公开研究
- **教育内容**（本项目适用）

### 许可与归属

- 数据许可：**CC BY 4.0**（<https://open-meteo.com/en/licence>）
- 展示 Open-Meteo 数据时须注明出处，例如：
  ```html
  <a href="https://open-meteo.com/">Weather data by Open-Meteo.com</a>
  ```
- 地理编码数据基于 **GeoNames**（CC-BY）
- 滥用服务可能被**无预告封禁** IP/应用

---

## 5. WMO `weather_code` 映射

### 5.1 Open-Meteo 官方码表（英文，权威来源）

**来源**：<https://open-meteo.com/en/docs> → 「WMO Weather interpretation codes (WW)」

Open-Meteo 文档明确：`weather_code` 遵循 WMO weather interpretation codes，并在文档中给出完整映射表。

| Code | 英文描述（官方） |
|------|------------------|
| 0 | Clear sky |
| 1 | Mainly clear |
| 2 | Partly cloudy |
| 3 | Overcast |
| 45 | Fog |
| 48 | Depositing rime fog |
| 51 | Light drizzle |
| 53 | Moderate drizzle |
| 55 | Dense drizzle |
| 56 | Light freezing drizzle |
| 57 | Dense freezing drizzle |
| 61 | Slight rain |
| 63 | Moderate rain |
| 65 | Heavy rain |
| 66 | Light freezing rain |
| 67 | Heavy freezing rain |
| 71 | Slight snow fall |
| 73 | Moderate snow fall |
| 75 | Heavy snow fall |
| 77 | Snow grains |
| 80 | Slight rain showers |
| 81 | Moderate rain showers |
| 82 | Violent rain showers |
| 85 | Slight snow showers |
| 86 | Heavy snow showers |
| 95 | Thunderstorm: Slight or moderate |
| 96 | Thunderstorm with slight hail |
| 99 | Thunderstorm with heavy hail |

> \* 带冰雹的雷暴预报（96、99）仅在**中欧**可用（官方文档脚注）。

### 5.2 WMO 官方文档关系

| 资源 | URL | 与 Open-Meteo 的关系 |
|------|-----|----------------------|
| Manual on Codes, Vol I.1 (WMO-No. 306) | <https://library.wmo.int/records/item/35713-manual-on-codes-volume-i-1-international-codes> | WMO 国际码表总册 |
| Code Table 4678: Significant weather phenomena | <https://codes.wmo.int/306/4678> | w′w′ 显著天气现象（**组合式**描述，非 Open-Meteo 的单一整数码） |

**重要区别**：Open-Meteo 的 `weather_code` 是数值模型后处理生成的 **WW 解释码子集**（0–99 中的特定值），与 WMO Code Table 4678 的组合式 w′w′ 码**不是一一对应关系**。MVP 应以 **Open-Meteo 官方码表** 为映射依据，WMO 文档提供概念背景。

### 5.3 中文描述策略

- Open-Meteo **不提供** `weather_code` 的中文 API 参数（`language=zh` 仅影响地理编码地名）
- WMO Codes Registry（codes.wmo.int）**仅英文**
- **推荐做法**：在 Android 客户端维护 `Map<Int, String>` 或 `strings.xml` 资源，基于上表英文官方描述翻译为中文；下方为建议映射（非 WMO 官方中文，供 MVP 参考）：

| Code | 建议中文 |
|------|----------|
| 0 | 晴 |
| 1 | 大部晴朗 |
| 2 | 局部多云 |
| 3 | 阴 |
| 45 | 雾 |
| 48 | 雾凇 |
| 51 | 小毛毛雨 |
| 53 | 中毛毛雨 |
| 55 | 大毛毛雨 |
| 56 | 小冻毛毛雨 |
| 57 | 大冻毛毛雨 |
| 61 | 小雨 |
| 63 | 中雨 |
| 65 | 大雨 |
| 66 | 小冻雨 |
| 67 | 大冻雨 |
| 71 | 小雪 |
| 73 | 中雪 |
| 75 | 大雪 |
| 77 | 雪粒 |
| 80 | 小阵雨 |
| 81 | 中阵雨 |
| 82 | 大阵雨 |
| 85 | 小阵雪 |
| 86 | 大阵雪 |
| 95 | 雷暴 |
| 96 | 雷暴伴小冰雹 |
| 99 | 雷暴伴大冰雹 |

---

## 6. 常见 HTTP 错误场景

基于官方文档说明 + 2026-09-17 实测。

### Geocoding API

| 场景 | HTTP 状态 | 响应体 | 客户端处理建议 |
|------|-----------|--------|----------------|
| 城市无匹配 | **200** | `{"generationtime_ms":...}`，**无 `results` 字段** | 提示「未找到该城市」 |
| 单字符搜索 | **200** | 无 `results`（官方：空/单字符无结果） | 提示输入至少 2 字符 |
| 缺少 `name` 参数 | **400** | `{"error":true,"reason":"No value found ... at path 'name'..."}` | 参数校验 |
| `count` 超出 1–100 | **400** | `{"error":true,"reason":"Parameter count must be between 1 and 100."}` | 限制 count ≤ 100 |
| 无效路径 | **404** | `{"error":true,"reason":"Not Found"}` | 检查 base URL |

### Forecast API

| 场景 | HTTP 状态 | 响应体 | 客户端处理建议 |
|------|-----------|--------|----------------|
| 纬度超出 -90~90 | **400** | `{"error":true,"reason":"Latitude must be in range of -90 to 90°. Given: 999.0."}` | 校验坐标 |
| 无效 weather 变量名 | **400** | `{"error":true,"reason":"Cannot initialize WeatherVariable from invalid String value ..."}` | 检查 `current` 参数拼写 |
| 无效路径 | **404** | `{"error":true,"reason":"Not Found"}` | 检查 endpoint |
| 网络超时/5xx |  varies | — | 重试 + 错误 UI |

> **注意**：官方文档**未记录 404 用于「无效城市」**；地理编码无匹配返回 **200 + 空结果**，而非 404。

---

## 7. Android 集成要点

### 7.1 HTTPS 与 cleartext

| 项目 | 结论 |
|------|------|
| 官方 API 域名 | `https://geocoding-api.open-meteo.com`、`https://api.open-meteo.com` |
| HTTPS | **推荐使用**；实测正常，响应 `Content-Type: application/json; charset=utf-8` |
| HTTP | 实测 `http://api.open-meteo.com` 亦返回 200，但 Android 9+（API 28）默认**禁止 cleartext** |
| cleartext 配置 | **MVP 不需要**——统一使用 HTTPS URL 即可 |
| 特殊请求头 | **无**——普通 GET，无需 Authorization、API Key 或自定义 Header |
| Manifest 权限 | `INTERNET` |

### 7.2 推荐 Retrofit Base URL

```kotlin
// Geocoding
private const val GEOCODING_BASE = "https://geocoding-api.open-meteo.com/"

// Forecast
private const val FORECAST_BASE = "https://api.open-meteo.com/"
```

### 7.3 错误解析模型

两个 API 共用错误结构：

```kotlin
@Serializable
data class OpenMeteoErrorResponse(
    val error: Boolean,
    val reason: String,
)
```

---

## 8. 推荐 Kotlin 数据模型（字段名与 JSON 一致）

使用 `kotlinx.serialization`；snake_case 字段通过 `@SerialName` 或 `Json { namingStrategy = JsonNamingStrategy.SnakeCase }` 对齐。

### Geocoding

```kotlin
@Serializable
data class GeocodingResponse(
    val results: List<GeocodingResult>? = null,
    @SerialName("generationtime_ms") val generationTimeMs: Double? = null,
)

@Serializable
data class GeocodingResult(
    val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double? = null,
    @SerialName("feature_code") val featureCode: String? = null,
    @SerialName("country_code") val countryCode: String? = null,
    val timezone: String? = null,
    val population: Int? = null,
    val country: String? = null,
    val admin1: String? = null,
    val admin2: String? = null,
)
```

### Forecast（MVP 当前天气）

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
)

@Serializable
data class CurrentUnits(
    val time: String? = null,
    val interval: String? = null,
    @SerialName("temperature_2m") val temperature2m: String? = null,
    @SerialName("relative_humidity_2m") val relativeHumidity2m: String? = null,
    @SerialName("weather_code") val weatherCode: String? = null,
    @SerialName("wind_speed_10m") val windSpeed10m: String? = null,
)

@Serializable
data class CurrentWeather(
    val time: String,
    val interval: Int,
    @SerialName("temperature_2m") val temperature2m: Double,
    @SerialName("relative_humidity_2m") val relativeHumidity2m: Int? = null,
    @SerialName("weather_code") val weatherCode: Int,
    @SerialName("wind_speed_10m") val windSpeed10m: Double? = null,
)
```

---

## 9. MVP 集成检查清单

- [x] 地理编码 + 当前天气 API 可用，无需 API Key
- [x] 北京端到端 URL 已验证
- [x] 免费非商业限额充足；学习项目符合条款
- [x] 使用 HTTPS，无需 cleartext 配置
- [x] 无特殊 HTTP Header 要求
- [x] `weather_code` 映射来源明确（Open-Meteo 官方码表 + 客户端中文本地化）
- [ ] UI 中需添加 Open-Meteo 归属链接（CC BY 4.0 要求）
- [ ] 处理 geocoding 200 空结果（非 404）
- [ ] 处理 forecast/geocoding 400 错误 JSON

---

## 参考链接

| 主题 | URL |
|------|-----|
| Geocoding API 文档 | <https://open-meteo.com/en/docs/geocoding-api> |
| Weather Forecast API 文档 | <https://open-meteo.com/en/docs> |
| 使用条款 | <https://open-meteo.com/en/terms> |
| 定价与限额 | <https://open-meteo.com/en/pricing> |
| 数据许可 | <https://open-meteo.com/en/licence> |
| WMO Manual on Codes Vol I.1 | <https://library.wmo.int/records/item/35713-manual-on-codes-volume-i-1-international-codes> |
| WMO Code Table 4678 | <https://codes.wmo.int/306/4678> |
