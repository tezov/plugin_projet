# Tezov plugin project

## Description
Gradle plugins for Android project. The 2 plugins are not dependant to each other hence they can be used separately or together.

**Plugin Catalog**
shared version, dependencies coordinates and variables between modules
  - auto apply plugin to modules
  - shared custom variables
  - shared dependencies coordinates/version
  - catalog can be remote or local
  - include file or url catalog inside another catalog recursively
  - check dependencies latest version
  - accepted catalog format Json, Yaml and Toml (can be mix when included)

Min gradle version : 8.0 :
[Catalog plugin on Gradle portal](https://plugins.gradle.org/plugin/com.tezov.plugin_project.catalog)

**Plugin Config**
auto configuration multi module application and proguard debug tool
- version "major.minor.patch(-alpha.x|RC.x)
- debug / release tool
- auto Jrunner path (must be inside the folder "'domain'.'module_name'.JUnit" and have JUnitRunner.kt file name)
- user friendly setup with build type available
- sourceSet config

Min gradle version : 8.0 and Min Android plugin version 8.0.2 :
[Config plugin on Gradle portal](https://plugins.gradle.org/plugin/com.tezov.plugin_project.config)

## How to install -Catalog- plugin
- add plugin and repositories to settings.gradle.kts. And apply the plugin!

```
buildscript {
    dependencies { ... }
    repositories {
        ...
        gradlePluginPortal()
        ...
    }
}

plugins {
    ...
    id("com.tezov.plugin_project.catalog") version "?version?" apply true
    ...
}

```

- inside **root project** gradle.properties, specify the path of your json catalog

```
systemProp.catalog=file://./tezov.catalog.yaml (./ is the root project directory or ../../ to get back more)
or systemProp.catalog=file://./tezov.catalog.json
or systemProp.catalog=file://./tezov.catalog.toml
or systemProp.catalog=url://https://www.tezov.com/tezov.catalog.json
```

- inside **root project** build.gradle.kts, you can use the version check dependencies (optional)

```
/* 
// uncomment to check dependencies from catalog
tezovCatalog {
   with("libraries.runtime"){
        with("core"){
            checkDependenciesVersion()
        }
        with("compose"){
            checkDependenciesVersion()
        }
    }
}
*/  
```

**Catalog example** in this repo:  tezov.catalog.json / tezov.catalog.yaml / tezov.catalog.toml

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
	
    //Can be use with the gradle catalog way
//    implementation(libraries.runtime.compose.ui)
//    implementation(libraries.runtime.compose.runtime)
//    implementation(libraries.runtime.compose.material3)
    implementation(libraries.runtime.compose.foundation)
    implementation(libraries.runtime.compose.activity)
    implementation(libraries.runtime.compose.ui.preview) //gradle catalog has replaced ui_preview by ui.preview

    //or can be write with the tezov catalog way
    tezovCatalog {
        with("libraries.runtime.compose") {
            implementation(string("ui"))
            implementation(string("runtime"))
            implementation(string("material3"))
//            implementation(string("foundation"))
//            implementation(string("activity"))
//            implementation(string("ui_preview"))
        }
        with("libraries.debug.compose") {
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

**NEW** : Instead of using string("") to retrieve your dependencies coordinates, you can now use direclty your names as variable (see the example above).
To make use of this, the first level of dependencies coordinates must be "libraries" and the first level of versions must ne "versions". See the demo application.

## Catalog features:
- You can have any level of json, yaml or toml
- You can use any name except
  - name of modules at level 0 are reserved for plugins apply
- You can use placeholder surrounded by ${...}
  - Placeholder key is flatten key dot separated
  - If the placeholder is inside the same level, you don't need to write the full key but just the last part. When placeholder is not complete, there is a look up from inside to outside.
- If you want to auto apply plugins to modules, use "plugins_apply_to" as first level and add any properties name equal to module name with an array of plugins is expected. (see the demo application)
- include file or url catalog inside another catalog
  - useful to have a common dependencies file between different project application to include inside another catalog of a specific application.
  - useful to inject different placeholder meanings between debug, release or keep private key from others.
  - the include is also recursive 
    - if two identical keys are in different files, only one will survive to the merge. Which one, can't be predicted. Be careful. 
    - The key used to include catalog file can be duplicated. When the merge is done, the key used is deleted. Any name can be used.
  - file include should follow "anything_key : ${file://./any_path_from_root_directory.extension_supported}" (yaml, json or toml)
      - . to specify the root dir project
      - .. (as many as you need ../../../) to get out of the root dir. Usefull to keep only 1 dependencies files for all your app in the same folder.
  - url include should follow "anything_key : ${url://complete_url_where_the_file_is.extension_supported}" (yaml, json or toml)
  - 
## Pro and Cons

pro
- one single version / dependencies path / constants for a multi external modules project.
- catalog can be remote or local
- catalog can be split and include from anywhere.
- can use the variable syntaxe of gradle catalog for dependencies only

con
- lost of notification from the IDE which tells you that there is a new version, but can do some check with checkDependenciesVersion
- must set the source catalog inside gradle.properties


## How to install -Config- plugin
- add plugin and repositories to settings.gradle.kts. Do not apply the plugin.

```
buildscript {

    dependencies { ... }

    repositories {
        ...
        gradlePluginPortal()
        ...
    }

}

plugins {
    ...
    id("com.tezov.plugin_project.config") version "?version?" apply false
    ...
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
**Note**: this plugin can be applied by the catalog plugin instead of standalone apply.

