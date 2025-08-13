// SPDX-License-Identifier: EUPL-1.2

import org.gradle.api.Plugin
import org.gradle.api.Project
import project.convention.logic.AppVersion

class AndroidVersionConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val version = AppVersion.parse(findProperty("version")?.toString() ?: "1.0.0")
            extensions.create("appVersion", AppVersion::class.java, version.major, version.minor, version.patch, version.build)
        }
    }
}