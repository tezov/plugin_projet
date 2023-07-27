val domainName = "com.tezov"
val tezovPluginVersion = "1.0.0"
val androidPluginVersion = "8.0.2+"
val alphaVersion:Int? = 11
val domainVersion = StringBuilder().apply {
    append(tezovPluginVersion)
    append("-")
    append(androidPluginVersion)
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
//    id("maven-publish")
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
            tags.set(listOf("android", "tezov"))
        }
        create("${domainName}.${project.name}.catalog") {
            id = name
            implementationClass = "${name}.ProjectCatalogPlugin"
            displayName = "Tezov plugin project - Catalog"
            description = "user friendly tool to share catalog of constant between project"
            tags.set(listOf("android", "tezov"))
        }
    }
}

dependencies {
    implementation(gradleApi())
//    implementation(kotlin("stdlib"))
    implementation("com.android.tools.build:gradle:${androidPluginVersion}")
    implementation("com.fasterxml.jackson.core:jackson-core:2.15.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
}

//val sourcesJar by tasks.registering(Jar::class) {
//    archiveClassifier.set("sources")
//    from(sourceSets.main.get().allSource)
//}

afterEvaluate {
    publishing {
        repositories {
            maven {
                name = "localRepository"
                url = uri(file("F:\\android_project\\repository\\").path)
            }
        }
//        publications {
//            register(project.name, MavenPublication::class) {
//                from(components["java"])
//                groupId = domain_name
//                artifactId = project.name
//                version = domain_version
//                artifact(sourcesJar.get())
//            }
//        }
    }
}


