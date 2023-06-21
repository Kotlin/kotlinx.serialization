/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import Java9Modularity.configureJava9ModuleInfo
import org.jetbrains.dokka.gradle.*
import java.net.*

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

apply(from = rootProject.file("gradle/native-targets.gradle"))
apply(from = rootProject.file("gradle/configure-source-sets.gradle"))

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":kotlinx-serialization-core"))
                api(project(":kotlinx-serialization-json"))
                implementation("com.squareup.okio:okio:${property("okio_version")}")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation("com.squareup.okio:okio:${property("okio_version")}")
            }
        }
    }
}

project.configureJava9ModuleInfo()

tasks.named<DokkaTaskPartial>("dokkaHtmlPartial") {
    dokkaSourceSets {
        configureEach {
            externalDocumentationLink {
                url.set(URL("https://square.github.io/okio/3.x/okio/"))
                packageListUrl.set(
                    file("dokka/okio.package.list").toURI().toURL()
                )
            }
        }
    }
}
