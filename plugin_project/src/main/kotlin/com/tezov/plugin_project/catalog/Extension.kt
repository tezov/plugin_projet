package com.tezov.plugin_project.catalog

import com.tezov.plugin_project.Logger.logInfo
import com.tezov.plugin_project.Logger.throwException
import com.tezov.plugin_project.PropertyDelegate
import com.tezov.plugin_project.catalog.CatalogMap.Companion.DEFAULT_THROW
import com.tezov.plugin_project.catalog.CatalogMap.Companion.KEY_SEPARATOR
import com.tezov.plugin_project.catalog.CatalogProjectExtension.FileFormat.Companion.format
import com.tezov.plugin_project.catalog.ProjectCatalogPlugin.Companion.CATALOG_EXTENSION_NAME
import com.tezov.plugin_project.catalog.ProjectCatalogPlugin.Companion.CATALOG_PLUGIN_ID
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.initialization.Settings
import org.gradle.kotlin.dsl.configure
import java.net.URL
import javax.inject.Inject
import java.nio.file.Path


open class CatalogScope internal constructor(
    private val project: Project,
    private val keyBase: String
) {

    internal lateinit var catalog: CatalogMap

    private fun String.isValid() = when {
        keyBase.isBlank() -> true
        else -> startsWith(keyBase) && getOrNull(keyBase.length).let { it == null || it == KEY_SEPARATOR }
    }

    internal fun String.absolute() = when {
        keyBase.isBlank() -> this
        isNotBlank() -> "$keyBase.$this"
        else -> keyBase
    }

    private fun String.relative() = if (isNotBlank()) {
        replaceFirst(keyBase, "").dropWhile { it == KEY_SEPARATOR }
    } else ""

    fun with(key: String, isKeyAbsolute: Boolean = false, block: CatalogScope.() -> Unit) =
        CatalogScope(
            project = project,
            keyBase = if (isKeyAbsolute) key else key.absolute(),
        ).also { it.catalog = catalog }.block()

    val keys get() = catalog.keys

    val values get() = catalog.values

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

    fun checkDependenciesVersion(
        ignore_alpha: Boolean = false,
        ignore_beta: Boolean = false,
        ignore_rc: Boolean = false
    ) {
        forEach { key, _ ->
            val dependencyFullName = string(key).lowercase()
            val indexOfVersionSeparator = dependencyFullName.lastIndexOf(':')
            if (indexOfVersionSeparator != -1) {
                val dependencyName = dependencyFullName.substring(0, indexOfVersionSeparator)
                val dependencyVersion = dependencyFullName.substring(indexOfVersionSeparator + 1)
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
                    }
                }
            }

        }
    }
}

open class CatalogProjectExtension @Inject constructor(
    internal val project: Project,
) : CatalogScope(project = project, keyBase = "") {

    interface CatalogFile {
        val format: FileFormat
        val data: String
    }

    enum class FileFormat(val extension: String) {
        Json("json"), Yaml("yaml"), Toml("toml");

        companion object {

            fun throwExceptionUnsupportedFormat(project: Project, message: String): Nothing =
                project.throwException("$message Only .json / .yaml / .toml are supported")

            inline val String.extension get() = substringAfterLast('.', "")

            inline val String.format
                get() = FileFormat.values().find { it.extension == extension }
        }
    }

    var catalogFile by PropertyDelegate<CatalogFile?> { null }
    var catalogType by PropertyDelegate<FileFormat?> { null }

    fun catalogFromFile(path: String) = catalogFromFile(
        path = Path.of(path)
    )

    fun catalogFromFile(path: Path) = object : CatalogFile {
        override val format: FileFormat
            get() = path.toString().format
                ?: FileFormat.throwExceptionUnsupportedFormat(project, "Couldn't resolve file format $path")

        override val data: String
            get() = path.toFile().also {
                if (!it.exists() || !it.isFile) {
                    project.throwException("catalog file not found")
                }
            }.readText()
    }

    fun catalogFromUrl(href: String) = catalogFromUrl(
        href = URL(href)
    )

    fun catalogFromUrl(href: URL) = object : CatalogFile {
        override val format: FileFormat
            get() = href.path.format
            ?: FileFormat.throwExceptionUnsupportedFormat(project, "Couldn't resolve file format $href")

        override val data: String
            get() = href.readText()
    }

    fun catalogFromString(data: String, format: FileFormat) = object : CatalogFile {
        override val format: FileFormat get() = format
        override val data: String
            get() = data
    }

    fun configureProjects() {
        buildRawCatalog()
        applyProjectsPlugin()
    }

    private fun buildRawCatalog() {
        val uri = catalogFile ?: run {
            project.throwException("catalog path is null")
        }
        catalog = CatalogMap(
            extension = this,
            uri = uri,
        )
    }

    private fun applyProjectsPlugin() {
        project.allprojects.filter { it !== project }.forEach { module ->
            module.plugins.apply(CATALOG_PLUGIN_ID)
            runCatching {
                module.extensions.findByName(CATALOG_EXTENSION_NAME) as? CatalogModuleExtension
            }.getOrNull()?.let {
                it.catalog = catalog
            } ?: run {
                project.throwException("catalog plugin not successfully apply to ${project.name}")
            }
            stringListOrNull(key = module.name)?.forEach { plugin ->
                module.plugins.apply(plugin)
            }
        }
    }

}

open class CatalogModuleExtension @Inject internal constructor(
    project: Project
) : CatalogScope(project = project, keyBase = "")



