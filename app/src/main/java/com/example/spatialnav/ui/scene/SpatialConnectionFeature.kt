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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pico.spatial.ui.design.CircularProgressIndicator
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.foundation.hover.spatialHoverEffect
import com.pico.spatial.ui.foundation.material.backgroundMaterial
import com.pico.spatial.ui.platform.Material
import kotlin.math.roundToInt
import kotlinx.coroutines.isActive

private val ConnectionColor = Color(0xFFCBFCFC)

/** Owned ABOVE NavHost. Navigation never resets this session's reducer state or in-memory log. */
class SpatialConnectionSession(source: SpatialConnectionSourceState) {
    private val simulation = source as? SpatialConnectionSourceState.Simulation
    val available: Boolean = simulation != null
    var state by
        mutableStateOf(
            SpatialConnectionStateMachine.initial(
                simulation?.scenarioId ?: "unavailable",
                simulation?.config ?: SpatialConnectionConfig()
            )
        )
        private set

    private val sessionRecords = mutableStateListOf<SpatialConnectionRecord>()
    val records: List<SpatialConnectionRecord>
        get() = sessionRecords

    fun dispatch(event: SpatialConnectionEvent) {
        if (!available) return
        val transition = SpatialConnectionStateMachine.reduce(state, event)
        state = transition.state
        sessionRecords.addAll(transition.records)
    }
}

@Composable
fun rememberSpatialConnectionSession(
    source: SpatialConnectionSourceState
): SpatialConnectionSession {
    val session = remember(source) { SpatialConnectionSession(source) }
    // SystemClock is used only by the UI adapter, never by the pure Kotlin reducer.
    LaunchedEffect(session, session.state.needsTick) {
        if (session.available && session.state.needsTick) {
            while (isActive) {
                withFrameNanos {
                    session.dispatch(SpatialConnectionEvent.Tick(SystemClock.uptimeMillis()))
                }
            }
        }
    }
    return session
}

/** All measured rectangles use DefaultWindowContainer's root-window coordinate system. */
data class SpatialConnectionAnchors(
    val workspace: Rect? = null,
    val a: Rect? = null,
    val b: Rect? = null,
    val c: Rect? = null
)

private data class PreviewCurve(
    val start: Offset,
    val control1: Offset,
    val control2: Offset,
    val end: Offset
) {
    val midpoint: Offset
        get() = start * 0.125f + control1 * 0.375f + control2 * 0.375f + end * 0.125f
}

/**
 * A 2D rehearsal only. Neither the full-window Box nor Canvas handles input. Only the visible Point
 * and confirmation entry accept local continuous hover (not a gaze-only API or A click).
 */
@Composable
fun SpatialConnectionOverlay(session: SpatialConnectionSession, anchors: SpatialConnectionAnchors) {
    if (!session.available || !session.state.connectionVisible) return
    val state = session.state
    val root = anchors.workspace ?: return
    val a = anchors.a ?: return
    val target =
        when (state.activeTarget) {
            SpatialParticipant.B -> anchors.b
            SpatialParticipant.C -> anchors.c
            null -> null
        } ?: return
    val density = LocalDensity.current
    val rise = with(density) { 80.dp.toPx() }
    val start = Offset(a.right - a.width * 0.12f, a.top + a.height * 0.15f) - root.topLeft
    val end =
        Offset(
            if (state.activeTarget == SpatialParticipant.B) target.right - target.width * 0.07f
            else target.left + target.width * 0.07f,
            target.top + target.height * 0.28f
        ) - root.topLeft
    val crest = (minOf(start.y, end.y) - rise).coerceAtLeast(with(density) { 30.dp.toPx() })
    val curve =
        PreviewCurve(
            start,
            Offset(start.x + (end.x - start.x) * 0.35f, crest),
            Offset(start.x + (end.x - start.x) * 0.65f, crest),
            end
        )
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val path =
                Path().apply {
                    moveTo(curve.start.x, curve.start.y)
                    cubicTo(
                        curve.control1.x,
                        curve.control1.y,
                        curve.control2.x,
                        curve.control2.y,
                        curve.end.x,
                        curve.end.y
                    )
                }
            drawPath(path, ConnectionColor.copy(alpha = 0.24f), style = Stroke(width = 6.dp.toPx()))
            drawPath(path, ConnectionColor, style = Stroke(width = 1.5.dp.toPx()))
            drawCircle(ConnectionColor, radius = 4.dp.toPx(), center = curve.start)
            drawCircle(ConnectionColor, radius = 4.dp.toPx(), center = curve.end)
        }
        AnchorLabel("A 模拟锚点 · 2D预演", curve.start)
        AnchorLabel("${state.activeTarget} 模拟锚点 · 2D预演", curve.end)
        key(state.roundId, state.activeTarget) {
            if (state.pointVisible) {
                val halfPoint = with(density) { 26.dp.toPx() }
                SpatialBreathingPoint(
                    state = state,
                    onEvent = session::dispatch,
                    modifier =
                        Modifier.offset {
                            IntOffset(
                                (curve.midpoint.x - halfPoint).roundToInt(),
                                (curve.midpoint.y - halfPoint).roundToInt()
                            )
                        }
                )
            }
            if (state.inviteVisible) {
                var cardSize by remember { mutableStateOf(IntSize.Zero) }
                val gap = with(density) { 32.dp.toPx() }
                SpatialInviteCard(
                    state = state,
                    onEvent = session::dispatch,
                    modifier =
                        Modifier.onSizeChanged { cardSize = it }
                            .offset {
                                IntOffset(
                                    (curve.midpoint.x - cardSize.width / 2f)
                                        .coerceIn(
                                            0f,
                                            (root.width - cardSize.width).coerceAtLeast(0f)
                                        )
                                        .roundToInt(),
                                    (curve.midpoint.y + gap)
                                        .coerceIn(
                                            0f,
                                            (root.height - cardSize.height).coerceAtLeast(0f)
                                        )
                                        .roundToInt()
                                )
                            }
                )
            }
        }
    }
}

@Composable
private fun AnchorLabel(text: String, anchor: Offset) {
    val lift = with(LocalDensity.current) { 26.dp.toPx() }
    Text(
        text = text,
        fontSize = 10.sp,
        color = Palette.TextGray,
        modifier =
            Modifier.offset { IntOffset(anchor.x.roundToInt(), (anchor.y - lift).roundToInt()) }
    )
}

@Composable
private fun SpatialBreathingPoint(
    state: SpatialConnectionState,
    onEvent: (SpatialConnectionEvent) -> Unit,
    modifier: Modifier
) {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val dispatch by rememberUpdatedState(onEvent)
    LaunchedEffect(hovered) {
        dispatch(SpatialConnectionEvent.PointHoverChanged(hovered, SystemClock.uptimeMillis()))
    }
    DisposableEffect(Unit) {
        onDispose {
            dispatch(SpatialConnectionEvent.PointHoverChanged(false, SystemClock.uptimeMillis()))
        }
    }
    val transition = rememberInfiniteTransition(label = "2D preview Point breathing")
    val breath by
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
            label = "4-second Point breath"
        )
    Box(
        modifier =
            modifier.size(52.dp).hoverable(source).semantics {
                contentDescription = "2D连接预演呼吸Point，局部连续驻留模拟"
                stateDescription =
                    if (state.cooldownStartedAtMillis != null) "冷却中，需移开后重新驻留"
                    else "驻留 ${state.config.pointDwellMillis} 毫秒"
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.size(24.dp)
                .graphicsLayer {
                    alpha = 0.35f + breath * 0.35f
                    scaleX = 0.9f + breath * 0.2f
                    scaleY = scaleX
                }
                .clip(CircleShape)
                .background(ConnectionColor)
        )
        if (state.stage == SpatialConnectionStage.POINT_DWELL) {
            CircularProgressIndicator(
                progress = { state.dwellProgress },
                modifier = Modifier.size(44.dp)
            )
        }
    }
}

@Composable
private fun SpatialInviteCard(
    state: SpatialConnectionState,
    onEvent: (SpatialConnectionEvent) -> Unit,
    modifier: Modifier
) {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val dispatch by rememberUpdatedState(onEvent)
    LaunchedEffect(hovered) {
        dispatch(SpatialConnectionEvent.ConfirmHoverChanged(hovered, SystemClock.uptimeMillis()))
    }
    DisposableEffect(Unit) {
        onDispose {
            dispatch(SpatialConnectionEvent.ConfirmHoverChanged(false, SystemClock.uptimeMillis()))
        }
    }
    Column(
        modifier =
            modifier
                .width(300.dp)
                .clip(RoundedCornerShape(18.dp))
                .backgroundMaterial(style = Material.Thin)
                .background(Color.White.copy(alpha = 0.22f))
                .border(1.dp, ConnectionColor.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("2D 连接预演 / 研究者模拟", fontSize = 10.sp, color = Palette.TextGray)
        Text("也许可以听听彼此的想法。", fontSize = 16.sp, color = Palette.TextDark)
        Row(
            modifier =
                Modifier.clip(RoundedCornerShape(10.dp))
                    .hoverable(source)
                    .spatialHoverEffect()
                    .background(ConnectionColor.copy(alpha = 0.24f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .semantics {
                        contentDescription = "开始讨论，局部连续驻留 ${state.config.confirmDwellMillis} 毫秒"
                    },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("开始讨论", fontSize = 14.sp, fontWeight = FontWeight.W600, color = Palette.TextDark)
            // Stable entry bounds, independent of progress visibility.
            Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                if (state.stage == SpatialConnectionStage.CONFIRM_DWELL) {
                    CircularProgressIndicator(
                        progress = { state.dwellProgress },
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

/** Clicks here are researcher controls, never A's formal interaction. Collapsed by default. */
@Composable
fun SpatialResearcherPanel(session: SpatialConnectionSession, modifier: Modifier = Modifier) {
    if (!session.available) return
    var expanded by remember { mutableStateOf(false) }
    val state = session.state
    val send: (SpatialConnectionEvent) -> Unit = session::dispatch
    Column(
        modifier =
            modifier
                .width(if (expanded) 430.dp else 260.dp)
                .clip(RoundedCornerShape(14.dp))
                .backgroundMaterial(style = Material.Thick)
                .border(1.dp, ConnectionColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
    ) {
        Text(
            text = "研究者控制 · 模拟 ${if (expanded) "收起" else "展开"}",
            fontSize = 12.sp,
            fontWeight = FontWeight.W600,
            color = Palette.TextDark,
            modifier = Modifier.clickable { expanded = !expanded }.padding(12.dp)
        )
        if (expanded) {
            Column(
                Modifier.heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("2D 连接预演 / 研究者模拟；无真实测距或眼动", fontSize = 11.sp, color = Palette.TextGray)
                Text(
                    "阶段 ${state.stage} · 目标 ${state.activeTarget ?: "无"}",
                    fontSize = 11.sp,
                    color = Palette.TextDark
                )
                Text(
                    "本轮 ${state.roundId} · ${state.scenarioId}",
                    fontSize = 10.sp,
                    color = Palette.TextGray
                )
                SpatialParticipant.entries.forEach { participant ->
                    val input = state.proximity(participant)
                    Text(
                        "$participant 输入${if (input.rawNear) "近" else "远"} / 确认${if (input.confirmedNear) "近" else "远"}" +
                            if (input.changeStartedAtMillis != null) " · 持续确认中" else "",
                        fontSize = 11.sp,
                        color = Palette.TextGray
                    )
                }
                ResearcherButton("完成关联材料查看", state.stage == SpatialConnectionStage.LOCKED) {
                    send(SpatialConnectionEvent.CompleteMaterialView(SystemClock.uptimeMillis()))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SpatialParticipant.entries.forEach { participant ->
                        ResearcherButton("$participant 近", !state.proximity(participant).rawNear) {
                            send(
                                SpatialConnectionEvent.SetNear(
                                    participant,
                                    true,
                                    SystemClock.uptimeMillis()
                                )
                            )
                        }
                        ResearcherButton("$participant 远", state.proximity(participant).rawNear) {
                            send(
                                SpatialConnectionEvent.SetNear(
                                    participant,
                                    false,
                                    SystemClock.uptimeMillis()
                                )
                            )
                        }
                    }
                }
                ResearcherButton(
                    "模拟注视 Point ${state.config.pointDwellMillis / 1000f}秒",
                    state.canSimulatePointDwell
                ) {
                    send(SpatialConnectionEvent.SimulatePointDwell(SystemClock.uptimeMillis()))
                }
                ResearcherButton(
                    "模拟确认 ${state.config.confirmDwellMillis / 1000f}秒",
                    state.inviteVisible
                ) {
                    send(SpatialConnectionEvent.SimulateConfirmDwell(SystemClock.uptimeMillis()))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ResearcherButton("已开始交流", state.connectionVisible && !state.discussionStarted) {
                        send(
                            SpatialConnectionEvent.MarkDiscussionStarted(SystemClock.uptimeMillis())
                        )
                    }
                    ResearcherButton("重置本轮", true) {
                        send(SpatialConnectionEvent.Reset(SystemClock.uptimeMillis()))
                    }
                }
                Text("会话内存日志 · 最近12条（重置不删除历史）", fontSize = 11.sp, color = Palette.TextDark)
                session.records.takeLast(12).forEach { record ->
                    Text(
                        "R${record.roundId} ${record.timestampMillis}ms ${record.type} ${record.participant ?: "-"}\n" +
                            "${record.source} · ${record.reason}",
                        fontSize = 9.sp,
                        lineHeight = 13.sp,
                        color = Palette.TextGray
                    )
                }
            }
        }
    }
}

@Composable
private fun ResearcherButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        label,
        fontSize = 11.sp,
        color = Palette.TextDark,
        modifier =
            Modifier.alpha(if (enabled) 1f else 0.4f)
                .clip(RoundedCornerShape(8.dp))
                .background(ConnectionColor.copy(alpha = 0.2f))
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 8.dp)
    )
}
