/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import Java9Modularity.configureJava9ModuleInfo

plugins {
    kotlin("multiplatform")

    alias(libs.plugins.serialization)

    id("native-targets-conventions")
    id("source-sets-conventions")
}

kotlin {
    sourceSets {
        configureEach {
            languageSettings.optIn("kotlinx.serialization.internal.CoreFriendModuleApi")
        }

        commonMain {
            dependencies {
                api(project(":kotlinx-serialization-core"))
            }
        }

        jvmTest {
            dependencies {
                implementation(project(":kotlinx-serialization-protobuf:proto-test-model"))
                implementation(libs.kotlintest)
            }
        }
    }
}

configureJava9ModuleInfo()
