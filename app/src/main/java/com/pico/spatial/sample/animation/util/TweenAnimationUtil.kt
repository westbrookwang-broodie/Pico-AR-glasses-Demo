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
package com.pico.spatial.sample.animation.util

import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.ModelComponent
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.animation.AnimationBindTarget
import com.pico.spatial.core.ecs.animation.MaterialTarget
import com.pico.spatial.core.ecs.animation.TweenAnimation
import com.pico.spatial.core.ecs.resource.AnimationResource
import com.pico.spatial.core.ecs.resource.BlendingMode
import com.pico.spatial.core.ecs.resource.PhysicallyBasedMaterial
import com.pico.spatial.core.math.Color4
import com.pico.spatial.core.math.EulerAngles
import com.pico.spatial.core.math.Transform
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.sample.animation.data.MaterialProperties
import com.pico.spatial.sample.animation.data.TweenAnimationControl
import com.pico.spatial.sample.animation.data.TweenAnimationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * TweenAnimationUtil provides stateless tween animation logic for entities. It handles loading and
 * tween playback for provided entities.
 */
object TweenAnimationUtil {
    private const val STATIC_ROBOT = "asset://pico_robot_static.usdz"
    private val INITIAL_POSITION_STATIC_ROBOT = Vector3(-0.07f, -0.2f, 0.1f)
    private val INITIAL_ROTATOR_STATIC_ROBOT = EulerAngles(0f, 30f, 0f)
    private val INITIAL_SCALE_STATIC_ROBOT = Vector3(0.45f)

    // Name of the body entity in the static robot model. This entity contains the ModelComponent
    // with material properties that are animated (base color, opacity, metallic, roughness,
    // emissive).
    // Refer to ./github/static_robot_structure.png for a visual diagram of the static robot model's
    // hierarchy.
    private const val NODE_BODY = "geo_body"

    private val POSITION_TO = Vector3(0.1f, -0.12f, 0.21f)
    private val ROTATION_FROM = EulerAngles(0f, 0.0f, 0.0f)
    private val ROTATION_TO = EulerAngles(0f, 100.0f, 0.0f)
    private val SCALE_BY = Vector3(-0.3f)
    private val TRANSFORM_FROM =
        Transform(Vector3(-0.22f, -0.26f, 0.1f), EulerAngles(0f, 60f, 0f), Vector3(0.2f))
    private val TRANSFORM_TO =
        Transform(Vector3(-0.12f, 0.07f, 0.18f), EulerAngles(0f, 0f, -90f), Vector3(0.5f))
    private val BASE_COLOR_TO = Color4(0.8f, 0.6f, 0.95f, 1f)
    private const val OPACITY_TO = 0.1f
    private const val METALLIC_TO = 0.0f
    private const val ROUGHNESS_TO = 0.0f
    private val EMISSIVE_TO = Color4(1.0f, 1.0f, 0.0f, 1f)

    data class TweenAnimationData(
        var tweenAnimationResource: AnimationResource? = null,
        var body: Entity = Entity(),
        var pbrMaterial: PhysicallyBasedMaterial? = null,
        var initMaterial: MaterialProperties? = null
    )

    fun initialize(
        entity: Entity,
        scope: CoroutineScope,
        onInitialized: (TweenAnimationData) -> Unit
    ) {
        scope.launch {
            val character = withContext(Dispatchers.IO) { Entity.load(STATIC_ROBOT) }
            entity.addChild(character)
            entity.components[TransformComponent::class.java]?.apply {
                setPosition(INITIAL_POSITION_STATIC_ROBOT)
                setEulerAngles(INITIAL_ROTATOR_STATIC_ROBOT)
                setScaleVector(INITIAL_SCALE_STATIC_ROBOT)
            }

            val bodyEntity =
                requireNotNull(character.findEntity(NODE_BODY)) {
                    "Body entity named $NODE_BODY not found"
                }

            val state = TweenAnimationData(body = bodyEntity)
            recordInitMaterial(state, state.body)
            onInitialized(state)
        }
    }

    private fun recordInitMaterial(state: TweenAnimationData, entity: Entity) {
        val material =
            requireNotNull(
                entity.components[ModelComponent::class.java]?.materials?.firstOrNull()
                    as? PhysicallyBasedMaterial
            ) {
                "Material not found for entity $entity or not a PhysicallyBasedMaterial"
            }
        state.pbrMaterial = material
        state.initMaterial =
            MaterialProperties(
                baseColor = material.getBaseColor(),
                metallic = material.getMetallic(),
                roughness = material.getRoughness(),
                emissiveColor = material.getEmissiveColor(),
                opacity = material.getOpacity()
            )
    }

    fun play(
        entity: Entity,
        type: TweenAnimationState,
        control: TweenAnimationControl,
        animationData: TweenAnimationData
    ) {
        reset(entity, animationData)

        var isMaterialAnimation = false
        var isOpacityAnimation = false

        val tweenAnimation =
            when (type) {
                TweenAnimationState.POSITION ->
                    TweenAnimation.createTweenAnimation(
                        bindTarget = AnimationBindTarget.bindPosition(),
                        to = POSITION_TO,
                        duration = control.duration,
                        speed = control.speed,
                        repeatCount = control.repeatCount,
                        repeatMode = control.repeatMode,
                        easeType = control.easeType,
                    )
                TweenAnimationState.ROTATION ->
                    TweenAnimation.createTweenAnimation(
                        bindTarget = AnimationBindTarget.bindRotation(),
                        from = ROTATION_FROM,
                        to = ROTATION_TO,
                        duration = control.duration,
                        speed = control.speed,
                        repeatCount = control.repeatCount,
                        repeatMode = control.repeatMode,
                        easeType = control.easeType,
                    )
                TweenAnimationState.SCALE ->
                    TweenAnimation.createTweenAnimation(
                        bindTarget = AnimationBindTarget.bindScale(),
                        by = SCALE_BY,
                        duration = control.duration,
                        speed = control.speed,
                        repeatCount = control.repeatCount,
                        repeatMode = control.repeatMode,
                        easeType = control.easeType,
                    )
                TweenAnimationState.TRANSFORM ->
                    TweenAnimation.createTweenAnimation(
                        bindTarget = AnimationBindTarget.bindTransform(),
                        from = TRANSFORM_FROM,
                        to = TRANSFORM_TO,
                        duration = control.duration,
                        speed = control.speed,
                        repeatCount = control.repeatCount,
                        repeatMode = control.repeatMode,
                        easeType = control.easeType,
                    )
                TweenAnimationState.BASE_COLOR ->
                    TweenAnimation.createTweenAnimation(
                            bindTarget =
                                AnimationBindTarget.bindMaterial(0, MaterialTarget.BASE_COLOR),
                            from = animationData.initMaterial?.baseColor,
                            to = BASE_COLOR_TO,
                            duration = control.duration,
                            speed = control.speed,
                            repeatCount = control.repeatCount,
                            repeatMode = control.repeatMode,
                            easeType = control.easeType,
                        )
                        .also { isMaterialAnimation = true }
                TweenAnimationState.OPACITY ->
                    TweenAnimation.createTweenAnimation(
                            bindTarget =
                                AnimationBindTarget.bindMaterial(0, MaterialTarget.OPACITY),
                            from = animationData.initMaterial?.opacity,
                            to = OPACITY_TO,
                            duration = control.duration,
                            speed = control.speed,
                            repeatCount = control.repeatCount,
                            repeatMode = control.repeatMode,
                            easeType = control.easeType,
                        )
                        .also {
                            isMaterialAnimation = true
                            isOpacityAnimation = true
                        }
                TweenAnimationState.METALLIC ->
                    TweenAnimation.createTweenAnimation(
                            bindTarget =
                                AnimationBindTarget.bindMaterial(0, MaterialTarget.METALLIC),
                            from = animationData.initMaterial?.metallic,
                            to = METALLIC_TO,
                            duration = control.duration,
                            speed = control.speed,
                            repeatCount = control.repeatCount,
                            repeatMode = control.repeatMode,
                            easeType = control.easeType,
                        )
                        .also { isMaterialAnimation = true }
                TweenAnimationState.ROUGHNESS ->
                    TweenAnimation.createTweenAnimation(
                            bindTarget =
                                AnimationBindTarget.bindMaterial(0, MaterialTarget.ROUGHNESS),
                            from = animationData.initMaterial?.roughness,
                            to = ROUGHNESS_TO,
                            duration = control.duration,
                            speed = control.speed,
                            repeatCount = control.repeatCount,
                            repeatMode = control.repeatMode,
                            easeType = control.easeType,
                        )
                        .also { isMaterialAnimation = true }
                TweenAnimationState.EMISSIVE ->
                    TweenAnimation.createTweenAnimation(
                            bindTarget =
                                AnimationBindTarget.bindMaterial(0, MaterialTarget.EMISSIVE),
                            from = animationData.initMaterial?.emissiveColor,
                            to = EMISSIVE_TO,
                            duration = control.duration,
                            speed = control.speed,
                            repeatCount = control.repeatCount,
                            repeatMode = control.repeatMode,
                            easeType = control.easeType,
                        )
                        .also { isMaterialAnimation = true }
                else -> throw IllegalArgumentException("Invalid TweenAnimationState: $type")
            }

        val animationResource = AnimationResource.generateWithTweenAnimation(tweenAnimation)
        animationData.tweenAnimationResource = animationResource

        if (!isMaterialAnimation) {
            entity.playAnimation(animationResource)
        } else {
            if (isOpacityAnimation) {
                animationData.pbrMaterial?.setBlendingMode(BlendingMode.TRANSPARENT)
            }
            animationData.body.playAnimation(animationResource)
        }
    }

    fun reset(entity: Entity, animationData: TweenAnimationData) {
        entity.stopAllAnimations()

        entity.components[TransformComponent::class.java]?.apply {
            setPosition(INITIAL_POSITION_STATIC_ROBOT)
            setEulerAngles(INITIAL_ROTATOR_STATIC_ROBOT)
            setScaleVector(INITIAL_SCALE_STATIC_ROBOT)
        }
        animationData.pbrMaterial?.apply {
            setBaseColor(animationData.initMaterial?.baseColor ?: Color4.WHITE)
            setMetallic(animationData.initMaterial?.metallic ?: 1f)
            setRoughness(animationData.initMaterial?.roughness ?: 1f)
            setEmissiveColor(animationData.initMaterial?.emissiveColor ?: Color4.WHITE)
            setBlendingMode(BlendingMode.OPAQUE)
            setOpacity(animationData.initMaterial?.opacity ?: 1f)
        }
        animationData.tweenAnimationResource?.close()
        animationData.tweenAnimationResource = null
    }
}
