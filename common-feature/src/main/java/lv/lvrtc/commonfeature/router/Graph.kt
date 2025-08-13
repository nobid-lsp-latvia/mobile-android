// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.commonfeature.router

import android.app.Activity
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import lv.lvrtc.commonfeature.BuildConfig
import lv.lvrtc.commonfeature.features.biometric.BiometricScreen
import lv.lvrtc.commonfeature.features.biometric.BiometricUiConfig
import lv.lvrtc.commonfeature.features.qr_scan.QrScanScreen
import lv.lvrtc.commonfeature.features.qr_scan.QrScanUiConfig
import lv.lvrtc.commonfeature.features.security.SecurityErrorScreen
import lv.lvrtc.commonfeature.features.success.SuccessScreen
import lv.lvrtc.commonfeature.features.success.SuccessUIConfig
import lv.lvrtc.uilogic.navigation.CommonScreens
import lv.lvrtc.uilogic.navigation.ModuleRoute
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf


fun NavGraphBuilder.featureCommonGraph(navController: NavController) {
    navigation(
        startDestination = CommonScreens.Biometric.screenRoute,
        route = ModuleRoute.CommonModule.route
    ) {
        composable(
            route = CommonScreens.Biometric.screenRoute,
            deepLinks = listOf(
                navDeepLink {
                    uriPattern =
                        BuildConfig.DEEPLINK + CommonScreens.Biometric.screenRoute
                }
            ),
            arguments = listOf(
                navArgument(BiometricUiConfig.serializedKeyName) {
                    type = NavType.StringType
                }
            )
        ) {
            BiometricScreen(
                navController,
                getViewModel(
                    parameters = {
                        parametersOf(
                            it.arguments?.getString(BiometricUiConfig.serializedKeyName).orEmpty()
                        )
                    }
                )
            )
        }

        composable(
            route = CommonScreens.Success.screenRoute,
            deepLinks = listOf(
                navDeepLink {
                    uriPattern =
                        BuildConfig.DEEPLINK + CommonScreens.Success.screenRoute
                }
            ),
            arguments = listOf(
                navArgument(SuccessUIConfig.serializedKeyName) {
                    type = NavType.StringType
                }
            )
        ) {
            SuccessScreen(
                navController,
                getViewModel(
                    parameters = {
                        parametersOf(
                            it.arguments?.getString(SuccessUIConfig.serializedKeyName).orEmpty()
                        )
                    }
                )
            )
        }

        composable(
            route = CommonScreens.QrScan.screenRoute,
            deepLinks = listOf(
                navDeepLink {
                    uriPattern =
                        BuildConfig.DEEPLINK + CommonScreens.QrScan.screenRoute
                }
            ),
            arguments = listOf(
                navArgument(QrScanUiConfig.serializedKeyName) {
                    type = NavType.StringType
                }
            )
        ) {
            QrScanScreen(
                navController,
                getViewModel(
                    parameters = {
                        parametersOf(
                            it.arguments?.getString(QrScanUiConfig.serializedKeyName).orEmpty()
                        )
                    }
                )
            )
        }

        composable(
            route = CommonScreens.SecurityError.screenRoute,
            arguments = listOf(
                navArgument("reason") {
                    type = NavType.StringType
                }
            )
        ) {
            SecurityErrorScreen(
                reason = it.arguments?.getString("reason") ?: "",
                onExit = { (navController.context as? Activity)?.finish() }
            )
        }
    }
}