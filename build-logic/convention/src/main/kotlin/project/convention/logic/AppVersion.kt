// SPDX-License-Identifier: EUPL-1.2

package project.convention.logic

open class AppVersion(
    val major: Int = 1,
    val minor: Int = 0,
    val patch: Int = 0,
    val build: Int = 0
) {
    val code: Int = major * 10000 + minor * 1000 + patch * 100 + (build.coerceIn(0, 99))
    val name: String = if (build > 0) {
        "$major.$minor.$patch-$build"
    } else {
        "$major.$minor.$patch"
    }

    companion object {
        fun parse(version: String): AppVersion {
            return try {
                if (version.contains("-")) {
                    val (versionPart, buildPart) = version.split("-")
                    val parts = versionPart.split(".")
                    require(parts.size == 3)
                    AppVersion(
                        major = parts[0].toInt(),
                        minor = parts[1].toInt(),
                        patch = parts[2].toInt(),
                        build = buildPart.toInt()
                    )
                } else {
                    val parts = version.split(".")
                    require(parts.size == 3)
                    AppVersion(
                        major = parts[0].toInt(),
                        minor = parts[1].toInt(),
                        patch = parts[2].toInt()
                    )
                }
            } catch (e: Exception) {
                AppVersion()
            }
        }
    }
}