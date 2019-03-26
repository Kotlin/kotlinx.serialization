plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin on the JVM.
    id("org.jetbrains.kotlin.jvm") version "whatever" // ../settings.gradle overrides plugin resolution rules
    id("kotlinx-serialization")

    // Apply the application plugin to add support for building a CLI application.
    application
}

repositories {
    // Use jcenter for resolving your dependencies.
    // You can declare any Maven/Ivy/file repository here.
    jcenter()
    maven("https://kotlin.bintray.com/kotlinx")
}

dependencies {
    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:${properties["mainLibVersion"]}")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")


    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")

}

application {
    // Define the main class for the application.
    mainClassName = "AppKt"
}
