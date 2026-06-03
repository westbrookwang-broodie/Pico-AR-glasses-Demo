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
package com.pico.spatial.sample.animation

import androidx.compose.foundation.layout.Box
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pico.spatial.sample.animation.ui.demo.BoardAScreen
import com.pico.spatial.sample.animation.ui.demo.BoardBScreen
import com.pico.spatial.sample.animation.ui.demo.HubScreen
import com.pico.spatial.sample.animation.ui.demo.Routes
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.foundation.dsl.DefaultWindowContainer
import com.pico.spatial.ui.foundation.dsl.SpatialAppScope

fun mainApp(scope: SpatialAppScope) =
    with(scope) {
        DefaultWindowContainer {
            PicoTheme {
                Box {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = Routes.HUB) {
                        // 协作板：点便签 → 资料板 A
                        composable(Routes.HUB) {
                            HubScreen(onOpenBoard = { navController.navigate(Routes.BOARD_A) })
                        }
                        // 资料板 A：继续学习 → 资料板 B；✕ → 返回协作板
                        composable(Routes.BOARD_A) {
                            BoardAScreen(
                                onNext = { navController.navigate(Routes.BOARD_B) },
                                onClose = { navController.popBackStack(Routes.HUB, false) }
                            )
                        }
                        // 资料板 B：✕ → 返回协作板
                        composable(Routes.BOARD_B) {
                            BoardBScreen(
                                onClose = { navController.popBackStack(Routes.HUB, false) }
                            )
                        }
                    }
                }
            }
        }
    }
