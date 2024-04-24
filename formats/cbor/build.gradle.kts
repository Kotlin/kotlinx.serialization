import Java9Modularity.configureJava9ModuleInfo

/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    kotlin("multiplatform")
    id("kotlinx-serialization")
}

apply(plugin = "native-targets-conventions")
apply(plugin = "source-sets-conventions")

kotlin {

    sourceSets {
        commonMain {
            dependencies {
                api(project(":kotlinx-serialization-core"))
            }
        }

        jvmTest {
            val jackson_version = property("jackson_version") as String

            dependencies {
                implementation("io.kotlintest:kotlintest:2.0.7")
                implementation("com.upokecenter:cbor:4.2.0")
                implementation("com.fasterxml.jackson.core:jackson-core:$jackson_version")
                implementation("com.fasterxml.jackson.core:jackson-databind:$jackson_version")
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jackson_version")
                implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-cbor:$jackson_version")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${property("coroutines_version") as String}")
            }
        }
    }
}

configureJava9ModuleInfo()
