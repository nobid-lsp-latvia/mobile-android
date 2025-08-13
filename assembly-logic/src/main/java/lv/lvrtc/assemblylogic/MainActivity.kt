// SPDX-License-Identifier: EUPL-1.2

package lv.lvrtc.assemblylogic

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import lv.lvrtc.businesslogic.controller.NetworkStatusController
import lv.lvrtc.commonfeature.router.featureCommonGraph
import lv.lvrtc.startupfeature.router.featureStartupGraph
import lv.lvrtc.startupfeature.ui.SplashViewModel
import lv.lvrtc.uilogic.NobidComponentActivity
import lv.lvrtc.webfeature.router.featureWebGraph
import lv.lvrtc.signfeature.util.FilePickerHelper
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : NobidComponentActivity() {
    private val networkStatusController: NetworkStatusController by inject()
    private val viewModel: SplashViewModel by viewModel()
    private val filePickerHelper: FilePickerHelper by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            filePickerHelper.handlePickedFiles(uris)
        }
        filePickerHelper.registerPicker(filePickerLauncher)
        
        networkStatusController.startMonitoring(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            splashScreen.setKeepOnScreenCondition { !viewModel.isInitialized }
        }

        enableEdgeToEdge()
        setContent {
            Content(intent) {
                featureStartupGraph(it)
                featureCommonGraph(it)
                featureWebGraph(it)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        networkStatusController.stopMonitoring()
    }
}