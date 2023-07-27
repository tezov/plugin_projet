package com.tezov.plugin_project.config

import com.tezov.plugin_project.GradleVersionCheck
import org.gradle.api.Plugin
import org.gradle.api.Project

class ProjectConfigPlugin : Plugin<Project> {

    companion object {
        internal const val CONFIG_PLUGIN_ID = "com.tezov.plugin_project.config"
        internal const val CONFIG_EXTENSION_NAME = "tezovConfig"
    }

    override fun apply(project: Project) {
        GradleVersionCheck(project, CONFIG_PLUGIN_ID)
        project.extensions.create(CONFIG_EXTENSION_NAME, ConfigExtension::class.java)
    }

}

