pluginManagement {
    resolutionStrategy {
        val mainKotlinVersion: String by settings
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
        // cache redirector for mavenCentral
        maven("https://cache-redirector.jetbrains.com/repo.maven.apache.org/maven2")
        // mavenCentral()
        maven("https://cache-redirector.jetbrains.com/plugins.gradle.org/m2")
        // maven("https://plugins.gradle.org/m2/")
        maven("https://redirector.kotlinlang.org/maven/dev")
        mavenLocal()
    }
}

rootProject.name = "kotlinx-serialization-integration-test"
