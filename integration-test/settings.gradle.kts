pluginManagement {
    includeBuild("../build-settings-logic")

    resolutionStrategy {
        val mainKotlinVersion = providers.gradleProperty("mainKotlinVersion").get()
        eachPlugin {
            if (requested.id.id == "org.jetbrains.kotlin.multiplatform") {
                useVersion("$mainKotlinVersion")
            }
            if (requested.id.id == "org.jetbrains.kotlin.kapt") {
                useVersion("$mainKotlinVersion")
            }
            if (requested.id.id == "org.jetbrains.kotlin.plugin.serialization") {
                useVersion("$mainKotlinVersion")
            }
        }
    }

    repositories {
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
        maven("https://redirector.kotlinlang.org/maven/dev")
        mavenLocal()
    }
}

plugins {
    id("serialization-cache-redirector")
}

rootProject.name = "kotlinx-serialization-integration-test"
