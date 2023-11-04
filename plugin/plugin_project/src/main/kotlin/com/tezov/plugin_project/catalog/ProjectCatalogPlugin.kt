package com.tezov.plugin_project.catalog

import com.tezov.plugin_project.Logger.PLUGIN_CATALOG
import com.tezov.plugin_project.Logger.throwException
import com.tezov.plugin_project.VersionCheck
import com.tezov.plugin_project.catalog.CatalogMap.Companion.KEY_SEPARATOR
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

        internal const val CATALOG_KEY_LIBRARIES = "libraries"
        internal const val CATALOG_KEY_VERSIONS = "versions"
        internal const val CATALOG_KEY_PLUGINS_APPLY_TO = "plugins_apply_to"
    }

    override fun apply(any: Any) {
        when (any) {
            is Settings -> {
                VersionCheck.gradle(any, PLUGIN_CATALOG, CATALOG_PLUGIN_ID)
                val source =
                    runCatching { System.getProperty(SYSTEM_PROP_CATALOG_SOURCE) }.getOrNull()
                val catalog = source?.let { buildCatalog(any, source) }
                any.gradle.rootProject {
                    catalog?.let {
                        hasBeenApplyToRootProject.set(true)
                        extensions.create(
                            CATALOG_EXTENSION_NAME,
                            CatalogProjectExtension::class.java,
                            catalog
                        )
                    } ?: run {
                        PLUGIN_CATALOG.throwException("catalog property not found. Add a valid property 'systemProp.catalog' in gradle.properties")
                    }
                }
            }

            is Project -> {
                if (!hasBeenApplyToRootProject.get()) {
                    PLUGIN_CATALOG.throwException(
                        any,
                        "$CATALOG_PLUGIN_ID must be applied in settings.gradle.kts of ${any.rootProject.name}"
                    )
                }
                any.extensions.create(CATALOG_EXTENSION_NAME, CatalogModuleExtension::class.java)
            }

            else -> {
                PLUGIN_CATALOG.throwException("unknown class ${any::class.java}. You must apply the plugin to the root project settings.gradle.kts")
            }
        }

    }

    private fun buildCatalog(settings: Settings, sourceCatalog: String): CatalogMap {
        val catalog = CatalogMap(
            settings = settings,
            catalogPointer = CatalogPointer.build(
                settings = settings,
                from = sourceCatalog
            )
        )

        val domainLibrary = settings
            .dependencyResolutionManagement
            .versionCatalogs
            .let { it.findByName(CATALOG_KEY_LIBRARIES) ?: it.create(CATALOG_KEY_LIBRARIES) }

        val versionsKey = CATALOG_KEY_VERSIONS + KEY_SEPARATOR
        catalog.filter {
            it.startsWith(versionsKey)
        }.takeIf { it.isNotEmpty() }?.let { catalogVersions ->
            with(domainLibrary) {
                catalogVersions.forEach { entry ->
                    version(entry.key.substringAfter(KEY_SEPARATOR), entry.value)
                }
            }
        }

        val librariesKey = CATALOG_KEY_LIBRARIES + KEY_SEPARATOR
        catalog.filter {
            it.startsWith(librariesKey)
        }.takeIf { it.isNotEmpty() }?.let { catalogLibraries ->
            with(domainLibrary) {
                catalogLibraries.forEach { entry ->
                    library(entry.key.substringAfter(KEY_SEPARATOR), entry.value)
                }
            }
        }
        return catalog
    }
}
