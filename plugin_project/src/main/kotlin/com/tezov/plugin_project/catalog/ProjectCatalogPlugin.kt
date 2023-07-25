package com.tezov.plugin_project.catalog

import org.gradle.api.Plugin
import org.gradle.api.Project

class ProjectCatalogPlugin : Plugin<Project> {

    companion object {
        const val CATALOG_PLUGIN_ID = "com.tezov.plugin_project.catalog"
        internal const val CATALOG_PLUGIN_NAME = "tezovCatalog"
    }

    override fun apply(project: Project) {
        project.takeIf { it === project.rootProject }?.let {
            project.extensions.create(CATALOG_PLUGIN_NAME, CatalogRootExtension::class.java)
        } ?: run {
            project.extensions.create(CATALOG_PLUGIN_NAME, CatalogExtension::class.java)
        }
    }


}
