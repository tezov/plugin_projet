package com.tezov.plugin_project.catalog

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.tezov.plugin_project.catalog.CatalogMap.Companion.ARRAY_SEPARATOR

internal object CatalogReader {

    fun read(uri: CatalogProjectExtension.CatalogFile) = when (uri.format) {
        CatalogProjectExtension.FileFormat.Json -> json(uri = uri)
        CatalogProjectExtension.FileFormat.Yaml -> yaml(uri = uri)
        CatalogProjectExtension.FileFormat.Toml -> toml(uri = uri)
    }

    private fun Map<String, Any>.flattenMap(
        outputMap: MutableMap<String, String>,
        parentKey: String = "",
    ) {
        for ((key, value) in this) {
            val newKey = if (parentKey.isNotEmpty()) "$parentKey.$key" else key
            @Suppress("UNCHECKED_CAST")
            when (value) {
                is Map<*, *> -> (value as Map<String, Any>).flattenMap(outputMap, newKey)
                is List<*> -> outputMap[newKey] =
                    value.joinToString(ARRAY_SEPARATOR.toString()) { it.toString().trim() }

                is Number -> outputMap[newKey] = value.toString()
                else -> outputMap[newKey] = value.toString().trim()
            }
        }
    }

    private fun json(
        uri: CatalogProjectExtension.CatalogFile,
    ): Map<String, String> {
        val objectMapper = ObjectMapper()
        objectMapper.findAndRegisterModules()
        val inputMap = objectMapper.readValue(
            uri.data,
            object : TypeReference<Map<String, Any>>() {}
        )
        val rawCatalog = mutableMapOf<String, String>()
        inputMap.flattenMap(rawCatalog)
        return rawCatalog
    }

    private fun yaml(
        uri: CatalogProjectExtension.CatalogFile,
    ): Map<String, String> {
        val objectMapper = ObjectMapper(YAMLFactory())
        objectMapper.findAndRegisterModules()
        val inputMap = objectMapper.readValue(
            uri.data,
            object : TypeReference<Map<String, Any>>() {}
        )
        val rawCatalog = mutableMapOf<String, String>()
        inputMap.flattenMap(rawCatalog)
        return rawCatalog
    }

    private fun toml(
        uri: CatalogProjectExtension.CatalogFile,
    ): Map<String, String> {
        val objectMapper = ObjectMapper(TomlFactory())
        objectMapper.findAndRegisterModules()
        val inputMap = objectMapper.readValue(
            uri.data,
            object : TypeReference<Map<String, Any>>() {}
        )
        val rawCatalog = mutableMapOf<String, String>()
        inputMap.flattenMap(rawCatalog)
        return rawCatalog
    }

}