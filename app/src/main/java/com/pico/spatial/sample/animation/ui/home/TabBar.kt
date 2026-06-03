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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pico.spatial.sample.animation.data.NavigationState
import com.pico.spatial.sample.animation.data.SkeletalAnimationState
import com.pico.spatial.sample.animation.data.TweenAnimationState
import com.pico.spatial.sample.animation.ktx.asTitle
import com.pico.spatial.sample.animation.ui.skeletal.SkeletalAnimationViewModel
import com.pico.spatial.sample.animation.ui.tween.TweenAnimationViewModel
import com.pico.spatial.ui.design.Icon
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.windows.TabBar
import com.pico.spatial.ui.platform.ViewPoint

@Composable
fun AnimationTypeTabBar(
    stateList: List<NavigationState> = NavigationState.getList(),
    homeViewModel: HomeViewModel = viewModel(),
    skeletalAnimationViewModel: SkeletalAnimationViewModel = viewModel(),
    tweenAnimationViewModel: TweenAnimationViewModel = viewModel(),
) {
    TabBar(
        followViewpoints = ViewPoint.FrontOnly,
    ) {
        stateList.forEachIndexed { _, state ->
            item(
                selected = homeViewModel.isStateSelected(state),
                modifier =
                    Modifier.semantics { contentDescription = "${state.name.asTitle()} tab" },
                mainContent = {
                    Icon(
                        painter = painterResource(id = state.icon),
                        contentDescription = "${state.name.asTitle()} animation"
                    )
                },
                supportContent = { Text(state.name.asTitle()) },
                onClick = {
                    if (!homeViewModel.isStateSelected(state)) {
                        // Default selections on every tab switch (state only)
                        skeletalAnimationViewModel.switchAnim(SkeletalAnimationState.STANDBY_MODE)
                        tweenAnimationViewModel.switchAnimation(TweenAnimationState.POSITION)
                        homeViewModel.switchMenu(state)
                    }
                }
            )
        }
    }
}
