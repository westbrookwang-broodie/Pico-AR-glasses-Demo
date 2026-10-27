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

/** Paper material IDs, unrelated to workspace navigation cards or member names. */
enum class KnowledgeMaterialId {
    I,
    II,
    III,
    IV,
    V,
    VI,
    UNKNOWN;

    companion object {
        /** Accepts both ASCII and printed Unicode Roman numerals from an upstream OCR module. */
        fun fromRecognition(value: String?): KnowledgeMaterialId =
            when (value?.trim()?.uppercase()) {
                "I",
                "Ⅰ" -> I
                "II",
                "Ⅱ" -> II
                "III",
                "Ⅲ" -> III
                "IV",
                "Ⅳ" -> IV
                "V",
                "Ⅴ" -> V
                "VI",
                "Ⅵ" -> VI
                else -> UNKNOWN
            }
    }
}

data class KnowledgeAssociation(
    val relatedMaterial: KnowledgeMaterialId,
    val title: String,
    val keywords: String,
    val points: List<String>
)

/** Fixed content only; all four mappings share the same interaction and exit rules. */
object KnowledgeAssociations {
    private val mappings =
        mapOf(
            KnowledgeMaterialId.I to
                KnowledgeAssociation(
                    KnowledgeMaterialId.VI,
                    "Moxie",
                    "Moxie · G.R.L.",
                    listOf("对话语音自然、有情感", "陪 6–9 岁孩子练社交", "脸上表情少而清楚")
                ),
            KnowledgeMaterialId.VI to
                KnowledgeAssociation(
                    KnowledgeMaterialId.I,
                    "Hanulíková",
                    "prosody · variability",
                    listOf("语音要有变化，不能每句一样", "夸张的韵律更抓儿童注意", "语速和音高影响理解")
                ),
            KnowledgeMaterialId.II to
                KnowledgeAssociation(
                    KnowledgeMaterialId.III,
                    "Breazeal",
                    "Kismet",
                    listOf("情绪有基本类别", "表情由可组合的单元构成", "机器人表情要能被快速读懂")
                ),
            KnowledgeMaterialId.III to
                KnowledgeAssociation(
                    KnowledgeMaterialId.II,
                    "Cozmo",
                    "Pixar · Cozmo",
                    listOf("只用眼睛表达情绪，不画眉毛嘴", "动画师方法：像做动画一样设计表情", "40 个原型才定下外形")
                )
        )

    fun forMaterial(material: KnowledgeMaterialId): KnowledgeAssociation? = mappings[material]
}

/** Availability of the upstream OCR/spatial signals; missing producers must not show the feature. */
sealed interface KnowledgeBlackHoleSourceState {
    object Unavailable : KnowledgeBlackHoleSourceState

    data class Ready(val input: KnowledgeBlackHoleInput) : KnowledgeBlackHoleSourceState
}

/**
 * Input contract for the current viewer's overlay. OCR and upstream spatial-stage producers must
 * explicitly supply these values. K is a knowledge pairing signal, NOT the presence of the member
 * named K. P is the next spatial stage, NOT a locally inferred distance threshold. The upstream
 * normalized distance brightness multiplier is clamped to [0, 1]; NaN is treated as zero. No raw
 * distance is interpreted here.
 */
data class KnowledgeBlackHoleInput(
    val currentMaterial: KnowledgeMaterialId,
    val kPairValid: Boolean,
    val pUpgraded: Boolean,
    val distanceBrightnessMultiplier: Float
) {
    val normalizedBrightness: Float
        get() =
            if (distanceBrightnessMultiplier.isNaN()) 0f
            else distanceBrightnessMultiplier.coerceIn(0f, 1f)

    val isEligible: Boolean
        get() =
            kPairValid && !pUpgraded && KnowledgeAssociations.forMaterial(currentMaterial) != null
}

enum class KnowledgeBlackHoleStage {
    HIDDEN,
    POINT,
    PROMPT,
    DWELL,
    KNOWLEDGE
}

data class KnowledgeBlackHoleState(
    val input: KnowledgeBlackHoleInput,
    val stage: KnowledgeBlackHoleStage,
    val promptRegionEntered: Boolean = false,
    val entryHovered: Boolean = false,
    val dwellStartedAtMillis: Long? = null,
    val dwellElapsedMillis: Long = 0L,
    val knowledgeRegionEntered: Boolean = false,
    val knowledgeHovered: Boolean = false,
    val knowledgeExitStartedAtMillis: Long? = null,
    val lastNowMillis: Long = 0L
) {
    val dwellProgress: Float
        get() =
            (dwellElapsedMillis.toFloat() / KnowledgeBlackHoleStateMachine.DWELL_MILLIS).coerceIn(
                0f,
                1f
            )
}

/**
 * Hover and tick times use SystemClock.uptimeMillis; the reducer has no clock or UI effects.
 * Input changes are untimed and must take effect immediately, without resetting the time watermark.
 */
sealed interface KnowledgeBlackHoleEvent {
    data class InputChanged(val input: KnowledgeBlackHoleInput) : KnowledgeBlackHoleEvent

    /** The union of Point, the connecting space, the prompt and its entry. */
    data class PromptRegionHoverChanged(val hovered: Boolean, val nowMillis: Long) :
        KnowledgeBlackHoleEvent

    data class EntryHoverChanged(val hovered: Boolean, val nowMillis: Long) :
        KnowledgeBlackHoleEvent

    data class KnowledgeHoverChanged(val hovered: Boolean, val nowMillis: Long) :
        KnowledgeBlackHoleEvent

    data class Tick(val nowMillis: Long) : KnowledgeBlackHoleEvent
}

object KnowledgeBlackHoleStateMachine {
    const val DWELL_MILLIS = 1500L
    const val KNOWLEDGE_EXIT_MILLIS = 1500L

    fun initial(input: KnowledgeBlackHoleInput): KnowledgeBlackHoleState =
        KnowledgeBlackHoleState(
            input = input,
            stage =
                if (input.isEligible) KnowledgeBlackHoleStage.POINT
                else KnowledgeBlackHoleStage.HIDDEN
        )

    fun reduce(
        state: KnowledgeBlackHoleState,
        event: KnowledgeBlackHoleEvent
    ): KnowledgeBlackHoleState {
        if (event is KnowledgeBlackHoleEvent.InputChanged) {
            val input = event.input
            return if (
                input.currentMaterial != state.input.currentMaterial ||
                    input.kPairValid != state.input.kPairValid ||
                    input.pUpgraded != state.input.pUpgraded
            ) {
                initial(input).copy(lastNowMillis = state.lastNowMillis)
            } else {
                // Brightness updates must not interrupt an otherwise valid interaction.
                state.copy(input = input)
            }
        }
        val nowMillis =
            when (event) {
                is KnowledgeBlackHoleEvent.PromptRegionHoverChanged -> event.nowMillis
                is KnowledgeBlackHoleEvent.EntryHoverChanged -> event.nowMillis
                is KnowledgeBlackHoleEvent.KnowledgeHoverChanged -> event.nowMillis
                is KnowledgeBlackHoleEvent.Tick -> event.nowMillis
                is KnowledgeBlackHoleEvent.InputChanged -> return state
            }
        // All timed events share one watermark, including events ignored by the current stage.
        // Equal times retain call order; stale events must not change hover flags or timers.
        if (nowMillis < state.lastNowMillis) return state

        val nextState =
            if (!state.input.isEligible) initial(state.input)
            else
                when (event) {
                    is KnowledgeBlackHoleEvent.PromptRegionHoverChanged ->
                        promptHover(state, event.hovered)
                    is KnowledgeBlackHoleEvent.EntryHoverChanged -> entryHover(state, event)
                    is KnowledgeBlackHoleEvent.KnowledgeHoverChanged -> knowledgeHover(state, event)
                    is KnowledgeBlackHoleEvent.Tick -> tick(state, nowMillis)
                    is KnowledgeBlackHoleEvent.InputChanged -> state
                }
        // Stage completion and resets clear interaction data, never the processed time watermark.
        return nextState.copy(lastNowMillis = nowMillis)
    }

    private fun promptHover(
        state: KnowledgeBlackHoleState,
        hovered: Boolean
    ): KnowledgeBlackHoleState =
        when (state.stage) {
            KnowledgeBlackHoleStage.POINT ->
                if (hovered) {
                    state.copy(stage = KnowledgeBlackHoleStage.PROMPT, promptRegionEntered = true)
                } else state
            KnowledgeBlackHoleStage.PROMPT,
            KnowledgeBlackHoleStage.DWELL ->
                if (hovered) state.copy(promptRegionEntered = true)
                else if (state.promptRegionEntered) initial(state.input) else state
            else -> state
        }

    private fun entryHover(
        state: KnowledgeBlackHoleState,
        event: KnowledgeBlackHoleEvent.EntryHoverChanged
    ): KnowledgeBlackHoleState {
        if (
            state.stage != KnowledgeBlackHoleStage.PROMPT &&
                state.stage != KnowledgeBlackHoleStage.DWELL
        ) {
            return state
        }
        return if (event.hovered) {
            if (state.entryHovered) state
            else
                state.copy(
                    stage = KnowledgeBlackHoleStage.DWELL,
                    promptRegionEntered = true,
                    entryHovered = true,
                    dwellStartedAtMillis = event.nowMillis,
                    dwellElapsedMillis = 0L
                )
        } else {
            state.copy(
                stage = KnowledgeBlackHoleStage.PROMPT,
                entryHovered = false,
                dwellStartedAtMillis = null,
                dwellElapsedMillis = 0L
            )
        }
    }

    private fun knowledgeHover(
        state: KnowledgeBlackHoleState,
        event: KnowledgeBlackHoleEvent.KnowledgeHoverChanged
    ): KnowledgeBlackHoleState {
        if (state.stage != KnowledgeBlackHoleStage.KNOWLEDGE) return state
        return if (event.hovered) {
            state.copy(
                knowledgeRegionEntered = true,
                knowledgeHovered = true,
                knowledgeExitStartedAtMillis = null
            )
        } else if (state.knowledgeRegionEntered && state.knowledgeHovered) {
            state.copy(knowledgeHovered = false, knowledgeExitStartedAtMillis = event.nowMillis)
        } else state
    }

    private fun tick(state: KnowledgeBlackHoleState, nowMillis: Long): KnowledgeBlackHoleState {
        val dwellStart = state.dwellStartedAtMillis
        val exitStart = state.knowledgeExitStartedAtMillis
        return when {
            state.stage == KnowledgeBlackHoleStage.DWELL &&
                state.entryHovered &&
                dwellStart != null -> {
                val elapsed = (nowMillis - dwellStart).coerceIn(0L, DWELL_MILLIS)
                if (elapsed >= DWELL_MILLIS) {
                    KnowledgeBlackHoleState(
                        input = state.input,
                        stage = KnowledgeBlackHoleStage.KNOWLEDGE
                    )
                } else state.copy(dwellElapsedMillis = elapsed)
            }
            state.stage == KnowledgeBlackHoleStage.KNOWLEDGE &&
                state.knowledgeRegionEntered &&
                !state.knowledgeHovered &&
                exitStart != null &&
                nowMillis - exitStart >= KNOWLEDGE_EXIT_MILLIS -> initial(state.input)
            else -> state
        }
    }
}
