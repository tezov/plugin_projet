package com.tezov.plugin_project.catalog

import com.tezov.plugin_project.Logger.throwException
import org.gradle.api.JavaVersion

internal class CatalogMap(
    private val extension: CatalogProjectExtension,
    rawCatalog: Map<String, String>
) {

    companion object {
        const val PLACE_HOLDER_START = '$'
        val PLACE_HOLDER_REGEX = Regex("""(\$\{(.*?)\})""")
        const val KEY_SEPARATOR = '.'
        const val ARRAY_SEPARATOR = ','
        val DEFAULT_THROW = { key: String ->
            throw IndexOutOfBoundsException("key $key not found")
        }
    }

    private val catalog: MutableMap<String, String> = mutableMapOf()

    init {
        rawCatalog.forEach {
            val value = if (it.value.contains(PLACE_HOLDER_START)) {
                stringPlaceHolderRecurseReplace(it.key, rawCatalog)!!
            } else it.value
            this@CatalogMap.catalog[it.key] = value
        }
    }

    private fun stringPlaceHolderRecurseReplace(
        key: String,
        catalog: Map<String, String>
    ): String? {
        with(extension) {
            val value = catalog[key] ?: return null
            if (!value.contains(PLACE_HOLDER_START)) return value
            val valueBuilder = StringBuilder(value)
            var indexOffset = 0
            //multiple placeholder can be in value
            for (it in PLACE_HOLDER_REGEX.findAll(value)) {
                if (it.groups.size < 3) continue
                //find first valid placeholder value from inside to outside
                val placeHolderKey = it.groups[2]?.value ?: continue
                val keys = key.split(KEY_SEPARATOR).toMutableList()
                var placeHolderValue: String?
                do {
                    keys.removeLast()
                    val placeHolderKeyRebuilt = StringBuilder().apply {
                        append(keys.joinToString(KEY_SEPARATOR.toString()))
                        if (isNotEmpty()) append(KEY_SEPARATOR)
                        append(placeHolderKey)
                    }.toString()
                    placeHolderValue =
                        stringPlaceHolderRecurseReplace(
                            key = placeHolderKeyRebuilt,
                            catalog = catalog
                        )
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
                    indexOffset += (placeHolderValue.length - it.count())
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