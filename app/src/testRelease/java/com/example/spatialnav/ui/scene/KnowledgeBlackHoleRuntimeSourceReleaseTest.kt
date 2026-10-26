package com.example.spatialnav.ui.scene

import org.junit.Assert.assertSame
import org.junit.Test

class KnowledgeBlackHoleRuntimeSourceReleaseTest {
    @Test
    fun releaseRuntimeSourceIsUnavailable() {
        assertSame(
            KnowledgeBlackHoleSourceState.Unavailable,
            KnowledgeBlackHoleRuntimeSource.state
        )
    }
}
