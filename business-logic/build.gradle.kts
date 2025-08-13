import project.convention.logic.config.LibraryModule

plugins {
    id("project.android.library")
}

android {
    namespace = "lv.lvrtc.businesslogic"
}

moduleConfig {
    module = LibraryModule.BusinessLogic
}

dependencies {
    implementation(project(LibraryModule.ResourcesLogic.path))
    implementation(libs.gson)
    implementation(libs.androidx.security)
    implementation(libs.androidx.appAuth)
    implementation(libs.google.phonenumber)
    implementation(libs.timber)
    implementation(libs.treessence)
    implementation(project(LibraryModule.AnalyticsLogic.path))
}