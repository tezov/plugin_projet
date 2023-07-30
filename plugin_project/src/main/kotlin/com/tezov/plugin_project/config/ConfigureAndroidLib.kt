package com.tezov.plugin_project.config

import com.android.build.api.dsl.LibraryBuildType
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.Task
import java.io.File

internal class ConfigureAndroidLib(
    val project: Project,
    val configExtensionLib: ExtensionLib,
    val configExtensionApp: ExtensionApp,
    val androidExtension: LibraryExtension,
):ConfigureAndroidCommon.Protocol {

    val common = ConfigureAndroidCommon(project, this)

    fun apply() {
        configureAndroid()
        configureProguard()
        common.apply()
    }

    override fun afterEvaluated() {
//        tasksProjectRegister()
    }

    override fun afterAllEvaluated() {
//        tasksProjectRegisterDependsOn()
    }

    override fun whenReady() {
//        tasksProjectRegisterInput()
    }

    private fun configureAndroid() {
        androidExtension.namespace = configExtensionLib.nameSpace
        defaultConfig()
        common.sourceSet(
            sourceSets = androidExtension.sourceSets,
            hasResources = configExtensionLib.configuration.hasResources,
            hasAssets = configExtensionLib.configuration.hasAssets
        )
        build()
    }

    private fun defaultConfig() {
        androidExtension.defaultConfig {
            if (configExtensionLib.debug.hasJUnitRunner) {
                testInstrumentationRunner = "${androidExtension.namespace}.jUnit.JUnitRunner"
            }
        }
    }

    private fun build() {
        androidExtension.buildFeatures {
            buildConfig = true
        }
        androidExtension.buildTypes {
            getByName("debug") {
                isMinifyEnabled = false
                common.buildConfig(this, BuildConfig.DEBUG_ONLY, true)
            }
            getByName("release") {
                isMinifyEnabled = if (configExtensionApp.release.enableDebug) {
                    configExtensionApp.debug.minify
                } else {
                    true
                }
                common.buildConfig(this, BuildConfig.DEBUG_ONLY, configExtensionApp.release.enableDebug)
            }
        }
    }

    private fun configureProguard() {
        common.proguard(
            buildTypes = androidExtension.buildTypes,
            proguards = configExtensionLib.release.proguards,
            keepProguardsDebug = configExtensionLib.release.keepProguardsDebug,
            keepProguardsRelease = configExtensionLib.release.keepProguardsRelease,
            enableDebug = configExtensionApp.release.enableDebug,
            keepProguard = configExtensionApp.debug.keepProguard,
            repackage = configExtensionApp.debug.repackage,
            repackageName = configExtensionApp.release.repackageName,
            keepSourceFile = configExtensionApp.debug.keepSourceFile,
            obfuscate = configExtensionApp.debug.obfuscate,
        )
    }

    override val commonExtension: ExtensionCommon
        get() = configExtensionLib.common

    override fun initCurrentBuildType(graphTasks: List<Task>) {
        configExtensionLib.initCurrentBuildType(graphTasks)
    }

    override fun proguardAdd(
        buildType: com.android.build.api.dsl.BuildType,
        element: File,
        placeholders: Map<String, String>?
    ) {
        if (androidExtension !is LibraryBuildType) return
        androidExtension.consumerProguardFiles.add(element)
    }

    override fun proguardAddAll(
        buildType: com.android.build.api.dsl.BuildType,
        element: Collection<File>
    ) {
        if (androidExtension !is LibraryBuildType) return
        androidExtension.consumerProguardFiles.addAll(element)
    }

}