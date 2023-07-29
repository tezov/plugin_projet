# Tezov plugin project

## What's New
- add min version plugin checker - 1.0.0-8.0.2+-alpha.12
- add dependencies version checker - 1.0.0-8.0.2+-alpha.11
- allow url, file or string data for json catalog - 1.0.0-8.0.2+-alpha.11
- change tag to show plugin version and minimum android version - 1.0.0-8.0.2+-alpha.10
- clean master branch - 8.0.2-alpha.9
- add catalog plugin - 8.0.2-alpha.6
- publish to gradle portal plugin - 8.0.2-alpha.2
- first working release - 8.0.2-alpha.1

## Description

This project is a gradle plugin to auto setup the **Android plugin application and library**

- Config - config android plugin
  - version "major.minor.patch(-alpha.x|RC.x)
  - debug / release tool
  - auto Jrunner path (must be inside the folder "'configuration.domain'.'module_name'.JUnit" and have JUnitRunner.kt file name)
  - user friendly setup with build type available
  - auto sourceSet config
  
- Catalog - shared catalog between modules
  - auto apply plugin to modules
  - shared custom variables
  - shared dependencies path/version
  - remote or local catalog

** there are 2 plugins** Config and Catalog, they are not dependant to each other. They can be used separately or together.

## Next To Come, not working yet
- proguard path + debug variables working

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
        //releaseCandidate = null
    }

    debug {
        keepLog = true
        keepSourceFile = true
        repackage = false
        obfuscate = false
        minify = false
        hasJUnitRunner = true
    }

    release {
        //enableDebug = true //to use the debug config in release build
        //obfuscate = false
        //minify = false
    }

    lint {
        //abortOnError = true
        //checkReleaseBuilds = true
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
    - name of modules are reserved to be used to apply plugin
  - You can use placeholder ${path of another value with dot separator}
  - If a property is the name of a module, an array of plugin is expected. All plugin will be auto apply and also the catalog plugin to each modules

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
    "jvmTarget": "17",
    "composeCompiler": "1.4.8",
    "dependencies": {
      "core": {
        "multidex": "2.0.1",
        "kotlin": "1.10.1",
        "annotation": "1.6.0",
        "coroutines": "1.7.2",
        "coroutines_android": "1.7.2",
        "appcompat": "1.6.1",
        "appcompat_resources": "1.5.1",
        "material": "1.9.0",
        "viewmodel": "2.6.1",
        "viewmodel_saved_state": "2.6.1",
        "lifecycle_runtime": "2.6.1",
        "lifecycle_extensions": "2.2.0",
        "lifecycle_livedata": "2.6.1",
        "reflection": "1.9.0",
        "dagger": "2.46.1",
        "dagger_kapt": "2.46.1"
      },
      "compose": {
        "ui": "1.4.3",
        "ui_util": "1.4.3",
        "ui_preview": "1.4.3",
        "runtime": "1.4.3",
        "material": "1.4.3",
        "material3": "1.1.1",
        "material_icons_core": "1.1.1",
        "material_icons_extended": "1.1.1",
        "foundation": "1.4.3",
        "accompanist_pager_indicators": "0.31.5-beta",
        "animation": "1.4.3",
        "activity": "1.7.2",
        "constraintlayout": "1.0.1",
        "viewmodel": "2.6.1",
        "livedata": "1.4.3",
        "navigation": "2.6.0",
        "google_maps": "2.2.0"
      },
      "lib": {
        "threetenabp": "1.4.6",
        "bouncycastle": "1.75",
        "zxing": "3.5.1",
        "webkit": "1.7.0",
        "browser": "1.4.0",
        "gson": "2.9.0",
        "jackson_core": "2.15.2",
        "jackson_databind": "2.15.2",
        "kotlin_serialization": "1.8.22",
        "kotlin_serialization_json": "1.5.1",
        "google_play_services_maps": "18.1.0",
        "okhttp3_interceptor": "5.0.0-alpha.11",
        "retrofit": "2.8.0",
        "retrofit2_scalar": "2.1.0",
        "retrofit2_gson": "2.9.0",
        "glide": "4.13.2",
        "glide_kapt": "4.13.2",
        "glide_okhttp3": "4.13.2"
      }
    },
    "dependencies_test": {
      "core_integration": {
        "test": "1.5.0",
        "test_ktx": "1.5.0",
        "junit_test": "1.1.5",
        "junit_test_ktx": "1.1.5",
        "espresso_core": "3.5.1",
        "espresso_contrib": "3.5.1",
        "uiautomator": "2.2.0",
        "truth": "1.1.5",
        "coroutine": "1.7.2",
        "compose_ui": "1.4.3",
        "compose_ui_tooling": "1.4.3",
        "compose_ui_manifest": "1.4.3"
      },
      "core_unit": {
        "juint": "4.13.2",
        "truth": "1.1.5",
        "mockk": "1.13.5",
        "mockk_android": "1.13.5"
      }
    }
  },
  "projectPath": {
    "dependencies": {
      "core": {
        "multidex": "androidx.multidex:multidex:${projectVersion.dependencies.core.multidex}",
        "kotlin": "androidx.core:core-ktx:${projectVersion.dependencies.core.kotlin}",
        "annotation": "androidx.annotation:annotation-jvm:${projectVersion.dependencies.core.annotation}",
        "coroutines": "org.jetbrains.kotlinx:kotlinx-coroutines-core:${projectVersion.dependencies.core.coroutines}",
        "coroutines_android": "org.jetbrains.kotlinx:kotlinx-coroutines-android:${projectVersion.dependencies.core.coroutines_android}",
        "appcompat": "androidx.appcompat:appcompat:${projectVersion.dependencies.core.appcompat}",
        "appcompat_resources": "androidx.appcompat:appcompat-resources:${projectVersion.dependencies.core.appcompat_resources}",
        "material": "com.google.android.material:material:${projectVersion.dependencies.core.material}",
        "viewmodel": "androidx.lifecycle:lifecycle-viewmodel-ktx:${projectVersion.dependencies.core.viewmodel}",
        "viewmodel_saved_state": "androidx.lifecycle:lifecycle-viewmodel-savedstate:${projectVersion.dependencies.core.viewmodel_saved_state}",
        "lifecycle_runtime": "androidx.lifecycle:lifecycle-runtime-ktx:${projectVersion.dependencies.core.lifecycle_runtime}",
        "lifecycle_extensions": "androidx.lifecycle:lifecycle-extensions:${projectVersion.dependencies.core.lifecycle_extensions}",
        "lifecycle_livedata": "androidx.lifecycle:lifecycle-livedata-ktx:${projectVersion.dependencies.core.lifecycle_livedata}",
        "reflection": "org.jetbrains.kotlin:kotlin-reflect:${projectVersion.dependencies.core.reflection}",
        "dagger": "com.google.dagger:dagger:${projectVersion.dependencies.core.dagger}",
        "dagger_kapt": "com.google.dagger:dagger-compiler:${projectVersion.dependencies.core.dagger_kapt}"
      },
      "compose": {
        "ui": "androidx.compose.ui:ui:${projectVersion.dependencies.compose.ui}",
        "ui_util": "androidx.compose.ui:ui-util:${projectVersion.dependencies.compose.ui_util}",
        "ui_preview": "androidx.compose.ui:ui-tooling-preview:${projectVersion.dependencies.compose.ui_preview}",
        "runtime": "androidx.compose.runtime:runtime:${projectVersion.dependencies.compose.runtime}",
        "material": "androidx.compose.material:material:${projectVersion.dependencies.compose.material}",
        "material3": "androidx.compose.material3:material3:${projectVersion.dependencies.compose.material3}",
        "material_icons_core": "androidx.compose.material:material-icons-core:${projectVersion.dependencies.compose.material_icons_core}",
        "material_icons_extended": "androidx.compose.material:material-icons-extended:${projectVersion.dependencies.compose.material_icons_extended}",
        "foundation": "androidx.compose.foundation:foundation:${projectVersion.dependencies.compose.foundation}",
        "accompanist_pager_indicators": "com.google.accompanist:accompanist-pager-indicators:${projectVersion.dependencies.compose.accompanist_pager_indicators}",
        "animation": "androidx.compose.ui:ui-graphics:${projectVersion.dependencies.compose.animation}",
        "activity": "androidx.activity:activity-compose:${projectVersion.dependencies.compose.activity}",
        "constraintlayout": "androidx.constraintlayout:constraintlayout-compose:${projectVersion.dependencies.compose.constraintlayout}",
        "viewmodel": "androidx.lifecycle:lifecycle-viewmodel-compose:${projectVersion.dependencies.compose.viewmodel}",
        "livedata": "androidx.compose.runtime:runtime-livedata:${projectVersion.dependencies.compose.livedata}",
        "navigation": "androidx.navigation:navigation-compose:${projectVersion.dependencies.compose.navigation}",
        "google_maps": "com.google.maps.android:maps-compose:${projectVersion.dependencies.compose.google_maps}"
      },
      "lib": {
        "threetenabp": "com.jakewharton.threetenabp:threetenabp:${projectVersion.dependencies.lib.threetenabp}",
        "bouncycastle": "org.bouncycastle:bcpkix-jdk15to18:${projectVersion.dependencies.lib.bouncycastle}",
        "zxing": "com.google.zxing:core:${projectVersion.dependencies.lib.zxing}",
        "webkit": "androidx.webkit:webkit:${projectVersion.dependencies.lib.webkit}",
        "browser": "androidx.browser:browser:${projectVersion.dependencies.lib.browser}",
        "gson": "com.google.code.gson:gson:${projectVersion.dependencies.lib.gson}",
        "jackson_core": "com.fasterxml.jackson.core:jackson-core:${projectVersion.dependencies.lib.jackson_core}",
        "jackson_databind": "com.fasterxml.jackson.core:jackson-databind:${projectVersion.dependencies.lib.jackson_databind}",
        "kotlin_serialization": "org.jetbrains.kotlin.plugin.serialization:${projectVersion.dependencies.lib.kotlin_serialization}",
        "kotlin_serialization_json": "org.jetbrains.kotlin.plugin.serialization-json:${projectVersion.dependencies.lib.kotlin_serialization_json}",
        "google_play_services_maps": "com.google.android.gms:play-services-maps:${projectVersion.dependencies.lib.google_play_services_maps}",
        "okhttp3_interceptor": "com.squareup.okhttp3:logging-interceptor:${projectVersion.dependencies.lib.okhttp3_interceptor}",
        "retrofit": "com.squareup.retrofit2:retrofit:${projectVersion.dependencies.lib.retrofit}",
        "retrofit2_scalar": "com.squareup.retrofit2:converter-scalars:${projectVersion.dependencies.lib.retrofit2_scalar}",
        "retrofit2_gson": "com.squareup.retrofit2:converter-gson:${projectVersion.dependencies.lib.retrofit2_gson}",
        "glide": "com.github.bumptech.glide:glide:${projectVersion.dependencies.lib.glide}",
        "glide_kapt": "com.github.bumptech.glide:compiler:${projectVersion.dependencies.lib.glide_kapt}",
        "glide_okhttp3": "com.github.bumptech.glide:okhttp3-integration:${projectVersion.dependencies.lib.glide_okhttp3}"
      }
    },
    "dependencies_test": {
      "core_integration": {
        "test": "androidx.test:core:${projectVersion.dependencies_test.core_integration.test}",
        "test_ktx": "androidx.test:core-ktx:${projectVersion.dependencies_test.core_integration.test_ktx}",
        "junit_test": "androidx.test.ext:junit:${projectVersion.dependencies_test.core_integration.junit_test}",
        "junit_test_ktx": "androidx.test.ext:junit-ktx:${projectVersion.dependencies_test.core_integration.junit_test_ktx}",
        "espresso_core": "androidx.test.espresso:espresso-core:${projectVersion.dependencies_test.core_integration.espresso_core}",
        "espresso_contrib": "androidx.test.espresso:espresso-contrib:${projectVersion.dependencies_test.core_integration.espresso_contrib}",
        "uiautomator": "androidx.test.uiautomator:uiautomator:${projectVersion.dependencies_test.core_integration.uiautomator}",
        "truth": "com.google.truth:truth:${projectVersion.dependencies_test.core_integration.truth}",
        "coroutine": "org.jetbrains.kotlinx:kotlinx-coroutines-test:${projectVersion.dependencies_test.core_integration.coroutine}",
        "compose_ui": "androidx.compose.ui:ui-test-junit4:${projectVersion.dependencies_test.core_integration.compose_ui}",
        "compose_ui_tooling": "androidx.compose.ui:ui-tooling:${projectVersion.dependencies_test.core_integration.compose_ui_tooling}",
        "compose_ui_manifest": "androidx.compose.ui:ui-test-manifest:${projectVersion.dependencies_test.core_integration.compose_ui_manifest}"
      },
      "core_unit": {
        "juint": "junit:junit:${projectVersion.dependencies_test.core_unit.juint}",
        "truth": "com.google.truth:truth:${projectVersion.dependencies_test.core_unit.truth}",
        "mockk": "io.mockk:mockk:${projectVersion.dependencies_test.core_unit.mockk}",
        "mockk_android": "io.mockk:mockk-android:${projectVersion.dependencies_test.core_unit.mockk_android}"
      }
    }
  },
  "appPlugin": {
    "android": "com.android",
    "application": "${android}.application",
    "library": "${android}.library",
    "kotlin": "org.jetbrains.kotlin.android",
    "kapt": "org.jetbrains.kotlin.kapt",
    "ksp": "com.google.devtools.ksp",
    "tezov_project": "com.tezov.plugin_project",
    "tezov_project_config": "${tezov_project}.config"
  },
  "app": [
    "${appPlugin.application}",
    "${appPlugin.kotlin}",
    "${appPlugin.kapt}",
    "${appPlugin.tezov_project_config}"
  ],
  "lib_core_android_kotlin": [
    "${appPlugin.library}",
    "${appPlugin.kotlin}",
    "${appPlugin.kapt}",
    "${appPlugin.tezov_project_config}"
  ],
  "lib_core_kotlin": [
    "${appPlugin.library}",
    "${appPlugin.kotlin}",
    "${appPlugin.kapt}",
    "${appPlugin.tezov_project_config}"
  ],
  "lib_test_common": [
    "${appPlugin.library}",
    "${appPlugin.kotlin}",
    "${appPlugin.kapt}",
    "${appPlugin.tezov_project_config}"
  ],
  "lib_test_common_integration": [
    "${appPlugin.library}",
    "${appPlugin.kotlin}",
    "${appPlugin.tezov_project_config}"
  ],
  "lib_test_common_unit": [
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
        with("projectVersion"){
            compileSdk = int("defaultCompileSdk")
            compileOptions {
                sourceCompatibility = javaVersion("javasource")
                targetCompatibility = javaVersion("javaTarget")
            }
            kotlinOptions {
                jvmTarget = string("jvmTarget")
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
    implementation(project(":lib_core_kotlin"))
    implementation(project(":lib_core_android_kotlin"))
    tezovCatalog {
        with("projectPath.dependencies.lib"){
            implementation(string("threetenabp"))
            implementation(string("webkit"))
        }
        with("projectPath.dependencies.core"){
            kapt(string("dagger_kapt"))
        }
    }
}

```

## Catalog functions
- string, stringList, int, javaVersion (+OrNull version, and default value optionel)
- forEach and Filter
- with to scope yourself inside an json level
- checkDependenciesVersion to manually to a manual check

## Pro and Cons

pro
- one single version / dependencies path / constants for a multi external module project.
- catalog can be remote

con
- lost of notification from the IDE which tells you that there is a new version, but can do some check with checkDependenciesVersion
- still need to manage classpath path

