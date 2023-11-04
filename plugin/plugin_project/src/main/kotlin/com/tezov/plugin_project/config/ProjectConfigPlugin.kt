package com.tezov.plugin_project.config

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.tezov.plugin_project.Logger
import com.tezov.plugin_project.Logger.PLUGIN_CONFIG
import com.tezov.plugin_project.Logger.throwException
import com.tezov.plugin_project.VersionCheck
import com.tezov.plugin_project.catalog.ProjectCatalogPlugin.Companion.CATALOG_PLUGIN_ID
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
        VersionCheck.gradle(project, PLUGIN_CONFIG, CONFIG_PLUGIN_ID)
        VersionCheck.androidClasspath(project, PLUGIN_CONFIG, CATALOG_PLUGIN_ID)
        project.extensions.findByName(ANDROID_EXTENSION_NAME)?.let {
            when(it){
                is ApplicationExtension -> {
                    project.extensions.create(CONFIG_EXTENSION_NAME, ExtensionApp::class.java)
                }
                is LibraryExtension -> {
                    project.extensions.create(CONFIG_EXTENSION_NAME, ExtensionLib::class.java)
                }
                else -> {
                    PLUGIN_CONFIG.throwException(project,"android plugin extension unknown type ${it::class.java.name}")
                }
            }
        } ?: kotlin.run {
            PLUGIN_CONFIG.throwException(project,"android plugin not found, $CONFIG_PLUGIN_ID need Android plugin applied")
        }
    }

}

