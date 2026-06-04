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
package com.example.spatialnav

import androidx.compose.foundation.layout.Box
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.spatialnav.ui.scene.CardADetailScreen
import com.example.spatialnav.ui.scene.CardBDetailScreen
import com.example.spatialnav.ui.scene.CardCDetailScreen
import com.example.spatialnav.ui.scene.Routes
import com.example.spatialnav.ui.scene.WorkspaceScreen
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.foundation.dsl.DefaultWindowContainer
import com.pico.spatial.ui.foundation.dsl.SpatialAppScope

/**
 * Spatial entry point for the "抬头共振" (Heads-up Resonance) scene, recreated from the demo HTML.
 *
 * Navigation:
 * - WORKSPACE: main stage (title + three knowledge cards + focus board + toolbar)
 * - CARD_A/B/C: per-card detail panels; CARD_A embeds the animated 3D robot
 * - each detail closes back to WORKSPACE
 */
fun mainApp(scope: SpatialAppScope) =
    with(scope) {
        DefaultWindowContainer {
            PicoTheme {
                Box {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = Routes.WORKSPACE) {
                        composable(Routes.WORKSPACE) {
                            WorkspaceScreen(
                                onOpenCard = { id ->
                                    when (id) {
                                        "B" -> navController.navigate(Routes.CARD_B)
                                        "C" -> navController.navigate(Routes.CARD_C)
                                        else -> navController.navigate(Routes.CARD_A)
                                    }
                                }
                            )
                        }
                        composable(Routes.CARD_A) {
                            CardADetailScreen(
                                onClose = { navController.popBackStack(Routes.WORKSPACE, false) }
                            )
                        }
                        composable(Routes.CARD_B) {
                            CardBDetailScreen(
                                onClose = { navController.popBackStack(Routes.WORKSPACE, false) }
                            )
                        }
                        composable(Routes.CARD_C) {
                            CardCDetailScreen(
                                onClose = { navController.popBackStack(Routes.WORKSPACE, false) }
                            )
                        }
                    }
                }
            }
        }
    }
