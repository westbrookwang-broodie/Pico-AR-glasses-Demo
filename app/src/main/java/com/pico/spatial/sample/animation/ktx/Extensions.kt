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
package com.pico.spatial.sample.animation.ktx

import com.pico.spatial.core.ecs.animation.EaseType
import java.util.Locale
import kotlin.reflect.KClass

fun String.asTitle(): String =
    split('_').joinToString(" ") { word ->
        word.lowercase(Locale.ROOT).replaceFirstChar { it.titlecase(Locale.ROOT) }
    }

fun EaseType.toDisplayName(): String =
    when (this) {
        EaseType.LINEAR -> "linear"
        EaseType.EASE_IN -> "ease-in"
        EaseType.EASE_OUT -> "ease-out"
        EaseType.EASE_INOUT -> "ease-in-out"
        EaseType.EASE_IN_CUBIC -> "ease-in-cubic"
        EaseType.EASE_OUT_CUBIC -> "ease-out-cubic"
        EaseType.EASE_INOUT_CUBIC -> "ease-in-out-cubic"
        else -> throw IllegalArgumentException("Unknown ease type: $this")
    }

class EnumValuesCache {
    // Thread-safe cache if accessed from multiple threads
    private val cache = mutableMapOf<KClass<out Enum<*>>, Array<out Enum<*>>>()

    fun <T : Enum<T>> getValues(enumClass: KClass<T>): Array<T> {
        @Suppress("UNCHECKED_CAST")
        return cache.getOrPut(enumClass) {
            enumClass.java.enumConstants
                ?: error("No enum constants found for ${enumClass.simpleName}")
        } as Array<T>
    }

    companion object {
        /** Singleton instance for global reuse */
        val instance: EnumValuesCache by lazy { EnumValuesCache() }
    }
}

inline fun <reified T : Enum<T>> cachedEnumValues(): Array<T> {
    return EnumValuesCache.instance.getValues(T::class)
}
