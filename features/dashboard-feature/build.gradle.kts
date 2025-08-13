import project.convention.logic.config.LibraryModule

plugins {
    id("project.android.feature")
}

android {
    namespace = "lv.lvrtc.dashboard"
}

moduleConfig {
    module = LibraryModule.DashboardFeature
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(project(LibraryModule.IssuanceFeature.path))
    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)
}