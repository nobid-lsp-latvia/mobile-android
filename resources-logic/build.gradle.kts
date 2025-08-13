import project.convention.logic.config.LibraryModule

plugins {
    id("project.android.library")
    id("project.android.library.compose")
}

android {
    namespace = "lv.lvrtc.resourceslogic"
}

moduleConfig {
    module = LibraryModule.ResourcesLogic
}

dependencies {
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material3.windowSizeClass)
    api(libs.material)
    api(libs.androidx.core.splashscreen)
}