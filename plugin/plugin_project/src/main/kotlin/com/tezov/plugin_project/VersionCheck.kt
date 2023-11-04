package com.tezov.plugin_project

import com.tezov.plugin_project.Logger.log
import com.tezov.plugin_project.config.ProjectConfigPlugin.Companion.ANDROID_PLUGIN_CLASSPATH
import org.gradle.api.Project
import org.gradle.util.GradleVersion
import java.io.File

object VersionCheck {

    private val MIN_VERSION_GRADLE = GradleVersion.version("8.0")
    private val MIN_VERSION_ANDROID = GradleVersion.version("8.0.2")
    private const val NO_VERSION_ANDROID = "0.0.0"

    private var androidPluginCurrentVersion:GradleVersion? = null

    fun gradle(project: Project, pluginType: Logger.PluginType, pluginId: String) {
        if (GradleVersion.current() < MIN_VERSION_GRADLE) {
            project.log(
                pluginType, "gradle ${MIN_VERSION_GRADLE.version} or greater is required by $pluginId."
            )
            return
        }
    }

    fun androidClasspath(project: Project, pluginType: Logger.PluginType, pluginId: String) {
        synchronized(VersionCheck){
            if(androidPluginCurrentVersion == null){
                val settingsGradle = File(project.rootDir, "settings.gradle.kts")
                if (settingsGradle.exists()) {
                    settingsGradle.forEachLine { line ->
                        val classpathMatch = Regex("""^\s*classpath\("(.+)"\)\s*$""").find(line)
                        classpathMatch?.groupValues?.get(1)?.let { dependency ->
                            if (dependency.startsWith(ANDROID_PLUGIN_CLASSPATH)) {
                                androidPluginCurrentVersion = kotlin.runCatching {
                                    GradleVersion.version(dependency.substringAfterLast(":", NO_VERSION_ANDROID))
                                }.getOrNull()
                                return@forEachLine
                            }
                        }
                    }
                }
            }
            androidPluginCurrentVersion?.let {
                if (it < MIN_VERSION_ANDROID) {
                    project.log(
                        pluginType, "android classpath ${MIN_VERSION_ANDROID.version} or greater is required by $pluginId.")
                }
            } ?: kotlin.run {
                project.log(
                    pluginType, "failed to check android classpath version in settings.gradle.kts. Plugin $pluginId required min version $${MIN_VERSION_ANDROID.version}"
                )
            }
        }
    }

}