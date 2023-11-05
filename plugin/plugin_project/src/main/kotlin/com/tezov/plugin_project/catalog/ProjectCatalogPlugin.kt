package com.tezov.plugin_project.catalog

import com.tezov.plugin_project.Logger.PLUGIN_CATALOG
import com.tezov.plugin_project.Logger.throwException
import com.tezov.plugin_project.VersionCheck
import com.tezov.plugin_project.catalog.CatalogMap.Companion.KEY_SEPARATOR
import com.tezov.plugin_project.catalog.ProjectCatalogPlugin.Companion.CATALOG_PLUGIN_ID
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.kotlin.dsl.KotlinSettingsScript
import java.util.concurrent.atomic.AtomicBoolean

fun Settings.tezovCatalogSource(value: String) {
    extensions.findByType(SettingsExtension::class.java)?.buildCatalog(value)
        ?: PLUGIN_CATALOG.throwException(
            this,
            "you need to apply the plugin $CATALOG_PLUGIN_ID before to use this extension"
        )
}

abstract class SettingsExtension constructor(
    private val settings: Settings
) {
    internal var catalog: CatalogMap? = null

    internal fun buildCatalog(sourceCatalog: String) {
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
            .let {
                it.findByName(ProjectCatalogPlugin.CATALOG_KEY_LIBRARIES) ?: it.create(
                    ProjectCatalogPlugin.CATALOG_KEY_LIBRARIES
                )
            }

        val versionsKey = ProjectCatalogPlugin.CATALOG_KEY_VERSIONS + KEY_SEPARATOR
        catalog.filter {
            it.startsWith(versionsKey)
        }.takeIf { it.isNotEmpty() }?.let { catalogVersions ->
            with(domainLibrary) {
                catalogVersions.forEach { entry ->
                    version(entry.key.substringAfter(KEY_SEPARATOR), entry.value)
                }
            }
        }

        val librariesKey = ProjectCatalogPlugin.CATALOG_KEY_LIBRARIES + KEY_SEPARATOR
        catalog.filter {
            it.startsWith(librariesKey)
        }.takeIf { it.isNotEmpty() }?.let { catalogLibraries ->
            with(domainLibrary) {
                catalogLibraries.forEach { entry ->
                    library(entry.key.substringAfter(KEY_SEPARATOR), entry.value)
                }
            }
        }
        this.catalog = catalog
    }
}

class ProjectCatalogPlugin : Plugin<Any> {

    companion object {
        internal const val CATALOG_PLUGIN_ID = "com.tezov.plugin_project.catalog"
        internal const val CATALOG_EXTENSION_NAME = "tezovCatalog"

        private val hasBeenApplyToRootProject = AtomicBoolean(false)

        internal const val CATALOG_KEY_LIBRARIES = "libraries"
        internal const val CATALOG_KEY_VERSIONS = "versions"
        internal const val CATALOG_KEY_PLUGINS_APPLY_TO = "plugins_apply_to"
    }

    override fun apply(any: Any) {
        when (any) {
            is Settings -> {
                VersionCheck.gradle(any, PLUGIN_CATALOG, CATALOG_PLUGIN_ID)
                val settingsExtension = any.extensions.create(
                    CATALOG_EXTENSION_NAME,
                    SettingsExtension::class.java,
                    any
                )
                any.gradle.rootProject {
                    settingsExtension.catalog?.let { catalog ->
                        hasBeenApplyToRootProject.set(true)
                        extensions.create(
                            CATALOG_EXTENSION_NAME,
                            CatalogProjectExtension::class.java,
                            catalog
                        )
                    } ?: run {
                        PLUGIN_CATALOG.throwException(
                            this,
                            "catalog not found. Maybe you forgot the call tezovCatalogSource(value:String) in settings.gradle after having applied the plugin ?"
                        )
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

}
