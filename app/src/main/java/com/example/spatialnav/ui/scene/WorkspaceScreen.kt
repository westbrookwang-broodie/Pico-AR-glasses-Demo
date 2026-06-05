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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.foundation.hover.spatialHoverEffect
import com.pico.spatial.ui.foundation.material.backgroundMaterial
import com.pico.spatial.ui.platform.Material

/** A member that can be present in the session (name + role). */
data class Member(val name: String, val role: String)

private val ALL_MEMBERS =
    listOf(
        Member("林", "产品"),
        Member("K", "研究"),
        Member("周", "交互"),
        Member("陈", "观察")
    )

/**
 * Main spatial workspace, mirroring the demo HTML "抬头共振" stage:
 * - top-left title block + step
 * - top-right presence chip (tap to expand/collapse the member list; toggle members in/out)
 * - three knowledge cards (B left · A center/active · C right)
 * - the "关注点画布" focus board with sticky notes & tabs
 * - bottom tool bar + collaboration status
 *
 * Presence state is hoisted here so the header, chip and bottom status all reflect the live count.
 */
@Composable
fun WorkspaceScreen(onOpenCard: (String) -> Unit) {
    // Live presence: who is currently in the session, and whether the member list is expanded.
    val present = remember { mutableStateListOf("林", "K", "周") }
    var presenceExpanded by remember { mutableStateOf(false) }
    val count = present.size

    val togglePerson: (String) -> Unit = { name ->
        if (present.contains(name)) {
            if (present.size > 1) present.remove(name) // keep at least one person in the room
        } else {
            present.add(name)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(28.dp)) {
            HeaderRow(
                count = count,
                expanded = presenceExpanded,
                onToggleExpanded = { presenceExpanded = !presenceExpanded }
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SideCard(SceneData.cardB, Modifier.weight(1f)) { onOpenCard("B") }
                Connector()
                MainCard(SceneData.cardA, Modifier.weight(1.3f)) { onOpenCard("A") }
                Connector()
                SideCard(SceneData.cardC, Modifier.weight(1f)) { onOpenCard("C") }
            }
            Spacer(Modifier.height(20.dp))
            FocusBoard()
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Toolbar()
                Spacer(Modifier.weight(1f))
                StatusChip(count)
            }
        }

        // Presence popover floats over the stage, anchored under the top-right chip.
        if (presenceExpanded) {
            PresencePopover(
                present = present,
                onToggle = togglePerson,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 84.dp, end = 28.dp)
            )
        }
    }
}

/* ----------------------------- 顶部：标题 + 在场 ----------------------------- */

@Composable
private fun HeaderRow(count: Int, expanded: Boolean, onToggleExpanded: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column {
            Text(
                text = "2. 抬头共振",
                fontSize = 26.sp,
                fontWeight = FontWeight.W800,
                color = Palette.TextDark
            )
            Spacer(Modifier.height(4.dp))
            Text(text = "共同关注形成 · $count 人在场", fontSize = 14.sp, color = Palette.TextGray)
        }
        Spacer(Modifier.weight(1f))
        PresenceChip(count = count, expanded = expanded, onClick = onToggleExpanded)
    }
}

@Composable
private fun PresenceChip(count: Int, expanded: Boolean, onClick: () -> Unit) {
    Row(
        modifier =
            Modifier.clip(RoundedCornerShape(22.dp))
                .backgroundMaterial(style = Material.Thin)
                .clickable { onClick() }
                .spatialHoverEffect()
                .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
            listOf(Palette.TagBlue, Palette.TagGreen, Palette.TagPurple).forEach { c ->
                Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(c))
            }
        }
        Text(text = "$count 人在场", fontSize = 13.sp, color = Palette.TextDark)
        Text(text = if (expanded) "▴" else "▾", fontSize = 12.sp, color = Palette.TextGray)
    }
}

/** Expanded member list. Each row toggles a person in/out of the session. */
@Composable
private fun PresencePopover(
    present: List<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier =
            modifier
                .width(200.dp)
                .clip(RoundedCornerShape(14.dp))
                .backgroundMaterial(style = Material.Thick)
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text = "在场成员", fontSize = 13.sp, fontWeight = FontWeight.W700, color = Palette.TextGray)
        ALL_MEMBERS.forEach { member ->
            val on = present.contains(member.name)
            PersonRow(member = member, on = on) { onToggle(member.name) }
        }
    }
}

@Composable
private fun PersonRow(member: Member, on: Boolean, onClick: () -> Unit) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .then(if (on) Modifier.background(Palette.NoteGreen) else Modifier)
                .clickable { onClick() }
                .spatialHoverEffect()
                .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier =
                Modifier.size(8.dp)
                    .clip(CircleShape)
                    .background(if (on) Palette.Online else Palette.TextFaint)
        )
        Text(
            text = "${member.name} · ${member.role}",
            fontSize = 13.sp,
            fontWeight = if (on) FontWeight.W600 else FontWeight.W400,
            color = if (on) Palette.TextDark else Palette.TextGray,
            modifier = Modifier.weight(1f)
        )
        Text(text = if (on) "在场" else "暂离", fontSize = 11.sp, color = Palette.TextFaint)
    }
}

/* ----------------------------- 知识卡 ----------------------------- */

@Composable
private fun MainCard(card: CardContent, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(16.dp))
                .backgroundMaterial(style = Material.Thick)
                .clickable { onClick() }
                .spatialHoverEffect()
                .padding(24.dp)
    ) {
        CardKicker(card.tag, card.tagColor, card.kicker)
        Spacer(Modifier.height(14.dp))
        Text(
            text = card.title,
            fontSize = 24.sp,
            fontWeight = FontWeight.W800,
            lineHeight = 30.sp,
            color = Palette.TextDark
        )
        Spacer(Modifier.height(12.dp))
        Text(text = card.relation, fontSize = 13.sp, color = Palette.TextGray)
        Spacer(Modifier.height(4.dp))
        Text(text = card.body, fontSize = 15.sp, lineHeight = 24.sp, color = Palette.TextDark)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PrimaryButton(text = "▶ ${card.actionLabel}", onClick = onClick)
            Spacer(Modifier.width(12.dp))
            Text(text = "等待共同关注", fontSize = 12.sp, color = Palette.TextFaint)
        }
    }
}

@Composable
private fun SideCard(card: CardContent, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier =
            modifier
                .alpha(0.92f)
                .clip(RoundedCornerShape(14.dp))
                .backgroundMaterial(style = Material.Thin)
                .clickable { onClick() }
                .spatialHoverEffect()
                .padding(18.dp)
    ) {
        CardKicker(card.tag, card.tagColor, card.kicker)
        Spacer(Modifier.height(12.dp))
        Text(
            text = card.title,
            fontSize = 18.sp,
            fontWeight = FontWeight.W800,
            lineHeight = 24.sp,
            color = Palette.TextDark
        )
        Spacer(Modifier.height(10.dp))
        Text(text = card.relation, fontSize = 12.sp, color = Palette.TextGray)
        Spacer(Modifier.height(4.dp))
        Text(text = card.body, fontSize = 13.sp, lineHeight = 20.sp, color = Palette.TextDark)
        Spacer(Modifier.height(14.dp))
        SoftButton(text = "✎ ${card.actionLabel}", onClick = onClick)
    }
}

/** Small glowing connector dot between cards (stand-in for the SVG link lines). */
@Composable
private fun Connector() {
    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Palette.Accent))
}

/* ----------------------------- 关注点画布 ----------------------------- */

@Composable
private fun FocusBoard() {
    var selected by remember { mutableStateOf("question") }
    var tab by remember { mutableStateOf("map") }
    GlassPanel(modifier = Modifier.fillMaxWidth(), padding = 18) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BoardTab("⊹ 关注点画布", tab == "map") { tab = "map" }
                Spacer(Modifier.width(20.dp))
                BoardTab("▤ 白板笔记点", tab == "whiteboard") { tab = "whiteboard" }
                Spacer(Modifier.width(20.dp))
                BoardTab("▥ 笔记和概要", tab == "summary") { tab = "summary" }
                Spacer(Modifier.weight(1f))
                Text(text = "12:06 · K", fontSize = 12.sp, color = Palette.TextFaint)
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StickyNote("共识点", Palette.NoteBlue, selected == "common") { selected = "common" }
                StickyNote("疑问点", Palette.NotePink, selected == "question") { selected = "question" }
                StickyNote("新观点", Palette.NoteGreen, selected == "new") { selected = "new" }
            }
        }
    }
}

@Composable
private fun BoardTab(label: String, current: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        fontSize = 13.sp,
        fontWeight = if (current) FontWeight.W700 else FontWeight.W400,
        color = if (current) Palette.TextDark else Palette.TextFaint,
        modifier = Modifier.clickable { onClick() }.spatialHoverEffect().padding(4.dp)
    )
}

/* ----------------------------- 底部：工具栏 + 状态 ----------------------------- */

@Composable
private fun Toolbar() {
    var mode by remember { mutableStateOf("connect") }
    Row(
        modifier =
            Modifier.clip(RoundedCornerShape(26.dp))
                .backgroundMaterial(style = Material.Thin)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolButton("⤓", "选择", mode == "select") { mode = "select" }
        ToolButton("✎", "标注", mode == "annotate") { mode = "annotate" }
        ToolButton("⛓", "连接", mode == "connect") { mode = "connect" }
        ToolButton("🎙", "语音", mode == "voice") { mode = "voice" }
        ToolButton("⋯", "更多", mode == "more") { mode = "more" }
    }
}

@Composable
private fun StatusChip(count: Int) {
    Row(
        modifier =
            Modifier.clip(RoundedCornerShape(20.dp))
                .backgroundMaterial(style = Material.Thin)
                .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Palette.Online))
        Text(text = "多人协作中 · $count 人在场", fontSize = 13.sp, color = Palette.TextDark)
    }
}
