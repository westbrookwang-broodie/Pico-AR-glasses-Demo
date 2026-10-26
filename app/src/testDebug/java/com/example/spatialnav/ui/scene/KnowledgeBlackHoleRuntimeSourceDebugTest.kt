package com.example.spatialnav.ui.scene

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KnowledgeBlackHoleRuntimeSourceDebugTest {
    @Test
    fun debugRuntimeSourceProvidesExplicitEligibleDemoInput() {
        val sourceState = KnowledgeBlackHoleRuntimeSource.state

        assertTrue(sourceState is KnowledgeBlackHoleSourceState.Ready)
        val input = (sourceState as KnowledgeBlackHoleSourceState.Ready).input
        assertEquals(
            KnowledgeBlackHoleInput(
                currentMaterial = KnowledgeMaterialId.I,
                kPairValid = true,
                pUpgraded = false,
                distanceBrightnessMultiplier = 1f
            ),
            input
        )
        assertTrue(input.isEligible)
    }
}
