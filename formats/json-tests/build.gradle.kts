/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import Java9Modularity.configureJava9ModuleInfo
import org.jetbrains.kotlin.gradle.tasks.*

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

apply(from = rootProject.file("gradle/native-targets.gradle"))
apply(from = rootProject.file("gradle/configure-source-sets.gradle"))

// disable kover tasks because there are no non-test classes in the project
tasks.named("koverHtmlReport") {
    enabled = false
}
tasks.named("koverXmlReport") {
    enabled = false
}
tasks.named("koverVerify") {
    enabled = false
}

kotlin {
    sourceSets {
        val commonTest by getting {
            dependencies {
                api(project(":kotlinx-serialization-json"))
                api(project(":kotlinx-serialization-json-okio"))
                implementation("com.squareup.okio:okio:${property("okio_version")}")
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation("com.google.code.gson:gson:2.8.5")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${property("coroutines_version")}")
            }
        }
    }
}

project.configureJava9ModuleInfo()
