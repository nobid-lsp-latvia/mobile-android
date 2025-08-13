import project.convention.logic.config.LibraryModule

plugins {
    id("project.android.library")
    id("project.android.library.compose")
}

android {
    namespace = "lv.lvrtc.webviewfeature"
}

moduleConfig {
    module = LibraryModule.WebBridge
}

dependencies {
    implementation(libs.androidx.webkit)
    implementation(libs.gson)
    implementation(project(":network-logic"))
}