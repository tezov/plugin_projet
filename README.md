# Tezov plugin project

## What's New
- 1.0.1-8.0.2
  - add debug proguard capabilities
- 1.0.0-8.0.2-alpha.12
  - add min version plugin checker
- 1.0.0-8.0.2+-alpha.11
  - add dependencies version checker
  - allow url, file or string data for json catalog
- 1.0.0-8.0.2+-alpha.10
  - change tag to show plugin version and minimum android version
- 8.0.2-alpha.9
  - clean master branch
- 8.0.2-alpha.6
  - add catalog plugin
- 8.0.2-alpha.2
  - publish to gradle portal plugin
- 8.0.2-alpha.1
  - first working release

## Description

This project is a gradle plugin to auto setup the **Android plugin application and library**

- Config - auto configuration of android plugin
  - version "major.minor.patch(-alpha.x|RC.x)
  - debug / release tool
  - auto Jrunner path (must be inside the folder "'configuration.domain'.'module_name'.JUnit" and have JUnitRunner.kt file name)
  - user friendly setup with build type available
  - sourceSet config
  
- Catalog - shared catalog between modules
  - auto apply plugin to modules
  - shared custom variables
  - shared dependencies path/version
  - remote or local catalog
  - check dependencies latest version

** there are 2 plugins** Config and Catalog, they are not dependant to each other. They can be used separately or together.

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

// here an example of application android plugin configuration (projectVersion and projectPath are constants coming from buildSrc)
android {
    //namespace, sourceSet, versionName, versionCode, applicationId, applicationIdSuffix will be done by tezov config plugin

    compileSdk = projectVersion.defaultCompileSdk
    compileOptions {
        sourceCompatibility = projectVersion.javasource
        targetCompatibility = projectVersion.javaTarget
    }
    kotlinOptions {
        jvmTarget = projectVersion.jvmTarget
    }
    defaultConfig {
        minSdk = projectVersion.defaultMinCompileSdk
        targetSdk = projectVersion.defaultCompileSdk
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = projectVersion.composeCompiler
    }
}


// here an example of library android plugin configuration (projectVersion and projectPath are constants coming from buildSrc)
android {
    //namespace, sourceSet, will be done by tezov config plugin
    
    compileSdk = projectVersion.defaultCompileSdk
    compileOptions {
        sourceCompatibility = projectVersion.javasource
        targetCompatibility = projectVersion.javaTarget
    }
    kotlinOptions {
        jvmTarget = projectVersion.jvmTarget
    }
    defaultConfig {
        minSdk = projectVersion.defaultMinCompileSdk
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = projectVersion.composeCompiler
    }
}
```


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

    jsonFile = jsonFromFile("F:/android_project/tezov_banque/tezov_bank.catalog.json")
    or jsonFile = jsonFromUrl("https://www.tezov.com/tezov_bank.catalog.json")
    or jsonFromString = "{** json catalog string here  **}"

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

- Catalog json

  - You can have any level of json
  - You can use any name except
    - name of modules are reserved to be used to apply plugins
  - You can use placeholder surrounded by ${...}
  - If a property name is equal to module name, an array of plugins is expected. All plugin will be auto apply and also the catalog plugin to each modules
    - plugin version come from from the version you used in classpath setting

```
{
  "domain": "com.tezov",
  "resourcesExcluded": [
    "META-INF/DEPENDENCIES",
    "META-INF/LICENSE",
    "META-INF/LICENSE.txt",
    "META-INF/LICENSE.md",
    "META-INF/LICENSE-notice.md",
    "META-INF/NOTICE",
    "META-INF/NOTICE.txt",
    "META-INF/NOTICE.md",
    "META-INF/ASL2.0",
    "META-INF/LGPL2.1"
  ],
  "projectVersion": {
    "defaultCompileSdk": 33,
    "defaultMinCompileSdk": 21,
    "defaultTargetCompileSdk": "${defaultCompileSdk}",
    "javasource": "VERSION_17",
    "javaTarget": "${javasource}",
    "jvmTarget": "${javasource}",
    "composeCompiler": "1.4.8",
    "dependencies": {
      "core": {
        "kotlin": "1.10.1",
        "lifecycle_runtime": "2.6.1"
      },
      "compose": {
        "ui": "1.4.3",
        "runtime": "1.4.3",
        "material3": "1.1.1",
        "foundation": "1.4.3",
        "activity": "1.7.2",
        "ui_preview": "1.4.3"
      }
    },
    "dependencies_debug": {
      "compose": {
        "compose_ui_tooling": "1.4.3",
        "compose_ui_manifest": "1.4.3"
      }
    }
  },
  "projectPath": {
    "dependencies": {
      "core": {
        "kotlin": "androidx.core:core-ktx:${projectVersion.dependencies.core.kotlin}",
        "lifecycle_runtime": "androidx.lifecycle:lifecycle-runtime-ktx:${projectVersion.dependencies.core.lifecycle_runtime}"
      },
      "compose": {
        "ui": "androidx.compose.ui:ui:${projectVersion.dependencies.compose.ui}",
        "runtime": "androidx.compose.runtime:runtime:${projectVersion.dependencies.compose.runtime}",
        "material3": "androidx.compose.material3:material3:${projectVersion.dependencies.compose.material3}",
        "foundation": "androidx.compose.foundation:foundation:${projectVersion.dependencies.compose.foundation}",
        "activity": "androidx.activity:activity-compose:${projectVersion.dependencies.compose.activity}",
        "ui_preview": "androidx.compose.ui:ui-tooling-preview:${projectVersion.dependencies.compose.ui_preview}"
      }
    },
    "dependencies_debug": {
      "compose": {
        "compose_ui_tooling": "androidx.compose.ui:ui-tooling:${projectVersion.dependencies_debug.compose.compose_ui_tooling}",
        "compose_ui_manifest": "androidx.compose.ui:ui-test-manifest:${projectVersion.dependencies_debug.compose.compose_ui_manifest}"
      }
    }
  },
  "appPlugin": {
    "android": "com.android",
    "application": "${android}.application",
    "library": "${android}.library",
    "kotlin": "org.jetbrains.kotlin.android",
    "tezov_project_config": "com.tezov.plugin_project.config"
  },
  "app": [
    "${appPlugin.application}",
    "${appPlugin.kotlin}",
    "${appPlugin.tezov_project_config}"
  ],
  "lib": [
    "${appPlugin.library}",
    "${appPlugin.kotlin}",
    "${appPlugin.tezov_project_config}"
  ]
}
```

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