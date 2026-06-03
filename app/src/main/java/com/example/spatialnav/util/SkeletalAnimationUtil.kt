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
package com.example.spatialnav.util

import android.util.Log
import com.example.spatialnav.data.SkeletalAnimationState
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.resource.AnimationResource
import com.pico.spatial.core.math.Vector3
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * SkeletalAnimationUtil provides stateless skeletal animation logic for entities. It handles
 * loading and playback of skeletal animations for provided entities.
 */
object SkeletalAnimationUtil {
    private const val ANIMATED_ROBOT = "asset://pico_robot_animated.glb"
    private val INITIAL_POSITION_ANIMATED_ROBOT = Vector3(-0.07f, -0.2f, 0.1f)
    private val INITIAL_SCALE_ANIMATED_ROBOT = Vector3(0.0045f)

    data class SkeletalAnimationData(
        val skeletalAnimationResources: Array<AnimationResource>?,
        val skinnedMeshEntities: List<Entity>?
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SkeletalAnimationData) return false

            if (skeletalAnimationResources != null) {
                if (other.skeletalAnimationResources == null) return false
                if (!skeletalAnimationResources.contentEquals(other.skeletalAnimationResources))
                    return false
            } else if (other.skeletalAnimationResources != null) return false

            if (skinnedMeshEntities != other.skinnedMeshEntities) return false

            return true
        }

        override fun hashCode(): Int {
            var result = skeletalAnimationResources?.contentHashCode() ?: 0
            result = 31 * result + (skinnedMeshEntities?.hashCode() ?: 0)
            return result
        }
    }

    fun initialize(
        entity: Entity,
        scope: CoroutineScope,
        onInitialized: (SkeletalAnimationData) -> Unit
    ) {
        scope.launch {
            val character = withContext(Dispatchers.IO) { Entity.load(ANIMATED_ROBOT) }
            entity.addChild(character)
            character.components[TransformComponent::class.java]?.apply {
                setPosition(INITIAL_POSITION_ANIMATED_ROBOT)
                setScaleVector(INITIAL_SCALE_ANIMATED_ROBOT)
            }

            val skinnedMeshEntities = entity.findSkinnedMeshEntity().toList()
            val skeletalAnimationResources =
                skinnedMeshEntities.firstOrNull()?.getAnimationResources()

            val animationData =
                SkeletalAnimationData(skeletalAnimationResources, skinnedMeshEntities)
            onInitialized(animationData)

            play(SkeletalAnimationState.STANDBY_MODE, animationData)
        }
    }

    fun play(skeletalAnimationState: SkeletalAnimationState, animationData: SkeletalAnimationData) {
        val state = animationData
        val entities = state.skinnedMeshEntities ?: return

        if (entities.isEmpty()) {
            Log.e("SkeletalAnimation", "No Skinned Mesh Found!")
        } else {
            Log.d("SkeletalAnimation", "Found ${entities.size} Skinned Mesh!")
            for (meshEntity in entities) {
                val animationResource =
                    state.skeletalAnimationResources?.get(skeletalAnimationState.value)
                animationResource?.let {
                    meshEntity.playAnimation(it)
                    // Do not use use() here; animation resource must remain open for continuous
                    // playback
                }
            }
        }
    }

    /**
     * Closes animation resources. Should be called when animations are no longer needed (e.g.,
     * during ViewModel cleanup).
     */
    fun closeResources(animationData: SkeletalAnimationData) {
        animationData.skeletalAnimationResources?.forEach { it.close() }
    }

    fun reset(entity: Entity, animationData: SkeletalAnimationData) {
        entity.stopAllAnimations()
        // Do not close animation resources here, as they may be reused for subsequent playback
    }
}
