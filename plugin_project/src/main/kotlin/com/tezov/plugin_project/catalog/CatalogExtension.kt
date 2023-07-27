package com.tezov.plugin_project.catalog

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.tezov.plugin_project.PropertyDelegate
import com.tezov.plugin_project.catalog.CatalogScope.Companion.DEFAULT_INT
import com.tezov.plugin_project.catalog.CatalogScope.Companion.DEFAULT_JAVA_VERSION
import com.tezov.plugin_project.catalog.CatalogScope.Companion.DEFAULT_STRING
import com.tezov.plugin_project.catalog.CatalogScope.Companion.DEFAULT_STRING_LIST
import com.tezov.plugin_project.catalog.ProjectCatalogPlugin.Companion.CATALOG_EXTENSION_NAME
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import java.io.File
import java.net.URL
import javax.inject.Inject

class CatalogScope(
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
        this.replaceFirst(keyBase, "").drop(1)
    } else ""

    fun with(key: String, block: CatalogScope.() -> Unit) =
        delegate.with(key = key.absolute(), block = block)

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

    fun checkDependenciesVersion() {
        delegate.logInfo("**$keyBase checkDependenciesVersion")
        forEach { key, value ->
            val dependencyFullName = string(key)
            val indexOfVersionSeparator = dependencyFullName.lastIndexOf(':')
            if (indexOfVersionSeparator == -1) {
                if (delegate.verboseCheckDependenciesVersion) {
                    delegate.logInfo("$key: version invalid $dependencyFullName")
                }
            } else {
                val dependencyName = dependencyFullName.substring(0, indexOfVersionSeparator)
                val dependencyVersion = dependencyFullName.substring(indexOfVersionSeparator + 1)
                if (delegate.verboseCheckDependenciesVersion) {
                    delegate.logInfo("$key:$dependencyVersion check")
                }
                kotlin.runCatching {
                    val resolvedVersions = project.configurations.detachedConfiguration(
                        project.dependencies.create("$dependencyName:+")
                    ).resolvedConfiguration.resolvedArtifacts
                    resolvedVersions.filter {
                        it.id.componentIdentifier.displayName.startsWith(dependencyName)
                    }.maxByOrNull { it.moduleVersion.id.version }?.moduleVersion?.id?.version?.let {
                        if (it != dependencyVersion) {
                            delegate.logInfo("$key: can be updated to $it")
                        }
                    } ?: run {
                        if (delegate.verboseCheckDependenciesVersion) {
                            delegate.logInfo("$key: $dependencyName latest version not found in")
                            resolvedVersions.forEach {
                                delegate.logInfo(">> ${it.id.displayName}")
                            }
                        }
                    }
                }.onFailure {
                    if (delegate.verboseCheckDependenciesVersion) {
                        delegate.logInfo("$key: failed to retrieve latest version")
                    }
                }
            }

        }
        delegate.logInfo("**")
    }

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

    var checkDependenciesVersion by PropertyDelegate { false }

    var jsonFile by PropertyDelegate<JsonFile?> { null }
    private val rawCatalog = mutableMapOf<String, String>()

    fun jsonFromFile(path: String) = object : JsonFile {
        override val data: String
            get() = File(path).also {
                if (!it.exists() || !it.isFile) {
                    throw GradleException("catalog file not found")
                }
                if (verboseCatalogBuild) logInfo("retrieve json catalog from file $path")
            }.readText()
    }

    fun jsonFromUrl(href: String) = object : JsonFile {
        override val data: String
            get() = URL(href).also {
                if (verboseCatalogBuild) logInfo("retrieve json catalog from url $href")
            }.readText()
    }

    fun jsonFromString(data: String) = object : JsonFile {
        override val data: String
            get() = data.also {
                if (verboseCatalogBuild) logInfo("retrieve json catalog from string")
            }
    }

    fun configureProjects() {
        buildRawCatalog()
        applyProjectsPlugin()
    }

    internal fun logInfo(data: String) {
        println(data)
    }

    private fun buildRawCatalog() {
        val uri = this.jsonFile ?: run {
            throw GradleException("catalog path is null")
        }
        if (verboseCatalogBuild) logInfo("Read catalog json : $uri")
        val objectMapper = ObjectMapper()
        val inputMap = objectMapper.readValue(
            uri.data,
            object : TypeReference<Map<String, Any>>() {}
        )
        flattenMap(inputMap, rawCatalog)
        if (verboseCatalogBuild) {
            rawCatalog.forEach { key, value ->
                logInfo("$key :: $value")
            }
        }

    }

    private fun applyProjectsPlugin() {
        if (verbosePluginApply) logInfo("Project : ${project.name}")
        project.allprojects.filter { it !== project }.forEach { module ->
            if (verbosePluginApply) logInfo("Module : ${module.name}")
            if (verbosePluginApply) logInfo("apply plugin : ${ProjectCatalogPlugin.CATALOG_PLUGIN_ID}")
            module.plugins.apply(ProjectCatalogPlugin.CATALOG_PLUGIN_ID)
            kotlin.runCatching {
                module.extensions.findByName(CATALOG_EXTENSION_NAME) as? CatalogExtension
            }.getOrNull()?.delegate(this) ?: run {
                throw GradleException("catalog plugin not successfully apply to ${project.name}")
            }
            stringList(key = module.name, default = { emptyList() })
                .takeIf { it.isNotEmpty() }?.let { plugins ->
                    plugins.forEach { plugin ->
                        if (verbosePluginApply) logInfo("apply plugin : $plugin")
                        module.plugins.apply(plugin)
                    }
                    if (verbosePluginApply) logInfo("** success **")
                } ?: run {
                if (verbosePluginApply) logInfo("!!! Warning... no plugins found in catalog")
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

    fun with(key: String, block: CatalogScope.() -> Unit) = CatalogScope(
        project = project,
        keyBase = key,
        delegate = this
    ).block()

    fun string(key: String, default: (key: String) -> String = DEFAULT_STRING): String {
        val value = rawCatalog[key] ?: run {
            if (verboseReadValue) logInfo("value not found for key: $key")
            return default(key)
        }
        if (verboseReadValue) logInfo("key: $key | value: $value")
        if (!value.contains('$')) return value
        if (verboseReadValue) logInfo("key: $key | value contains placeholder, start replacement...")
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
            if (verboseReadValue) logInfo("absolute placeHolderKey : $placeHolderKey")
            //recurse retrieve placeholder holder value
            val placeHolderValue = string(placeHolderKey) ?: kotlin.run {
                throw GradleException("placeholder key $placeHolderKey not found for key $key with value $value")
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
        if (verboseReadValue) logInfo("... end replacement -> key: $key | value: $valueBuilder")
        return valueBuilder.toString()
    }

    fun stringList(
        key: String,
        default: (key: String) -> List<String> = DEFAULT_STRING_LIST
    ): List<String> = string(key = key, default = {
        default(key).joinToString(",")
    }).takeIf { it.isNotBlank() }?.split(",") ?: emptyList()

    fun int(key: String, default: (key: String) -> Int = DEFAULT_INT): Int =
        string(key = key, default = {
            default(key).toString()
        }).toIntOrNull() ?: default(key)

    fun javaVersion(
        key: String,
        default: (key: String) -> JavaVersion = DEFAULT_JAVA_VERSION
    ): JavaVersion =
        string(key = key, default = {
            default(key).toString()
        }).let { value ->
            JavaVersion.values().find { it.name == value } ?: default(key)
        }

    fun filter(
        predicate: (key: String) -> Boolean
    ) = rawCatalog.filter { predicate(it.key) }

    fun forEach(block: (key: String, value: String) -> Unit) = rawCatalog.forEach {
        block(it.key, it.value)
    }

}

open class CatalogExtension {
    private lateinit var delegate: CatalogRootExtension

    internal fun delegate(catalog: CatalogRootExtension) {
        delegate = catalog
    }

    fun with(key: String, block: CatalogScope.() -> Unit) = delegate.with(key = key, block = block)

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

    fun filter(predicate: (key: String) -> Boolean) =
        delegate.filter(predicate = predicate)

    fun forEach(block: (key: String, value: String) -> Unit) = delegate.forEach(block = block)

}