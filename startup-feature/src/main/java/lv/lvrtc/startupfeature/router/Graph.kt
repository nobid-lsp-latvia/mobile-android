// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.startupfeature.router

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navDeepLink
import lv.lvrtc.startupfeature.BuildConfig
import lv.lvrtc.uilogic.navigation.ModuleRoute
import lv.lvrtc.startupfeature.ui.SplashScreen
import lv.lvrtc.uilogic.navigation.StartupScreens
import org.koin.androidx.compose.koinViewModel

fun NavGraphBuilder.featureStartupGraph(navController: NavController) {
    navigation(
        startDestination = StartupScreens.Splash.screenRoute,
        route = ModuleRoute.StartupModule.route
    ) {
        composable(
            route = StartupScreens.Splash.screenRoute,
            deepLinks = listOf(
                navDeepLink {
                    uriPattern =
                        BuildConfig.DEEPLINK + StartupScreens.Splash.screenRoute
                }
            )
        ) {
            SplashScreen(navController, koinViewModel())
        }
    }
}