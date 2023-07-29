package com.tezov.plugin_project

import com.tezov.plugin_project.Logger.logInfo
import com.tezov.plugin_project.config.ProjectConfigPlugin.Companion.ANDROID_PLUGIN_CLASSPATH
import org.gradle.api.Project
import org.gradle.util.GradleVersion
import java.io.File

object VersionCheck {

    private val MIN_VERSION_GRADLE = GradleVersion.version("8.0")
    private val MIN_VERSION_ANDROID = GradleVersion.version("8.0.2")

    private var androidPluginCurrentVersion:GradleVersion? = null

    fun gradle(project: Project, pluginName: String) {
        if (GradleVersion.current() < MIN_VERSION_GRADLE) {
            project.logInfo(
                "${project.name}: gradle ${MIN_VERSION_GRADLE.version} or greater is required by $pluginName."
            )
            return
        }
    }

    fun androidClasspath(project: Project, pluginName: String) {
        synchronized(VersionCheck){
            if(androidPluginCurrentVersion == null){
                val settingsGradle = File(project.rootDir, "settings.gradle.kts")
                if (settingsGradle.exists()) {
                    settingsGradle.forEachLine { line ->
                        val classpathMatch = Regex("""^\s*classpath\("(.+)"\)\s*$""").find(line)
                        classpathMatch?.groupValues?.get(1)?.let { dependency ->
                            if (dependency.startsWith(ANDROID_PLUGIN_CLASSPATH)) {
                                androidPluginCurrentVersion = kotlin.runCatching {
                                    GradleVersion.version(dependency.substringAfterLast(":"))
                                }.getOrNull()
                                return@forEachLine
                            }
                        }
                    }
                }
            }
            androidPluginCurrentVersion?.let {
                if (it < MIN_VERSION_ANDROID) {
                    project.logInfo(
                        "${project.name}: android classpath ${MIN_VERSION_ANDROID.version} or greater is required by $pluginName.")
                }
            } ?: kotlin.run {
                project.logInfo(
                    "${project.name}: failed to check android classpath version in settings.gradle.kts. Plugin $pluginName required min version $${MIN_VERSION_ANDROID.version}"
                )
            }
        }
    }

}