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
package com.pico.spatial.sample.animation.data

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.pico.spatial.core.ecs.animation.EaseType
import com.pico.spatial.core.ecs.animation.RepeatMode
import com.pico.spatial.core.math.Color4
import com.pico.spatial.sample.animation.R

enum class NavigationState(val value: Int, val icon: Int) {
    SKELETAL(0, R.drawable.ic_skeletal_animation),
    TWEEN(1, R.drawable.ic_tween_animation);

    companion object {
        fun getList(): List<NavigationState> = NavigationState.entries.toList()
    }
}

enum class SkeletalAnimationState(
    val value: Int,
    @DrawableRes val imageRes: Int,
    @StringRes val descriptionRes: Int
) {
    STANDBY_MODE(0, R.drawable.img_standby_mode, R.string.standby_mode),
    SPIN_LEAP(1, R.drawable.img_spin_leap, R.string.spin_leap),
    CURIOUS_LOOK(2, R.drawable.img_curious_look, R.string.curious_look),
    TURBO_DASH(3, R.drawable.img_turbo_dash, R.string.turbo_dash),
    HELLO_WAVE(4, R.drawable.img_hello_wave, R.string.hello_wave)
}

data class MaterialProperties(
    var baseColor: Color4 = Color4.WHITE,
    var metallic: Float = 1f,
    var roughness: Float = 1f,
    var emissiveColor: Color4 = Color4.WHITE,
    var opacity: Float = 1f
)

data class TweenAnimationControl(
    val duration: Float = 1.0F,
    val speed: Float = 1.0F,
    val repeatCount: Int = -1,
    val repeatMode: RepeatMode = RepeatMode.REVERSE,
    val easeType: EaseType = EaseType.EASE_INOUT,
) {
    operator fun get(property: TweenAnimationProperties): Any {
        return when (property) {
            TweenAnimationProperties.DURATION -> duration
            TweenAnimationProperties.SPEED -> speed
            TweenAnimationProperties.REPEAT_COUNT -> repeatCount
            TweenAnimationProperties.REPEAT_MODE -> repeatMode
            TweenAnimationProperties.EASE_TYPE -> easeType
        }
    }

    fun copyWith(property: TweenAnimationProperties, value: Any): TweenAnimationControl {
        return when (property) {
            TweenAnimationProperties.DURATION -> copy(duration = value as Float)
            TweenAnimationProperties.SPEED -> copy(speed = value as Float)
            TweenAnimationProperties.REPEAT_COUNT -> copy(repeatCount = value as Int)
            TweenAnimationProperties.REPEAT_MODE -> copy(repeatMode = value as RepeatMode)
            TweenAnimationProperties.EASE_TYPE -> copy(easeType = value as EaseType)
        }
    }
}

enum class TweenAnimationProperties(val value: Int) {
    DURATION(0),
    SPEED(1),
    REPEAT_COUNT(2),
    REPEAT_MODE(3),
    EASE_TYPE(4)
}

enum class TweenAnimationState(val value: Int) {
    NONE(0),
    POSITION(1),
    ROTATION(2),
    SCALE(3),
    TRANSFORM(4),
    BASE_COLOR(5),
    OPACITY(6),
    METALLIC(7),
    ROUGHNESS(8),
    EMISSIVE(9)
}
