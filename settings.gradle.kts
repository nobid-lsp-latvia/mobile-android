pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven {
            url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            mavenContent { snapshotsOnly() }
        }
        maven {
            url = uri("https://jitpack.io")
        }
    }
}

rootProject.name = "NOBID"
include(":app")
include(":core-logic")
include(":auth-logic")
include(":web-bridge")
include(":resources-logic")
include(":business-logic")
include(":analytics-logic")
include(":assembly-logic")
include(":startup-feature")
include(":ui-logic")
include(":common-feature")
include(":features:issuance-feature")
include(":features:dashboard-feature")
include(":web-feature")
include(":network-logic")
include(":features:presentation-feature")
include(":storage-logic")
include(":features:transactions-feature")
include(":sign-feature")
