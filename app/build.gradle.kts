plugins {
    id("com.android.application") version "8.0.2" apply true
    id("org.jetbrains.kotlin.android") version "1.8.22" apply true
    id("com.tezov.plugin_project.config")
}

tezovConfig {

    configuration {
        domain = "com.tezov"
//        proguardPaths.apply {
//            add("proguard-android-optimize.txt")
//            add("proguard-rules.pro")
//        }
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
        keepLog = true
        keepSourceFile = true
        repackage = false
        obfuscate = false
        minify = false
        hasJUnitRunner = true
    }

    release {
        enableDebug = true
    }

    configureAndroidPlugin()
}

android {
    compileSdk = 33
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    defaultConfig {
        minSdk = 24
        targetSdk = 33
        vectorDrawables {
            useSupportLibrary = true
        }
    }
    packaging {
        resources {
            excludes.addAll(
                listOf(
                    "META-INF/DEPENDENCIES",
                    "META-INF/LICENSE",
                    "META-INF/LICENSE.txt",
                    "META-INF/LICENSE.md",
                    "META-INF/LICENSE-notice.md",
                    "META-INF/NOTICE",
                    "META-INF/NOTICE.txt",
                    "META-INF/NOTICE.md",
                    "META-INF/ASL2.0",
                    "META-INF/LGPL2.1",
                )
            )
        }
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.8"
    }
//    signingConfigs {
//        releaseConfig {
//        }
//        debugConfig {
//        }
//    }
    buildTypes {
        getByName("release") {
//            signingConfig signingConfigs.releaseConfig
        }
        getByName("debug") {
//            signingConfig signingConfigs.debugConfig
        }
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.8.22")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")

    implementation("androidx.compose.ui:ui:1.4.3")
    implementation("androidx.compose.runtime:runtime:1.4.3")
    implementation("androidx.compose.material3:material3:1.1.1")
    implementation("androidx.compose.foundation:foundation:1.4.3")
    implementation("androidx.activity:activity-compose:1.7.2")

    implementation("androidx.compose.ui:ui-tooling-preview:1.4.3")
    debugImplementation("androidx.compose.ui:ui-tooling:1.4.3")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.4.3")
}