// SPDX-License-Identifier: EUPL-1.2

package project.convention.logic.config

enum class LibraryModule(val path: String) {
    Unspecified(""),
    AssemblyLogic(":assembly-logic"),
    AnalyticsLogic(":analytics-logic"),
    AuthLogic(":auth-logic"),
    BusinessLogic(":business-logic"),
    CoreLogic(":core-logic"),
    ResourcesLogic(":resources-logic"),
    NetworkLogic(":network-logic"),
    WebBridge(":web-bridge"),
    StartupFeature(":startup-feature"),
    SignFeature(":sign-feature"),
    StorageLogic(":storage-logic"),
    CommonFeature(":common-feature"),
    UiLogic(":ui-logic"),
    WebFeature(":web-feature"),
    IssuanceFeature(":features:issuance-feature"),
    PresentationFeature(":features:presentation-feature"),
    DashboardFeature(":features:dashboard-feature"),
    TransactionsFeature(":features:transactions-feature");

    val isLogicModule: Boolean
        get() {
            return this.name.contains("Logic") || this == WebBridge
        }

    val isFeatureCommon: Boolean get() = this == CommonFeature
}

open class LibraryPluginConfig(var module: LibraryModule)