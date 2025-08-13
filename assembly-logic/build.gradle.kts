import project.convention.logic.config.LibraryModule
import project.convention.logic.config.LibraryModule.AnalyticsLogic
import project.convention.logic.config.LibraryModule.AssemblyLogic
import project.convention.logic.config.LibraryModule.AuthLogic
import project.convention.logic.config.LibraryModule.BusinessLogic
import project.convention.logic.config.LibraryModule.CoreLogic
import project.convention.logic.config.LibraryModule.ResourcesLogic
import project.convention.logic.config.LibraryModule.WebBridge
import project.convention.logic.config.LibraryModule.StartupFeature
import project.convention.logic.config.LibraryModule.UiLogic
import project.convention.logic.config.LibraryModule.WebFeature
import project.convention.logic.config.LibraryModule.CommonFeature
import project.convention.logic.config.LibraryModule.IssuanceFeature
import project.convention.logic.config.LibraryModule.NetworkLogic
import project.convention.logic.config.LibraryModule.DashboardFeature
import project.convention.logic.config.LibraryModule.PresentationFeature
import project.convention.logic.config.LibraryModule.SignFeature

plugins {
    id("project.android.library")
    id("project.android.library.compose")
}

android {
    namespace = "lv.lvrtc.assemblylogic"
}

moduleConfig {
    module = AssemblyLogic
}

dependencies {
    api(project(AnalyticsLogic.path))
    api(project(AuthLogic.path))
    api(project(BusinessLogic.path))
    api(project(CoreLogic.path))
    api(project(ResourcesLogic.path))
    api(project(WebBridge.path))
    api(project(StartupFeature.path))
    api(project(UiLogic.path))
    api(project(WebFeature.path))
    api(project(CommonFeature.path))
    api(project(IssuanceFeature.path))
    api(project(NetworkLogic.path))
    api(project(DashboardFeature.path))
    api(project(PresentationFeature.path))
    api(project(SignFeature.path))
    api(project(LibraryModule.StorageLogic.path))
    api(project(LibraryModule.TransactionsFeature.path))
}