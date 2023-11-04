package com.tezov.plugin_project

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.initialization.Settings

object Logger {

    @JvmInline
    value class PluginType(val value: String)

    val PLUGIN_CONFIG = PluginType("Tezov config plugin")
    val PLUGIN_CATALOG = PluginType("Tezov catalog plugin")

    internal fun PluginType.log(project:Project, data: String) {
        println("${value}: ${project.name}: $data")
    }

    internal fun PluginType.throwException(project:Project, data: String): Nothing {
        throw GradleException("${value}: ${project.name}: $data")
    }


    internal fun PluginType.log(settings:Settings, data: String) {
        println("${value}: ${settings.rootProject.name}: $data")
    }

    internal fun PluginType.throwException(settings:Settings, data: String): Nothing {
        throw GradleException("${value}: ${settings.rootProject.name}: $data")
    }


    internal fun PluginType.throwException(data: String): Nothing {
        throw GradleException("${value}: $data")
    }

}