package com.tezov.plugin_project.config

import com.android.build.api.dsl.ApplicationExtension
import com.tezov.plugin_project.Logger
import com.tezov.plugin_project.Logger.PLUGIN_CONFIG
import com.tezov.plugin_project.Logger.throwException
import com.tezov.plugin_project.PropertyDelegate
import com.tezov.plugin_project.config.ProjectConfigPlugin.Companion.ANDROID_EXTENSION_NAME
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.model.ObjectFactory
import java.io.File
import javax.inject.Inject

open class ExtensionApp @Inject constructor(
    factory: ObjectFactory,
    private val project: Project
) {

    open class Debug {
        var keepProguard by PropertyDelegate { true }
        var keepSourceFile by PropertyDelegate { true }
        var repackage by PropertyDelegate { false }
        var obfuscate by PropertyDelegate { false }
        var minify by PropertyDelegate { false }
        var hasJUnitRunner by PropertyDelegate { false }
    }

    open class Release {
        var enableDebug by PropertyDelegate { false }
        var repackageName by PropertyDelegate { "tezov" }
        val proguards = mutableListOf<File>()
        val keepProguardsDebug = mutableListOf<File>()
        val keepProguardsRelease = mutableListOf<File>()
    }

    open class Configuration {
        var domain by PropertyDelegate<String>()
        var subDomain by PropertyDelegate<String?>{ null }
        val languages = mutableListOf<String>()
        var hasResources by PropertyDelegate { false }
        var hasAssets by PropertyDelegate { false }
    }

    open class Version @Inject constructor(
        private val project: Project
    ) {
        var major by PropertyDelegate { 0 }
        var minor by PropertyDelegate { 0 }
        var patch by PropertyDelegate { 0 }
        var alpha by PropertyDelegate<Int?> { null }
        var beta by PropertyDelegate<Int?> { null }
        var rc by PropertyDelegate<Int?> { null }
        val name
            get() = "$major.$minor.$patch".let {
                var notNull = 0
                if (alpha != null) notNull++
                if (beta != null) notNull++
                if (rc != null) notNull++
                if (notNull > 1) {
                    PLUGIN_CONFIG.throwException(project,"can't be alpha, beta and rc at the same time")
                }
                when {
                    alpha != null -> "$it-alpha.${alpha}"
                    beta != null -> "$it-beta.${beta}"
                    rc != null -> "${it}rc.${rc}"
                    else -> it
                }
            }
        val value get() = major * 10000 + minor * 100 + patch
    }

    open class Build {
        fun BuildType.suffix(value: String) {
            suffix = value
        }

        var currentType by PropertyDelegate<BuildType>()
            internal set
    }

    open class Lint {
        var abortOnError by PropertyDelegate { false }
        var checkReleaseBuilds by PropertyDelegate { false }
        var checkDependencies by PropertyDelegate { false }
    }

    internal val common = ExtensionCommon(project)
    val debug = factory.newInstance(Debug::class.java)
    val release = factory.newInstance(Release::class.java)
    val configuration = factory.newInstance(Configuration::class.java)
    val version = factory.newInstance(Version::class.java)
    internal val build = factory.newInstance(Build::class.java)
    val lint = factory.newInstance(Lint::class.java)

    internal val nameSpace get() = kotlin.runCatching { common.nameSpace(this, true) }.getOrNull() ?: run {
        PLUGIN_CONFIG.throwException(project,"nameSpace is not ready yet")
    }
    val applicationId get() = kotlin.runCatching { common.applicationId(this, true) }.getOrNull() ?: run {
        PLUGIN_CONFIG.throwException(project,"applicationId is not ready yet")
    }
    val packageName get() = kotlin.runCatching { common.packageName(this) }.getOrNull() ?: run {
        PLUGIN_CONFIG.throwException(project,"packageName is not ready yet")
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

    fun version(block: Version.() -> Unit) {
        version.block()
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
        val androidExtensionApp = (project.extensions.findByName(ANDROID_EXTENSION_NAME) as? ApplicationExtension) ?: kotlin.run {
            PLUGIN_CONFIG.throwException(project,"android plugin application not found")
        }
        ConfigureAndroidApp(
            project = project,
            configExtension = this,
            androidExtension = androidExtensionApp
        ).apply()
    }

}