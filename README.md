# Animation

[English](README.md) | [简体中文](README_zh.md)

A reference application for PICO OS 6 demonstrating best practices for implementing skeletal and tween animations with the PICO Spatial SDK.

## Overview

This sample provides a practical guide for developers to explore and implement animation capabilities of the PICO Spatial SDK. Through an interactive 3D environment, users can experience skeletal animations (character bone-based movements), tween animations (smooth transform and material interpolations), and animation playback controls.

The app follows MVVM architecture with stateless utility classes and proper Android lifecycle management for efficient animation resource handling.

![Screenshot](.github/screenshot.png)

## Features

Animation Sample highlights the following core animation capabilities of the PICO Spatial SDK:

- **3D Model Integration**: load and animate GLB/USDZ models in spatial environments
- **Skeletal Animation System**: bone-based character animations with multiple animation states
- **Tween Animation Framework**: smooth interpolation for 3D object transform and material properties
- **Animation Control Interface**: interactive controls for playback, speed adjustment, and state management
- **Animation Resource Management**: efficient loading and cleanup of animation resources
- **MVVM Architecture**: clean separation of concerns with stateless utility classes and ViewModel state management
- **Lifecycle Management**: proper handling of animation resources during Android lifecycle events

## Requirements

To build and run this project, configure your development environment by following the official PICO Spatial SDK setup guide:

- [PICO Spatial SDK Setup Guide (Outside Chinese Mainland)](https://developer.picoxr.com/document/spatial-sdk/set-up-development-environment/)
- [PICO Spatial SDK Setup Guide (Chinese Mainland)](https://developer-cn.picoxr.com/document/spatial-sdk/set-up-development-environment/)

## Getting Started

### 1. Clone the Repository

```bash
git clone <repository-url>
cd Animation
```

### 2. Open in Android Studio

Launch Android Studio, select **Open an Existing Project**, and choose the `Animation` directory.

### 3. Run the App

#### On Emulator

1. Open **Device Manager** (toolbar or **Tools > Device Manager**)
2. Click **Add a new device** > **Create a PICO Emulator**
3. Start the emulator from **Device Manager**
4. Click **Run 'app'** to launch the app

#### On Physical Device

1. Connect your PICO device to your development machine
2. Enable **USB Debugging** on the device
3. Click **Run 'app'** to deploy directly to your device

## Project Structure

Animation Sample follows a modular architecture to separate application logic from 3D resources:

```txt
Animation/
├── app/                                  # Main application module
│   └── src/main/
│       ├── java/com/pico/spatial/sample/animation/ # Kotlin source files
│       │   ├── Main.kt                     # App entry point
│       │   ├── data/                       # Animation state definitions
│       │   │   └── AnimationModels.kt
│       │   ├── ktx/                        # Extension functions
│       │   │   └── Extensions.kt
│       │   ├── manager/                    # Event and animation managers
│       │   │   └── EventManager.kt
│       │   ├── platform/                   # Platform-specific implementations
│       │   │   ├── LaunchActivity.kt
│       │   │   └── SpatialApplication.kt   # App initialization
│       │   ├── ui/                         # UI components and pages (Compose)
│       │   │   ├── common/
│       │   │   │   └── AnimationPlayView.kt # Main play view with auto-restart logic
│       │   │   ├── home/                   # Home page and navigation
│       │   │   ├── skeletal/               # Skeletal animation UI and ViewModel
│       │   │   └── tween/                  # Tween animation UI and ViewModel
│       │   └── util/                       # Stateless animation utilities
│       │       ├── SkeletalAnimationUtil.kt # Skeletal animation management
│       │       └── TweenAnimationUtil.kt   # Tween animation management
│       ├── assets/                         # 3D models and animation assets
│       │   ├── pico_robot_animated.glb     # Animated robot model (skeletal)
│       │   └── pico_robot_static.usdz      # Static robot model (tween)
│       ├── res/                            # Android resources
│       └── AndroidManifest.xml             # App manifest with Spatial component declarations
├── gradle/                               # Gradle dependencies and scripts
├── build.gradle.kts                      # Root build configuration
└── settings.gradle.kts                   # Project settings and SDK configuration
```

## Editing 3D Assets

To modify or extend the 3D assets in Animation Sample:

1. In Android Studio, navigate to the **app/src/main/assets/** directory
2. Replace the existing GLB/USDZ files with your animated models
3. Update the animation configurations in source code to match your new assets
4. Ensure your models contain animation data compatible with the Spatial SDK

## Learning Resources

To deepen your understanding of spatial app development, explore the resources on our official developer websites:

- [PICO Developer Website (Outside Chinese Mainland)](https://developer.picoxr.com/)
- [PICO Developer Website (Chinese Mainland)](https://developer-cn.picoxr.com/)

## License

© 2026 PICO. The project's source code is licensed under the [Apache 2.0 License](LICENSE.txt). All assets, including textures, 3D models, audio, and others, are licensed via [Creative Commons BY 4.0](https://creativecommons.org/licenses/by/4.0/).

## Support

For questions, issues, or feature requests, please contact us through the [PICO Developer Support Portal](https://picodevsupport.freshdesk.com/support/home) and submit a ticket.

---

Developed as a reference for spatial application developers
