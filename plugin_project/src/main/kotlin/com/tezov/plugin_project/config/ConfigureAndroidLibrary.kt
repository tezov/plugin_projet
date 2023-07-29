package com.tezov.plugin_project.config

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project

internal class ConfigureAndroidLibrary(
    project: Project,
    configExtension: ConfigExtension,
    private val androidExtension: LibraryExtension,
) : ConfigureAndroidBase(
    project = project,
    configExtension = configExtension,
) {

    val applicationId get() = "${configExtension.configuration.domain}.${project.name}"

    override fun onApply() {
        configureAndroidLib()
    }

    override fun afterEvaluated() {
//       tasksProjectRegister()
    }

    override fun afterAllEvaluated() {
//       tasksProjectRegisterDependsOn()
    }

    override fun whenReady() {
//       tasksProjectRegisterInput()
    }

    private fun configureAndroidLib() {
        androidExtension.namespace = applicationId
        defaultConfig()
        androidExtension.sourceSets.configure(
            hasResources = configExtension.configuration.hasResources,
            hasAssets = configExtension.configuration.hasAssets
        )
        build()
    }

    private fun defaultConfig() {
        androidExtension.defaultConfig {
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
                isMinifyEnabled = configExtension.debug.minify
                buildConfigDebug(true)
            }
            getByName("release") {
                isMinifyEnabled = if (configExtension.release.enableDebug) {
                    configExtension.debug.minify
                } else {
                    configExtension.release.minify
                }
                buildConfigDebug(configExtension.release.enableDebug)
            }
        }

    }

}