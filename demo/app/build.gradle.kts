tezovConfig {

    configuration {
        domain = tezovCatalog.string("domain")
//        languages.apply{
//            add("fr")
//            add("en")
//        }
    }

    version {
        major = 1
        minor = 0
        patch = 0
    }

    debug {
        keepProguard = true
        keepSourceFile = true
        repackage = false
        obfuscate = false
        minify = false
    }

    release {
        enableDebug = true
//        proguards.apply {
//            add(File("proguards-rules.pro"))
//        }
    }

    configureAndroidPlugin()
}

android {
    tezovCatalog {
        with("projectVersions") {
            compileSdk = int("defaultCompileSdk")
            compileOptions {
                sourceCompatibility = javaVersion("javaSource")
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
    implementation(project(":lib"))

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