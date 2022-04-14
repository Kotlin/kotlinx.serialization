import java.util.*
import java.io.FileInputStream

plugins {
    `kotlin-dsl`
}

repositories {
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap")
    //maven("F:\\JetBrains\\kotlin\\build\\repo")
    mavenCentral()
    // kotlin-dev with space redirector
    maven("https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
}

val kotlinVersion = FileInputStream(file("../gradle.properties")).use { propFile ->
    val ver = project.findProperty("kotlin.version")?.toString()
        ?: Properties().apply { load(propFile) }["kotlin.version"]
    require(ver is String) { "kotlin.version must be string in ../gradle.properties, got $ver instead" }
    ver
}

dependencies {
    implementation(kotlin("gradle-plugin", kotlinVersion))
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}
