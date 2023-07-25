package com.tezov.plugin_project.config

import org.gradle.configurationcache.extensions.capitalized

enum class BuildType(val suffix: String) {
    UNKNOWN(".unknown"),
    DEBUG(".dbg"),
    RELEASE(".rse");

    fun capitalName() = name.lowercase().capitalized()

    fun preBuildName() = "pre${capitalName()}Build"

}