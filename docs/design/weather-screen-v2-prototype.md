# WeatherScreen v2 Success 态 UI 原型

> **关联 Issue**：[#18 设计深色模式主题与扩展 Success 态 UI 原型](https://github.com/Zoti321/app1/issues/18)  
> **前置**：[weather-screen-prototype.md](./weather-screen-prototype.md)（MVP 线框）· [#14](https://github.com/Zoti321/app1/issues/14) 搜索 · [#15](https://github.com/Zoti321/app1/issues/15) 预报 · [#16](https://github.com/Zoti321/app1/issues/16) 收藏 · [#17](https://github.com/Zoti321/app1/issues/17) GPS（待决）  
> **状态**：**已锁定**（2026-09-17，[#18](https://github.com/Zoti321/app1/issues/18)）

## 设计方向

- **主题**：跟随系统浅/深色（`isSystemInDarkTheme()`），**关闭 dynamicColor**，固定 Sky 调色板
- **布局**：延续 Hero-Centric；v2.2 起在湿度卡片下增加横向预报条；TopAppBar 扩展操作图标
- **组件**：Material 3 Compose；Sheet 与 MVP 一致

---

## Success 态线框（v2.3 完整态）

```
┌──────────────────────────────────────┐
│  北京          [📍][🔖][★][🔍]      │  TopAppBar: title=当前地点名
│                   ↑   ↑  ↑  ↑       │  MyLocation Bookmark Star Search
├──────────────────────────────────────┤
│  ╔════════════════════════════════╗  │  PullToRefreshBox（Success 内）
│  ║                                ║  │
│  ║         [WbSunny 48dp]         ║  │  Hero：当前温度 + 现象
│  ║            22°                 ║  │
│  ║             晴                 ║  │
│  ║                                ║  │
│  ║  ┌──────────────────────────┐  ║  │
│  ║  │ 湿度 45%    风速 12 km/h │  ║  │  ElevatedCard
│  ║  └──────────────────────────┘  ║  │
│  ║                                ║  │
│  ║  未来7天                        ║  │  titleSmall（v2.2+）
│  ║  ┌──┐ ┌──┐ ┌──┐ ┌──┐ →       ║  │  LazyRow 日卡片 ×7
│  ║  │今│ │明│ │三│ │四│         ║  │
│  ║  └──┘ └──┘ └──┘ └──┘         ║  │
│  ║                                ║  │
│  ║   数据来源 Open-Meteo.com      ║  │
│  ╚════════════════════════════════╝  │
└──────────────────────────────────────┘
```

**TopAppBar 图标（自左向右，`actions`）**

| 图标 | 功能 | 引入阶段 |
|------|------|----------|
| `MyLocation` | GPS 定位 | v2.3 |
| `Bookmark` | 收藏列表 Sheet | v2.3 |
| `Star` / `StarBorder` | 切换当前地点收藏 | v2.3 |
| `Search` | 搜索 Sheet | v2.1 |

**阶段裁剪**

| 阶段 | Success 态可见元素 |
|------|-------------------|
| v2.1 | TopAppBar（仅 Search）+ Hero + 湿度卡片 + 出处；无预报条 |
| v2.2 | + 未来7天 LazyRow；TopAppBar 仍仅 Search |
| v2.3 | + MyLocation / Bookmark / Star；完整线框 |

---

## 深色模式 Token（Material 3 `darkColorScheme`）

与 `Color.kt` 实现对齐；浅色 token 见 MVP 原型。

| Token | Dark | 用途 |
|-------|------|------|
| `primary` | `#38BDF8` | 温度、图标、ProgressIndicator |
| `onPrimary` | `#0F172A` | primary 上文字 |
| `background` | `#0F172A` | 全屏背景（sky-950 系） |
| `surface` | `#1E293B` | Card、Sheet 表面 |
| `surfaceVariant` | `#334155` | 预报日卡片背景 |
| `onSurface` | `#F8FAFC` | 主文案 |
| `onSurfaceVariant` | `#94A3B8` | 副文案、出处、降水 |
| `tertiary` | `#FBBF24` | 重试按钮、Star  filled tint |
| `outline` | `#475569` | 卡片描边（可选） |

**Sheet / 搜索 / 收藏**

- Sheet 背景：`surface`
- `OutlinedTextField`：默认 Material3 深色边框，`primary` focus
- 列表分割线：`outline` 或 `surfaceVariant`

**预报日卡片（LazyRow 项）**

- 背景：`surfaceVariant`，圆角 12dp
- 选中/今天：可选 `primary` 12% alpha 描边（实现可选，非必须）

**对比度**

- 正文与背景对比 ≥ 4.5:1（`onSurface` on `background` 已满足）

---

## 无障碍（v2 增量）

| 元素 | contentDescription |
|------|-------------------|
| Search | 「搜索地点」 |
| MyLocation | 「使用当前位置」 |
| Bookmark | 「收藏地点列表」 |
| Star / StarBorder | 「收藏当前地点」/「取消收藏当前地点」 |
| 预报日卡片 | 「周三，最高 31 度，最低 20 度，晴」 |

---

## 已确认（#18）

- [x] TopAppBar 四图标顺序：MyLocation → Bookmark → Star → Search
- [x] 深色 token 表（与现有 `Color.kt` 一致）
- [x] 预报日卡片 `surfaceVariant` 背景
- [x] v2.1/v2.2 隐藏未引入图标（非 disabled 占位）
