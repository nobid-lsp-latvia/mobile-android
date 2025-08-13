import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

group = "project.build.convention.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.secrets.gradlePlugin)
    compileOnly(libs.kotlinx.kover.gradlePlugin)
}


gradlePlugin {
    plugins {
        register("androidApplicationCompose") {
            id = "project.android.application.compose"
            implementationClass = "AndroidApplicationComposeConventionPlugin"
        }
        register("androidApplication") {
            id = "project.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "project.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidFeature") {
            id = "project.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidLibraryKover") {
            id = "project.android.library.kover"
            implementationClass = "AndroidLibraryKoverConventionPlugin"
        }
        register("androidTest") {
            id = "project.android.test"
            implementationClass = "AndroidTestConventionPlugin"
        }
        register("androidKoin") {
            id = "project.android.koin"
            implementationClass = "AndroidKoinConventionPlugin"
        }
        register("androidFlavors") {
            id = "project.android.application.flavors"
            implementationClass = "AndroidApplicationFlavorsConventionPlugin"
        }
        register("androidLint") {
            id = "project.android.lint"
            implementationClass = "AndroidLintConventionPlugin"
        }
        register("jvmLibrary") {
            id = "project.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("eudiWalletCore") {
            id = "project.wallet.core"
            implementationClass = "EudiWalletCorePlugin"
        }
        register("sonar") {
            id = "project.sonar"
            implementationClass = "SonarPlugin"
        }
        register("androidVersion") {
            id = "project.android.version"
            implementationClass = "AndroidVersionConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "project.android.library.compose"
            implementationClass = "AndroidLibraryComposeConventionPlugin"
        }
        register("kotlinRealm") {
            id = "project.kotlin.realm"
            implementationClass = "RealmPlugin"
        }
    }
}