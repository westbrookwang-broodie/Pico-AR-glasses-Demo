package com.example.spatialnav.ui.scene

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KnowledgeBlackHoleStateMachineTest {
    private val input =
        KnowledgeBlackHoleInput(
            currentMaterial = KnowledgeMaterialId.I,
            kPairValid = true,
            pUpgraded = false,
            distanceBrightnessMultiplier = 1f
        )

    @Test
    fun sourceStateReadyPreservesExactInput() {
        val expectedInput =
            KnowledgeBlackHoleInput(
                currentMaterial = KnowledgeMaterialId.VI,
                kPairValid = false,
                pUpgraded = true,
                distanceBrightnessMultiplier = 0.37f
            )
        val sourceState: KnowledgeBlackHoleSourceState =
            KnowledgeBlackHoleSourceState.Ready(expectedInput)

        assertEquals(KnowledgeBlackHoleSourceState.Ready(expectedInput), sourceState)
        assertEquals(expectedInput, readyInputOrNull(sourceState))
    }

    @Test
    fun sourceStateUnavailableCarriesNoInputAndCannotBecomeEligible() {
        val inputOrNull = readyInputOrNull(KnowledgeBlackHoleSourceState.Unavailable)

        assertNull(inputOrNull)
        assertFalse(inputOrNull?.isEligible == true)
    }

    @Test
    fun fixedMappingsIncludeBothDirectionsAndExactContent() {
        assertAssociation(
            KnowledgeMaterialId.I,
            KnowledgeMaterialId.VI,
            "Moxie",
            "Moxie · G.R.L.",
            listOf("对话语音自然、有情感", "陪 6–9 岁孩子练社交", "脸上表情少而清楚")
        )
        assertAssociation(
            KnowledgeMaterialId.VI,
            KnowledgeMaterialId.I,
            "Hanulíková",
            "prosody · variability",
            listOf("语音要有变化，不能每句一样", "夸张的韵律更抓儿童注意", "语速和音高影响理解")
        )
        assertAssociation(
            KnowledgeMaterialId.II,
            KnowledgeMaterialId.III,
            "Breazeal",
            "Kismet",
            listOf("情绪有基本类别", "表情由可组合的单元构成", "机器人表情要能被快速读懂")
        )
        assertAssociation(
            KnowledgeMaterialId.III,
            KnowledgeMaterialId.II,
            "Cozmo",
            "Pixar · Cozmo",
            listOf("只用眼睛表达情绪，不画眉毛嘴", "动画师方法：像做动画一样设计表情", "40 个原型才定下外形")
        )
    }

    @Test
    fun unpairedOrUnknownMaterialsNeverShowUi() {
        listOf(KnowledgeMaterialId.IV, KnowledgeMaterialId.V, KnowledgeMaterialId.UNKNOWN)
            .forEach { id ->
                assertNull(KnowledgeAssociations.forMaterial(id))
                var state = KnowledgeBlackHoleStateMachine.initial(input.copy(currentMaterial = id))
                assertEquals(KnowledgeBlackHoleStage.HIDDEN, state.stage)
                state = reduce(state, KnowledgeBlackHoleEvent.PromptRegionHoverChanged(true))
                state = reduce(state, KnowledgeBlackHoleEvent.EntryHoverChanged(true, 0L))
                state = reduce(state, KnowledgeBlackHoleEvent.Tick(5000L))
                assertEquals(KnowledgeBlackHoleStage.HIDDEN, state.stage)
                assertCleared(state)
            }
    }

    @Test
    fun recognitionAcceptsPrintedAndAsciiRomanNumeralsWithoutGuessingUnknownIds() {
        val ascii = listOf("I", "II", "III", "IV", "V", "VI")
        val printed = listOf("Ⅰ", "Ⅱ", "Ⅲ", "Ⅳ", "Ⅴ", "Ⅵ")
        ascii.zip(printed).forEach { (a, p) ->
            assertEquals(KnowledgeMaterialId.valueOf(a), KnowledgeMaterialId.fromRecognition(a))
            assertEquals(KnowledgeMaterialId.valueOf(a), KnowledgeMaterialId.fromRecognition(p))
        }
        assertEquals(KnowledgeMaterialId.VI, KnowledgeMaterialId.fromRecognition(" vi "))
        listOf(null, "", "VII", "Card A", "K").forEach { value ->
            assertEquals(KnowledgeMaterialId.UNKNOWN, KnowledgeMaterialId.fromRecognition(value))
        }
    }

    @Test
    fun pointPromptDwellKnowledgeUsesContinuous1500MillisForEveryMapping() {
        listOf(
                KnowledgeMaterialId.I,
                KnowledgeMaterialId.VI,
                KnowledgeMaterialId.II,
                KnowledgeMaterialId.III
            )
            .forEach { id ->
                var state = KnowledgeBlackHoleStateMachine.initial(input.copy(currentMaterial = id))
                assertEquals(KnowledgeBlackHoleStage.POINT, state.stage)
                state = reduce(state, KnowledgeBlackHoleEvent.PromptRegionHoverChanged(true))
                assertEquals(KnowledgeBlackHoleStage.PROMPT, state.stage)
                state = reduce(state, KnowledgeBlackHoleEvent.EntryHoverChanged(true, 100L))
                assertEquals(KnowledgeBlackHoleStage.DWELL, state.stage)
                assertEquals(0f, state.dwellProgress, 0f)
                state = reduce(state, KnowledgeBlackHoleEvent.Tick(850L))
                assertEquals(0.5f, state.dwellProgress, 0f)
                state = reduce(state, KnowledgeBlackHoleEvent.Tick(1599L))
                assertEquals(KnowledgeBlackHoleStage.DWELL, state.stage)
                state = reduce(state, KnowledgeBlackHoleEvent.Tick(1600L))
                assertEquals(KnowledgeBlackHoleStage.KNOWLEDGE, state.stage)
                assertEquals(id, state.input.currentMaterial)
                assertCleared(state)
            }
    }

    @Test
    fun entryInterruptionClearsProgressAndNeverAccumulatesEarlierDwell() {
        var state = dwelling()
        state = reduce(state, KnowledgeBlackHoleEvent.Tick(1000L))
        assertEquals(1000L, state.dwellElapsedMillis)
        state = reduce(state, KnowledgeBlackHoleEvent.EntryHoverChanged(false, 1000L))
        assertEquals(KnowledgeBlackHoleStage.PROMPT, state.stage)
        assertEquals(0f, state.dwellProgress, 0f)
        assertNull(state.dwellStartedAtMillis)
        state = reduce(state, KnowledgeBlackHoleEvent.Tick(3000L))
        assertEquals(KnowledgeBlackHoleStage.PROMPT, state.stage)
        state = reduce(state, KnowledgeBlackHoleEvent.EntryHoverChanged(true, 3000L))
        state = reduce(state, KnowledgeBlackHoleEvent.Tick(4499L))
        assertEquals(KnowledgeBlackHoleStage.DWELL, state.stage)
        state = reduce(state, KnowledgeBlackHoleEvent.Tick(4500L))
        assertEquals(KnowledgeBlackHoleStage.KNOWLEDGE, state.stage)
    }

    @Test
    fun repeatedEntryHoverDoesNotRestartDwellAndEarlyClockCannotMakeNegativeProgress() {
        var state = reduce(prompt(), KnowledgeBlackHoleEvent.EntryHoverChanged(true, 100L))
        state = reduce(state, KnowledgeBlackHoleEvent.Tick(50L))
        assertEquals(0L, state.dwellElapsedMillis)
        state = reduce(state, KnowledgeBlackHoleEvent.EntryHoverChanged(true, 900L))
        assertEquals(100L, state.dwellStartedAtMillis)
        state = reduce(state, KnowledgeBlackHoleEvent.Tick(1600L))
        assertEquals(KnowledgeBlackHoleStage.KNOWLEDGE, state.stage)
    }

    @Test
    fun promptUnionKeepsPromptWhileMovingFromPointToEntryButClosesOnRealExit() {
        var state = prompt()
        state = reduce(state, KnowledgeBlackHoleEvent.PromptRegionHoverChanged(true))
        state = reduce(state, KnowledgeBlackHoleEvent.EntryHoverChanged(false, 0L))
        assertEquals(KnowledgeBlackHoleStage.PROMPT, state.stage)
        state = reduce(state, KnowledgeBlackHoleEvent.EntryHoverChanged(true, 100L))
        state = reduce(state, KnowledgeBlackHoleEvent.Tick(500L))
        state = reduce(state, KnowledgeBlackHoleEvent.PromptRegionHoverChanged(false))
        assertEquals(KnowledgeBlackHoleStage.POINT, state.stage)
        assertCleared(state)
        state = reduce(state, KnowledgeBlackHoleEvent.Tick(3000L))
        assertEquals(KnowledgeBlackHoleStage.POINT, state.stage)
    }

    @Test
    fun outOfStageHoverEventsCannotSkipOrCollapseRequiredStages() {
        val point = KnowledgeBlackHoleStateMachine.initial(input)
        var state = reduce(point, KnowledgeBlackHoleEvent.EntryHoverChanged(true, 0L))
        state = reduce(state, KnowledgeBlackHoleEvent.KnowledgeHoverChanged(true, 0L))
        state = reduce(state, KnowledgeBlackHoleEvent.Tick(10_000L))
        assertEquals(point, state)

        val prompt = reduce(point, KnowledgeBlackHoleEvent.PromptRegionHoverChanged(true))
        state = reduce(prompt, KnowledgeBlackHoleEvent.KnowledgeHoverChanged(true, 100L))
        state = reduce(state, KnowledgeBlackHoleEvent.Tick(10_000L))
        assertEquals(prompt, state)

        val knowledge = knowledge()
        state = reduce(knowledge, KnowledgeBlackHoleEvent.PromptRegionHoverChanged(false))
        state = reduce(state, KnowledgeBlackHoleEvent.EntryHoverChanged(false, 2_000L))
        assertEquals(knowledge, state)
    }

    @Test
    fun initialFalseHoverDoesNotCollapseNewPromptOrKnowledgeCard() {
        var state = KnowledgeBlackHoleStateMachine.initial(input)
        state = reduce(state, KnowledgeBlackHoleEvent.PromptRegionHoverChanged(false))
        assertEquals(KnowledgeBlackHoleStage.POINT, state.stage)
        // A freshly mounted region has not observed an enter yet.
        state = state.copy(stage = KnowledgeBlackHoleStage.PROMPT)
        state = reduce(state, KnowledgeBlackHoleEvent.PromptRegionHoverChanged(false))
        assertEquals(KnowledgeBlackHoleStage.PROMPT, state.stage)
        state = knowledge()
        state = reduce(state, KnowledgeBlackHoleEvent.KnowledgeHoverChanged(false, 1500L))
        state = reduce(state, KnowledgeBlackHoleEvent.Tick(10000L))
        assertEquals(KnowledgeBlackHoleStage.KNOWLEDGE, state.stage)
        assertNull(state.knowledgeExitStartedAtMillis)
    }

    @Test
    fun knowledgeExitIsArmedByActualEnterAndReturnsToPointAfter1500Millis() {
        var state = knowledge()
        state = reduce(state, KnowledgeBlackHoleEvent.KnowledgeHoverChanged(true, 2000L))
        assertTrue(state.knowledgeRegionEntered)
        state = reduce(state, KnowledgeBlackHoleEvent.KnowledgeHoverChanged(false, 2500L))
        state = reduce(state, KnowledgeBlackHoleEvent.Tick(3999L))
        assertEquals(KnowledgeBlackHoleStage.KNOWLEDGE, state.stage)
        state = reduce(state, KnowledgeBlackHoleEvent.Tick(4000L))
        assertEquals(KnowledgeBlackHoleStage.POINT, state.stage)
        assertCleared(state)
    }

    @Test
    fun knowledgeReentryCancelsExitAndRepeatedFalseDoesNotRestartExit() {
        var state = knowledge()
        state = reduce(state, KnowledgeBlackHoleEvent.KnowledgeHoverChanged(true, 2000L))
        state = reduce(state, KnowledgeBlackHoleEvent.KnowledgeHoverChanged(false, 2500L))
        state = reduce(state, KnowledgeBlackHoleEvent.KnowledgeHoverChanged(true, 3900L))
        assertNull(state.knowledgeExitStartedAtMillis)
        state = reduce(state, KnowledgeBlackHoleEvent.Tick(5000L))
        assertEquals(KnowledgeBlackHoleStage.KNOWLEDGE, state.stage)
        state = reduce(state, KnowledgeBlackHoleEvent.KnowledgeHoverChanged(false, 6000L))
        state = reduce(state, KnowledgeBlackHoleEvent.KnowledgeHoverChanged(false, 7000L))
        assertEquals(6000L, state.knowledgeExitStartedAtMillis)
        state = reduce(state, KnowledgeBlackHoleEvent.Tick(7500L))
        assertEquals(KnowledgeBlackHoleStage.POINT, state.stage)
    }

    @Test
    fun invalidKOrUpgradedPClearsEveryVisibleStageImmediately() {
        val states =
            listOf(KnowledgeBlackHoleStateMachine.initial(input), prompt(), dwelling(), knowledge())
        listOf(input.copy(kPairValid = false), input.copy(pUpgraded = true)).forEach { invalid ->
            states.forEach { old ->
                var state = reduce(old, KnowledgeBlackHoleEvent.InputChanged(invalid))
                assertEquals(KnowledgeBlackHoleStage.HIDDEN, state.stage)
                assertCleared(state)
                state = reduce(state, KnowledgeBlackHoleEvent.Tick(10000L))
                assertEquals(KnowledgeBlackHoleStage.HIDDEN, state.stage)
                state = reduce(state, KnowledgeBlackHoleEvent.InputChanged(input))
                assertEquals(KnowledgeBlackHoleStage.POINT, state.stage)
                assertCleared(state)
            }
        }
    }

    @Test
    fun switchingToUnpairedMaterialClearsEveryVisibleStageImmediately() {
        val states = listOf(prompt(), dwelling(), knowledge())
        listOf(KnowledgeMaterialId.IV, KnowledgeMaterialId.V, KnowledgeMaterialId.UNKNOWN)
            .forEach { material ->
                states.forEach { old ->
                    var state =
                        reduce(
                            old,
                            KnowledgeBlackHoleEvent.InputChanged(
                                input.copy(currentMaterial = material)
                            )
                        )
                    assertEquals(KnowledgeBlackHoleStage.HIDDEN, state.stage)
                    assertEquals(material, state.input.currentMaterial)
                    assertCleared(state)
                    state = reduce(state, KnowledgeBlackHoleEvent.Tick(10_000L))
                    assertEquals(KnowledgeBlackHoleStage.HIDDEN, state.stage)
                }
            }
    }

    @Test
    fun materialSwitchClearsPromptDwellAndKnowledgeBeforeUsingNewMapping() {
        listOf(prompt(), dwelling(), knowledge()).forEach { old ->
            val changed = input.copy(currentMaterial = KnowledgeMaterialId.III)
            var state = reduce(old, KnowledgeBlackHoleEvent.InputChanged(changed))
            assertEquals(KnowledgeBlackHoleStage.POINT, state.stage)
            assertEquals(changed, state.input)
            assertCleared(state)
            assertEquals(
                "Cozmo",
                KnowledgeAssociations.forMaterial(state.input.currentMaterial)?.title
            )
            state = reduce(state, KnowledgeBlackHoleEvent.Tick(10000L))
            assertEquals(KnowledgeBlackHoleStage.POINT, state.stage)
            state =
                reduce(
                    state,
                    KnowledgeBlackHoleEvent.InputChanged(
                        input.copy(currentMaterial = KnowledgeMaterialId.IV)
                    )
                )
            assertEquals(KnowledgeBlackHoleStage.HIDDEN, state.stage)
            assertCleared(state)
        }
    }

    @Test
    fun brightnessIsClampedWithoutInventingDistanceThresholds() {
        assertEquals(1f, input.normalizedBrightness, 0f)
        assertEquals(0f, input.copy(distanceBrightnessMultiplier = -1f).normalizedBrightness, 0f)
        assertEquals(0.4f, input.copy(distanceBrightnessMultiplier = 0.4f).normalizedBrightness, 0f)
        assertEquals(1f, input.copy(distanceBrightnessMultiplier = 2f).normalizedBrightness, 0f)
        assertEquals(
            0f,
            input.copy(distanceBrightnessMultiplier = Float.NaN).normalizedBrightness,
            0f
        )
        assertEquals(
            1f,
            input.copy(distanceBrightnessMultiplier = Float.POSITIVE_INFINITY).normalizedBrightness,
            0f
        )
        assertEquals(
            0f,
            input.copy(distanceBrightnessMultiplier = Float.NEGATIVE_INFINITY).normalizedBrightness,
            0f
        )
        assertTrue(input.copy(distanceBrightnessMultiplier = 0f).isEligible)
    }

    @Test
    fun brightnessUpdatesPreserveDwellAndKnowledgeExitTimers() {
        val changed = input.copy(distanceBrightnessMultiplier = 0.4f)
        var state = reduce(dwelling(), KnowledgeBlackHoleEvent.Tick(750L))
        state = reduce(state, KnowledgeBlackHoleEvent.InputChanged(changed))
        assertEquals(KnowledgeBlackHoleStage.DWELL, state.stage)
        assertEquals(0.5f, state.dwellProgress, 0f)
        assertEquals(0L, state.dwellStartedAtMillis)
        state = reduce(state, KnowledgeBlackHoleEvent.Tick(1500L))
        assertEquals(KnowledgeBlackHoleStage.KNOWLEDGE, state.stage)
        state = reduce(state, KnowledgeBlackHoleEvent.KnowledgeHoverChanged(true, 2000L))
        state = reduce(state, KnowledgeBlackHoleEvent.KnowledgeHoverChanged(false, 2500L))
        state = reduce(state, KnowledgeBlackHoleEvent.InputChanged(input))
        assertEquals(2500L, state.knowledgeExitStartedAtMillis)
        state = reduce(state, KnowledgeBlackHoleEvent.Tick(4000L))
        assertEquals(KnowledgeBlackHoleStage.POINT, state.stage)
    }

    private fun readyInputOrNull(
        sourceState: KnowledgeBlackHoleSourceState
    ): KnowledgeBlackHoleInput? =
        when (sourceState) {
            KnowledgeBlackHoleSourceState.Unavailable -> null
            is KnowledgeBlackHoleSourceState.Ready -> sourceState.input
        }

    private fun prompt(): KnowledgeBlackHoleState =
        reduce(
            KnowledgeBlackHoleStateMachine.initial(input),
            KnowledgeBlackHoleEvent.PromptRegionHoverChanged(true)
        )

    private fun dwelling(): KnowledgeBlackHoleState =
        reduce(prompt(), KnowledgeBlackHoleEvent.EntryHoverChanged(true, 0L))

    private fun knowledge(): KnowledgeBlackHoleState =
        reduce(dwelling(), KnowledgeBlackHoleEvent.Tick(1500L))

    private fun reduce(
        state: KnowledgeBlackHoleState,
        event: KnowledgeBlackHoleEvent
    ): KnowledgeBlackHoleState = KnowledgeBlackHoleStateMachine.reduce(state, event)

    private fun assertCleared(state: KnowledgeBlackHoleState) {
        assertFalse(state.promptRegionEntered)
        assertFalse(state.entryHovered)
        assertNull(state.dwellStartedAtMillis)
        assertEquals(0L, state.dwellElapsedMillis)
        assertFalse(state.knowledgeRegionEntered)
        assertFalse(state.knowledgeHovered)
        assertNull(state.knowledgeExitStartedAtMillis)
    }

    private fun assertAssociation(
        current: KnowledgeMaterialId,
        related: KnowledgeMaterialId,
        title: String,
        keywords: String,
        points: List<String>
    ) {
        val association = KnowledgeAssociations.forMaterial(current)
        assertNotNull(association)
        assertEquals(KnowledgeAssociation(related, title, keywords, points), association)
    }
}
