package com.tezov.plugin_project.catalog

import com.tezov.plugin_project.Logger.logError
import com.tezov.plugin_project.Logger.throwException
import com.tezov.plugin_project.catalog.CatalogProjectExtension.FileFormat.Companion.format
import com.tezov.plugin_project.catalog.CatalogProjectExtension.FileFormat.Companion.throwExceptionUnsupportedFormat
import org.gradle.api.JavaVersion
import java.net.URL
import kotlin.io.path.Path

internal class CatalogMap(
    private val extension: CatalogProjectExtension,
    uri: CatalogProjectExtension.CatalogFile,
) {

    companion object {
        const val PLACE_HOLDER_START = '$'
        val PLACE_HOLDER_REGEX = Regex("""(\$\{(.*?)\})""")

        const val PLACE_HOLDER_FILE_START = "file://"
        const val PLACE_HOLDER_URL_START = "url://"

        const val KEY_SEPARATOR = '.'
        const val ARRAY_SEPARATOR = ','
        const val FILE_SEPARATOR = '/'

        val DEFAULT_THROW = { key: String ->
            throw IndexOutOfBoundsException("key $key not found")
        }
    }

    private val catalog: MutableMap<String, String>

    init {
        catalog = CatalogReader.read(uri).toMutableMap()
        placeHolderFileReplace(catalog)
        placeHolderValueReplaceAll(catalog)
    }

    private fun placeHolderFileReplace(catalog: MutableMap<String, String>) {
        val mapToAppend = mutableListOf<Pair<String, Map<String, String>>>()
        placeHolderFileReplaceAllRecurse(catalog, mapToAppend)
        mapToAppend.forEach {
            catalog.remove(it.first)
            catalog.putAll(it.second)
        }
    }

    private fun placeHolderFileReplaceAllRecurse(
        catalog: Map<String, String>,
        catalogToAppend: MutableList<Pair<String, Map<String, String>>>
    ) {
        catalog.forEach { entry ->
            placeHolderFileRetrieveMap(entry.key, entry.value)?.let {
                catalogToAppend.add(Pair(entry.key, it))
                placeHolderFileReplaceAllRecurse(it, catalogToAppend)
            }
        }
    }

    private fun placeHolderFileRetrieveMap(
        key: String,
        value: String
    ): Map<String, String>? {
        with(extension) {
            if (!value.startsWith(PLACE_HOLDER_START)) return null
            var path = PLACE_HOLDER_REGEX.find(value)?.let {
                if (it.groups.size >= 3) {
                    it.groups[2]?.value
                } else null
            } ?: return null
            val uri = when {
                path.contains(PLACE_HOLDER_FILE_START) -> {
                    path = path.replaceFirst(PLACE_HOLDER_FILE_START, "")
                    val authority = path.substringBefore(FILE_SEPARATOR, "")
                    if (authority.isEmpty()) {
                        extension.project.logError("Module name (Authority) not found in file $value for key $key")
                    }
                    path = path.replaceFirst(authority, "")
                    val project = extension.project.allprojects.find {
                        it.name == authority
                    } ?: run {
                        extension.project.throwException("Module $authority not found in all project $value for key $key")
                    }
                    object : CatalogProjectExtension.CatalogFile {
                        override val format: CatalogProjectExtension.FileFormat
                            get() = path.format
                                ?: throwExceptionUnsupportedFormat(
                                    project,
                                    "Couldn't resolve file format $path for key $key."
                                )
                        override val data: String
                            get() = Path(project.projectDir.path, path).toFile().also {
                                if (!it.exists() || !it.isFile) {
                                    project.throwException("catalog file not found $it form $key")
                                }
                            }.readText()
                    }
                }

                path.contains(PLACE_HOLDER_URL_START) -> {
                    path = path.replaceFirst(PLACE_HOLDER_URL_START, "")
                    object : CatalogProjectExtension.CatalogFile {
                        override val format: CatalogProjectExtension.FileFormat
                            get() = path.format
                                ?: throwExceptionUnsupportedFormat(
                                    project,
                                    "Couldn't resolve url format $path for key $key."
                                )
                        override val data: String
                            get() = URL(path).readText()
                    }
                }

                else -> null
            } ?: return null
            return CatalogReader.read(uri)
        }
    }

    private fun placeHolderValueReplaceAll(catalog: MutableMap<String, String>) {
        val valueToUpdate = mutableMapOf<String, String>()
        catalog.forEach { entry ->
            placeHolderValueReplaceAllRecurse(
                key = entry.key,
                value = entry.value,
                catalog = catalog
            ).takeIf { it != entry.value }?.let {
                valueToUpdate[entry.key] = it
            }
        }
        valueToUpdate.forEach { entry ->
            catalog[entry.key] = entry.value
        }
    }

    private fun placeHolderValueReplaceAllRecurse(
        key: String,
        value: String,
        catalog: Map<String, String>
    ): String {
        with(extension) {
            if (!value.contains(PLACE_HOLDER_START)) return value
            val valueBuilder = StringBuilder(value)
            var indexOffset = 0
            //multiple placeholder can be in value
            for (it in PLACE_HOLDER_REGEX.findAll(value)) {
                if (it.groups.size < 3) continue
                //find first valid placeholder value from inside to outside
                val placeHolderKey = it.groups[2]?.value ?: continue
                val keys = key.split(KEY_SEPARATOR).toMutableList()
                var placeHolderValue: String? = null
                do {
                    keys.removeLast()
                    val placeHolderKeyRebuilt = StringBuilder().apply {
                        append(keys.joinToString(KEY_SEPARATOR.toString()))
                        if (isNotEmpty()) append(KEY_SEPARATOR)
                        append(placeHolderKey)
                    }.toString()
                    catalog[placeHolderKeyRebuilt]?.let {
                        placeHolderValue =
                            placeHolderValueReplaceAllRecurse(
                                key = placeHolderKeyRebuilt,
                                value = it,
                                catalog = catalog
                            )
                    }
                    if (placeHolderValue != null) break
                } while (keys.isNotEmpty())
                placeHolderValue ?: kotlin.run {
                    project.throwException("placeholder key $placeHolderKey not found for key $key with value $value")
                }
                //replace placeholder by placeholder value in value
                it.groups[1]?.range?.let {
                    valueBuilder.replace(
                        (it.first + indexOffset),
                        (it.last + indexOffset + 1),
                        placeHolderValue
                    )
                    indexOffset += (placeHolderValue!!.length - it.count())
                }
            }
            return valueBuilder.toString()
        }
    }

    val keys get() = catalog.keys

    val values get() = catalog.values

    fun filter(
        predicate: (key: String) -> Boolean
    ) = catalog.filter { predicate(it.key) }

    fun stringOrNull(key: String) = catalog[key]

    fun stringListOrNull(key: String) =
        stringOrNull(key = key)?.split(ARRAY_SEPARATOR)

    fun intOrNull(key: String) = stringOrNull(key = key)?.toIntOrNull()

    fun javaVersionOrNull(key: String) = stringOrNull(key = key)?.let { value ->
        JavaVersion.values().find { it.name == value }
    }

    fun string(key: String, default: (key: String) -> String = DEFAULT_THROW) =
        stringOrNull(key) ?: default(key)

    fun stringList(
        key: String,
        default: (key: String) -> List<String> = DEFAULT_THROW
    ) = stringListOrNull(key = key) ?: default(key)

    fun int(key: String, default: (key: String) -> Int = DEFAULT_THROW) =
        intOrNull(key) ?: default(key)

    fun javaVersion(
        key: String,
        default: (key: String) -> JavaVersion = DEFAULT_THROW
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