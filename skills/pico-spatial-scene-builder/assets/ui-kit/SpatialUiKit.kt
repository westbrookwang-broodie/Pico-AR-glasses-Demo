/*
 * Spatial UI Kit — shared visual style for PICO Spatial scene panels.
 *
 * USAGE:
 * 1. Copy this file into the app's UI package (e.g. ui/scene/).
 * 2. Change the `package` line below to the app's package.
 * 3. Build every panel from PanelScaffold / SpatialPanelButton / SpatialCloseButton / StickyNote
 *    so all scenes share one look.
 *
 * NOTE: Visual elements use com.pico.spatial.ui.* (the project excludes standard Compose UI).
 * Layout/state come from androidx.compose.foundation.layout / androidx.compose.ui / runtime.
 */
package com.example.app.ui.scene

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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

/* ---- Unified palette ---- */
object SpatialPalette {
    val TextDark = Color(0xFF1A1A1A)
    val TextGray = Color(0xFF666666)
    val NoteBlue = Color(0xFFBBD2FF)
    val NotePink = Color(0xFFFFC1C8)
    val NoteGreen = Color(0xFFC2EBB8)
}

/**
 * Standard panel layout: title bar (title + optional close) over a content area.
 * Wrap every scene's body in this for a consistent header and padding.
 */
@Composable
fun PanelScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxSize().padding(32.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                fontSize = 26.sp,
                fontWeight = FontWeight.W700,
                lineHeight = 34.sp,
                color = SpatialPalette.TextDark,
                modifier = Modifier.weight(1f)
            )
            if (onClose != null) SpatialCloseButton(onClose)
        }
        Spacer(Modifier.height(16.dp))
        content()
    }
}

/** Pill-style primary button with material background and hover feedback. */
@Composable
fun SpatialPanelButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
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
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.W600,
            color = SpatialPalette.TextDark
        )
    }
}

/** Circular "✕" close button. */
@Composable
fun SpatialCloseButton(onClick: () -> Unit) {
    Box(
        modifier =
            Modifier.size(36.dp).clip(CircleShape).clickable { onClick() }.spatialHoverEffect(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "✕",
            fontSize = 20.sp,
            fontWeight = FontWeight.W600,
            color = SpatialPalette.TextGray
        )
    }
}

/** Colored sticky note used on collaboration / hub panels. */
@Composable
fun StickyNote(title: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier =
            Modifier.size(160.dp, 120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color)
                .clickable { onClick() }
                .spatialHoverEffect(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.W700,
            color = SpatialPalette.TextDark
        )
    }
}
