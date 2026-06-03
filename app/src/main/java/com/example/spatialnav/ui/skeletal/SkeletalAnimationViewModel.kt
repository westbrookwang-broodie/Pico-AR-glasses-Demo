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
package com.example.spatialnav.ui.skeletal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spatialnav.data.SkeletalAnimationState
import com.example.spatialnav.util.SkeletalAnimationUtil
import com.pico.spatial.core.ecs.Entity

class SkeletalAnimationViewModel : ViewModel() {
    var state by mutableStateOf(SkeletalAnimationState.STANDBY_MODE)
        private set

    val entity = Entity()
    var animationData by mutableStateOf<SkeletalAnimationUtil.SkeletalAnimationData?>(null)
        private set

    init {
        SkeletalAnimationUtil.initialize(entity, viewModelScope) { data -> animationData = data }
    }

    fun switchAnim(state: SkeletalAnimationState) {
        if (this.state == state) return
        this.state = state
        animationData?.let { SkeletalAnimationUtil.play(state, it) }
    }

    /** Restart the current skeletal animation from the beginning. */
    fun restart() {
        animationData?.let { SkeletalAnimationUtil.play(state, it) }
    }

    override fun onCleared() {
        super.onCleared()
        animationData?.let {
            SkeletalAnimationUtil.reset(entity, it)
            SkeletalAnimationUtil.closeResources(it)
        }
    }
}
