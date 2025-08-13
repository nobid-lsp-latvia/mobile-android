import project.convention.logic.config.LibraryModule

plugins {
    id("project.android.library")
}

android {
    namespace = "lv.lvrtc.networklogic"
}

moduleConfig {
    module = LibraryModule.NetworkLogic
}

dependencies {
    implementation(project(LibraryModule.BusinessLogic.path))

    api(libs.retrofit.core)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.okhttp.mockwebserver)
}