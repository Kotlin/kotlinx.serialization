import Java9Modularity.configureJava9ModuleInfo
import org.jetbrains.kotlin.gradle.tasks.*

/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.serialization)

    id("native-targets-conventions")
    id("source-sets-conventions")
}

// disable kover tasks because there are no tests in the project
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
        commonMain {
            dependencies {
                api(project(":kotlinx-serialization-core"))
            }
        }
        register("jsWasmMain") {
            dependsOn(commonMain.get())
        }
        named("jsMain") {
            dependsOn(named("jsWasmMain").get())
        }
        named("wasmJsMain") {
            dependsOn(named("jsWasmMain").get())
        }
        named("wasmWasiMain") {
            dependsOn(named("jsWasmMain").get())
        }
    }
}

// This task should be disabled because of no need to build and publish intermediate JsWasm sourceset
tasks.whenTaskAdded {
    if (name == "compileJsWasmMainKotlinMetadata") {
        enabled = false
    }
}

configureJava9ModuleInfo()
