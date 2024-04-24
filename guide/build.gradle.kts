import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    kotlin("jvm")
    id("kotlinx-serialization")
}

kotlin {
    jvmToolchain(8)
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        val kotlin_lv_override = rootProject.extra["kotlin_lv_override"] as String?
        if (kotlin_lv_override != null) {
            languageVersion = kotlin_lv_override
            freeCompilerArgs += "-Xsuppress-version-warnings"
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
