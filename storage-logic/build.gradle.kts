import project.convention.logic.config.LibraryModule

plugins {
    id("project.android.library")
    alias(libs.plugins.realm)
}

android {
    namespace = "lv.lvrtc.storagelogic"
}


moduleConfig {
    module = LibraryModule.StorageLogic
}

dependencies {
    implementation(project(LibraryModule.BusinessLogic.path))

    implementation(libs.androidx.appcompat)
    implementation(libs.kotlin.realm)
}