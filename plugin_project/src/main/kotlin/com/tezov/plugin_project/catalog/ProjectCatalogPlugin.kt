package com.tezov.plugin_project.catalog

import com.tezov.plugin_project.Logger.logError
import com.tezov.plugin_project.VersionCheck
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.util.concurrent.atomic.AtomicBoolean

class ProjectCatalogPlugin : Plugin<Project> {

    companion object {
        internal const val CATALOG_PLUGIN_ID = "com.tezov.plugin_project.catalog"
        internal const val CATALOG_EXTENSION_NAME = "tezovCatalog"

        private val hasBeenApplyToRootProject = AtomicBoolean(false)
    }

    override fun apply(project: Project) {
        VersionCheck.gradle(project, CATALOG_PLUGIN_ID)
        project.takeIf { it === project.rootProject }?.let {
            project.extensions.create(CATALOG_EXTENSION_NAME, CatalogProjectExtension::class.java)
            hasBeenApplyToRootProject.set(true)
        } ?: run {
            if (!hasBeenApplyToRootProject.get()) {
                project.logError("$CATALOG_PLUGIN_ID has not been set to root project, plugin must be only apply at setup inside build.gradle.kts of ${project.rootProject.name}")
            }
            project.extensions.create(CATALOG_EXTENSION_NAME, CatalogModuleExtension::class.java)
        }
    }

}
