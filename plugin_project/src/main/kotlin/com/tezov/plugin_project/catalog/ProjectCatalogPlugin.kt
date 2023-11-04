package com.tezov.plugin_project.catalog

import com.tezov.plugin_project.Logger
import com.tezov.plugin_project.Logger.PLUGIN_CATALOG
import com.tezov.plugin_project.Logger.throwException
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

        internal const val SYSTEM_PROP_CATALOG_SOURCE = "catalog"

        private val hasBeenApplyToRootProject = AtomicBoolean(false)
    }

    override fun apply(any: Any) {
        when (any) {
            is Settings -> any.gradle.rootProject {
                VersionCheck.gradle(this, PLUGIN_CATALOG, CATALOG_PLUGIN_ID)
                kotlin.runCatching {
                    System.getProperty(SYSTEM_PROP_CATALOG_SOURCE)
                }.getOrNull()?.let {source ->
                    hasBeenApplyToRootProject.set(true)
                    extensions.create(CATALOG_EXTENSION_NAME, CatalogProjectExtension::class.java, any, source)
                } ?: kotlin.run {
                    this.throwException(PLUGIN_CATALOG,"catalog property not found. Add a valid property 'systemProp.catalog' in gradle.properties")
                }
            }

            is Project -> {
                if (!hasBeenApplyToRootProject.get()) {
                    any.throwException(PLUGIN_CATALOG,"$CATALOG_PLUGIN_ID must be applied in settings.gradle.kts of ${any.rootProject.name}")
                }
                any.extensions.create(CATALOG_EXTENSION_NAME, CatalogModuleExtension::class.java)
            }

            else -> {
                throwException(PLUGIN_CATALOG,"unknown class ${any::class.java}. You must apply the plugin to the root project settings.gradle.kts")
            }
        }

    }
}
