# Hilt 与 Koin 适用性对比（天气 App）

> **关联 Issue**：[Hilt 与 Koin 在本项目中的适用性对比](https://github.com/Zoti321/app1/issues/23)  
> **调研日期**：2026-09-18  
> **数据来源**：Android Developers 官方文档、Dagger/Hilt 文档、Koin 官方文档（insert-koin.io）、Android architecture-samples 仓库

## 结论摘要

针对本天气 App（单 `:app` 模块、Jetpack Compose、`WeatherViewModelFactory` 手动 wiring、Retrofit + DataStore + Play Services Location），**Hilt 更契合 map 已锁定的动机（求职作品集 + 系统学习 DI）**：Google 官方推荐、编译期校验、与 Jetpack（ViewModel / Navigation / Compose）有一等集成。Koin 上手更快、运行时 DSL 更轻，但国内 Junior 岗面试与官方样本生态更偏 Hilt。

**对本项目的具体建议（供下游「选定 DI 框架」ticket 引用，非最终决策）：**

| 维度 | Hilt | Koin |
|------|------|------|
| 单模块 Compose 接入成本 | 中（KSP 插件、`@HiltAndroidApp`、Module、`@AndroidEntryPoint`） | 低（`startKoin` + `module {}` + `koinViewModel()`） |
| 求职/面试信号 | 强（官方推荐 + architecture-samples） | 中（常见于 Kotlin 社区，非 Google 首选） |
| 学习曲线（已有 Factory 基础） | 陡但结构化（理解 Dagger 图、Scope、Module） | 缓（DSL 接近手动 `new`） |
| 现有 JVM 单元测试 | **可不变**（继续手动构造 ViewModel/Repository） | **可不变** |
| 编译期 vs 运行时 | 编译期生成 + 缺依赖即编译失败 | 运行时解析，错误推迟到启动/首次 get |

---

## 1. 项目上下文（调研基准）

当前依赖组装位于 `WeatherViewModelFactory`：

- `WeatherRepository(NetworkModule.openMeteoApi)`
- `AppPreferencesStore(appContext)`
- `FusedDeviceLocationProvider(appContext)`
- `AndroidPlaceNameResolver(appContext)`

ADR-0002 刻意在 MVP 阶段不引入 DI 框架。v2 功能（收藏、定位、多日预报）已使 Factory 成为唯一 wiring 点。

现有测试策略：

- `WeatherViewModelTest`：手动 fake `WeatherDataSource`、`PreferencesStore` 等，**不经过 Factory**
- `WeatherRepositoryTest`：使用 `NetworkModule.createOpenMeteoApi(...)` 工厂方法 + MockWebServer

这意味着：**引入 DI 后，JVM 单元测试不必强制迁移到 Hilt/Koin test 框架**——两种框架均允许测试侧继续直接 `WeatherViewModel(...)` 构造。

---

## 2. Google / Android 官方立场

Android Developers 明确写道：

> *「Hilt is the officially recommended library for dependency injection in Android.」*  
> — [Dependency injection with Hilt](https://developer.android.com/training/dependency-injection/hilt-android)

同页说明 Hilt 基于 Dagger，提供**编译期正确性**与 Android Studio 支持；并针对 Jetpack Compose 与单 Activity 架构优化。

Hilt 与 Jetpack 集成文档列出官方支持的组件：Compose、ViewModel、Navigation、WorkManager。  
— [Use Hilt with other Jetpack libraries](https://developer.android.com/training/dependency-injection/hilt-jetpack)

**对求职动机的影响**：简历与面试叙述与官方 architecture-samples 对齐，认知成本更低。

---

## 3. 单模块 Compose 接入复杂度

### 3.1 Hilt

**Gradle（官方 2026 文档示例）：**

- 根 `build.gradle`：`id("com.google.dagger.hilt.android") version "2.57.1" apply false`
- `app/build.gradle`：启用 `com.google.devtools.ksp` + `com.google.dagger.hilt.android`
- 依赖：`implementation("com.google.dagger:hilt-android:2.57.1")` + `ksp("com.google.dagger:hilt-android-compiler:2.57.1")`
- Compose ViewModel：`implementation("androidx.hilt:hilt-lifecycle-viewmodel-compose:1.3.0")`

来源：[Dependency injection with Hilt](https://developer.android.com/training/dependency-injection/hilt-android)、[Use Hilt with other Jetpack libraries](https://developer.android.com/training/dependency-injection/hilt-jetpack)

**Application 入口（必需）：**

```kotlin
@HiltAndroidApp
class MyApplication : Application()
```

来源：[Dependency injection with Hilt — Hilt application class](https://developer.android.com/training/dependency-injection/hilt-android)

**Activity 入口：**

- `MainActivity` 需 `@AndroidEntryPoint`（Compose 不逐个标注 Composable，Activity 为 DI 入口）
- Composable 内：`val viewModel: WeatherViewModel = hiltViewModel()`

来源：[Dependency injection with Hilt — Inject dependencies into Android classes](https://developer.android.com/training/dependency-injection/hilt-android)、[Use Hilt with other Jetpack libraries — Integration with Jetpack Compose](https://developer.android.com/training/dependency-injection/hilt-jetpack)

**ViewModel：**

```kotlin
@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val dataSource: WeatherDataSource,
    ...
) : ViewModel()
```

来源：[Use Hilt with other Jetpack libraries — Inject ViewModel objects with Hilt](https://developer.android.com/training/dependency-injection/hilt-jetpack)

**第三方/接口绑定**：Retrofit、`OpenMeteoApi`、Play Services 等需 `@Module` + `@Provides` / `@Binds` + `@InstallIn(SingletonComponent::class)`。

**本项目额外注意**：官方文档要求 Compose + Hilt 使用 **Java 17**（`compileOptions`）。当前 `app/build.gradle.kts` 为 Java 11，迁移 DI 时需一并升级。  
— [Dependency injection with Hilt — Java 17](https://developer.android.com/training/dependency-injection/hilt-android)

**参考实现**：[android/architecture-samples `app/build.gradle.kts`](https://github.com/android/architecture-samples/blob/main/app/build.gradle.kts)（KSP + Hilt + Compose + `hilt-android-testing`）。

### 3.2 Koin

**Gradle（官方 Quickstart）：**

```kotlin
implementation("io.insert-koin:koin-android:$koin_version")
implementation("io.insert-koin:koin-androidx-compose:$koin_version")
```

来源：[Android - Jetpack Compose | Koin](https://insert-koin.io/docs/quickstart/android-compose/)

**Application 入口：**

```kotlin
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@MainApplication)
            modules(appModule)
        }
    }
}
```

**Module 声明（Classic DSL，与当前 Factory 思维最接近）：**

```kotlin
val appModule = module {
    single { WeatherRepository(get()) }
    single<WeatherDataSource> { get<WeatherRepository>() }
    viewModel { WeatherViewModel(get(), get(), get(), get()) }
}
```

**Compose ViewModel：**

```kotlin
@Composable
fun WeatherScreen(viewModel: WeatherViewModel = koinViewModel()) { ... }
```

来源：[Android - Jetpack Compose | Koin](https://insert-koin.io/docs/quickstart/android-compose/)

**对比**：Koin 无需 annotation processor，**无 `@InstallIn` / `@AndroidEntryPoint`**；但模块为运行时 DSL，缺少 Hilt 的编译期图校验（Koin Compiler Plugin 为可选增强，见 Koin 文档 Compiler Plugin 章节）。

---

## 4. ViewModel + Compose 对照

| 能力 | Hilt | Koin |
|------|------|------|
| 获取 ViewModel | `hiltViewModel()`（需 `hilt-lifecycle-viewmodel-compose`） | `koinViewModel()`（`koin-androidx-compose`） |
| ViewModel 声明 | `@HiltViewModel` + `@Inject constructor` | `viewModel { }` 或 `viewModel<WeatherViewModel>()` |
| Activity 要求 | `@AndroidEntryPoint` on `MainActivity` | 普通 `ComponentActivity` 即可 |
| Navigation 多页（未来 fog） | `hiltViewModel()` 按 destination scope | `koinViewModel()` 按 destination scope |

来源：[Use Hilt with other Jetpack libraries](https://developer.android.com/training/dependency-injection/hilt-jetpack)、[Koin Compose Quickstart](https://insert-koin.io/docs/quickstart/android-compose/)

---

## 5. 测试策略（对照现有测试）

### 5.1 现状

- `WeatherViewModelTest`：纯 JVM，手动 fake 依赖 → **与 DI 框架解耦**
- `WeatherRepositoryTest`：依赖 `NetworkModule.createOpenMeteoApi(baseUrl=...)` 测真实 Retrofit 栈

### 5.2 Hilt 测试（官方）

- 需 `hilt-android-testing` + `kspTest` / `kspAndroidTest`
- UI / Instrumented：`@HiltAndroidTest`、`HiltAndroidRule`、`HiltTestApplication`、自定义 test runner
- Robolectric：`@Config(application = HiltTestApplication::class)`

来源：[Hilt testing guide](https://developer.android.com/training/dependency-injection/hilt-testing)

**对本项目**：现有 **JVM 单元测试可继续不启用 Hilt**——直接构造被测类是官方也认可的测试隔离方式。Hilt test 模块更适合 **androidTest** 或未来需要 `@Inject` 的集成测试。

**`NetworkModule.createOpenMeteoApi`**：建议**保留**为测试专用 factory（或迁入 `TestNetworkModule`），与生产 `@Provides` 并存——MockWebServer 需自定义 baseUrl，与 Singleton 生产 API 场景不同。

### 5.3 Koin 测试（官方）

- JVM：`KoinTest` + `KoinTestRule.create { modules(...) }` + `declareMock` / `get()`
- 明确说明：**不适用于 Android Instrumented tests**（Instrumented 有单独文档）

来源：[Injecting in Tests | Koin](https://insert-koin.io/docs/reference/koin-test/testing/)

**对本项目**：同样可 **仅在新测试中选用 KoinTestRule**，现有测试无需改动。

### 5.4 测试维度小结

| 场景 | Hilt | Koin |
|------|------|------|
| 保持现有 JVM 测试不变 | ✅ 推荐继续手动构造 | ✅ 推荐继续手动构造 |
| 新增 Instrumented Compose 测试 | 配置重（TestRunner、HiltTestActivity） | 相对轻 |
| 编译期验证测试 Module | `@TestInstallIn` / `@BindValue` | `verify()`（Koin 文档称 Compiler Plugin 可替代 runtime verify） |

---

## 6. 学习曲线（已有手动 Factory 基础）

**从 `WeatherViewModelFactory` 迁移的心智模型：**

| 手动 Factory | Hilt | Koin |
|--------------|------|------|
| `create()` 里 `new` 依赖 | `@Inject constructor` + Module 补第三方 | `module { single { ... } }` |
| 单例 Retrofit | `@Singleton @Provides` | `single { }` |
| ViewModel 创建 | `@HiltViewModel` + `hiltViewModel()` | `viewModel { }` + `koinViewModel()` |
| 错误发现时机 | 编译失败（缺绑定） | 运行时 crash（`NoBeanDefFoundException`） |

Hilt 初期概念更多（Component、Scope、`@InstallIn`），但与 Dagger 生态、官方 codelab 一致，**长期学习 ROI 更高**（面试常考 Hilt/Dagger 原理）。

Koin DSL 与 Factory 代码 **字面相似度最高**，适合「先理解 DI 思想、少碰注解处理器」的路径——但与 map 动机「求职」略错位。

---

## 7. 编译期 vs 运行时 DI

**Hilt / Dagger**（官方）：

> *「benefit from the compile-time correctness, runtime performance, scalability, and Android Studio support that Dagger provides」*  
> — [Dependency injection with Hilt](https://developer.android.com/training/dependency-injection/hilt-android)

构建时生成组件、校验依赖图无环、无缺失。

**Koin**（官方测试文档间接说明）：

- 运行时 `get()` 解析；`verify()` / Compiler Plugin 用于补齐校验
- 无 annotation processor 默认路径，**增量编译更快**，但错误推迟

---

## 8. KSP vs kapt（2025–2026）

Android 官方 [Migrate from kapt to KSP](https://developer.android.com/build/migrate-to-ksp)：

> *「Kapt is now in maintenance mode, and we recommend that you migrate from kapt to KSP for all processors that support it.」*

Dagger/Hilt 在官方 Hilt 文档示例中已使用 **KSP**（`ksp("com.google.dagger:hilt-android-compiler:2.57.1")`），不再推荐新项目从 kapt 起步。

**对本项目**：应直接采用 **KSP + Hilt**（若选 Hilt），并在 `libs.versions.toml` 增加 KSP 插件版本（需与 Kotlin 2.2.10 兼容，查阅 [KSP releases](https://github.com/google/ksp/releases)）。

Koin 默认路径 **不需要 KSP**；若采用 Koin Compiler Plugin 则为可选 KSP 增强。

---

## 9. 综合对比表（本项目权重：求职 + 学习）

| 评估项 | Hilt | Koin | 本项目权重 |
|--------|------|------|------------|
| 官方推荐 | ✅ Google 首选 | ❌ 第三方 | 高 |
| 单模块首次接入工作量 | 中–高 | 低–中 | 中 |
| Compose ViewModel 集成 | `hiltViewModel()` 官方扩展 | `koinViewModel()` 官方扩展 | 中 |
| 与现有单元测试兼容 | 高（可不改测试） | 高（可不改测试） | 高 |
| 面试叙述 | 强 | 中 | 高 |
| 编译期安全 | 强 | 弱（除非 Compiler Plugin） | 中 |
| Java 版本要求 | 17（需升级） | 无额外要求 | 中 |
| 删除 `WeatherViewModelFactory` 后代码量 | Module + 注解 | Module DSL | — |

---

## 10. 供下游 ticket 的事实结论（非框架选定）

1. **两种框架均能满足**单模块天气 App 的 DI 需求；无功能性 blocker。
2. **若动机含求职**，Hilt 有官方文档与 architecture-samples 背书，优先级应高于 Koin。
3. **现有 JVM 测试不必因 DI 重写**；`NetworkModule.createOpenMeteoApi` 建议保留为测试 seam。
4. **选 Hilt 的隐藏工作量**：Java 11 → 17、`Application` 类、Manifest 注册、KSP 插件对齐 Kotlin 版本。
5. **选 Koin 的隐藏风险**：运行时错误、国内部分团队栈不统一；优势是上手快、与 Factory 思维接近。

---

## 参考链接

- [Dependency injection with Hilt | Android Developers](https://developer.android.com/training/dependency-injection/hilt-android)
- [Use Hilt with other Jetpack libraries | Android Developers](https://developer.android.com/training/dependency-injection/hilt-jetpack)
- [Hilt testing guide | Android Developers](https://developer.android.com/training/dependency-injection/hilt-testing)
- [Migrate from kapt to KSP | Android Developers](https://developer.android.com/build/migrate-to-ksp)
- [Android - Jetpack Compose | Koin](https://insert-koin.io/docs/quickstart/android-compose/)
- [Injecting in Tests | Koin](https://insert-koin.io/docs/reference/koin-test/testing/)
- [architecture-samples app/build.gradle.kts](https://github.com/android/architecture-samples/blob/main/app/build.gradle.kts)
