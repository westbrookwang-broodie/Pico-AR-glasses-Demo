/*
 * Copyright 2025 - 2026 PICO. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.spatialnav.data

/**
 * Skeletal animation clips bundled in the robot model. The [value] is the clip index inside the
 * model's animation resource array, used by SkeletalAnimationUtil to select which clip to play.
 */
enum class SkeletalAnimationState(val value: Int) {
    STANDBY_MODE(0),
    SPIN_LEAP(1),
    CURIOUS_LOOK(2),
    TURBO_DASH(3),
    HELLO_WAVE(4)
}
