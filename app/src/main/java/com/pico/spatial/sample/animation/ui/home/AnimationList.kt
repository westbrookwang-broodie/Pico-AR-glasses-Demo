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
package com.pico.spatial.sample.animation.ui.home

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pico.spatial.sample.animation.data.NavigationState
import com.pico.spatial.sample.animation.ui.skeletal.SkeletalAnimationList
import com.pico.spatial.sample.animation.ui.tween.TweenAnimationList
import com.pico.spatial.ui.foundation.layout.offset
import com.pico.spatial.ui.foundation.material.backgroundMaterial

@Composable
fun AnimationList(viewModel: HomeViewModel = viewModel()) {
    val modifier =
        Modifier.size(580.dp, 645.dp)
            .offset(100.dp, 0.dp)
            .offset(300.dp)
            .backgroundMaterial()
            .clip(RoundedCornerShape(16.dp))
    if (viewModel.isStateSelected(NavigationState.SKELETAL)) {
        SkeletalAnimationList(modifier)
    } else {
        TweenAnimationList(modifier)
    }
}
