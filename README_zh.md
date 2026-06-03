# 空间导航示例

[English](README.md) | [简体中文](README_zh.md)

一个最小化的 PICO Spatial SDK 应用，演示如何在同一个空间窗口内的多个 3D 面板之间跳转，其中一个面板内嵌了可播放的 3D 模型。

## 功能

用 Jetpack Navigation Compose 串起三个面板：

- **协作板（Hub）**：带三张便签的协作板，点击任意便签进入资料板 A。
- **资料板 A（Board A）**：内嵌一个动画 3D 机器人（`SpatialView`）。可继续前往资料板 B，或关闭返回协作板。
- **资料板 B（Board B）**：纯文本资料面板，关闭后返回协作板。

```
协作板 ──点便签──▶ 资料板A ──继续学习──▶ 资料板B
  ▲                  │                    │
  └───────关闭────────┴────────关闭─────────┘
```

## 技术栈

- Kotlin + Jetpack Compose
- PICO Spatial SDK 0.12.2（`SpatialView`、`DefaultWindowContainer`、ECS 实体）
- Jetpack Navigation Compose 负责窗口内路由
- 单 Gradle 模块（`:app`），包名 `com.example.spatialnav`，`arm64-v8a`，`minSdk 26`

## 项目结构

```txt
app/src/main/java/com/example/spatialnav/
├── Main.kt                         # 入口：DefaultWindowContainer + NavHost（3 个路由）
├── platform/
│   ├── SpatialApplication.kt       # launch(::mainApp)
│   └── LaunchActivity.kt           # SpatialLaunchActivity
├── ui/
│   ├── demo/
│   │   └── DemoScreens.kt          # 协作板 / 资料板A / 资料板B + Robot3DView + Routes
│   └── skeletal/
│       └── SkeletalAnimationViewModel.kt  # 持有机器人 Entity，在 onCleared 清理
├── util/
│   └── SkeletalAnimationUtil.kt    # 加载机器人模型、播放骨骼动画
└── data/
    └── AnimationModels.kt          # SkeletalAnimationState（动画片段索引）
app/src/main/assets/
└── pico_robot_animated.glb         # 资料板 A 中展示的 3D 模型
```

导航逻辑集中在 `Main.kt`，每个面板是 `ui/demo/DemoScreens.kt` 里的一个 Composable。3D 模型由 `SkeletalAnimationViewModel`（位于 `ui/skeletal/`）加载，其生命周期绑定到资料板 A 的返回栈条目，离开面板时自动清理模型资源。

## 环境要求

请按 PICO Spatial SDK 官方指南配置开发环境：

- [开发环境配置（中国大陆）](https://developer-cn.picoxr.com/document/spatial-sdk/set-up-development-environment/)
- [开发环境配置（海外）](https://developer.picoxr.com/document/spatial-sdk/set-up-development-environment/)

需要 JDK 11 及以上（本项目已用 Android Studio 自带的 JBR JDK 17 验证通过）。

## 运行

1. 用 Android Studio 打开项目。
2. 在 **Device Manager** 创建 PICO 模拟器，或连接已开启 USB 调试的 PICO 设备。
3. 点击 **Run 'app'**。

## 替换 3D 模型

1. 把你的动画 `.glb` 放进 `app/src/main/assets/`。
2. 在 `util/SkeletalAnimationUtil.kt` 与 `data/AnimationModels.kt` 中更新 `ANIMATED_ROBOT` 路径和动画片段数量。

## 许可

源代码遵循 [Apache 2.0 License](LICENSE.txt)。3D 素材遵循 [Creative Commons BY 4.0](https://creativecommons.org/licenses/by/4.0/)。
