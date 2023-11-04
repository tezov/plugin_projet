package com.tezov.plugin_project.config

import java.io.ByteArrayInputStream
import java.io.InputStream

object Proguard {
    private const val BASE_PATH = "proguard/"

    enum class PlaceHolder(val value: String) {
        REPACKAGE_NAME("repackageName")
    }

    enum class File(private val path: String) {
        DEBUG_KEEP("${BASE_PATH}rules_release_debug_keep.pro"),
        DEBUG_REMOVE("${BASE_PATH}rules_release_debug_remove.pro"),
        DISALLOW_OBFUSCATION("${BASE_PATH}rules_release_disallowObfusc.pro"),
        KEEP_SOURCE_FILE("${BASE_PATH}rules_release_keepSourceFile.pro"),
        REPACKAGE("${BASE_PATH}rules_release_repackage.pro");

        operator fun invoke(placeHolders: Map<String, String>? = null): InputStream? {
            return try {
                val inputStream = javaClass.classLoader.getResourceAsStream(path)
                placeHolders?.let{
                    val text = inputStream?.bufferedReader().use { it?.readText() }
                    val finalText = replacePlaceholders(text ?: "", placeHolders)
                    ByteArrayInputStream(finalText.toByteArray(Charsets.UTF_8))
                } ?: inputStream
            } catch (e: Throwable) {
                null
            }
        }

        private fun replacePlaceholders(text: String, placeHolders: Map<String, String>): String {
            val placeholderPositions = sortedMapOf<Int, String>(reverseOrder())
            for (key in placeHolders.keys) {
                val placeholder = "\${$key}"
                var index = -1
                while (-1 != text.indexOf(placeholder, index + 1).also { index = it }) {
                    placeholderPositions[index] = placeholder
                }
            }
            val result = StringBuilder(text)
            for ((index, placeholder) in placeholderPositions) {
                placeHolders[placeholder.removeSurrounding("\${", "}")]?.let {
                    result.replace(index, index + placeholder.length, it)
                }
            }
            return result.toString()
        }

    }

}

