import project.convention.logic.config.LibraryModule

plugins {
    id("project.android.library")
    id("project.wallet.core")
}

android {
    namespace = "lv.lvrtc.corelogic"
}

moduleConfig {
    module = LibraryModule.CoreLogic
}

dependencies {
    implementation(project(LibraryModule.ResourcesLogic.path))
    implementation(project(LibraryModule.BusinessLogic.path))
    implementation(project(LibraryModule.AuthLogic.path))
    implementation(project(LibraryModule.StorageLogic.path))
    implementation(libs.androidx.biometric)
    implementation(libs.ktor.android)
    implementation(libs.ktor.logging)
}
