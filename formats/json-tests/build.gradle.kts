/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import Java9Modularity.configureJava9ModuleInfo
import org.jetbrains.kotlin.gradle.targets.js.testing.*

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

apply(plugin = "native-targets-conventions")
apply(plugin = "source-sets-conventions")

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
        configureEach {
            languageSettings {
                optIn("kotlinx.serialization.internal.CoreFriendModuleApi")
                optIn("kotlinx.serialization.json.internal.JsonFriendModuleApi")
            }
        }
        val commonTest by getting {
            dependencies {
                api(project(":kotlinx-serialization-json"))
                api(project(":kotlinx-serialization-json-okio"))
                implementation(libs.okio)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(libs.gson)
                implementation(libs.coroutines.core)
            }
        }
    }
}

project.configureJava9ModuleInfo()

// TODO: Remove this after okio will be updated to the version with 1.9.20 stdlib dependency
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.name == "kotlin-stdlib-wasm") {
            useTarget("org.jetbrains.kotlin:kotlin-stdlib-wasm-js:${requested.version}")
        }
    }
}
