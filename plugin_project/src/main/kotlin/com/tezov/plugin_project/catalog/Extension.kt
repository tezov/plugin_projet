package com.tezov.plugin_project.catalog

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.tezov.plugin_project.Logger.logInfo
import com.tezov.plugin_project.Logger.throwException
import com.tezov.plugin_project.PropertyDelegate
import com.tezov.plugin_project.catalog.CatalogScope.Companion.DEFAULT_INT
import com.tezov.plugin_project.catalog.CatalogScope.Companion.DEFAULT_JAVA_VERSION
import com.tezov.plugin_project.catalog.CatalogScope.Companion.DEFAULT_STRING
import com.tezov.plugin_project.catalog.CatalogScope.Companion.DEFAULT_STRING_LIST
import com.tezov.plugin_project.catalog.ProjectCatalogPlugin.Companion.CATALOG_EXTENSION_NAME
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import java.io.File
import java.net.URL
import javax.inject.Inject

open class CatalogScope(
    private val project: Project,
    private val keyBase: String,
    private val delegate: CatalogRootExtension,
) {

    internal companion object {

        val DEFAULT_STRING = { key: String ->
            throw IndexOutOfBoundsException("key $key not found")
        }
        val DEFAULT_STRING_LIST = { key: String ->
            throw IndexOutOfBoundsException("key $key not found")
        }
        val DEFAULT_INT = { key: String ->
            throw IndexOutOfBoundsException("key $key not found")
        }
        val DEFAULT_JAVA_VERSION = { key: String ->
            throw IndexOutOfBoundsException("key $key not found")
        }

    }

    private fun String.isValid() =
        startsWith(keyBase) && getOrNull(keyBase.length).let { it == null || it == '.' }

    private fun String.absolute() = if (isNotBlank()) "$keyBase.$this" else keyBase

    private fun String.relative() = if (isNotBlank()) {
        replaceFirst(keyBase, "").dropWhile { it == '.' }
    } else ""

    fun checkDependenciesVersion(
        ignore_alpha: Boolean = false,
        ignore_beta: Boolean = false,
        ignore_rc: Boolean = false
    ) {
        forEach { key, _ ->
            val dependencyFullName = string(key).lowercase()
            val indexOfVersionSeparator = dependencyFullName.lastIndexOf(':')
            if (indexOfVersionSeparator == -1) {
                if (delegate.verboseCheckDependenciesVersion) {
                    project.logInfo("${key.absolute()}: version invalid $dependencyFullName")
                }
            } else {
                val dependencyName = dependencyFullName.substring(0, indexOfVersionSeparator)
                val dependencyVersion = dependencyFullName.substring(indexOfVersionSeparator + 1)
                if (delegate.verboseCheckDependenciesVersion) {
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
                        if (delegate.verboseCheckDependenciesVersion) {
                            project.logInfo("${key.absolute()}: $dependencyName latest version not found in")
                            resolvedVersions.forEach {
                                project.logInfo(">> ${it.id.displayName}")
                            }
                        }
                    }
                }.onFailure {
                    if (delegate.verboseCheckDependenciesVersion) {
                        project.logInfo("${key.absolute()}:$dependencyFullName failed to retrieve latest version")
                    }
                }
            }

        }
    }

    fun with(key: String, block: CatalogScope.() -> Unit) =
        delegate.with(key = key.absolute(), block = block)

    fun filter(predicate: (key: String) -> Boolean) = delegate.filter {
        it.isValid() && predicate(it.relative())
    }.mapKeys {
        it.key.relative()
    }

    fun forEach(block: (key: String, value: String) -> Unit) = delegate.filter {
        it.isValid()
    }.mapKeys {
        it.key.relative()
    }.forEach {
        block(it.key, it.value)
    }

    fun stringOrNull(key: String) =
        delegate.stringOrNull(key = key)

    fun stringListOrNull(key: String) =
        delegate.stringListOrNull(key = key)

    fun intOrNull(key: String) = delegate.intOrNull(key = key)

    fun javaVersionOrNull(key: String) = delegate.javaVersionOrNull(key = key)


    fun string(key: String = "", default: (key: String) -> String = DEFAULT_STRING) =
        delegate.string(key = key.absolute(), default = default)

    fun stringList(key: String = "", default: (key: String) -> List<String> = DEFAULT_STRING_LIST) =
        delegate.stringList(key = key.absolute(), default = default)

    fun int(key: String = "", default: (key: String) -> Int = DEFAULT_INT) =
        delegate.int(key = key.absolute(), default = default)

    fun javaVersion(
        key: String = "",
        default: (key: String) -> JavaVersion = DEFAULT_JAVA_VERSION
    ) = delegate.javaVersion(key = key.absolute(), default = default)


    inline val String.stringOrNull get() = stringOrNull(key = this)
    inline val String.stringListOrNull get() = stringListOrNull(key = this)
    inline val String.intOrNull get() = intOrNull(key = this)
    inline val String.javaVersionOrNull get() = javaVersionOrNull(key = this)

    inline val String.string get() = string(key = this)
    inline val String.stringList get() = stringList(key = this)
    inline val String.int get() = int(key = this)
    inline val String.javaVersion get() = javaVersion(key = this)

}

interface JsonFile {
    val data: String
}

open class CatalogRootExtension @Inject constructor(
    private val project: Project
) {
    var verboseCatalogBuild by PropertyDelegate { false }
    var verbosePluginApply by PropertyDelegate { false }
    var verboseReadValue by PropertyDelegate { false }
    var verboseCheckDependenciesVersion by PropertyDelegate { false }

    var jsonFile by PropertyDelegate<JsonFile?> { null }
    private val rawCatalog = mutableMapOf<String, String>()

    fun jsonFromFile(path: String) = object : JsonFile {
        override val data: String
            get() = File(path).also {
                if (!it.exists() || !it.isFile) {
                    project.throwException("catalog file not found")
                }
                if (verboseCatalogBuild) project.logInfo("retrieve json catalog from file $path")
            }.readText()
    }

    fun jsonFromUrl(href: String) = object : JsonFile {
        override val data: String
            get() = URL(href).also {
                if (verboseCatalogBuild) project.logInfo("retrieve json catalog from url $href")
            }.readText()
    }

    fun jsonFromString(data: String) = object : JsonFile {
        override val data: String
            get() = data.also {
                if (verboseCatalogBuild) project.logInfo("retrieve json catalog from string")
            }
    }

    fun configureProjects() {
        buildRawCatalog()
        applyProjectsPlugin()
    }

    private fun buildRawCatalog() {
        val uri = this.jsonFile ?: run {
            project.throwException("catalog path is null")
        }
        if (verboseCatalogBuild) project.logInfo("Read catalog json : $uri")
        val objectMapper = ObjectMapper()
        val inputMap = objectMapper.readValue(
            uri.data,
            object : TypeReference<Map<String, Any>>() {}
        )
        flattenMap(inputMap, rawCatalog)
        if (verboseCatalogBuild) {
            rawCatalog.forEach { key, value ->
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
                module.extensions.findByName(CATALOG_EXTENSION_NAME) as? CatalogExtension
            }.getOrNull()?.delegate(this) ?: run {
                project.throwException("catalog plugin not successfully apply to ${project.name}")
            }
            stringList(key = module.name, default = { emptyList() })
                .takeIf { it.isNotEmpty() }?.let { plugins ->
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

    private fun flattenMap(
        inputMap: Map<String, Any>,
        outputMap: MutableMap<String, String>,
        parentKey: String = "",
    ) {
        for ((key, value) in inputMap) {
            val newKey = if (parentKey.isNotEmpty()) "$parentKey.$key" else key
            @Suppress("UNCHECKED_CAST")
            when (value) {
                is Map<*, *> -> flattenMap(value as Map<String, Any>, outputMap, newKey)
                is List<*> -> outputMap[newKey] = value.joinToString(",") { it.toString().trim() }
                is Number -> outputMap[newKey] = value.toString()
                else -> outputMap[newKey] = value.toString().trim()
            }
        }
    }

    fun filter(
        predicate: (key: String) -> Boolean
    ) = rawCatalog.filter { predicate(it.key) }

    fun forEach(block: (key: String, value: String) -> Unit) = rawCatalog.forEach {
        block(it.key, it.value)
    }

    fun with(key: String, block: CatalogScope.() -> Unit) = CatalogScope(
        project = project,
        keyBase = key,
        delegate = this
    ).block()

    fun stringOrNull(key: String): String? {
        val value = rawCatalog[key] ?: run {
            if (verboseReadValue) project.logInfo("value not found for key: $key")
            return null
        }
        if (verboseReadValue) project.logInfo("key: $key | value: $value")
        if (!value.contains('$')) return value
        if (verboseReadValue) project.logInfo("key: $key | value contains placeholder, start replacement...")
        val regexValue = Regex("""(\$\{(.*?)\})""")
        val valueBuilder = StringBuilder(value)
        var indexOffset = 0
        for (it in regexValue.findAll(value)) {
            if (it.groups.size < 3) continue
            //rebuild absolute place holder key
            val placeHolderKeyEnd = it.groups[2]?.value ?: continue
            val placeHolderKeyEndDotCount = placeHolderKeyEnd.count { it == '.' } + 1
            val placeHolderKeyStart = key.split('.').let {
                it.subList(0, (it.size - placeHolderKeyEndDotCount).coerceAtLeast(0))
                    .joinToString(".")
            }
            val placeHolderKey = if (placeHolderKeyStart.isNotBlank()) {
                "$placeHolderKeyStart.$placeHolderKeyEnd"
            } else {
                placeHolderKeyEnd
            }
            if (verboseReadValue) project.logInfo("absolute placeHolderKey : $placeHolderKey")
            //recurse retrieve placeholder holder value
            val placeHolderValue = stringOrNull(placeHolderKey) ?: kotlin.run {
                project.throwException("placeholder key $placeHolderKey not found for key $key with value $value")
            }
            //replace placeholder by placeholder value in value
            it.groups[1]?.range?.let {
                valueBuilder.replace(
                    (it.first + indexOffset),
                    (it.last + indexOffset + 1),
                    placeHolderValue
                )
                indexOffset += (placeHolderValue.length - it.count())
            }
        }
        if (verboseReadValue) project.logInfo(
            "... end replacement -> key: $key | value: $valueBuilder"
        )
        return valueBuilder.toString()
    }

    fun stringListOrNull(key: String): List<String>? = stringOrNull(key = key)?.split(",")

    fun intOrNull(key: String): Int? = stringOrNull(key = key)?.toIntOrNull()

    fun javaVersionOrNull(key: String): JavaVersion? = stringOrNull(key = key)?.let { value ->
        JavaVersion.values().find { it.name == value }
    }


    fun string(key: String, default: (key: String) -> String = DEFAULT_STRING) =
        stringOrNull(key) ?: default(key)

    fun stringList(
        key: String,
        default: (key: String) -> List<String> = DEFAULT_STRING_LIST
    ) = stringListOrNull(key = key) ?: default(key)

    fun int(key: String, default: (key: String) -> Int = DEFAULT_INT) =
        intOrNull(key) ?: default(key)

    fun javaVersion(
        key: String,
        default: (key: String) -> JavaVersion = DEFAULT_JAVA_VERSION
    ): JavaVersion = javaVersionOrNull(key) ?: default(key)


    inline val String.stringOrNull get() = stringOrNull(key = this)
    inline val String.stringListOrNull get() = stringListOrNull(key = this)
    inline val String.intOrNull get() = intOrNull(key = this)
    inline val String.javaVersionOrNull get() = javaVersionOrNull(key = this)

    inline val String.string get() = string(key = this)
    inline val String.stringList get() = stringList(key = this)
    inline val String.int get() = int(key = this)
    inline val String.javaVersion get() = javaVersion(key = this)

}

open class CatalogExtension {
    private lateinit var delegate: CatalogRootExtension

    internal fun delegate(catalog: CatalogRootExtension) {
        delegate = catalog
    }

    fun with(key: String, block: CatalogScope.() -> Unit) = delegate.with(key = key, block = block)

    fun filter(predicate: (key: String) -> Boolean) =
        delegate.filter(predicate = predicate)

    fun forEach(block: (key: String, value: String) -> Unit) = delegate.forEach(block = block)

    fun stringOrNull(key: String) =
        delegate.stringOrNull(key = key)

    fun stringListOrNull(key: String) =
        delegate.stringListOrNull(key = key)

    fun intOrNull(key: String) = delegate.intOrNull(key = key)

    fun javaVersionOrNull(key: String) = delegate.javaVersionOrNull(key = key)


    fun string(key: String, default: (key: String) -> String = DEFAULT_STRING) =
        delegate.string(key = key, default = default)

    fun stringList(key: String, default: (key: String) -> List<String> = DEFAULT_STRING_LIST) =
        delegate.stringList(key = key, default = default)

    fun int(key: String, default: (key: String) -> Int = DEFAULT_INT) = delegate.int(
        key = key,
        default = default
    )

    fun javaVersion(key: String, default: (key: String) -> JavaVersion = DEFAULT_JAVA_VERSION) =
        delegate.javaVersion(key = key, default = default)


    inline val String.stringOrNull get() = stringOrNull(key = this)
    inline val String.stringListOrNull get() = stringListOrNull(key = this)
    inline val String.intOrNull get() = intOrNull(key = this)
    inline val String.javaVersionOrNull get() = javaVersionOrNull(key = this)

    inline val String.string get() = string(key = this)
    inline val String.stringList get() = stringList(key = this)
    inline val String.int get() = int(key = this)
    inline val String.javaVersion get() = javaVersion(key = this)


}