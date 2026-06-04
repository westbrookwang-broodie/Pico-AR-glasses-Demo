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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.foundation.hover.spatialHoverEffect
import com.pico.spatial.ui.foundation.material.backgroundMaterial
import com.pico.spatial.ui.platform.Material

/** Square colored tag (A / B / C) used in card kickers. */
@Composable
fun Tag(label: String, color: Color, size: Int = 28) {
    Box(
        modifier = Modifier.size(size.dp).clip(RoundedCornerShape(6.dp)).background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, fontSize = (size * 0.5f).sp, fontWeight = FontWeight.W800, color = Color.White)
    }
}

/** Kicker row: colored tag + category text (e.g. "关键词洞察"). */
@Composable
fun CardKicker(tag: String, color: Color, kicker: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Tag(tag, color)
        Spacer(Modifier.width(10.dp))
        Text(text = kicker, fontSize = 15.sp, color = Palette.TextGray)
    }
}

/** Solid primary (light) pill button — "带入讨论". */
@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF0F0F0))
                .clickable { onClick() }
                .spatialHoverEffect()
                .padding(horizontal = 22.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.W600, color = Palette.TextDark)
    }
}

/** Soft (material/glass) pill button — "添加到对话", "继续" 等。 */
@Composable
fun SoftButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(10.dp))
                .backgroundMaterial(style = Material.Thin)
                .clickable { onClick() }
                .spatialHoverEffect()
                .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.W600, color = Palette.TextDark)
    }
}

/** Circular "✕" close button. */
@Composable
fun CloseButton(onClick: () -> Unit) {
    Box(
        modifier =
            Modifier.size(36.dp)
                .clip(CircleShape)
                .backgroundMaterial(style = Material.Thin)
                .clickable { onClick() }
                .spatialHoverEffect(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "✕", fontSize = 18.sp, fontWeight = FontWeight.W600, color = Palette.TextGray)
    }
}

/** Glass panel container with rounded corners and material background. */
@Composable
fun GlassPanel(modifier: Modifier = Modifier, padding: Int = 24, content: @Composable () -> Unit) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(16.dp))
                .backgroundMaterial(style = Material.Thin)
                .padding(padding.dp)
    ) {
        content()
    }
}

/**
 * Standard detail-panel layout: title bar (kicker tag + title + close) over a content area. Used
 * by the per-card detail screens.
 */
@Composable
fun PanelScaffold(
    card: CardContent,
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.padding(32.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            CardKicker(card.tag, card.tagColor, card.kicker)
            Spacer(Modifier.weight(1f))
            CloseButton(onClose)
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = card.title,
            fontSize = 28.sp,
            fontWeight = FontWeight.W800,
            lineHeight = 36.sp,
            color = Palette.TextDark
        )
        Spacer(Modifier.height(16.dp))
        content()
    }
}

/** Colored sticky note used on the focus board. */
@Composable
fun StickyNote(title: String, color: Color, selected: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier =
            Modifier.size(132.dp, 92.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color)
                .clickable { onClick() }
                .spatialHoverEffect(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = (if (selected) "● " else "") + title,
            fontSize = 18.sp,
            fontWeight = FontWeight.W800,
            color = Palette.TextDark
        )
    }
}

/** Bottom toolbar item (icon glyph + label) with active highlight. */
@Composable
fun ToolButton(glyph: String, label: String, active: Boolean = false, onClick: () -> Unit) {
    Column(
        modifier =
            Modifier.clip(RoundedCornerShape(14.dp))
                .clickable { onClick() }
                .spatialHoverEffect()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(text = glyph, fontSize = 20.sp, color = if (active) Palette.Accent else Palette.TextGray)
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (active) FontWeight.W700 else FontWeight.W400,
            color = if (active) Palette.Accent else Palette.TextGray
        )
    }
}
