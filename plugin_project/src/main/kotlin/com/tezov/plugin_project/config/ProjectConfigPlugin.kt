package com.tezov.plugin_project.config

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.tezov.plugin_project.Logger.throwException
import com.tezov.plugin_project.VersionCheck
import com.tezov.plugin_project.catalog.ProjectCatalogPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

class ProjectConfigPlugin : Plugin<Project> {

    companion object {
        internal const val CONFIG_PLUGIN_ID = "com.tezov.plugin_project.config"
        internal const val CONFIG_EXTENSION_NAME = "tezovConfig"

        internal const val ANDROID_PLUGIN_CLASSPATH = "com.android.tools.build:gradle"
        internal const val ANDROID_EXTENSION_NAME = "android"
    }

    override fun apply(project: Project) {
        VersionCheck.gradle(project, CONFIG_PLUGIN_ID)
        VersionCheck.androidClasspath(project, ProjectCatalogPlugin.CATALOG_PLUGIN_ID)
        project.extensions.findByName(ANDROID_EXTENSION_NAME)?.let {
            when(it){
                is ApplicationExtension -> {
                    project.extensions.create(CONFIG_EXTENSION_NAME, ExtensionApp::class.java)
                }
                is LibraryExtension -> {
                    project.extensions.create(CONFIG_EXTENSION_NAME, ExtensionLib::class.java)
                }
                else -> {
                    project.throwException("Android plugin extension unknown type ${it::class.java.name}")
                }
            }
        } ?: kotlin.run {
            project.throwException("Android plugin not found, $CONFIG_PLUGIN_ID need Android plugin applied")
        }
    }

}

