package com.tezov.plugin_project.config

import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.Project

internal class ConfigureAndroidApp(
    project: Project,
    configExtension: ConfigExtension,
    private val androidExtension: BaseAppModuleExtension,
) : ConfigureAndroidBase(
    project = project,
    configExtension = configExtension,
) {

    val applicationId get() = "${configExtension.configuration.domain}.${project.rootProject.name}"

    val packageName get() = "${configExtension.configuration.domain}.${project.rootProject.name}${configExtension.build.type.suffix}"

    val isPackageNameValid get() = configExtension.build.type != BuildType.UNKNOWN

    override fun onApply() {
        configureAndroidApp()
//        configureProguardApp()
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

    private fun configureAndroidApp() {
        androidExtension.namespace = applicationId
        defaultConfig()
        androidExtension.sourceSets.configure(
            hasResources = true,
            hasAssets = true
        )
        build()
        lint()
    }

    private fun defaultConfig() {
        androidExtension.defaultConfig {
            applicationId = androidExtension.namespace
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
                isMinifyEnabled = configExtension.debug.minify
                isShrinkResources = false
                buildConfigDebug(true)
            }
            getByName("release") {
                applicationIdSuffix = BuildType.RELEASE.suffix
                isDebuggable = configExtension.release.enableDebug
                if (isDebuggable) {
                    isMinifyEnabled = configExtension.debug.minify
                    isShrinkResources = false
                } else {
                    isMinifyEnabled = configExtension.release.minify
                    isShrinkResources = configExtension.release.minify
                }
                buildConfigDebug(configExtension.release.enableDebug)
            }
        }

    }

    private fun lint() {
        androidExtension.lint {
            abortOnError = configExtension.lint.abortOnError
            checkReleaseBuilds = configExtension.lint.checkReleaseBuilds
            disable.addAll(configExtension.lint.disable)
        }

    }

    private fun tasksProjectRegister() {
        //refactor from java
    }

    private fun tasksProjectRegisterDependsOn() {
        //refactor from java
    }

    private fun tasksProjectRegisterInput() {
        //refactor from java
    }

}