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

import androidx.compose.ui.graphics.Color

/** Route names for the "抬头共振" (Heads-up Resonance) spatial scene. */
object Routes {
    const val WORKSPACE = "workspace" // 主工作台：标题 + 三张知识卡 + 关注点画布 + 工具栏
    const val CARD_A = "card_a" // 关键词洞察（含 3D 机器人）
    const val CARD_B = "card_b" // 案例补充
    const val CARD_C = "card_c" // 趋势延展
}

/** Shared palette mirroring the demo HTML look (glass panels, colored tags & notes). */
object Palette {
    val TextDark = Color(0xFF1A1A1A)
    val TextGray = Color(0xFF666666)
    val TextFaint = Color(0xFF999999)

    val TagBlue = Color(0xFF5A7CDC) // A · 关键词洞察
    val TagGreen = Color(0xFF6EA067) // B · 案例补充
    val TagPurple = Color(0xFF8774BE) // C · 趋势延展

    val NoteBlue = Color(0xFFBBD2FF) // 共识点
    val NotePink = Color(0xFFFFC1C8) // 疑问点
    val NoteGreen = Color(0xFFC2EBB8) // 新观点

    val Accent = Color(0xFF5291FF) // 高亮/连接
    val Online = Color(0xFF8FD078) // 在场状态点
}

/** Static content for the three knowledge cards, mirroring the demo HTML copy. */
data class CardContent(
    val id: String,
    val tag: String,
    val tagColor: Color,
    val kicker: String,
    val title: String,
    val relation: String,
    val body: String,
    val expanded: List<String>,
    val actionLabel: String
)

object SceneData {
    val cardA =
        CardContent(
            id = "A",
            tag = "A",
            tagColor = Palette.TagBlue,
            kicker = "关键词洞察",
            title = "具身智能发展综述",
            relation = "A 当前关注：",
            body = "视觉、语言与动作模型结合后，机器从 “执行工具” 转向 “环境理解者”。",
            expanded =
                listOf(
                    "核心判断：具身智能的关键不只是执行动作，而是理解环境反馈。",
                    "讨论焦点：模型如何从感知输入转成可行动的空间决策。"
                ),
            actionLabel = "带入讨论"
        )

    val cardB =
        CardContent(
            id = "B",
            tag = "B",
            tagColor = Palette.TagGreen,
            kicker = "案例补充",
            title = "空间计算的未来",
            relation = "与 A 的关系：",
            body = "空间计算可作为具身智能的应用场景。",
            expanded =
                listOf(
                    "可补充的场景：空间标注、物体定位、多人协作决策。",
                    "案例价值：让模型的空间理解能力进入可验证场景。"
                ),
            actionLabel = "添加到对话"
        )

    val cardC =
        CardContent(
            id = "C",
            tag = "C",
            tagColor = Palette.TagPurple,
            kicker = "趋势延展",
            title = "人与空间的协同",
            relation = "与 A 的关系：",
            body = "人机交互将从屏幕转向空间理解。",
            expanded =
                listOf(
                    "可延展的趋势：空间理解、意图预测、协同反馈。",
                    "趋势价值：从单点指令转向人与环境共同塑造任务。"
                ),
            actionLabel = "添加到对话"
        )

    fun byId(id: String): CardContent =
        when (id) {
            "B" -> cardB
            "C" -> cardC
            else -> cardA
        }
}
