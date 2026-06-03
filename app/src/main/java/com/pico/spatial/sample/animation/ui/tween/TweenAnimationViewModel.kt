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
package com.pico.spatial.sample.animation.ui.tween

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.sample.animation.data.TweenAnimationControl
import com.pico.spatial.sample.animation.data.TweenAnimationProperties
import com.pico.spatial.sample.animation.data.TweenAnimationState
import com.pico.spatial.sample.animation.util.TweenAnimationUtil

class TweenAnimationViewModel : ViewModel() {
    var state by mutableStateOf(TweenAnimationState.NONE)
        private set

    val entity = Entity()
    var animationData by mutableStateOf<TweenAnimationUtil.TweenAnimationData?>(null)
        private set

    var control by mutableStateOf(TweenAnimationControl())
        private set

    init {
        TweenAnimationUtil.initialize(entity, viewModelScope) { data -> animationData = data }
    }

    fun switchAnimation(state: TweenAnimationState) {
        if (this.state == state) return
        this.state = state
        resetControl()
        animationData?.let { TweenAnimationUtil.play(entity, state, control, it) }
    }

    fun updateControl(property: TweenAnimationProperties, value: Any) {
        control = control.copyWith(property, value)
        animationData?.let { TweenAnimationUtil.play(entity, state, control, it) }
    }

    fun getControl(property: TweenAnimationProperties) = control[property]

    /** Restart the current tween animation from the beginning using existing control. */
    fun restart() {
        animationData?.let { TweenAnimationUtil.play(entity, state, control, it) }
    }

    fun resetControl() {
        animationData?.let { TweenAnimationUtil.reset(entity, it) }
        control = TweenAnimationControl()
    }

    override fun onCleared() {
        super.onCleared()
        animationData?.let { TweenAnimationUtil.reset(entity, it) }
    }
}
