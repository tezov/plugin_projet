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

    private fun String.absolute() = "$keyBase.$this"

    fun string(key: String, default: (key: String) -> String = DEFAULT_STRING) =
        delegate.string(key = key.absolute(), default = default)

    fun stringList(key: String, default: (key: String) -> List<String> = DEFAULT_STRING_LIST) =
        delegate.stringList(key = key.absolute(), default = default)

    fun int(key: String, default: (key: String) -> Int = DEFAULT_INT) =
        delegate.int(key = key.absolute(), default = default)

    fun javaVersion(key: String, default: (key: String) -> JavaVersion = DEFAULT_JAVA_VERSION) =
        delegate.javaVersion(key = key.absolute(), default = default)

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

    var jsonFile by PropertyDelegate<JsonFile?> { null }
    private val rawCatalog = mutableMapOf<String, String>()

    fun jsonFromFile(path: String) = object : JsonFile {
        override val data: String
            get() = File(path).also {
                if (!it.exists() || !it.isFile) {
                    throw GradleException("catalog file not found")
                }
                if(verboseCatalogBuild) println("retrieve json catalog from file $path")
            }.readText()
    }

    fun jsonFromUrl(href: String) = object : JsonFile {
        override val data: String
            get() = URL(href).also {
                if(verboseCatalogBuild) println("retrieve json catalog from url $href")
            }.readText()
    }

    fun jsonFromString(data: String) = object : JsonFile {
        override val data: String get() = data.also {
            if(verboseCatalogBuild) println("retrieve json catalog from string")
        }
    }

    fun configureProjects() {
        buildRawCatalog()
        applyProjectsPlugin()
    }

    private fun buildRawCatalog() {
        val uri = this.jsonFile ?: run {
            throw GradleException("catalog path is null")
        }
        if (verboseCatalogBuild) println("Read catalog json : $uri")
        val objectMapper = ObjectMapper()
        val inputMap = objectMapper.readValue(
            uri.data,
            object : TypeReference<Map<String, Any>>() {}
        )
        flattenMap(inputMap, rawCatalog)
        if (verboseCatalogBuild) {
            rawCatalog.forEach { key, value ->
                println("$key :: $value")
            }
        }

    }

    private fun applyProjectsPlugin() {
        if (verbosePluginApply) println("Project : ${project.name}")
        project.allprojects.filter { it !== project }.forEach { module ->
            if (verbosePluginApply) println("Module : ${module.name}")
            if (verbosePluginApply) println("apply plugin : ${ProjectCatalogPlugin.CATALOG_PLUGIN_ID}")
            module.plugins.apply(ProjectCatalogPlugin.CATALOG_PLUGIN_ID)
            kotlin.runCatching {
                module.extensions.findByName(CATALOG_EXTENSION_NAME) as? CatalogExtension
            }.getOrNull()?.delegate(this) ?: run {
                throw GradleException("catalog plugin not successfully apply to ${project.name}")
            }
            stringList(key = module.name, default = { emptyList() })
                .takeIf { it.isNotEmpty() }?.let { plugins ->
                    plugins.forEach { plugin ->
                        if (verbosePluginApply) println("apply plugin : $plugin")
                        module.plugins.apply(plugin)
                    }
                    if (verbosePluginApply) println("** success **")
                } ?: run {
                if (verbosePluginApply) println("!!! Warning... no plugins found in catalog")
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
        keyBase = key,
        delegate = this
    ).block()

    fun string(key: String, default: (key: String) -> String = DEFAULT_STRING): String {
        val value = rawCatalog[key] ?: run {
            if (verboseReadValue) println("value not found for key: $key")
            return default(key)
        }
        if (verboseReadValue) println("key: $key | value: $value")
        if (!value.contains('$')) return value
        if (verboseReadValue) println("key: $key | value contains placeholder, start replacement...")
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
            if (verboseReadValue) println("absolute placeHolderKey : $placeHolderKey")
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
        if (verboseReadValue) println("... end replacement -> key: $key | value: $valueBuilder")
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

}

open class CatalogExtension @Inject constructor(
    private val project: Project
) {
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
}