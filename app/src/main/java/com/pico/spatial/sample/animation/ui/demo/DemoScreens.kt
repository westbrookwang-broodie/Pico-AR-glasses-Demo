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
package com.pico.spatial.sample.animation.ui.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pico.spatial.sample.animation.ui.skeletal.SkeletalAnimationViewModel
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.foundation.content.SpatialView
import com.pico.spatial.ui.foundation.hover.spatialHoverEffect
import com.pico.spatial.ui.foundation.layout.requiredDepth
import com.pico.spatial.ui.foundation.material.backgroundMaterial
import com.pico.spatial.ui.platform.Material

/** Route names for the three-panel navigation demo. */
object Routes {
    const val HUB = "hub" // 协作板（便签）
    const val BOARD_A = "board_a" // 资料板 A
    const val BOARD_B = "board_b" // 资料板 B
}

private val TextDark = Color(0xFF1A1A1A)
private val TextGray = Color(0xFF666666)
private val NoteBlue = Color(0xFFBBD2FF) // 共识点
private val NotePink = Color(0xFFFFC1C8) // 疑问点
private val NoteGreen = Color(0xFFC2EBB8) // 新提议

/** A pill-style button using the spatial material background. */
@Composable
fun DemoButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(10.dp))
                .backgroundMaterial(style = Material.Thin)
                .clickable { onClick() }
                .spatialHoverEffect()
                .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.W600, color = TextDark)
    }
}

/** Top-right "✕" close button. */
@Composable
private fun CloseButton(onClick: () -> Unit) {
    Box(
        modifier =
            Modifier.size(36.dp)
                .clip(CircleShape)
                .clickable { onClick() }
                .spatialHoverEffect(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "✕", fontSize = 20.sp, fontWeight = FontWeight.W600, color = TextGray)
    }
}

/* ----------------------------- 页面 1：协作板（便签 Hub） ----------------------------- */

/**
 * The collaboration hub. Shows three sticky notes. Tapping a note opens resource board A. Mirrors
 * the bottom panel in the reference screenshot ("共识点 / 疑问点 / 新提议").
 */
@Composable
fun HubScreen(onOpenBoard: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(40.dp)) {
        Text(
            text = "拾头共振 · 协作板",
            fontSize = 30.sp,
            fontWeight = FontWeight.W700,
            color = TextDark
        )
        Spacer(Modifier.height(8.dp))
        Text(text = "点击任意便签进入资料板", fontSize = 16.sp, color = TextGray)
        Spacer(Modifier.height(40.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            StickyNote("共识点", NoteBlue, onOpenBoard)
            StickyNote("疑问点", NotePink, onOpenBoard)
            StickyNote("新提议", NoteGreen, onOpenBoard)
        }
        Spacer(Modifier.height(40.dp))
        Box(
            modifier =
                Modifier.clip(RoundedCornerShape(20.dp))
                    .backgroundMaterial(style = Material.Thin)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(text = "● 多人协作中  3人在线", fontSize = 14.sp, color = TextGray)
        }
    }
}

@Composable
private fun StickyNote(title: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier =
            Modifier.size(160.dp, 120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color)
                .clickable { onClick() }
                .spatialHoverEffect(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.W700, color = TextDark)
    }
}

/* ----------------------------- 页面 2：资料板 A ----------------------------- */

/** Resource board A: title, abstract, an embedded 3D model, and navigation to board B. */
@Composable
fun BoardAScreen(onNext: () -> Unit, onClose: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "A 资料板",
                fontSize = 26.sp,
                fontWeight = FontWeight.W700,
                color = TextDark,
                modifier = Modifier.weight(1f)
            )
            CloseButton(onClose)
        }
        Spacer(Modifier.height(16.dp))
        Text(text = "具身智能发展综述", fontSize = 22.sp, fontWeight = FontWeight.W600, color = TextDark)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "当前具身智能在环境理解上取得突破……",
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = TextGray
        )
        Spacer(Modifier.height(16.dp))
        Robot3DView()
        Spacer(Modifier.height(20.dp))
        DemoButton(text = "继续学习 →", onClick = onNext)
    }
}

/* ----------------------------- 页面 3：资料板 B ----------------------------- */

/** Resource board B: text-only panel that closes back to the hub. */
@Composable
fun BoardBScreen(onClose: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "B 资料板",
                fontSize = 26.sp,
                fontWeight = FontWeight.W700,
                color = TextDark,
                modifier = Modifier.weight(1f)
            )
            CloseButton(onClose)
        }
        Spacer(Modifier.height(16.dp))
        Text(text = "空间计算的未来", fontSize = 22.sp, fontWeight = FontWeight.W600, color = TextDark)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "空间计算将重塑人机交互范式……",
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = TextGray
        )
        Spacer(Modifier.weight(1f))
        DemoButton(text = "✕ 关闭，返回协作板", onClick = onClose)
    }
}

/* ----------------------------- 3D 视图（复用骨骼机器人） ----------------------------- */

/**
 * Renders the animated robot inside a SpatialView. Reuses SkeletalAnimationViewModel, which loads
 * the model and plays the standby animation on initialization. Scoped to this destination's back
 * stack entry, so leaving the page triggers its onCleared cleanup automatically.
 */
@Composable
private fun Robot3DView(viewModel: SkeletalAnimationViewModel = viewModel()) {
    LaunchedEffect(Unit) {
        viewModel.entity.enabled = true
        viewModel.restart()
    }
    SpatialView(
        modifier = Modifier.height(280.dp).fillMaxWidth().requiredDepth(100.dp),
        initial = { content, _ -> content.addEntity(viewModel.entity) }
    )
}
