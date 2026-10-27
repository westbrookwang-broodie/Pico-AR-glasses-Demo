package com.example.spatialnav.ui.scene

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SpatialConnectionStateMachineTest {
    @Test
    fun lockedInputsCannotAdvanceAndNearRestartsItsFullDebounceAfterUnlock() {
        val harness = harness()

        harness.send(SpatialConnectionEvent.SetNear(SpatialParticipant.B, true, 100L))
        harness.send(SpatialConnectionEvent.PointHoverChanged(true, 200L))
        harness.send(SpatialConnectionEvent.ConfirmHoverChanged(true, 300L))
        harness.send(SpatialConnectionEvent.SimulatePointDwell(400L))
        harness.send(SpatialConnectionEvent.SimulateConfirmDwell(500L))
        harness.send(SpatialConnectionEvent.MarkDiscussionStarted(600L))
        harness.send(SpatialConnectionEvent.Tick(10_000L))

        assertEquals(SpatialConnectionStage.LOCKED, harness.state.stage)
        assertTrue(harness.state.b.rawNear)
        assertFalse(harness.state.b.confirmedNear)
        assertNull(harness.state.b.changeStartedAtMillis)
        assertNull(harness.state.activeTarget)
        assertFalse(harness.state.pointHovered)
        assertFalse(harness.state.confirmHovered)
        assertFalse(harness.state.discussionStarted)
        assertEquals(
            listOf(SpatialRecordType.RAW_PROXIMITY_CHANGED),
            harness.records.map { it.type }
        )

        val unlock = harness.send(SpatialConnectionEvent.CompleteMaterialView(10_000L))

        assertEquals(SpatialConnectionStage.WAITING_FOR_NEAR, harness.state.stage)
        assertEquals(10_000L, harness.state.b.changeStartedAtMillis)
        assertEquals(
            listOf(
                SpatialConnectionRecord(
                    roundId = 1L,
                    scenarioId = SCENARIO,
                    timestampMillis = 10_000L,
                    type = SpatialRecordType.MATERIAL_VIEW_COMPLETED,
                    participant = null,
                    source = SpatialEventSource.RESEARCHER_SIMULATION,
                    reason = "distance_simulation_unlocked"
                ),
                SpatialConnectionRecord(
                    roundId = 1L,
                    scenarioId = SCENARIO,
                    timestampMillis = 10_000L,
                    type = SpatialRecordType.NEAR_STARTED,
                    participant = SpatialParticipant.B,
                    source = SpatialEventSource.RESEARCHER_SIMULATION,
                    reason = "fresh_timer_after_unlock"
                )
            ),
            unlock.records
        )

        harness.send(SpatialConnectionEvent.Tick(11_999L))
        assertEquals(SpatialConnectionStage.WAITING_FOR_NEAR, harness.state.stage)
        assertFalse(harness.state.b.confirmedNear)

        harness.send(SpatialConnectionEvent.Tick(12_000L))
        assertEquals(SpatialConnectionStage.CONNECTED_WAITING_POINT, harness.state.stage)
        assertEquals(SpatialParticipant.B, harness.state.activeTarget)
        assertEquals(12_000L, harness.state.connectedAtMillis)
    }

    @Test
    fun nearDebounceUses1999And2000BoundariesAndInterruptionCancelsIt() {
        val boundary = waiting()
        boundary.send(SpatialConnectionEvent.SetNear(SpatialParticipant.B, true, 100L))

        boundary.send(SpatialConnectionEvent.Tick(2_099L))
        assertFalse(boundary.state.b.confirmedNear)
        assertNull(boundary.state.activeTarget)

        val confirmed = boundary.send(SpatialConnectionEvent.Tick(2_100L))
        assertTrue(boundary.state.b.confirmedNear)
        assertEquals(SpatialParticipant.B, boundary.state.activeTarget)
        assertEquals(
            listOf(SpatialRecordType.NEAR_CONFIRMED, SpatialRecordType.CONNECTION_SHOWN),
            confirmed.records.map { it.type }
        )

        val interrupted = waiting()
        interrupted.send(SpatialConnectionEvent.SetNear(SpatialParticipant.B, true, 100L))
        interrupted.send(SpatialConnectionEvent.Tick(1_100L))
        val cancellation =
            interrupted.send(
                SpatialConnectionEvent.SetNear(SpatialParticipant.B, false, 1_100L)
            )

        assertEquals(
            listOf(SpatialRecordType.RAW_PROXIMITY_CHANGED, SpatialRecordType.NEAR_CANCELLED),
            cancellation.records.map { it.type }
        )
        assertFalse(interrupted.state.b.rawNear)
        assertFalse(interrupted.state.b.confirmedNear)
        assertNull(interrupted.state.b.changeStartedAtMillis)

        interrupted.send(SpatialConnectionEvent.Tick(10_000L))
        assertEquals(SpatialConnectionStage.WAITING_FOR_NEAR, interrupted.state.stage)
        assertNull(interrupted.state.activeTarget)
        assertFalse(
            interrupted.records.any { it.type == SpatialRecordType.NEAR_CONFIRMED }
        )
    }

    @Test
    fun farDebounceUses1999And2000BoundariesAndNearReturnCancelsExit() {
        val boundary = connectedB()
        boundary.send(SpatialConnectionEvent.SetNear(SpatialParticipant.B, false, 3_000L))

        boundary.send(SpatialConnectionEvent.Tick(4_999L))
        assertEquals(SpatialParticipant.B, boundary.state.activeTarget)
        assertTrue(boundary.state.connectionVisible)

        val disconnected = boundary.send(SpatialConnectionEvent.Tick(5_000L))
        assertEquals(SpatialConnectionStage.WAITING_FOR_NEAR, boundary.state.stage)
        assertNull(boundary.state.activeTarget)
        assertFalse(boundary.state.connectionVisible)
        assertEquals(
            listOf(SpatialRecordType.FAR_CONFIRMED, SpatialRecordType.CONNECTION_HIDDEN),
            disconnected.records.map { it.type }
        )

        val cancelled = connectedB()
        cancelled.send(SpatialConnectionEvent.SetNear(SpatialParticipant.B, false, 3_000L))
        cancelled.send(SpatialConnectionEvent.Tick(4_999L))
        val returnNear =
            cancelled.send(
                SpatialConnectionEvent.SetNear(SpatialParticipant.B, true, 4_999L)
            )

        assertEquals(
            listOf(SpatialRecordType.RAW_PROXIMITY_CHANGED, SpatialRecordType.FAR_CANCELLED),
            returnNear.records.map { it.type }
        )
        assertTrue(cancelled.state.b.rawNear)
        assertTrue(cancelled.state.b.confirmedNear)
        assertNull(cancelled.state.b.changeStartedAtMillis)

        cancelled.send(SpatialConnectionEvent.Tick(5_000L))
        assertEquals(SpatialParticipant.B, cancelled.state.activeTarget)
        assertTrue(cancelled.state.connectionVisible)
        assertFalse(cancelled.records.any { it.type == SpatialRecordType.FAR_CONFIRMED })
    }

    @Test
    fun pointAppearsOnlyAtConnectionDelay8000Boundary() {
        val harness = connectedB()

        harness.send(SpatialConnectionEvent.Tick(9_999L))
        assertEquals(SpatialConnectionStage.CONNECTED_WAITING_POINT, harness.state.stage)
        assertFalse(harness.state.pointVisible)

        val shown = harness.send(SpatialConnectionEvent.Tick(10_000L))
        assertEquals(SpatialConnectionStage.POINT_READY, harness.state.stage)
        assertTrue(harness.state.pointVisible)
        assertEquals(
            listOf(
                SpatialConnectionRecord(
                    roundId = 1L,
                    scenarioId = SCENARIO,
                    timestampMillis = 10_000L,
                    type = SpatialRecordType.POINT_SHOWN,
                    participant = SpatialParticipant.B,
                    source = SpatialEventSource.TIMER,
                    reason = "connection_delay_completed"
                )
            ),
            shown.records
        )

        assertTrue(harness.send(SpatialConnectionEvent.Tick(10_000L)).records.isEmpty())
        assertTrue(harness.send(SpatialConnectionEvent.Tick(10_001L)).records.isEmpty())
    }

    @Test
    fun pointDwellUses799And800BoundariesAndInterruptionDoesNotAccumulate() {
        val boundary = pointReady()
        boundary.send(SpatialConnectionEvent.PointHoverChanged(true, 10_000L))

        boundary.send(SpatialConnectionEvent.Tick(10_799L))
        assertEquals(SpatialConnectionStage.POINT_DWELL, boundary.state.stage)
        assertEquals(799L, boundary.state.dwellElapsedMillis)
        assertEquals(799f / 800f, boundary.state.dwellProgress, 0f)

        val completed = boundary.send(SpatialConnectionEvent.Tick(10_800L))
        assertEquals(SpatialConnectionStage.INVITE_VISIBLE, boundary.state.stage)
        assertEquals(10_800L, boundary.state.inviteShownAtMillis)
        assertEquals(
            listOf(SpatialRecordType.POINT_DWELL_COMPLETED, SpatialRecordType.INVITE_SHOWN),
            completed.records.map { it.type }
        )

        val interrupted = pointReady()
        interrupted.send(SpatialConnectionEvent.PointHoverChanged(true, 10_000L))
        interrupted.send(SpatialConnectionEvent.Tick(10_799L))
        val cancellation =
            interrupted.send(SpatialConnectionEvent.PointHoverChanged(false, 10_799L))

        assertEquals(SpatialConnectionStage.POINT_READY, interrupted.state.stage)
        assertNull(interrupted.state.dwellStartedAtMillis)
        assertEquals(0L, interrupted.state.dwellElapsedMillis)
        assertEquals(0f, interrupted.state.dwellProgress, 0f)
        assertEquals(
            listOf(SpatialRecordType.POINT_DWELL_CANCELLED),
            cancellation.records.map { it.type }
        )

        interrupted.send(SpatialConnectionEvent.PointHoverChanged(true, 12_000L))
        interrupted.send(SpatialConnectionEvent.Tick(12_799L))
        assertEquals(SpatialConnectionStage.POINT_DWELL, interrupted.state.stage)
        assertEquals(799L, interrupted.state.dwellElapsedMillis)
        interrupted.send(SpatialConnectionEvent.Tick(12_800L))
        assertEquals(SpatialConnectionStage.INVITE_VISIBLE, interrupted.state.stage)
    }

    @Test
    fun inviteTimeoutUses5999And6000BoundariesAndStartsCooldownOnce() {
        val harness = inviteVisible()

        harness.send(SpatialConnectionEvent.Tick(16_799L))
        assertEquals(SpatialConnectionStage.INVITE_VISIBLE, harness.state.stage)
        assertTrue(harness.state.inviteVisible)
        assertNull(harness.state.cooldownStartedAtMillis)

        val timedOut = harness.send(SpatialConnectionEvent.Tick(16_800L))
        assertEquals(SpatialConnectionStage.POINT_READY, harness.state.stage)
        assertFalse(harness.state.inviteVisible)
        assertEquals(16_800L, harness.state.cooldownStartedAtMillis)
        assertEquals(
            listOf(
                SpatialConnectionRecord(
                    roundId = 1L,
                    scenarioId = SCENARIO,
                    timestampMillis = 16_800L,
                    type = SpatialRecordType.INVITE_TIMED_OUT,
                    participant = SpatialParticipant.B,
                    source = SpatialEventSource.TIMER,
                    reason = "unconfirmed_card_cooldown_requires_new_enter"
                )
            ),
            timedOut.records
        )

        assertTrue(harness.send(SpatialConnectionEvent.Tick(16_800L)).records.isEmpty())
        assertEquals(
            1,
            harness.records.count { it.type == SpatialRecordType.INVITE_TIMED_OUT }
        )
    }

    @Test
    fun cooldownUses7999And8000BoundariesAndSustainedHoverCannotAutoTrigger() {
        val harness = inviteVisible()
        assertTrue(harness.state.pointHovered)
        harness.send(SpatialConnectionEvent.Tick(16_800L))

        harness.send(SpatialConnectionEvent.Tick(24_799L))
        assertEquals(16_800L, harness.state.cooldownStartedAtMillis)
        assertEquals(SpatialConnectionStage.POINT_READY, harness.state.stage)

        harness.send(SpatialConnectionEvent.Tick(24_800L))
        assertNull(harness.state.cooldownStartedAtMillis)
        assertTrue(harness.state.pointHovered)
        assertEquals(SpatialConnectionStage.POINT_READY, harness.state.stage)

        harness.send(SpatialConnectionEvent.Tick(30_000L))
        assertEquals(SpatialConnectionStage.POINT_READY, harness.state.stage)
        assertNull(harness.state.dwellStartedAtMillis)

        harness.send(SpatialConnectionEvent.PointHoverChanged(false, 30_001L))
        val newEnter =
            harness.send(SpatialConnectionEvent.PointHoverChanged(true, 30_002L))
        assertEquals(SpatialConnectionStage.POINT_DWELL, harness.state.stage)
        assertEquals(30_002L, harness.state.dwellStartedAtMillis)
        assertEquals(
            listOf(SpatialRecordType.POINT_DWELL_STARTED),
            newEnter.records.map { it.type }
        )
    }

    @Test
    fun hoverEnteredDuringCooldownRequiresExitAndAnotherEnterAfterCooldown() {
        val harness = inviteVisibleFromSimulation()
        assertFalse(harness.state.pointHovered)
        harness.send(SpatialConnectionEvent.Tick(16_000L))

        harness.send(SpatialConnectionEvent.Tick(23_999L))
        harness.send(SpatialConnectionEvent.PointHoverChanged(true, 23_999L))
        assertTrue(harness.state.pointHovered)
        assertEquals(SpatialConnectionStage.POINT_READY, harness.state.stage)
        assertEquals(16_000L, harness.state.cooldownStartedAtMillis)

        harness.send(SpatialConnectionEvent.Tick(24_000L))
        assertNull(harness.state.cooldownStartedAtMillis)
        assertEquals(SpatialConnectionStage.POINT_READY, harness.state.stage)
        assertNull(harness.state.dwellStartedAtMillis)

        harness.send(SpatialConnectionEvent.PointHoverChanged(false, 24_001L))
        harness.send(SpatialConnectionEvent.PointHoverChanged(true, 24_002L))
        assertEquals(SpatialConnectionStage.POINT_DWELL, harness.state.stage)
        assertEquals(24_002L, harness.state.dwellStartedAtMillis)

        harness.send(SpatialConnectionEvent.Tick(24_801L))
        assertEquals(SpatialConnectionStage.POINT_DWELL, harness.state.stage)
        harness.send(SpatialConnectionEvent.Tick(24_802L))
        assertEquals(SpatialConnectionStage.INVITE_VISIBLE, harness.state.stage)
        assertEquals(2, harness.records.count { it.type == SpatialRecordType.INVITE_SHOWN })
        assertEquals(1, harness.records.count { it.type == SpatialRecordType.INVITE_TIMED_OUT })
    }

    @Test
    fun confirmationUses1199And1200BoundariesAndInterruptionClearsProgress() {
        val boundary = inviteVisible()
        boundary.send(SpatialConnectionEvent.ConfirmHoverChanged(true, 11_000L))

        boundary.send(SpatialConnectionEvent.Tick(12_199L))
        assertEquals(SpatialConnectionStage.CONFIRM_DWELL, boundary.state.stage)
        assertEquals(1_199L, boundary.state.dwellElapsedMillis)
        assertEquals(1_199f / 1_200f, boundary.state.dwellProgress, 0f)

        val completed = boundary.send(SpatialConnectionEvent.Tick(12_200L))
        assertEquals(SpatialConnectionStage.DISCUSSION_STARTED, boundary.state.stage)
        assertTrue(boundary.state.connectionVisible)
        assertTrue(boundary.state.discussionStarted)
        assertFalse(boundary.state.pointVisible)
        assertFalse(boundary.state.inviteVisible)
        assertEquals(
            listOf(SpatialRecordType.CONFIRM_COMPLETED),
            completed.records.map { it.type }
        )

        val interrupted = inviteVisible()
        interrupted.send(SpatialConnectionEvent.ConfirmHoverChanged(true, 11_000L))
        interrupted.send(SpatialConnectionEvent.Tick(11_500L))
        val cancellation =
            interrupted.send(SpatialConnectionEvent.ConfirmHoverChanged(false, 11_500L))

        assertEquals(SpatialConnectionStage.INVITE_VISIBLE, interrupted.state.stage)
        assertNull(interrupted.state.dwellStartedAtMillis)
        assertEquals(0L, interrupted.state.dwellElapsedMillis)
        assertEquals(0f, interrupted.state.dwellProgress, 0f)
        assertEquals(
            listOf(SpatialRecordType.CONFIRM_DWELL_CANCELLED),
            cancellation.records.map { it.type }
        )

        interrupted.send(SpatialConnectionEvent.ConfirmHoverChanged(true, 12_000L))
        interrupted.send(SpatialConnectionEvent.Tick(13_199L))
        assertEquals(SpatialConnectionStage.CONFIRM_DWELL, interrupted.state.stage)
        interrupted.send(SpatialConnectionEvent.Tick(13_200L))
        assertEquals(SpatialConnectionStage.DISCUSSION_STARTED, interrupted.state.stage)
    }

    @Test
    fun confirmationMayFinishPastDeadlineButInterruptedConfirmationRestoresOriginalDeadline() {
        val completing = inviteVisible()
        completing.send(SpatialConnectionEvent.ConfirmHoverChanged(true, 16_700L))

        completing.send(SpatialConnectionEvent.Tick(17_899L))
        assertEquals(SpatialConnectionStage.CONFIRM_DWELL, completing.state.stage)
        assertTrue(completing.state.inviteVisible)
        completing.send(SpatialConnectionEvent.Tick(17_900L))
        assertEquals(SpatialConnectionStage.DISCUSSION_STARTED, completing.state.stage)
        assertFalse(
            completing.records.any { it.type == SpatialRecordType.INVITE_TIMED_OUT }
        )

        val interrupted = inviteVisible()
        interrupted.send(SpatialConnectionEvent.ConfirmHoverChanged(true, 16_700L))
        val restored =
            interrupted.send(SpatialConnectionEvent.ConfirmHoverChanged(false, 16_800L))

        assertEquals(SpatialConnectionStage.POINT_READY, interrupted.state.stage)
        assertFalse(interrupted.state.inviteVisible)
        assertNull(interrupted.state.dwellStartedAtMillis)
        assertEquals(0L, interrupted.state.dwellElapsedMillis)
        assertEquals(16_800L, interrupted.state.cooldownStartedAtMillis)
        assertEquals(
            listOf(
                SpatialRecordType.CONFIRM_DWELL_CANCELLED,
                SpatialRecordType.INVITE_TIMED_OUT
            ),
            restored.records.map { it.type }
        )
    }

    @Test
    fun simultaneousBAndCConfirmationSelectsBAndConfirmedCDoesNotPreemptActiveB() {
        val simultaneous = waiting()
        simultaneous.send(SpatialConnectionEvent.SetNear(SpatialParticipant.C, true, 0L))
        simultaneous.send(SpatialConnectionEvent.SetNear(SpatialParticipant.B, true, 0L))
        val confirmed = simultaneous.send(SpatialConnectionEvent.Tick(2_000L))

        assertTrue(simultaneous.state.b.confirmedNear)
        assertTrue(simultaneous.state.c.confirmedNear)
        assertEquals(SpatialParticipant.B, simultaneous.state.activeTarget)
        assertEquals(
            listOf(
                SpatialRecordType.NEAR_CONFIRMED,
                SpatialRecordType.NEAR_CONFIRMED,
                SpatialRecordType.CONNECTION_SHOWN
            ),
            confirmed.records.map { it.type }
        )
        assertEquals(
            listOf(SpatialParticipant.B, SpatialParticipant.C, SpatialParticipant.B),
            confirmed.records.map { it.participant }
        )

        val laterC = connectedB()
        laterC.send(SpatialConnectionEvent.SetNear(SpatialParticipant.C, true, 3_000L))
        val cConfirmed = laterC.send(SpatialConnectionEvent.Tick(5_000L))

        assertTrue(laterC.state.c.confirmedNear)
        assertEquals(SpatialParticipant.B, laterC.state.activeTarget)
        assertEquals(
            listOf(SpatialRecordType.NEAR_CONFIRMED),
            cConfirmed.records.map { it.type }
        )
        assertFalse(
            laterC.records.any { it.type == SpatialRecordType.CONNECTION_SWITCHED }
        )
    }

    @Test
    fun bFarSwitchesToConfirmedCAndRestartsFullPointDelay() {
        val harness = connectedBothWithBActive()
        harness.send(SpatialConnectionEvent.SetNear(SpatialParticipant.B, false, 3_000L))

        harness.send(SpatialConnectionEvent.Tick(4_999L))
        assertEquals(SpatialParticipant.B, harness.state.activeTarget)

        val switched = harness.send(SpatialConnectionEvent.Tick(5_000L))
        assertEquals(SpatialParticipant.C, harness.state.activeTarget)
        assertEquals(SpatialConnectionStage.CONNECTED_WAITING_POINT, harness.state.stage)
        assertEquals(5_000L, harness.state.connectedAtMillis)
        assertEquals(
            listOf(
                SpatialRecordType.FAR_CONFIRMED,
                SpatialRecordType.CONNECTION_HIDDEN,
                SpatialRecordType.CONNECTION_SWITCHED,
                SpatialRecordType.CONNECTION_SHOWN
            ),
            switched.records.map { it.type }
        )
        assertEquals(
            listOf(
                SpatialParticipant.B,
                SpatialParticipant.B,
                SpatialParticipant.C,
                SpatialParticipant.C
            ),
            switched.records.map { it.participant }
        )
        assertEquals("B_to_C", switched.records[2].reason)

        harness.send(SpatialConnectionEvent.Tick(12_999L))
        assertEquals(SpatialConnectionStage.CONNECTED_WAITING_POINT, harness.state.stage)
        harness.send(SpatialConnectionEvent.Tick(13_000L))
        assertEquals(SpatialConnectionStage.POINT_READY, harness.state.stage)
    }

    @Test
    fun activeParticipantFarWithNoAlternativeHidesConnection() {
        val harness = connectedB()
        harness.send(SpatialConnectionEvent.SetNear(SpatialParticipant.B, false, 3_000L))
        val hidden = harness.send(SpatialConnectionEvent.Tick(5_000L))

        assertEquals(SpatialConnectionStage.WAITING_FOR_NEAR, harness.state.stage)
        assertNull(harness.state.activeTarget)
        assertFalse(harness.state.connectionVisible)
        assertNull(harness.state.connectedAtMillis)
        assertTrue(harness.state.spatialStageEntered)
        assertEquals(
            listOf(SpatialRecordType.FAR_CONFIRMED, SpatialRecordType.CONNECTION_HIDDEN),
            hidden.records.map { it.type }
        )
    }

    @Test
    fun researcherMarkCancelsInvitationWorkAtEveryConnectedStageAndKeepsLine() {
        val cases =
            listOf(
                StageCase("connected waiting", connectedB(), 2_500L),
                StageCase("point ready", pointReady(), 10_000L),
                StageCase("point dwell", pointDwell(), 10_400L),
                StageCase("invite", inviteVisible(), 11_000L),
                StageCase("confirm", confirmDwell(), 10_900L)
            )

        cases.forEach { case ->
            val beforeStage = case.harness.state.stage
            val transition =
                case.harness.send(
                    SpatialConnectionEvent.MarkDiscussionStarted(case.markAtMillis)
                )

            assertEquals(
                case.name,
                SpatialConnectionStage.DISCUSSION_STARTED,
                case.harness.state.stage
            )
            assertEquals(case.name, SpatialParticipant.B, case.harness.state.activeTarget)
            assertTrue(case.name, case.harness.state.connectionVisible)
            assertTrue(case.name, case.harness.state.discussionStarted)
            assertFalse(case.name, case.harness.state.pointVisible)
            assertFalse(case.name, case.harness.state.inviteVisible)
            assertNull(case.name, case.harness.state.connectedAtMillis)
            assertNull(case.name, case.harness.state.dwellStartedAtMillis)
            assertEquals(case.name, 0L, case.harness.state.dwellElapsedMillis)
            assertTrue(
                case.name,
                transition.records.any {
                    it.type == SpatialRecordType.DISCUSSION_MARKED &&
                        it.participant == SpatialParticipant.B
                }
            )
            if (
                beforeStage == SpatialConnectionStage.POINT_DWELL ||
                    beforeStage == SpatialConnectionStage.CONFIRM_DWELL
            ) {
                assertTrue(
                    case.name,
                    transition.records.any {
                        it.type == SpatialRecordType.POINT_DWELL_CANCELLED ||
                            it.type == SpatialRecordType.CONFIRM_DWELL_CANCELLED
                    }
                )
            }

            case.harness.send(SpatialConnectionEvent.Tick(case.markAtMillis + 50_000L))
            assertEquals(
                case.name,
                SpatialConnectionStage.DISCUSSION_STARTED,
                case.harness.state.stage
            )
            assertFalse(case.name, case.harness.state.pointVisible)
            assertFalse(case.name, case.harness.state.inviteVisible)
        }
    }

    @Test
    fun discussionMarkRemainsRoundWideAcrossFarAndReconnection() {
        val harness = inviteVisible()
        harness.send(SpatialConnectionEvent.MarkDiscussionStarted(11_000L))

        harness.send(SpatialConnectionEvent.SetNear(SpatialParticipant.B, false, 12_000L))
        harness.send(SpatialConnectionEvent.Tick(14_000L))
        assertEquals(SpatialConnectionStage.WAITING_FOR_NEAR, harness.state.stage)
        assertNull(harness.state.activeTarget)
        assertTrue(harness.state.discussionStarted)

        harness.send(SpatialConnectionEvent.SetNear(SpatialParticipant.B, true, 15_000L))
        harness.send(SpatialConnectionEvent.Tick(17_000L))
        assertEquals(SpatialConnectionStage.DISCUSSION_STARTED, harness.state.stage)
        assertEquals(SpatialParticipant.B, harness.state.activeTarget)
        assertTrue(harness.state.connectionVisible)
        assertNull(harness.state.connectedAtMillis)

        harness.send(SpatialConnectionEvent.Tick(50_000L))
        assertEquals(SpatialConnectionStage.DISCUSSION_STARTED, harness.state.stage)
        assertFalse(harness.state.pointVisible)
        assertFalse(harness.state.inviteVisible)
        assertEquals(1, harness.records.count { it.type == SpatialRecordType.DISCUSSION_MARKED })
        assertEquals(1, harness.records.count { it.type == SpatialRecordType.INVITE_SHOWN })
        assertEquals(1, harness.records.count { it.type == SpatialRecordType.POINT_SHOWN })
    }

    @Test
    fun resetFromEveryKeyStageClearsRoundStateIncrementsRoundAndRecordsReset() {
        val cases =
            listOf(
                StageCase("locked", harness(), 1L),
                StageCase("waiting", waiting(), 1L),
                StageCase("connected", connectedB(), 2_001L),
                StageCase("point ready", pointReady(), 10_001L),
                StageCase("point dwell", pointDwell(), 10_001L),
                StageCase("invite", inviteVisible(), 10_801L),
                StageCase("confirm", confirmDwell(), 10_801L),
                StageCase("discussion", discussionStarted(), 10_801L)
            )

        cases.forEach { case ->
            val before = case.harness.state
            val transition =
                case.harness.send(SpatialConnectionEvent.Reset(case.markAtMillis))
            val expected =
                SpatialConnectionStateMachine.initial(SCENARIO, before.config)
                    .copy(roundId = before.roundId + 1L, lastNowMillis = case.markAtMillis)

            assertEquals(case.name, expected, case.harness.state)
            assertEquals(case.name, SpatialRecordType.RESET, transition.records.last().type)
            assertEquals(case.name, before.roundId + 1L, transition.records.last().roundId)
            assertEquals(case.name, SCENARIO, transition.records.last().scenarioId)
            assertEquals(case.name, case.markAtMillis, transition.records.last().timestampMillis)
            assertNull(case.name, transition.records.last().participant)
            assertEquals(
                case.name,
                SpatialEventSource.RESEARCHER_SIMULATION,
                transition.records.last().source
            )
            assertEquals(case.name, "new_round", transition.records.last().reason)

            if (before.connectionVisible) {
                assertTrue(
                    case.name,
                    transition.records.any {
                        it.type == SpatialRecordType.CONNECTION_HIDDEN &&
                            it.roundId == before.roundId &&
                            it.reason == "round_reset"
                    }
                )
            }
            if (before.stage == SpatialConnectionStage.POINT_DWELL) {
                assertEquals(
                    case.name,
                    SpatialRecordType.POINT_DWELL_CANCELLED,
                    transition.records.first().type
                )
            }
            if (before.stage == SpatialConnectionStage.CONFIRM_DWELL) {
                assertEquals(
                    case.name,
                    SpatialRecordType.CONFIRM_DWELL_CANCELLED,
                    transition.records.first().type
                )
            }
        }
    }

    @Test
    fun sessionRetainsPreviousRoundRecordsWhenReducerIsReset() {
        val session =
            SpatialConnectionSession(
                SpatialConnectionSourceState.Simulation(
                    scenarioId = SCENARIO,
                    config = SpatialConnectionConfig()
                )
            )
        session.dispatch(SpatialConnectionEvent.CompleteMaterialView(0L))
        session.dispatch(SpatialConnectionEvent.SetNear(SpatialParticipant.B, true, 0L))
        session.dispatch(SpatialConnectionEvent.Tick(2_000L))
        val previousRoundRecords = session.records.toList()

        session.dispatch(SpatialConnectionEvent.Reset(3_000L))

        assertEquals(previousRoundRecords, session.records.take(previousRoundRecords.size))
        assertEquals(2L, session.state.roundId)
        assertEquals(SpatialConnectionStage.LOCKED, session.state.stage)
        assertEquals(
            listOf(SpatialRecordType.CONNECTION_HIDDEN, SpatialRecordType.RESET),
            session.records.drop(previousRoundRecords.size).map { it.type }
        )
        assertEquals(1L, session.records[previousRoundRecords.size].roundId)
        assertEquals(2L, session.records.last().roundId)
    }

    @Test
    fun spatialStageEnteredStartsOnlyOnFirstConfirmedConnectionIsStickyUntilReset() {
        val harness = harness()
        assertFalse(harness.state.spatialStageEntered)

        harness.send(SpatialConnectionEvent.CompleteMaterialView(10L))
        assertFalse(harness.state.spatialStageEntered)
        harness.send(SpatialConnectionEvent.SetNear(SpatialParticipant.B, true, 20L))
        harness.send(SpatialConnectionEvent.Tick(2_019L))
        assertFalse(harness.state.spatialStageEntered)

        harness.send(SpatialConnectionEvent.Tick(2_020L))
        assertTrue(harness.state.spatialStageEntered)
        harness.send(SpatialConnectionEvent.SetNear(SpatialParticipant.B, false, 3_000L))
        harness.send(SpatialConnectionEvent.Tick(5_000L))
        assertNull(harness.state.activeTarget)
        assertTrue(harness.state.spatialStageEntered)

        harness.send(SpatialConnectionEvent.Reset(6_000L))
        assertFalse(harness.state.spatialStageEntered)
        assertEquals(2L, harness.state.roundId)
    }

    @Test
    fun staleEventsCannotChangeRawInputStageOrLogs() {
        val harness = pointReady()
        val before = harness.state
        val recordCount = harness.records.size
        val staleEvents =
            listOf(
                SpatialConnectionEvent.CompleteMaterialView(9_999L),
                SpatialConnectionEvent.SetNear(SpatialParticipant.B, false, 9_999L),
                SpatialConnectionEvent.SetNear(SpatialParticipant.C, true, 9_999L),
                SpatialConnectionEvent.PointHoverChanged(true, 9_999L),
                SpatialConnectionEvent.ConfirmHoverChanged(true, 9_999L),
                SpatialConnectionEvent.SimulatePointDwell(9_999L),
                SpatialConnectionEvent.SimulateConfirmDwell(9_999L),
                SpatialConnectionEvent.MarkDiscussionStarted(9_999L),
                SpatialConnectionEvent.Tick(9_999L)
            )

        staleEvents.forEach { event ->
            val transition = harness.send(event)
            assertEquals(event.toString(), before, transition.state)
            assertTrue(event.toString(), transition.records.isEmpty())
        }

        assertEquals(before, harness.state)
        assertTrue(harness.state.b.rawNear)
        assertFalse(harness.state.c.rawNear)
        assertEquals(recordCount, harness.records.size)
    }

    @Test
    fun repeatedTicksDoNotDuplicateConnectionPointOrTimeoutRecords() {
        val harness = waiting()
        harness.send(SpatialConnectionEvent.SetNear(SpatialParticipant.B, true, 0L))
        harness.send(SpatialConnectionEvent.Tick(2_000L))
        harness.send(SpatialConnectionEvent.Tick(2_000L))
        harness.send(SpatialConnectionEvent.Tick(2_001L))

        harness.send(SpatialConnectionEvent.Tick(10_000L))
        harness.send(SpatialConnectionEvent.Tick(10_000L))
        harness.send(SpatialConnectionEvent.Tick(10_001L))

        harness.send(SpatialConnectionEvent.SimulatePointDwell(10_001L))
        harness.send(SpatialConnectionEvent.Tick(16_001L))
        harness.send(SpatialConnectionEvent.Tick(16_001L))
        harness.send(SpatialConnectionEvent.Tick(16_002L))

        assertEquals(1, harness.records.count { it.type == SpatialRecordType.NEAR_CONFIRMED })
        assertEquals(1, harness.records.count { it.type == SpatialRecordType.CONNECTION_SHOWN })
        assertEquals(1, harness.records.count { it.type == SpatialRecordType.POINT_SHOWN })
        assertEquals(1, harness.records.count { it.type == SpatialRecordType.INVITE_TIMED_OUT })
    }

    @Test
    fun simulatedPointDwellIsIgnoredOutsideLegalPointStagesAndCompletesLegalStages() {
        assertIgnored(harness(), SpatialConnectionEvent.SimulatePointDwell(0L))
        assertIgnored(waiting(), SpatialConnectionEvent.SimulatePointDwell(0L))
        assertIgnored(connectedB(), SpatialConnectionEvent.SimulatePointDwell(2_000L))
        assertIgnored(inviteVisible(), SpatialConnectionEvent.SimulatePointDwell(10_800L))
        assertIgnored(confirmDwell(), SpatialConnectionEvent.SimulatePointDwell(10_800L))
        assertIgnored(discussionStarted(), SpatialConnectionEvent.SimulatePointDwell(10_800L))

        val ready = pointReady()
        val fromReady = ready.send(SpatialConnectionEvent.SimulatePointDwell(10_000L))
        assertEquals(SpatialConnectionStage.INVITE_VISIBLE, ready.state.stage)
        assertEquals(
            listOf(
                SpatialRecordType.POINT_DWELL_STARTED,
                SpatialRecordType.POINT_DWELL_COMPLETED,
                SpatialRecordType.INVITE_SHOWN
            ),
            fromReady.records.map { it.type }
        )
        assertTrue(
            fromReady.records.all { it.source == SpatialEventSource.RESEARCHER_SIMULATION }
        )

        val dwelling = pointDwell()
        val fromDwell = dwelling.send(SpatialConnectionEvent.SimulatePointDwell(10_000L))
        assertEquals(SpatialConnectionStage.INVITE_VISIBLE, dwelling.state.stage)
        assertEquals(
            listOf(SpatialRecordType.POINT_DWELL_COMPLETED, SpatialRecordType.INVITE_SHOWN),
            fromDwell.records.map { it.type }
        )
    }

    @Test
    fun simulatedConfirmationIsIgnoredOutsideInviteAndConfirmAndCompletesLegalStages() {
        assertIgnored(harness(), SpatialConnectionEvent.SimulateConfirmDwell(0L))
        assertIgnored(waiting(), SpatialConnectionEvent.SimulateConfirmDwell(0L))
        assertIgnored(pointReady(), SpatialConnectionEvent.SimulateConfirmDwell(10_000L))
        assertIgnored(discussionStarted(), SpatialConnectionEvent.SimulateConfirmDwell(10_800L))

        val invite = inviteVisible()
        val fromInvite = invite.send(SpatialConnectionEvent.SimulateConfirmDwell(10_800L))
        assertEquals(SpatialConnectionStage.DISCUSSION_STARTED, invite.state.stage)
        assertEquals(
            listOf(SpatialRecordType.CONFIRM_DWELL_STARTED, SpatialRecordType.CONFIRM_COMPLETED),
            fromInvite.records.map { it.type }
        )
        assertTrue(
            fromInvite.records.all { it.source == SpatialEventSource.RESEARCHER_SIMULATION }
        )

        val confirming = confirmDwell()
        val fromConfirm =
            confirming.send(SpatialConnectionEvent.SimulateConfirmDwell(10_800L))
        assertEquals(SpatialConnectionStage.DISCUSSION_STARTED, confirming.state.stage)
        assertEquals(
            listOf(SpatialRecordType.CONFIRM_COMPLETED),
            fromConfirm.records.map { it.type }
        )
    }

    @Test
    fun recordsCarryExactRoundScenarioTimestampTypeParticipantSourceAndReason() {
        val harness = harness()
        harness.send(SpatialConnectionEvent.CompleteMaterialView(100L))
        harness.send(SpatialConnectionEvent.SetNear(SpatialParticipant.B, true, 200L))
        harness.send(SpatialConnectionEvent.Tick(2_200L))
        harness.send(SpatialConnectionEvent.Tick(10_200L))
        harness.send(SpatialConnectionEvent.PointHoverChanged(true, 10_201L))
        harness.send(SpatialConnectionEvent.Tick(11_001L))
        harness.send(SpatialConnectionEvent.ConfirmHoverChanged(true, 11_002L))
        harness.send(SpatialConnectionEvent.Tick(12_202L))

        assertEquals(
            listOf(
                record(
                    100L,
                    SpatialRecordType.MATERIAL_VIEW_COMPLETED,
                    null,
                    SpatialEventSource.RESEARCHER_SIMULATION,
                    "distance_simulation_unlocked"
                ),
                record(
                    200L,
                    SpatialRecordType.RAW_PROXIMITY_CHANGED,
                    SpatialParticipant.B,
                    SpatialEventSource.RESEARCHER_SIMULATION,
                    "raw_near"
                ),
                record(
                    200L,
                    SpatialRecordType.NEAR_STARTED,
                    SpatialParticipant.B,
                    SpatialEventSource.RESEARCHER_SIMULATION,
                    "debounce_started"
                ),
                record(
                    2_200L,
                    SpatialRecordType.NEAR_CONFIRMED,
                    SpatialParticipant.B,
                    SpatialEventSource.TIMER,
                    "sustained_raw_input"
                ),
                record(
                    2_200L,
                    SpatialRecordType.CONNECTION_SHOWN,
                    SpatialParticipant.B,
                    SpatialEventSource.TIMER,
                    "confirmed_near_simulated_2d_connection"
                ),
                record(
                    10_200L,
                    SpatialRecordType.POINT_SHOWN,
                    SpatialParticipant.B,
                    SpatialEventSource.TIMER,
                    "connection_delay_completed"
                ),
                record(
                    10_201L,
                    SpatialRecordType.POINT_DWELL_STARTED,
                    SpatialParticipant.B,
                    SpatialEventSource.LOCAL_HOVER,
                    "fresh_point_hover_enter"
                ),
                record(
                    11_001L,
                    SpatialRecordType.POINT_DWELL_COMPLETED,
                    SpatialParticipant.B,
                    SpatialEventSource.LOCAL_HOVER,
                    "continuous_dwell_800ms"
                ),
                record(
                    11_001L,
                    SpatialRecordType.INVITE_SHOWN,
                    SpatialParticipant.B,
                    SpatialEventSource.LOCAL_HOVER,
                    "point_dwell_completed"
                ),
                record(
                    11_002L,
                    SpatialRecordType.CONFIRM_DWELL_STARTED,
                    SpatialParticipant.B,
                    SpatialEventSource.LOCAL_HOVER,
                    "confirm_hover_enter_timeout_suspended"
                ),
                record(
                    12_202L,
                    SpatialRecordType.CONFIRM_COMPLETED,
                    SpatialParticipant.B,
                    SpatialEventSource.LOCAL_HOVER,
                    "continuous_dwell_1200ms"
                )
            ),
            harness.records
        )
    }

    @Test
    fun configRejectsZeroAndNegativeValuesForEveryDuration() {
        val valid = SpatialConnectionConfig()
        val invalidFactories =
            listOf<() -> SpatialConnectionConfig>(
                { valid.copy(nearEnterMillis = 0L) },
                { valid.copy(nearEnterMillis = -1L) },
                { valid.copy(farExitMillis = 0L) },
                { valid.copy(farExitMillis = -1L) },
                { valid.copy(pointDelayMillis = 0L) },
                { valid.copy(pointDelayMillis = -1L) },
                { valid.copy(pointDwellMillis = 0L) },
                { valid.copy(pointDwellMillis = -1L) },
                { valid.copy(confirmDwellMillis = 0L) },
                { valid.copy(confirmDwellMillis = -1L) },
                { valid.copy(inviteVisibleMillis = 0L) },
                { valid.copy(inviteVisibleMillis = -1L) },
                { valid.copy(cooldownMillis = 0L) },
                { valid.copy(cooldownMillis = -1L) }
            )

        invalidFactories.forEach { factory ->
            assertThrows(IllegalArgumentException::class.java) { factory() }
        }
    }

    private fun harness(config: SpatialConnectionConfig = SpatialConnectionConfig()): Harness =
        Harness(SpatialConnectionStateMachine.initial(SCENARIO, config))

    private fun waiting(): Harness =
        harness().apply { send(SpatialConnectionEvent.CompleteMaterialView(0L)) }

    private fun connectedB(): Harness =
        waiting().apply {
            send(SpatialConnectionEvent.SetNear(SpatialParticipant.B, true, 0L))
            send(SpatialConnectionEvent.Tick(2_000L))
        }

    private fun connectedBothWithBActive(): Harness =
        waiting().apply {
            send(SpatialConnectionEvent.SetNear(SpatialParticipant.C, true, 0L))
            send(SpatialConnectionEvent.SetNear(SpatialParticipant.B, true, 0L))
            send(SpatialConnectionEvent.Tick(2_000L))
        }

    private fun pointReady(): Harness =
        connectedB().apply { send(SpatialConnectionEvent.Tick(10_000L)) }

    private fun pointDwell(): Harness =
        pointReady().apply {
            send(SpatialConnectionEvent.PointHoverChanged(true, 10_000L))
        }

    private fun inviteVisible(): Harness =
        pointDwell().apply { send(SpatialConnectionEvent.Tick(10_800L)) }

    private fun inviteVisibleFromSimulation(): Harness =
        pointReady().apply { send(SpatialConnectionEvent.SimulatePointDwell(10_000L)) }

    private fun confirmDwell(): Harness =
        inviteVisible().apply {
            send(SpatialConnectionEvent.ConfirmHoverChanged(true, 10_800L))
        }

    private fun discussionStarted(): Harness =
        inviteVisible().apply {
            send(SpatialConnectionEvent.SimulateConfirmDwell(10_800L))
        }

    private fun assertIgnored(harness: Harness, event: SpatialConnectionEvent) {
        val before = harness.state
        val transition = harness.send(event)

        assertEquals(before, transition.state)
        assertTrue(transition.records.isEmpty())
    }

    private fun record(
        timestampMillis: Long,
        type: SpatialRecordType,
        participant: SpatialParticipant?,
        source: SpatialEventSource,
        reason: String
    ): SpatialConnectionRecord =
        SpatialConnectionRecord(
            roundId = 1L,
            scenarioId = SCENARIO,
            timestampMillis = timestampMillis,
            type = type,
            participant = participant,
            source = source,
            reason = reason
        )

    private class Harness(initialState: SpatialConnectionState) {
        var state: SpatialConnectionState = initialState
            private set

        val records = mutableListOf<SpatialConnectionRecord>()

        fun send(event: SpatialConnectionEvent): SpatialConnectionTransition {
            val transition = SpatialConnectionStateMachine.reduce(state, event)
            state = transition.state
            records += transition.records
            return transition
        }
    }

    private data class StageCase(
        val name: String,
        val harness: Harness,
        val markAtMillis: Long
    )

    private companion object {
        const val SCENARIO = "scenario-test-alpha"
    }
}
