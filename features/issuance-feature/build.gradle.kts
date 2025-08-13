import project.convention.logic.config.LibraryModule

plugins {
    id("project.android.feature")
}

android {
    namespace = "lv.lvrtc.issuancefeature"
}
dependencies {
    implementation(libs.androidx.browser)
    implementation(project(LibraryModule.NetworkLogic.path))
    implementation(project(LibraryModule.SignFeature.path))
}

moduleConfig {
    module = LibraryModule.IssuanceFeature
}