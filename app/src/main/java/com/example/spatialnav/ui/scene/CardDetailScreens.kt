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
package com.example.spatialnav.ui.scene

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.spatialnav.ui.skeletal.SkeletalAnimationViewModel
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.foundation.content.SpatialView
import com.pico.spatial.ui.foundation.layout.requiredDepth

/**
 * Card A detail — "关键词洞察". Mirrors the expanded main card in the demo and embeds the animated
 * 3D robot to represent the embodied-intelligence subject.
 */
@Composable
fun CardADetailScreen(onClose: () -> Unit) {
    val card = SceneData.cardA
    PanelScaffold(card = card, modifier = Modifier.fillMaxSize(), onClose = onClose) {
        Text(text = card.relation, fontSize = 14.sp, color = Palette.TextGray)
        Spacer(Modifier.height(4.dp))
        Text(text = card.body, fontSize = 16.sp, lineHeight = 26.sp, color = Palette.TextDark)
        Spacer(Modifier.height(12.dp))
        ExpandedNotes(card.expanded)
        Spacer(Modifier.height(16.dp))
        Robot3DView()
        Spacer(Modifier.height(16.dp))
        PrimaryButton(text = "▶ ${card.actionLabel}", onClick = onClose)
    }
}

/** Card B detail — "案例补充". Text-only panel. */
@Composable
fun CardBDetailScreen(onClose: () -> Unit) = CardTextDetail(SceneData.cardB, onClose)

/** Card C detail — "趋势延展". Text-only panel. */
@Composable
fun CardCDetailScreen(onClose: () -> Unit) = CardTextDetail(SceneData.cardC, onClose)

@Composable
private fun CardTextDetail(card: CardContent, onClose: () -> Unit) {
    PanelScaffold(card = card, modifier = Modifier.fillMaxSize(), onClose = onClose) {
        Text(text = card.relation, fontSize = 14.sp, color = Palette.TextGray)
        Spacer(Modifier.height(4.dp))
        Text(text = card.body, fontSize = 16.sp, lineHeight = 26.sp, color = Palette.TextDark)
        Spacer(Modifier.height(12.dp))
        ExpandedNotes(card.expanded)
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SoftButton(text = "✎ ${card.actionLabel}", onClick = onClose)
            SoftButton(text = "✕ 关闭，返回工作台", onClick = onClose)
        }
    }
}

/** The expandable supplementary lines (the "card-expanded" block in the HTML). */
@Composable
private fun ExpandedNotes(lines: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        lines.forEach { line ->
            Text(text = "· $line", fontSize = 13.sp, lineHeight = 20.sp, color = Palette.TextGray)
        }
    }
}

/* ----------------------------- 3D 视图（复用骨骼机器人） ----------------------------- */

/**
 * Renders the animated robot inside a SpatialView. Reuses SkeletalAnimationViewModel, which loads
 * the model and plays the standby animation on init. Scoped to this destination's back-stack entry,
 * so leaving the page triggers its onCleared cleanup automatically.
 */
@Composable
private fun Robot3DView(viewModel: SkeletalAnimationViewModel = viewModel()) {
    LaunchedEffect(Unit) {
        viewModel.entity.enabled = true
        viewModel.restart()
    }
    SpatialView(
        modifier = Modifier.height(260.dp).fillMaxWidth().requiredDepth(100.dp),
        initial = { content, _ -> content.addEntity(viewModel.entity) }
    )
}
