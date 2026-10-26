package com.example.spatialnav.ui.scene

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpatialConnectionRuntimeSourceDebugTest {
    @Test
    fun debugRuntimeSourceIsSimulationWithExactDefaultScenarioAndDurations() {
        val source = SpatialConnectionRuntimeSource.state

        assertTrue(source is SpatialConnectionSourceState.Simulation)
        val simulation = source as SpatialConnectionSourceState.Simulation
        assertEquals("spatial-connection-preview", simulation.scenarioId)
        assertEquals(
            SpatialConnectionConfig(
                nearEnterMillis = 2_000L,
                farExitMillis = 2_000L,
                pointDelayMillis = 8_000L,
                pointDwellMillis = 800L,
                confirmDwellMillis = 1_200L,
                inviteVisibleMillis = 6_000L,
                cooldownMillis = 8_000L
            ),
            simulation.config
        )
        assertEquals(2_000L, simulation.config.nearEnterMillis)
        assertEquals(2_000L, simulation.config.farExitMillis)
        assertEquals(8_000L, simulation.config.pointDelayMillis)
        assertEquals(800L, simulation.config.pointDwellMillis)
        assertEquals(1_200L, simulation.config.confirmDwellMillis)
        assertEquals(6_000L, simulation.config.inviteVisibleMillis)
        assertEquals(8_000L, simulation.config.cooldownMillis)
    }
}
