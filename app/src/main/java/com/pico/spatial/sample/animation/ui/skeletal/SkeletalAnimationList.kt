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
package com.pico.spatial.sample.animation.ui.skeletal

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pico.spatial.sample.animation.data.NavigationState
import com.pico.spatial.sample.animation.data.SkeletalAnimationState
import com.pico.spatial.sample.animation.ktx.asTitle
import com.pico.spatial.sample.animation.ktx.cachedEnumValues
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.foundation.hover.spatialHoverEffect
import com.pico.spatial.ui.foundation.vibrant.Vibrant
import com.pico.spatial.ui.foundation.vibrant.withVibrant
import com.pico.spatial.ui.graphics.Vibrant

@Composable
fun SkeletalAnimationList(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = NavigationState.SKELETAL.name,
            fontSize = 28.sp,
            fontWeight = FontWeight.W700,
            lineHeight = 34.sp,
            modifier = Modifier.height(96.dp).padding(32.dp),
            color = Color.Vibrant.withVibrant(Vibrant.Darkest)
        )
        LazyVerticalGrid(
            modifier = Modifier.size(530.dp, 456.dp).align(Alignment.CenterHorizontally),
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(cachedEnumValues<SkeletalAnimationState>()) { state ->
                SkeletalAnimationItemCard(state)
            }
        }
    }
}

@Composable
fun SkeletalAnimationItemCard(
    skeletalAnimationState: SkeletalAnimationState,
    viewModel: SkeletalAnimationViewModel = viewModel()
) {
    Column(
        modifier =
            Modifier.size(167.dp, 166.dp)
                .semantics {
                    contentDescription =
                        "${ skeletalAnimationState.name.asTitle()} skeletal animation item"
                }
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (viewModel.state == skeletalAnimationState) Color(0x6FFFFFFF)
                    else Color.Transparent
                )
                .clickable { viewModel.switchAnim(skeletalAnimationState) }
                .spatialHoverEffect(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = skeletalAnimationState.imageRes),
            contentDescription = stringResource(skeletalAnimationState.descriptionRes),
            modifier = Modifier.size(150.dp, 120.dp).clip(RoundedCornerShape(4.dp))
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(skeletalAnimationState.descriptionRes),
            fontSize = 16.sp,
            fontWeight = FontWeight.W500,
            lineHeight = 20.sp,
            color = Color.Vibrant.withVibrant(Vibrant.UltraDark)
        )
    }
}
