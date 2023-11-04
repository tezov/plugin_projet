package com.tezov.plugin_project.catalog

import com.tezov.plugin_project.Logger
import com.tezov.plugin_project.Logger.PLUGIN_CATALOG
import com.tezov.plugin_project.Logger.throwException
import com.tezov.plugin_project.catalog.CatalogPointer.Format.Companion.formatOrNull
import com.tezov.plugin_project.catalog.CatalogPointer.Type.Companion.scheme
import com.tezov.plugin_project.catalog.CatalogPointer.Type.Companion.substringAfter
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import java.net.URL
import java.nio.file.Path

interface CatalogPointer {
    val error: String?
    val format: Format?
    val data: String?

    enum class Format(val extension: String) {
        Json("json"),
        Yaml("yaml"),
        Toml("toml");

        companion object {

            inline val String.extension get() = substringAfterLast('.', "")

            val String.format
                get() = formatOrNull ?: PLUGIN_CATALOG.throwException(
                        "Couldn't resolve the catalog extension for $this. Accepted extensions are (${Format.joinToString()})"
                    )

            val String.formatOrNull
                get() = Format.values().find { it.extension == extension }

            internal fun joinToString() = values().joinToString(",") { it.extension }
        }
    }

    enum class Type(val scheme: String) {
        File("file://"),
        Url("url://");

        companion object {

            val String.scheme
                get() = schemeOrNull ?: PLUGIN_CATALOG.throwException(
                    "Couldn't resolve the catalog scheme for $this. Accepted schemes are (${Type.joinToString()})"
                )

            val String.schemeOrNull
                get() = """(.+?//).*""".toRegex().find(this)?.groupValues?.getOrNull(1).let { codeFound ->
                    Type.values().find { it.scheme == codeFound }
                }

            internal fun joinToString() = Type.values().joinToString(",") { it.scheme }

            fun String.substringAfter(scheme: Type) = substringAfter(scheme.scheme)

            fun String.substringBefore(scheme: Type) = substringBefore(scheme.scheme)
        }
    }

    companion object {

        fun build(settings: Settings, from: String): CatalogPointer {
            val catalog = when (val scheme = from.scheme) {
                Type.File -> initFromFile(pathString = from.substringAfter(scheme))
                Type.Url -> initFromUrl(href = from.substringAfter(scheme))
            }
            catalog.error?.let { PLUGIN_CATALOG.throwException(settings, it) }
            return catalog
        }

        fun build(from: Path) = initFromFile(from)

        fun build(from: URL) = initFromUrl(from)

        private fun initFromFile(pathString: String) = initFromFile(Path.of(pathString))

        private fun initFromFile(path: Path) = object : CatalogPointer {
            private val file = path.toFile()

            override val error: String?
                get() = when {
                    !file.exists() || !file.isFile -> "Invalid file $path. Not a file, the file doesn't exist or the file can't be accessed"
                    format == null -> "Couldn't resolve the catalog extension for $path. Accepted extensions are (${Format.joinToString()})"
                    else -> null
                }

            override val format
                get() = path.toString().formatOrNull

            override val data: String
                get() = file.readText()
        }

        private fun initFromUrl(href: String) = initFromUrl(URL(href))

        private fun initFromUrl(url: URL) = object : CatalogPointer {

            override val error: String?
                get() = when (format) {
                    null -> "Couldn't resolve the catalog extension for $url. Accepted extensions are (${Format.joinToString()})"
                    else -> null
                }

            override val format
                get() = url.path.formatOrNull

            override val data: String
                get() = url.readText()
        }

    }


}