# 天气 App（原生 Android 学习项目）

> **路线图**：[天气 App v2 — 路线图 (#11)](https://github.com/Zoti321/app1/issues/11) — ✅ 已交付并关闭


通过开发一个调用公开 API 的天气应用，系统学习原生 Android 开发。项目基于 **Kotlin + Jetpack Compose + Material 3**，从零开始逐步引入网络请求、状态管理、权限与架构分层。

## 项目目标

- 调用公开天气 API，展示**当前城市**与**实时天气**
- 理解 Android 应用的基本结构（Activity、Manifest、资源）
- 掌握 Jetpack Compose 声明式 UI 与状态驱动刷新
- 学会使用协程发起异步网络请求并处理加载 / 错误状态
- 实践 `ViewModel` + Repository 分层，为后续扩展打基础

## 技术栈

| 类别 | 选型 |
|------|------|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 网络 | Retrofit + OkHttp |
| JSON | kotlinx.serialization |
| 异步 | Kotlin Coroutines |
| 架构 | MVVM（UI → ViewModel → Repository） |
| 最低 SDK | 26（Android 8.0） |

## 功能范围

### MVP（第一版）— 已锁定 ([#3](https://github.com/Zoti321/app1/issues/3))

- [x] 预设固定城市「北京」，启动即加载（不含搜索、GPS）
- [x] 显示：城市名、温度、天气描述、湿度、风速、Open-Meteo 出处标注
- [x] 加载中 / 加载失败 / 成功 三种 UI 状态；失败态提供「重试」
- [x] 屏幕旋转后数据不丢失（ViewModel 保留）
- [x] **不含**：GPS、城市搜索、本地数据库、多城市收藏（v2.1 起含下拉刷新，见 [#12](https://github.com/Zoti321/app1/issues/12)）

### MVP 验收清单 ([#10](https://github.com/Zoti321/app1/issues/10))

- [x] 联网启动：Loading → 显示北京天气
- [x] 断网或请求失败：显示「无法连接网络，请检查后重试」，重试可恢复
- [x] API/解析失败：显示「获取天气失败，请稍后重试」
- [x] 屏幕旋转后不崩溃，天气数据由 ViewModel 保留
- [x] Loading 区域 `liveRegion = Polite`；温度、湿度/风速、重试按钮具备 `contentDescription`
- [x] Open-Meteo 出处标注可见（CC BY 4.0）

### v2 扩展（已实现 — [#11](https://github.com/Zoti321/app1/issues/11)）

- [x] 搜索城市并切换展示（[#19](https://github.com/Zoti321/app1/issues/19)）
- [x] 未来几天天气预报（[#20](https://github.com/Zoti321/app1/issues/20)）
- [x] 深色模式（[#20](https://github.com/Zoti321/app1/issues/20)）
- [x] 多城市收藏与 DataStore 持久化（[#21](https://github.com/Zoti321/app1/issues/21)）
- [x] GPS 定位（[#21](https://github.com/Zoti321/app1/issues/21)）

领域术语见 [`CONTEXT.md`](CONTEXT.md)；MVP 架构决策见 [`docs/adr/`](docs/adr/)。

## 公开 API 方案

本项目采用 **[Open-Meteo](https://open-meteo.com/)** 作为数据源：

- **免费、无需注册 API Key**，适合初学者快速上手
- 提供地理编码（城市名 → 经纬度）与天气预报接口
- 返回 JSON，结构清晰，便于练习数据模型与解析

### 1. 地理编码（按城市名查询）

```
GET https://geocoding-api.open-meteo.com/v1/search?name={城市名}&count=1&language=zh&format=json
```

示例：`name=北京`

关键响应字段：

| 字段 | 含义 |
|------|------|
| `results[0].name` | 城市名 |
| `results[0].latitude` | 纬度 |
| `results[0].longitude` | 经度 |
| `results[0].country` | 国家 |

### 2. 当前天气（按经纬度查询）

```
GET https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lon}&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m&timezone=auto
```

关键响应字段（`current` 对象内）：

| 字段 | 含义 |
|------|------|
| `temperature_2m` | 温度（°C） |
| `relative_humidity_2m` | 相对湿度（%） |
| `weather_code` | WMO 天气代码（需映射为中文描述） |
| `wind_speed_10m` | 风速（km/h） |

> **备选方案：** [OpenWeatherMap](https://openweathermap.org/api) 需免费注册 API Key，适合第二版练习密钥管理与 `local.properties` 配置。

## UI 设计 — 已锁定 ([#5](https://github.com/Zoti321/app1/issues/5))

- Hero 居中布局、Sky 固定主题、Material Icons
- 线框与 token：[`docs/design/weather-screen-prototype.md`](docs/design/weather-screen-prototype.md)
- 设计系统：[`design-system/weather-app/MASTER.md`](design-system/weather-app/MASTER.md)

## 架构设计 — 已锁定 ([#4](https://github.com/Zoti321/app1/issues/4))

- **DI**：MVP 手动构造，无 Hilt/Koin；`ViewModelProvider.Factory` 内 wiring
- **ViewModel**：`lifecycle-viewmodel-compose` 的 `viewModel(factory = …)`
- **约束**：`WeatherScreen → WeatherViewModel → WeatherRepository → OpenMeteoApi`（UI 不直连 Retrofit）

```
┌─────────────────────────────────────┐
│  UI (Compose)                       │
│  WeatherScreen · Loading · Error    │
└──────────────┬──────────────────────┘
               │ collect StateFlow
┌──────────────▼──────────────────────┐
│  ViewModel                          │
│  WeatherViewModel                   │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│  Repository                         │
│  WeatherRepository                  │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│  Remote Data Source                 │
│  OpenMeteoApi (Retrofit)            │
└─────────────────────────────────────┘
```

### 当前目录结构

```
app/src/main/java/com/example/mynativeapp1/
├── MainActivity.kt
├── ui/
│   ├── theme/
│   └── weather/
│       ├── WeatherScreen.kt
│       ├── WeatherViewModel.kt
│       ├── WeatherUiState.kt
│       └── WeatherIconMapper.kt
└── data/
    ├── WeatherRepository.kt
    ├── WeatherInfo.kt
    ├── WeatherCodeMapper.kt
    └── remote/
        ├── NetworkModule.kt
        ├── OpenMeteoApi.kt
        └── dto/
```

## 开发计划

| 阶段 | 任务 | 学习重点 |
|------|------|----------|
| **1** | 搭建 Retrofit，手动请求 API 并打印 Log | HTTP、JSON、协程 `suspend` |
| **2** | 定义数据模型与 Repository | 序列化、`@Serializable` |
| **3** | 编写 ViewModel 与 UI 状态 | `StateFlow`、`viewModelScope` |
| **4** | 实现 WeatherScreen 主界面 | Compose 布局、条件渲染 |
| **5** | 添加加载 / 错误 / 下拉刷新 | 用户体验、副作用处理 |
| **6** | 城市搜索与切换 | `TextField`、状态更新 |
| **7** | GPS 定位（可选） | 运行时权限、`FusedLocationProvider` |

## 环境要求

- Android Studio（推荐最新稳定版）
- JDK 11+
- Android SDK 37
- 模拟器或真机（需联网）

## 快速开始

```bash
# 克隆仓库后，在项目根目录执行
./gradlew assembleDebug

# 安装到已连接设备
./gradlew installDebug
```

> MVP 阶段使用 Open-Meteo，**无需配置 API Key**，确保设备或模拟器可以访问互联网即可。

## 学习检查清单

完成 MVP 后，应能回答以下问题：

- [ ] Activity 的 `onCreate` 里做了什么？`setContent` 的作用是什么？
- [ ] Compose 中 `remember` 与 `ViewModel` 各自管理什么状态？
- [ ] 为什么网络请求不能放在主线程？
- [ ] `Loading` / `Success` / `Error` 状态如何在 UI 中分支渲染？
- [ ] Repository 存在的意义是什么？UI 能否直接调用 Retrofit？

## 参考资源

- [Open-Meteo API 文档](https://open-meteo.com/en/docs)
- [Jetpack Compose 官方教程](https://developer.android.com/jetpack/compose/tutorial)
- [Retrofit 官方文档](https://square.github.io/retrofit/)
- [Android 架构指南（ViewModel）](https://developer.android.com/topic/libraries/architecture/viewmodel)

## 许可证

本项目仅供个人学习使用。Open-Meteo 数据适用于非商业用途，详见其[服务条款](https://open-meteo.com/en/terms)。
