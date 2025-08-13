import project.convention.logic.config.LibraryModule

plugins {
    id("project.android.feature")
}

android {
    namespace = "lv.lvrtc.transactionsfeature"
}
dependencies {
    implementation(project(":storage-logic"))
}

moduleConfig {
    module = LibraryModule.TransactionsFeature
}