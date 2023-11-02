package com.tezov.plugin_project.catalog

import com.tezov.plugin_project.Logger.logError
import com.tezov.plugin_project.VersionCheck
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import java.util.concurrent.atomic.AtomicBoolean

class ProjectCatalogPlugin : Plugin<Any> {

    companion object {
        internal const val CATALOG_PLUGIN_ID = "com.tezov.plugin_project.catalog"
        internal const val CATALOG_EXTENSION_NAME = "tezovCatalog"

        private val hasBeenApplyToRootProject = AtomicBoolean(false)
    }

    override fun apply(any: Any) {
        when (any) {
            is Settings -> any.gradle.rootProject {
                VersionCheck.gradle(this, CATALOG_PLUGIN_ID)
                extensions.create(CATALOG_EXTENSION_NAME, CatalogProjectExtension::class.java)
                hasBeenApplyToRootProject.set(true)
            }

            is Project -> {
                VersionCheck.gradle(any, CATALOG_PLUGIN_ID)
                if (any === any.rootProject || !hasBeenApplyToRootProject.get()) {
                    any.logError("$CATALOG_PLUGIN_ID must be applied in settings.gradle.kts of ${any.rootProject.name}")
                }
                any.extensions.create(CATALOG_EXTENSION_NAME, CatalogModuleExtension::class.java)
            }

            else -> {
                GradleException("unknown class ${any::class.java}. You must apply the plugin to the root project settings.gradle.kts")
            }
        }

    }
}
