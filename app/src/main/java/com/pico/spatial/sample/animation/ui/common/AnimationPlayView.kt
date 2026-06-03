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
package com.pico.spatial.sample.animation.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pico.spatial.core.container.SpatialViewContent
import com.pico.spatial.core.ecs.event.AnimationEvents
import com.pico.spatial.sample.animation.data.NavigationState
import com.pico.spatial.sample.animation.manager.EventManager
import com.pico.spatial.sample.animation.ui.home.HomeViewModel
import com.pico.spatial.sample.animation.ui.skeletal.SkeletalAnimationViewModel
import com.pico.spatial.sample.animation.ui.tween.TweenAnimationViewModel
import com.pico.spatial.sample.animation.util.SkeletalAnimationUtil
import com.pico.spatial.ui.foundation.content.SpatialView
import com.pico.spatial.ui.foundation.layout.requiredDepth

@Composable
fun AnimationPlayView(
    homeViewModel: HomeViewModel = viewModel(),
    skeletalAnimationViewModel: SkeletalAnimationViewModel = viewModel(),
    tweenAnimationViewModel: TweenAnimationViewModel = viewModel(),
) {
    val skeletalEntity = skeletalAnimationViewModel.entity
    val tweenEntity = tweenAnimationViewModel.entity

    val isSkeletalSelected = homeViewModel.isStateSelected(NavigationState.SKELETAL)

    LaunchedEffect(isSkeletalSelected) {
        if (isSkeletalSelected) {
            skeletalEntity.enabled = true
            tweenEntity.enabled = false
            // Ensure tween entity is reset before switching
            tweenAnimationViewModel.resetControl()
            // Autoplay skeletal using the current default set in TabBar
            skeletalAnimationViewModel.restart()
        } else {
            skeletalEntity.enabled = false
            tweenEntity.enabled = true
            // Ensure skeletal entity is reset before switching
            skeletalAnimationViewModel.animationData?.let {
                SkeletalAnimationUtil.reset(skeletalEntity, it)
            }
            // Autoplay tween using the current default set in TabBar
            tweenAnimationViewModel.restart()
        }
    }

    DisposableEffect(Unit) { onDispose { EventManager.unsubscribeAllAnimationEvents() } }

    SpatialView(
        modifier = Modifier.height(645.dp).fillMaxWidth().requiredDepth(100.dp),
        initial = { content, _ ->
            content.addEntity(skeletalEntity)
            content.addEntity(tweenEntity)
            content.subscribeAnimationEvents()
        }
    )
}

private fun SpatialViewContent.subscribeAnimationEvents() {
    EventManager.subscribeAnimationEvent(this, AnimationEvents.Started::class.java)
    EventManager.subscribeAnimationEvent(this, AnimationEvents.Terminated::class.java)
}
