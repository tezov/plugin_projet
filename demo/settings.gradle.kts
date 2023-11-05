import com.tezov.plugin_project.catalog.tezovCatalogSource

rootProject.name = "plugin_project_demo"
include (":app", ":lib")

pluginManagement {

    repositories {
        mavenLocal()
        gradlePluginPortal()
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

buildscript {

    dependencies {
        classpath("com.android.tools.build:gradle:8.0.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.22")
    }

    repositories {
        mavenLocal()
        gradlePluginPortal()
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }

}

plugins {
    id("com.tezov.plugin_project.catalog") version "1.0.7-1" apply true
}

//uncoment the catalog you want to use for the demo
tezovCatalogSource("file://./tezov.catalog.yaml")
//tezovCatalogSource("file://./tezov.catalog.json")
//tezovCatalogSource("file://./tezov.catalog.toml")
//tezovCatalogSource("url://https://raw.githubusercontent.com/tezov/plugin_projet/master/demo/tezov.catalog.yaml")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}