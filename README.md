# Tezov plugin project

## What's New
- 02/08/2023 - 1.0.0 - first release

## Description
Gradle plugin for Android project:

**Plugin Config**
auto configuration multi module application and proguard debug tool
  - version "major.minor.patch(-alpha.x|RC.x)
  - debug / release tool
  - auto Jrunner path (must be inside the folder "'configuration.domain'.'module_name'.JUnit" and have JUnitRunner.kt file name)
  - user friendly setup with build type available
  - sourceSet config

**Plugin Catalog**
shared version, dependencies and constants between modules
  - auto apply plugin to modules
  - shared custom variables
  - shared dependencies path/version
  - remote or local catalog
  - check dependencies latest version
  - accepted catalog format Json, Yaml and Toml

** The 2 plugins are not dependant to each other. They can be used separately or together.

## How to install -Config- plugin
- add classpath and repositories to settings.gradle.kts

```
buildscript {

    dependencies {
        classpath("com.tezov:plugin_project:?version?")
        classpath("com.android.tools.build:gradle:?version?")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:?version?")
    }

    repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }

}
```

- add plugin to module

```
plugins {
    //id("com.android.application") //for android application
    // id("com.android.library") //for android libray
    ...
    id(com.tezov.plugin_project.config)
}
```

- use plugin

```
tezovConfig {

    configuration {
        domain = "com.my_domain"
        //hasJUnitRunner = true
        //hasResources = false
        //hasAssets = false
        //languages.add("fr")
        //proguardPaths.add()
        //proguardConsumerPaths.add()
    }

    version {
        major = 1
        minor = 2
        patch = 2
        //alpha = 1
        //beta = null
        //rc = null
    }

    debug {
        keepProguard = true
        keepSourceFile = true
        repackage = false
        obfuscate = false
        minify = false
        hasJUnitRunner = true
    }

    release {
        //enableDebug = true //to use the debug config in release build
        //proguards.apply {
        //  add(File("proguards-rules.pro"))
        //}
    }

    lint {
        //abortOnError = true
        //checkReleaseBuilds = true
        //checkDependencies = true
    }

    configureAndroidPlugin() // !!!! MANDATORY !!!!
}

//if you wanna add some logic where you need the current build type
tezovConfig {

     beforeVariant { buildType: BuildType ->
         when(buildType){
              BuildType.DEBUG -> {}
              BuildType.RELEASE -> {}
              else -> {}
         }
     }
     
     whenEvaluated { buildType: BuildType ->
         when(buildType){
              BuildType.DEBUG -> {}
              BuildType.RELEASE -> {}
              else -> {}
         }
     }
     
     whenReady { buildType: BuildType ->
         //build.currentType available here
         when(buildType){
              BuildType.DEBUG -> {}
              BuildType.RELEASE -> {}
              else -> {}
         }
     }

}
```

**You can check to demo project sources juste in this repo**

## How to install -Catalog- plugin
- add classpath and repositories to settings.gradle.kts

```
buildscript {

    dependencies {
        classpath("com.tezov:plugin_project:?version?")
        classpath("com.android.tools.build:gradle:?version?")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:?version?")
    }

    repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }

}
```

- add plugin to **root project** build.gradle.kts

```
plugins {
   ...
    id("com.tezov.plugin_project.catalog")
}
```

- still inside **root project** build.gradle.kts, specify the path of your json catalog

```
tezovCatalog {
//    verboseCatalogBuild = true
//    verbosePluginApply = true
//    verboseReadValue = true
//    verboseCheckDependenciesVersion = true

    catalogFile = catalogFromFile("F:/android_project/tezov_banque/tezov_bank.catalog.yaml")
    or catalogFile = catalogFromUrl("https://www.tezov.com/tezov_bank.catalog.json")
    or catalogFile = catalogFromString("{** json catalog string here  **}")

  // uncomment to check dependencies from catalog
/*    with("projectPath.dependencies"){
        with("core"){
            checkDependenciesVersion()
        }
        with("compose"){
            checkDependenciesVersion()
        }
        with("lib"){
            checkDependenciesVersion()
        }
    }
*/    

    configureProjects() !!!! MANDATORY !!!!
}
```

- Catalog

  - You can have any level of json, yaml or toml
  - You can use any name except
    - name of modules are reserved to be used to apply plugins
  - You can use placeholder surrounded by ${...}
    - Placeholder key is flatten key dot separated
    - If the placeholder is inside the same level, you don't need to write the full key but just the last part.
  - If a property name is equal to module name, an array of plugins is expected. All plugin will be auto apply and also the catalog plugin to each modules
    - plugin version come from from the version you used in classpath setting

**Catalog example:  tezov.catalog.json / tezov.catalog.yaml tezov.catalog.toml in this repo** 

- Then in build.gradle.kts of each module that have been defined in the json, you can use the catalog plugin

```
android {
    tezovCatalog {
        with("projectVersion") {
            compileSdk = int("defaultCompileSdk")
            compileOptions {
                sourceCompatibility = javaVersion("javasource")
                targetCompatibility = javaVersion("javaTarget")
            }
            kotlinOptions {
                jvmTarget = javaVersion("jvmTarget").toString()
            }
            defaultConfig {
                minSdk = int("defaultMinCompileSdk")
                targetSdk = int("defaultTargetCompileSdk")
            }
            buildFeatures {
                compose = true
            }
            composeOptions {
                kotlinCompilerExtensionVersion = string("composeCompiler")
            }
        }
        packaging {
            resources {
                stringListOrNull("resourcesExcluded")?.let {
                    excludes.addAll(it)
                }
            }
        }
    }
}

dependencies {
    implementation(project(":demo_lib"))
    tezovCatalog {
        with("projectPath.dependencies.compose") {
            implementation(string("ui"))
            implementation(string("runtime"))
            implementation(string("material3"))
            implementation(string("foundation"))
            implementation(string("activity"))
            implementation(string("ui_preview"))
        }
        with("projectPath.dependencies_debug.compose") {
            debugImplementation(string("compose_ui_tooling"))
            debugImplementation(string("compose_ui_manifest"))
        }
    }
}

```

## Catalog functions
- **string**, **stringList**, **int**, **javaVersion** (+ **OrNull** version, and optional default value)
- **forEach** and **filter**
- **with** to scope yourself inside some json level****
- **checkDependenciesVersion**

## Pro and Cons

pro
- one single version / dependencies path / constants for a multi external modules project.
- catalog can be remote or local

con
- lost of notification from the IDE which tells you that there is a new version, but can do some check with checkDependenciesVersion

## How to try the demo
- Uncomment // include (":demo_app", ":demo_lib") in setting.gradle.kts
- Comment includeBuild("plugin_project") in setting.gradle.kts
- Uncomment classpath("com.tezov:plugin_project:1.0.1-8.0.2") in setting.gradle.kts
- Uncomment all in build.gradle.kts of root project