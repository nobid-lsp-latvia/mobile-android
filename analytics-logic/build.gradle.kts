import project.convention.logic.config.LibraryModule

plugins {
    id("project.android.library")
}

android {
    namespace = "lv.lvrtc.analyticslogic"
}

moduleConfig {
    module = LibraryModule.AnalyticsLogic
}


dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
}