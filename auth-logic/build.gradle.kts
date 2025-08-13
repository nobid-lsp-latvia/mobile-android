import project.convention.logic.config.LibraryModule

plugins {
    id("project.android.library")
}

android {
    namespace = "lv.lvrtc.authlogic"
}

moduleConfig {
    module = LibraryModule.AuthLogic
}

dependencies {

    implementation(project(LibraryModule.ResourcesLogic.path))
    implementation(project(LibraryModule.BusinessLogic.path))

    implementation(libs.gson)
    api(libs.androidx.biometric)
    implementation(project(LibraryModule.NetworkLogic.path))
}