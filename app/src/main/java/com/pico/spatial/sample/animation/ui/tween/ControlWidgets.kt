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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pico.spatial.core.ecs.animation.EaseType
import com.pico.spatial.core.ecs.animation.RepeatMode
import com.pico.spatial.sample.animation.R
import com.pico.spatial.sample.animation.data.TweenAnimationProperties
import com.pico.spatial.sample.animation.ktx.asTitle
import com.pico.spatial.sample.animation.ktx.cachedEnumValues
import com.pico.spatial.sample.animation.ktx.toDisplayName
import com.pico.spatial.ui.design.Divider
import com.pico.spatial.ui.design.Icon
import com.pico.spatial.ui.design.SegmentControl
import com.pico.spatial.ui.design.SegmentItem
import com.pico.spatial.ui.design.Slider
import com.pico.spatial.ui.design.SliderDefaults
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.menu.Menu
import com.pico.spatial.ui.design.menu.MenuItem
import com.pico.spatial.ui.design.menu.rememberMenuPositionProvider
import com.pico.spatial.ui.design.windows.popup.HorizontalPlacement
import com.pico.spatial.ui.design.windows.popup.VerticalPlacement
import com.pico.spatial.ui.foundation.layout.offset
import com.pico.spatial.ui.foundation.material.backgroundMaterial
import com.pico.spatial.ui.foundation.vibrant.Vibrant
import com.pico.spatial.ui.foundation.vibrant.vibrantEffect
import com.pico.spatial.ui.foundation.vibrant.withVibrant
import com.pico.spatial.ui.graphics.Vibrant
import com.pico.spatial.ui.platform.Material

@Composable
fun ControlWidgets() {
    Column(
        modifier = Modifier.width(310.dp).offset(10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SliderControlWidget(TweenAnimationProperties.DURATION, DURATION_RANGE)
        SliderControlWidget(TweenAnimationProperties.SPEED, SPEED_RANGE)
        RepeatControlsWidget()
        EaseTypeWidget()
    }
}

@Composable
fun SliderControlWidget(
    property: TweenAnimationProperties,
    valueRange: ClosedFloatingPointRange<Float>,
    viewModel: TweenAnimationViewModel = viewModel()
) {
    var sliderValue by remember { mutableFloatStateOf(viewModel.getControl(property) as Float) }
    DisposableEffect(viewModel.state) {
        onDispose { sliderValue = viewModel.getControl(property) as Float }
    }
    Column(
        modifier =
            Modifier.size(310.dp, 88.dp)
                .clip(RoundedCornerShape(8.dp))
                .backgroundMaterial(style = Material.Thick)
    ) {
        Text(
            text = property.name.asTitle(),
            fontSize = 16.sp,
            fontWeight = FontWeight.W500,
            lineHeight = 20.sp,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp),
            color = Color.Vibrant.withVibrant(Vibrant.UltraDark)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.width(290.dp).padding(start = 4.dp, top = 8.dp)
        ) {
            Slider(
                modifier = Modifier.size(256.dp, 32.dp),
                value = sliderValue,
                valueRange = valueRange,
                sliderSpec = SliderDefaults.Small,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { viewModel.updateControl(property, sliderValue) },
                colors =
                    SliderDefaults.sliderColors(
                        trackColor = colorResource(R.color.widget_button_inactive),
                        progressColor = colorResource(R.color.widget_slider_progress),
                        progressHighColor = colorResource(R.color.widget_slider_progress),
                        thumbColor = colorResource(R.color.white),
                        thumbHighColor = colorResource(R.color.white),
                    ),
            )
            Text(
                text = "x%.1f ".format(sliderValue),
                fontSize = 14.sp,
                fontWeight = FontWeight.W500,
                lineHeight = 18.sp,
                modifier = Modifier.height(18.dp),
                color = Color.Vibrant.withVibrant(Vibrant.UltraDark)
            )
        }
    }
}

@Composable
fun RepeatControlsWidget() {
    Column(
        modifier =
            Modifier.clip(RoundedCornerShape(8.dp)).backgroundMaterial(style = Material.Thick)
    ) {
        RepeatCountWidget()
        Divider(Modifier.height(2.dp))
        RepeatModeWidget()
    }
}

@Composable
fun RepeatCountWidget() {
    Column(
        modifier = Modifier.size(310.dp, 106.dp).padding(start = 16.dp, end = 16.dp, top = 16.dp)
    ) {
        Text(
            text = "Repeat Count",
            fontSize = 16.sp,
            fontWeight = FontWeight.W500,
            lineHeight = 20.sp,
            color = Color.Vibrant.withVibrant(Vibrant.UltraDark)
        )
        Spacer(Modifier.height(12.dp))
        CustomSegmentControl(
            TabRowParams.RepeatCountParams(REPEAT_COUNT_RANGE),
            TweenAnimationProperties.REPEAT_COUNT
        )
    }
}

@Composable
fun RepeatModeWidget() {
    Column(
        modifier = Modifier.size(310.dp, 110.dp).padding(start = 16.dp, end = 16.dp, top = 16.dp)
    ) {
        Text(
            text = "Repeat Mode",
            fontSize = 16.sp,
            fontWeight = FontWeight.W500,
            lineHeight = 20.sp,
            color = Color.Vibrant.withVibrant(Vibrant.UltraDark)
        )
        Spacer(Modifier.height(12.dp))
        CustomSegmentControl(
            TabRowParams.RepeatModeParams(REPEAT_MODE_RANGE),
            TweenAnimationProperties.REPEAT_MODE
        )
    }
}

@Composable
fun CustomSegmentControl(
    params: TabRowParams,
    property: TweenAnimationProperties,
    viewModel: TweenAnimationViewModel = viewModel()
) {
    var selectIndex by remember {
        mutableIntStateOf(params.values.indexOf(viewModel.getControl(property)))
    }
    DisposableEffect(viewModel.state) {
        onDispose { selectIndex = params.values.indexOf(viewModel.getControl(property)) }
    }
    val options =
        when (params) {
            is TabRowParams.RepeatModeParams -> params.values.map { it.name.asTitle() }
            is TabRowParams.RepeatCountParams ->
                params.values.map { if (it == -1) "∞" else it.toString() }
        }
    SegmentControl(modifier = Modifier.size(278.dp, 48.dp)) {
        options.forEachIndexed { index, option ->
            SegmentItem(
                title = { Text(text = option) },
                selected = index == selectIndex,
                modifier = Modifier.height(40.dp),
                onClick = {
                    selectIndex = index
                    viewModel.updateControl(property, params.values[index])
                }
            )
        }
    }
}

sealed class TabRowParams {
    abstract val values: List<Any>

    data class RepeatModeParams(override val values: List<RepeatMode>) : TabRowParams()

    data class RepeatCountParams(override val values: List<Int>) : TabRowParams()
}

@Composable
fun EaseTypeWidget() {
    Row(
        modifier =
            Modifier.size(310.dp, 60.dp)
                .clip(RoundedCornerShape(8.dp))
                .backgroundMaterial(style = Material.Thick)
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Ease Type",
            fontSize = 16.sp,
            fontWeight = FontWeight.W500,
            lineHeight = 20.sp,
            modifier = Modifier.height(20.dp).weight(1f),
            color = Color.Vibrant.withVibrant(Vibrant.UltraDark)
        )
        DropdownMenu()
    }
}

@Composable
fun DropdownMenu(viewModel: TweenAnimationViewModel = viewModel()) {
    val easeTypes = cachedEnumValues<EaseType>().filterNot { it == EaseType.UNKNOWN }
    var showMenu by remember { mutableStateOf(false) }
    var selectedIndex by remember {
        mutableIntStateOf(
            easeTypes.indexOf(viewModel.getControl(TweenAnimationProperties.EASE_TYPE))
        )
    }
    DisposableEffect(viewModel.state) {
        onDispose {
            selectedIndex =
                easeTypes.indexOf(viewModel.getControl(TweenAnimationProperties.EASE_TYPE))
        }
    }
    Row(
        Modifier.size(158.dp, 40.dp)
            .offset(10.dp)
            .clip(RoundedCornerShape(32.dp))
            .backgroundMaterial(true, Material.Thin)
            .vibrantEffect(Vibrant.SemiLight)
            .background(Color.Vibrant)
            .clickable { showMenu = !showMenu }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val selectedEaseType = viewModel.getControl(TweenAnimationProperties.EASE_TYPE) as EaseType
        Text(
            text = selectedEaseType.toDisplayName(),
            fontSize = 14.sp,
            fontWeight = FontWeight.W500,
            lineHeight = 16.sp,
            textAlign = TextAlign.Start,
            modifier = Modifier.height(16.dp).weight(1f).padding(start = 4.dp),
            color = Color.Vibrant.withVibrant(Vibrant.UltraDark)
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_dropdown_menu),
            contentDescription = stringResource(R.string.dropdown_menu),
            modifier = Modifier.size(14.dp)
        )
    }
    if (showMenu) {
        Menu(
            modifier =
                Modifier.width(140.dp)
                    .offset(520.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .backgroundMaterial(true, Material.Thin)
                    .vibrantEffect(Vibrant.SemiLight)
                    .background(Color.Vibrant),
            onDismissRequest = { showMenu = false },
            positionProvider =
                rememberMenuPositionProvider(
                    horizontalPlacement = HorizontalPlacement.alignEnd(),
                    verticalPlacement = VerticalPlacement.above(),
                ),
        ) {
            for (easeType in easeTypes) {
                CustomMenuItem(easeType.toDisplayName()) {
                    showMenu = false
                    selectedIndex = easeTypes.indexOf(easeType)
                    viewModel.updateControl(TweenAnimationProperties.EASE_TYPE, easeType)
                }
            }
        }
    }
}

@Composable
fun CustomMenuItem(title: String, onClick: () -> Unit) {
    MenuItem(
        modifier = Modifier.height(32.dp),
        title = {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.W500,
                lineHeight = 16.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.height(16.dp)
            )
        },
        onClick = onClick,
    )
}

// Control ranges
private val DURATION_RANGE = 0.5f..3f
private val SPEED_RANGE = 0.5f..2f
private val REPEAT_COUNT_RANGE = listOf(1, 2, 3, -1)
private val REPEAT_MODE_RANGE = listOf(RepeatMode.RESTART, RepeatMode.REVERSE)
