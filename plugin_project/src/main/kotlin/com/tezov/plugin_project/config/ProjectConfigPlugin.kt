package com.tezov.plugin_project.config

import org.gradle.api.Plugin
import org.gradle.api.Project

class ProjectConfigPlugin : Plugin<Project> {

    companion object {
        private const val CONFIG_PLUGIN_ID = "com.tezov.plugin_project.config"
        private const val CONFIG_PLUGIN_NAME = "tezovConfig"
    }

    override fun apply(project: Project) {
        project.extensions.create(CONFIG_PLUGIN_NAME, ConfigExtension::class.java)
    }

}

