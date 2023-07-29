package com.tezov.plugin_project

import org.gradle.api.GradleException
import org.gradle.api.Project

object Logger {

    internal fun Project.logInfo(data: String) {
        "${project.name}: $data".let {
            if(logger.isInfoEnabled) logger.info(it)
            else println(it)
        }
    }

    internal fun Project.logError(data: String) {
        "${project.name}: $data".let {
            if(logger.isErrorEnabled) logger.info(it)
            else println(it)
        }
    }

    internal fun Project.logWarning(data: String) {
        "${project.name}: $data".let {
            if(logger.isWarnEnabled) logger.info(it)
            else println(it)
        }
    }

    internal fun Project.throwException(data: String): Nothing {
        "${project.name}: $data".let {
            throw GradleException(it)
        }
    }

}