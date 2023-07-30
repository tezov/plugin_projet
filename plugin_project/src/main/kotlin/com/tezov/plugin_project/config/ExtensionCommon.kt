package com.tezov.plugin_project.config

import com.tezov.plugin_project.Logger.logError
import org.gradle.api.Project
import org.gradle.api.Task

internal class ExtensionCommon (
    private val project: Project
) {

    fun nameSpace(configExtension: ExtensionApp) =
        "${configExtension.configuration.domain}.${project.name}"

    fun applicationId(configExtension: ExtensionApp) =
        "${configExtension.configuration.domain}.${project.rootProject.name}"

    fun packageName(configExtension: ExtensionApp) =
        "${configExtension.configuration.domain}.${project.rootProject.name}${configExtension.build.currentType.suffix}"

    var beforeVariant: ((buildType: BuildType) -> Unit)? = null
        set(value) {
            field?.let {
                project.logError("beforeVariant can be used only once")
            }
            field = value
        }
    var whenEvaluated: ((buildType: BuildType) -> Unit)? = null
        set(value) {
            field?.let {
                project.logError("whenEvaluated can be used only once")
            }
            field = value
        }
    var whenReady: ((buildType: BuildType) -> Unit)? = null
        set(value) {
            field?.let {
                project.logError("whenReady can be used only once")
            }
            field = value
        }

    fun findCurrentBuildType(graphTasks: List<Task>):BuildType {
        val taskPreDebugBuild =
            graphTasks.find { task -> task.name == BuildType.DEBUG.preBuildName() }
        val taskPreReleaseBuild =
            graphTasks.find { task -> task.name == BuildType.RELEASE.preBuildName() }
        if (taskPreDebugBuild != null && taskPreReleaseBuild != null) {
            project.logError("Debug and Release task found...")
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