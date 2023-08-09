import kotlin.io.path.Path

val domainName = "com.tezov"
val tezovPluginVersion = "1.0.3"
val alphaVersion:Int? = null
val domainVersion = StringBuilder().apply {
    append(tezovPluginVersion)
    alphaVersion?.let {
        append("-alpha.${alphaVersion}")
    }
}.toString()

buildscript {
    dependencies {
        classpath("com.gradle.publish:plugin-publish-plugin:1.2.0")
    }
}

plugins {
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "1.2.0"
}

group = domainName
version = domainVersion

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

gradlePlugin {
    website.set("https://github.com/tezov/plugin_projet")
    vcsUrl.set("https://github.com/tezov/plugin_projet")
    plugins {
        create("${domainName}.${project.name}.config") {
            id = name
            implementationClass = "${name}.ProjectConfigPlugin"
            displayName = "Tezov plugin project - Config"
            description = "user friendly tool to setup Android plugin application and library"
            tags.set(listOf("android", "tezov", "config", "configuration", "debug", "proguard"))
        }
        create("${domainName}.${project.name}.catalog") {
            id = name
            implementationClass = "${name}.ProjectCatalogPlugin"
            displayName = "Tezov plugin project - Catalog"
            description = "shared catalog versions, dependencies and constants between project modules in json, yaml or toml format. Local or Remote file."
            tags.set(listOf("android", "tezov", "catalog", "dependency", "dependencies", "version", "yaml", "toml", "json"))
        }
    }
}

dependencies {
    implementation(gradleApi())
    implementation("com.android.tools.build:gradle:8.0.2")
    implementation("com.fasterxml.jackson.core:jackson-core:2.15.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-toml:2.15.2")
}

afterEvaluate {
    publishing {
        repositories {
            maven {
                name = "localRepository"
                url = uri(Path("${project.projectDir}", "/repository/").toString())
            }
        }
    }
}


