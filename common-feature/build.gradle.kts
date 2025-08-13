import project.convention.logic.config.LibraryModule

plugins {
    id("project.android.feature")
}

android {
    namespace = "lv.lvrtc.commonfeature"
}

moduleConfig {
    module = LibraryModule.CommonFeature
}

dependencies {
    implementation(libs.gson)
    implementation(libs.zxing)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.androidx.browser)
    implementation(project(LibraryModule.NetworkLogic.path))
}