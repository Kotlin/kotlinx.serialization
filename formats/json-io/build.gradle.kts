/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import Java9Modularity.configureJava9ModuleInfo
import org.jetbrains.dokka.gradle.*
import java.net.*

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")

    id("native-targets-conventions")
    id("source-sets-conventions")
}

kotlin {
    sourceSets {
        configureEach {
            languageSettings {
                optIn("kotlinx.serialization.internal.CoreFriendModuleApi")
                optIn("kotlinx.serialization.json.internal.JsonFriendModuleApi")
            }
        }
        val commonMain by getting {
            dependencies {
                api(project(":kotlinx-serialization-core"))
                api(project(":kotlinx-serialization-json"))
                implementation(libs.kotlinx.io)
            }
        }
    }
}

project.configureJava9ModuleInfo()

dokka.dokkaSourceSets.configureEach {
    externalDocumentationLinks.register("kotlinx-io") {
        url("https://kotlinlang.org/api/kotlinx-io/")
        packageListUrl = file("dokka/kotlinx-io.package-list").toURI()
    }
}
