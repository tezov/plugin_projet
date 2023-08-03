package com.tezov.plugin_project.catalog

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.tezov.plugin_project.catalog.CatalogMap.Companion.ARRAY_SEPARATOR

internal object CatalogBuilder {

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

    fun json(
        extension: CatalogProjectExtension,
        uri: CatalogProjectExtension.CatalogFile,
    ):CatalogMap {
        val objectMapper = ObjectMapper()
        objectMapper.findAndRegisterModules()
        val inputMap = objectMapper.readValue(
            uri.data,
            object : TypeReference<Map<String, Any>>() {}
        )
        val rawCatalog = mutableMapOf<String, String>()
        inputMap.flattenMap(rawCatalog)
        return CatalogMap(
            extension = extension,
            rawCatalog = rawCatalog
        )
    }

    fun yaml(
        extension: CatalogProjectExtension,
        uri: CatalogProjectExtension.CatalogFile,
    ):CatalogMap {
        val objectMapper = ObjectMapper(YAMLFactory())
        objectMapper.findAndRegisterModules()
        val inputMap = objectMapper.readValue(
            uri.data,
            object : TypeReference<Map<String, Any>>() {}
        )
        val rawCatalog = mutableMapOf<String, String>()
        inputMap.flattenMap(rawCatalog)
        return CatalogMap(
            extension = extension,
            rawCatalog = rawCatalog
        )
    }

    fun toml(
        extension: CatalogProjectExtension,
        uri: CatalogProjectExtension.CatalogFile,
    ):CatalogMap {
        val objectMapper = ObjectMapper(TomlFactory())
        objectMapper.findAndRegisterModules()
        val inputMap = objectMapper.readValue(
            uri.data,
            object : TypeReference<Map<String, Any>>() {}
        )
        val rawCatalog = mutableMapOf<String, String>()
        inputMap.flattenMap(rawCatalog)
        return CatalogMap(
            extension = extension,
            rawCatalog = rawCatalog
        )
    }

}