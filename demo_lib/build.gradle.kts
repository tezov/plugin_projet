tezovConfig {
    configuration {
        hasResources = false
        hasAssets = false
    }
    release {
//        proguards.apply {
//            add(File("consumer-rules.pro"))
//        }
    }
    configureAndroidPlugin()
}

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
            }
        }
    }
}

dependencies {
    tezovCatalog {
        with("projectPath.dependencies.core") {
            api(string("kotlin"))
            api(string("lifecycle_runtime"))
        }
    }
}