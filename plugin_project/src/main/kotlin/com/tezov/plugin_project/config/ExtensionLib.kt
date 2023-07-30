package com.tezov.plugin_project.config

import com.android.build.api.dsl.LibraryExtension
import com.tezov.plugin_project.Logger.throwException
import com.tezov.plugin_project.PropertyDelegate
import com.tezov.plugin_project.config.ProjectConfigPlugin.Companion.ANDROID_EXTENSION_NAME
import com.tezov.plugin_project.config.ProjectConfigPlugin.Companion.CONFIG_EXTENSION_NAME
import com.tezov.plugin_project.config.ProjectConfigPlugin.Companion.CONFIG_PLUGIN_ID
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.model.ObjectFactory
import java.io.File
import javax.inject.Inject

open class ExtensionLib @Inject constructor(
    factory: ObjectFactory,
    private val project: Project
) {

    open class Debug {
        var hasJUnitRunner by PropertyDelegate { false }
    }

    open class Release {
        val proguards = mutableListOf<File>()
        val keepProguardsDebug = mutableListOf<File>()
        val keepProguardsRelease = mutableListOf<File>()
    }

    open class Configuration {
        var hasResources by PropertyDelegate { false }
        var hasAssets by PropertyDelegate { false }
    }

    open class Build {
        var currentType by PropertyDelegate<BuildType>()
            internal set
    }

    open class Lint {
        var abortOnError by PropertyDelegate { false }
        var checkReleaseBuilds by PropertyDelegate { false }
        val disable = mutableListOf<String>()
    }

    internal val common = ExtensionCommon(project)
    val debug = factory.newInstance(Debug::class.java)
    val release = factory.newInstance(Release::class.java)
    val configuration = factory.newInstance(Configuration::class.java)
    internal val build = factory.newInstance(Build::class.java)
    val lint = factory.newInstance(Lint::class.java)

    val nameSpace get() = findConfigExtensionApp()?.let {
        kotlin.runCatching { common.nameSpace(it) }.getOrNull()
    } ?: run {
        project.throwException("nameSpace is not ready yet")
    }
    val applicationId get() = findConfigExtensionApp()?.let {
        kotlin.runCatching { common.applicationId(it) }.getOrNull()
    } ?: run {
        project.throwException("applicationId is not ready yet")
    }
    val packageName get() = findConfigExtensionApp()?.let {
        kotlin.runCatching { common.packageName(it) }.getOrNull()
    } ?: run {
        project.throwException("packageName is not ready yet")
    }

    internal fun initCurrentBuildType(graphTasks: List<Task>) {
        build.currentType = common.findCurrentBuildType(graphTasks)
    }

    fun debug(block: Debug.() -> Unit) {
        debug.block()
    }

    fun release(block: Release.() -> Unit) {
        release.block()
    }

    fun configuration(block: Configuration.() -> Unit) {
        configuration.block()
    }

    fun lint(block: Lint.() -> Unit) {
        lint.block()
    }

    fun beforeVariant(block: (buildType: BuildType) -> Unit) {
        common.beforeVariant = block
    }

    fun whenEvaluated(block: (buildType: BuildType) -> Unit) {
        common.whenEvaluated = block
    }

    fun whenReady(block: (buildType: BuildType) -> Unit) {
        common.whenReady = block
    }

    fun configureAndroidPlugin() {
        val androidExtensionLib = (project.extensions.findByName(ANDROID_EXTENSION_NAME) as? LibraryExtension) ?: kotlin.run {
            project.throwException("Android plugin library not found")
        }
        val configExtensionApp = findConfigExtensionApp()?: kotlin.run {
            project.throwException("Tezov plugin config application not found in all projects")
        }
        ConfigureAndroidLib(
            project = project,
            configExtensionLib = this,
            configExtensionApp = configExtensionApp,
            androidExtension = androidExtensionLib,
        ).apply()
    }

    private fun findConfigExtensionApp():ExtensionApp? {
        var configExtensionApp:ExtensionApp? = null
        for (project in project.rootProject.allprojects) {
            configExtensionApp = (project.extensions.findByName(CONFIG_EXTENSION_NAME) as? ExtensionApp)
            if(configExtensionApp != null) break
        }
        return configExtensionApp
    }

}