# MVVM 分层且 MVP 阶段不引入 Hilt/Koin

UI 仅通过 ViewModel 的 StateFlow 驱动；ViewModel 依赖 Repository；Repository 依赖 Retrofit 接口。依赖在 `WeatherViewModelFactory` 内手动组装，避免初学者同时消化 Compose、协程与 DI 框架。若模块与依赖数量在 v2 明显增长，可在单独 ADR 中评估 Hilt。

**Considered options**: 手动 Factory wiring（选用）、Hilt/Koin（v2 再议）
