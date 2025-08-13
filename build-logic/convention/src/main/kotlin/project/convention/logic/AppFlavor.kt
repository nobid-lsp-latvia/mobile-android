// SPDX-License-Identifier: EUPL-1.2

package project.convention.logic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.ApplicationProductFlavor
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.ProductFlavor
import org.gradle.api.Project

@Suppress("EnumEntryName")
enum class FlavorDimension {
    contentType
}

enum class AppFlavor(
    val dimension: FlavorDimension,
    val applicationIdSuffix: String? = null,
    val applicationNameSuffix: String? = null
) {
    ZZDev(FlavorDimension.contentType, applicationIdSuffix = ".zz.dev"),
    ZZWeb(FlavorDimension.contentType, applicationIdSuffix = ".zz.web"),
    ZZDemo(FlavorDimension.contentType, applicationIdSuffix = ".zz"),

    Demo(FlavorDimension.contentType),
    Prod(FlavorDimension.contentType),
}

fun Project.configureFlavors(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
    flavorConfigurationBlock: ProductFlavor.(flavor: AppFlavor) -> Unit = {}
) {

    val version = getProperty<String>(
        "VERSION_NAME",
        "version.properties"
    ).orEmpty()

    commonExtension.apply {
        flavorDimensions += FlavorDimension.contentType.name
        productFlavors {
            AppFlavor.values().forEach {
                create(it.name.lowercase()) {
                    dimension = it.dimension.name
                    if (this@apply is ApplicationExtension && this is ApplicationProductFlavor) {
                        versionName = version
                        if (it.applicationIdSuffix != null) {
                            applicationIdSuffix = it.applicationIdSuffix
                        }
                    }
                    manifestPlaceholders["appNameSuffix"] = it.applicationNameSuffix.orEmpty()
                    addConfigField(
                        "APP_VERSION",
                        version
                    )
                    flavorConfigurationBlock(this, it)
                }
            }
        }
    }
}
