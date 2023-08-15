package com.tezov.plugin_project.catalog

import com.tezov.plugin_project.Logger.logError
import com.tezov.plugin_project.Logger.throwException
import com.tezov.plugin_project.catalog.CatalogProjectExtension.FileFormat.Companion.format
import com.tezov.plugin_project.catalog.CatalogProjectExtension.FileFormat.Companion.throwExceptionUnsupportedFormat
import org.gradle.api.JavaVersion
import java.net.URL
import java.nio.file.Path
import kotlin.io.path.Path

internal class CatalogMap(
    private val extension: CatalogProjectExtension,
    uri: CatalogProjectExtension.CatalogFile,
) {

    companion object {
        const val PLACE_HOLDER_START = '$'
        val PLACE_HOLDER_REGEX = Regex("""(\$\{(.*?)\})""")
        val BACK_TOKEN_REGEX = Regex("^(\\.\\./)*\\.\\.$")

        const val PLACE_HOLDER_FILE_START = "file://"
        const val PLACE_HOLDER_URL_START = "url://"
        const val PATH_BACK_TOKEN = "../"
        const val PATH_ROOT_TOKEN = "."

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
        if (!value.startsWith(PLACE_HOLDER_START)) return null
        val path = PLACE_HOLDER_REGEX.find(value)?.let {
            if (it.groups.size >= 3) {
                it.groups[2]?.value
            } else null
        } ?: return null
        val uri = when {
            path.contains(PLACE_HOLDER_FILE_START) -> {
                placeHolderFileRetrieveUriFromPath(
                    key = key,
                    value = value,
                    path = path.replaceFirst(PLACE_HOLDER_FILE_START, "")
                )
            }

            path.contains(PLACE_HOLDER_URL_START) -> {
                placeHolderFileRetrieveUriFromUrl(
                    key = key,
                    value = value,
                    path = path.replaceFirst(PLACE_HOLDER_URL_START, "")
                )
            }

            else -> null
        } ?: return null
        return CatalogReader.read(uri)
    }

    private fun placeHolderFileRetrieveUriFromPath(
        key: String,
        value: String,
        path: String,
    ): CatalogProjectExtension.CatalogFile {
        val authority = path.substringBefore(FILE_SEPARATOR, "")
        if (authority.isEmpty()) {
            extension.project.logError("Module name (Authority) or '../' (back token) not found in file $value for key $key")
        }
        val absolutePath: Path = when {
            authority == PATH_ROOT_TOKEN -> {
                Path(extension.project.rootDir.path, path.replaceFirst(authority, ""))
            }
            BACK_TOKEN_REGEX.matches(authority) -> {
                val backTokenCount = (authority.length / PATH_BACK_TOKEN.length) + 1
                var root = extension.project.rootDir.toPath()
                (0 until backTokenCount).forEach { _ -> root = root.parent }
                Path(root.toString(), path.replaceFirst(authority, ""))
            }
            else -> {
                val project = extension.project.allprojects.find {
                    it.name == authority
                } ?: run {
                    extension.project.throwException("Module $authority not found in all project $value for key $key")
                }
                Path(project.projectDir.path, path.replaceFirst(authority, ""))
            }
        }
        return object : CatalogProjectExtension.CatalogFile {
            override val format: CatalogProjectExtension.FileFormat
                get() = absolutePath.toString().format
                    ?: throwExceptionUnsupportedFormat(
                        extension.project,
                        "Couldn't resolve file format $path for key $key."
                    )
            override val data: String
                get() = absolutePath.toFile().also {
                    if (!it.exists() || !it.isFile) {
                        extension.project.throwException("catalog file not found $it from $key")
                    }
                }.readText()
        }
    }

    private fun placeHolderFileRetrieveUriFromUrl(
        key: String,
        value: String,
        path: String,
    ) = object : CatalogProjectExtension.CatalogFile {
        override val format: CatalogProjectExtension.FileFormat
            get() = path.format
                ?: throwExceptionUnsupportedFormat(
                    extension.project,
                    "Couldn't resolve url format $value for key $key."
                )
        override val data: String
            get() = URL(path).readText()
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
            //multiple placeholder can be in value
            var indexOffset = 0
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