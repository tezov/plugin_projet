package com.tezov.plugin_project.catalog

import com.tezov.plugin_project.Logger.logInfo
import com.tezov.plugin_project.Logger.throwException
import org.gradle.api.JavaVersion

internal class CatalogMap(
    private val extension: CatalogProjectExtension,
    private val rawCatalog: Map<String, String>
) {

    companion object {
        val DEFAULT_THROW = { key: String ->
            throw IndexOutOfBoundsException("key $key not found")
        }
    }

    fun filter(
        predicate: (key: String) -> Boolean
    ) = rawCatalog.filter { predicate(it.key) }

    fun forEach(block: (key: String, value: String) -> Unit) = rawCatalog.forEach {
        block(it.key, it.value)
    }

    fun stringOrNull(key: String): String? {
        with(extension) {
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
    }

    fun stringListOrNull(key: String): List<String>? = stringOrNull(key = key)?.split(",")

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