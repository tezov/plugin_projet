package com.tezov.plugin_project.config

import com.android.build.api.dsl.ApplicationBuildType
import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Project
import org.gradle.api.Task
import java.io.File


internal class ConfigureAndroidApp(
    val project: Project,
    val configExtension: ExtensionApp,
    private val androidExtension: ApplicationExtension,
) : ConfigureAndroidCommon.Protocol {

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
        androidExtension.namespace = configExtension.nameSpace
        defaultConfig()
        common.sourceSet(
            sourceSets = androidExtension.sourceSets,
            hasResources = true,
            hasAssets = true
        )
        build()
        lint()
    }

    private fun defaultConfig() {
        androidExtension.defaultConfig {
            applicationId = configExtension.applicationId
            versionName = configExtension.version.name
            versionCode = configExtension.version.value
            resourceConfigurations.addAll(configExtension.configuration.languages)
            if (configExtension.debug.hasJUnitRunner) {
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
                applicationIdSuffix = BuildType.DEBUG.suffix
                isDebuggable = true
                isMinifyEnabled = false
                isShrinkResources = false
                common.buildConfig(this, BuildConfig.DEBUG_ONLY, true)
            }
            getByName("release") {
                applicationIdSuffix = BuildType.RELEASE.suffix
                isDebuggable = configExtension.release.enableDebug
                if (configExtension.release.enableDebug) {
                    isMinifyEnabled = configExtension.debug.minify
                    isShrinkResources = configExtension.debug.minify
                } else {
                    isMinifyEnabled = true
                    isShrinkResources = true
                }
                common.buildConfig(
                    this,
                    BuildConfig.DEBUG_ONLY,
                    configExtension.release.enableDebug
                )
            }
        }
    }

    private fun lint() {
        androidExtension.lint {
            abortOnError = configExtension.lint.abortOnError
            checkReleaseBuilds = configExtension.lint.checkReleaseBuilds
            checkDependencies = configExtension.lint.checkDependencies
        }

    }

    private fun configureProguard() {
        common.proguard(
            buildTypes = androidExtension.buildTypes,
            proguards = configExtension.release.proguards,
            keepProguardsDebug = configExtension.release.keepProguardsDebug,
            keepProguardsRelease = configExtension.release.keepProguardsRelease,
            enableDebug = configExtension.release.enableDebug,
            keepProguard = configExtension.debug.keepProguard,
            repackage = configExtension.debug.repackage,
            repackageName = configExtension.release.repackageName,
            keepSourceFile = configExtension.debug.keepSourceFile,
            obfuscate = configExtension.debug.obfuscate,
        )
    }

    override val commonExtension: ExtensionCommon
        get() = configExtension.common

    override fun initCurrentBuildType(graphTasks: List<Task>) {
        configExtension.initCurrentBuildType(graphTasks)
    }

    override fun proguardAdd(
        buildType: com.android.build.api.dsl.BuildType,
        element: File,
        placeholders: Map<String, String>?
    ) {
        if (androidExtension !is ApplicationBuildType) return
        androidExtension.proguardFiles.add(element)
    }

    override fun proguardAddAll(
        buildType: com.android.build.api.dsl.BuildType,
        element: Collection<File>
    ) {
        if (androidExtension !is ApplicationBuildType) return
        androidExtension.proguardFiles.addAll(element)
    }

}