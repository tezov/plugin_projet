package com.tezov.plugin_project.catalog

import com.tezov.plugin_project.GradleVersionCheck
import com.tezov.plugin_project.config.ProjectConfigPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

class ProjectCatalogPlugin : Plugin<Project> {

    companion object {
        internal const val CATALOG_PLUGIN_ID = "com.tezov.plugin_project.catalog"
        internal const val CATALOG_EXTENSION_NAME = "tezovCatalog"
    }

    override fun apply(project: Project) {
        GradleVersionCheck(project, CATALOG_PLUGIN_ID)

        project.takeIf { it === project.rootProject }?.let {
            project.extensions.create(CATALOG_EXTENSION_NAME, CatalogRootExtension::class.java)
        } ?: run {
            project.extensions.create(CATALOG_EXTENSION_NAME, CatalogExtension::class.java)
        }
    }


}
