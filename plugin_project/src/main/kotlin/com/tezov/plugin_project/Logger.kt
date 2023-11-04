package com.tezov.plugin_project

import org.gradle.api.GradleException
import org.gradle.api.Project

object Logger {

    @JvmInline
    value class PluginType(val value: String)

    val PLUGIN_CONFIG = PluginType("Tezov config plugin")
    val PLUGIN_CATALOG = PluginType("Tezov catalog plugin")

    internal fun Project.log(plugin:PluginType, data: String) {
        val message = "${plugin.value}: ${project.name}: $data"
        if(logger.isInfoEnabled) logger.info(message)
        else println(message)
    }

    internal fun Project.throwException(plugin:PluginType, data: String): Nothing {
        throw GradleException("${plugin.value}: ${project.name}: $data")
    }

    internal fun throwException(plugin:PluginType, data: String): Nothing {
        throw GradleException("${plugin.value}: $data")
    }

}