package com.tezov.plugin_project.config

import com.android.build.api.dsl.AndroidSourceSet
import com.tezov.plugin_project.Logger.PLUGIN_CONFIG
import com.tezov.plugin_project.Logger.log
import com.tezov.plugin_project.Logger.throwException
import com.tezov.plugin_project.Utils.normalizePath
import com.tezov.plugin_project.config.ExtensionCommon.Companion.invoke
import com.tezov.plugin_project.config.Proguard.PlaceHolder
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.Task
import java.io.File


internal class ConfigureAndroidCommon(
    val project: Project,
    val protocol: Protocol,
) {

    interface Protocol {
        val commonExtension: ExtensionCommon
        fun initCurrentBuildType(graphTasks: List<Task>)
        fun afterEvaluated()
        fun afterAllEvaluated()
        fun whenReady()

        fun proguardAdd(
            buildType: com.android.build.api.dsl.BuildType,
            element: File,
            placeholders: Map<String, String>? = null
        )
        fun proguardAddAll(
            buildType: com.android.build.api.dsl.BuildType,
            element: Collection<File>
        )
    }

    fun apply() {
        project.afterEvaluate {
            protocol.afterEvaluated()
            protocol.commonExtension.beforeVariant?.invoke()
        }
        project.gradle.projectsEvaluated {
            protocol.afterAllEvaluated()
            protocol.commonExtension.whenEvaluated?.invoke()
        }
        project.gradle.taskGraph.whenReady {
            protocol.initCurrentBuildType(allTasks)
            protocol.whenReady()
            protocol.commonExtension.whenReady?.invoke()
        }
    }

    fun buildConfig(
        buildType: com.android.build.api.dsl.BuildType,
        key: BuildConfig,
        value: Boolean
    ) {
        buildType.buildConfigField(
            type = key.type,
            name = key.name,
            value = value.toString()
        )
    }

    fun sourceSet(
        sourceSets: NamedDomainObjectContainer<out AndroidSourceSet>,
        hasResources: Boolean,
        hasAssets: Boolean
    ) {
        with(sourceSets) {
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
            if (hasResources) {
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
            if (hasAssets) {
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
    }

    fun proguard(
        buildTypes: NamedDomainObjectContainer<out com.android.build.api.dsl.BuildType>,
        proguards: Collection<File>,
        keepProguardsDebug: Collection<File>,
        keepProguardsRelease: Collection<File>,
        enableDebug: Boolean,
        keepProguard: Boolean,
        repackage: Boolean,
        repackageName: String,
        keepSourceFile: Boolean,
        obfuscate: Boolean,
    ) {
        buildTypes.getByName("release") {
            if (enableDebug) {
                PLUGIN_CONFIG.log(project,"************** DEBUG IS ACTIVE ON RELEASE **************")
            }
            protocol.proguardAddAll(this, proguards)
            if (enableDebug) {
                if (keepProguard) {
                    proguardAdd(this, Proguard.File.DEBUG_KEEP)
                    protocol.proguardAddAll(this, keepProguardsDebug)
                } else {
                    proguardAdd(this, Proguard.File.DEBUG_REMOVE)
                    protocol.proguardAddAll(this, keepProguardsRelease)
                }
                if (repackage) {
                    proguardAdd(
                        this,
                        Proguard.File.REPACKAGE,
                        mapOf(PlaceHolder.REPACKAGE_NAME.value to repackageName)
                    )
                    if (keepSourceFile) {
                        proguardAdd(this, Proguard.File.KEEP_SOURCE_FILE)
                    }
                }
                if (!obfuscate) {
                    proguardAdd(this, Proguard.File.DISALLOW_OBFUSCATION)
                }
            } else {
                proguardAdd(
                    this,
                    Proguard.File.REPACKAGE,
                    mapOf(PlaceHolder.REPACKAGE_NAME.value to repackageName)
                )
                proguardAdd(this, Proguard.File.DEBUG_REMOVE)
                protocol.proguardAddAll(this, keepProguardsRelease)
            }
        }
    }

    fun proguardAdd(buildType: com.android.build.api.dsl.BuildType, element: Proguard.File, placeholders: Map<String, String>? = null) {
        val tmpDir = File("${project.buildDir}/tmp/${ProjectConfigPlugin.CONFIG_PLUGIN_ID}/".normalizePath)
        if(!tmpDir.exists()) tmpDir.mkdirs()
        val proguardFile = File(tmpDir, "${element.name.lowercase()}.pro".normalizePath)
        if(!proguardFile.exists()){
            element.invoke(placeholders)?.let {
                try {
                    proguardFile.createNewFile()
                    proguardFile.outputStream().use { output ->
                        it.use { input -> input.copyTo(output) }
                    }
                } catch (e: Throwable) {
                    PLUGIN_CONFIG.throwException(project,"error when add proguard file ${element.name}")
                }
            }
        }
        protocol.proguardAdd(buildType, proguardFile)
    }

}