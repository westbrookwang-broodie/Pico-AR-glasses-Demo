# Animation

[English](README.md) | [简体中文](README_zh.md)

Animation 是一款基于 PICO OS 6 的参考示例应用，使用 PICO Spatial SDK 演示骨骼动画与补间动画的最佳实践。

## 概览

本示例为开发者提供了探索和实现 PICO Spatial SDK 动画能力的实践指南。通过交互式 3D 环境，用户可以体验骨骼动画（基于骨骼的角色运动）、补间动画（平滑的变换与材质插值）以及动画播放控制。

应用采用 MVVM 架构，配合无状态工具类与规范的 Android 生命周期管理，实现高效的动画资源处理。

![Screenshot](.github/screenshot.png)

## 功能特性

Animation 示例展示了 PICO Spatial SDK 的以下核心动画能力：

- **3D 模型集成**：在空间环境中加载并驱动 GLB/USDZ 模型动画
- **骨骼动画系统**：支持多动画状态的骨骼驱动角色动画
- **补间动画框架**：3D 对象变换与材质属性的平滑插值动画
- **动画控制界面**：播放、速度调节与状态管理的交互式控件
- **动画资源管理**：动画资源的高效加载与按需清理
- **MVVM 架构**：通过无状态工具类与 ViewModel 状态管理实现清晰的关注点分离
- **生命周期管理**：Android 生命周期事件中的动画资源规范处理

## 环境要求

构建和运行本项目前，请参照 PICO Spatial SDK 官方文档完成开发环境配置：

- [PICO Spatial SDK 环境配置指南（中国大陆）](https://developer-cn.picoxr.com/document/spatial-sdk/set-up-development-environment/)
- [PICO Spatial SDK 环境配置指南（非中国大陆）](https://developer.picoxr.com/document/spatial-sdk/set-up-development-environment/)

## 快速开始

### 1. 克隆项目

```bash
git clone <repository-url>
cd Animation
```

### 2. 在 Android Studio 中打开项目

启动 Android Studio，选择 **Open an Existing Project**，然后选择 `Animation` 目录。

### 3. 运行项目

#### 在模拟器上运行

1. 打开 **Device Manager**（可通过工具栏或 **Tools > Device Manager** 进入）
2. 点击 **Add a new device** > **Create a PICO Emulator**
3. 在 **Device Manager** 中启动模拟器
4. 点击 **Run 'app'** 启动应用

#### 在物理设备上运行

1. 将 PICO 设备连接至开发机
2. 在设备上启用 **USB 调试（USB Debugging）**
3. 点击 **Run 'app'** 将应用直接部署到设备

## 项目结构

Animation 示例采用模块化架构，将应用逻辑与 3D 资源分离：

```txt
Animation/
├── app/                                  # 主应用模块
│   └── src/main/
│       ├── java/com/pico/spatial/sample/animation/ # Kotlin 源代码
│       │   ├── Main.kt                     # 应用入口
│       │   ├── data/                       # 动画状态定义
│       │   │   └── AnimationModels.kt
│       │   ├── ktx/                        # 扩展函数
│       │   │   └── Extensions.kt
│       │   ├── manager/                    # 事件与动画管理器
│       │   │   └── EventManager.kt
│       │   ├── platform/                   # 平台相关实现
│       │   │   ├── LaunchActivity.kt
│       │   │   └── SpatialApplication.kt   # 应用初始化
│       │   ├── ui/                         # UI 组件与页面（Compose）
│       │   │   ├── common/
│       │   │   │   └── AnimationPlayView.kt # 含自动重播逻辑的主播放视图
│       │   │   ├── home/                   # 主页与导航
│       │   │   ├── skeletal/               # 骨骼动画 UI 与 ViewModel
│       │   │   └── tween/                  # 补间动画 UI 与 ViewModel
│       │   └── util/                       # 无状态动画工具类
│       │       ├── SkeletalAnimationUtil.kt # 骨骼动画管理
│       │       └── TweenAnimationUtil.kt   # 补间动画管理
│       ├── assets/                         # 3D 模型与动画资源
│       │   ├── pico_robot_animated.glb     # 带动画的机器人模型（骨骼动画）
│       │   └── pico_robot_static.usdz      # 静态机器人模型（补间动画）
│       ├── res/                            # Android 资源
│       └── AndroidManifest.xml             # 包含 Spatial 组件声明的应用清单
├── gradle/                               # Gradle 依赖与构建脚本
├── build.gradle.kts                      # 根构建配置
└── settings.gradle.kts                   # 项目设置与 SDK 配置
```

## 编辑 3D 资源

如需修改或扩展 Animation 示例中的 3D 资源，请按以下步骤操作：

1. 在 Android Studio 中导航至 **app/src/main/assets/** 目录
2. 将现有 GLB/USDZ 文件替换为你的动画模型
3. 更新源代码中的动画配置以匹配新资源
4. 确保模型包含与 Spatial SDK 兼容的动画数据

## 学习资源

如需深入了解空间应用开发，欢迎访问官方开发者网站获取完整资源：

- [PICO 开发者网站（中国大陆）](https://developer-cn.picoxr.com/)
- [PICO 开发者网站（非中国大陆）](https://developer.picoxr.com/)

## 许可证

© 2026 PICO。本项目的源代码依据 [Apache 2.0 License](LICENSE.txt) 协议开源。本项目的美术资产（包括但不限于纹理、3D 模型和音频）基于 [Creative Commons BY 4.0](https://creativecommons.org/licenses/by/4.0/) 授权。

## 支持

如有任何问题、疑惑或功能建议，欢迎通过 [PICO 开发者支持门户](https://picodevsupport.freshdesk.com/support/home) 提交工单与我们联系。

---

为空间应用开发者提供的官方参考示例
