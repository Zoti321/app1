# 引入 Hilt 依赖注入（supersedes ADR-0002）

v2 功能完成后，`WeatherViewModelFactory` 成为唯一手动 wiring 点；为求职作品集与系统学习 DI，改用 **Google Hilt**（KSP 注解处理）管理依赖图。MVVM 分层不变：UI 仍只通过 ViewModel 的 StateFlow 驱动，ViewModel 依赖接口而非 Retrofit。

**Status**: accepted  
**Supersedes**: [ADR-0002](0002-mvvm-layering-without-di-framework.md)

**Considered options**: 维持手动 Factory（拒绝，无法展示 DI）、Koin（拒绝，非官方推荐且与求职动机错位）、Hilt + KSP（选用）

## DI 图

```
WeatherViewModel (@HiltViewModel)
  ├─ WeatherDataSource  ← @Binds WeatherRepository  ← OpenMeteoApi (@Singleton)
  ├─ PreferencesStore   ← @Binds AppPreferencesStore
  ├─ DeviceLocationProvider ← @Binds FusedDeviceLocationProvider
  └─ PlaceNameResolver  ← @Binds AndroidPlaceNameResolver
```

**不进图**：`WeatherCodeMapper`、`WeatherIconMapper`、`ForecastDayLabel` 等无状态纯函数。

## Hilt Module

| Module | `@InstallIn` | 内容 |
|--------|--------------|------|
| `di/NetworkModule.kt` | `SingletonComponent` | `@Provides` Json、OkHttp、Retrofit API、`OpenMeteoApi` |
| `di/DataModule.kt` | `SingletonComponent` | `@Binds` 四个接口 → 实现 |

Scope：Network/Data 实现均为 `@Singleton`；ViewModel 为 `@HiltViewModel`（`ViewModelComponent`）。

## 入口

- `WeatherApplication` — `@HiltAndroidApp`，Manifest `android:name`
- `MainActivity` — `@AndroidEntryPoint`
- `WeatherScreen` — `hiltViewModel()`（依赖 `hilt-lifecycle-viewmodel-compose`）

## 测试策略

- **JVM 单元测试**：不启用 Hilt test 栈；`WeatherViewModelTest` 继续手动 fake 构造
- **`createOpenMeteoApi`**：从旧 `NetworkModule` object 迁至 `OpenMeteoApiFactory.kt`，供 `WeatherRepositoryTest` + MockWebServer 使用
- **androidTest**：暂不配置 `@HiltAndroidTest` / `HiltTestRunner`（无 Compose UI 测试）

## 迁移步骤（实现 session）

1. Gradle：KSP + Hilt 插件；`compileOptions` → Java 17；添加 Hilt 与 `hilt-lifecycle-viewmodel-compose` 依赖
2. 新增 `WeatherApplication`；Manifest 注册
3. 新增 `di/NetworkModule.kt`、`di/DataModule.kt`；实现类加 `@Inject constructor`
4. `WeatherViewModel` → `@HiltViewModel`；`MainActivity` / `WeatherScreen` 接入 Hilt
5. 删除 `WeatherViewModelFactory`、旧 `data/remote/NetworkModule.kt` object
6. 提取 `createOpenMeteoApi` 至 factory；修正 `WeatherRepositoryTest` import
7. 运行 `./gradlew test` 验证

## 验证清单

- [ ] App 冷启动正常，天气加载、搜索、收藏、定位功能与迁移前一致（需设备手工冒烟）
- [x] `./gradlew test` 全部 JVM 测试通过
- [x] Release 构建可编译（R8 keep 规则留待启用 optimization 时再议）

## 参考

- [docs/research/hilt-vs-koin-for-weather-app.md](../research/hilt-vs-koin-for-weather-app.md)
- [Wayfinder: 天气 App 引入依赖注入](https://github.com/Zoti321/app1/issues/22)
