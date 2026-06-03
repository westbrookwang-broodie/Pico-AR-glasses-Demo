# Spatial Nav Demo

[English](README.md) | [简体中文](README_zh.md)

A minimal PICO Spatial SDK app showing how to navigate between multiple 3D panels inside a single spatial window, including a panel that embeds a live 3D model.

## What it does

Three panels wired together with Jetpack Navigation Compose:

- **Hub** — a collaboration board with three sticky notes. Tapping any note opens Board A.
- **Board A** — a resource panel that embeds an animated 3D robot (`SpatialView`). It can move forward to Board B or close back to the Hub.
- **Board B** — a text resource panel that closes back to the Hub.

```
Hub ──tap note──▶ Board A ──continue──▶ Board B
 ▲                   │                     │
 └──────close────────┴─────────close───────┘
```

## Tech stack

- Kotlin + Jetpack Compose
- PICO Spatial SDK 0.12.2 (`SpatialView`, `DefaultWindowContainer`, ECS entities)
- Jetpack Navigation Compose for in-window routing
- Single Gradle module (`:app`), package `com.example.spatialnav`, `arm64-v8a`, `minSdk 26`

## Project structure

```txt
animation-0.12.2/
├── app/                                    # The spatial app module
│   └── src/main/
│       ├── java/com/example/spatialnav/
│       │   ├── Main.kt                      # Entry: DefaultWindowContainer + NavHost (3 routes)
│       │   ├── platform/
│       │   │   ├── SpatialApplication.kt    # launch(::mainApp)
│       │   │   └── LaunchActivity.kt        # SpatialLaunchActivity
│       │   ├── ui/
│       │   │   ├── demo/
│       │   │   │   └── DemoScreens.kt       # Hub / Board A / Board B + Robot3DView + Routes
│       │   │   └── skeletal/
│       │   │       └── SkeletalAnimationViewModel.kt  # Owns the robot Entity, cleans up in onCleared
│       │   ├── util/
│       │   │   └── SkeletalAnimationUtil.kt # Loads the robot model, plays skeletal clips
│       │   └── data/
│       │       └── AnimationModels.kt       # SkeletalAnimationState (clip indices)
│       └── assets/
│           └── pico_robot_animated.glb      # The 3D model shown on Board A
└── skills/                                 # Authoring skill for generating spatial scenes
    └── pico-spatial-scene-builder/
        ├── SKILL.md                         # Workflow + hard constraints for building panels
        ├── agents/
        │   └── openai.yaml                  # UI metadata (display name, prompt)
        ├── references/
        │   ├── sdk-patterns.md              # PICO SDK APIs, build/JDK notes, compile pitfalls
        │   ├── ui-style.md                  # Unified palette / spacing / panel layout
        │   └── reference-image-workflow.md  # Turn an uploaded mockup into panels + routes
        └── assets/
            ├── ui-kit/
            │   └── SpatialUiKit.kt          # Shared style components (copy + rename package)
            └── templates/
                ├── SceneScreens.kt.template
                ├── Scene3DView.kt.template
                └── SceneNavHost.kt.template
```

The app's navigation lives in `Main.kt`; each panel is a Composable in `ui/demo/DemoScreens.kt`. The 3D model is loaded by `SkeletalAnimationViewModel` (in `ui/skeletal/`), which is scoped to Board A's back stack entry, so leaving the panel cleans up the model automatically.

The `skills/` folder holds an authoring skill, `pico-spatial-scene-builder`, that captures the patterns used here so new spatial scenes can be generated with consistent style, image-driven layout, and navigation wiring. See its `SKILL.md` for the workflow.

## Requirements

Set up your environment per the official PICO Spatial SDK guide:

- [Setup Guide (Outside Chinese Mainland)](https://developer.picoxr.com/document/spatial-sdk/set-up-development-environment/)
- [Setup Guide (Chinese Mainland)](https://developer-cn.picoxr.com/document/spatial-sdk/set-up-development-environment/)

JDK 11+ is required (the project is verified with the JDK 17 bundled in Android Studio's JBR).

## Run

1. Open the project in Android Studio.
2. Create a PICO Emulator via **Device Manager**, or connect a PICO device with USB debugging enabled.
3. Click **Run 'app'**.

## Swap the 3D model

1. Drop your animated `.glb` into `app/src/main/assets/`.
2. Update `ANIMATED_ROBOT` and the clip count in `util/SkeletalAnimationUtil.kt` and `data/AnimationModels.kt`.

## License

Source code under the [Apache 2.0 License](LICENSE.txt). 3D assets under [Creative Commons BY 4.0](https://creativecommons.org/licenses/by/4.0/).
