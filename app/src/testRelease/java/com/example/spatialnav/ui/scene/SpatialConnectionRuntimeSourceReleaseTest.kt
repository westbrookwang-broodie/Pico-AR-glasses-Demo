package com.example.spatialnav.ui.scene

import org.junit.Assert.assertSame
import org.junit.Test

class SpatialConnectionRuntimeSourceReleaseTest {
    @Test
    fun releaseRuntimeSourceIsExactlyUnavailable() {
        assertSame(
            SpatialConnectionSourceState.Unavailable,
            SpatialConnectionRuntimeSource.state
        )
    }
}
