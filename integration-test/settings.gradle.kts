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
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
        maven("https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
        maven("https://redirector.kotlinlang.org/maven/dev")
        mavenLocal()
    }
}

rootProject.name = "kotlinx-serialization-integration-test"
