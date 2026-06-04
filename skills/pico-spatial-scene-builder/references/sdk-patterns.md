
# PICO Spatial SDK 关键模式参考

适用版本：PICO Spatial SDK 0.12.x（PICO OS 6）。所有结论以工程实际依赖为准（`com.pico.spatial:bom`）。

## 目录

1. 入口与应用结构
2. 空间窗口（AndroidManifest）
3. UI 组件来源（最易踩坑）
4. SpatialView 与 3D 视图
5. 3D 实体加载与生命周期
6. 导航
7. 构建与 JDK 要求
8. 常见编译错误对照

## 1. 入口与应用结构

PICO 空间应用不是传统 `Activity.setContentView`，而是：

```kotlin
// platform/SpatialApplication.kt
class SpatialApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        launch(::mainApp) // com.pico.spatial.ui.foundation.dsl.launch
    }
}

// platform/LaunchActivity.kt
class LaunchActivity : SpatialLaunchActivity() // com.pico.spatial.ui.platform.stub.SpatialLaunchActivity
```

UI 根入口是一个普通函数：

```kotlin
// Main.kt
fun mainApp(scope: SpatialAppScope) =
    with(scope) {
        DefaultWindowContainer {        // com.pico.spatial.ui.foundation.dsl.DefaultWindowContainer
            PicoTheme {                 // com.pico.spatial.ui.design.PicoTheme
                // 这里放 NavHost 或面板
            }
        }
    }
```

`AndroidManifest.xml` 中 `SpatialApplication` 用 `android:name`，`LaunchActivity` 用 `.platform.LaunchActivity`（相对应用 namespace），并带 `pico.spatial.windowcontainer.*` 元数据。

## 2. 空间窗口（AndroidManifest 元数据）

LaunchActivity 下的关键 meta-data（决定窗口是否有 3D 体积）：

- `pico.spatial.windowcontainer.style` = `2`（Volumetric 体积窗口，3D 内容才能凸出）；`1` 为 Planar 平面。
- `pico.spatial.windowcontainer.defaultsize` = `宽x高x深`（如 `1520x700x600`，第三个值是 Z 轴深度）。
- `pico.spatial.windowcontainer.worldscaletype` = `1`（Dynamic 动态缩放）。

要内嵌 3D 模型时，必须确保 style=2 且 defaultsize 带深度，否则模型被压扁。

## 3. UI 组件来源（最易踩坑）

PICO 工程在 `app/build.gradle.kts` 通常有：

```kotlin
configurations.all {
    resolutionStrategy {
        exclude("androidx.compose.ui", "ui")
        exclude("androidx.compose.ui", "ui-graphics")
        exclude("androidx.compose.ui", "ui-text")
        exclude("androidx.compose.foundation", "foundation")
    }
}
```

因此：

| 用途 | 用这个（PICO） | 不要用（被排除/不一致） |
|------|----------------|--------------------------|
| 文本 | `com.pico.spatial.ui.design.Text` | `androidx.compose.material.Text` |
| 图标 | `com.pico.spatial.ui.design.Icon` | material Icon |
| 按钮/分段/滑块/菜单 | `com.pico.spatial.ui.design.*`（`SegmentControl`/`Slider`/`Menu` 等） | material 同名组件 |
| 面板背景材质 | `Modifier.backgroundMaterial(style = Material.Thin/Thick)`（`com.pico.spatial.ui.foundation.material`，`com.pico.spatial.ui.platform.Material`） | 自绘背景 |
| 悬停反馈 | `Modifier.spatialHoverEffect()`（`com.pico.spatial.ui.foundation.hover`） | — |
| 主题色 | `Color.Vibrant.withVibrant(Vibrant.UltraDark)`（`com.pico.spatial.ui.foundation.vibrant` + `com.pico.spatial.ui.graphics.Vibrant`） | — |

仍可正常使用的标准库（布局/修饰/状态，不属于被排除项）：
- `androidx.compose.foundation.layout.*`（`Column`/`Row`/`Box`/`Spacer`/`padding`/`size`/`fillMaxSize` 等）
- `androidx.compose.foundation.*`（`background`/`clickable`/`shape`/`Image`/`lazy.*`）
- `androidx.compose.ui.*`（`Modifier`/`Alignment`/`graphics.Color`/`unit.dp`/`unit.sp`/`draw.clip`/`text.font.*`/`semantics`）
- `androidx.compose.runtime.*`（`Composable`/`remember`/`mutableStateOf`/`LaunchedEffect`/`DisposableEffect`）
- `androidx.lifecycle.viewmodel.compose.viewModel`

经验法则：**可视化呈现（文字、图标、控件、背景材质）走 `com.pico.spatial.ui.*`；纯布局与状态走标准 `androidx.compose.foundation.layout` / `androidx.compose.ui` / `androidx.compose.runtime`。**

## 4. SpatialView 与 3D 视图

`SpatialView`（`com.pico.spatial.ui.foundation.content.SpatialView`）在 Compose 布局里嵌一块 3D 渲染区：

```kotlin
SpatialView(
    modifier = Modifier.height(280.dp).fillMaxWidth().requiredDepth(100.dp), // requiredDepth 来自 com.pico.spatial.ui.foundation.layout
    initial = { content, _ -> content.addEntity(viewModel.entity) }
)
```

- `requiredDepth(dp)` 给 3D 内容留 Z 轴深度，必须有。
- `initial` lambda 在内容首次创建时把 `Entity` 加进 `SpatialViewContent`。

## 5. 3D 实体加载与生命周期

模型加载较重，放 IO 线程；实体生命周期跟随 ViewModel。

加载（无状态工具，参数进、回调出）：

```kotlin
fun initialize(entity: Entity, scope: CoroutineScope, onInitialized: (Data) -> Unit) {
    scope.launch {
        val character = withContext(Dispatchers.IO) { Entity.load("asset://model.glb") }
        entity.addChild(character)
        character.components[TransformComponent::class.java]?.apply {
            setPosition(Vector3(-0.07f, -0.2f, 0.1f))
            setScaleVector(Vector3(0.0045f))
        }
        // 蒙皮网格 / 动画资源等
        onInitialized(data)
    }
}
```

ViewModel 持有 `Entity` 并负责清理：

```kotlin
class SceneObjectViewModel : ViewModel() {
    val entity = Entity()
    init { SceneObjectUtil.initialize(entity, viewModelScope) { /* ... */ } }
    override fun onCleared() {
        super.onCleared()
        // entity.stopAllAnimations() / 关闭资源
    }
}
```

要点：
- 播放动画后**不要**提前 `close()` 动画资源（会导致连续播放失效）；资源在 `onCleared` 统一关闭。
- 把 3D ViewModel 用默认 `viewModel()` 注入到需要它的面板里，导航离开该面板时 ViewModel 随返回栈条目销毁、自动清理。
- 模型资源放 `app/src/main/assets/`，`build.gradle.kts` 里 `androidResources { noCompress += listOf(".glb", ".usdz") }` 避免压缩。

## 6. 导航

用 `androidx.navigation.compose`（多数模板已在 `libs.versions.toml` / `app/build.gradle.kts` 声明 `navigation-compose`，无需新增依赖；若确实缺失再添加）。

```kotlin
val navController = rememberNavController()
NavHost(navController = navController, startDestination = Routes.HUB) {
    composable(Routes.HUB)     { HubScreen(onOpen = { navController.navigate(Routes.BOARD_A) }) }
    composable(Routes.BOARD_A) { BoardAScreen(
        onNext  = { navController.navigate(Routes.BOARD_B) },
        onClose = { navController.popBackStack(Routes.HUB, false) }) }
    composable(Routes.BOARD_B) { BoardBScreen(onClose = { navController.popBackStack(Routes.HUB, false) }) }
}
```

- `navigate(route)` 前进；`popBackStack()` 返回上一个；`popBackStack(route, false)` 一步回到指定面板（适合多个面板的"关闭"统一回到 Hub）。
- `navigation-compose` 是纯逻辑层，不依赖被排除的 Compose 图形库，可安全使用。

## 7. 构建与 JDK 要求

- AGP 8.x 需要 **JDK 11+**；系统默认 JDK 8 会在配置阶段失败（`Dependency requires at least JVM runtime version 11`）。
- 优先用 Android Studio 自带 JBR（JDK 17），命令行：
  ```bash
  # PowerShell
  $env:JAVA_HOME="D:/andriod-studio/android-studio/jbr"; ./gradlew :app:assembleDebug
  ```
  实际路径以机器为准，可在 Android Studio 安装目录下找 `jbr/bin/java.exe` 确认版本。
- 验证编译用 `:app:compileDebugKotlin`（快），验证含资源/打包用 `:app:assembleDebug`（全）。
- 改了包名/namespace 后用 `:app:clean :app:assembleDebug` 全量验证，避免旧 R 类缓存。

## 8. 常见编译错误对照

| 现象 | 原因 | 解决 |
|------|------|------|
| `Unresolved reference: Text`（或 Text 参数不匹配） | 误用 material Text | 改 import 为 `com.pico.spatial.ui.design.Text` |
| `Unresolved reference: ui` / foundation 类找不到 | 用了被 `exclude` 的标准 Compose UI | 换成 `com.pico.spatial.ui.*` 对应组件 |
| 3D 模型不显示或被压扁 | 缺 `requiredDepth` 或窗口 style 非 Volumetric | 加 `Modifier.requiredDepth(...)`；manifest style=2 + defaultsize 带深度 |
| `Dependency requires at least JVM runtime version 11` | 用了 JDK 8 | 把 `JAVA_HOME` 指到 JDK 17 (JBR) |
| 离开面板后内存增长 / 资源未释放 | Entity 资源未在 onCleared 清理 | ViewModel `onCleared` 里 reset/close |
| `navigate` 无返回栈、返回无效 | 用 if/else 状态切页 | 改用 NavHost + composable |
