/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import Java9Modularity.configureJava9ModuleInfo

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

apply(from = rootProject.file("gradle/native-targets.gradle"))
apply(from = rootProject.file("gradle/configure-source-sets.gradle"))

kotlin {
    targets.removeIf { it.name == "mingwX86" || it.name == "linuxArm64" || it.name == "linuxArm32Hfp" || it.name == "iosArm32" }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":kotlinx-serialization-core"))
                api(project(":kotlinx-serialization-json"))
                compileOnly("com.squareup.okio:okio-multiplatform:3.0.0-alpha.9")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation("com.squareup.okio:okio-multiplatform:3.0.0-alpha.9")
            }
        }
    }
}

project.configureJava9ModuleInfo()
