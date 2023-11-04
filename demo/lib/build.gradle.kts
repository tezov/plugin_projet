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
            }
        }
    }
}

dependencies {

    //Can be use with the gradle catalog way
    api(libraries.runtime.core.kotlin)
//    api(libraries.runtime.core.lifecycle_runtime)

    //or can be write with the tezov catalog way
    tezovCatalog {
        with("libraries.runtime.core") {
//            api(string("kotlin"))
            api(string("lifecycle_runtime"))
        }
    }

}