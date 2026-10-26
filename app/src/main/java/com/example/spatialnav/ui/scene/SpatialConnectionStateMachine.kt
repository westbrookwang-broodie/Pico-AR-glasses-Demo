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

/** No physical-distance, tracking or gaze-only producer is connected to this prototype. */
sealed interface SpatialConnectionSourceState {
    object Unavailable : SpatialConnectionSourceState

    data class Simulation(
        val scenarioId: String = "spatial-connection-preview",
        val config: SpatialConnectionConfig = SpatialConnectionConfig()
    ) : SpatialConnectionSourceState
}

data class SpatialConnectionConfig(
    val nearEnterMillis: Long = 2000L,
    val farExitMillis: Long = 2000L,
    val pointDelayMillis: Long = 8000L,
    val pointDwellMillis: Long = 800L,
    val confirmDwellMillis: Long = 1200L,
    val inviteVisibleMillis: Long = 6000L,
    val cooldownMillis: Long = 8000L
) {
    init {
        require(nearEnterMillis > 0L && farExitMillis > 0L)
        require(pointDelayMillis > 0L && pointDwellMillis > 0L && confirmDwellMillis > 0L)
        require(inviteVisibleMillis > 0L && cooldownMillis > 0L)
    }
}

enum class SpatialParticipant {
    B,
    C
}

enum class SpatialConnectionStage {
    LOCKED,
    WAITING_FOR_NEAR,
    CONNECTED_WAITING_POINT,
    POINT_READY,
    POINT_DWELL,
    INVITE_VISIBLE,
    CONFIRM_DWELL,
    DISCUSSION_STARTED
}

/** A raw researcher input and its debounced confirmation are intentionally separate. */
data class SpatialProximityState(
    val rawNear: Boolean = false,
    val confirmedNear: Boolean = false,
    val changeStartedAtMillis: Long? = null
)

data class SpatialConnectionState(
    val scenarioId: String,
    val config: SpatialConnectionConfig = SpatialConnectionConfig(),
    val roundId: Long = 1L,
    val stage: SpatialConnectionStage = SpatialConnectionStage.LOCKED,
    val lastNowMillis: Long = 0L,
    val b: SpatialProximityState = SpatialProximityState(),
    val c: SpatialProximityState = SpatialProximityState(),
    val activeTarget: SpatialParticipant? = null,
    val spatialStageEntered: Boolean = false,
    val discussionStarted: Boolean = false,
    val connectedAtMillis: Long? = null,
    val pointHovered: Boolean = false,
    val confirmHovered: Boolean = false,
    val dwellStartedAtMillis: Long? = null,
    val dwellElapsedMillis: Long = 0L,
    val inviteShownAtMillis: Long? = null,
    val cooldownStartedAtMillis: Long? = null
) {
    fun proximity(participant: SpatialParticipant): SpatialProximityState =
        if (participant == SpatialParticipant.B) b else c

    val connectionVisible: Boolean
        get() = activeTarget != null

    val pointVisible: Boolean
        get() =
            connectionVisible &&
                stage in
                    setOf(
                        SpatialConnectionStage.POINT_READY,
                        SpatialConnectionStage.POINT_DWELL,
                        SpatialConnectionStage.INVITE_VISIBLE,
                        SpatialConnectionStage.CONFIRM_DWELL
                    )

    val inviteVisible: Boolean
        get() =
            stage == SpatialConnectionStage.INVITE_VISIBLE ||
                stage == SpatialConnectionStage.CONFIRM_DWELL

    val dwellProgress: Float
        get() {
            val duration =
                when (stage) {
                    SpatialConnectionStage.POINT_DWELL -> config.pointDwellMillis
                    SpatialConnectionStage.CONFIRM_DWELL -> config.confirmDwellMillis
                    else -> return 0f
                }
            return (dwellElapsedMillis.toFloat() / duration).coerceIn(0f, 1f)
        }

    val canSimulatePointDwell: Boolean
        get() =
            stage == SpatialConnectionStage.POINT_DWELL ||
                (stage == SpatialConnectionStage.POINT_READY &&
                    cooldownStartedAtMillis == null &&
                    !pointHovered)

    val needsTick: Boolean
        get() =
            stage != SpatialConnectionStage.LOCKED &&
                (b.changeStartedAtMillis != null ||
                    c.changeStartedAtMillis != null ||
                    stage == SpatialConnectionStage.CONNECTED_WAITING_POINT ||
                    stage == SpatialConnectionStage.POINT_DWELL ||
                    inviteVisible ||
                    cooldownStartedAtMillis != null)
}

enum class SpatialEventSource {
    RESEARCHER_SIMULATION,
    LOCAL_HOVER,
    TIMER
}

enum class SpatialRecordType {
    MATERIAL_VIEW_COMPLETED,
    RAW_PROXIMITY_CHANGED,
    NEAR_STARTED,
    NEAR_CANCELLED,
    NEAR_CONFIRMED,
    FAR_STARTED,
    FAR_CANCELLED,
    FAR_CONFIRMED,
    CONNECTION_SHOWN,
    CONNECTION_HIDDEN,
    CONNECTION_SWITCHED,
    POINT_SHOWN,
    POINT_DWELL_STARTED,
    POINT_DWELL_CANCELLED,
    POINT_DWELL_COMPLETED,
    INVITE_SHOWN,
    INVITE_TIMED_OUT,
    CONFIRM_DWELL_STARTED,
    CONFIRM_DWELL_CANCELLED,
    CONFIRM_COMPLETED,
    DISCUSSION_MARKED,
    RESET
}

data class SpatialConnectionRecord(
    val roundId: Long,
    val scenarioId: String,
    val timestampMillis: Long,
    val type: SpatialRecordType,
    val participant: SpatialParticipant?,
    val source: SpatialEventSource,
    val reason: String
)

/** Every event carries a monotonic timestamp; the reducer never obtains time itself. */
sealed interface SpatialConnectionEvent {
    val nowMillis: Long

    data class CompleteMaterialView(override val nowMillis: Long) : SpatialConnectionEvent

    data class SetNear(
        val participant: SpatialParticipant,
        val near: Boolean,
        override val nowMillis: Long
    ) : SpatialConnectionEvent

    data class PointHoverChanged(val hovered: Boolean, override val nowMillis: Long) :
        SpatialConnectionEvent

    data class ConfirmHoverChanged(val hovered: Boolean, override val nowMillis: Long) :
        SpatialConnectionEvent

    /** Explicitly simulated completed dwell, not a click on A's Point. */
    data class SimulatePointDwell(override val nowMillis: Long) : SpatialConnectionEvent

    data class SimulateConfirmDwell(override val nowMillis: Long) : SpatialConnectionEvent

    data class MarkDiscussionStarted(override val nowMillis: Long) : SpatialConnectionEvent

    data class Tick(override val nowMillis: Long) : SpatialConnectionEvent

    data class Reset(override val nowMillis: Long) : SpatialConnectionEvent
}

data class SpatialConnectionTransition(
    val state: SpatialConnectionState,
    val records: List<SpatialConnectionRecord> = emptyList()
)

object SpatialConnectionStateMachine {
    fun initial(
        scenarioId: String = "spatial-connection-preview",
        config: SpatialConnectionConfig = SpatialConnectionConfig()
    ): SpatialConnectionState = SpatialConnectionState(scenarioId = scenarioId, config = config)

    fun reduce(
        state: SpatialConnectionState,
        event: SpatialConnectionEvent
    ): SpatialConnectionTransition {
        // Stale input cannot move a timer backwards, change raw inputs or jump stages.
        // Reset remains usable from any stage, retaining the session's monotonic watermark.
        if (event.nowMillis < state.lastNowMillis && event !is SpatialConnectionEvent.Reset) {
            return SpatialConnectionTransition(state)
        }
        return Reduction(state, event.nowMillis.coerceAtLeast(state.lastNowMillis)).apply(event)
    }

    private class Reduction(initial: SpatialConnectionState, private val now: Long) {
        private var state = initial.copy(lastNowMillis = now)
        private val records = mutableListOf<SpatialConnectionRecord>()

        private fun record(
            type: SpatialRecordType,
            source: SpatialEventSource,
            reason: String,
            participant: SpatialParticipant? = state.activeTarget
        ) {
            records +=
                SpatialConnectionRecord(
                    state.roundId,
                    state.scenarioId,
                    now,
                    type,
                    participant,
                    source,
                    reason
                )
        }

        // Inputs are nonnegative monotonic times. Clamp before subtraction to avoid underflow.
        private fun elapsed(start: Long): Long = (now - start.coerceIn(0L, now)).coerceAtLeast(0L)

        fun apply(event: SpatialConnectionEvent): SpatialConnectionTransition {
            if (event is SpatialConnectionEvent.Reset) {
                cancelDwell(SpatialEventSource.RESEARCHER_SIMULATION, "round_reset")
                if (state.connectionVisible) {
                    record(
                        SpatialRecordType.CONNECTION_HIDDEN,
                        SpatialEventSource.RESEARCHER_SIMULATION,
                        "round_reset"
                    )
                }
                state =
                    initial(state.scenarioId, state.config)
                        .copy(roundId = state.roundId + 1L, lastNowMillis = now)
                record(
                    SpatialRecordType.RESET,
                    SpatialEventSource.RESEARCHER_SIMULATION,
                    "new_round",
                    null
                )
                return SpatialConnectionTransition(state, records.toList())
            }

            // Settle timers before consuming current input. Confirmation dwell suppresses timeout.
            advance()
            when (event) {
                is SpatialConnectionEvent.CompleteMaterialView -> unlock()
                is SpatialConnectionEvent.SetNear -> setNear(event)
                is SpatialConnectionEvent.PointHoverChanged -> pointHover(event.hovered)
                is SpatialConnectionEvent.ConfirmHoverChanged -> confirmHover(event.hovered)
                is SpatialConnectionEvent.SimulatePointDwell -> {
                    if (state.canSimulatePointDwell) {
                        if (state.stage != SpatialConnectionStage.POINT_DWELL) {
                            record(
                                SpatialRecordType.POINT_DWELL_STARTED,
                                SpatialEventSource.RESEARCHER_SIMULATION,
                                "simulated_continuous_dwell"
                            )
                        }
                        completePoint(SpatialEventSource.RESEARCHER_SIMULATION)
                    }
                }
                is SpatialConnectionEvent.SimulateConfirmDwell -> {
                    if (state.inviteVisible) {
                        if (state.stage != SpatialConnectionStage.CONFIRM_DWELL) {
                            record(
                                SpatialRecordType.CONFIRM_DWELL_STARTED,
                                SpatialEventSource.RESEARCHER_SIMULATION,
                                "simulated_continuous_dwell"
                            )
                        }
                        completeConfirmation(SpatialEventSource.RESEARCHER_SIMULATION)
                    }
                }
                is SpatialConnectionEvent.MarkDiscussionStarted -> {
                    if (state.connectionVisible && !state.discussionStarted) {
                        cancelDwell(SpatialEventSource.RESEARCHER_SIMULATION, "discussion_marked")
                        record(
                            SpatialRecordType.DISCUSSION_MARKED,
                            SpatialEventSource.RESEARCHER_SIMULATION,
                            "researcher_marked_actual_discussion"
                        )
                        discussion()
                    }
                }
                is SpatialConnectionEvent.Tick -> Unit
                is SpatialConnectionEvent.Reset -> Unit
            }
            // An interrupted confirmation restores the ORIGINAL card deadline, not a fresh 6s.
            expireInvite()
            return SpatialConnectionTransition(state, records.toList())
        }

        private fun unlock() {
            if (state.stage != SpatialConnectionStage.LOCKED) return
            state =
                state.copy(
                    stage = SpatialConnectionStage.WAITING_FOR_NEAR,
                    b = state.b.copy(changeStartedAtMillis = if (state.b.rawNear) now else null),
                    c = state.c.copy(changeStartedAtMillis = if (state.c.rawNear) now else null)
                )
            record(
                SpatialRecordType.MATERIAL_VIEW_COMPLETED,
                SpatialEventSource.RESEARCHER_SIMULATION,
                "distance_simulation_unlocked",
                null
            )
            SpatialParticipant.entries.forEach { participant ->
                if (state.proximity(participant).rawNear) {
                    record(
                        SpatialRecordType.NEAR_STARTED,
                        SpatialEventSource.RESEARCHER_SIMULATION,
                        "fresh_timer_after_unlock",
                        participant
                    )
                }
            }
        }

        private fun updateProximity(participant: SpatialParticipant, value: SpatialProximityState) {
            state =
                if (participant == SpatialParticipant.B) state.copy(b = value)
                else state.copy(c = value)
        }

        private fun setNear(event: SpatialConnectionEvent.SetNear) {
            val old = state.proximity(event.participant)
            if (old.rawNear == event.near) return
            record(
                SpatialRecordType.RAW_PROXIMITY_CHANGED,
                SpatialEventSource.RESEARCHER_SIMULATION,
                if (event.near) "raw_near" else "raw_far",
                event.participant
            )
            val unlocked = state.stage != SpatialConnectionStage.LOCKED
            if (unlocked && old.changeStartedAtMillis != null) {
                record(
                    if (old.rawNear) SpatialRecordType.NEAR_CANCELLED
                    else SpatialRecordType.FAR_CANCELLED,
                    SpatialEventSource.RESEARCHER_SIMULATION,
                    "raw_input_reversed",
                    event.participant
                )
            }
            val pending = unlocked && event.near != old.confirmedNear
            updateProximity(
                event.participant,
                old.copy(rawNear = event.near, changeStartedAtMillis = if (pending) now else null)
            )
            if (pending) {
                record(
                    if (event.near) SpatialRecordType.NEAR_STARTED
                    else SpatialRecordType.FAR_STARTED,
                    SpatialEventSource.RESEARCHER_SIMULATION,
                    "debounce_started",
                    event.participant
                )
            }
        }

        private fun advance() {
            if (state.stage == SpatialConnectionStage.LOCKED) return
            // Confirm BOTH inputs before selecting a target: simultaneous approach always picks B.
            SpatialParticipant.entries.forEach { participant ->
                val input = state.proximity(participant)
                val start = input.changeStartedAtMillis
                val duration =
                    if (input.rawNear) state.config.nearEnterMillis else state.config.farExitMillis
                if (start != null && elapsed(start) >= duration) {
                    updateProximity(
                        participant,
                        input.copy(confirmedNear = input.rawNear, changeStartedAtMillis = null)
                    )
                    record(
                        if (input.rawNear) SpatialRecordType.NEAR_CONFIRMED
                        else SpatialRecordType.FAR_CONFIRMED,
                        SpatialEventSource.TIMER,
                        "sustained_raw_input",
                        participant
                    )
                }
            }
            val active = state.activeTarget
            if (active == null || !state.proximity(active).confirmedNear) {
                val next =
                    SpatialParticipant.entries.firstOrNull { state.proximity(it).confirmedNear }
                if (next != active) changeTarget(next)
            }
            when (state.stage) {
                SpatialConnectionStage.CONNECTED_WAITING_POINT -> {
                    val start = state.connectedAtMillis
                    if (start != null && elapsed(start) >= state.config.pointDelayMillis) {
                        state = state.copy(stage = SpatialConnectionStage.POINT_READY)
                        record(
                            SpatialRecordType.POINT_SHOWN,
                            SpatialEventSource.TIMER,
                            "connection_delay_completed"
                        )
                    }
                }
                SpatialConnectionStage.POINT_DWELL,
                SpatialConnectionStage.CONFIRM_DWELL -> {
                    val start = state.dwellStartedAtMillis
                    if (start != null) {
                        val point = state.stage == SpatialConnectionStage.POINT_DWELL
                        val duration =
                            if (point) state.config.pointDwellMillis
                            else state.config.confirmDwellMillis
                        state =
                            state.copy(dwellElapsedMillis = elapsed(start).coerceAtMost(duration))
                        if (state.dwellElapsedMillis >= duration) {
                            if (point) completePoint(SpatialEventSource.LOCAL_HOVER)
                            else completeConfirmation(SpatialEventSource.LOCAL_HOVER)
                        }
                    }
                }
                else -> Unit
            }
            val cooldown = state.cooldownStartedAtMillis
            if (cooldown != null && elapsed(cooldown) >= state.config.cooldownMillis) {
                // Do not synthesize an enter. A hover begun during cooldown stays unarmed.
                state = state.copy(cooldownStartedAtMillis = null)
            }
            expireInvite()
        }

        private fun cancelDwell(source: SpatialEventSource, reason: String) {
            when (state.stage) {
                SpatialConnectionStage.POINT_DWELL ->
                    record(SpatialRecordType.POINT_DWELL_CANCELLED, source, reason)
                SpatialConnectionStage.CONFIRM_DWELL ->
                    record(SpatialRecordType.CONFIRM_DWELL_CANCELLED, source, reason)
                else -> Unit
            }
        }

        private fun changeTarget(next: SpatialParticipant?) {
            val previous = state.activeTarget
            cancelDwell(SpatialEventSource.TIMER, "active_target_left")
            if (previous != null) {
                record(
                    SpatialRecordType.CONNECTION_HIDDEN,
                    SpatialEventSource.TIMER,
                    "active_target_far_confirmed",
                    previous
                )
            }
            state =
                state.copy(
                    activeTarget = next,
                    stage =
                        when {
                            next == null -> SpatialConnectionStage.WAITING_FOR_NEAR
                            state.discussionStarted -> SpatialConnectionStage.DISCUSSION_STARTED
                            else -> SpatialConnectionStage.CONNECTED_WAITING_POINT
                        },
                    spatialStageEntered = state.spatialStageEntered || next != null,
                    connectedAtMillis = if (next != null && !state.discussionStarted) now else null,
                    pointHovered = false,
                    confirmHovered = false,
                    dwellStartedAtMillis = null,
                    dwellElapsedMillis = 0L,
                    inviteShownAtMillis = null,
                    cooldownStartedAtMillis = null
                )
            if (next != null) {
                if (previous != null) {
                    record(
                        SpatialRecordType.CONNECTION_SWITCHED,
                        SpatialEventSource.TIMER,
                        "${previous.name}_to_${next.name}"
                    )
                }
                record(
                    SpatialRecordType.CONNECTION_SHOWN,
                    SpatialEventSource.TIMER,
                    "confirmed_near_simulated_2d_connection"
                )
            }
        }

        private fun pointHover(hovered: Boolean) {
            if (!state.pointVisible || hovered == state.pointHovered) return
            state = state.copy(pointHovered = hovered)
            if (!hovered && state.stage == SpatialConnectionStage.POINT_DWELL) {
                cancelDwell(SpatialEventSource.LOCAL_HOVER, "point_hover_exit")
                state =
                    state.copy(
                        stage = SpatialConnectionStage.POINT_READY,
                        dwellStartedAtMillis = null,
                        dwellElapsedMillis = 0L
                    )
            } else if (
                hovered &&
                    state.stage == SpatialConnectionStage.POINT_READY &&
                    state.cooldownStartedAtMillis == null
            ) {
                state =
                    state.copy(
                        stage = SpatialConnectionStage.POINT_DWELL,
                        dwellStartedAtMillis = now,
                        dwellElapsedMillis = 0L
                    )
                record(
                    SpatialRecordType.POINT_DWELL_STARTED,
                    SpatialEventSource.LOCAL_HOVER,
                    "fresh_point_hover_enter"
                )
            }
        }

        private fun completePoint(source: SpatialEventSource) {
            record(
                SpatialRecordType.POINT_DWELL_COMPLETED,
                source,
                "continuous_dwell_${state.config.pointDwellMillis}ms"
            )
            state =
                state.copy(
                    stage = SpatialConnectionStage.INVITE_VISIBLE,
                    dwellStartedAtMillis = null,
                    dwellElapsedMillis = 0L,
                    inviteShownAtMillis = now,
                    confirmHovered = false
                )
            record(SpatialRecordType.INVITE_SHOWN, source, "point_dwell_completed")
        }

        private fun confirmHover(hovered: Boolean) {
            if (!state.inviteVisible || hovered == state.confirmHovered) return
            state = state.copy(confirmHovered = hovered)
            if (hovered && state.stage == SpatialConnectionStage.INVITE_VISIBLE) {
                state =
                    state.copy(
                        stage = SpatialConnectionStage.CONFIRM_DWELL,
                        dwellStartedAtMillis = now,
                        dwellElapsedMillis = 0L
                    )
                record(
                    SpatialRecordType.CONFIRM_DWELL_STARTED,
                    SpatialEventSource.LOCAL_HOVER,
                    "confirm_hover_enter_timeout_suspended"
                )
            } else if (!hovered && state.stage == SpatialConnectionStage.CONFIRM_DWELL) {
                cancelDwell(
                    SpatialEventSource.LOCAL_HOVER,
                    "confirm_hover_exit_original_deadline_restored"
                )
                state =
                    state.copy(
                        stage = SpatialConnectionStage.INVITE_VISIBLE,
                        dwellStartedAtMillis = null,
                        dwellElapsedMillis = 0L
                    )
            }
        }

        private fun completeConfirmation(source: SpatialEventSource) {
            record(
                SpatialRecordType.CONFIRM_COMPLETED,
                source,
                "continuous_dwell_${state.config.confirmDwellMillis}ms"
            )
            discussion()
        }

        private fun discussion() {
            // Round-wide: leaving or switching participants must not re-arm an invitation.
            state =
                state.copy(
                    stage = SpatialConnectionStage.DISCUSSION_STARTED,
                    discussionStarted = true,
                    connectedAtMillis = null,
                    pointHovered = false,
                    confirmHovered = false,
                    dwellStartedAtMillis = null,
                    dwellElapsedMillis = 0L,
                    inviteShownAtMillis = null,
                    cooldownStartedAtMillis = null
                )
        }

        private fun expireInvite() {
            val start = state.inviteShownAtMillis
            if (
                state.stage == SpatialConnectionStage.INVITE_VISIBLE &&
                    start != null &&
                    elapsed(start) >= state.config.inviteVisibleMillis
            ) {
                record(
                    SpatialRecordType.INVITE_TIMED_OUT,
                    SpatialEventSource.TIMER,
                    "unconfirmed_card_cooldown_requires_new_enter"
                )
                state =
                    state.copy(
                        stage = SpatialConnectionStage.POINT_READY,
                        inviteShownAtMillis = null,
                        cooldownStartedAtMillis = now,
                        confirmHovered = false,
                        dwellStartedAtMillis = null,
                        dwellElapsedMillis = 0L
                    )
            }
        }
    }
}
