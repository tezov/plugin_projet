package com.tezov.plugin_project.config

import org.gradle.configurationcache.extensions.capitalized

enum class BuildType{
    UNKNOWN,
    DEBUG { init { suffix = ".dbg" } },
    RELEASE { init { suffix = ".rse" } };

    var suffix: String = ".unknown"
        internal set(value) {
            field = if (value.isBlank()) {
                UNKNOWN.suffix
            } else if (value.startsWith(".")) {
                value
            } else {
                ".$value"
            }
        }

    fun capitalName() = name.lowercase().capitalized()

    fun preBuildName() = "pre${capitalName()}Build"

}