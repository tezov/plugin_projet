package com.tezov.plugin_project.config

import com.android.build.api.dsl.LibraryExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import com.tezov.plugin_project.PropertyDelegate
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

open class ConfigExtension @Inject constructor(
    factory: ObjectFactory,
    private val project: Project
) {

    open class Debug {
        var keepLog by PropertyDelegate { true }
        var keepSourceFile by PropertyDelegate { true }
        var repackage by PropertyDelegate { false }
        var obfuscate by PropertyDelegate { false }
        var minify by PropertyDelegate { false }
        var hasJUnitRunner by PropertyDelegate { false }
    }

    open class Release {
        var enableDebug by PropertyDelegate { false }
        var obfuscate by PropertyDelegate { true }
        var minify by PropertyDelegate { true }
    }

    open class Configuration {
        var domain by PropertyDelegate<String>()
        val proguardPaths = mutableListOf<String>()
        val proguardConsumerPaths = mutableListOf<String>()
        val languages = mutableListOf<String>()
        var hasResources by PropertyDelegate { false }
        var hasAssets by PropertyDelegate { false }
    }

    open class Version {
        var major by PropertyDelegate { 0 }
        var minor by PropertyDelegate { 0 }
        var patch by PropertyDelegate { 0 }
        var alpha by PropertyDelegate<Int?> { null }
        var releaseCandidate by PropertyDelegate<Int?> { null }
        val name
            get() = "$major.$minor.$patch".let {
                if (alpha != null && releaseCandidate != null) {
                    throw GradleException("can't be alpha and releaseCandidate...")
                }
                when {
                    alpha != null -> "$it-alpha.${alpha}"
                    releaseCandidate != null -> "${it}RC.${releaseCandidate}"
                    else -> it
                }
            }
        val value get() = major * 10000 + minor * 100 + patch
    }

    open class Build {
        fun BuildType.suffix(value: String) {
            suffix = value
        }

        var type by PropertyDelegate<BuildType>()
            internal set
    }

    open class Lint {
        var abortOnError by PropertyDelegate { false }
        var checkReleaseBuilds by PropertyDelegate { false }
        val disable = mutableListOf<String>()
    }

    var debug = factory.newInstance(Debug::class.java)
        private set
    var release = factory.newInstance(Release::class.java)
        private set
    var configuration = factory.newInstance(Configuration::class.java)
        private set
    var version = factory.newInstance(Version::class.java)
        private set
    internal var build = factory.newInstance(Build::class.java)
    var lint = factory.newInstance(Lint::class.java)
        private set

    var beforeVariant: ((buildType: BuildType) -> Unit)? = null
        private set(value) {
            field?.let {
                throw GradleException("beforeVariant can be used only once")
            }
            field = value
        }
    var whenEvaluated: ((buildType: BuildType) -> Unit)? = null
        private set(value) {
            field?.let {
                throw GradleException("beforeVariant can be used only once")
            }
            field = value
        }
    var whenReady: ((buildType: BuildType) -> Unit)? = null
        private set(value) {
            field?.let {
                throw GradleException("beforeVariant can be used only once")
            }
            field = value
        }

    internal fun initBuildType(graphTasks: List<Task>) {
        val taskPreDebugBuild =
            graphTasks.find { task -> task.name == BuildType.DEBUG.preBuildName() }
        val taskPreReleaseBuild =
            graphTasks.find { task -> task.name == BuildType.RELEASE.preBuildName() }
        if (taskPreDebugBuild != null && taskPreReleaseBuild != null) {
            throw GradleException("Debug and Release task found...")
        }
        when {
            taskPreDebugBuild != null -> build.type = BuildType.DEBUG
            taskPreReleaseBuild != null -> build.type = BuildType.RELEASE
            else -> build.type = BuildType.UNKNOWN

        }
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
        beforeVariant = block
    }

    fun whenEvaluated(block: (buildType: BuildType) -> Unit) {
        whenEvaluated = block
    }

    fun whenReady(block: (buildType: BuildType) -> Unit) {
        whenReady = block
    }

    companion object {
        private const val ANDROID_PLUGIN_NAME = "android"
    }

    fun configureAndroidPlugin() {
        val androidExtension = kotlin.runCatching {
            project.extensions.findByName(ANDROID_PLUGIN_NAME)
        }.getOrNull()?.takeIf {
            it is LibraryExtension || it is BaseAppModuleExtension
        }
        androidExtension ?: return
        when (androidExtension) {
            is BaseAppModuleExtension -> ConfigureAndroidApp(
                project = project,
                configExtension = this,
                androidExtension = androidExtension
            )

            is LibraryExtension -> ConfigureAndroidLibrary(
                project = project,
                configExtension = this,
                androidExtension = androidExtension
            )

            else -> null
        }?.apply()
    }

    private fun ((buildType: BuildType) -> Unit).execute() {
        BuildType.values().filter { it != BuildType.UNKNOWN }.forEach {
            invoke(it)
        }
    }

    internal fun executeBeforeVariant() {
        beforeVariant?.execute()
    }

    internal fun executeWhenEvaluated() {
        whenEvaluated?.execute()
    }

    internal fun executeWhenReady() {
        whenReady?.execute()
    }

}