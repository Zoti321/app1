# 天气现象描述在客户端映射 WMO 码

Open-Meteo 与 WMO 均未提供中文天气描述 API。应用在客户端维护 `weather_code` → 中文现象表（见 `WeatherCodeMapper`），保证离线可读且与 MVP 语言一致。英文描述仅作映射依据，不直接展示给用户。

**Considered options**: 客户端本地映射（选用）、第三方 i18n API（无合适免费源）
