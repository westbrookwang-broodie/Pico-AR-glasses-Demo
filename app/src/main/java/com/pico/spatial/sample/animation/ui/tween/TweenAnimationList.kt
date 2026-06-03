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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pico.spatial.sample.animation.R
import com.pico.spatial.sample.animation.data.NavigationState
import com.pico.spatial.sample.animation.data.TweenAnimationState
import com.pico.spatial.sample.animation.ktx.asTitle
import com.pico.spatial.sample.animation.ktx.cachedEnumValues
import com.pico.spatial.ui.design.Icon
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.foundation.hover.spatialHoverEffect
import com.pico.spatial.ui.foundation.vibrant.Vibrant
import com.pico.spatial.ui.foundation.vibrant.withVibrant
import com.pico.spatial.ui.graphics.Vibrant

@Composable
fun TweenAnimationList(
    modifier: Modifier = Modifier,
    viewModel: TweenAnimationViewModel = viewModel()
) {
    Row(modifier = modifier) {
        Column(modifier = Modifier.width(222.dp)) {
            Text(
                text = NavigationState.TWEEN.name,
                fontSize = 28.sp,
                fontWeight = FontWeight.W700,
                lineHeight = 34.sp,
                modifier = Modifier.height(96.dp).padding(32.dp),
                color = Color.Vibrant.withVibrant(Vibrant.Darkest)
            )
            LazyColumn(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(cachedEnumValues<TweenAnimationState>().drop(1)) { animState ->
                    TweenAnimationListItem(animState)
                }
            }
        }
        if (viewModel.state != TweenAnimationState.NONE) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 32.dp)) {
                Box(modifier = Modifier.size(310.dp, 96.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = viewModel.state.name.asTitle(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.W700,
                        lineHeight = 30.sp,
                        color = Color.Vibrant.withVibrant(Vibrant.UltraDark)
                    )
                }
                ControlWidgets()
            }
        }
    }
}

@Composable
fun TweenAnimationListItem(
    animationState: TweenAnimationState,
    viewModel: TweenAnimationViewModel = viewModel()
) {
    Row(
        modifier =
            Modifier.fillMaxSize()
                .semantics {
                    contentDescription = "${ animationState.name.asTitle()} tween animation item"
                }
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (viewModel.state == animationState) Color(0x9FFFFFFF) else Color.Transparent
                )
                .clickable { viewModel.switchAnimation(animationState) }
                .spatialHoverEffect(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(R.drawable.ic_list_item_circle),
            contentDescription = null,
            modifier = Modifier.padding(14.dp)
        )
        Text(
            text = animationState.name.asTitle(),
            fontSize = 16.sp,
            lineHeight = 24.sp,
            color = Color.Vibrant.withVibrant(Vibrant.UltraDark)
        )
    }
}
