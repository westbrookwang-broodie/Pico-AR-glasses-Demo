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
package com.pico.spatial.sample.animation.manager

import android.util.Log
import com.pico.spatial.core.container.SpatialViewContent
import com.pico.spatial.core.ecs.event.AnimationEvents
import com.pico.spatial.core.ecs.event.Event
import com.pico.spatial.core.lifecycle.Cancellable

object EventManager {
    private var subscription: Cancellable? = null

    fun <T : Event> subscribeAnimationEvent(content: SpatialViewContent, animEvent: Class<T>) {
        when (animEvent) {
            AnimationEvents.Started::class.java -> {
                subscription =
                    content.subscribe(animEvent) {
                        Log.d("EventManager", "Animation Started!")
                        // Implement your logic here
                    }
            }
            AnimationEvents.Terminated::class.java -> {
                subscription =
                    content.subscribe(animEvent) {
                        Log.d("EventManager", "Animation Terminated!")
                        // Implement your logic here
                    }
            }
            else -> {
                Log.e("EventManager", "No Matching Animation Event Found!")
            }
        }
    }

    fun unsubscribeAllAnimationEvents() {
        subscription?.cancel()
    }
}
