import project.convention.logic.config.LibraryModule

plugins {
    id("project.android.feature")
}

android {
    namespace = "lv.lvrtc.startupfeature"
}
dependencies {
    implementation(project(LibraryModule.CommonFeature.path))
    implementation(project(LibraryModule.NetworkLogic.path))
}

moduleConfig {
    module = LibraryModule.StartupFeature
}