package com.tezov.plugin_project.catalog

import com.tezov.plugin_project.Logger.logInfo
import com.tezov.plugin_project.Logger.throwException
import org.gradle.api.JavaVersion

internal class CatalogMap(
    private val extension: CatalogProjectExtension,
    private val rawCatalog: Map<String, String>
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

    private val catalog: Map<String, String>

    init {
        catalog = rawCatalog
    }

    fun filter(
        predicate: (key: String) -> Boolean
    ) = catalog.filter { predicate(it.key) }

    fun forEach(block: (key: String, value: String) -> Unit) = catalog.forEach {
        block(it.key, it.value)
    }

    fun stringOrNull(key: String): String? {
        with(extension) {
            val value = this@CatalogMap.catalog[key] ?: run {
                if (verboseReadValue) project.logInfo("value not found for key: $key")
                return null
            }
            if (verboseReadValue) project.logInfo("key: $key = $value")
            if (!value.contains(PLACE_HOLDER_START)) return value
            if (verboseReadValue) project.logInfo("key: $key > value contains placeholder, start replacement...")
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
                    if (verboseReadValue) project.logInfo("try to retrieve place holder value with key: $placeHolderKey")
                    placeHolderValue =
                        stringOrNull(placeHolderKeyRebuilt) //placeholder value recurse replace
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
            if (verboseReadValue) project.logInfo(
                "... end replacement -> key: $key = $valueBuilder"
            )
            return valueBuilder.toString()
        }
    }

    fun stringListOrNull(key: String): List<String>? =
        stringOrNull(key = key)?.split(ARRAY_SEPARATOR)

    fun intOrNull(key: String): Int? = stringOrNull(key = key)?.toIntOrNull()

    fun javaVersionOrNull(key: String): JavaVersion? = stringOrNull(key = key)?.let { value ->
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