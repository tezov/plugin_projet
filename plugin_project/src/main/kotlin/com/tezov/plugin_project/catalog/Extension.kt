package com.tezov.plugin_project.catalog

import com.tezov.plugin_project.Logger.logInfo
import com.tezov.plugin_project.Logger.throwException
import com.tezov.plugin_project.PropertyDelegate
import com.tezov.plugin_project.catalog.CatalogMap.Companion.DEFAULT_THROW
import com.tezov.plugin_project.catalog.CatalogProjectExtension.FileFormat.Companion.format
import com.tezov.plugin_project.catalog.ProjectCatalogPlugin.Companion.CATALOG_EXTENSION_NAME
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import java.io.File
import java.net.URL
import javax.inject.Inject


open class CatalogScope internal constructor(
    private val project: Project,
    private val keyBase: String
) {

    internal lateinit var catalog: CatalogMap

    private fun String.isValid() = when {
        keyBase.isBlank() -> true
        else -> startsWith(keyBase) && getOrNull(keyBase.length).let { it == null || it == '.' }
    }

    internal fun String.absolute() = when {
        keyBase.isBlank() -> this
        isNotBlank() -> "$keyBase.$this"
        else -> keyBase
    }

    private fun String.relative() = if (isNotBlank()) {
        replaceFirst(keyBase, "").dropWhile { it == '.' }
    } else ""

    fun with(key: String, block: CatalogScope.() -> Unit) = CatalogScope(
        project = project,
        keyBase = key,
    ).also { it.catalog = catalog }.block()

    fun filter(predicate: (key: String) -> Boolean) = catalog.filter {
        it.isValid() && predicate(it.relative())
    }.mapKeys {
        it.key.relative()
    }

    fun forEach(block: (key: String, value: String) -> Unit) = catalog.filter {
        it.isValid()
    }.mapKeys {
        it.key.relative()
    }.forEach {
        block(it.key, it.value)
    }

    fun stringOrNull(key: String) =
        catalog.stringOrNull(key = key)

    fun stringListOrNull(key: String) =
        catalog.stringListOrNull(key = key)

    fun intOrNull(key: String) = catalog.intOrNull(key = key)

    fun javaVersionOrNull(key: String) = catalog.javaVersionOrNull(key = key)


    fun string(key: String = "", default: (key: String) -> String = DEFAULT_THROW) =
        catalog.string(key = key.absolute(), default = default)

    fun stringList(key: String = "", default: (key: String) -> List<String> = DEFAULT_THROW) =
        catalog.stringList(key = key.absolute(), default = default)

    fun int(key: String = "", default: (key: String) -> Int = DEFAULT_THROW) =
        catalog.int(key = key.absolute(), default = default)

    fun javaVersion(
        key: String = "",
        default: (key: String) -> JavaVersion = DEFAULT_THROW
    ) = catalog.javaVersion(key = key.absolute(), default = default)

    inline val String.stringOrNull get() = stringOrNull(key = this)
    inline val String.stringListOrNull get() = stringListOrNull(key = this)
    inline val String.intOrNull get() = intOrNull(key = this)
    inline val String.javaVersionOrNull get() = javaVersionOrNull(key = this)

    inline val String.string get() = string(key = this)
    inline val String.stringList get() = stringList(key = this)
    inline val String.int get() = int(key = this)
    inline val String.javaVersion get() = javaVersion(key = this)

}

open class CatalogProjectExtension @Inject constructor(
    internal val project: Project
) : CatalogScope(project = project, keyBase = "") {

    interface CatalogFile {
        val format: FileFormat
        val data: String
    }

    enum class FileFormat(val extension: String) {
        Json("json"), Yaml("yaml"), Toml("toml");

        companion object {
            inline val String.extension
                get(): String? {
                    val dotIndex = lastIndexOf('.')
                    return if (dotIndex > 0 && dotIndex < length - 1) {
                        substring(dotIndex + 1)
                    } else {
                        null
                    }
                }

            inline val String.format
                get() = extension?.lowercase()?.let { extension ->
                    FileFormat.values().find { it.extension == extension }
                }

        }


    }

    var verboseCatalogBuild by PropertyDelegate { false }
    var verbosePluginApply by PropertyDelegate { false }
    var verboseReadValue by PropertyDelegate { false }
    var verboseCheckDependenciesVersion by PropertyDelegate { false }

    var catalogFile by PropertyDelegate<CatalogFile?> { null }
    var catalogType by PropertyDelegate<FileFormat?> { null }

    fun catalogFromFile(path: String, format: FileFormat? = null) = object : CatalogFile {
        override val format: FileFormat
            get() = format ?: path.format
            ?: project.throwException("Couldn't resolve file format $path")

        override val data: String
            get() = File(path).also {
                if (!it.exists() || !it.isFile) {
                    project.throwException("catalog file not found")
                }
                if (verboseCatalogBuild) project.logInfo("retrieve catalog from file $path -> format is ${this.format}")
            }.readText()
    }

    fun catalogFromUrl(href: String, format: FileFormat? = null) = object : CatalogFile {
        override val format: FileFormat
            get() = format ?: href.format
            ?: project.throwException("Couldn't resolve file format $href")

        override val data: String
            get() = URL(href).also {
                if (verboseCatalogBuild) project.logInfo("retrieve json catalog from url $href -> format is ${this.format}")
            }.readText()
    }

    fun catalogFromString(data: String, format: FileFormat) = object : CatalogFile {
        override val format: FileFormat get() = format
        override val data: String
            get() = data.also {
                if (verboseCatalogBuild) project.logInfo("retrieve json catalog from string -> format is ${this.format}")
            }
    }

    fun configureProjects() {
        buildRawCatalog()
        applyProjectsPlugin()
    }

    private fun buildRawCatalog() {
        val uri = catalogFile ?: run {
            project.throwException("catalog path is null")
        }
        if (verboseCatalogBuild) project.logInfo("Read catalog")
        catalog = when (uri.format) {
            FileFormat.Json -> CatalogBuilder.json(
                extension = this,
                uri = uri
            )
            FileFormat.Yaml -> CatalogBuilder.yaml(
                extension = this,
                uri = uri
            )
            FileFormat.Toml -> CatalogBuilder.toml(
                extension = this,
                uri = uri
            )
        }
        if (verboseCatalogBuild) {
            catalog.forEach { key, value ->
                project.logInfo("$key :: $value")
            }
        }
    }

    private fun applyProjectsPlugin() {
        if (verbosePluginApply) project.logInfo("Project : ${project.name}")
        project.allprojects.filter { it !== project }.forEach { module ->
            if (verbosePluginApply) project.logInfo("Module : ${module.name}")
            if (verbosePluginApply) project.logInfo("apply plugin : ${ProjectCatalogPlugin.CATALOG_PLUGIN_ID}")
            module.plugins.apply(ProjectCatalogPlugin.CATALOG_PLUGIN_ID)
            kotlin.runCatching {
                module.extensions.findByName(CATALOG_EXTENSION_NAME) as? CatalogModuleExtension
            }.getOrNull()?.let {
                it.catalog = catalog
            } ?: run {
                project.throwException("catalog plugin not successfully apply to ${project.name}")
            }
            stringListOrNull(key = module.name)?.let { plugins ->
                plugins.forEach { plugin ->
                    if (verbosePluginApply) project.logInfo("apply plugin : $plugin")
                    module.plugins.apply(plugin)
                }
                if (verbosePluginApply) project.logInfo("** success **")
            } ?: run {
                if (verbosePluginApply) project.logInfo("!!! Warning... no plugins found in catalog")
            }
        }
    }

    fun checkDependenciesVersion(
        ignore_alpha: Boolean = false,
        ignore_beta: Boolean = false,
        ignore_rc: Boolean = false
    ) {
        forEach { key, _ ->
            val dependencyFullName = string(key).lowercase()
            val indexOfVersionSeparator = dependencyFullName.lastIndexOf(':')
            if (indexOfVersionSeparator == -1) {
                if (verboseCheckDependenciesVersion) {
                    project.logInfo("${key.absolute()}: version invalid $dependencyFullName")
                }
            } else {
                val dependencyName = dependencyFullName.substring(0, indexOfVersionSeparator)
                val dependencyVersion = dependencyFullName.substring(indexOfVersionSeparator + 1)
                if (verboseCheckDependenciesVersion) {
                    project.logInfo("${key.absolute()}:$dependencyFullName check")
                }
                kotlin.runCatching {
                    val resolvedVersions = project.configurations.detachedConfiguration(
                        project.dependencies.create("$dependencyName:+")
                    ).resolvedConfiguration.resolvedArtifacts
                    resolvedVersions.filter {
                        val displayName = it.id.componentIdentifier.displayName.lowercase()
                        val version = it.moduleVersion.id.version.lowercase()
                        displayName.startsWith(dependencyName)
                                && (!ignore_alpha || dependencyVersion.contains("alpha") || !version.contains(
                            "alpha"
                        ))
                                && (!ignore_beta || dependencyVersion.contains("beta") || !version.contains(
                            "beta"
                        ))
                                && (!ignore_rc || dependencyVersion.contains("rc") || !version.contains(
                            "rc"
                        ))
                    }.map { it.moduleVersion.id.version.lowercase() }.maxByOrNull { it }?.let {
                        if (it != dependencyVersion) {
                            project.logInfo("${key.absolute()}: can be updated from $dependencyVersion to $it")
                        }
                    } ?: run {
                        if (verboseCheckDependenciesVersion) {
                            project.logInfo("${key.absolute()}: $dependencyName latest version not found in")
                            resolvedVersions.forEach {
                                project.logInfo(">> ${it.id.displayName}")
                            }
                        }
                    }
                }.onFailure {
                    if (verboseCheckDependenciesVersion) {
                        project.logInfo("${key.absolute()}:$dependencyFullName failed to retrieve latest version")
                    }
                }
            }

        }
    }

}

open class CatalogModuleExtension @Inject internal constructor(
    project: Project
) : CatalogScope(project = project, keyBase = "")



