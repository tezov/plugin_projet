package com.tezov.plugin_project.config

import com.tezov.plugin_project.VersionCheck
import com.tezov.plugin_project.catalog.ProjectCatalogPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

class ProjectConfigPlugin : Plugin<Project> {

    companion object {
        internal const val CONFIG_PLUGIN_ID = "com.tezov.plugin_project.config"
        internal const val CONFIG_EXTENSION_NAME = "tezovConfig"
    }

    override fun apply(project: Project) {
        VersionCheck.gradle(project, CONFIG_PLUGIN_ID)
        VersionCheck.androidClasspath(project, ProjectCatalogPlugin.CATALOG_PLUGIN_ID)
        project.extensions.create(CONFIG_EXTENSION_NAME, ConfigExtension::class.java)
    }

}

