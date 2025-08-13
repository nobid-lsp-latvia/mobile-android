import project.convention.logic.AppVersion
import project.convention.logic.config.LibraryModule

plugins {
    id("project.android.application")
    id("project.android.version")
    id("project.android.application.compose")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "lv.lvrtc.edim"
    compileSdk = 35

    packaging {
        resources {
            excludes += "META-INF/*"
            excludes += "/META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }

    defaultConfig {
        applicationId = "lv.lvrtc.edim"
        minSdk = 26
        targetSdk = 35

        val appVersion: AppVersion by extensions
        versionCode = appVersion.code
        versionName = appVersion.name

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            enableAndroidTestCoverage = true
            enableUnitTestCoverage = true

            matchingFallbacks += listOf("production")
        }
        release {
            firebaseCrashlytics {
                nativeSymbolUploadEnabled = true
            }
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false

            matchingFallbacks += listOf("production")

            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

tasks.register("createRelease") {
    group = "release"
    description = "Creates a new release build"

    dependsOn("clean")
    dependsOn("assembleRelease")
    dependsOn("bundleRelease")

    tasks.findByName("bundleRelease")?.mustRunAfter("clean")
    tasks.findByName("assembleRelease")?.mustRunAfter("clean")
}

tasks.withType<Test> {
    useJUnit()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    sourceDirectories.setFrom(files("${project.projectDir}/src/main/java"))

    classDirectories.setFrom(
        files(
            fileTree("${project.layout.buildDirectory}/tmp/kotlin-classes/debug") {
                exclude(
                    "**/R.class",
                    "**/R$*.class",
                    "**/BuildConfig.*",
                    "**/Manifest*.*",
                    "**/*Test*.*",
                    "android/**/*.*"
                )
            }
        )
    )

    executionData.setFrom(files("${project.layout.buildDirectory}/jacoco/testDebugUnitTest.exec"))
}

dependencies {
    implementation(project(LibraryModule.AssemblyLogic.path))
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
}