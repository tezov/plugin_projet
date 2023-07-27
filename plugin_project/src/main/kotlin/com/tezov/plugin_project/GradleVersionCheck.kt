package com.tezov.plugin_project

import org.gradle.api.Project
import org.gradle.util.GradleVersion

object GradleVersionCheck {

    val MIN_VERSION =  GradleVersion.version("8.0")

    operator fun invoke(project: Project, pluginName:String){
        if (GradleVersion.current() < MIN_VERSION) {
            project.logger.error("${project.name}: gradle ${MIN_VERSION.version} or greater is required to apply $pluginName.")
            return
        }
    }

}