import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    kotlin("jvm")
    alias(libs.plugins.serialization)
}

kotlin {
    jvmToolchain(8)
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        if (overriddenLanguageVersion != null) {
            languageVersion = overriddenLanguageVersion
            freeCompilerArgs += "-Xsuppress-version-warnings"
            allWarningsAsErrors = true
        }
    }
}

dependencies {
    testImplementation(libs.knitTest)
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    testImplementation(project(":kotlinx-serialization-core"))
    testImplementation(project(":kotlinx-serialization-json"))
    testImplementation(project(":kotlinx-serialization-cbor"))
    testImplementation(project(":kotlinx-serialization-protobuf"))
    testImplementation(project(":kotlinx-serialization-properties"))
}

sourceSets.test {
    java.srcDirs("example", "test")
}
