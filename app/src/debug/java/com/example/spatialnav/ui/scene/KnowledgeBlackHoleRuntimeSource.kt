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

/** Debug-only demo; these values do not come from OCR, K/P or physical-distance producers. */
object KnowledgeBlackHoleRuntimeSource {
    val state: KnowledgeBlackHoleSourceState =
        KnowledgeBlackHoleSourceState.Ready(
            KnowledgeBlackHoleInput(
                currentMaterial = KnowledgeMaterialId.I,
                kPairValid = true,
                pUpgraded = false,
                distanceBrightnessMultiplier = 1f
            )
        )
}
