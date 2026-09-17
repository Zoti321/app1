# WeatherScreen UI 原型

> **关联 Issue**：[#5 设计主界面 UI 布局与信息层级](https://github.com/Zoti321/app1/issues/5)  
> **设计依据**：ui-ux-pro-max（Hero-Centric + Sky 配色）+ Material 3 Compose 规范  
> **设计系统**：[`design-system/weather-app/MASTER.md`](../../design-system/weather-app/MASTER.md) · [`pages/weather.md`](../../design-system/weather-app/pages/weather.md)

## 设计方向（ui-ux-pro-max 摘要）

| 维度 | 选型 |
|------|------|
| 模式 | Hero-Centric — 温度作为视觉焦点，一屏读完 |
| 风格 | 轻量 Glass / 卡片分层（MVP 用 Material `Card` + 浅 sky 背景，不做重度 blur） |
| 主色 | `#0284C7`（sky-600） |
| 背景 | `#F0F9FF`（sky-50） |
| 强调 | `#F59E0B`（amber-500，仅用于重试按钮等 CTA） |
| 字体 | Material 3 默认 Typography（系统字体，MVP 不引入 Google Fonts） |
| 图标 | **Material Icons**（`Icons.Outlined.*`），禁止 emoji 作结构图标 |

---

## 成功态线框（Success）

```
┌──────────────────────────────────────┐
│  TopAppBar: 「天气」                  │  ← Material3 TopAppBar, centerAligned
├──────────────────────────────────────┤
│           (safe area padding)        │
│                                      │
│              北京                     │  headlineSmall, onSurface
│                                      │
│         [WbSunny 48dp]               │  weather icon, primary tint
│            22°                       │  displayLarge, primary
│             晴                       │  titleMedium, onSurfaceVariant
│                                      │
│  ┌────────────────────────────────┐  │
│  │  [WaterDrop] 湿度  [Air] 风速    │  │  ElevatedCard, Material Icons
│  │   45%           12 km/h        │  │  bodyLarge values
│  └────────────────────────────────┘  │
│                                      │
│     数据来源 Open-Meteo.com          │  labelSmall, muted, 居中
│                                      │
└──────────────────────────────────────┘
```

**信息层级（上 → 下，重要性递减）**

1. 温度 `displayLarge` — Hero
2. 天气描述 `titleMedium`
3. 城市名 `headlineSmall`
4. 湿度 / 风速 `ElevatedCard` 内 `bodyLarge`
5. 出处 `labelSmall`

**Compose 组件映射**

| 区域 | 组件 |
|------|------|
| 根布局 | `Scaffold` + `TopAppBar` |
| 内容 | `Column(horizontalAlignment = Center)` + `verticalArrangement = spacedBy(16.dp)` |
| 指标区 | `ElevatedCard` > `Row(horizontalArrangement = SpaceEvenly)` × 2 列 |
| 间距 | 8dp 节奏：内边距 16dp，区块间距 24dp |

---

## 加载态线框（Loading）

```
┌──────────────────────────────────────┐
│  TopAppBar: 「天气」                  │
├──────────────────────────────────────┤
│                                      │
│         CircularProgressIndicator    │  Material3 默认，primary 色
│              加载中…                  │  bodyMedium, onSurfaceVariant
│                                      │
└──────────────────────────────────────┘
```

- 居中，`Modifier.semantics { liveRegion = LiveRegionMode.Polite }` 播报状态
- 保留 TopAppBar，避免布局跳动

---

## 错误态线框（Error）

```
┌──────────────────────────────────────┐
│  TopAppBar: 「天气」                  │
├──────────────────────────────────────┤
│                                      │
│      [CloudOff 48dp]                 │  Icons.Outlined.CloudOff
│   无法获取天气，请检查网络            │  bodyLarge, 居中
│                                      │
│      [ 重 试 ]                       │  FilledTonalButton, min 48dp 高
│                                      │
└──────────────────────────────────────┘
```

- 错误文案必须可见 + 可操作（ui-ux：Empty/Error 需 guide + action）
- 重试按钮使用 accent `#F59E0B` 容器色（`tertiary` token）

---

## 主题 Token 映射（Material 3）

实现时将 sky 配色写入 `Color.kt` / `Theme.kt`（**关闭 dynamicColor**，MVP 使用固定 sky 主题以便学习）：

| Token | Light | 用途 |
|-------|-------|------|
| `primary` | `#0284C7` | 温度、图标、ProgressIndicator |
| `onPrimary` | `#FFFFFF` | — |
| `background` | `#F0F9FF` | 全屏背景 |
| `surface` | `#FFFFFF` | Card 表面 |
| `onSurfaceVariant` | `#475569` | 副文案、出处 |
| `tertiary` | `#F59E0B` | 重试按钮 |

深色模式：沿用 Material `darkColorScheme` 同 hue 降亮度（实现阶段补全，MVP 可跟随系统 dark theme 基础适配）。

---

## 无障碍要点

- 温度：`contentDescription = "当前温度 22 度"`
- 图标：装饰性图标 `contentDescription = null`；若单独传达天气则 `"晴"`
- 重试按钮：`contentDescription = "重试加载天气"`
- 触控目标：重试按钮 ≥ 48dp

---

## 交互 UX — 已锁定 ([#6](https://github.com/Zoti321/app1/issues/6))

| 项 | 决策 |
|---|---|
| 首次加载 | ViewModel init → `loadWeather("北京")` |
| Loading | 内容区居中，TopAppBar 可见，「加载中…」 |
| 重试 | Error → Loading → 再请求；Loading 期间无按钮 |
| 下拉刷新 | 不做 |
| Empty | 不做，归入 Error |

**错误文案**

- 无网络：「无法连接网络，请检查后重试」
- API/解析失败：「获取天气失败，请稍后重试」
- 无 geocode 结果：「未找到该城市天气信息」

**UiState**

```kotlin
sealed interface WeatherUiState {
    data object Loading : WeatherUiState
    data class Success(val weather: WeatherInfo) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}
```

## 状态

**已锁定** — [#5](https://github.com/Zoti321/app1/issues/5) UI 布局 + [#6](https://github.com/Zoti321/app1/issues/6) 交互 UX（2026-09-17）。本文件为 UI 实现规格。
