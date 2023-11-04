package com.tezov.plugin_project.config

import com.tezov.plugin_project.Logger
import com.tezov.plugin_project.Logger.PLUGIN_CONFIG
import com.tezov.plugin_project.Logger.throwException
import org.gradle.api.Project
import org.gradle.api.Task

internal class ExtensionCommon(
    private val project: Project
) {

    fun domain(configExtension: ExtensionApp, withSubDomain:Boolean) = StringBuilder().apply {
        with(configExtension.configuration) {
            append(domain)
            if(withSubDomain){
                subDomain?.let {
                    append('.')
                    append(it)
                }
            }
        }
    }.toString()

    fun nameSpace(configExtension: ExtensionApp, withSubDomain:Boolean) = StringBuilder().apply {
        append(domain(configExtension, withSubDomain))
        append('.')
        append(project.name)
    }.toString()

    fun applicationId(configExtension: ExtensionApp, withSubDomain:Boolean) = StringBuilder().apply {
        append(domain(configExtension, withSubDomain))
        append('.')
        append(project.rootProject.name)
    }.toString()

    fun packageName(configExtension: ExtensionApp) = StringBuilder().apply {
        append(applicationId(configExtension, true))
        append(configExtension.build.currentType.suffix)
    }.toString()

    var beforeVariant: ((buildType: BuildType) -> Unit)? = null
        set(value) {
            field?.let {
                PLUGIN_CONFIG.throwException(project,"beforeVariant can be used only once")
            }
            field = value
        }
    var whenEvaluated: ((buildType: BuildType) -> Unit)? = null
        set(value) {
            field?.let {
                PLUGIN_CONFIG.throwException(project,"whenEvaluated can be used only once")
            }
            field = value
        }
    var whenReady: ((buildType: BuildType) -> Unit)? = null
        set(value) {
            field?.let {
                PLUGIN_CONFIG.throwException(project,"whenReady can be used only once")
            }
            field = value
        }

    fun findCurrentBuildType(graphTasks: List<Task>): BuildType {
        val taskPreDebugBuild =
            graphTasks.find { task -> task.name == BuildType.DEBUG.preBuildName() }
        val taskPreReleaseBuild =
            graphTasks.find { task -> task.name == BuildType.RELEASE.preBuildName() }
        if (taskPreDebugBuild != null && taskPreReleaseBuild != null) {
            PLUGIN_CONFIG.throwException(project,"debug and release task found...")
        }
        return when {
            taskPreDebugBuild != null -> BuildType.DEBUG
            taskPreReleaseBuild != null -> BuildType.RELEASE
            else -> BuildType.UNKNOWN
        }
    }

    companion object {
        fun ((buildType: BuildType) -> Unit).invoke() {
            BuildType.values().filter { it != BuildType.UNKNOWN }.forEach {
                invoke(it)
            }
        }
    }

}