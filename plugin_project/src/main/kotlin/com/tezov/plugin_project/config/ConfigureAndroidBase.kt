package com.tezov.plugin_project.config

import com.android.build.api.dsl.AndroidSourceSet
import com.android.build.api.dsl.Packaging
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project

internal abstract class ConfigureAndroidBase(
    val project: Project,
    val configExtension: ConfigExtension,
) {

    fun apply() {
        onApply()

        project.afterEvaluate {
            afterEvaluated()
            configExtension.executeBeforeVariant()
        }
        project.gradle.projectsEvaluated {
            afterAllEvaluated()
            configExtension.executeWhenEvaluated()
        }
        project.gradle.taskGraph.whenReady {
            configExtension.initBuildType(allTasks)
            whenReady()
            configExtension.executeWhenReady()
        }
    }

    abstract fun onApply()

    abstract fun afterEvaluated()

    abstract fun afterAllEvaluated()

    abstract fun whenReady()

    fun com.android.build.api.dsl.BuildType.buildConfigDebug(value:Boolean){
        buildConfigField(
            type = "boolean",
            name = "DEBUG_ONLY",
            value = value.toString()
        )
    }

    fun NamedDomainObjectContainer<out AndroidSourceSet>.configure(hasResources:Boolean, hasAssets:Boolean){
        getByName("main") {
            java.srcDir("src/main/java")
            kotlin.srcDir("src/main/kotlin")
        }
        getByName("debug") {
            java.srcDir("src/build_type/debug/java")
            kotlin.srcDir("src/build_type/debug/kotlin")
        }
        getByName("release") {
            java.srcDir("src/build_type/release/java")
            kotlin.srcDir("src/build_type/release/kotlin")
        }
        if(hasResources){
            getByName("main") {
                res.srcDir("src/res")
            }
            getByName("debug") {
                res.srcDir("src/build_type/debug/res")
            }
            getByName("release") {
                res.srcDir("src/build_type/release/res")
            }
        }
        if(hasAssets){
            getByName("main") {
                assets.srcDir("src/res")
            }
            getByName("debug") {
                assets.srcDir("src/build_type/debug/res")
            }
            getByName("release") {
                assets.srcDir("src/build_type/release/res")
            }
        }


    }

    fun Packaging.configure(){
        if (configExtension.configuration.excludeAllMetaInf) {
            resources {
                excludes.add("META-INF/**")
            }
        }
    }
}