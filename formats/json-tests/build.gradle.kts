/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import Java9Modularity.configureJava9ModuleInfo

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.serialization)

    id("native-targets-conventions")
    id("source-sets-conventions")
}

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
