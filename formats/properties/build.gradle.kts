import Java9Modularity.configureJava9ModuleInfo

/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    kotlin("multiplatform")

    alias(libs.plugins.serialization)

    id("native-targets-conventions")
    id("source-sets-conventions")
}

kotlin {

    sourceSets {
        commonMain {
            dependencies {
                api(project(":kotlinx-serialization-core"))
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.kotlintest)
                implementation(libs.cbor)
                implementation(libs.jackson.core)
                implementation(libs.jackson.databind)
                implementation(libs.jackson.module.kotlin)
                implementation(libs.jackson.cbor)
            }
        }
    }
}

configureJava9ModuleInfo()
