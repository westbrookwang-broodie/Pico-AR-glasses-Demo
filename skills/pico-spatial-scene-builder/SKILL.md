---
name: pico-spatial-scene-builder
description: |
  在 PICO Spatial SDK（PICO OS 6 AR/MR 眼镜）项目中，按用户需求创建一组 3D 场景界面（面板/Panel），使用统一的视觉风格，支持把用户上传的效果图拆解为面板，并用 Jetpack Navigation Compose 实现简单的页面跳转逻辑。生成的代码保证可正确编译（使用 PICO 提供的 UI 组件而非标准 Compose UI，并用 JDK 17 验证构建）。触发场景：用户想用 PICO/Spatial SDK 创建/新增 3D 场景、空间面板、SpatialView 视图、悬浮窗口界面，想做面板之间的跳转，或提供了一张效果图/截图要求据此搭建空间界面时使用。关键词："PICO", "Spatial SDK", "空间应用", "3D 场景", "3D 界面", "空间面板", "SpatialView", "WindowContainer", "面板跳转", "场景跳转", "AR 眼镜界面", "效果图", "spatial scene", "spatial view", "panel navigation"
---

# PICO 空间场景界面生成

为 PICO Spatial SDK 项目按需创建多个 3D 场景面板，应用统一风格，支持据效果图拆解界面，并用 Navigation Compose 串起跳转。

## 核心约束（必须遵守，否则编译或运行失败）

1. **不要用标准 Compose UI 组件做可视元素**。PICO 工程在 `app/build.gradle.kts` 里用 `configurations.all { exclude("androidx.compose.ui", "ui") ... }` 排除了标准 Compose 的 `ui`/`ui-graphics`/`ui-text`/`foundation`。文本/图标必须用 `com.pico.spatial.ui.design.Text` / `Icon`，背景用 `backgroundMaterial()`，**不要** `import androidx.compose.material*` 的 `Text`/`Button`。布局类（`Column`/`Row`/`Box`/`Modifier`/`Spacer`/尺寸）仍来自 `androidx.compose.foundation.layout` 和 `androidx.compose.ui`，可正常使用。
2. **每个 3D 模型放在 `SpatialView` 里**，并通过 `Modifier.requiredDepth(...)` 给 Z 轴留出深度，否则模型不立体或不显示。
3. **3D 实体（`Entity`）的生命周期绑定到 ViewModel**，在 `onCleared()` 里 `reset` / `closeResources`，避免离开页面后资源泄漏。
4. **导航用 `androidx.navigation.compose`**（多数 PICO 模板已声明该依赖，无需新增）。不要自己用 `if/else` 状态切页，要用 `NavHost` + `composable` 才有返回栈。
5. **构建验证用 JDK 17**。系统默认 JDK 8 跑不起 AGP 8.x。命令行构建前把 `JAVA_HOME` 指到 Android Studio 自带 JBR（如 `D:/andriod-studio/android-studio/jbr`），用 `./gradlew :app:assembleDebug` 验证。

详细 API、清单、构建细节见 `references/sdk-patterns.md`。

## 工作流程

### 第 1 步：明确需求

先确认这几点（一次问清，不要逐条追问）：
- 要几个场景/面板，各自叫什么、放什么内容（文本？列表？3D 模型？）
- 跳转关系：谁能到谁、有没有"返回/关闭"
- 哪些面板需要内嵌 3D 模型（`.glb` / `.usdz`）
- 是否提供了效果图

若用户上传了效果图，转到第 2 步；否则跳到第 3 步。

### 第 2 步：把效果图拆解为面板（仅当有效果图）

按 `references/reference-image-workflow.md` 的方法，把图中每个独立悬浮区域识别为一个面板，记录它的标题、主要内容、控件（按钮/关闭/便签），以及箭头表示的跳转关系。产出一份"面板清单 + 跳转表"，与用户确认后再生成代码。

### 第 3 步：定位工程结构

读 `settings.gradle.kts` / `app/build.gradle.kts` 确认这是 PICO 工程（依赖 `com.pico.spatial:bom`）和应用包名（`namespace`）。新代码放在应用包下的 `ui/`（如 `ui/scene` 或 `ui/demo`）。找到空间入口函数（通常是 `Main.kt` 里的 `fun mainApp(scope: SpatialAppScope)`）。

### 第 4 步：放入统一风格工具集

把 `assets/ui-kit/SpatialUiKit.kt` 复制到工程的 UI 包下，并改 `package` 为该工程的包名。它提供统一的调色板、`SpatialPanelButton`、`SpatialCloseButton`、`StickyNote`、`PanelScaffold` 等，所有面板都基于它们搭建，保证风格一致。风格规范见 `references/ui-style.md`。

### 第 5 步：生成场景面板

参照 `assets/templates/SceneScreens.kt.template` 为每个面板写一个 `@Composable`。每个面板用 `PanelScaffold` 包裹（统一标题栏 + 关闭按钮 + 内容区）。需要 3D 的面板用 `assets/templates/Scene3DView.kt.template` 内嵌 `SpatialView`，并配套一个 `Entity` ViewModel（模型加载/清理见 `references/sdk-patterns.md`）。

### 第 6 步：接入导航

参照 `assets/templates/SceneNavHost.kt.template`，在 `mainApp` 的 `DefaultWindowContainer { PicoTheme { ... } }` 内放一个 `NavHost`，按第 1/2 步的跳转表写 `composable(route)`，用 `navController.navigate(route)` 前进、`popBackStack(...)` 返回/关闭。

### 第 7 步：编译验证

用 JDK 17 跑 `./gradlew :app:assembleDebug` 确认通过。修复所有编译错误后再交付。常见错误对照 `references/sdk-patterns.md` 的"常见编译错误"小节。完成后用一两句话说明做了什么，不要泄露内部文件路径细节给最终用户之外的内容。

## 资源索引

- `references/sdk-patterns.md` — PICO Spatial SDK 关键 API、AndroidManifest 空间窗口配置、3D 实体加载与生命周期、构建/JDK 要求、常见编译错误。**写任何 SDK 调用前先读它。**
- `references/ui-style.md` — 统一视觉风格规范：调色板、间距、字号、面板结构约定。
- `references/reference-image-workflow.md` — 把上传的效果图拆解成面板清单与跳转表的方法。
- `assets/ui-kit/SpatialUiKit.kt` — 可直接复制的统一风格组件库（改包名即用）。
- `assets/templates/` — 面板、3D 视图、NavHost、3D ViewModel 的代码模板（`.template` 后缀，复制后去掉后缀并改包名）。
