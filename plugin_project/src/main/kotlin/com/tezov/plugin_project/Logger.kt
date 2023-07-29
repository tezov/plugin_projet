package com.tezov.plugin_project

import org.gradle.api.Project

object Logger {

    internal fun logInfo(data: String, project:Project? = null) {
        project?.logger?.info(data) ?: run {
            println(data)
        }
    }

    internal fun logError(data: String, project:Project? = null) {
        project?.logger?.error(data) ?: run {
            println(data)
        }
    }

    internal fun logWarning(data: String, project:Project? = null) {
        project?.logger?.warn(data) ?: run {
            println(data)
        }
    }

}