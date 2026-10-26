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

import android.os.SystemClock
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pico.spatial.ui.design.CircularProgressIndicator
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.foundation.hover.spatialHoverEffect
import com.pico.spatial.ui.foundation.material.backgroundMaterial
import com.pico.spatial.ui.platform.Material
import kotlinx.coroutines.isActive

private val PointColor = Color(0xFFCBFCFC)
private val GlassTextColor = Color(0xFF243840)
private val GlassSecondaryColor = Color(0xFF53666E)
private const val PROMPT_TEXT = "当前内容可能与其他成员的材料相关"
private const val ENTRY_TEXT = "查看材料关联 ›"

/**
 * A local overlay for the current viewer, with placement modifiers supplied by the workspace.
 * Unavailable upstream signals render nothing. Only its small visible regions handle hover; no
 * full-window pointer surface or navigation changes. PICO system spatial hover may originate from
 * gaze or a controller. This is intentionally NOT a gaze-only implementation and does not request
 * eye-tracking permissions or construct rays.
 */
@Composable
fun KnowledgeBlackHoleOverlay(
    sourceState: KnowledgeBlackHoleSourceState,
    promptRegionModifier: Modifier,
    knowledgeCardModifier: Modifier
) {
    val input =
        when (sourceState) {
            KnowledgeBlackHoleSourceState.Unavailable -> return
            is KnowledgeBlackHoleSourceState.Ready -> sourceState.input
        }
    if (!input.isEligible) return

    // Recreate interaction sources and cancel old timers immediately on a material/K/P change.
    // Brightness is deliberately excluded so upstream distance updates do not reset dwell.
    key(input.currentMaterial, input.kPairValid, input.pUpgraded) {
        KnowledgeBlackHoleSession(input, promptRegionModifier, knowledgeCardModifier)
    }
}

@Composable
private fun KnowledgeBlackHoleSession(
    input: KnowledgeBlackHoleInput,
    promptRegionModifier: Modifier,
    knowledgeCardModifier: Modifier
) {
    var storedState by remember { mutableStateOf(KnowledgeBlackHoleStateMachine.initial(input)) }
    val currentInput by rememberUpdatedState(input)
    val state =
        KnowledgeBlackHoleStateMachine.reduce(
            storedState,
            KnowledgeBlackHoleEvent.InputChanged(input)
        )
    val onEvent: (KnowledgeBlackHoleEvent) -> Unit = { event ->
        storedState =
            KnowledgeBlackHoleStateMachine.reduce(
                KnowledgeBlackHoleStateMachine.reduce(
                    storedState,
                    KnowledgeBlackHoleEvent.InputChanged(currentInput)
                ),
                event
            )
    }

    LaunchedEffect(state.stage, state.dwellStartedAtMillis, state.knowledgeExitStartedAtMillis) {
        if (
            state.stage == KnowledgeBlackHoleStage.DWELL ||
                state.knowledgeExitStartedAtMillis != null
        ) {
            while (isActive) {
                withFrameNanos { onEvent(KnowledgeBlackHoleEvent.Tick(SystemClock.uptimeMillis())) }
            }
        }
    }

    when (state.stage) {
        KnowledgeBlackHoleStage.POINT,
        KnowledgeBlackHoleStage.PROMPT,
        KnowledgeBlackHoleStage.DWELL -> {
            // This node and its interaction source survive all Point/prompt/dwell transitions.
            // The single Row includes the gap: moving from Point to the entry never exits it.
            KnowledgePromptRegion(
                state = state,
                onEvent = onEvent,
                modifier = promptRegionModifier
            )
        }
        KnowledgeBlackHoleStage.KNOWLEDGE -> {
            KnowledgePointsCard(
                association = KnowledgeAssociations.forMaterial(input.currentMaterial) ?: return,
                onEvent = onEvent,
                modifier = knowledgeCardModifier
            )
        }
        KnowledgeBlackHoleStage.HIDDEN -> Unit
    }
}

@Composable
private fun KnowledgePromptRegion(
    state: KnowledgeBlackHoleState,
    onEvent: (KnowledgeBlackHoleEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val regionSource = remember { MutableInteractionSource() }
    val entrySource = remember { MutableInteractionSource() }
    val regionHovered by regionSource.collectIsHoveredAsState()
    val entryHovered by entrySource.collectIsHoveredAsState()
    val expanded = state.stage != KnowledgeBlackHoleStage.POINT

    LaunchedEffect(regionHovered, entryHovered) {
        // Child hover also proves the union was entered, independent of event delivery order.
        onEvent(KnowledgeBlackHoleEvent.PromptRegionHoverChanged(regionHovered || entryHovered))
        onEvent(KnowledgeBlackHoleEvent.EntryHoverChanged(entryHovered, SystemClock.uptimeMillis()))
    }

    Row(
        modifier = modifier.hoverable(regionSource).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BreathingKnowledgePoint(state.input.normalizedBrightness)
        if (expanded) {
            Spacer(Modifier.width(8.dp))
            Column(
                modifier =
                    Modifier.width(296.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .backgroundMaterial(style = Material.Thin)
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(1.dp, PointColor.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                        .semantics {
                            contentDescription = "材料关联提示卡"
                            stateDescription =
                                if (state.stage == KnowledgeBlackHoleStage.DWELL) "连续驻留中"
                                else "等待查看材料关联"
                        },
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = PROMPT_TEXT,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = GlassTextColor
                )
                Text(
                    text =
                        KnowledgeAssociations.forMaterial(state.input.currentMaterial)
                            ?.keywords
                            .orEmpty(),
                    fontSize = 12.sp,
                    color = GlassSecondaryColor
                )
                Row(
                    modifier =
                        Modifier.clip(RoundedCornerShape(10.dp))
                            .hoverable(entrySource)
                            .spatialHoverEffect()
                            .background(PointColor.copy(alpha = 0.22f))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .semantics { contentDescription = "查看材料关联，连续驻留 1.5 秒" },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = ENTRY_TEXT,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W600,
                        color = GlassTextColor
                    )
                    // Reserve the progress footprint to avoid changing the entry's hover bounds.
                    Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                        if (state.stage == KnowledgeBlackHoleStage.DWELL) {
                            CircularProgressIndicator(
                                progress = { state.dwellProgress },
                                modifier =
                                    Modifier.size(28.dp).semantics {
                                        contentDescription = "材料关联驻留进度"
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BreathingKnowledgePoint(brightness: Float) {
    val transition = rememberInfiniteTransition(label = "Knowledge Point breathing")
    // Two 2-second legs give one full 4-second cycle, synchronizing alpha and scale.
    val breath by
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
            label = "Knowledge Point breath phase"
        )
    Box(
        modifier = Modifier.size(44.dp).semantics { contentDescription = "知识黑洞呼吸 Point" },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier =
                Modifier.size(20.dp)
                    .graphicsLayer {
                        alpha = (0.20f + 0.10f * breath) * brightness
                        scaleX = 0.90f + 0.20f * breath
                        scaleY = scaleX
                    }
                    .clip(CircleShape)
                    .background(PointColor)
        )
    }
}

@Composable
private fun KnowledgePointsCard(
    association: KnowledgeAssociation,
    onEvent: (KnowledgeBlackHoleEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    LaunchedEffect(hovered) {
        // Initial false is ignored by the reducer; only a real enter arms the exit timer.
        onEvent(KnowledgeBlackHoleEvent.KnowledgeHoverChanged(hovered, SystemClock.uptimeMillis()))
    }
    Column(
        modifier =
            modifier
                .widthIn(max = 420.dp)
                .clip(RoundedCornerShape(20.dp))
                .hoverable(source)
                .backgroundMaterial(style = Material.Thin)
                .background(Color.White.copy(alpha = 0.22f))
                .border(1.dp, PointColor.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                .padding(24.dp)
                .semantics { contentDescription = "关联材料 ${association.relatedMaterial.name} 知识要点" },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "关联材料 ${association.relatedMaterial.name}",
            fontSize = 12.sp,
            color = GlassSecondaryColor
        )
        Text(
            text = association.title,
            fontSize = 24.sp,
            fontWeight = FontWeight.W700,
            color = GlassTextColor
        )
        Text(text = association.keywords, fontSize = 14.sp, color = GlassSecondaryColor)
        Spacer(Modifier.height(2.dp))
        association.points.forEachIndexed { index, point ->
            Text(
                text = "${index + 1}. $point",
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = GlassTextColor
            )
        }
    }
}
